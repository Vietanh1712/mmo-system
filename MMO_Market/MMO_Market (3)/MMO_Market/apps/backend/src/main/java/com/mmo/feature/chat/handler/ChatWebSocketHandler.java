package com.mmo.feature.chat.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmo.feature.chat.service.ChatService;
import com.mmo.shared.model.ChatMessage;
import com.mmo.shared.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final Map<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ChatService chatService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        URI uri = session.getUri();
        if (uri != null) {
            String token = UriComponentsBuilder.fromUri(uri).build().getQueryParams().getFirst("token");
            if (token != null) {
                try {
                    Long userId = jwtTokenProvider.getUserIdFromToken(token);
                    session.getAttributes().put("userId", userId);
                    userSessions.put(userId, session);
                    return;
                } catch (Exception e) {
                    // Invalid token
                }
            }
        }
        session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Invalid or missing token"));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long senderId = (Long) session.getAttributes().get("userId");
        if (senderId == null) return;

        JsonNode jsonMessage = objectMapper.readTree(message.getPayload());
        
        Long recipientId = jsonMessage.has("recipientId") ? jsonMessage.get("recipientId").asLong() : null;
        String content = jsonMessage.has("content") ? jsonMessage.get("content").asText() : "";
        String attachmentUrl = jsonMessage.has("attachmentUrl") ? jsonMessage.get("attachmentUrl").asText() : null;

        if (recipientId == null || content.isEmpty()) {
            return; // Invalid message format
        }

        // Save to DB
        ChatMessage savedMsg = chatService.saveMessage(senderId, recipientId, content, attachmentUrl);

        // Forward to recipient if online
        WebSocketSession recipientSession = userSessions.get(recipientId);
        if (recipientSession != null && recipientSession.isOpen()) {
            String broadcastStr = objectMapper.writeValueAsString(savedMsg);
            recipientSession.sendMessage(new TextMessage(broadcastStr));
        }

        // Send confirmation back to sender
        String confirmationStr = objectMapper.writeValueAsString(savedMsg);
        session.sendMessage(new TextMessage(confirmationStr));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            userSessions.remove(userId);
        }
    }
}
