package com.mmo.feature.chat.service;

import com.mmo.feature.chat.repository.ChatMessageRepository;
import com.mmo.shared.model.ChatMessage;
import com.mmo.shared.model.User;
import com.mmo.shared.dal.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ChatService {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private UserRepository userRepository;

    public ChatMessage saveMessage(Long senderId, Long recipientId, String content, String attachmentUrl) {
        User sender = userRepository.findById(senderId).orElseThrow(() -> new RuntimeException("Sender not found"));
        User recipient = userRepository.findById(recipientId).orElseThrow(() -> new RuntimeException("Recipient not found"));

        ChatMessage message = ChatMessage.builder()
                .sender(sender)
                .recipient(recipient)
                .content(content)
                .attachmentUrl(attachmentUrl)
                .isRead(false)
                .build();

        return chatMessageRepository.save(message);
    }

    public List<ChatMessage> getChatHistory(Long user1Id, Long user2Id, int limit) {
        User user1 = userRepository.findById(user1Id).orElseThrow();
        User user2 = userRepository.findById(user2Id).orElseThrow();
        
        List<ChatMessage> history = chatMessageRepository.findChatHistory(user1, user2, PageRequest.of(0, limit));
        Collections.reverse(history); // Return oldest to newest for UI
        return history;
    }

    @Transactional
    public void markAsRead(Long currentUserId, Long partnerId) {
        User currentUser = userRepository.findById(currentUserId).orElseThrow();
        User partner = userRepository.findById(partnerId).orElseThrow();
        chatMessageRepository.markMessagesAsRead(currentUser, partner);
    }

    public List<Map<String, Object>> getActiveChats(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        List<ChatMessage> allMessages = chatMessageRepository.findAllMessagesByUser(user);
        
        // Group by partner
        Map<Long, Map<String, Object>> chatMap = new LinkedHashMap<>();
        
        for (ChatMessage msg : allMessages) {
            User partner = msg.getSender().getId().equals(userId) ? msg.getRecipient() : msg.getSender();
            
            if (!chatMap.containsKey(partner.getId())) {
                Map<String, Object> chatInfo = new HashMap<>();
                chatInfo.put("contactId", partner.getId());
                chatInfo.put("contactName", partner.getFullName());
                chatInfo.put("contactAvatar", "/images/default-avatar.png");
                chatInfo.put("lastMessage", msg.getContent());
                chatInfo.put("lastMessageTime", msg.getCreatedAt().toString());
                
                // Count unread from this partner
                long unreadCount = chatMessageRepository.countByRecipientAndSenderAndIsReadFalse(user, partner);
                chatInfo.put("unreadCount", unreadCount);
                
                chatMap.put(partner.getId(), chatInfo);
            }
        }
        
        return new ArrayList<>(chatMap.values());
    }
}
