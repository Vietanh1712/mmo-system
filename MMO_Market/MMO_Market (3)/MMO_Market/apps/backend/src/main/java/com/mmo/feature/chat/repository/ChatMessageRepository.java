package com.mmo.feature.chat.repository;

import com.mmo.shared.model.ChatMessage;
import com.mmo.shared.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("SELECT m FROM ChatMessage m WHERE (m.sender = :user1 AND m.recipient = :user2) OR (m.sender = :user2 AND m.recipient = :user1) ORDER BY m.createdAt DESC")
    List<ChatMessage> findChatHistory(@Param("user1") User user1, @Param("user2") User user2, Pageable pageable);

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.recipient = :recipient AND m.isRead = false")
    long countByRecipientAndIsReadFalse(@Param("recipient") User recipient);

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.recipient = :recipient AND m.sender = :sender AND m.isRead = false")
    long countByRecipientAndSenderAndIsReadFalse(@Param("recipient") User recipient, @Param("sender") User sender);

    @Modifying
    @Query("UPDATE ChatMessage m SET m.isRead = true WHERE m.recipient = :recipient AND m.sender = :sender AND m.isRead = false")
    void markMessagesAsRead(@Param("recipient") User recipient, @Param("sender") User sender);

    @Query("SELECT m FROM ChatMessage m WHERE m.sender = :user OR m.recipient = :user ORDER BY m.createdAt DESC")
    List<ChatMessage> findAllMessagesByUser(@Param("user") User user);
}
