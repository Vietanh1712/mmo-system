package com.mmo.feature.chat.controller;

import com.mmo.shared.dal.ChatBlockRepository;
import com.mmo.shared.dal.ChatMuteRepository;
import com.mmo.shared.dal.ChatRepository;
import com.mmo.shared.dal.UserRepository;
import com.mmo.shared.model.Chat;
import com.mmo.shared.model.ChatBlock;
import com.mmo.shared.model.ChatMute;
import com.mmo.shared.model.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.mmo.feature.admin.service.UserStatusService;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/chats")
public class ChatController {

    private final ChatRepository chatRepository;
    private final ChatBlockRepository chatBlockRepository;
    private final ChatMuteRepository chatMuteRepository;
    private final UserRepository userRepository;
    private final UserStatusService userStatusService;

    public ChatController(ChatRepository chatRepository,
                          ChatBlockRepository chatBlockRepository,
                          ChatMuteRepository chatMuteRepository,
                          UserRepository userRepository,
                          UserStatusService userStatusService) {
        this.chatRepository = chatRepository;
        this.chatBlockRepository = chatBlockRepository;
        this.chatMuteRepository = chatMuteRepository;
        this.userRepository = userRepository;
        this.userStatusService = userStatusService;
    }

    // 0. Get contact info by userId (for chat header when no existing chat)
    @GetMapping("/contact/{contactId}/info")
    public ResponseEntity<?> getContactInfo(@AuthenticationPrincipal Long userId,
                                             @PathVariable Long contactId) {
        userStatusService.updateActiveTime(userId);
        Optional<User> contactOpt = userRepository.findById(contactId);
        if (contactOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        User contact = contactOpt.get();
        String name = contact.getFullName() != null ? contact.getFullName() : contact.getEmail().split("@")[0];
        String avatar = (name.length() >= 2) ? name.substring(0, 2).toUpperCase() : "US";
        Map<String, Object> info = new HashMap<>();
        info.put("id", contact.getId());
        info.put("name", name);
        info.put("avatar", avatar);
        info.put("online", userStatusService.isOnline(contactId));
        return ResponseEntity.ok(info);
    }

    // 1. Get list of chat contacts (recent contacts) for current user
    @GetMapping
    public ResponseEntity<?> getChatContacts(@AuthenticationPrincipal Long userId) {
        userStatusService.updateActiveTime(userId);
        Optional<User> currentUserOpt = userRepository.findById(userId);
        if (currentUserOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "User not found"));
        }
        User currentUser = currentUserOpt.get();

        // Fetch ALL chats (including soft-deleted) to build contact list
        // This ensures contacts stay in sidebar even after clearing history
        List<Chat> allChats = chatRepository.findAllChatsForContactList(currentUser);

        // Group by contact ID to get latest message for each contact
        Map<Long, Chat> contactLatestChat = new HashMap<>();
        for (Chat chat : allChats) {
            User contact = chat.getSender().getId().equals(userId) ? chat.getReceiver() : chat.getSender();
            Long contactId = contact.getId();
            Chat existing = contactLatestChat.get(contactId);
            if (existing == null || chat.getCreatedAt().isAfter(existing.getCreatedAt())) {
                contactLatestChat.put(contactId, chat);
            }
        }

