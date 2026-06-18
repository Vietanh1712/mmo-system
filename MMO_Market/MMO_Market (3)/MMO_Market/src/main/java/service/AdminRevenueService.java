package service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import controller.dto.CashflowTransactionDto;
import controller.dto.RevenueSummaryResponse;
import dal.*;
import model.*;
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
        List<Transaction> transactions = transactionRepository.findAll().stream()
                .filter(t -> !Boolean.TRUE.equals(t.getIsDelete()))
                .filter(t -> "Completed".equals(t.getStatus()) || "Held".equals(t.getStatus()))
                .toList();
        long commissions = transactions.stream().mapToLong(Transaction::getCommissionVnd).sum();

        // 2. Phí nâng cấp Seller: từ số lượng SellerRegistrations có trạng thái 'Approved'
        long upgradeFee = systemConfigurationRepository.findByConfigKey("SELLER_UPGRADE_FEE_VND")
                .map(c -> {
                    try { return Long.parseLong(c.getConfigValue()); }
                    catch (NumberFormatException e) { return 50000L; }
                }).orElse(50000L);

        long approvedSellers = sellerRegistrationRepository.findAllByIsDeleteFalseOrderByCreatedAtDesc().stream()
                .filter(r -> "Approved".equals(r.getStatus()))
                .count();
        long sellerUpgradeFees = approvedSellers * upgradeFee;

        // 3. Phí đẩy tin nổi bật sản phẩm: từ số lượng sản phẩm đang hoạt động
        long featuredFee = systemConfigurationRepository.findByConfigKey("PRODUCT_FEATURED_FEE_VND")
                .map(c -> {
                    try { return Long.parseLong(c.getConfigValue()); }
                    catch (NumberFormatException e) { return 10000L; }
                }).orElse(10000L);

        long activeProducts = productRepository.findAllByIsDeleteFalse().size();
        // Giả sử có khoảng 20% sản phẩm sử dụng tính năng đẩy tin nổi bật
        long productFeaturedFees = (long) (activeProducts * 0.2) * featuredFee;

        // 4. Phí rút tiền: từ các Withdrawals có trạng thái 'Completed'
        double withdrawalPercent = systemConfigurationRepository.findByConfigKey("WITHDRAWAL_FEE_PERCENT")
                .map(c -> {
                    try { return Double.parseDouble(c.getConfigValue()); }
                    catch (NumberFormatException e) { return 1.5; }
                }).orElse(1.5);

        long minWithdrawFee = systemConfigurationRepository.findByConfigKey("MIN_WITHDRAW_FEE_VND")
                .map(c -> {
                    try { return Long.parseLong(c.getConfigValue()); }
                    catch (NumberFormatException e) { return 10000L; }
                }).orElse(10000L);

        List<Withdrawal> withdrawals = withdrawalRepository.findAll().stream()
                .filter(w -> !Boolean.TRUE.equals(w.getIsDelete()))
                .filter(w -> "Completed".equals(w.getStatus()))
                .toList();

        long withdrawalFees = withdrawals.stream().mapToLong(w -> {
            long calculatedFee = (long) (w.getAmountVnd() * (withdrawalPercent / 100.0));
            return Math.max(calculatedFee, minWithdrawFee);
        }).sum();

        // 5. Doanh thu ròng tổng cộng
        long netTotal = commissions + sellerUpgradeFees + productFeaturedFees + withdrawalFees;

        return RevenueSummaryResponse.builder()
                .commissions(commissions)
                .sellerUpgradeFees(sellerUpgradeFees)
                .productFeaturedFees(productFeaturedFees)
                .withdrawalFees(withdrawalFees)
                .netTotal(netTotal)
                .build();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getCashflowTransactions(Long operatorId, String keyword, String type, String time, int page, int size) {
        requireAdmin(operatorId);
        
        List<CashflowTransactionDto> allCashflow = getFilteredCashflowList(keyword, type, time);

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
    public byte[] exportRevenueCsv(Long operatorId, String keyword, String type, String time) {
        requireAdmin(operatorId);

        List<CashflowTransactionDto> transactions = getFilteredCashflowList(keyword, type, time);
        
        StringBuilder csv = new StringBuilder();
        // Byte Order Mark (BOM) for Excel UTF-8 support
        csv.append("\uFEFF");
        csv.append("STT,Mã GD,Thời gian,Tài khoản,Loại giao dịch,Số tiền (VNĐ),Phí sàn (VNĐ),Trạng thái\n");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        for (int i = 0; i < transactions.size(); i++) {
            CashflowTransactionDto tx = transactions.get(i);
            String txTypeLabel = "Deposit".equals(tx.getType()) ? "Nạp tiền" : ("Withdrawal".equals(tx.getType()) ? "Rút tiền" : "Giao dịch C2C");
            String statusLabel = "Completed".equals(tx.getStatus()) ? "Hoàn tất" : ("Held".equals(tx.getStatus()) ? "Đang giữ" : ("Pending".equals(tx.getStatus()) ? "Đang xử lý" : "Thất bại"));
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

    private List<CashflowTransactionDto> getFilteredCashflowList(String keyword, String type, String time) {
        double withdrawalPercent = systemConfigurationRepository.findByConfigKey("WITHDRAWAL_FEE_PERCENT")
                .map(c -> {
                    try { return Double.parseDouble(c.getConfigValue()); }
                    catch (NumberFormatException e) { return 1.5; }
                }).orElse(1.5);

        long minWithdrawFee = systemConfigurationRepository.findByConfigKey("MIN_WITHDRAW_FEE_VND")
                .map(c -> {
                    try { return Long.parseLong(c.getConfigValue()); }
                    catch (NumberFormatException e) { return 10000L; }
                }).orElse(10000L);

        List<CashflowTransactionDto> allCashflow = new ArrayList<>();

        // 1. Nạp tiền (TopupTransactions)
        List<TopupTransaction> topups = topupTransactionRepository.findAll().stream()
                .filter(t -> !Boolean.TRUE.equals(t.getIsDelete()))
                .toList();
        for (TopupTransaction t : topups) {
            String email = userRepository.findById(t.getUserId()).map(User::getEmail).orElse("unknown@mmo.com");
            allCashflow.add(CashflowTransactionDto.builder()
                    .id("DEP" + t.getId())
                    .timestamp(t.getCreatedAt())
                    .email(email)
                    .type("Deposit")
                    .amount(t.getAmountVnd())
                    .fee(0L) // Nạp tiền không mất phí nạp
                    .status("Success".equalsIgnoreCase(t.getStatus()) || "Completed".equalsIgnoreCase(t.getStatus()) ? "Completed" : t.getStatus())
                    .build());
        }

        // 2. Rút tiền (Withdrawals)
        List<Withdrawal> withdrawals = withdrawalRepository.findAll().stream()
                .filter(w -> !Boolean.TRUE.equals(w.getIsDelete()))
                .toList();
        for (Withdrawal w : withdrawals) {
            long calculatedFee = (long) (w.getAmountVnd() * (withdrawalPercent / 100.0));
            long fee = Math.max(calculatedFee, minWithdrawFee);
            String status = "Rejected".equalsIgnoreCase(w.getStatus()) ? "Failed" : w.getStatus();

            allCashflow.add(CashflowTransactionDto.builder()
                    .id("WTH" + w.getId())
                    .timestamp(w.getCreatedAt())
                    .email(w.getSeller().getEmail())
                    .type("Withdrawal")
                    .amount(w.getAmountVnd())
                    .fee(fee)
                    .status(status)
                    .build());
        }

        // 3. Giao dịch mua bán C2C (Transactions)
        List<Transaction> transactions = transactionRepository.findAll().stream()
                .filter(t -> !Boolean.TRUE.equals(t.getIsDelete()))
                .toList();
        for (Transaction t : transactions) {
            String status = t.getStatus();
            if ("Refunded".equalsIgnoreCase(status) || "Cancelled".equalsIgnoreCase(status)) {
                status = "Failed";
            }
            allCashflow.add(CashflowTransactionDto.builder()
                    .id("TX" + t.getId())
                    .timestamp(t.getCreatedAt())
                    .email(t.getCustomer().getEmail())
                    .type("C2C_Purchase")
                    .amount(t.getAmountVnd())
                    .fee(t.getCommissionVnd())
                    .status(status)
                    .build());
        }

        // Áp dụng bộ lọc
        String kw = keyword != null ? keyword.trim().toLowerCase(Locale.ROOT) : "";
        String tp = type != null ? type.trim() : "";
        String tm = time != null ? time.trim() : "";

        return allCashflow.stream()
                .filter(tx -> kw.isEmpty() || tx.getId().toLowerCase(Locale.ROOT).contains(kw) || tx.getEmail().toLowerCase(Locale.ROOT).contains(kw))
                .filter(tx -> tp.isEmpty() || tx.getType().equalsIgnoreCase(tp))
                .filter(tx -> {
                    if (tm.isEmpty()) return true;
                    LocalDateTime limit = LocalDateTime.now();
                    if ("today".equalsIgnoreCase(tm)) {
                        limit = LocalDate.now().atStartOfDay();
                    } else if ("7days".equalsIgnoreCase(tm)) {
                        limit = LocalDate.now().minusDays(7).atStartOfDay();
                    } else if ("30days".equalsIgnoreCase(tm)) {
                        limit = LocalDate.now().minusDays(30).atStartOfDay();
                    }
                    return tx.getTimestamp().isAfter(limit);
                })
                .sorted(Comparator.comparing(CashflowTransactionDto::getTimestamp).reversed())
                .collect(Collectors.toList());
    }
}
