package com.mmo.feature.chat.controller;

import com.mmo.shared.dal.ChatRepository;
import com.mmo.shared.dal.UserRepository;
import com.mmo.shared.model.Chat;
import com.mmo.shared.model.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.mmo.feature.admin.service.UserStatusService;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/staff/chat")
public class StaffChatRestController {

    private final ChatRepository chatRepository;
    private final UserRepository userRepository;
    private final UserStatusService userStatusService;
    private final com.mmo.shared.dal.ComplaintRepository complaintRepository;

    public StaffChatRestController(ChatRepository chatRepository, 
                                  UserRepository userRepository,
                                  UserStatusService userStatusService,
                                  com.mmo.shared.dal.ComplaintRepository complaintRepository) {
        this.chatRepository = chatRepository;
        this.userRepository = userRepository;
        this.userStatusService = userStatusService;
        this.complaintRepository = complaintRepository;
    }

    private User getStaff(Long userId) {
        User user = userRepository.findByIdAndIsDeleteFalse(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin tài khoản."));
        if (!user.getRole().contains("Staff") && !user.getRole().contains("Admin")) {
            throw new IllegalArgumentException("Bạn không có quyền truy cập chức năng này.");
        }
        return user;
    }

    @GetMapping("/conversations")
    public ResponseEntity<?> getConversations(@AuthenticationPrincipal Long userId) {
        try {
            User staff = getStaff(userId);
            userStatusService.updateActiveTime(staff.getId());
            List<Chat> allChats = chatRepository.findRecentNormalChatsForUser(staff);

            // Group by other user to get the latest chat message
            Map<Long, Chat> latestChats = new LinkedHashMap<>();
            Map<Long, Integer> unreadCounts = new HashMap<>();

            for (Chat chat : allChats) {
                User otherUser = chat.getSender().getId().equals(staff.getId()) ? chat.getReceiver() : chat.getSender();
                Long otherId = otherUser.getId();

                if (!latestChats.containsKey(otherId)) {
                    latestChats.put(otherId, chat);
                }

                // Count unread if the staff is the receiver and message is not read
                if (chat.getReceiver().getId().equals(staff.getId()) && Boolean.FALSE.equals(chat.getIsRead())) {
                    unreadCounts.put(otherId, unreadCounts.getOrDefault(otherId, 0) + 1);
                }
            }

            List<Map<String, Object>> result = new ArrayList<>();
            for (Map.Entry<Long, Chat> entry : latestChats.entrySet()) {
                Long otherId = entry.getKey();
                Chat chat = entry.getValue();
                User otherUser = chat.getSender().getId().equals(staff.getId()) ? chat.getReceiver() : chat.getSender();

                Map<String, Object> map = new HashMap<>();
                map.put("userId", otherUser.getId());
                map.put("userName", otherUser.getFullName());
                map.put("userEmail", otherUser.getEmail());
                map.put("lastMessage", chat.getMessage());
                map.put("lastMessageTime", chat.getCreatedAt().toString());
                map.put("unreadCount", unreadCounts.getOrDefault(otherId, 0));
                map.put("online", userStatusService.isOnline(otherId));
                
                // Determine user role badge
                String roleBadge = "Customer";
                if (otherUser.getRole() != null) {
                    if (otherUser.getRole().contains("Seller")) roleBadge = "Seller";
                    else if (otherUser.getRole().contains("Staff")) roleBadge = "Staff";
                }
                map.put("userRole", roleBadge);

                result.add(map);
            }

            // Inject active complaints group chats for Staff
            List<com.mmo.shared.model.Complaint> activeComplaints = complaintRepository.findAllByIsDeleteFalseOrderByCreatedAtDesc();
            for (com.mmo.shared.model.Complaint comp : activeComplaints) {
                if ("In_Progress".equalsIgnoreCase(comp.getStatus()) || "InProgress".equalsIgnoreCase(comp.getStatus())) {
                    List<Chat> compChats = chatRepository.findByComplaintAndIsDeleteFalseOrderByCreatedAtAsc(comp);
                    String latestMsg = "Hệ thống: Đối chất đang diễn ra.";
                    java.time.LocalDateTime latestTime = comp.getCreatedAt();
                    if (!compChats.isEmpty()) {
                        Chat lastChat = compChats.get(compChats.size() - 1);
                        latestMsg = lastChat.getMessage();
                        latestTime = lastChat.getCreatedAt();
                    }

                    long unread = compChats.stream()
                        .filter(c -> !c.getSender().getId().equals(userId) && (c.getIsRead() == null || Boolean.FALSE.equals(c.getIsRead())))
                        .count();

                    Map<String, Object> map = new HashMap<>();
                    map.put("userId", -comp.getId()); // Use negative ID to represent complaint chat
                    map.put("userName", "Tranh chấp #CMP-" + comp.getId());
                    map.put("userEmail", comp.getCustomer().getFullName() + " vs " + comp.getSeller().getFullName());
                    map.put("lastMessage", latestMsg);
                    map.put("lastMessageTime", latestTime.toString());
                    map.put("unreadCount", unread);
                    map.put("online", true);
                    map.put("userRole", "Dispute");
                    result.add(map);
                }
            }

            // Sort by latest message time descending
            result.sort((m1, m2) -> {
                String t1 = (String) m1.get("lastMessageTime");
                String t2 = (String) m2.get("lastMessageTime");
                if (t1 == null && t2 == null) return 0;
                if (t1 == null) return 1;
                if (t2 == null) return -1;
                return t2.compareTo(t1);
            });

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchUsers(@AuthenticationPrincipal Long userId, @RequestParam String keyword) {
        try {
            getStaff(userId);
            userStatusService.updateActiveTime(userId);
            if (keyword == null || keyword.trim().isEmpty()) {
                return ResponseEntity.ok(Collections.emptyList());
            }

            List<User> users = userRepository.searchUsers(keyword);
            List<Map<String, Object>> result = users.stream().limit(10).map(u -> {
                Map<String, Object> map = new HashMap<>();
                map.put("userId", u.getId());
                map.put("userName", u.getFullName());
                map.put("userEmail", u.getEmail());
                map.put("online", userStatusService.isOnline(u.getId()));
                
                String roleBadge = "Customer";
                if (u.getRole() != null) {
                    if (u.getRole().contains("Seller")) roleBadge = "Seller";
                    else if (u.getRole().contains("Staff")) roleBadge = "Staff";
                }
                map.put("userRole", roleBadge);
                
                return map;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{targetUserId}")
    public ResponseEntity<?> getChatHistory(@AuthenticationPrincipal Long userId, @PathVariable Long targetUserId) {
        try {
            User staff = getStaff(userId);
            userStatusService.updateActiveTime(staff.getId());

            if (targetUserId < 0) {
                Long complaintId = -targetUserId;
                com.mmo.shared.model.Complaint complaint = complaintRepository.findById(complaintId)
                        .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khiếu nại."));
                
                List<Chat> chats = chatRepository.findByComplaintAndIsDeleteFalseOrderByCreatedAtAsc(complaint);
                List<Map<String, Object>> result = chats.stream().map(c -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", c.getId());
                    map.put("senderId", c.getSender().getId());
                    map.put("senderName", c.getSender().getFullName() != null ? c.getSender().getFullName() : c.getSender().getEmail().split("@")[0]);
                    map.put("message", c.getMessage());
                    map.put("createdAt", c.getCreatedAt().toString());
                    
                    boolean isStaffMessage = c.getSender().getRole() != null && (c.getSender().getRole().contains("Staff") || c.getSender().getRole().contains("Admin"));
                    map.put("type", isStaffMessage ? "out" : "in");
                    
                    String role = "Khách hàng";
                    if (c.getSender().getId().equals(complaint.getSeller().getId())) {
                        role = "Cửa hàng";
                    } else if (isStaffMessage) {
                        role = "Staff";
                    }
                    map.put("role", role);
                    return map;
                }).collect(Collectors.toList());

                Map<String, Object> response = new HashMap<>();
                response.put("messages", result);
                if (complaint.getTransaction() != null && complaint.getTransaction().getProduct() != null) {
                    response.put("contextProductId", complaint.getTransaction().getProduct().getId());
                }
                return ResponseEntity.ok(response);
            }

            User targetUser = userRepository.findByIdAndIsDeleteFalse(targetUserId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng."));

            List<Chat> chats = chatRepository.findNormalChatsBetween(staff, targetUser);

            // Mark as read
            boolean hasUnread = false;
            for (Chat chat : chats) {
                if (chat.getReceiver().getId().equals(staff.getId()) && Boolean.FALSE.equals(chat.getIsRead())) {
                    chat.setIsRead(true);
                    hasUnread = true;
                }
            }
            if (hasUnread) {
                chatRepository.saveAll(chats);
            }

            List<Map<String, Object>> result = chats.stream().map(c -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", c.getId());
                map.put("senderId", c.getSender().getId());
                map.put("senderName", c.getSender().getFullName());
                map.put("message", c.getMessage());
                map.put("createdAt", c.getCreatedAt().toString());
                map.put("type", c.getSender().getId().equals(staff.getId()) ? "out" : "in");
                return map;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{targetUserId}")
    public ResponseEntity<?> sendChatMessage(@AuthenticationPrincipal Long userId, @PathVariable Long targetUserId, @RequestBody Map<String, String> request) {
        try {
            User staff = getStaff(userId);
            userStatusService.updateActiveTime(staff.getId());

            if (targetUserId < 0) {
                return ResponseEntity.status(403).body(Map.of("message", "Nhân viên chỉ có quyền Read-only đối với phòng chat đối chất."));
            }

            User targetUser = userRepository.findByIdAndIsDeleteFalse(targetUserId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng."));


            String message = request.get("message");
            if (message == null || message.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Tin nhắn không được để trống."));
            }

            Chat chat = new Chat();
            chat.setSender(staff);
            chat.setReceiver(targetUser);
            chat.setChatType("Normal");
            chat.setMessage(message);
            chat.setIsRead(false);
            chat.setIsDelete(false);
            chatRepository.save(chat);

            Map<String, Object> map = new HashMap<>();
            map.put("id", chat.getId());
            map.put("senderId", chat.getSender().getId());
            map.put("senderName", chat.getSender().getFullName());
            map.put("message", chat.getMessage());
            map.put("createdAt", chat.getCreatedAt().toString());
            map.put("type", "out");  // Staff always sends "out"

            return ResponseEntity.ok(map);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
