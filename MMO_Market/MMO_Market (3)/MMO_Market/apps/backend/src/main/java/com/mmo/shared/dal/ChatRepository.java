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
import java.time.LocalDateTime;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {
    List<Chat> findByComplaintAndIsDeleteFalseOrderByCreatedAtAsc(Complaint complaint);

    @Query(value = """
            WITH MessageSequence AS (
                SELECT
                    sender_id, receiver_id, created_at,
                    LEAD(sender_id) OVER (PARTITION BY CASE WHEN sender_id = :sellerId THEN receiver_id ELSE sender_id END ORDER BY created_at) AS next_sender_id,
                    LEAD(created_at) OVER (PARTITION BY CASE WHEN sender_id = :sellerId THEN receiver_id ELSE sender_id END ORDER BY created_at) AS next_created_at
                FROM Chats
                WHERE (sender_id = :sellerId OR receiver_id = :sellerId)
                  AND (chat_type = 'Normal' OR chat_type IS NULL)
                  AND complaint_id IS NULL
                  AND isDelete = 0
                  AND created_at >= :since
            )
            SELECT AVG(CAST(DATEDIFF(MINUTE, created_at, next_created_at) AS FLOAT))
            FROM MessageSequence
            WHERE receiver_id = :sellerId
              AND next_sender_id = :sellerId
            """, nativeQuery = true)
    Double findAverageResponseTimeInMinutes(@Param("sellerId") Long sellerId, @Param("since") LocalDateTime since);

    @Query("SELECT c FROM Chat c WHERE c.isDelete = false AND (" +
           "(c.sender = :user1 AND c.receiver = :user2 AND c.senderDeleted = false) OR " +
           "(c.sender = :user2 AND c.receiver = :user1 AND c.receiverDeleted = false)" +
           ") AND (c.chatType = 'Normal' OR c.chatType IS NULL) AND c.complaint IS NULL ORDER BY c.createdAt ASC")
    List<Chat> findActiveChatsBetweenUsers(@Param("user1") User user1, @Param("user2") User user2);

    @Query("SELECT c FROM Chat c WHERE c.isDelete = false AND (" +
           "(c.sender = :user1 AND c.receiver = :user2 AND c.senderDeleted = false) OR " +
           "(c.sender = :user2 AND c.receiver = :user1 AND c.receiverDeleted = false)" +
           ") AND (c.chatType = 'Normal' OR c.chatType IS NULL) AND c.complaint IS NULL AND LOWER(c.message) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY c.createdAt ASC")
    List<Chat> searchActiveChatsBetweenUsers(
            @Param("user1") User user1,
            @Param("user2") User user2,
            @Param("keyword") String keyword
    );

    @Query("SELECT c FROM Chat c WHERE ((c.sender = :user1 AND c.receiver = :user2) OR (c.sender = :user2 AND c.receiver = :user1)) AND c.chatType = 'Normal' AND c.complaint IS NULL AND c.isDelete = false ORDER BY c.createdAt ASC")
    List<Chat> findNormalChatsBetween(@Param("user1") User user1, @Param("user2") User user2);

    @Query("SELECT DISTINCT c FROM Chat c " +
           "JOIN FETCH c.sender s " +
           "JOIN FETCH c.receiver r " +
           "WHERE (c.isDelete IS NULL OR c.isDelete = false) AND " +
           "((c.sender = :user) OR (c.receiver = :user)) AND " +
           "(c.chatType = 'Normal' OR c.chatType IS NULL) AND " +
           "c.complaint IS NULL " +
           "ORDER BY c.createdAt DESC")
    List<Chat> findAllChatsForContactList(@Param("user") User user);

    @Query("SELECT DISTINCT c FROM Chat c " +
           "JOIN FETCH c.sender s " +
           "JOIN FETCH c.receiver r " +
           "WHERE (c.isDelete IS NULL OR c.isDelete = false) AND " +
           "((c.sender = :user AND (c.senderDeleted IS NULL OR c.senderDeleted = false)) OR " +
           " (c.receiver = :user AND (c.receiverDeleted IS NULL OR c.receiverDeleted = false))) AND " +
           "(c.chatType = 'Normal' OR c.chatType IS NULL) AND " +
           "c.complaint IS NULL " +
           "ORDER BY c.createdAt DESC")
    List<Chat> findRecentNormalChatsForUser(@Param("user") User user);

    @Query("SELECT COUNT(c) FROM Chat c WHERE " +
           "c.sender = :sender AND c.receiver = :receiver AND " +
           "(c.isRead IS NULL OR c.isRead = false) AND " +
           "(c.isDelete IS NULL OR c.isDelete = false) AND " +
           "(c.receiverDeleted IS NULL OR c.receiverDeleted = false) AND " +
           "(c.chatType = 'Normal' OR c.chatType IS NULL) AND " +
           "c.complaint IS NULL")
    long countUnreadFrom(@Param("sender") User sender, @Param("receiver") User receiver);

    @Query("SELECT COUNT(DISTINCT c.sender) FROM Chat c WHERE " +
           "c.receiver = :receiver AND " +
           "(c.isRead IS NULL OR c.isRead = false) AND " +
           "(c.isDelete IS NULL OR c.isDelete = false) AND " +
           "(c.receiverDeleted IS NULL OR c.receiverDeleted = false)")
    long countUnreadRoomsForReceiver(@Param("receiver") User receiver);

    @Modifying
    @Transactional
    @Query("UPDATE Chat c SET c.isRead = true WHERE " +
           "c.sender = :sender AND c.receiver = :receiver AND " +
           "(c.isRead IS NULL OR c.isRead = false) AND " +
           "(c.chatType = 'Normal' OR c.chatType IS NULL) AND " +
           "c.complaint IS NULL")
    void markAllReadFrom(@Param("sender") User sender, @Param("receiver") User receiver);
}
