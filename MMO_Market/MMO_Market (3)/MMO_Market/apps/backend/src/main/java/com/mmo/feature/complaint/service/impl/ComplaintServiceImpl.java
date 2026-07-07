package com.mmo.feature.complaint.service.impl;
import com.mmo.feature.complaint.service.ComplaintService;

import com.mmo.shared.dto.ComplaintDTO;
import com.mmo.shared.dal.ComplaintRepository;
import jakarta.transaction.Transactional;
import com.mmo.shared.model.Complaint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import com.mmo.shared.dal.TransactionRepository;
import com.mmo.shared.dal.UserRepository;
import com.mmo.shared.model.Transaction;
import com.mmo.shared.model.User;
import com.mmo.feature.wallet.service.WalletService;
import com.mmo.shared.dal.NotificationRepository;
import com.mmo.shared.model.Notification;

@Service
@Slf4j
public class ComplaintServiceImpl implements ComplaintService {

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private WalletService walletService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private com.mmo.shared.dal.ChatRepository chatRepository;

    @Override
    public List<ComplaintDTO> getAllComplaints() {

        return complaintRepository
                .findAllByIsDeleteFalseOrderByCreatedAtDesc()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public long getTotalComplaints() {
        return complaintRepository.countByIsDeleteFalse();
    }

    @Override
    public long getInProgressComplaints() {
        return complaintRepository
                .countByStatusAndIsDeleteFalse("InProgress");
    }

    @Override
    public long getResolvedComplaints() {
        return complaintRepository
                .countByStatusAndIsDeleteFalse("Resolved");
    }

    @Override
    public long getRefusedComplaints() {
        return complaintRepository
                .countByStatusAndIsDeleteFalse("Rejected");
    }

    private ComplaintDTO toDTO(Complaint c) {

        return ComplaintDTO.builder()
                .id(c.getId())
                .customerName(c.getCustomer().getFullName())
                .customerEmail(c.getCustomer().getEmail())
                .sellerName(c.getSeller().getFullName())
                .description(c.getDescription())
                .status(c.getStatus())
                .createdAt(c.getCreatedAt())
                .build();
    }

    @Override
    public Page<ComplaintDTO> getComplaints(
            int page,
            String keyword,
            String status) {

        Pageable pageable = PageRequest.of(page, 4);

        Page<Complaint> complaints;

        boolean hasKeyword =
                keyword != null && !keyword.trim().isEmpty();

        boolean hasStatus =
                status != null &&
                        !status.trim().isEmpty() &&
                        !status.equals("ALL");

//        if (hasKeyword && hasStatus) {
//
//            complaints =
//                    complaintRepository
//                            .findByDescriptionContainingIgnoreCaseAndStatusAndIsDeleteFalse(
//                                    keyword,
//                                    status,
//                                    pageable);
//
//        } else if (hasKeyword) {
//
//            complaints =
//                    complaintRepository
//                            .findByDescriptionContainingIgnoreCaseAndIsDeleteFalse(
//                                    keyword,
//                                    pageable);
//
//        } else if (hasStatus) {
//
//            complaints =
//                    complaintRepository
//                            .findByStatusAndIsDeleteFalse(
//                                    status,
//                                    pageable);
//
//        } else {
//
//            complaints =
//                    complaintRepository
//                            .findByIsDeleteFalse(pageable);
//        }

        if (!hasKeyword) {
            keyword = null;
        }

        if (!hasStatus) {
            status = null;
        }

        complaints =
                complaintRepository.searchComplaints(
                        keyword,
                        status,
                        pageable
                );

        return complaints.map(this::toDTO);
    }

    @Override
    public List<String> getAllStatuses() {
        return complaintRepository.getAllStatuses();
    }

    @Override
    public ComplaintDTO getComplaintById(Long id) {

        Complaint complaint = complaintRepository
                .findByIdAndIsDeleteFalse(id)
                .orElseThrow(() ->
                        new RuntimeException("Complaint not found"));

        return ComplaintDTO.builder()
                .id(complaint.getId())
                .customerName(complaint.getCustomer().getFullName())
                .customerEmail(complaint.getCustomer().getEmail())
                .sellerName(complaint.getSeller().getFullName())
                .sellerEmail(complaint.getSeller().getEmail())
                .description(complaint.getDescription())
                .evidence(complaint.getEvidence())
                .resolution(complaint.getResolution())
                .status(complaint.getStatus())
                .createdAt(complaint.getCreatedAt())
                .amountVnd(
                        complaint.getTransaction() != null
                                ? complaint.getTransaction().getAmountVnd()
                                : null
                )
                .commissionVnd(
                        complaint.getTransaction() != null
                                ? complaint.getTransaction().getCommissionVnd()
                                : null
                )
                .transactionId(
                        complaint.getTransaction() != null
                                ? complaint.getTransaction().getId()
                                : null)
                .build();


    }

    @Override
    @Transactional
    public void processComplaint(
            Long complaintId,
            String status,
            String resolution) {

        Complaint complaint = complaintRepository
                .findByIdAndIsDeleteFalse(complaintId)
                .orElseThrow(() ->
                        new RuntimeException("Complaint not found"));

        complaint.setStatus(status);
        complaint.setResolution(resolution);

        complaintRepository.save(complaint);
    }
    
    // --- Methods from HEAD ---
    @Override
    @Transactional
    public Complaint createComplaint(Long customerId, Long transactionId, String description, String evidence) {
        User customer = userRepository.findByIdAndIsDeleteFalse(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Người mua không tồn tại."));

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Giao dịch không tồn tại."));

        if (transaction.getIsDelete() != null && transaction.getIsDelete()) {
            throw new IllegalArgumentException("Giao dịch không tồn tại.");
        }

        // Kiểm tra quyền sở hữu
        if (!transaction.getCustomer().getId().equals(customerId)) {
            throw new IllegalArgumentException("Bạn không có quyền khiếu nại giao dịch này.");
        }

        // Kiểm tra trạng thái giao dịch
        String status = transaction.getStatus();
        if ("Disputed".equalsIgnoreCase(status)) {
            throw new IllegalArgumentException("Giao dịch này đã được khiếu nại trước đó.");
        }
        if ("Cancelled".equalsIgnoreCase(status) || "Refunded".equalsIgnoreCase(status)) {
            throw new IllegalArgumentException("Không thể khiếu nại giao dịch đã bị hủy hoặc hoàn tiền.");
        }

        // Kiểm tra xem khách hàng và người bán đã chat với nhau chưa
        List<com.mmo.shared.model.Chat> chats = chatRepository.findActiveChatsBetweenUsers(customer, transaction.getSeller());
        if (chats == null || chats.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhắn tin trao đổi với người bán trước khi tạo khiếu nại. Chỉ tạo khiếu nại khi hai bên không thể tự giải quyết.");
        }

        // Bắt buộc có lý do và bằng chứng
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng cung cấp lý do khiếu nại.");
        }
        if (evidence == null || evidence.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng cung cấp bằng chứng (hình ảnh/video) khiếu nại.");
        }

        // Đóng băng tiền/giao dịch: Cập nhật trạng thái giao dịch thành 'Disputed'
        transaction.setStatus("Disputed");
        transactionRepository.save(transaction);

        // Tạo khiếu nại mới
        Complaint complaint = Complaint.builder()
                .transaction(transaction)
                .customer(customer)
                .seller(transaction.getSeller())
                .description(description)
                .evidence(evidence)
                .status("Open")
                .isDelete(false)
                .createdAt(LocalDateTime.now())
                .build();

        return complaintRepository.save(complaint);
    }

    @Override
    public List<Complaint> getCustomerComplaints(Long customerId) {
        User customer = userRepository.findByIdAndIsDeleteFalse(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Người mua không tồn tại."));
        return complaintRepository.findByCustomerAndIsDeleteFalseOrderByCreatedAtDesc(customer);
    }

    @Override
    public Complaint getComplaintById(Long complaintId, Long customerId) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .filter(c -> c.getIsDelete() == null || !c.getIsDelete())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khiếu nại."));

        if (!complaint.getCustomer().getId().equals(customerId)) {
            throw new IllegalArgumentException("Bạn không có quyền xem khiếu nại này.");
        }

        return complaint;
    }

    @Override
    public List<Complaint> getAllComplaintsForStaff() {
        return complaintRepository.findAllByIsDeleteFalseOrderByCreatedAtDesc();
    }

    @Override
    public Complaint getComplaintByIdForStaff(Long complaintId) {
        return complaintRepository.findById(complaintId)
                .filter(c -> c.getIsDelete() == null || !c.getIsDelete())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khiếu nại."));
    }

    @Override
    public List<com.mmo.shared.model.Chat> getComplaintChatHistory(Long complaintId, Long staffId) {
        Complaint complaint = getComplaintByIdForStaff(complaintId);
        if (complaint.getTransaction() == null) {
            return java.util.Collections.emptyList();
        }
        User customer = complaint.getCustomer();
        User seller = complaint.getSeller();
        if (customer == null || seller == null) {
            return java.util.Collections.emptyList();
        }
        // Staff views full chat history without deleted filters
        return chatRepository.findNormalChatsBetween(customer, seller);
    }

    @Override
    @Transactional
    public Complaint updateComplaintStatus(Long complaintId, String status, String resolution) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .filter(c -> c.getIsDelete() == null || !c.getIsDelete())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khiếu nại."));

        complaint.setStatus(status);
        complaint.setResolution(resolution);
        complaint.setResolvedBy(null); // Có thể được set từ controller nếu cần
        complaint.setResolvedAt(LocalDateTime.now());

        Transaction tx = complaint.getTransaction();
        if (tx != null) {
            if ("Resolved".equalsIgnoreCase(status) || "Completed".equalsIgnoreCase(status)) {
                tx.setStatus("Refunded");
                transactionRepository.save(tx);

                // Hoàn trả tiền về ví cho Buyer
                User customer = tx.getCustomer();
                if (customer != null) {
                    long oldBalance = (customer.getBalanceVnd() != null ? customer.getBalanceVnd() : 0L);
                    long newBalance = oldBalance + tx.getAmountVnd();
                    customer.setBalanceVnd(newBalance);
                    userRepository.save(customer);

                    // Ghi lại giao dịch hoàn tiền vào lịch sử ví
                    String referenceCode = String.format("REFUND-CMP-%d-TX-%d", complaint.getId(), tx.getId());
                    walletService.recordTransaction(
                            customer,
                            "REFUND",
                            tx.getAmountVnd(),
                            "SUCCESS",
                            String.format("Hoàn tiền khiếu nại #CMP-%d: %s", complaint.getId(), resolution),
                            referenceCode,
                            newBalance,
                            complaint.getId()
                    );

                    // Gửi thông báo hoàn tiền cho Buyer
                    Notification buyerNotif = Notification.builder()
                            .userId(customer.getId())
                            .title("Thông báo hoàn tiền khiếu nại")
                            .content(String.format("Khiếu nại #CMP-%d đối với đơn hàng MMO-ORD-%d đã được chấp nhận. Số tiền %,d VNĐ đã được hoàn trả vào ví của bạn.", 
                                    complaint.getId(), tx.getId(), tx.getAmountVnd()))
                            .type("WALLET")
                            .severity("SUCCESS")
                            .isRead(false)
                            .isDelete(false)
                            .targetUrl("/account/transactions")
                            .build();
                    notificationRepository.save(buyerNotif);

                    // Gửi thông báo cho Seller
                    User seller = tx.getSeller();
                    if (seller != null) {
                        Notification sellerNotif = Notification.builder()
                                .userId(seller.getId())
                                .title("Đơn hàng bị hoàn tiền (Khiếu nại được chấp nhận)")
                                .content(String.format("Khiếu nại #CMP-%d đối với đơn hàng MMO-ORD-%d đã được giải quyết. Đơn hàng đã bị hoàn tiền cho người mua.", 
                                        complaint.getId(), tx.getId()))
                                .type("WALLET")
                                .severity("WARNING")
                                .isRead(false)
                                .isDelete(false)
                                .targetUrl("/account/transactions")
                                .build();
                        notificationRepository.save(sellerNotif);
                    }

                    log.info("Ghi lại giao dịch hoàn tiền REFUND cho Customer ID {} từ khiếu nại #CMP-{}. Số tiền: {} VNĐ. Số dư mới: {} VNĐ",
                            customer.getId(), complaint.getId(), tx.getAmountVnd(), newBalance);
                }
            } else if ("Rejected".equalsIgnoreCase(status)) {
                tx.setStatus("Completed");
                transactionRepository.save(tx);

                // Giải ngân tiền về ví cho Seller
                User seller = tx.getSeller();
                if (seller != null) {
                    long payout = tx.getAmountVnd() - tx.getCommissionVnd();
                    long oldBalance = (seller.getBalanceVnd() != null ? seller.getBalanceVnd() : 0L);
                    long newBalance = oldBalance + payout;
                    seller.setBalanceVnd(newBalance);
                    userRepository.save(seller);

                    // Ghi lại giao dịch giải ngân vào lịch sử ví
                    String referenceCode = String.format("PAYOUT-CMP-REJECTED-%d-TX-%d", complaint.getId(), tx.getId());
                    walletService.recordTransaction(
                            seller,
                            "PAYMENT",
                            payout,
                            "SUCCESS",
                            String.format("Giải ngân từ khiếu nại bị từ chối #CMP-%d: %s", complaint.getId(), resolution),
                            referenceCode,
                            newBalance,
                            complaint.getId()
                    );

                    // Gửi thông báo giải ngân cho Seller
                    Notification sellerNotif = Notification.builder()
                            .userId(seller.getId())
                            .title("Thông báo giải ngân đơn hàng")
                            .content(String.format("Khiếu nại #CMP-%d đối với đơn hàng MMO-ORD-%d đã bị từ chối. Số tiền %,d VNĐ đã được giải ngân vào ví của bạn.", 
                                    complaint.getId(), tx.getId(), payout))
                            .type("WALLET")
                            .severity("SUCCESS")
                            .isRead(false)
                            .isDelete(false)
                            .targetUrl("/account/transactions")
                            .build();
                    notificationRepository.save(sellerNotif);

                    // Gửi thông báo cho Buyer
                    User customer = tx.getCustomer();
                    if (customer != null) {
                        Notification customerNotif = Notification.builder()
                                .userId(customer.getId())
                                .title("Khiếu nại đơn hàng bị từ chối")
                                .content(String.format("Khiếu nại #CMP-%d đối với đơn hàng MMO-ORD-%d đã bị từ chối. Lý do: %s", 
                                        complaint.getId(), tx.getId(), resolution))
                                .type("WALLET")
                                .severity("DANGER")
                                .isRead(false)
                                .isDelete(false)
                                .targetUrl("/account/transactions")
                                .build();
                        notificationRepository.save(customerNotif);
                    }

                    log.info("Ghi lại giao dịch giải ngân PAYMENT cho Seller ID {} từ khiếu nại #CMP-{} bị từ chối. Số tiền: {} VNĐ. Số dư mới: {} VNĐ",
                            seller.getId(), complaint.getId(), payout, newBalance);
                }
            } else if ("InProgress".equalsIgnoreCase(status) || "In_Progress".equalsIgnoreCase(status)) {
                tx.setStatus("Disputed");
                transactionRepository.save(tx);
            }
        }

        return complaintRepository.save(complaint);
    }
}