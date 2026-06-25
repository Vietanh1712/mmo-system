package dal;

import model.SupportTicket;
import model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    List<SupportTicket> findByUserAndIsDeleteFalseOrderByCreatedAtDesc(User user);
    List<SupportTicket> findByIsDeleteFalseOrderByCreatedAtDesc();
    List<SupportTicket> findByStatusAndIsDeleteFalseOrderByCreatedAtDesc(String status);
}
