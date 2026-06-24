package dal;

import model.Chat;
import model.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {
    List<Chat> findByComplaintAndIsDeleteFalseOrderByCreatedAtAsc(Complaint complaint);

    @org.springframework.data.jpa.repository.Query("SELECT c FROM Chat c WHERE ((c.sender = :user1 AND c.receiver = :user2) OR (c.sender = :user2 AND c.receiver = :user1)) AND c.chatType = 'Normal' AND c.isDelete = false ORDER BY c.createdAt ASC")
    List<Chat> findNormalChatsBetween(@org.springframework.data.repository.query.Param("user1") model.User user1, @org.springframework.data.repository.query.Param("user2") model.User user2);

    @org.springframework.data.jpa.repository.Query("SELECT c FROM Chat c WHERE (c.sender = :user OR c.receiver = :user) AND c.chatType = 'Normal' AND c.isDelete = false ORDER BY c.createdAt DESC")
    List<Chat> findRecentNormalChatsForUser(@org.springframework.data.repository.query.Param("user") model.User user);
}
