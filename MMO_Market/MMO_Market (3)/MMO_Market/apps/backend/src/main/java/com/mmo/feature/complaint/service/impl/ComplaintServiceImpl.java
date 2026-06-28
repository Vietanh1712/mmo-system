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

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import com.mmo.shared.dal.TransactionRepository;
import com.mmo.shared.dal.UserRepository;
import com.mmo.shared.model.Transaction;
import com.mmo.shared.model.User;

@Service
public class ComplaintServiceImpl implements ComplaintService {

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

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
                .countByStatusAndIsDeleteFalse("refuse");
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