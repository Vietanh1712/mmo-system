package com.mmo.shared.dal;

import com.mmo.shared.model.Chat;
import com.mmo.shared.model.Complaint;
import com.mmo.shared.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {
    List<Chat> findByComplaintAndIsDeleteFalseOrderByCreatedAtAsc(Complaint complaint);

    @Query("SELECT c FROM Chat c WHERE c.isDelete = false AND (" +
           "(c.sender = :user1 AND c.receiver = :user2 AND c.senderDeleted = false) OR " +
           "(c.sender = :user2 AND c.receiver = :user1 AND c.receiverDeleted = false)" +
           ") ORDER BY c.createdAt ASC")
    List<Chat> findActiveChatsBetweenUsers(@Param("user1") User user1, @Param("user2") User user2);

    @Query("SELECT c FROM Chat c WHERE c.isDelete = false AND (" +
           "(c.sender = :user1 AND c.receiver = :user2 AND c.senderDeleted = false) OR " +
           "(c.sender = :user2 AND c.receiver = :user1 AND c.receiverDeleted = false)" +
           ") AND LOWER(c.message) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY c.createdAt ASC")
    List<Chat> searchActiveChatsBetweenUsers(
            @Param("user1") User user1, 
            @Param("user2") User user2, 
            @Param("keyword") String keyword
    );

    @org.springframework.data.jpa.repository.Query("SELECT c FROM Chat c WHERE ((c.sender = :user1 AND c.receiver = :user2) OR (c.sender = :user2 AND c.receiver = :user1)) AND c.chatType = 'Normal' AND c.isDelete = false ORDER BY c.createdAt ASC")
    List<Chat> findNormalChatsBetween(@org.springframework.data.repository.query.Param("user1") User user1, @org.springframework.data.repository.query.Param("user2") User user2);    
    
    @org.springframework.data.jpa.repository.Query("SELECT c FROM Chat c WHERE (c.sender = :user AND c.senderDeleted = false) OR (c.receiver = :user AND c.receiverDeleted = false) ORDER BY c.createdAt DESC")
    List<Chat> findRecentNormalChatsForUser(@org.springframework.data.repository.query.Param("user") User user);
}
