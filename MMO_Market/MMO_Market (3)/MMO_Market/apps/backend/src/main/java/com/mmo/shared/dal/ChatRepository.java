package com.mmo.shared.dal;

import com.mmo.shared.model.Chat;
import com.mmo.shared.model.Complaint;
import com.mmo.shared.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {
    List<Chat> findByComplaintAndIsDeleteFalseOrderByCreatedAtAsc(Complaint complaint);

    @Query("SELECT c FROM Chat c WHERE c.isDelete = false AND (" +
           "(c.sender = :user1 AND c.receiver = :user2 AND c.senderDeleted = false) OR " +
           "(c.sender = :user2 AND c.receiver = :user1 AND c.receiverDeleted = false)" +
           ") AND (c.chatType = 'Normal' OR c.chatType IS NULL) ORDER BY c.createdAt ASC")
    List<Chat> findActiveChatsBetweenUsers(@Param("user1") User user1, @Param("user2") User user2);

    @Query("SELECT c FROM Chat c WHERE c.isDelete = false AND (" +
           "(c.sender = :user1 AND c.receiver = :user2 AND c.senderDeleted = false) OR " +
           "(c.sender = :user2 AND c.receiver = :user1 AND c.receiverDeleted = false)" +
           ") AND (c.chatType = 'Normal' OR c.chatType IS NULL) AND LOWER(c.message) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY c.createdAt ASC")
    List<Chat> searchActiveChatsBetweenUsers(
            @Param("user1") User user1,
            @Param("user2") User user2,
            @Param("keyword") String keyword
    );

    @Query("SELECT c FROM Chat c WHERE ((c.sender = :user1 AND c.receiver = :user2) OR (c.sender = :user2 AND c.receiver = :user1)) AND c.chatType = 'Normal' AND c.isDelete = false ORDER BY c.createdAt ASC")
    List<Chat> findNormalChatsBetween(@Param("user1") User user1, @Param("user2") User user2);

    @Query("SELECT DISTINCT c FROM Chat c " +
           "JOIN FETCH c.sender s " +
           "JOIN FETCH c.receiver r " +
           "WHERE (c.isDelete IS NULL OR c.isDelete = false) AND " +
           "((c.sender = :user) OR (c.receiver = :user)) AND " +
           "(c.chatType = 'Normal' OR c.chatType IS NULL) " +
           "ORDER BY c.createdAt DESC")
    List<Chat> findAllChatsForContactList(@Param("user") User user);

    @Query("SELECT DISTINCT c FROM Chat c " +
           "JOIN FETCH c.sender s " +
           "JOIN FETCH c.receiver r " +
           "WHERE (c.isDelete IS NULL OR c.isDelete = false) AND " +
           "((c.sender = :user AND (c.senderDeleted IS NULL OR c.senderDeleted = false)) OR " +
           " (c.receiver = :user AND (c.receiverDeleted IS NULL OR c.receiverDeleted = false))) AND " +
           "(c.chatType = 'Normal' OR c.chatType IS NULL) " +
           "ORDER BY c.createdAt DESC")
    List<Chat> findRecentNormalChatsForUser(@Param("user") User user);

    @Query("SELECT COUNT(c) FROM Chat c WHERE " +
           "c.sender = :sender AND c.receiver = :receiver AND " +
           "(c.isRead IS NULL OR c.isRead = false) AND " +
           "(c.isDelete IS NULL OR c.isDelete = false) AND " +
           "(c.receiverDeleted IS NULL OR c.receiverDeleted = false) AND " +
           "(c.chatType = 'Normal' OR c.chatType IS NULL)")
    long countUnreadFrom(@Param("sender") User sender, @Param("receiver") User receiver);

    @Modifying
    @Transactional
    @Query("UPDATE Chat c SET c.isRead = true WHERE " +
           "c.sender = :sender AND c.receiver = :receiver AND " +
           "(c.isRead IS NULL OR c.isRead = false) AND " +
           "(c.chatType = 'Normal' OR c.chatType IS NULL)")
    void markAllReadFrom(@Param("sender") User sender, @Param("receiver") User receiver);
}
