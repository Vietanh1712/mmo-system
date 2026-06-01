package controller;

import dal.UserRepository;
import dal.SystemConfigurationRepository;
import dal.AuditLogRepository;
import model.User;
import model.SystemConfiguration;
import model.AuditLog;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SystemConfigurationRepository systemConfigurationRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    /**
     * Helper to verify if the current authenticated user has Admin or Staff roles.
     */
    private User verifyAdminOrStaff() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập.");
        }

        Long currentUserId = (Long) auth.getPrincipal();
        User currentUser = userRepository.findById(currentUserId).orElse(null);

        if (currentUser == null || Boolean.TRUE.equals(currentUser.getIsDelete())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tài khoản không tồn tại hoặc đã bị xóa.");
        }

        if (!isAdminOrStaff(currentUser)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập tính năng này.");
        }

        return currentUser;
    }

    private boolean isAdminOrStaff(User user) {
        if (user == null || user.getRole() == null) return false;
        String roleStr = user.getRole().toLowerCase();
        return roleStr.contains("admin") || roleStr.contains("staff");
    }

    /**
     * Get user list with filtering capability.
     */
    @GetMapping("/users")
    public ResponseEntity<?> getUsers(@RequestParam(required = false) String search,
                                      @RequestParam(required = false) String role) {
        try {
            User admin = verifyAdminOrStaff();
            List<User> users = userRepository.findAll().stream()
                    .filter(u -> !Boolean.TRUE.equals(u.getIsDelete()))
                    .collect(Collectors.toList());

            // Search filtering (email, full name, phone)
            if (search != null && !search.trim().isEmpty()) {
                String keyword = search.trim().toLowerCase();
                users = users.stream()
                        .filter(u -> (u.getEmail() != null && u.getEmail().toLowerCase().contains(keyword)) ||
                                (u.getFullName() != null && u.getFullName().toLowerCase().contains(keyword)) ||
                                (u.getPhone() != null && u.getPhone().contains(keyword)))
                        .collect(Collectors.toList());
            }

            // Role filtering
            if (role != null && !role.trim().isEmpty()) {
                String roleKeyword = role.trim().toLowerCase();
                users = users.stream()
                        .filter(u -> u.getRole() != null && u.getRole().toLowerCase().contains(roleKeyword))
                        .collect(Collectors.toList());
            }

            List<Map<String, Object>> response = users.stream().map(u -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", u.getId());
                map.put("email", u.getEmail());
                map.put("fullName", u.getFullName());
                map.put("role", u.getRole());
                map.put("phone", u.getPhone());
                map.put("shopStatus", u.getShopStatus());
                map.put("balanceVnd", u.getBalanceVnd());
                map.put("commissionPercent", u.getPermissions()); // stored custom commission % here
                map.put("isVerified", u.getIsVerified());
                map.put("createdAt", u.getCreatedAt());
                return map;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("message", e.getReason()));
        } catch (Exception e) {
            log.error("Lỗi khi lấy danh sách user: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    /**
     * Lock or Unlock a user account.
     */
    @PostMapping("/users/{id}/toggle-lock")
    public ResponseEntity<?> toggleLock(@PathVariable Long id) {
        try {
            User admin = verifyAdminOrStaff();
            User user = userRepository.findById(id).orElse(null);

            if (user == null || Boolean.TRUE.equals(user.getIsDelete())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Không tìm thấy người dùng."));
            }

            // Prevent Admin locking themselves or other admins
            if (user.getId().equals(admin.getId())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Bạn không thể tự khóa tài khoản của chính mình."));
            }

            boolean isBanned = "Banned".equalsIgnoreCase(user.getShopStatus());
            String newStatus = isBanned ? "Active" : "Banned";
            user.setShopStatus(newStatus);
            userRepository.save(user);

            // Log action
            String action = isBanned ? "UNLOCK_USER" : "LOCK_USER";
            String details = String.format("Nhân viên %s (%d) đã %s tài khoản %s (%d)",
                    admin.getFullName(), admin.getId(), isBanned ? "mở khóa" : "khóa", user.getEmail(), user.getId());

            AuditLog logEntry = AuditLog.builder()
                    .userId(admin.getId())
                    .action(action)
                    .details(details)
                    .build();
            auditLogRepository.save(logEntry);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", (isBanned ? "Mở khóa" : "Khóa") + " tài khoản thành công.",
                    "newStatus", newStatus
            ));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("message", e.getReason()));
        } catch (Exception e) {
            log.error("Lỗi khi khóa/mở khóa user: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    /**
     * Set User role (Staff / Customer / Seller).
     */
    @PostMapping("/users/{id}/role")
    public ResponseEntity<?> updateRole(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        try {
            User admin = verifyAdminOrStaff();
            User user = userRepository.findById(id).orElse(null);

            if (user == null || Boolean.TRUE.equals(user.getIsDelete())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Không tìm thấy người dùng."));
            }

            String targetRole = payload.get("role");
            if (targetRole == null || targetRole.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Vui lòng cung cấp vai trò hợp lệ."));
            }

            String roleJson = "{\"role\": \"" + targetRole + "\"}";
            String oldRole = user.getRole();
            user.setRole(roleJson);
            userRepository.save(user);

            // Log action
            String details = String.format("Nhân viên %s (%d) đã thay đổi vai trò của %s (%d) từ '%s' sang '%s'",
                    admin.getFullName(), admin.getId(), user.getEmail(), user.getId(), oldRole, roleJson);

            AuditLog logEntry = AuditLog.builder()
                    .userId(admin.getId())
                    .action("CHANGE_USER_ROLE")
                    .details(details)
                    .build();
            auditLogRepository.save(logEntry);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Cập nhật vai trò người dùng thành công.",
                    "newRole", roleJson
            ));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("message", e.getReason()));
        } catch (Exception e) {
            log.error("Lỗi khi cập nhật vai trò user: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    /**
     * Get commission configurations (Global Default & Sellers with custom commissions).
     */
    @GetMapping("/commissions")
    public ResponseEntity<?> getCommissions() {
        try {
            verifyAdminOrStaff();

            // Fetch default percent
            SystemConfiguration config = systemConfigurationRepository.findByConfigKey("DEFAULT_COMMISSION_PERCENT").orElse(null);
            String globalPercent = config != null ? config.getConfigValue() : "5.0";

            // Fetch all active sellers (role contains Seller or Customer_Seller)
            List<User> sellers = userRepository.findAll().stream()
                    .filter(u -> !Boolean.TRUE.equals(u.getIsDelete()))
                    .filter(u -> u.getRole() != null && u.getRole().toLowerCase().contains("seller"))
                    .collect(Collectors.toList());

            List<Map<String, Object>> sellerCommissions = sellers.stream().map(s -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", s.getId());
                map.put("email", s.getEmail());
                map.put("fullName", s.getFullName());
                map.put("role", s.getRole());
                map.put("customPercent", s.getPermissions()); // null/empty means using global default
                return map;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "globalDefaultPercent", globalPercent,
                    "sellers", sellerCommissions
            ));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("message", e.getReason()));
        } catch (Exception e) {
            log.error("Lỗi khi lấy cấu hình hoa hồng: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    /**
     * Configure System-wide Global Commission Percent.
     */
    @PostMapping("/commissions/global")
    public ResponseEntity<?> updateGlobalCommission(@RequestBody Map<String, String> payload) {
        try {
            User admin = verifyAdminOrStaff();
            String percentStr = payload.get("percent");

            if (percentStr == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Vui lòng nhập tỷ lệ phần trăm."));
            }

            double percent;
            try {
                percent = Double.parseDouble(percentStr);
                if (percent < 0 || percent > 100) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Tỷ lệ hoa hồng phải từ 0% đến 100%."));
                }
            } catch (NumberFormatException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Tỷ lệ hoa hồng không hợp lệ."));
            }

            SystemConfiguration config = systemConfigurationRepository.findByConfigKey("DEFAULT_COMMISSION_PERCENT")
                    .orElse(SystemConfiguration.builder().configKey("DEFAULT_COMMISSION_PERCENT").build());

            String oldValue = config.getConfigValue();
            config.setConfigValue(String.valueOf(percent));
            config.setDescription("Phần trăm hoa hồng mặc định sàn thu của Seller");
            config.setUpdatedBy(admin.getId());
            config.setUpdatedAt(LocalDateTime.now());
            systemConfigurationRepository.save(config);

            // Log action
            String details = String.format("Nhân viên %s (%d) đã cập nhật tỷ lệ hoa hồng mặc định hệ thống từ %s%% sang %s%%",
                    admin.getFullName(), admin.getId(), oldValue != null ? oldValue : "5.0", percentStr);

            AuditLog logEntry = AuditLog.builder()
                    .userId(admin.getId())
                    .action("UPDATE_GLOBAL_COMMISSION")
                    .details(details)
                    .build();
            auditLogRepository.save(logEntry);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Cập nhật hoa hồng mặc định hệ thống thành công.",
                    "newPercent", percent
            ));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("message", e.getReason()));
        } catch (Exception e) {
            log.error("Lỗi khi cập nhật hoa hồng mặc định: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    /**
     * Configure Custom Commission Percent for a specific Seller.
     */
    @PostMapping("/commissions/seller/{id}")
    public ResponseEntity<?> updateSellerCommission(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        try {
            User admin = verifyAdminOrStaff();
            User seller = userRepository.findById(id).orElse(null);

            if (seller == null || Boolean.TRUE.equals(seller.getIsDelete())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Không tìm thấy người bán."));
            }

            if (seller.getRole() == null || !seller.getRole().toLowerCase().contains("seller")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Người dùng này không phải là Seller."));
            }

            String percentStr = payload.get("percent"); // If null/empty/clear, reset to default (save null)

            if (percentStr != null && !percentStr.trim().isEmpty()) {
                double percent;
                try {
                    percent = Double.parseDouble(percentStr);
                    if (percent < 0 || percent > 100) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Tỷ lệ hoa hồng phải từ 0% đến 100%."));
                    }
                } catch (NumberFormatException e) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Tỷ lệ hoa hồng không hợp lệ."));
                }
                String oldValue = seller.getPermissions();
                seller.setPermissions(String.valueOf(percent)); // custom commission stored in permissions
                userRepository.save(seller);

                // Log action
                String details = String.format("Nhân viên %s (%d) đã thiết lập tỷ lệ hoa hồng riêng cho Seller %s (%d) là %s%% (Trước đó: %s%%)",
                        admin.getFullName(), admin.getId(), seller.getEmail(), seller.getId(), percentStr, oldValue != null ? oldValue : "Mặc định");

                AuditLog logEntry = AuditLog.builder()
                        .userId(admin.getId())
                        .action("SET_SELLER_COMMISSION")
                        .details(details)
                        .build();
                auditLogRepository.save(logEntry);
            } else {
                // Clear custom commission -> revert to global default
                String oldValue = seller.getPermissions();
                seller.setPermissions(null);
                userRepository.save(seller);

                // Log action
                String details = String.format("Nhân viên %s (%d) đã xóa tỷ lệ hoa hồng riêng của Seller %s (%d), chuyển về mặc định hệ thống. (Trước đó: %s%%)",
                        admin.getFullName(), admin.getId(), seller.getEmail(), seller.getId(), oldValue != null ? oldValue : "N/A");

                AuditLog logEntry = AuditLog.builder()
                        .userId(admin.getId())
                        .action("CLEAR_SELLER_COMMISSION")
                        .details(details)
                        .build();
                auditLogRepository.save(logEntry);
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Cập nhật hoa hồng của người bán thành công."
            ));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("message", e.getReason()));
        } catch (Exception e) {
            log.error("Lỗi khi cập nhật hoa hồng người bán: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    /**
     * Get system audit logs.
     */
    @GetMapping("/audit-logs")
    public ResponseEntity<?> getAuditLogs() {
        try {
            verifyAdminOrStaff();
            List<AuditLog> logs = auditLogRepository.findAllByOrderByCreatedAtDesc();

            // Map response including operator details
            List<Map<String, Object>> response = logs.stream().map(l -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", l.getId());
                map.put("userId", l.getUserId());
                map.put("action", l.getAction());
                map.put("details", l.getDetails());
                map.put("createdAt", l.getCreatedAt());

                // Fetch operator details
                User operator = userRepository.findById(l.getUserId()).orElse(null);
                if (operator != null) {
                    map.put("operatorEmail", operator.getEmail());
                    map.put("operatorName", operator.getFullName());
                } else {
                    map.put("operatorEmail", "Hệ thống");
                    map.put("operatorName", "System");
                }
                return map;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("message", e.getReason()));
        } catch (Exception e) {
            log.error("Lỗi khi lấy nhật ký hệ thống: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    // ResponseStatusException fallback class for older Spring versions compatibility
    private static class ResponseStatusException extends RuntimeException {
        private final HttpStatus status;
        private final String reason;

        public ResponseStatusException(HttpStatus status, String reason) {
            super(reason);
            this.status = status;
            this.reason = reason;
        }

        public HttpStatus getStatusCode() {
            return status;
        }

        public String getReason() {
            return reason;
        }
    }
}
