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

/**
 * Dịch vụ xử lý Thông báo Hệ thống (System Notifications) và Chế độ Bảo trì (Maintenance Mode).
 * Hỗ trợ tạo, lưu bản nháp, phát hành thông báo toàn sàn, ghi nhận nhật ký kiểm toán và gửi thông báo tới người dùng.
 */
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

    /**
     * Lấy danh sách thông báo hệ thống có phân trang và lọc (phiên bản thu gọn).
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getNotifications(String search, String type, int page, int size) {
        return getNotifications(search, type, "ALL", null, null, "DESC", page, size);
    }

    /**
     * Lấy danh sách thông báo hệ thống có lọc theo khoảng thời gian và thứ tự sắp xếp.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getNotifications(String search, String type, String startDateStr, String endDateStr, String sort, int page, int size) {
        return getNotifications(search, type, "ALL", startDateStr, endDateStr, sort, page, size);
    }

    /**
     * Lấy danh sách thông báo hệ thống đầy đủ tiêu chí (tìm kiếm, loại, trạng thái, ngày tạo, sắp xếp và phân trang).
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getNotifications(String search, String type, String status, String startDateStr, String endDateStr, String sort, int page, int size) {
        LocalDateTime startAt = null;
        LocalDateTime endAt = null;
        if (startDateStr != null && !startDateStr.isBlank()) {
            try { startAt = java.time.LocalDate.parse(startDateStr.trim()).atStartOfDay(); } catch (Exception ignored) {}
        }
        if (endDateStr != null && !endDateStr.isBlank()) {
            try { endAt = java.time.LocalDate.parse(endDateStr.trim()).atTime(23, 59, 59); } catch (Exception ignored) {}
        }

        org.springframework.data.domain.Sort.Direction direction = "ASC".equalsIgnoreCase(sort)
                ? org.springframework.data.domain.Sort.Direction.ASC
                : org.springframework.data.domain.Sort.Direction.DESC;

        Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by(direction, "createdAt"));
        Page<Notification> notifPage = notificationRepository.searchNotifications(
                (type == null || type.isBlank()) ? null : type,
                (status == null || status.isBlank()) ? "PUBLISHED" : status.trim(),
                (search == null || search.isBlank()) ? null : search.trim(),
                startAt,
                endAt,
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
            map.put("status", n.getStatus() != null ? n.getStatus() : "PUBLISHED");
            map.put("activateMaintenance", Boolean.TRUE.equals(n.getActivateMaintenance()));

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

    /**
     * Tạo và phát hành thông báo mới toàn sàn, tự động ghi nhận nhật ký kiểm toán.
     */
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
                .status("PUBLISHED")
                .isDelete(false)
                .isRead(false)
                .severity("INFO")
                .activateMaintenance(activateMaintenance)
                .build();
        notificationRepository.save(notif);

        // Audit Log
        Map<String, Object> diff = new HashMap<>();
        diff.put("title", title);
        diff.put("type", type);
        saveAuditLog(operator, "Notification_Create", "Đã phát hành thông báo: " + title, ipAddress, diff);

        if (activateMaintenance && "maintenance".equalsIgnoreCase(type)) {
            updateMaintenanceConfig("TRUE", operator.getId());

            Map<String, Object> maintDiff = new HashMap<>();
            maintDiff.put("maintenance_mode", "FALSE -> TRUE");
            saveAuditLog(operator, "Maintenance_Toggle", "Kích hoạt chế độ bảo trì hệ thống qua thông báo", ipAddress, maintDiff);
        }
    }

    /**
     * Lưu thông báo hệ thống dưới dạng bản nháp (không kích hoạt bảo trì).
     */
    @Transactional
    public void saveDraft(Long operatorId, String title, String content, String type, String ipAddress) {
        saveDraft(operatorId, title, content, type, false, ipAddress);
    }

    /**
     * Lưu thông báo hệ thống dưới dạng bản nháp với đầy đủ tham số.
     */
    @Transactional
    public void saveDraft(Long operatorId, String title, String content, String type, boolean activateMaintenance, String ipAddress) {
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
                .status("DRAFT")
                .isDelete(false)
                .isRead(false)
                .severity("INFO")
                .activateMaintenance(activateMaintenance)
                .build();
        notificationRepository.save(notif);

        Map<String, Object> diff = new HashMap<>();
        diff.put("title", title);
        saveAuditLog(operator, "Notification_Draft_Save", "Lưu bản nháp thông báo: " + title, ipAddress, diff);
    }

    /**
     * Cập nhật thông tin bản nháp (phiên bản thu gọn).
     */
    @Transactional
    public void updateDraft(Long operatorId, Long draftId, String title, String content, String type, String ipAddress) {
        updateDraft(operatorId, draftId, title, content, type, false, ipAddress);
    }

    /**
     * Cập nhật tiêu đề, nội dung và cài đặt bảo trì của bản nháp thông báo.
     */
    @Transactional
    public void updateDraft(Long operatorId, Long draftId, String title, String content, String type, Boolean activateMaintenance, String ipAddress) {
        User operator = requireAdminOrStaff(operatorId);
        Notification notif = notificationRepository.findById(draftId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thông báo."));

        if (!"DRAFT".equalsIgnoreCase(notif.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thông báo đã phát hành không thể chỉnh sửa.");
        }

        if (title == null || title.trim().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tiêu đề không được để trống.");
        }
        if (content == null || content.trim().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nội dung không được để trống.");
        }

        notif.setTitle(title.trim());
        notif.setContent(content.trim());
        if (type != null && !type.trim().isBlank()) {
            notif.setType(type.trim().toLowerCase());
        }
        if (activateMaintenance != null) {
            notif.setActivateMaintenance(activateMaintenance);
        }
        notificationRepository.save(notif);

        Map<String, Object> diff = new HashMap<>();
        diff.put("title", title);
        saveAuditLog(operator, "Notification_Draft_Update", "Cập nhật bản nháp: " + title, ipAddress, diff);
    }

    /**
     * Phát hành bản nháp thông báo thành thông báo chính thức toàn sàn.
     */
    @Transactional
    public void publishDraft(Long operatorId, Long draftId, boolean activateMaintenance, String ipAddress) {
        User operator = requireAdminOrStaff(operatorId);
        Notification notif = notificationRepository.findById(draftId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy bản nháp."));

        if (!"DRAFT".equalsIgnoreCase(notif.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thông báo này đã được phát hành trước đó.");
        }

        notif.setStatus("PUBLISHED");
        notif.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notif);

        Map<String, Object> diff = new HashMap<>();
        diff.put("title", notif.getTitle());
        saveAuditLog(operator, "Notification_Publish", "Phát hành bản nháp: " + notif.getTitle(), ipAddress, diff);

        boolean shouldActivate = activateMaintenance || Boolean.TRUE.equals(notif.getActivateMaintenance());

        if (shouldActivate && "maintenance".equalsIgnoreCase(notif.getType())) {
            updateMaintenanceConfig("TRUE", operator.getId());

            Map<String, Object> maintDiff = new HashMap<>();
            maintDiff.put("maintenance_mode", "FALSE -> TRUE");
            saveAuditLog(operator, "Maintenance_Toggle", "Kích hoạt chế độ bảo trì hệ thống qua phát hành bản nháp", ipAddress, maintDiff);
        }
    }

    /**
     * Đánh dấu xóa mềm (`isDelete = true`) đối với bản nháp thông báo.
     */
    @Transactional
    public void deleteNotification(Long operatorId, Long notifId, String ipAddress) {
        User operator = requireAdminOrStaff(operatorId);
        Notification notif = notificationRepository.findById(notifId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thông báo."));

        if (!"DRAFT".equalsIgnoreCase(notif.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thông báo đã phát hành không thể xóa.");
        }

        if (Boolean.TRUE.equals(notif.getIsDelete())) {
            return;
        }

        notif.setIsDelete(true);
        notificationRepository.save(notif);

        Map<String, Object> diff = new HashMap<>();
        diff.put("isDelete", "false -> true");
        saveAuditLog(operator, "Notification_Delete", "Xóa bản nháp thông báo: " + notif.getTitle(), ipAddress, diff);
    }

    /**
     * Kích hoạt hoặc tắt Chế độ Bảo trì Hệ thống (MAINTENANCE_MODE) và ghi log audit.
     */
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

        if (active) {
            Page<Notification> existing = notificationRepository.searchNotifications(
                    "maintenance",
                    "PUBLISHED",
                    null,
                    null,
                    null,
                    org.springframework.data.domain.PageRequest.of(0, 1, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"))
            );
            if (existing == null || existing.isEmpty()) {
                Notification notif = Notification.builder()
                        .userId(operator.getId())
                        .title("Thông báo bảo trì hệ thống")
                        .content("Hệ thống đang thực hiện bảo trì nâng cấp định kỳ. Xin lỗi vì sự bất tiện.")
                        .type("maintenance")
                        .status("PUBLISHED")
                        .isDelete(false)
                        .isRead(false)
                        .severity("DANGER")
                        .build();
                notificationRepository.save(notif);
            }
        }
    }

    /**
     * Truy xuất trạng thái bảo trì hệ thống hiện tại cùng nội dung thông báo bảo trì gần nhất.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getMaintenanceStatus() {
        String val = systemConfigurationRepository.findByConfigKey("MAINTENANCE_MODE")
                .map(SystemConfiguration::getConfigValue)
                .orElse("FALSE");

        Map<String, Object> result = new HashMap<>();
        boolean active = "TRUE".equalsIgnoreCase(val);
        result.put("active", active);

        if (active) {
            Page<Notification> latest = notificationRepository.searchNotifications(
                    "maintenance",
                    "PUBLISHED",
                    null,
                    null,
                    null,
                    org.springframework.data.domain.PageRequest.of(0, 1, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"))
            );
            if (!latest.isEmpty()) {
                result.put("message", latest.getContent().get(0).getContent());
                result.put("latestNotifId", latest.getContent().get(0).getId());
            } else {
                result.put("message", "Hệ thống đang bảo trì nâng cấp định kỳ. Xin lỗi vì sự bất tiện.");
                result.put("latestNotifId", null);
            }
        } else {
            result.put("message", "");
            result.put("latestNotifId", null);
        }
        return result;
    }

    /**
     * Lấy danh sách thông báo cá nhân và thông báo toàn sàn (broadcast) cho người dùng đăng nhập.
     */
    @Transactional
    public List<Map<String, Object>> getUserNotifications(Long userId) {
        User user = userRepository.findByIdAndIsDeleteFalse(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng."));

        List<Notification> personal = notificationRepository.findAllByUserIdAndIsDeleteFalseOrderByCreatedAtDesc(userId);

        List<Notification> broadcasts = notificationRepository.findAllBroadcastNotifications();

        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");

        List<Map<String, Object>> result = new ArrayList<>();

        String userRole = normalizeRole(user.getRole());

        for (Notification n : personal) {
            // Admin chỉ nhận thông báo hệ thống/an ninh/chính sách, không nhận thông báo tác nghiệp xử lý của Staff (KYC, rút tiền, ticket, khiếu nại)
            if ("Admin".equalsIgnoreCase(userRole)) {
                String nType = n.getType() != null ? n.getType().toUpperCase() : "";
                String targetUrl = n.getTargetUrl() != null ? n.getTargetUrl().toLowerCase() : "";
                if ("KYC".equals(nType) || targetUrl.contains("/staff/")) {
                    continue;
                }
            }

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

    /**
     * Đánh dấu một thông báo cụ thể của người dùng là đã đọc (`isRead = true`).
     */
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

    /**
     * Đánh dấu tất cả thông báo của người dùng là đã đọc.
     */
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
