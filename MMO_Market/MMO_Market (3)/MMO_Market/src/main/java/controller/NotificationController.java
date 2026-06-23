package controller;

import controller.dto.AdminActionResponse;
import controller.dto.NotificationCreateRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import service.NotificationService;

import java.util.List;
import java.util.Map;

@RestController
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/api/notifications")
    public Map<String, Object> getNotifications(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        return notificationService.getNotifications(search, type, page, size);
    }

    @GetMapping("/api/admin/notifications/maintenance-status")
    public Map<String, Object> getMaintenanceStatus() {
        return notificationService.getMaintenanceStatus();
    }

    @PostMapping("/api/admin/notifications")
    public AdminActionResponse createNotification(
            @AuthenticationPrincipal Long operatorId,
            @RequestBody NotificationCreateRequest request,
            HttpServletRequest servletRequest) {
        try {
            boolean shouldActivate = request.getActivateMaintenance() != null && request.getActivateMaintenance();
            notificationService.createNotification(
                    operatorId,
                    request.getTitle(),
                    request.getContent(),
                    request.getType(),
                    shouldActivate,
                    getClientIp(servletRequest)
            );
            return AdminActionResponse.builder()
                    .success(true)
                    .message("Phát thông báo thành công.")
                    .build();
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return AdminActionResponse.builder()
                    .success(false)
                    .message(e.getReason())
                    .build();
        } catch (Exception e) {
            return AdminActionResponse.builder()
                    .success(false)
                    .message("Không thể phát thông báo: " + e.getMessage())
                    .build();
        }
    }

    @PostMapping("/api/admin/notifications/toggle-maintenance")
    public AdminActionResponse toggleMaintenance(
            @AuthenticationPrincipal Long operatorId,
            @RequestBody Map<String, Boolean> request,
            HttpServletRequest servletRequest) {
        try {
            Boolean active = request.get("active");
            if (active == null) {
                active = false;
            }
            notificationService.toggleMaintenance(operatorId, active, getClientIp(servletRequest));
            return AdminActionResponse.builder()
                    .success(true)
                    .message(active ? "Đã kích hoạt chế độ bảo trì hệ thống." : "Đã tắt chế độ bảo trì hệ thống.")
                    .build();
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return AdminActionResponse.builder()
                    .success(false)
                    .message(e.getReason())
                    .build();
        } catch (Exception e) {
            return AdminActionResponse.builder()
                    .success(false)
                    .message("Thao tác thất bại: " + e.getMessage())
                    .build();
        }
    }

    @DeleteMapping("/api/admin/notifications/{id}")
    public AdminActionResponse deleteNotification(
            @AuthenticationPrincipal Long operatorId,
            @PathVariable("id") Long id,
            HttpServletRequest servletRequest) {
        try {
            notificationService.deleteNotification(operatorId, id, getClientIp(servletRequest));
            return AdminActionResponse.builder()
                    .success(true)
                    .message("Xóa thông báo thành công.")
                    .build();
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return AdminActionResponse.builder()
                    .success(false)
                    .message(e.getReason())
                    .build();
        } catch (Exception e) {
            return AdminActionResponse.builder()
                    .success(false)
                    .message("Không thể xóa thông báo: " + e.getMessage())
                    .build();
        }
    }

    // User endpoints (logged in)
    @GetMapping("/api/v1/notifications")
    public List<Map<String, Object>> getUserNotifications(@AuthenticationPrincipal Long userId) {
        return notificationService.getUserNotifications(userId);
    }

    @PostMapping("/api/v1/notifications/{id}/read")
    public AdminActionResponse markAsRead(@AuthenticationPrincipal Long userId, @PathVariable("id") Long id) {
        try {
            notificationService.markAsRead(userId, id);
            return AdminActionResponse.builder()
                    .success(true)
                    .message("Đã đánh dấu thông báo là đã đọc.")
                    .build();
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return AdminActionResponse.builder()
                    .success(false)
                    .message(e.getReason())
                    .build();
        } catch (Exception e) {
            return AdminActionResponse.builder()
                    .success(false)
                    .message("Lỗi: " + e.getMessage())
                    .build();
        }
    }

    @PostMapping("/api/v1/notifications/mark-all-read")
    public AdminActionResponse markAllAsRead(@AuthenticationPrincipal Long userId) {
        try {
            notificationService.markAllAsRead(userId);
            return AdminActionResponse.builder()
                    .success(true)
                    .message("Đã đánh dấu tất cả thông báo là đã đọc.")
                    .build();
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return AdminActionResponse.builder()
                    .success(false)
                    .message(e.getReason())
                    .build();
        } catch (Exception e) {
            return AdminActionResponse.builder()
                    .success(false)
                    .message("Lỗi: " + e.getMessage())
                    .build();
        }
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "127.0.0.1";
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            ip = "127.0.0.1";
        }
        return ip;
    }
}