        // Map to response objects, excluding:
        // 1. Self-contacts (sender = receiver)
        // 2. Contacts where current user cleared history (latest message is deleted)
        List<Map<String, Object>> responseList = contactLatestChat.entrySet().stream()
                .filter(entry -> {
                    if (entry.getKey().equals(userId)) return false; // exclude self
                    Chat latestChat = entry.getValue();
                    boolean isCurrentUserSender = latestChat.getSender().getId().equals(userId);
                    boolean isDeletedByUser = isCurrentUserSender
                            ? Boolean.TRUE.equals(latestChat.getSenderDeleted())
                            : Boolean.TRUE.equals(latestChat.getReceiverDeleted());
                    return !isDeletedByUser; // hide contact if history was cleared
                })
                .map(entry -> {
                    Long contactId = entry.getKey();
                    Chat latestChat = entry.getValue();
                    User contactUser = latestChat.getSender().getId().equals(userId) ? latestChat.getReceiver() : latestChat.getSender();

                    boolean isBlocked = chatBlockRepository.existsByBlockerAndBlocked(currentUser, contactUser);
                    boolean isBlockedByContact = chatBlockRepository.existsByBlockerAndBlocked(contactUser, currentUser);
                    boolean isMuted = chatMuteRepository.existsByUserAndContact(currentUser, contactUser);
                    long unreadCount = chatRepository.countUnreadFrom(contactUser, currentUser);

                    Map<String, Object> map = new HashMap<>();
                    map.put("contactId", contactId);
                    map.put("name", contactUser.getFullName() != null ? contactUser.getFullName() : contactUser.getEmail().split("@")[0]);
                    map.put("avatar", (contactUser.getFullName() != null && contactUser.getFullName().length() >= 2) ?
                            contactUser.getFullName().substring(0, 2).toUpperCase() : "US");
                    map.put("latestMessage", latestChat.getMessage());
                    map.put("latestTime", latestChat.getCreatedAt());
                    map.put("isBlocked", isBlocked);
                    map.put("isBlockedByContact", isBlockedByContact);
                    map.put("isMuted", isMuted);
                    map.put("online", userStatusService.isOnline(contactId));
                    map.put("unreadCount", unreadCount);
                    return map;
                })
                .sorted((m1, m2) -> {
                    java.time.LocalDateTime t1 = (java.time.LocalDateTime) m1.get("latestTime");
                    java.time.LocalDateTime t2 = (java.time.LocalDateTime) m2.get("latestTime");
                    return t2.compareTo(t1); // Sort desc by time
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(responseList);
    }


    // 2. Get active chats between current user and contact
    @GetMapping("/{contactId}")
    public ResponseEntity<?> getChatHistory(@AuthenticationPrincipal Long userId, @PathVariable Long contactId) {
        userStatusService.updateActiveTime(userId);
        Optional<User> currentUserOpt = userRepository.findById(userId);
        Optional<User> contactOpt = userRepository.findById(contactId);
        if (currentUserOpt.isEmpty() || contactOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "User or contact not found"));
        }

        List<Chat> chats = chatRepository.findActiveChatsBetweenUsers(currentUserOpt.get(), contactOpt.get());

        // Mark all incoming messages from contact as read
        chatRepository.markAllReadFrom(contactOpt.get(), currentUserOpt.get());

        // Find the context product from the first message that has a productId
        Long contextProductId = chats.stream()
                .filter(c -> c.getProductId() != null)
                .findFirst()
                .map(Chat::getProductId)
                .orElse(null);

        List<Map<String, Object>> messageList = chats.stream().map(c -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", c.getId());
            map.put("senderId", c.getSender().getId());
            map.put("receiverId", c.getReceiver().getId());
            map.put("message", c.getMessage());
            map.put("createdAt", c.getCreatedAt());
            map.put("type", c.getSender().getId().equals(userId) ? "out" : "in");
            return map;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("messages", messageList);
        result.put("contextProductId", contextProductId);

        return ResponseEntity.ok(result);

    }

