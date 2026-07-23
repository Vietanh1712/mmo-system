package com.mmo.feature.chat.controller.api;

import com.mmo.feature.chat.service.ChatService;
import com.mmo.shared.model.ChatMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController("apiChatController")
@RequestMapping("/api/v2/chats")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getActiveChats(@AuthenticationPrincipal Long userId) {
        List<Map<String, Object>> activeChats = chatService.getActiveChats(userId);
        return ResponseEntity.ok(activeChats);
    }

    @GetMapping("/{partnerId}")
    public ResponseEntity<?> getChatHistory(@AuthenticationPrincipal Long userId, @PathVariable Long partnerId) {
        List<ChatMessage> history = chatService.getChatHistory(userId, partnerId, 50);
        return ResponseEntity.ok(history);
    }

    @PostMapping("/{partnerId}/read")
    public ResponseEntity<?> markMessagesAsRead(@AuthenticationPrincipal Long userId, @PathVariable Long partnerId) {
        chatService.markAsRead(userId, partnerId);
        return ResponseEntity.ok().build();
    }
}
