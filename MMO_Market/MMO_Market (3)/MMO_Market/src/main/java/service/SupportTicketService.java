package service;

import dal.SupportTicketRepository;
import dal.UserRepository;
import model.SupportTicket;
import model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SupportTicketService {

    @Autowired
    private SupportTicketRepository supportTicketRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public SupportTicket createTicket(Long userId, String category, String title, String description) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng với ID: " + userId));

        SupportTicket ticket = SupportTicket.builder()
                .user(user)
                .category(category)
                .title(title)
                .description(description)
                .status("Open")
                .isDelete(false)
                .build();

        return supportTicketRepository.save(ticket);
    }

    public List<SupportTicket> getUserTickets(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng với ID: " + userId));
        return supportTicketRepository.findByUserAndIsDeleteFalseOrderByCreatedAtDesc(user);
    }

    public List<SupportTicket> getAllTickets() {
        return supportTicketRepository.findByIsDeleteFalseOrderByCreatedAtDesc();
    }

    public SupportTicket getTicketById(Long id) {
        return supportTicketRepository.findById(id)
                .filter(t -> !t.getIsDelete())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ticket hỗ trợ với ID: " + id));
    }

    @Transactional
    public SupportTicket updateTicketStatus(Long id, String status, String resolution) {
        SupportTicket ticket = getTicketById(id);
        
        // Chấp nhận trạng thái: Open, Processing, Resolved, Closed, Replied
        if (!status.equals("Open") && !status.equals("Processing") && !status.equals("Resolved") && !status.equals("Closed") && !status.equals("Replied")) {
            throw new IllegalArgumentException("Trạng thái ticket không hợp lệ.");
        }
        
        if (resolution != null && !resolution.trim().isEmpty()) {
            if (!status.equals("Resolved") && !status.equals("Closed")) {
                status = "Replied";
            }
        }
        
        ticket.setStatus(status);
        ticket.setResolution(resolution);
        
        return supportTicketRepository.save(ticket);
    }
}
