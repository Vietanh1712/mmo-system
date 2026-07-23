package com.mmo.feature.notification.service;
import com.mmo.shared.model.Transaction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmo.shared.dal.AuditLogRepository;
import com.mmo.shared.dal.NotificationRepository;
import com.mmo.shared.dal.SystemConfigurationRepository;
import com.mmo.shared.dal.UserRepository;
import com.mmo.shared.model.AuditLog;
import com.mmo.shared.model.Notification;
import com.mmo.shared.model.SystemConfiguration;
import com.mmo.shared.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SystemConfigurationRepository systemConfigurationRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public NotificationService(NotificationRepository notificationRepository,
                               SystemConfigurationRepository systemConfigurationRepository,
                               UserRepository userRepository,
                               AuditLogRepository auditLogRepository,
                               ObjectMapper objectMapper) {
        this.notificationRepository = notificationRepository;
        this.systemConfigurationRepository = systemConfigurationRepository;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getNotifications(String search, String type, int page, int size) {
        Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        Page<Notification> notifPage = notificationRepository.searchNotifications(
                (type == null || type.isBlank()) ? null : type,
                (search == null || search.isBlank()) ? null : search.trim(),
                pageable
        );

        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

        List<Map<String, Object>> dtos = notifPage.getContent().stream().map(n -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", n.getId());
            map.put("timestamp", n.getCreatedAt() != null ? n.getCreatedAt().format(formatter) : "");
            map.put("title", n.getTitle());
            map.put("content", n.getContent());
            map.put("type", n.getType());

            String authorName = "Hệ thống";
            if (n.getUserId() != null) {
                authorName = userRepository.findById(n.getUserId())
                        .map(User::getFullName)
                        .orElse("Admin");
            }
            map.put("author", authorName);
            return map;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("content", dtos);
        result.put("page", notifPage.getNumber());
        result.put("size", notifPage.getSize());
        result.put("totalElements", notifPage.getTotalElements());
        result.put("totalPages", notifPage.getTotalPages());
        return result;
    }

    @Transactional
    public void createNotification(Long operatorId, String title, String content, String type, boolean activateMaintenance, String ipAddress) {
        User operator = requireAdminOrStaff(operatorId);

        if (title == null || title.trim().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tiêu đề không được để trống.");
        }
        if (content == null || content.trim().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nội dung không được để trống.");
        }
        if (type == null || type.trim().isBlank()) {
            type = "info";
        }

        Notification notif = Notification.builder()
                .userId(operator.getId())
                .title(title.trim())
                .content(content.trim())
                .type(type.trim().toLowerCase())
                .isDelete(false)
                .isRead(false)
                .severity("INFO")
                .build();
        notificationRepository.save(notif);

        // Audit Log
        Map<String, Object> diff = new HashMap<>();
        diff.put("title", title);
        diff.put("type", type);
        saveAuditLog(operator, "Notification_Create", "Đã tạo thông báo: " + title, ipAddress, diff);

        if (activateMaintenance && "maintenance".equalsIgnoreCase(type)) {
            updateMaintenanceConfig("TRUE", operator.getId());

            Map<String, Object> maintDiff = new HashMap<>();
            maintDiff.put("maintenance_mode", "FALSE -> TRUE");
            saveAuditLog(operator, "Maintenance_Toggle", "Kích hoạt chế độ bảo trì hệ thống qua thông báo", ipAddress, maintDiff);
        }
    }

    @Transactional
    public void deleteNotification(Long operatorId, Long notifId, String ipAddress) {
        User operator = requireAdminOrStaff(operatorId);
        Notification notif = notificationRepository.findById(notifId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thông báo."));

        if (Boolean.TRUE.equals(notif.getIsDelete())) {
            return;
        }

        notif.setIsDelete(true);
        notificationRepository.save(notif);

        Map<String, Object> diff = new HashMap<>();
        diff.put("isDelete", "false -> true");
        saveAuditLog(operator, "Notification_Delete", "Xóa thông báo: " + notif.getTitle(), ipAddress, diff);
    }

    @Transactional
    public void toggleMaintenance(Long operatorId, boolean active, String ipAddress) {
        User operator = requireAdminOrStaff(operatorId);
        String val = active ? "TRUE" : "FALSE";

        String oldVal = systemConfigurationRepository.findByConfigKey("MAINTENANCE_MODE")
                .map(SystemConfiguration::getConfigValue)
                .orElse("FALSE");

        if (oldVal.equalsIgnoreCase(val)) {
            return;
        }

        updateMaintenanceConfig(val, operator.getId());

        Map<String, Object> diff = new HashMap<>();
        diff.put("maintenance_mode", oldVal + " -> " + val);
        saveAuditLog(operator, "Maintenance_Toggle", active ? "Kích hoạt chế độ bảo trì hệ thống" : "Tắt chế độ bảo trì hệ thống", ipAddress, diff);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getMaintenanceStatus() {
        String val = systemConfigurationRepository.findByConfigKey("MAINTENANCE_MODE")
                .map(SystemConfiguration::getConfigValue)
                .orElse("FALSE");

        Map<String, Object> result = new HashMap<>();
        result.put("active", "TRUE".equalsIgnoreCase(val));

        if ("TRUE".equalsIgnoreCase(val)) {
            Page<Notification> latest = notificationRepository.searchNotifications(
                    "maintenance",
                    null,
                    org.springframework.data.domain.PageRequest.of(0, 1)
            );
            if (!latest.isEmpty()) {
                result.put("message", latest.getContent().get(0).getContent());
            } else {
                result.put("message", "Hệ thống đang bảo trì nâng cấp định kỳ. Xin lỗi vì sự bất tiện.");
            }
        } else {
            result.put("message", "");
        }
        return result;
    }

    @Transactional
    public List<Map<String, Object>> getUserNotifications(Long userId) {
        User user = userRepository.findByIdAndIsDeleteFalse(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng."));

        List<Notification> personal = notificationRepository.findAllByUserIdAndIsDeleteFalseOrderByCreatedAtDesc(userId);

        List<Notification> broadcasts = notificationRepository.findAllBroadcastNotifications();

        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");

        List<Map<String, Object>> result = new ArrayList<>();

        for (Notification n : personal) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", String.valueOf(n.getId()));
            map.put("type", n.getType().toUpperCase());
            map.put("title", n.getTitle());
            map.put("message", n.getContent());
            map.put("status", Boolean.TRUE.equals(n.getIsRead()) ? "READ" : "UNREAD");
            map.put("severity", n.getSeverity() != null ? n.getSeverity().toUpperCase() : "INFO");
            map.put("createdAt", n.getCreatedAt() != null ? n.getCreatedAt().format(formatter) : "");
            map.put("targetUrl", n.getTargetUrl() != null ? n.getTargetUrl() : "#");
            map.put("isBroadcast", false);
            result.add(map);
        }

        for (Notification n : broadcasts) {
            Map<String, Object> map = new HashMap<>();
            
            String type = "SYSTEM";
            String severity = "INFO";
            if ("warning".equalsIgnoreCase(n.getType())) {
                type = "SECURITY";
                severity = "WARNING";
            } else if ("maintenance".equalsIgnoreCase(n.getType())) {
                type = "SYSTEM";
                severity = "DANGER";
            }

            map.put("id", "SYS-" + n.getCreatedAt().toString());
            map.put("type", type);
            map.put("title", n.getTitle());
            map.put("message", n.getContent());
            map.put("status", "UNREAD");
            map.put("severity", severity);
            map.put("createdAt", n.getCreatedAt() != null ? n.getCreatedAt().format(formatter) : "");
            map.put("targetUrl", "/account/notifications");
            map.put("isBroadcast", true);

            java.time.format.DateTimeFormatter isoFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
            map.put("originalTimestamp", n.getCreatedAt().format(isoFormatter));
            result.add(map);
        }

        result.sort((a, b) -> {
            String ca = (String) a.get("createdAt");
            String cb = (String) b.get("createdAt");
            try {
                LocalDateTime dtA = LocalDateTime.parse(ca, java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy"));
                LocalDateTime dtB = LocalDateTime.parse(cb, java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy"));
                return dtB.compareTo(dtA);
            } catch (Exception e) {
                return 0;
            }
        });

        return result;
    }

    @Transactional
    public void markAsRead(Long userId, Long notifId) {
        Notification notif = notificationRepository.findById(notifId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thông báo."));
        if (!userId.equals(notif.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không có quyền thực hiện thao tác này.");
        }
        notif.setIsRead(true);
        notificationRepository.save(notif);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> list = notificationRepository.findAllByUserIdAndIsDeleteFalseOrderByCreatedAtDesc(userId);
        for (Notification n : list) {
            if (!Boolean.TRUE.equals(n.getIsRead())) {
                n.setIsRead(true);
            }
        }
        notificationRepository.saveAll(list);
    }



    private void updateMaintenanceConfig(String value, Long operatorId) {
        SystemConfiguration config = systemConfigurationRepository.findByConfigKey("MAINTENANCE_MODE")
                .orElse(SystemConfiguration.builder()
                        .configKey("MAINTENANCE_MODE")
                        .description("Trạng thái bảo trì hệ thống (TRUE/FALSE)")
                        .build());
        config.setConfigValue(value);
        config.setUpdatedBy(operatorId);
        config.setUpdatedAt(LocalDateTime.now());
        systemConfigurationRepository.save(config);
    }

    private User requireAdminOrStaff(Long operatorId) {
        if (operatorId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập.");
        }
        User operator = userRepository.findByIdAndIsDeleteFalse(operatorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng."));
        String role = normalizeRole(operator.getRole());
        if (!"Admin".equalsIgnoreCase(role) && !"Staff".equalsIgnoreCase(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ Admin hoặc Nhân viên mới có quyền thực hiện chức năng này.");
        }
        if (Boolean.TRUE.equals(operator.getIsLocked())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản đang bị khóa.");
        }
        return operator;
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

    private void saveAuditLog(User operator, String action, String desc, String ipAddress, Map<String, Object> diff) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("desc", desc);
            payload.put("ipAddress", ipAddress);
            payload.put("diff", diff);

            String jsonDetails = objectMapper.writeValueAsString(payload);

            auditLogRepository.save(AuditLog.builder()
                    .userId(operator.getId())
                    .action(action)
                    .details(jsonDetails)
                    .build());
        } catch (Exception e) {
            auditLogRepository.save(AuditLog.builder()
                    .userId(operator.getId())
                    .action(action)
                    .details(desc)
                    .build());
        }
    }
}
