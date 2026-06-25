package service;

import dal.ComplaintRepository;
import dal.TransactionRepository;
import dal.UserRepository;
import model.Complaint;
import model.Transaction;
import model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ComplaintService {

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    /**
     * Tạo một khiếu nại mới liên quan tới giao dịch mua hàng.
     * Trạng thái giao dịch sẽ bị đóng băng (chuyển sang 'Disputed').
     */
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

    /**
     * Lấy danh sách khiếu nại của một khách hàng.
     */
    public List<Complaint> getCustomerComplaints(Long customerId) {
        User customer = userRepository.findByIdAndIsDeleteFalse(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Người mua không tồn tại."));
        return complaintRepository.findByCustomerAndIsDeleteFalseOrderByCreatedAtDesc(customer);
    }

    /**
     * Lấy chi tiết khiếu nại của khách hàng.
     */
    public Complaint getComplaintById(Long complaintId, Long customerId) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .filter(c -> c.getIsDelete() == null || !c.getIsDelete())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khiếu nại."));

        if (!complaint.getCustomer().getId().equals(customerId)) {
            throw new IllegalArgumentException("Bạn không có quyền xem khiếu nại này.");
        }

        return complaint;
    }

    /**
     * Lấy danh sách tất cả khiếu nại (dành cho Staff/Admin).
     */
    public List<Complaint> getAllComplaints() {
        return complaintRepository.findByIsDeleteFalseOrderByCreatedAtDesc();
    }

    /**
     * Lấy chi tiết khiếu nại không giới hạn chủ sở hữu (dành cho Staff/Admin).
     */
    public Complaint getComplaintByIdForStaff(Long complaintId) {
        return complaintRepository.findById(complaintId)
                .filter(c -> c.getIsDelete() == null || !c.getIsDelete())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khiếu nại."));
    }

    /**
     * Cập nhật trạng thái xử lý khiếu nại và giải quyết tiền (dành cho Staff/Admin).
     */
    @Transactional
    public Complaint updateComplaintStatus(Long complaintId, String status, String resolution) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .filter(c -> c.getIsDelete() == null || !c.getIsDelete())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khiếu nại."));

        complaint.setStatus(status);
        complaint.setResolution(resolution);

        Transaction tx = complaint.getTransaction();
        if (tx != null) {
            if ("Resolved".equalsIgnoreCase(status) || "Completed".equalsIgnoreCase(status)) {
                tx.setStatus("Refunded");
                transactionRepository.save(tx);

                // Hoàn trả tiền về ví cho Buyer
                User customer = tx.getCustomer();
                if (customer != null) {
                    customer.setBalanceVnd((customer.getBalanceVnd() != null ? customer.getBalanceVnd() : 0L) + tx.getAmountVnd());
                    userRepository.save(customer);
                }
            } else if ("Rejected".equalsIgnoreCase(status)) {
                tx.setStatus("Completed");
                transactionRepository.save(tx);

                // Giải ngân tiền về ví cho Seller
                User seller = tx.getSeller();
                if (seller != null) {
                    long payout = tx.getAmountVnd() - tx.getCommissionVnd();
                    seller.setBalanceVnd((seller.getBalanceVnd() != null ? seller.getBalanceVnd() : 0L) + payout);
                    userRepository.save(seller);
                }
            } else if ("InProgress".equalsIgnoreCase(status) || "In_Progress".equalsIgnoreCase(status)) {
                tx.setStatus("Disputed");
                transactionRepository.save(tx);
            }
        }

        return complaintRepository.save(complaint);
    }
}
