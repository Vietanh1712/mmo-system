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

import com.mmo.shared.dal.ShopFlagRepository;
import com.mmo.shared.model.ShopFlag;
import com.mmo.shared.model.Chat;
import com.mmo.shared.dal.ChatRepository;

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
    private ShopFlagRepository shopFlagRepository;

    @Autowired
    private ChatRepository chatRepository;

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
        return complaintRepository.countAllNotDeleted();
    }

    @Override
    public long getInProgressComplaints() {
        return complaintRepository.countByStatusesAndNotDeleted(java.util.List.of(
                "InProgress", "inprogress", "In_Progress", "in_progress", "Processing", "processing",
                "Open", "open", "New", "new"
        ));
    }

    @Override
    public long getResolvedComplaints() {
        return complaintRepository.countByStatusesAndNotDeleted(java.util.List.of(
                "Resolved", "resolved", "Completed", "completed", "Success", "success"
        ));
    }

    @Override
    public long getRefusedComplaints() {
        return complaintRepository.countByStatusesAndNotDeleted(java.util.List.of(
                "Rejected", "rejected", "Refused", "refused", "Failed", "failed", "Fail", "fail"
        ));
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

        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();

        boolean hasStatus = status != null &&
                !status.trim().isEmpty() &&
                !status.equals("ALL");

        // if (hasKeyword && hasStatus) {
        //
        // complaints =
        // complaintRepository
        // .findByDescriptionContainingIgnoreCaseAndStatusAndIsDeleteFalse(
        // keyword,
        // status,
        // pageable);
        //
        // } else if (hasKeyword) {
        //
        // complaints =
        // complaintRepository
        // .findByDescriptionContainingIgnoreCaseAndIsDeleteFalse(
        // keyword,
        // pageable);
        //
        // } else if (hasStatus) {
        //
        // complaints =
        // complaintRepository
        // .findByStatusAndIsDeleteFalse(
        // status,
        // pageable);
        //
        // } else {
        //
        // complaints =
        // complaintRepository
        // .findByIsDeleteFalse(pageable);
        // }

        if (!hasKeyword) {
            keyword = null;
        }

        if (!hasStatus) {
            status = null;
        }

        complaints = complaintRepository.searchComplaints(
                keyword,
                status,
                pageable);

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
                .orElseThrow(() -> new RuntimeException("Complaint not found"));

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
                                : null)
                .commissionVnd(
                        complaint.getTransaction() != null
                                ? complaint.getTransaction().getCommissionVnd()
                                : null)
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
                .orElseThrow(() -> new RuntimeException("Complaint not found"));

        complaint.setStatus(status);
        complaint.setResolution(resolution);

        complaintRepository.save(complaint);
    }

    // --- Methods from HEAD ---
    @Override
    @Transactional
    public Complaint createComplaint(Long customerId, Long transactionId, String description, String evidence, String preferredSolution) {
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

        // Kiểm tra bằng chứng bắt buộc
        if (evidence == null || evidence.trim().isEmpty()) {
            throw new IllegalArgumentException("Bằng chứng hình ảnh/video là bắt buộc khi gửi khiếu nại.");
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
                .status("PENDING_REVIEW")
                .preferredSolution(preferredSolution)
                .isDelete(false)
                .createdAt(LocalDateTime.now())
                .build();

        return complaintRepository.save(complaint);
    }

    @Override
    @Transactional
    public Complaint startDispute(Long complaintId, Long staffId) {
        User staff = userRepository.findByIdAndIsDeleteFalse(staffId)
                .orElseThrow(() -> new IllegalArgumentException("Nhân viên không tồn tại."));

        Complaint complaint = complaintRepository.findByIdAndIsDeleteFalse(complaintId)
                .orElseThrow(() -> new IllegalArgumentException("Khiếu nại không tồn tại hoặc đã bị xóa."));

        if (!"PENDING_REVIEW".equalsIgnoreCase(complaint.getStatus())) {
            throw new IllegalArgumentException("Chỉ có thể mở cuộc đối chất cho khiếu nại đang chờ duyệt (PENDING_REVIEW).");
        }

        // Cập nhật trạng thái khiếu nại
        complaint.setStatus("In_Progress");
        complaint = complaintRepository.save(complaint);

        // Tạo tin nhắn đối chất hệ thống tự động để kích hoạt phòng chat
        Chat systemMsg = new Chat();
        systemMsg.setSender(staff);
        systemMsg.setReceiver(complaint.getCustomer());
        systemMsg.setComplaint(complaint);
        systemMsg.setChatType("Complaint");
        systemMsg.setMessage("Hệ thống: Nhân viên " + (staff.getFullName() != null ? staff.getFullName() : staff.getEmail()) + " đã mở cuộc đối chất. Hai bên vui lòng đối chất thương lượng và cập nhật bằng chứng trong vòng 24 giờ.");
        systemMsg.setIsDelete(false);
        systemMsg.setSenderDeleted(false);
        systemMsg.setReceiverDeleted(false);
        systemMsg.setIsRead(false);
        systemMsg.setCreatedAt(LocalDateTime.now());
        chatRepository.save(systemMsg);

        return complaint;
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
    public Complaint updateComplaintStatus(Long complaintId, String status, String resolution, String flagLevel, String flagReason, Long staffId) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .filter(c -> c.getIsDelete() == null || !c.getIsDelete())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khiếu nại."));

        complaint.setStatus(status);
        complaint.setResolution(resolution);
        
        User staffUser = null;
        if (staffId != null) {
            staffUser = userRepository.findByIdAndIsDeleteFalse(staffId).orElse(null);
            complaint.setResolvedBy(staffUser);
        }
        complaint.setResolvedAt(LocalDateTime.now());

        // Xử lý tạo ShopFlag nếu Staff chọn gắn cờ
        if (flagLevel != null && !flagLevel.trim().isEmpty() && !"None".equalsIgnoreCase(flagLevel)) {
            User seller = complaint.getSeller();
            if (seller != null && staffUser != null) {
                ShopFlag flag = ShopFlag.builder()
                        .seller(seller)
                        .staff(staffUser)
                        .complaint(complaint)
                        .reason(flagReason != null && !flagReason.trim().isEmpty() ? flagReason.trim() : "Vi phạm trong khiếu nại #" + complaintId)
                        .flagLevel(flagLevel.trim())
                        .status("Effect")
                        .isDelete(false)
                        .createdAt(LocalDateTime.now())
                        .build();
                shopFlagRepository.save(flag);
                log.info("Staff ID {} đã gắn cờ {} cho Seller ID {} từ khiếu nại #{}", staffId, flagLevel, seller.getId(), complaintId);
            }
        }

        Transaction tx = complaint.getTransaction();
        if (tx != null) {
            if ("Resolved".equalsIgnoreCase(status) || "Completed".equalsIgnoreCase(status)) {
                tx.setStatus("Refunded");

                User customer = tx.getCustomer();
                User seller = tx.getSeller();

                // 1. Xác định tổng số ngày của gói dựa trên tên biến thể
                String variantName = (tx.getVariant() != null) ? tx.getVariant().getVariantName() : "";
                int totalDays = parseDurationDays(variantName);

                // 2. Tính số ngày sử dụng thực tế (từ khi mua đến khi gửi khiếu nại)
                long daysUsed = 0;
                if (tx.getCreatedAt() != null && complaint.getCreatedAt() != null) {
                    daysUsed = java.time.Duration.between(tx.getCreatedAt(), complaint.getCreatedAt()).toDays();
                }
                if (daysUsed < 0) daysUsed = 0;
                if (daysUsed > totalDays) daysUsed = totalDays;

                long daysRemaining = totalDays - daysUsed;

                // 3. Tính số tiền hoàn cho Buyer và phần còn lại giải ngân cho Seller
                long refundAmount = (long) Math.ceil((double) tx.getAmountVnd() / totalDays * daysRemaining);
                if (refundAmount > tx.getAmountVnd()) refundAmount = tx.getAmountVnd();
                if (refundAmount < 0) refundAmount = 0;

                long payoutAmount = tx.getAmountVnd() - refundAmount;

                // Hoa hồng sàn tính theo tỉ lệ gốc
                double commissionRate = tx.getAmountVnd() > 0 ? (double) tx.getCommissionVnd() / tx.getAmountVnd() : 0.05;
                long actualCommission = (long) (payoutAmount * commissionRate);
                long sellerNetPayout = payoutAmount - actualCommission;
                if (sellerNetPayout < 0) sellerNetPayout = 0;

                tx.setCommissionVnd(actualCommission);
                transactionRepository.save(tx);

                // Hoàn tiền cho Buyer
                if (customer != null && refundAmount > 0) {
                    long oldBalance = (customer.getBalanceVnd() != null ? customer.getBalanceVnd() : 0L);
                    long newBalance = oldBalance + refundAmount;
                    customer.setBalanceVnd(newBalance);
                    userRepository.save(customer);

                    String referenceCode = String.format("REFUND-CMP-%d-TX-%d", complaint.getId(), tx.getId());
                    walletService.recordTransaction(
                            customer,
                            "REFUND",
                            refundAmount,
                            "SUCCESS",
                            String.format("Hoàn tiền khiếu nại #CMP-%d (sử dụng %d/%d ngày): %s", complaint.getId(), daysUsed, totalDays, resolution),
                            referenceCode,
                            newBalance,
                            complaint.getId());

                    Notification buyerNotif = Notification.builder()
                            .userId(customer.getId())
                            .title("Thông báo hoàn tiền khiếu nại")
                            .content(String.format(
                                    "Khiếu nại #CMP-%d đối với đơn hàng MMO-ORD-%d đã được chấp nhận. Số tiền %,d VNĐ (%d ngày chưa sử dụng) đã được hoàn vào ví.",
                                    complaint.getId(), tx.getId(), refundAmount, daysRemaining))
                            .type("WALLET")
                            .severity("SUCCESS")
                            .isRead(false)
                            .isDelete(false)
                            .targetUrl("/account/transactions")
                            .build();
                    notificationRepository.save(buyerNotif);
                }

                // Giải ngân phần còn lại cho Seller
                if (seller != null && sellerNetPayout > 0) {
                    long oldBalance = (seller.getBalanceVnd() != null ? seller.getBalanceVnd() : 0L);
                    long newBalance = oldBalance + sellerNetPayout;
                    seller.setBalanceVnd(newBalance);
                    userRepository.save(seller);

                    String referenceCode = String.format("PAYOUT-CMP-RESOLVED-%d-TX-%d", complaint.getId(), tx.getId());
                    walletService.recordTransaction(
                            seller,
                            "PAYMENT",
                            sellerNetPayout,
                            "SUCCESS",
                            String.format("Giải ngân từ khiếu nại được chấp nhận #CMP-%d (sử dụng %d/%d ngày): %s", complaint.getId(), daysUsed, totalDays, resolution),
                            referenceCode,
                            newBalance,
                            complaint.getId());

                    Notification sellerNotif = Notification.builder()
                            .userId(seller.getId())
                            .title("Thông báo giải ngân khiếu nại")
                            .content(String.format(
                                    "Khiếu nại #CMP-%d đối với đơn hàng MMO-ORD-%d đã được giải quyết. Phần tiền %,d VNĐ (%d ngày đã dùng) đã được giải ngân.",
                                    complaint.getId(), tx.getId(), sellerNetPayout, daysUsed))
                            .type("WALLET")
                            .severity("SUCCESS")
                            .isRead(false)
                            .isDelete(false)
                            .targetUrl("/account/transactions")
                            .build();
                    notificationRepository.save(sellerNotif);
                }

                log.info(
                        "Giao dịch khiếu nại #CMP-{} đã được duyệt. Buyer nhận: {} VNĐ (hoàn {} ngày). Seller nhận: {} VNĐ (giải ngân {} ngày).",
                        complaint.getId(), refundAmount, daysRemaining, sellerNetPayout, daysUsed);
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
                            String.format("Giải ngân từ khiếu nại bị từ chối #CMP-%d: %s", complaint.getId(),
                                    resolution),
                            referenceCode,
                            newBalance,
                            complaint.getId());

                    // Gửi thông báo giải ngân cho Seller
                    Notification sellerNotif = Notification.builder()
                            .userId(seller.getId())
                            .title("Thông báo giải ngân đơn hàng")
                            .content(String.format(
                                    "Khiếu nại #CMP-%d đối với đơn hàng MMO-ORD-%d đã bị từ chối. Số tiền %,d VNĐ đã được giải ngân vào ví của bạn.",
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
                                .content(String.format(
                                        "Khiếu nại #CMP-%d đối với đơn hàng MMO-ORD-%d đã bị từ chối. Lý do: %s",
                                        complaint.getId(), tx.getId(), resolution))
                                .type("WALLET")
                                .severity("DANGER")
                                .isRead(false)
                                .isDelete(false)
                                .targetUrl("/account/transactions")
                                .build();
                        notificationRepository.save(customerNotif);
                    }

                    log.info(
                            "Ghi lại giao dịch giải ngân PAYMENT cho Seller ID {} từ khiếu nại #CMP-{} bị từ chối. Số tiền: {} VNĐ. Số dư mới: {} VNĐ",
                            seller.getId(), complaint.getId(), payout, newBalance);
                }
            } else if ("InProgress".equalsIgnoreCase(status) || "In_Progress".equalsIgnoreCase(status)) {
                tx.setStatus("Disputed");
                transactionRepository.save(tx);
            }
        }

        return complaintRepository.save(complaint);
    }

    @Override
    public org.springframework.data.domain.Page<Complaint> searchComplaintsForStaff(String keyword, String status, int page, int size) {
        org.springframework.data.domain.Pageable pageable = 
                org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by("createdAt").descending());
        Long complaintId = null;
        String searchKeyword = null;
        if (keyword != null && !keyword.trim().isEmpty()) {
            String clean = keyword.trim();
            if (clean.startsWith("#")) {
                clean = clean.substring(1);
            }
            if (clean.toUpperCase().startsWith("CMP-")) {
                clean = clean.substring(4);
            }
            clean = clean.trim();
            if (!clean.isEmpty()) {
                try {
                    complaintId = Long.parseLong(clean);
                } catch (NumberFormatException e) {
                    searchKeyword = clean;
                }
            }
        }
        java.util.Collection<String> queryStatuses = new java.util.ArrayList<>();
        boolean hasStatus = false;
        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            String statusVal = status.trim();
            hasStatus = true;
            if ("New".equalsIgnoreCase(statusVal)) {
                queryStatuses.addAll(java.util.List.of("New", "new", "Open", "open"));
            } else if ("InProgress".equalsIgnoreCase(statusVal)) {
                queryStatuses.addAll(java.util.List.of(
                        "InProgress", "inprogress", "In_Progress", "in_progress", "Processing", "processing",
                        "Open", "open", "New", "new"
                ));
            } else if ("Resolved".equalsIgnoreCase(statusVal) || "Completed".equalsIgnoreCase(statusVal)) {
                queryStatuses.addAll(java.util.List.of("Resolved", "resolved", "Completed", "completed"));
            } else if ("Rejected".equalsIgnoreCase(statusVal)) {
                queryStatuses.addAll(java.util.List.of("Rejected", "rejected", "Refused", "refused"));
            } else {
                queryStatuses.add(statusVal);
            }
        }
        if (!hasStatus) {
            queryStatuses.add("DUMMY_STATUS_EMPTY");
        }
        
        return complaintRepository.searchComplaintsForStaff(complaintId, searchKeyword, queryStatuses, hasStatus, pageable);
    }

    private int parseDurationDays(String variantName) {
        if (variantName == null) return 30;
        String name = variantName.toLowerCase();
        if (name.contains("năm") || name.contains("12 tháng") || name.contains("1 year") || name.contains("12 month")) {
            return 365;
        }
        if (name.contains("6 tháng") || name.contains("6 month")) {
            return 180;
        }
        if (name.contains("3 tháng") || name.contains("3 month")) {
            return 90;
        }
        if (name.contains("1 tháng") || name.contains("tháng") || name.contains("1 month") || name.contains("month")) {
            return 30;
        }
        // Thử tìm số ngày cụ thể (Ví dụ: "10 ngày" hay "10 days")
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)\\s*(ngày|day)");
        java.util.regex.Matcher matcher = pattern.matcher(name);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 30; // Mặc định 30 ngày
    }
}