    // 3. Send message to contact
    @PostMapping("/{contactId}")
    public ResponseEntity<?> sendMessage(@AuthenticationPrincipal Long userId, 
                                         @PathVariable Long contactId, 
                                         @RequestBody Map<String, String> request) {

        // Guard: cannot send message to yourself
        if (userId.equals(contactId)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Không thể gửi tin nhắn cho chính mình."));
        }

        Optional<User> senderOpt = userRepository.findById(userId);
        Optional<User> receiverOpt = userRepository.findById(contactId);
        if (senderOpt.isEmpty() || receiverOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Sender or receiver not found"));
        }

        User sender = senderOpt.get();
        User receiver = receiverOpt.get();

        // Check block status
        boolean hasBlocked = chatBlockRepository.existsByBlockerAndBlocked(sender, receiver);
        boolean isBlockedByReceiver = chatBlockRepository.existsByBlockerAndBlocked(receiver, sender);
        if (hasBlocked) {
            return ResponseEntity.badRequest().body(Map.of("message", "Bạn đã chặn liên hệ này. Vui lòng mở chặn để tiếp tục gửi tin nhắn."));
        }
        if (isBlockedByReceiver) {
            return ResponseEntity.badRequest().body(Map.of("message", "Không thể gửi tin nhắn. Bạn đã bị liên hệ này chặn."));
        }

        String msgText = request.get("message");
        if (msgText == null || msgText.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Nội dung tin nhắn không thể bỏ trống."));
        }

        // Parse optional productId for product inquiry context
        Long productId = null;
        String productIdStr = request.get("productId");
        if (productIdStr != null && !productIdStr.isBlank()) {
            try { productId = Long.parseLong(productIdStr); } catch (NumberFormatException ignored) {}
        }

        Chat chat = Chat.builder()
                .sender(sender)
                .receiver(receiver)
                .message(msgText.trim())
                .chatType("Normal")
                .isDelete(false)
                .senderDeleted(false)
                .receiverDeleted(false)
                .productId(productId)
                .build();

        Chat saved = chatRepository.save(chat);

        Map<String, Object> response = new HashMap<>();
        response.put("id", saved.getId());
        response.put("senderId", userId);
        response.put("receiverId", contactId);
        response.put("message", saved.getMessage());
        response.put("createdAt", saved.getCreatedAt() != null ? saved.getCreatedAt() : java.time.LocalDateTime.now());
        response.put("type", "out");

        return ResponseEntity.ok(response);
    }

