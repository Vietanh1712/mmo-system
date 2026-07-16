package com.mmo.feature.complaint.controller;
import com.mmo.shared.model.Transaction;
import com.mmo.shared.model.User;
import com.mmo.shared.dal.UserRepository;

import com.mmo.shared.model.Complaint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.mmo.feature.complaint.service.ComplaintService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

import com.mmo.shared.dal.ChatRepository;
import com.mmo.shared.dal.ComplaintRepository;
import com.mmo.shared.model.Chat;

@RestController
@RequestMapping("/api/complaints")
public class ComplaintController {

    @Autowired
    private ComplaintService complaintService;

    @Autowired
    private com.mmo.shared.dal.UserRepository userRepository;

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private ComplaintRepository complaintRepository;

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

    private Map<String, Object> mapComplaintToDto(Complaint c) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", c.getId());
        map.put("description", c.getDescription());
        map.put("evidence", c.getEvidence());
        map.put("status", c.getStatus());
        map.put("resolution", c.getResolution());
        map.put("createdAt", c.getCreatedAt() != null ? c.getCreatedAt().toString() : null);

        if (c.getTransaction() != null) {
            Map<String, Object> tMap = new HashMap<>();
            tMap.put("id", c.getTransaction().getId());
            tMap.put("amountVnd", c.getTransaction().getAmountVnd());
            if (c.getTransaction().getProduct() != null) {
                tMap.put("productName", c.getTransaction().getProduct().getName());
            }
            map.put("transaction", tMap);
        }

        if (c.getCustomer() != null) {
            Map<String, Object> custMap = new HashMap<>();
            custMap.put("id", c.getCustomer().getId());
            custMap.put("email", c.getCustomer().getEmail());
            custMap.put("fullName", c.getCustomer().getFullName());
            map.put("customer", custMap);
        }

        if (c.getSeller() != null) {
            Map<String, Object> sellerMap = new HashMap<>();
            sellerMap.put("id", c.getSeller().getId());
            sellerMap.put("email", c.getSeller().getEmail());
            sellerMap.put("fullName", c.getSeller().getFullName());
            map.put("seller", sellerMap);
        }

