package com.mmo.feature.chat.controller.api;

import com.mmo.feature.chat.service.ChatService;
import com.mmo.shared.model.ChatMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller cung cấp API phục vụ tính năng Nhắn tin (Chat) thời gian thực giữa Người mua và Người bán.
 * Cho phép xem danh sách người đang chat, xem lịch sử tin nhắn và đánh dấu đã đọc.
 */
@RestController("apiChatController")
@RequestMapping("/api/v2/chats")
public class ChatController {

    @Autowired
    private ChatService chatService;

    /**
     * Lấy danh sách các cuộc trò chuyện (Hộp thoại) hiện có của người dùng.
     * Bao gồm thông tin đối tác chat, tin nhắn cuối cùng và số lượng tin nhắn chưa đọc.
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getActiveChats(@AuthenticationPrincipal Long userId) {
        List<Map<String, Object>> activeChats = chatService.getActiveChats(userId);
        return ResponseEntity.ok(activeChats);
    }

    /**
     * Lấy lịch sử tin nhắn chi tiết với một đối tác cụ thể.
     * @param partnerId ID của đối tác chat (Ví dụ: ID của Shop).
     */
    @GetMapping("/{partnerId}")
    public ResponseEntity<?> getChatHistory(@AuthenticationPrincipal Long userId, @PathVariable Long partnerId) {
        List<ChatMessage> history = chatService.getChatHistory(userId, partnerId, 50);
        return ResponseEntity.ok(history);
    }

    /**
     * Đánh dấu toàn bộ tin nhắn trong cuộc trò chuyện với đối tác này là "đã đọc".
     */
    @PostMapping("/{partnerId}/read")
    public ResponseEntity<?> markMessagesAsRead(@AuthenticationPrincipal Long userId, @PathVariable Long partnerId) {
        chatService.markAsRead(userId, partnerId);
        return ResponseEntity.ok().build();
    }
}
