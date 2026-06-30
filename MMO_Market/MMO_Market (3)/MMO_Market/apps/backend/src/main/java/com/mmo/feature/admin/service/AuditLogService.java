package com.mmo.feature.admin.service;
import com.mmo.shared.model.Transaction;
import com.mmo.shared.model.KycStatus;
import com.mmo.shared.model.Notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmo.shared.dto.AuditLogDto;
import com.mmo.shared.dal.AuditLogRepository;
import com.mmo.shared.dal.UserRepository;
import com.mmo.shared.model.AuditLog;
import com.mmo.shared.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public AuditLogService(AuditLogRepository auditLogRepository,
                           UserRepository userRepository,
                           ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAuditLogs(Long operatorId, String search, String action, int page, int size) {
        // Enforce role authorization (Admin or Staff can view logs)
        User operator = requireAdminOrStaff(operatorId);

        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLog> logPage = auditLogRepository.searchLogs(
                (action == null || action.isBlank()) ? null : action,
                (search == null || search.isBlank()) ? null : search.trim(),
                pageable
        );

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

        List<AuditLogDto> dtos = logPage.getContent().stream().map(log -> {
            String operatorName = "Không rõ";
            if (log.getUserId() != null) {
                operatorName = userRepository.findById(log.getUserId())
                        .map(u -> {
                            String name = u.getFullName();
                            return (name == null || name.isBlank()) ? u.getEmail() : name;
                        })
                        .orElse("ID: " + log.getUserId());
            }

            String desc = log.getDetails();
            String ipAddress = "127.0.0.1";
            String diff = null;

            // Attempt to parse structured JSON details
            if (log.getDetails() != null && log.getDetails().trim().startsWith("{")) {
                try {
                    JsonNode node = objectMapper.readTree(log.getDetails());
                    if (node.has("desc")) {
                        desc = node.get("desc").asText();
                    }
                    if (node.has("ipAddress")) {
                        ipAddress = node.get("ipAddress").asText();
                    }
                    if (node.has("diff")) {
                        JsonNode diffNode = node.get("diff");
                        if (diffNode.isObject() || diffNode.isArray()) {
                            diff = objectMapper.writeValueAsString(diffNode);
                        } else {
                            diff = diffNode.asText();
                        }
                    }
                } catch (Exception ignored) {
                    // Fallback to text
                }
            }

            desc = cleanDescription(desc);

            // Fallback diff calculation for legacy plain-text logs
            if (diff == null || diff.isBlank() || "null".equals(diff)) {
                if ("LOCK_USER".equalsIgnoreCase(log.getAction())) {
                    diff = "{\"isLocked\":\"false -> true\"}";
                } else if ("UNLOCK_USER".equalsIgnoreCase(log.getAction())) {
                    diff = "{\"isLocked\":\"true -> false\"}";
                } else if ("CREATE_STAFF".equalsIgnoreCase(log.getAction())) {
                    diff = "{\"role\":\"none -> Staff\"}";
                } else if ("KYC_Approve".equalsIgnoreCase(log.getAction())) {
                    diff = "{\"kycStatus\":\"pending -> verified\"}";
                } else if ("Fund_Withdraw".equalsIgnoreCase(log.getAction())) {
                    diff = "{\"status\":\"pending -> completed\"}";
                }
            }

            String displayAction = log.getAction();
            if (displayAction != null) {
                String actUpper = displayAction.toUpperCase(Locale.ROOT);
                if (actUpper.contains("LOCK_USER") || actUpper.contains("UNLOCK_USER") || actUpper.contains("STAFF") || actUpper.contains("ROLE") || actUpper.contains("SOFT_DELETE_USER") || actUpper.contains("DELETE_USER")) {
                    displayAction = "Lock_User";
                } else if (actUpper.contains("CONFIG")) {
                    displayAction = "Config_Update";
                } else if (actUpper.contains("KYC")) {
                    displayAction = "KYC_Approve";
                } else if (actUpper.contains("WITHDRAW")) {
                    displayAction = "Fund_Withdraw";
                } else if (actUpper.contains("MAINTENANCE")) {
                    displayAction = "Maintenance_Toggle";
                } else if (actUpper.contains("NOTIFICATION_CREATE") || (actUpper.contains("NOTIFICATION") && actUpper.contains("CREATE"))) {
                    displayAction = "Notification_Create";
                } else if (actUpper.contains("NOTIFICATION_DELETE") || (actUpper.contains("NOTIFICATION") && actUpper.contains("DELETE"))) {
                    displayAction = "Notification_Delete";
                }
            }

            return AuditLogDto.builder()
                    .id(log.getId())
                    .timestamp(log.getCreatedAt() != null ? log.getCreatedAt().format(formatter) : "")
                    .operator(operatorName)
                    .action(displayAction)
                    .ipAddress(ipAddress)
                    .desc(desc)
                    .diff(diff)
                    .targetUserId(log.getTargetUserId())
                    .targetId(log.getTargetId())
                    .targetType(log.getTargetType())
                    .build();
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("content", dtos);
        result.put("page", logPage.getNumber());
        result.put("size", logPage.getSize());
        result.put("totalElements", logPage.getTotalElements());
        result.put("totalPages", logPage.getTotalPages());
        return result;
    }

    private User requireAdminOrStaff(Long operatorId) {
        if (operatorId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập.");
        }
        User user = userRepository.findByIdAndIsDeleteFalse(operatorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng."));
        
        String role = normalizeRole(user.getRole());
        if (!"Admin".equalsIgnoreCase(role) && !"Staff".equalsIgnoreCase(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện hành động này.");
        }
        if (Boolean.TRUE.equals(user.getIsLocked())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản của bạn đang bị khóa.");
        }
        return user;
    }

    private String normalizeRole(String roleValue) {
        if (roleValue == null || roleValue.isBlank()) {
            return "Customer";
        }
        try {
            JsonNode node = objectMapper.readTree(roleValue);
            JsonNode roleNode = node.get("role");
            if (roleNode != null && !roleNode.asText().isBlank()) {
                return canonicalRole(roleNode.asText());
            }
        } catch (Exception ignored) {}
        return canonicalRole(roleValue.replace("\"", "").trim());
    }

    @Transactional(readOnly = true)
    public byte[] exportAuditLogsCsv(Long operatorId, String search, String action) {
        User operator = requireAdminOrStaff(operatorId);

        Pageable pageable = PageRequest.of(0, 5000);
        Page<AuditLog> logPage = auditLogRepository.searchLogs(
                (action == null || action.isBlank()) ? null : action,
                (search == null || search.isBlank()) ? null : search.trim(),
                pageable
        );

        StringBuilder csv = new StringBuilder();
        // BOM for UTF-8 CSV Excel compatibility
        csv.append("\uFEFF");
        csv.append("STT,Mã,Thời gian,Nhân viên,Hành động,Chi tiết,Trạng thái\n");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        List<AuditLog> logs = logPage.getContent();
        for (int i = 0; i < logs.size(); i++) {
            AuditLog log = logs.get(i);

            String operatorName = "Không rõ";
            if (log.getUserId() != null) {
                operatorName = userRepository.findById(log.getUserId())
                        .map(u -> {
                            String name = u.getFullName();
                            return (name == null || name.isBlank()) ? u.getEmail() : name;
                        })
                        .orElse("ID: " + log.getUserId());
            }

            String desc = log.getDetails();
            if (log.getDetails() != null && log.getDetails().trim().startsWith("{")) {
                try {
                    JsonNode node = objectMapper.readTree(log.getDetails());
                    if (node.has("desc")) {
                        desc = node.get("desc").asText();
                    }
                } catch (Exception ignored) {}
            }

            String cleanDesc = desc == null ? "" : cleanDescription(desc).replace("\"", "\"\"").replace("\n", " ").replace("\r", "");
            String cleanOperator = operatorName.replace("\"", "\"\"");
            String displayAction = translateAction(log.getAction());

            csv.append(i + 1).append(",")
                    .append(log.getId()).append(",")
                    .append(log.getCreatedAt() != null ? log.getCreatedAt().format(formatter) : "").append(",")
                    .append("\"").append(cleanOperator).append("\",")
                    .append("\"").append(displayAction).append("\",")
                    .append("\"").append(cleanDesc).append("\",")
                    .append("Thành công\n");
        }

        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private String canonicalRole(String role) {
        if (role == null || role.isBlank()) {
            return "Customer";
        }
        String normalized = role.trim();
        if (normalized.toLowerCase(Locale.ROOT).contains("admin")) {
            return "Admin";
        }
        if (normalized.toLowerCase(Locale.ROOT).contains("staff")) {
            return "Staff";
        }
        if (normalized.toLowerCase(Locale.ROOT).contains("seller")) {
            return "Seller";
        }
        return "Customer";
    }

    private String cleanDescription(String desc) {
        if (desc == null) {
            return "";
        }
        String cleaned = desc.replaceAll("^.+?\\(\\d+\\)\\s+đã\s+", "Đã ");
        if (cleaned.length() > 0) {
            cleaned = cleaned.substring(0, 1).toUpperCase(Locale.ROOT) + cleaned.substring(1);
        }
        return cleaned;
    }

    private String translateAction(String action) {
        if (action == null) {
            return "";
        }
        String actUpper = action.toUpperCase(Locale.ROOT);
        if (actUpper.contains("LOCK_USER") || actUpper.contains("UNLOCK_USER") || actUpper.contains("STAFF") || actUpper.contains("ROLE") || actUpper.contains("SOFT_DELETE_USER") || actUpper.contains("DELETE_USER")) {
            return "Khóa tài khoản";
        } else if (actUpper.contains("CONFIG")) {
            return "Cập nhật cấu hình";
        } else if (actUpper.contains("KYC")) {
            return "Duyệt KYC";
        } else if (actUpper.contains("WITHDRAW")) {
            return "Duyệt rút tiền";
        } else if (actUpper.contains("MAINTENANCE")) {
            return "Bảo trì hệ thống";
        } else if (actUpper.contains("NOTIFICATION_CREATE") || (actUpper.contains("NOTIFICATION") && actUpper.contains("CREATE"))) {
            return "Tạo thông báo";
        } else if (actUpper.contains("NOTIFICATION_DELETE") || (actUpper.contains("NOTIFICATION") && actUpper.contains("DELETE"))) {
            return "Xóa thông báo";
        }
        return action;
    }
}
