package com.mmo.feature.support.service;
import com.mmo.shared.model.Transaction;
import com.mmo.shared.model.Category;

import com.mmo.shared.dal.SupportTicketRepository;
import com.mmo.shared.dal.UserRepository;
import com.mmo.shared.dal.NotificationRepository;
import com.mmo.shared.model.SupportTicket;
import com.mmo.shared.model.User;
import com.mmo.shared.model.Notification;
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

    @Autowired
    private NotificationRepository notificationRepository;

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

        SupportTicket savedTicket = supportTicketRepository.save(ticket);

        // 1. Tạo thông báo xác nhận gửi ticket thành công cho Customer/Seller
        Notification customerNotif = Notification.builder()
                .userId(user.getId())
                .title("Yêu cầu hỗ trợ đã được gửi thành công")
                .content(String.format("Yêu cầu hỗ trợ #TKT-%d về \"%s\" của bạn đã được gửi thành công và đang chờ xử lý.", savedTicket.getId(), savedTicket.getTitle()))
                .type("SYSTEM")
                .severity("INFO")
                .isRead(false)
                .isDelete(false)
                .targetUrl("/account/tickets")
                .build();
        notificationRepository.save(customerNotif);

        // 2. Tạo thông báo cho toàn bộ Staff và Admin để vào xử lý
        List<User> staffAndAdmins = userRepository.findStaffAndAdmins();
        for (User staff : staffAndAdmins) {
            if (staff.getId().equals(user.getId())) {
                continue;
            }
            Notification staffNotif = Notification.builder()
                    .userId(staff.getId())
                    .title("Có yêu cầu hỗ trợ mới")
                    .content(String.format("Yêu cầu hỗ trợ mới #TKT-%d: \"%s\" từ %s (%s) cần xử lý.", savedTicket.getId(), savedTicket.getTitle(), user.getFullName(), user.getEmail()))
                    .type("SYSTEM")
                    .severity("WARNING")
                    .isRead(false)
                    .isDelete(false)
                    .targetUrl("/staff/support-tickets/detail?id=" + savedTicket.getId())
                    .build();
            notificationRepository.save(staffNotif);
        }

        return savedTicket;
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
        
        // Chấp nhận trạng thái rút gọn: Open, Processing, Resolved
        if (!status.equals("Open") && !status.equals("Processing") && !status.equals("Resolved")) {
            throw new IllegalArgumentException("Trạng thái ticket không hợp lệ.");
        }
        
        if (resolution != null && !resolution.trim().isEmpty()) {
            if (!status.equals("Resolved")) {
                status = "Processing";
            }
        }
        
        ticket.setStatus(status);
        ticket.setResolution(resolution);
        
        SupportTicket savedTicket = supportTicketRepository.save(ticket);

        // Tạo thông báo cập nhật cho người gửi (Customer/Seller)
        String statusLabel = "Đang xử lý";
        String severity = "INFO";
        if (status.equals("Resolved")) {
            statusLabel = "Đã giải quyết";
            severity = "SUCCESS";
        }
        
        Notification updateNotif = Notification.builder()
                .userId(savedTicket.getUser().getId())
                .title("Cập nhật trạng thái yêu cầu hỗ trợ")
                .content(String.format("Yêu cầu hỗ trợ #TKT-%d về \"%s\" đã được cập nhật trạng thái: %s.", savedTicket.getId(), savedTicket.getTitle(), statusLabel))
                .type("SYSTEM")
                .severity(severity)
                .isRead(false)
                .isDelete(false)
                .targetUrl("/account/tickets")
                .build();
        notificationRepository.save(updateNotif);

        return savedTicket;
    }
}
