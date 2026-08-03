package com.mmo.feature.admin.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmo.shared.dto.CashflowTransactionDto;
import com.mmo.shared.dto.RevenueSummaryResponse;
import com.mmo.shared.dal.*;
import com.mmo.shared.model.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminRevenueService {

    private final TransactionRepository transactionRepository;
    private final WithdrawalRepository withdrawalRepository;
    private final TopupTransactionRepository topupTransactionRepository;
    private final SellerRegistrationRepository sellerRegistrationRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final SystemConfigurationRepository systemConfigurationRepository;
    private final ObjectMapper objectMapper;

    public AdminRevenueService(TransactionRepository transactionRepository,
                               WithdrawalRepository withdrawalRepository,
                               TopupTransactionRepository topupTransactionRepository,
                               SellerRegistrationRepository sellerRegistrationRepository,
                               ProductRepository productRepository,
                               UserRepository userRepository,
                               SystemConfigurationRepository systemConfigurationRepository,
                               ObjectMapper objectMapper) {
        this.transactionRepository = transactionRepository;
        this.withdrawalRepository = withdrawalRepository;
        this.topupTransactionRepository = topupTransactionRepository;
        this.sellerRegistrationRepository = sellerRegistrationRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.systemConfigurationRepository = systemConfigurationRepository;
        this.objectMapper = objectMapper;
    }

    private User requireAdmin(Long operatorId) {
        if (operatorId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập.");
        }
        User operator = userRepository.findByIdAndIsDeleteFalse(operatorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng."));
        if (!"Admin".equalsIgnoreCase(normalizeRole(operator.getRole()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ Admin mới có quyền truy cập chức năng này.");
        }
        if (Boolean.TRUE.equals(operator.getIsLocked())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản Admin đang bị khóa.");
        }
        return operator;
    }

    private String normalizeRole(String roleValue) {
        if (roleValue == null || roleValue.isBlank()) {
            return "Customer";
        }
        try {
            JsonNode node = objectMapper.readTree(roleValue);
            JsonNode roleNode = node.get("role");
            if (roleNode != null && !roleNode.asText().isBlank()) {
                return canonicalRole(roleNode.asText());
            }
        } catch (Exception ignored) {}
        return canonicalRole(roleValue.replace("\"", "").trim());
    }

    private String canonicalRole(String role) {
        if (role == null || role.isBlank()) {
            return "Customer";
        }
        String normalized = role.trim();
        if (normalized.toLowerCase(Locale.ROOT).contains("admin")) {
            return "Admin";
        }
        if (normalized.toLowerCase(Locale.ROOT).contains("staff")) {
            return "Staff";
        }
        if (normalized.toLowerCase(Locale.ROOT).contains("seller")) {
            return "Seller";
        }
        return "Customer";
    }

    @Transactional(readOnly = true)
    public RevenueSummaryResponse getRevenueSummary(Long operatorId) {
        requireAdmin(operatorId);

        // 1. Phí hoa hồng: từ các C2C Transactions có trạng thái 'Completed' hoặc 'Held'
        long commissions = transactionRepository.sumCommissionForCompletedOrHeldTransactions();

        // 2. Phí mở Shop: Tổng phí ghi nhận của các SellerRegistrations có trạng thái 'Approved'
        long shopOpeningFeeConfig = systemConfigurationRepository.findByConfigKey("SHOP_OPENING_FEE_VND")
                .map(c -> {
                    try { return Long.parseLong(c.getConfigValue()); }
                    catch (NumberFormatException e) { return 500000L; }
                }).orElse(500000L);

        List<SellerRegistration> approvedRegistrations = sellerRegistrationRepository.findAllByIsDeleteFalseOrderByCreatedAtDesc();
        long shopOpeningFees = approvedRegistrations.stream()
                .filter(reg -> "Approved".equalsIgnoreCase(reg.getStatus()))
                .mapToLong(reg -> reg.getFeeVnd() != null ? reg.getFeeVnd() : 500000L)
                .sum();

        // 3. Phí rút tiền: từ các Withdrawals có trạng thái 'Completed'
        double withdrawalPercent = systemConfigurationRepository.findByConfigKey("WITHDRAWAL_FEE_PERCENT")
                .map(c -> {
                    try { return Double.parseDouble(c.getConfigValue()); }
                    catch (NumberFormatException e) { return 1.5; }
                }).orElse(1.5);

        List<Withdrawal> withdrawals = withdrawalRepository.findByStatusAndIsDeleteFalse("Completed");

        long withdrawalFees = withdrawals.stream().mapToLong(w -> {
            if (w.getFeeVnd() != null) {
                return w.getFeeVnd();
            }
            return (long) (w.getAmountVnd() * (withdrawalPercent / 100.0));
        }).sum();

        // 4. Doanh thu ròng tổng cộng
        long netTotal = commissions + shopOpeningFees + withdrawalFees;

        return RevenueSummaryResponse.builder()
                .commissions(commissions)
                .shopOpeningFees(shopOpeningFees)
                .withdrawalFees(withdrawalFees)
                .netTotal(netTotal)
                .build();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getCashflowTransactions(Long operatorId, String keyword, String type, String startDate, String endDate, int page, int size) {
        return getCashflowTransactions(operatorId, keyword, type, "", startDate, endDate, "DESC", page, size);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getCashflowTransactions(Long operatorId, String keyword, String type, String status, String startDate, String endDate, String sort, int page, int size) {
        requireAdmin(operatorId);
        
        List<CashflowTransactionDto> allCashflow = getFilteredCashflowList(keyword, type, status, startDate, endDate, sort);

        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        int fromIndex = Math.min(safePage * safeSize, allCashflow.size());
        int toIndex = Math.min(fromIndex + safeSize, allCashflow.size());

        Map<String, Object> result = new HashMap<>();
        result.put("content", allCashflow.subList(fromIndex, toIndex));
        result.put("page", safePage);
        result.put("size", safeSize);
        result.put("totalElements", allCashflow.size());
        result.put("totalPages", (int) Math.ceil((double) allCashflow.size() / safeSize));
        return result;
    }

    @Transactional(readOnly = true)
    public byte[] exportRevenueCsv(Long operatorId, String keyword, String type, String startDate, String endDate) {
        return exportRevenueCsv(operatorId, keyword, type, "", startDate, endDate, "DESC");
    }

    @Transactional(readOnly = true)
    public byte[] exportRevenueCsv(Long operatorId, String keyword, String type, String status, String startDate, String endDate, String sort) {
        requireAdmin(operatorId);

        List<CashflowTransactionDto> transactions = getFilteredCashflowList(keyword, type, status, startDate, endDate, sort);
        
        StringBuilder csv = new StringBuilder();
        // Byte Order Mark (BOM) for Excel UTF-8 support
        csv.append("\uFEFF");
        csv.append("STT,Mã GD,Thời gian,Tài khoản,Loại giao dịch,Số tiền (VNĐ),Phí sàn (VNĐ),Trạng thái\n");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        for (int i = 0; i < transactions.size(); i++) {
            CashflowTransactionDto tx = transactions.get(i);
            String txTypeLabel = "Shop_Opening".equals(tx.getType()) ? "Phí mở shop" : ("Withdrawal".equals(tx.getType()) ? "Rút tiền" : "Giao dịch C2C");
            String statusLabel = "Completed".equals(tx.getStatus()) ? "Hoàn tất" : ("Held".equals(tx.getStatus()) ? "Tạm giữ" : ("Pending".equals(tx.getStatus()) ? "Đang xử lý" : "Thất bại"));
            csv.append(i + 1).append(",")
                    .append(tx.getId()).append(",")
                    .append(tx.getTimestamp().format(formatter)).append(",")
                    .append(tx.getEmail()).append(",")
                    .append(txTypeLabel).append(",")
                    .append(tx.getAmount()).append(",")
                    .append(tx.getFee()).append(",")
                    .append(statusLabel).append("\n");
        }

        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private List<CashflowTransactionDto> getFilteredCashflowList(String keyword, String type, String statusFilter, String startDate, String endDate, String sort) {
        double withdrawalPercent = systemConfigurationRepository.findByConfigKey("WITHDRAWAL_FEE_PERCENT")
                .map(c -> {
                    try { return Double.parseDouble(c.getConfigValue()); }
                    catch (NumberFormatException e) { return 1.5; }
                }).orElse(1.5);

        List<CashflowTransactionDto> allCashflow = new ArrayList<>();

        long shopOpeningFee = systemConfigurationRepository.findByConfigKey("SHOP_OPENING_FEE_VND")
                .map(c -> {
                    try { return Long.parseLong(c.getConfigValue()); }
                    catch (NumberFormatException e) { return 50000L; }
                }).orElse(50000L);

        // 1. Phí mở Shop (SellerRegistrations được phê duyệt)
        List<SellerRegistration> shopRegistrations = sellerRegistrationRepository.findAllByIsDeleteFalseOrderByCreatedAtDesc();
        if (shopRegistrations != null) {
            for (SellerRegistration reg : shopRegistrations) {
                if ("Approved".equalsIgnoreCase(reg.getStatus())) {
                    long actualFee = reg.getFeeVnd() != null ? reg.getFeeVnd() : 500000L;
                    allCashflow.add(CashflowTransactionDto.builder()
                            .id("SHOP" + reg.getId())
                            .timestamp(reg.getCreatedAt())
                            .email(reg.getUser() != null ? reg.getUser().getEmail() : "unknown@mmo.com")
                            .type("Shop_Opening")
                            .amount(actualFee)
                            .fee(actualFee)
                            .status("Completed")
                            .build());
                }
            }
        }

        // 2. Rút tiền (Withdrawals)
        List<Withdrawal> withdrawals = withdrawalRepository.findAllWithSellerByIsDeleteFalse();
        for (Withdrawal w : withdrawals) {
            long fee;
            if (w.getFeeVnd() != null) {
                fee = w.getFeeVnd();
            } else {
                fee = (long) (w.getAmountVnd() * (withdrawalPercent / 100.0));
            }
            String status = "Rejected".equalsIgnoreCase(w.getStatus()) ? "Failed" : w.getStatus();

            allCashflow.add(CashflowTransactionDto.builder()
                    .id("WTH" + w.getId())
                    .timestamp(w.getCreatedAt())
                    .email(w.getSeller() != null ? w.getSeller().getEmail() : "unknown@mmo.com")
                    .type("Withdrawal")
                    .amount(w.getAmountVnd())
                    .fee(fee)
                    .status(status)
                    .build());
        }

        // 3. Giao dịch mua bán C2C (Transactions)
        List<Transaction> transactions = transactionRepository.findAllWithCustomerByIsDeleteFalse();
        for (Transaction t : transactions) {
            String status = t.getStatus();
            if ("Refunded".equalsIgnoreCase(status) || "Cancelled".equalsIgnoreCase(status)) {
                status = "Failed";
            }
            allCashflow.add(CashflowTransactionDto.builder()
                    .id("TX" + t.getId())
                    .timestamp(t.getCreatedAt())
                    .email(t.getCustomer() != null ? t.getCustomer().getEmail() : "unknown@mmo.com")
                    .type("C2C_Purchase")
                    .amount(t.getAmountVnd())
                    .fee(t.getCommissionVnd())
                    .status(status)
                    .build());
        }

        // Áp dụng bộ lọc
        String kw = keyword != null ? keyword.trim().toLowerCase(Locale.ROOT) : "";
        String tp = type != null ? type.trim() : "";
        String st = statusFilter != null ? statusFilter.trim() : "";
        
        LocalDate parsedStart = null;
        LocalDate parsedEnd = null;
        try {
            if (startDate != null && !startDate.isBlank()) {
                parsedStart = LocalDate.parse(startDate.trim());
            }
            if (endDate != null && !endDate.isBlank()) {
                parsedEnd = LocalDate.parse(endDate.trim());
            }
        } catch (Exception ignored) {}

        final LocalDate finalStart = parsedStart;
        final LocalDate finalEnd = parsedEnd;

        boolean isAsc = "ASC".equalsIgnoreCase(sort);
        Comparator<CashflowTransactionDto> comparator = Comparator.comparing(CashflowTransactionDto::getTimestamp);
        if (!isAsc) {
            comparator = comparator.reversed();
        }

        return allCashflow.stream()
                .filter(tx -> kw.isEmpty() || tx.getId().toLowerCase(Locale.ROOT).contains(kw) || tx.getEmail().toLowerCase(Locale.ROOT).contains(kw))
                .filter(tx -> tp.isEmpty() || tx.getType().equalsIgnoreCase(tp))
                .filter(tx -> st.isEmpty() || tx.getStatus().equalsIgnoreCase(st))
                .filter(tx -> {
                    if (tx.getTimestamp() == null) return false;
                    LocalDate txDate = tx.getTimestamp().toLocalDate();
                    if (finalStart != null && txDate.isBefore(finalStart)) {
                        return false;
                    }
                    if (finalEnd != null && txDate.isAfter(finalEnd)) {
                        return false;
                    }
                    return true;
                })
                .sorted(comparator)
                .collect(Collectors.toList());
    }
}