    // 4. Clear chat history with contact
    @DeleteMapping("/{contactId}/history")
    public ResponseEntity<?> clearHistory(@AuthenticationPrincipal Long userId, @PathVariable Long contactId) {
        Optional<User> currentUserOpt = userRepository.findById(userId);
        Optional<User> contactOpt = userRepository.findById(contactId);
        if (currentUserOpt.isEmpty() || contactOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "User or contact not found"));
        }

        User currentUser = currentUserOpt.get();
        User contact = contactOpt.get();

        List<Chat> chats = chatRepository.findActiveChatsBetweenUsers(currentUser, contact);
        for (Chat chat : chats) {
            if (chat.getSender().getId().equals(userId)) {
                chat.setSenderDeleted(true);
            } else {
                chat.setReceiverDeleted(true);
            }
            chatRepository.save(chat);
        }

        return ResponseEntity.ok(Map.of("message", "Đã xóa lịch sử trò chuyện thành công."));
    }

    // 5. Block contact
    @PostMapping("/{contactId}/block")
    public ResponseEntity<?> blockContact(@AuthenticationPrincipal Long userId, @PathVariable Long contactId) {
        Optional<User> currentUserOpt = userRepository.findById(userId);
        Optional<User> contactOpt = userRepository.findById(contactId);
        if (currentUserOpt.isEmpty() || contactOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "User or contact not found"));
        }

        User currentUser = currentUserOpt.get();
        User contact = contactOpt.get();

        if (chatBlockRepository.existsByBlockerAndBlocked(currentUser, contact)) {
            return ResponseEntity.ok(Map.of("message", "Liên hệ đã được chặn từ trước."));
        }

        ChatBlock block = ChatBlock.builder()
                .blocker(currentUser)
                .blocked(contact)
                .build();
        chatBlockRepository.save(block);

        return ResponseEntity.ok(Map.of("message", "Đã chặn liên hệ thành công."));
    }

    // 6. Unblock contact
    @PostMapping("/{contactId}/unblock")
    public ResponseEntity<?> unblockContact(@AuthenticationPrincipal Long userId, @PathVariable Long contactId) {
        Optional<User> currentUserOpt = userRepository.findById(userId);
        Optional<User> contactOpt = userRepository.findById(contactId);
        if (currentUserOpt.isEmpty() || contactOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "User or contact not found"));
        }

        User currentUser = currentUserOpt.get();
        User contact = contactOpt.get();

        Optional<ChatBlock> blockOpt = chatBlockRepository.findByBlockerAndBlocked(currentUser, contact);
        if (blockOpt.isPresent()) {
            chatBlockRepository.delete(blockOpt.get());
            return ResponseEntity.ok(Map.of("message", "Đã mở chặn liên hệ thành công."));
        }

        return ResponseEntity.ok(Map.of("message", "Liên hệ chưa được chặn."));
    }

    // 7. Search active chats with contact by keyword
    @GetMapping("/{contactId}/search")
    public ResponseEntity<?> searchChats(@AuthenticationPrincipal Long userId, 
                                         @PathVariable Long contactId, 
                                         @RequestParam String keyword) {
        Optional<User> currentUserOpt = userRepository.findById(userId);
        Optional<User> contactOpt = userRepository.findById(contactId);
        if (currentUserOpt.isEmpty() || contactOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "User or contact not found"));
        }

        if (keyword == null || keyword.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Từ khóa tìm kiếm không thể bỏ trống."));
        }

        List<Chat> chats = chatRepository.searchActiveChatsBetweenUsers(currentUserOpt.get(), contactOpt.get(), keyword.trim());
        List<Map<String, Object>> messageList = chats.stream().map(c -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", c.getId());
            map.put("senderId", c.getSender().getId());
            map.put("receiverId", c.getReceiver().getId());
            map.put("message", c.getMessage());
            map.put("createdAt", c.getCreatedAt());
            map.put("type", c.getSender().getId().equals(userId) ? "out" : "in");
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(messageList);
    }

    // 8. Mute notifications for contact
    @PostMapping("/{contactId}/mute")
    public ResponseEntity<?> muteContact(@AuthenticationPrincipal Long userId, @PathVariable Long contactId) {
        Optional<User> currentUserOpt = userRepository.findById(userId);
        Optional<User> contactOpt = userRepository.findById(contactId);
        if (currentUserOpt.isEmpty() || contactOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "User or contact not found"));
        }

        User currentUser = currentUserOpt.get();
        User contact = contactOpt.get();

        if (chatMuteRepository.existsByUserAndContact(currentUser, contact)) {
            return ResponseEntity.ok(Map.of("message", "Liên hệ đã được tắt thông báo từ trước."));
        }

        ChatMute mute = ChatMute.builder()
                .user(currentUser)
                .contact(contact)
                .build();
        chatMuteRepository.save(mute);

        return ResponseEntity.ok(Map.of("message", "Đã tắt thông báo cuộc trò chuyện thành công."));
    }

    // 9. Unmute notifications for contact
    @PostMapping("/{contactId}/unmute")
    public ResponseEntity<?> unmuteContact(@AuthenticationPrincipal Long userId, @PathVariable Long contactId) {
        Optional<User> currentUserOpt = userRepository.findById(userId);
        Optional<User> contactOpt = userRepository.findById(contactId);
        if (currentUserOpt.isEmpty() || contactOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "User or contact not found"));
        }

        User currentUser = currentUserOpt.get();
        User contact = contactOpt.get();

        Optional<ChatMute> muteOpt = chatMuteRepository.findByUserAndContact(currentUser, contact);
        if (muteOpt.isPresent()) {
            chatMuteRepository.delete(muteOpt.get());
            return ResponseEntity.ok(Map.of("message", "Đã bật thông báo cuộc trò chuyện thành công."));
        }

        return ResponseEntity.ok(Map.of("message", "Liên hệ chưa được tắt thông báo."));
    }
}
