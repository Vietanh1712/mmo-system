package controller;

import model.SupportTicket;
import model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import service.SupportTicketService;
import dal.UserRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/support-tickets")
public class SupportTicketController {

    @Autowired
    private SupportTicketService supportTicketService;

    @Autowired
    private UserRepository userRepository;

    private boolean isStaffOrAdmin(Long userId) {
        if (userId == null) return false;
        return userRepository.findByIdAndIsDeleteFalse(userId)
                .map(user -> {
                    String roleValue = user.getRole();
                    if (roleValue == null) return false;
                    String roleLower = roleValue.toLowerCase();
                    return roleLower.contains("staff") || roleLower.contains("admin");
                })
                .orElse(false);
    }

    private Map<String, Object> mapTicketToDto(SupportTicket ticket) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", ticket.getId());
        map.put("category", ticket.getCategory());
        map.put("title", ticket.getTitle());
        map.put("description", ticket.getDescription());
        map.put("status", ticket.getStatus());
        map.put("resolution", ticket.getResolution());
        map.put("createdAt", ticket.getCreatedAt() != null ? ticket.getCreatedAt().toString() : null);
        
        if (ticket.getUser() != null) {
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", ticket.getUser().getId());
            userMap.put("email", ticket.getUser().getEmail());
            userMap.put("fullName", ticket.getUser().getFullName());
            map.put("user", userMap);
        }
        return map;
    }

    private boolean isCustomerOrSeller(Long userId) {
        if (userId == null) return false;
        return userRepository.findByIdAndIsDeleteFalse(userId)
                .map(user -> {
                    String roleValue = user.getRole();
                    if (roleValue == null) return false;
                    String roleLower = roleValue.toLowerCase();
                    return roleLower.contains("customer") || roleLower.contains("seller");
                })
                .orElse(false);
    }

    @PostMapping
    public ResponseEntity<?> createTicket(
            @AuthenticationPrincipal Long userId,
            @RequestBody Map<String, String> request) {

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Vui lòng đăng nhập trước khi gửi ticket."));
        }
        if (!isCustomerOrSeller(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Chỉ tài khoản Customer hoặc Seller mới được phép tạo ticket hỗ trợ."));
        }

        String category = request.get("category");
        String title = request.get("title");
        String description = request.get("description");

        if (category == null || category.trim().isEmpty() ||
            title == null || title.trim().isEmpty() ||
            description == null || description.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Các trường category, title và description không được để trống."));
        }

        try {
            SupportTicket ticket = supportTicketService.createTicket(userId, category.trim(), title.trim(), description.trim());
            return ResponseEntity.ok(mapTicketToDto(ticket));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Lỗi tạo ticket: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getUserTickets(@AuthenticationPrincipal Long userId) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Vui lòng đăng nhập để xem lịch sử ticket."));
        }
        if (!isCustomerOrSeller(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Chỉ tài khoản Customer hoặc Seller mới có lịch sử ticket."));
        }

        try {
            List<SupportTicket> tickets = supportTicketService.getUserTickets(userId);
            List<Map<String, Object>> response = tickets.stream()
                    .map(this::mapTicketToDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Lỗi lấy danh sách ticket: " + e.getMessage()));
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllTickets(@AuthenticationPrincipal Long userId) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Vui lòng đăng nhập."));
        }
        if (!isStaffOrAdmin(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Bạn không có quyền xem tất cả ticket."));
        }

        try {
            List<SupportTicket> tickets = supportTicketService.getAllTickets();
            List<Map<String, Object>> response = tickets.stream()
                    .map(this::mapTicketToDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Lỗi lấy danh sách ticket toàn hệ thống: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTicketById(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Vui lòng đăng nhập."));
        }

        try {
            SupportTicket ticket = supportTicketService.getTicketById(id);
            // Quyền truy cập: Chủ sở hữu ticket hoặc Staff/Admin
            if (!ticket.getUser().getId().equals(userId) && !isStaffOrAdmin(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Bạn không có quyền xem chi tiết ticket này."));
            }
            return ResponseEntity.ok(mapTicketToDto(ticket));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Lỗi chi tiết ticket: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateTicketStatus(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Vui lòng đăng nhập."));
        }
        if (!isStaffOrAdmin(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Bạn không có quyền cập nhật ticket."));
        }

        String status = request.get("status");
        String resolution = request.get("resolution");

        if (status == null || status.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Trạng thái status không được để trống."));
        }

        try {
            SupportTicket ticket = supportTicketService.updateTicketStatus(id, status.trim(), resolution != null ? resolution.trim() : null);
            return ResponseEntity.ok(mapTicketToDto(ticket));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Lỗi cập nhật ticket: " + e.getMessage()));
        }
    }
}
