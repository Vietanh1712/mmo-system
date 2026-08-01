package com.mmo.feature.staff.controller.mvc;
import com.mmo.shared.model.Chat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import com.mmo.feature.staff.service.StaffDashboardService;
import com.mmo.shared.dal.ComplaintRepository;
import com.mmo.shared.dal.TransactionRepository;
import com.mmo.shared.dal.WithdrawalRepository;
import com.mmo.shared.dal.ShopFlagRepository;
import com.mmo.shared.dal.NotificationRepository;
import com.mmo.shared.model.ShopFlag;
import com.mmo.shared.model.Transaction;
import com.mmo.shared.model.Withdrawal;
import com.mmo.shared.model.Complaint;
import com.mmo.shared.model.Notification;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/staff")
public class StaffController {

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private WithdrawalRepository withdrawalRepository;

    @Autowired
    private ShopFlagRepository shopFlagRepository;

    @Autowired
    private StaffDashboardService staffDashboardService;

    @Autowired
    private com.mmo.feature.wallet.service.WithdrawalService withdrawalService;

    @Autowired
    private NotificationRepository notificationRepository;



    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("dashboard", staffDashboardService.getDashboardData());
        return "staff/dashboard";
    }

    @GetMapping("/categories")
    public String categories() {
        return "staff/categories";
    }

    @GetMapping("/categories/detail")
    public String categoryDetail() {
        return "staff/category-detail";
    }

    @GetMapping("/documents")
    public String documentsDashboard(Model model) {
        model.addAttribute("dashboard", staffDashboardService.getDashboardData());
        return "staff/documents-dashboard";
    }

    @GetMapping("/complaints")
    public String complaints() {
        return "staff/complaints";
    }

    @GetMapping("/complaints/detail")
    public String complaintDetail() {
        return "staff/complaint-detail";
    }

    @GetMapping("/support-tickets")
    public String supportTickets() {
        return "staff/support-tickets";
    }

    @GetMapping("/support-tickets/detail")
    public String supportTicketDetail() {
        return "staff/support-ticket-detail";
    }

    @GetMapping("/kyc")
    public String kyc() {
        return "staff/kyc";
    }

    @GetMapping("/kyc/detail")
    public String kycDetail() {
        return "staff/kyc-detail";
    }

    @GetMapping("/transactions")
    public String transactions(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        LocalDateTime from = null, to = null;
        try {
            if (fromDate != null && !fromDate.isBlank()) {
                from = LocalDate.parse(fromDate).atStartOfDay();
                to = LocalDate.parse(fromDate).atTime(23, 59, 59);
            }
        } catch (Exception ignored) {}
        try {
            if (toDate != null && !toDate.isBlank()) {
                to = LocalDate.parse(toDate).atTime(23, 59, 59);
            }
        } catch (Exception ignored) {}

        Long id = null;
        String kw = (keyword != null && keyword.isBlank()) ? null : keyword;
        if (kw != null) {
            try { id = Long.parseLong(kw.replace("#TXN-", "").trim()); kw = null; } catch (Exception ignored) {}
        }

        String typeParam = (type != null && !type.isBlank()) ? type : null;

        java.util.List<String> statusList = java.util.Collections.emptyList();
        boolean hasStatusFilter = false;
        if (status != null && !status.isBlank()) {
            hasStatusFilter = true;
            String sUpper = status.trim().toUpperCase();
            if (sUpper.equals("SUCCESS") || sUpper.equals("COMPLETED")) {
                statusList = java.util.List.of("Success", "SUCCESS", "Completed", "COMPLETED", "Approved", "APPROVED");
            } else if (sUpper.equals("PENDING") || sUpper.equals("PROCESSING")) {
                statusList = java.util.List.of("Pending", "PENDING", "Processing", "PROCESSING");
            } else if (sUpper.equals("HOLDING") || sUpper.equals("HOLD") || sUpper.equals("ESCROW") || sUpper.equals("HELD")) {
                statusList = java.util.List.of("Holding", "HOLDING", "Hold", "HOLD", "Escrow", "ESCROW", "Held", "HELD");
            } else if (sUpper.equals("REFUNDED") || sUpper.equals("REFUND")) {
                statusList = java.util.List.of("Refunded", "REFUNDED", "Refund", "REFUND");
            } else if (sUpper.equals("DISPUTED") || sUpper.equals("COMPLAINT")) {
                statusList = java.util.List.of("Disputed", "DISPUTED", "Complaint", "COMPLAINT");
            } else if (sUpper.equals("FAILED") || sUpper.equals("FAIL") || sUpper.equals("REJECTED") || sUpper.equals("CANCELLED") || sUpper.equals("CANCEL")) {
                statusList = java.util.List.of("Failed", "FAILED", "Fail", "FAIL", "Rejected", "REJECTED", "Cancelled", "CANCELLED", "Cancel", "CANCEL");
            } else {
                statusList = java.util.List.of(status, status.toUpperCase(), status.toLowerCase());
            }
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Transaction> txPage = transactionRepository.searchTransactions(
                kw, id, typeParam, hasStatusFilter, statusList, from, to, pageable);

        model.addAttribute("transactions",         txPage.getContent());
        model.addAttribute("currentPage",           page);
        model.addAttribute("totalPages",            txPage.getTotalPages());
        model.addAttribute("totalFilteredTransactions", txPage.getTotalElements());
        long overallTotalCount = transactionRepository.countByIsDeleteFalse();
        model.addAttribute("totalTransactions",     overallTotalCount);
        long completedCount = transactionRepository.countByStatusesAndNotDeleted(java.util.List.of("Success", "success", "Completed", "completed", "Approved", "approved"));
        long pendingCount = transactionRepository.countByStatusesAndNotDeleted(java.util.List.of("Pending", "pending", "Processing", "processing", "Held", "held", "Holding", "holding", "Hold", "hold"));
        long failCount = transactionRepository.countByStatusesAndNotDeleted(java.util.List.of("Failed", "failed", "Fail", "fail", "Rejected", "rejected", "Cancelled", "cancelled", "Cancel", "cancel"));

        model.addAttribute("completedTransactions", completedCount);
        model.addAttribute("pendingTransactions",   pendingCount);
        model.addAttribute("failTransactions",      failCount);
        model.addAttribute("types",           transactionRepository.findAllTransactionTypes());
        model.addAttribute("statuses",        transactionRepository.findAllStatus());
        model.addAttribute("keyword",         keyword);
        model.addAttribute("selectedType",    type);
        model.addAttribute("selectedStatus",  status);
        model.addAttribute("fromDate",        fromDate);
        model.addAttribute("toDate",          toDate);
        model.addAttribute("pageSize",        size);
        return "staff/transactions";
    }

    @GetMapping("/transactions/detail")
    public String transactionDetail(@RequestParam Long id, Model model) {
        Transaction transaction = transactionRepository.findDetailById(id);
        if (transaction == null) {
            return "redirect:/staff/transactions";
        }
        model.addAttribute("transaction", transaction);

        String remainingHours = "";
        if (transaction.getEscrowReleaseDate() != null) {
            long hours = Duration.between(LocalDateTime.now(), transaction.getEscrowReleaseDate()).toHours();
            if (hours < 0) {
                remainingHours = "Đã giải ngân (quá hạn bảo lãnh)";
            } else {
                remainingHours = hours + " giờ";
            }
        } else {
            remainingHours = "Không có thông tin bảo lãnh";
        }
        model.addAttribute("remainingHours", remainingHours);

        Complaint complaint = complaintRepository.findFirstByTransactionIdAndIsDeleteFalseOrderByIdDesc(id).orElse(null);
        model.addAttribute("complaint", complaint);

        return "staff/transaction-detail";
    }

    @GetMapping("/withdrawals")
    public String withdrawals(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long minAmount,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        String st = (status == null || status.isBlank() || status.equals("ALL")) ? null : status;
        String kw = (keyword != null && keyword.isBlank()) ? null : keyword;

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Withdrawal> wPage = withdrawalRepository.searchWithdrawals(st, kw, minAmount, pageable);

        model.addAttribute("withdrawals",          wPage.getContent());
        model.addAttribute("currentPage",           page);
        model.addAttribute("totalPages",            wPage.getTotalPages());
        long pending = withdrawalRepository.countByStatusesAndNotDeleted(java.util.List.of("Pending", "pending", "Processing", "processing"));
        long completed = withdrawalRepository.countByStatusesAndNotDeleted(java.util.List.of("Completed", "completed", "Approved", "approved", "Success", "success"));
        long rejected = withdrawalRepository.countByStatusesAndNotDeleted(java.util.List.of("Rejected", "rejected", "Failed", "failed"));

        model.addAttribute("totalWithdrawals",      withdrawalRepository.count());
        model.addAttribute("pendingWithdrawals",    pending);
        model.addAttribute("completedWithdrawals",  completed);
        model.addAttribute("rejectedWithdrawals",   rejected);
        model.addAttribute("statuses",              withdrawalRepository.findAllStatus());
        model.addAttribute("keyword",               keyword);
        model.addAttribute("selectedStatus",        st);
        model.addAttribute("minAmount",             minAmount);
        model.addAttribute("pageSize",              size);
        return "staff/withdrawals";
    }

    @GetMapping("/withdrawals/detail")
    public String withdrawalDetail(@RequestParam Long id, Model model) {
        Withdrawal withdrawal = withdrawalRepository.findDetailById(id)
                .orElseThrow(() -> new RuntimeException("Withdrawal request not found"));
        model.addAttribute("withdrawal", withdrawal);

        List<String> statuses = withdrawalRepository.findAllStatus();
        if (statuses.isEmpty()) {
            statuses = List.of("Pending", "Approved", "Rejected");
        }
        model.addAttribute("statuses", statuses);

        return "staff/withdrawal-detail";
    }

    @PostMapping("/withdrawals/update-status")
    public String updateWithdrawalStatus(
            @RequestParam Long id,
            @RequestParam String status,
            @RequestParam(value = "proofFile", required = false) MultipartFile proofFile,
            RedirectAttributes redirectAttributes
    ) {
        Object principal243 = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long reviewerId = (principal243 instanceof Long) ? (Long) principal243 : null;
        
        String proofFileUrl = null;
        try {
            if ("Completed".equalsIgnoreCase(status)) {
                if (proofFile == null || proofFile.isEmpty()) {
                    throw new RuntimeException("Vui lòng đính kèm hình ảnh giao dịch để hoàn tất.");
                }
                String uploadDir = "uploads";
                File dir = new File(uploadDir);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                String originalFilename = proofFile.getOriginalFilename();
                String extension = "";
                if (originalFilename != null && originalFilename.contains(".")) {
                    extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                }
                String fileName = UUID.randomUUID().toString() + extension;
                Path path = Paths.get(uploadDir, fileName);
                Files.copy(proofFile.getInputStream(), path);
                proofFileUrl = "/uploads/" + fileName;
            }

            withdrawalService.updateWithdrawalStatus(id, status, reviewerId, null, proofFileUrl);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi duyệt rút tiền: " + e.getMessage());
            return "redirect:/staff/withdrawals/detail?id=" + id;
        }

        Withdrawal withdrawal = withdrawalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Withdrawal request not found"));

        // Gửi thông báo cho Seller
        String title = "Cập nhật yêu cầu rút tiền";
        String content = String.format("Yêu cầu rút tiền số tiền %s VNĐ của bạn đã chuyển sang trạng thái: %s.", String.format("%,d", withdrawal.getAmountVnd()), "Approved".equalsIgnoreCase(status) || "Completed".equalsIgnoreCase(status) ? "Đã duyệt" : status);
        String severity = "Approved".equalsIgnoreCase(status) || "Completed".equalsIgnoreCase(status) ? "SUCCESS" : "INFO";
        if ("Rejected".equalsIgnoreCase(status)) {
            title = "Yêu cầu rút tiền bị từ chối";
            content = String.format("Yêu cầu rút tiền số tiền %s VNĐ của bạn đã bị từ chối.", String.format("%,d", withdrawal.getAmountVnd()));
            severity = "DANGER";
        }

        Notification notif = Notification.builder()
                .userId(withdrawal.getSeller().getId())
                .title(title)
                .content(content)
                .type("WALLET")
                .severity(severity)
                .isRead(false)
                .isDelete(false)
                .targetUrl("/wallet/transactions")
                .build();
        notificationRepository.save(notif);

        redirectAttributes.addFlashAttribute(
                "success",
                "Cập nhật trạng thái thành công"
        );
        return "redirect:/staff/withdrawals/detail?id=" + id;
    }

    @PostMapping("/withdrawals/reject")
    public String rejectWithdrawal(
            @RequestParam Long id,
            @RequestParam(required = false) String reason,
            RedirectAttributes redirectAttributes
    ) {
        Object principal284 = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long reviewerId = (principal284 instanceof Long) ? (Long) principal284 : null;
        withdrawalService.updateWithdrawalStatus(id, "Rejected", reviewerId, reason, null);

        Withdrawal withdrawal = withdrawalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Withdrawal request not found"));

        // Gửi thông báo từ chối cho Seller
        Notification notif = Notification.builder()
                .userId(withdrawal.getSeller().getId())
                .title("Yêu cầu rút tiền bị từ chối")
                .content(String.format("Yêu cầu rút tiền số tiền %s VNĐ của bạn đã bị từ chối.", String.format("%,d", withdrawal.getAmountVnd())))
                .type("WALLET")
                .severity("DANGER")
                .isRead(false)
                .isDelete(false)
                .targetUrl("/wallet/transactions")
                .build();
        notificationRepository.save(notif);

        redirectAttributes.addFlashAttribute(
                "warning",
                "Đã từ chối rút tiền"
        );
        return "redirect:/staff/withdrawals/detail?id=" + id;
    }

    @GetMapping("/flags")
    public String flags(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        String lv = (level  == null || level.isBlank()  || level.equals("ALL"))  ? null : level;
        String st = (status == null || status.isBlank() || status.equals("ALL")) ? null : status;
        String kw = (keyword != null && keyword.isBlank()) ? null : keyword;

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ShopFlag> fPage = shopFlagRepository.searchFlags(kw, lv, st, pageable);

        model.addAttribute("flags",          fPage.getContent());
        model.addAttribute("currentPage",    page);
        model.addAttribute("totalPages",     fPage.getTotalPages());
        model.addAttribute("totalFlags",     shopFlagRepository.countByIsDeleteFalse());
        model.addAttribute("dangerFlags",    shopFlagRepository.countByFlagLevelAndIsDeleteFalse("Danger"));
        model.addAttribute("warningFlags",   shopFlagRepository.countByFlagLevelAndIsDeleteFalse("Warning"));
        model.addAttribute("criticalFlags",  shopFlagRepository.countByFlagLevelAndIsDeleteFalse("Critical"));
        model.addAttribute("removedFlags",   shopFlagRepository.countRemovedFlags());
        model.addAttribute("activeFlags",    shopFlagRepository.countActiveFlags());

        List<String> levels = shopFlagRepository.findDistinctFlagLevels();
        if (levels.isEmpty()) {
            levels = List.of("Warning", "Critical", "Danger");
        }
        model.addAttribute("levels",         levels);

        List<String> statuses = shopFlagRepository.findDistinctStatuses();
        if (statuses.isEmpty()) {
            statuses = List.of("Effect", "Removed");
        }
        model.addAttribute("statuses",       statuses);
        model.addAttribute("keyword",        keyword);
        model.addAttribute("selectedLevel",  lv);
        model.addAttribute("selectedStatus", st);
        model.addAttribute("pageSize",       size);
        return "staff/flags";
    }

    @GetMapping("/flags/detail")
    public String flagDetail(@RequestParam Long id, Model model) {
        ShopFlag flag = shopFlagRepository.findByIdAndIsDeleteFalse(id)
                .orElse(null);
        if (flag == null) {
            return "redirect:/staff/flags";
        }
        model.addAttribute("flag", flag);
        
        List<String> levels = shopFlagRepository.findDistinctFlagLevels();
        if (levels.isEmpty()) {
            levels = List.of("Warning", "Critical", "Danger");
        }
        model.addAttribute("levels", levels);

        List<String> statuses = shopFlagRepository.findDistinctStatuses();
        if (statuses.isEmpty()) {
            statuses = List.of("Effect", "Removed");
        }
        model.addAttribute("statuses", statuses);
        
        return "staff/flag-detail";
    }

    @PostMapping("/flags/update")
    public String updateFlag(
            @RequestParam Long id,
            @RequestParam String flagLevel,
            @RequestParam String status,
            @RequestParam String reason,
            RedirectAttributes redirectAttributes
    ) {
        ShopFlag flag = shopFlagRepository.findByIdAndIsDeleteFalse(id)
                .orElse(null);
        if (flag != null) {
            flag.setFlagLevel(flagLevel);
            flag.setStatus(status);
            flag.setReason(reason);
            shopFlagRepository.save(flag);
            redirectAttributes.addFlashAttribute(
                    "success",
                    "Cập nhật cờ thành công"
            );
        }
        return "redirect:/staff/flags/detail?id=" + id;
    }

    @PostMapping("/flags/remove")
    public String removeFlag(
            @RequestParam Long id,
            RedirectAttributes redirectAttributes
    ) {
        ShopFlag flag = shopFlagRepository.findByIdAndIsDeleteFalse(id)
                .orElse(null);
        if (flag != null) {
            flag.setIsDelete(true);
            shopFlagRepository.save(flag);
            redirectAttributes.addFlashAttribute(
                    "success",
                    "Đã gỡ cờ thành công"
            );
        }
        return "redirect:/staff/flags";
    }

    @GetMapping("/chat")
    public String chat() {
        return "staff/chat";
    }

    @GetMapping("/shop-registrations")
    public String shopRegistrations() {
        return "staff/shop-registrations";
    }

    @GetMapping("/shop-registrations/detail")
    public String shopRegistrationDetail() {
        return "staff/shop-registration-detail";
    }

    @GetMapping("/shop-registrations/update-status")
    public String shopRegistrationUpdateStatus() {
        return "staff/shop-registration-update-status";
    }

    @GetMapping("/topups")
    public String topups() {
        return "staff/topups";
    }

    @GetMapping("/topups/detail")
    public String topupDetail() {
        return "staff/topup-detail";
    }
}

