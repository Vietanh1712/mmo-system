package com.mmo.feature.wallet.service;

import com.mmo.shared.dal.NotificationRepository;
import com.mmo.shared.dal.SystemConfigurationRepository;
import com.mmo.shared.dal.TopupTransactionRepository;
import com.mmo.shared.dal.UserRepository;
import com.mmo.shared.dal.WalletTransactionRepository;
import com.mmo.shared.dto.SePayWebhookRequest;
import com.mmo.shared.dto.TopupResponseDto;
import com.mmo.shared.dto.TopupRetryRequestDto;
import com.mmo.shared.model.Notification;
import com.mmo.shared.model.TopupTransaction;
import com.mmo.shared.model.User;
import com.mmo.shared.model.WalletTransaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TopupService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TopupTransactionRepository topupTransactionRepository;

    @Autowired
    private WalletTransactionRepository walletTransactionRepository;

    @Autowired
    private WalletService walletService;

    @Autowired
    private SystemConfigurationRepository systemConfigurationRepository;

    @Autowired
    private com.mmo.feature.seller.service.ShopLevelService shopLevelService;

    @Autowired
    private NotificationRepository notificationRepository;

    private static final Pattern TRANSFER_CONTENT_PATTERN = Pattern.compile("MMO[\\s-]*TOPUP[\\s-]*(\\d+)", Pattern.CASE_INSENSITIVE);

    @EventListener(ApplicationReadyEvent.class)
    public void initTopupData() {
        try {
            long existingCount = topupTransactionRepository.countTotalTopups();
            log.info("Checking TopupTransactions in database. Current count: {}", existingCount);

            List<WalletTransaction> walletTopups = walletTransactionRepository.findAll().stream()
                    .filter(w -> "TOPUP".equalsIgnoreCase(w.getType()))
                    .collect(Collectors.toList());

            if (!walletTopups.isEmpty()) {
                for (WalletTransaction w : walletTopups) {
                    try {
                        String refCode = w.getReferenceId() != null ? String.valueOf(w.getReferenceId()) : ("WTX-" + w.getId());
                        if (topupTransactionRepository.findBySepayCode(refCode).isEmpty()) {
                            Long uId = (w.getUser() != null && w.getUser().getId() != null) ? w.getUser().getId() : 0L;
                            TopupTransaction tx = TopupTransaction.builder()
                                    .userId(uId)
                                    .amountVnd(w.getAmountVnd() != null ? w.getAmountVnd() : 0L)
                                    .sepayCode(refCode)
                                    .transferContent("MMO-TOPUP-" + (uId != 0L ? uId : ""))
                                    .balanceBefore(w.getBalanceAfter() != null && w.getAmountVnd() != null ? w.getBalanceAfter() - w.getAmountVnd() : 0L)
                                    .balanceAfter(w.getBalanceAfter() != null ? w.getBalanceAfter() : 0L)
                                    .status("SUCCESS".equalsIgnoreCase(w.getStatus()) ? "Success" : "Failed")
                                    .build();
                            topupTransactionRepository.save(tx);
                            log.info("Synced WalletTransaction #{} to TopupTransaction #{}", w.getId(), tx.getId());
                        }
                    } catch (Exception ex) {
                        log.warn("Could not sync WalletTransaction #{}: {}", w.getId(), ex.getMessage());
                    }
                }
            }

            if (topupTransactionRepository.countTotalTopups() == 0) {
                log.info("Seeding initial TopupTransactions into database...");
                User firstUser = userRepository.findAll().stream().findFirst().orElse(null);
                Long uId = (firstUser != null && firstUser.getId() != null) ? firstUser.getId() : 0L;

                TopupTransaction t1 = TopupTransaction.builder()
                        .userId(uId)
                        .amountVnd(500000L)
                        .sepayCode("SEPAY-20260723-001")
                        .transferContent("MMO TOPUP " + uId)
                        .balanceBefore(0L)
                        .balanceAfter(500000L)
                        .status("Success")
                        .build();

                TopupTransaction t2 = TopupTransaction.builder()
                        .userId(uId)
                        .amountVnd(200000L)
                        .sepayCode("SEPAY-20260723-002")
                        .transferContent("MMO-TOPUP-" + uId)
                        .balanceBefore(500000L)
                        .balanceAfter(700000L)
                        .status("Success")
                        .build();

                TopupTransaction t3 = TopupTransaction.builder()
                        .userId(0L)
                        .amountVnd(100000L)
                        .sepayCode("SEPAY-20260723-003")
                        .transferContent("NAP TIEN TK MMO CHUYEN KHOAN KHONG GO ID")
                        .failureReason("Nội dung chuyển khoản không đúng cú pháp MMO-TOPUP-<userID>")
                        .status("Failed")
                        .build();

                TopupTransaction t4 = TopupTransaction.builder()
                        .userId(uId)
                        .amountVnd(5000L)
                        .sepayCode("SEPAY-20260723-004")
                        .transferContent("MMO TOPUP " + uId)
                        .failureReason("Số tiền nạp (5,000 đ) nhỏ hơn hạn mức tối thiểu cấu hình (10,000 đ)")
                        .status("Failed")
                        .build();

                topupTransactionRepository.saveAll(List.of(t1, t2, t3, t4));
                log.info("Successfully seeded 4 initial TopupTransactions into database.");
            }
        } catch (Exception e) {
            log.error("Lỗi khởi tạo/đồng bộ dữ liệu nạp tiền: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public boolean processSepayWebhook(SePayWebhookRequest request) {
        log.info("Processing SePay Webhook transaction: {}, content: '{}', amount: {}", 
                request.getId(), request.getContent(), request.getTransferAmount());

        if (!"in".equalsIgnoreCase(request.getTransferType())) {
            log.warn("Ignored non-deposit transaction (transferType: {})", request.getTransferType());
            return false;
        }

        String sepayCode = String.valueOf(request.getId());
        if (topupTransactionRepository.findBySepayCode(sepayCode).isPresent()) {
            log.warn("SePay transaction {} already processed.", sepayCode);
            return true;
        }

        String content = request.getContent();
        Long amount = request.getTransferAmount();

        if (content == null || content.trim().isEmpty()) {
            log.warn("Transaction content is empty.");
            saveFailedTopup(sepayCode, amount, content, 0L, "Nội dung chuyển khoản rỗng.");
            return false;
        }

        Matcher matcher = TRANSFER_CONTENT_PATTERN.matcher(content);
        if (!matcher.find()) {
            log.warn("Transaction content '{}' does not match pattern 'MMO-TOPUP-<userId>'.", content);
            saveFailedTopup(sepayCode, amount, content, 0L, "Nội dung chuyển khoản không đúng cú pháp MMO-TOPUP-<userID>");
            return false;
        }

        String userIdStr = matcher.group(1);
        Long userId;
        try {
            userId = Long.parseLong(userIdStr);
        } catch (NumberFormatException e) {
            log.error("Failed to parse user ID: {}", userIdStr);
            saveFailedTopup(sepayCode, amount, content, 0L, "Không thể đọc ID người dùng từ cú pháp nội dung chuyển khoản: " + userIdStr);
            return false;
        }

        Optional<User> userOptional = userRepository.findByIdAndIsDeleteFalse(userId);
        if (userOptional.isEmpty()) {
            log.error("User with ID {} not found for top-up.", userId);
            saveFailedTopup(sepayCode, amount, content, userId, "Không tìm thấy người dùng ID " + userId + " trong hệ thống");
            return false;
        }

        User user = userOptional.get();

        if (amount == null || amount <= 0) {
            log.error("Invalid top-up amount: {}", amount);
            saveFailedTopup(sepayCode, amount, content, user.getId(), "Số tiền chuyển khoản không hợp lệ: " + amount);
            return false;
        }

        long minDeposit = systemConfigurationRepository.findByConfigKey("MIN_DEPOSIT_LIMIT_VND")
                .map(c -> {
                    try { return Long.parseLong(c.getConfigValue()); }
                    catch (NumberFormatException e) { return 10000L; }
                }).orElse(10000L);

        if (amount < minDeposit) {
            log.error("Top-up amount {} VND is below minimum deposit limit {} VND. User ID: {}", 
                    amount, minDeposit, user.getId());
            saveFailedTopup(sepayCode, amount, content, user.getId(), 
                    String.format("Số tiền nạp (%d đ) nhỏ hơn hạn mức tối thiểu cấu hình (%d đ)", amount, minDeposit));
            return false;
        }

        long maxDeposit = systemConfigurationRepository.findByConfigKey("MAX_DEPOSIT_LIMIT_VND")
                .map(c -> {
                    try { return Long.parseLong(c.getConfigValue()); }
                    catch (NumberFormatException e) { return 50000000L; }
                }).orElse(50000000L);

        if (amount > maxDeposit) {
            log.error("Top-up amount {} VND is above maximum deposit limit {} VND. User ID: {}", 
                    amount, maxDeposit, user.getId());
            saveFailedTopup(sepayCode, amount, content, user.getId(), 
                    String.format("Số tiền nạp (%d đ) vượt quá hạn mức tối đa cấu hình (%d đ)", amount, maxDeposit));
            return false;
        }

        Long oldBalance = user.getBalanceVnd() != null ? user.getBalanceVnd() : 0L;
        user.setBalanceVnd(oldBalance + amount);
        userRepository.save(user);

        shopLevelService.updateShopLockStatus(user.getId());

        TopupTransaction transaction = TopupTransaction.builder()
                .userId(user.getId())
                .amountVnd(amount)
                .sepayCode(sepayCode)
                .transferContent(content)
                .balanceBefore(oldBalance)
                .balanceAfter(user.getBalanceVnd())
                .status("Success")
                .build();
        topupTransactionRepository.save(transaction);

        walletService.recordTransaction(user, "TOPUP", amount, "SUCCESS", "Nạp tiền qua SePay", "SEPAY-" + sepayCode, user.getBalanceVnd());

        log.info("Successfully topped up {} VND for User ID {} ({}). New balance: {}", 
                amount, user.getId(), user.getEmail(), user.getBalanceVnd());

        return true;
    }

    private void saveFailedTopup(String sepayCode, Long amount, String content, Long userId, String reason) {
        try {
            if (userId == null || userId <= 0 || !userRepository.existsById(userId)) {
                log.warn("Cannot save failed top-up transaction to database because user ID {} does not exist. Reason: {}", userId, reason);
                return;
            }
            TopupTransaction tx = TopupTransaction.builder()
                    .sepayCode(sepayCode)
                    .amountVnd(amount != null ? amount : 0L)
                    .transferContent(content)
                    .userId(userId)
                    .status("Failed")
                    .failureReason(reason)
                    .build();
            topupTransactionRepository.save(tx);
            log.info("Saved Failed top-up transaction #{}, sepayCode: {}, reason: {}", tx.getId(), sepayCode, reason);
        } catch (Exception e) {
            log.error("Failed to save failed topup transaction: {}", e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public Page<TopupResponseDto> getAllTopups(String status, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        String cleanStatus = (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) ? null : status.trim();
        String cleanKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();

        Long searchId = null;
        Long searchUserId = null;
        if (cleanKeyword != null) {
            String cleanRaw = cleanKeyword.trim().replaceAll("^[#\\s]+", "");
            cleanRaw = cleanRaw.replaceFirst("^(?i)TOPUP[-_]*", "").replaceFirst("^(?i)TP[-_]*", "");
            try {
                Long parsedNum = Long.parseLong(cleanRaw);
                searchId = parsedNum;
                searchUserId = parsedNum;
            } catch (NumberFormatException ignored) {}
        }

        String cleanKeywordPattern = (cleanKeyword != null) ? ("%" + cleanKeyword.toLowerCase() + "%") : null;
        Page<TopupTransaction> pageResult = topupTransactionRepository.searchTopups(cleanStatus, cleanKeywordPattern, searchId, searchUserId, pageable);
        return pageResult.map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public TopupResponseDto getTopupById(Long id) {
        TopupTransaction tx = topupTransactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch nạp tiền #" + id));
        return mapToDto(tx);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getTopupStats() {
        Map<String, Object> stats = new HashMap<>();
        long totalTopups = topupTransactionRepository.countTotalTopups();
        long successTopups = topupTransactionRepository.countByStatusIgnoreCase("Success");
        long failedTopups = topupTransactionRepository.countByStatusIgnoreCase("Failed");
        long pendingTopups = topupTransactionRepository.countByStatusIgnoreCase("Pending");

        long totalTopupVnd = topupTransactionRepository.findAllByIsDeleteFalse().stream()
                .filter(t -> "Success".equalsIgnoreCase(t.getStatus()))
                .mapToLong(t -> t.getAmountVnd() != null ? t.getAmountVnd() : 0L)
                .sum();

        stats.put("totalTopups", totalTopups);
        stats.put("successTopups", successTopups);
        stats.put("failedTopups", failedTopups);
        stats.put("pendingTopups", pendingTopups);
        stats.put("totalTopupVnd", totalTopupVnd);
        return stats;
    }

    @Transactional
    public TopupResponseDto retryTopup(Long topupId, TopupRetryRequestDto dto, Long staffUserId) {
        TopupTransaction tx = topupTransactionRepository.findById(topupId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch nạp tiền #" + topupId));

        if ("Success".equalsIgnoreCase(tx.getStatus())) {
            throw new IllegalStateException("Giao dịch nạp tiền này đã thành công trước đó, không thể kích hoạt lại.");
        }

        Long targetUserId = (dto != null && dto.getTargetUserId() != null) ? dto.getTargetUserId() : tx.getUserId();
        if (targetUserId == null || targetUserId <= 0) {
            throw new IllegalArgumentException("Vui lòng nhập ID người dùng hợp lệ để cộng tiền.");
        }

        User user = userRepository.findByIdAndIsDeleteFalse(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng ID " + targetUserId));

        Long amount = tx.getAmountVnd();
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Số tiền giao dịch nạp không hợp lệ.");
        }

        boolean skipMinCheck = (dto != null && Boolean.TRUE.equals(dto.getSkipMinCheck()));
        if (!skipMinCheck) {
            long minDeposit = systemConfigurationRepository.findByConfigKey("MIN_DEPOSIT_LIMIT_VND")
                    .map(c -> {
                        try { return Long.parseLong(c.getConfigValue()); }
                        catch (NumberFormatException e) { return 10000L; }
                    }).orElse(10000L);
            if (amount < minDeposit) {
                throw new IllegalArgumentException(String.format("Số tiền nạp (%d đ) nhỏ hơn hạn mức tối thiểu (%d đ). Vui lòng tích chọn 'Bỏ qua kiểm tra hạn mức' nếu muốn duyệt đặc biệt.", amount, minDeposit));
            }
        }

        User staffUser = userRepository.findById(staffUserId).orElse(null);
        String staffName = staffUser != null ? (staffUser.getFullName() != null ? staffUser.getFullName() : staffUser.getEmail()) : "Staff #" + staffUserId;
        String note = (dto != null && dto.getStaffNote() != null && !dto.getStaffNote().isBlank()) 
                ? dto.getStaffNote().trim() : "Duyệt nạp tiền thủ công bởi Staff";

        Long oldBalance = user.getBalanceVnd() != null ? user.getBalanceVnd() : 0L;
        user.setBalanceVnd(oldBalance + amount);
        userRepository.save(user);

        shopLevelService.updateShopLockStatus(user.getId());

        walletService.recordTransaction(
                user,
                "TOPUP",
                amount,
                "SUCCESS",
                "Duyệt nạp tiền thủ công bởi Staff [" + staffName + "]. Ghi chú: " + note,
                "MANUAL-RETRY-" + tx.getSepayCode(),
                user.getBalanceVnd()
        );

        tx.setUserId(user.getId());
        tx.setStatus("Success");
        tx.setBalanceBefore(oldBalance);
        tx.setBalanceAfter(user.getBalanceVnd());
        tx.setStaffNote(note);
        tx.setProcessedByStaffId(staffUserId);
        topupTransactionRepository.save(tx);

        try {
            Notification notif = Notification.builder()
                    .userId(user.getId())
                    .title("Nạp tiền thành công (Duyệt thủ công)")
                    .content(String.format("Giao dịch nạp tiền #%d (số tiền %d đ) của bạn đã được Staff [%s] kiểm tra và duyệt thành công vào tài khoản.", 
                            tx.getId(), amount, staffName))
                    .type("SYSTEM")
                    .severity("SUCCESS")
                    .isRead(false)
                    .isDelete(false)
                    .targetUrl("/account/profile")
                    .build();
            notificationRepository.save(notif);
        } catch (Exception e) {
            log.error("Lỗi tạo thông báo duyệt nạp tiền thủ công: {}", e.getMessage());
        }

        log.info("Staff {} successfully retried top-up #{} for User ID {}. Amount: {} VND", 
                staffName, tx.getId(), user.getId(), amount);

        return mapToDto(tx);
    }

    public TopupResponseDto mapToDto(TopupTransaction tx) {
        if (tx == null) return null;

        String userFullName = null;
        String userEmail = null;
        Long displayUserId = (tx.getUserId() != null && tx.getUserId() > 0L) ? tx.getUserId() : null;

        if (displayUserId != null) {
            User user = userRepository.findById(displayUserId).orElse(null);
            if (user != null) {
                userFullName = user.getFullName();
                userEmail = user.getEmail();
            }
        }

        String staffName = null;
        if (tx.getProcessedByStaffId() != null) {
            User staff = userRepository.findById(tx.getProcessedByStaffId()).orElse(null);
            if (staff != null) {
                staffName = staff.getFullName() != null ? staff.getFullName() : staff.getEmail();
            }
        }

        return TopupResponseDto.builder()
                .id(tx.getId())
                .userId(displayUserId)
                .userFullName(userFullName)
                .userEmail(userEmail)
                .amountVnd(tx.getAmountVnd())
                .sepayCode(tx.getSepayCode())
                .status(tx.getStatus())
                .transferContent(tx.getTransferContent())
                .balanceBefore(tx.getBalanceBefore())
                .balanceAfter(tx.getBalanceAfter())
                .failureReason(tx.getFailureReason())
                .staffNote(tx.getStaffNote())
                .processedByStaffId(tx.getProcessedByStaffId())
                .processedByStaffName(staffName)
                .createdAt(tx.getCreatedAt())
                .build();
    }
}
