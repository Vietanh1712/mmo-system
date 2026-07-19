package com.mmo.feature.chat.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmo.shared.dal.ChatRepository;
import com.mmo.shared.dal.UserRepository;
import com.mmo.shared.dal.ComplaintRepository;
import com.mmo.shared.model.Chat;
import com.mmo.shared.model.User;
import com.mmo.shared.model.Complaint;
import com.mmo.shared.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final Map<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ComplaintRepository complaintRepository;

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

        if (recipientId == null || content.isEmpty()) {
            return; // Invalid message format
        }

        Chat savedChat = null;

        // If recipientId < 0, it's a complaint chat
        if (recipientId < 0) {
            Long complaintId = -recipientId;
            Optional<Complaint> compOpt = complaintRepository.findById(complaintId);
            if (compOpt.isEmpty()) {
                return;
            }
            Complaint complaint = compOpt.get();
            if (!complaint.getCustomer().getId().equals(senderId) && !complaint.getSeller().getId().equals(senderId)) {
                return; // Access denied
            }

            if (!"In_Progress".equalsIgnoreCase(complaint.getStatus()) && !"InProgress".equalsIgnoreCase(complaint.getStatus())) {
                return; // Complaint chat closed
            }

            User sender = userRepository.findByIdAndIsDeleteFalse(senderId)
                    .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại."));
            User receiver = complaint.getCustomer().getId().equals(senderId) ? complaint.getSeller() : complaint.getCustomer();

            Chat chat = new Chat();
            chat.setComplaint(complaint);
            chat.setSender(sender);
            chat.setReceiver(receiver);
            chat.setChatType("Complaint");
            chat.setMessage(content.trim());
            chat.setIsDelete(false);
            chat.setSenderDeleted(false);
            chat.setReceiverDeleted(false);
            chat.setIsRead(false);
            chat.setCreatedAt(LocalDateTime.now());

            savedChat = chatRepository.save(chat);
        } else {
            // Normal chat
            if (senderId.equals(recipientId)) {
                return; // Cannot send message to yourself
            }

            Optional<User> senderOpt = userRepository.findById(senderId);
            Optional<User> receiverOpt = userRepository.findById(recipientId);
            if (senderOpt.isEmpty() || receiverOpt.isEmpty()) {
                return;
            }

            User sender = senderOpt.get();
            User receiver = receiverOpt.get();

            Long productId = null;
            if (jsonMessage.has("productId") && !jsonMessage.get("productId").isNull() && !jsonMessage.get("productId").asText().isBlank()) {
                try {
                    productId = jsonMessage.get("productId").asLong();
                } catch (Exception ignored) {}
            }

            Chat chat = Chat.builder()
                    .sender(sender)
                    .receiver(receiver)
                    .message(content.trim())
                    .chatType("Normal")
                    .isDelete(false)
                    .senderDeleted(false)
                    .receiverDeleted(false)
                    .productId(productId)
                    .build();
            chat.setCreatedAt(LocalDateTime.now());
            chat.setIsRead(false);

            savedChat = chatRepository.save(chat);
        }

        // Build broadcast message payload
        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("id", savedChat.getId());
        payloadMap.put("senderId", senderId);
        payloadMap.put("receiverId", recipientId);
        payloadMap.put("message", savedChat.getMessage());
        payloadMap.put("createdAt", savedChat.getCreatedAt().toString());
        payloadMap.put("chatType", savedChat.getChatType());
        if (savedChat.getComplaint() != null) {
            payloadMap.put("complaintId", savedChat.getComplaint().getId());
        }
        if (savedChat.getProductId() != null) {
            payloadMap.put("productId", savedChat.getProductId());
        }

        String broadcastStr = objectMapper.writeValueAsString(payloadMap);

        // Forward to recipient if online
        Long receiverUserId = (recipientId < 0)
                ? (savedChat.getReceiver().getId())
                : recipientId;

        WebSocketSession recipientSession = userSessions.get(receiverUserId);
        if (recipientSession != null && recipientSession.isOpen()) {
            recipientSession.sendMessage(new TextMessage(broadcastStr));
        }

        // Send confirmation back to sender
        session.sendMessage(new TextMessage(broadcastStr));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            userSessions.remove(userId);
        }
    }
}