        return map;
    }

    @PostMapping
    public ResponseEntity<?> createComplaint(
            @AuthenticationPrincipal Long userId,
            @RequestBody Map<String, Object> request) {

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Vui lòng đăng nhập trước khi gửi khiếu nại."));
        }

        Object txIdObj = request.get("transactionId");
        String description = (String) request.get("description");
        String evidence = (String) request.get("evidence");
        String preferredSolution = (String) request.get("preferredSolution");

        if (txIdObj == null || description == null || description.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Mã giao dịch và chi tiết khiếu nại không được để trống."));
        }

        try {
            Long transactionId = Long.valueOf(txIdObj.toString());
            Complaint complaint = complaintService.createComplaint(
                    userId, 
                    transactionId, 
                    description.trim(), 
                    evidence != null ? evidence.trim() : null,
                    preferredSolution != null ? preferredSolution.trim() : null
            );
            return ResponseEntity.ok(mapComplaintToDto(complaint));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Lỗi tạo khiếu nại: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getCustomerComplaints(@AuthenticationPrincipal Long userId) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Vui lòng đăng nhập để xem lịch sử khiếu nại."));
        }

        try {
            List<Complaint> complaints = complaintService.getCustomerComplaints(userId);
            List<Map<String, Object>> response = complaints.stream()
                    .map(this::mapComplaintToDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Lỗi lấy danh sách khiếu nại: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getComplaintById(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Vui lòng đăng nhập."));
        }

        try {
            Complaint complaint;
            if (isStaffOrAdmin(userId)) {
                complaint = complaintService.getComplaintByIdForStaff(id);
            } else {
                complaint = complaintService.getComplaintById(id, userId);
            }
            return ResponseEntity.ok(mapComplaintToDto(complaint));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Lỗi lấy chi tiết khiếu nại: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}/chat")
    public ResponseEntity<?> getComplaintChatHistory(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Vui lòng đăng nhập."));
        }
        if (!isStaffOrAdmin(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Bạn không có quyền truy cập thông tin này."));
        }

        try {
            List<com.mmo.shared.model.Chat> chats = complaintService.getComplaintChatHistory(id, userId);
            // Convert to DTO/Map if needed, for simplicity we can return the raw list or map it
            List<Map<String, Object>> response = chats.stream().map(c -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", c.getId());
                map.put("message", c.getMessage());
                map.put("createdAt", c.getCreatedAt());
                map.put("senderId", c.getSender() != null ? c.getSender().getId() : null);
                map.put("receiverId", c.getReceiver() != null ? c.getReceiver().getId() : null);
                return map;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Lỗi lấy tin nhắn: " + e.getMessage()));
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllComplaints(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Vui lòng đăng nhập."));
        }
        if (!isStaffOrAdmin(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Bạn không có quyền truy cập thông tin này."));
        }

        try {
            org.springframework.data.domain.Page<Complaint> complaintsPage = 
                    complaintService.searchComplaintsForStaff(keyword, status, page, size);
            org.springframework.data.domain.Page<Map<String, Object>> responsePage = 
                    complaintsPage.map(this::mapComplaintToDto);
            return ResponseEntity.ok(responsePage);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Lỗi lấy danh sách khiếu nại toàn hệ thống: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateComplaintStatus(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Vui lòng đăng nhập."));
        }
        if (!isStaffOrAdmin(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Bạn không có quyền cập nhật trạng thái khiếu nại."));
        }

        String status = request.get("status");
        String resolution = request.get("resolution");
        String flagLevel = request.get("flagLevel");
        String flagReason = request.get("flagReason");

        if (status == null || status.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Trạng thái status không được để trống."));
        }

        try {
            Complaint complaint = complaintService.updateComplaintStatus(
                    id, 
                    status.trim(), 
                    resolution != null ? resolution.trim() : null,
                    flagLevel,
                    flagReason,
                    userId
            );
            return ResponseEntity.ok(mapComplaintToDto(complaint));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Lỗi cập nhật khiếu nại: " + e.getMessage()));
        }
    }

    @GetMapping("/statuses")
    public ResponseEntity<?> getComplaintStatuses(@AuthenticationPrincipal Long userId) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Vui lòng đăng nhập."));
        }
        if (!isStaffOrAdmin(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Bạn không có quyền truy cập thông tin này."));
        }

        try {
            List<String> statuses = complaintService.getAllStatuses();
            return ResponseEntity.ok(statuses);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Lỗi lấy danh sách trạng thái khiếu nại: " + e.getMessage()));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getComplaintStats(@AuthenticationPrincipal Long userId) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Vui lòng đăng nhập."));
        }
        if (!isStaffOrAdmin(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Bạn không có quyền truy cập thông tin này."));
        }

        try {
            Map<String, Long> stats = new HashMap<>();
            stats.put("total", complaintService.getTotalComplaints());
            stats.put("inProgress", complaintService.getInProgressComplaints());
            stats.put("resolved", complaintService.getResolvedComplaints());
            stats.put("rejected", complaintService.getRefusedComplaints());
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Lỗi lấy thống kê khiếu nại: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/start-dispute")
    public ResponseEntity<?> startDispute(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Vui lòng đăng nhập."));
        }
        if (!isStaffOrAdmin(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Chỉ nhân viên mới được tiếp nhận đối chất."));
        }

        try {
            Complaint complaint = complaintService.startDispute(id, userId);
            return ResponseEntity.ok(mapComplaintToDto(complaint));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Lỗi mở cuộc đối chất: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}/chats")
    public ResponseEntity<?> getComplaintChats(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Vui lòng đăng nhập."));
        }

        try {
            Complaint complaint = complaintRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khiếu nại."));

            boolean isBuyer = complaint.getCustomer().getId().equals(userId);
            boolean isSeller = complaint.getSeller().getId().equals(userId);
            boolean isStaff = isStaffOrAdmin(userId);

            if (!isBuyer && !isSeller && !isStaff) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Bạn không có quyền truy cập cuộc đối chất này."));
            }

            List<Chat> chats = chatRepository.findByComplaintAndIsDeleteFalseOrderByCreatedAtAsc(complaint);
            List<Map<String, Object>> chatList = chats.stream().map(msg -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", msg.getId());
                map.put("senderId", msg.getSender().getId());
                map.put("senderName", msg.getSender().getFullName());
                
                String role = "Customer";
                if (msg.getSender().getId().equals(complaint.getSeller().getId())) {
                    role = "Seller";
                } else if (isStaffOrAdmin(msg.getSender().getId())) {
                    role = "Staff";
                }
                map.put("senderRole", role);
                map.put("message", msg.getMessage());
                map.put("createdAt", msg.getCreatedAt() != null ? msg.getCreatedAt().toString() : "");
                return map;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(chatList);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Lỗi lấy danh sách chat đối chất: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/chats")
    public ResponseEntity<?> sendComplaintChat(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Vui lòng đăng nhập."));
        }

        try {
            Complaint complaint = complaintRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khiếu nại."));

            if (!"In_Progress".equalsIgnoreCase(complaint.getStatus()) && !"InProgress".equalsIgnoreCase(complaint.getStatus())) {
                return ResponseEntity.badRequest().body(Map.of("message", "Phòng chat đối chất chưa được mở hoặc đã kết thúc."));
            }

            if (isStaffOrAdmin(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Nhân viên chỉ có quyền Read-only đối với phòng chat đối chất."));
            }

            boolean isBuyer = complaint.getCustomer().getId().equals(userId);
            boolean isSeller = complaint.getSeller().getId().equals(userId);

            if (!isBuyer && !isSeller) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Bạn không có quyền tham gia phòng chat này."));
            }

            String msgText = request.get("message");
            if (msgText == null || msgText.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Tin nhắn không được để trống."));
            }

            User currentUser = userRepository.findByIdAndIsDeleteFalse(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại."));
            User receiver = isBuyer ? complaint.getSeller() : complaint.getCustomer();

            Chat chat = new Chat();
            chat.setComplaint(complaint);
            chat.setSender(currentUser);
            chat.setReceiver(receiver);
            chat.setChatType("Complaint");
            chat.setMessage(msgText.trim());
            chat.setIsDelete(false);
            chat.setSenderDeleted(false);
            chat.setReceiverDeleted(false);
            chat.setIsRead(false);
            chat.setCreatedAt(LocalDateTime.now());

            chat = chatRepository.save(chat);

            Map<String, Object> response = new HashMap<>();
            response.put("id", chat.getId());
            response.put("senderId", userId);
            response.put("senderName", currentUser.getFullName());
            response.put("senderRole", isBuyer ? "Customer" : "Seller");
            response.put("message", chat.getMessage());
            response.put("createdAt", chat.getCreatedAt().toString());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Lỗi gửi tin nhắn đối chất: " + e.getMessage()));
        }
    }
}
