package controller;

import dal.ChatRepository;
import dal.UserRepository;
import model.Chat;
import model.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/staff/chat")
public class StaffChatRestController {

    private final ChatRepository chatRepository;
    private final UserRepository userRepository;

    public StaffChatRestController(ChatRepository chatRepository, UserRepository userRepository) {
        this.chatRepository = chatRepository;
        this.userRepository = userRepository;
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
                
                // Determine user role badge
                String roleBadge = "Customer";
                if (otherUser.getRole() != null) {
                    if (otherUser.getRole().contains("Seller")) roleBadge = "Seller";
                    else if (otherUser.getRole().contains("Staff")) roleBadge = "Staff";
                }
                map.put("userRole", roleBadge);

                result.add(map);
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchUsers(@AuthenticationPrincipal Long userId, @RequestParam String keyword) {
        try {
            getStaff(userId);
            if (keyword == null || keyword.trim().isEmpty()) {
                return ResponseEntity.ok(Collections.emptyList());
            }

            List<User> users = userRepository.searchUsers(keyword);
            List<Map<String, Object>> result = users.stream().limit(10).map(u -> {
                Map<String, Object> map = new HashMap<>();
                map.put("userId", u.getId());
                map.put("userName", u.getFullName());
                map.put("userEmail", u.getEmail());
                
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

            return ResponseEntity.ok(map);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
