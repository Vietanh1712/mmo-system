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

        if (txIdObj == null || description == null || description.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Mã giao dịch và chi tiết khiếu nại không được để trống."));
        }

        try {
            Long transactionId = Long.valueOf(txIdObj.toString());
            Complaint complaint = complaintService.createComplaint(userId, transactionId, description.trim(), evidence != null ? evidence.trim() : null);
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
}
