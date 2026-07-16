package com.mmo.feature.wallet.service;
import com.mmo.shared.model.Transaction;
import com.mmo.feature.auth.service.AuthenticationService;

import com.mmo.shared.dal.SellerBankInfoRepository;
import com.mmo.shared.dal.SystemConfigurationRepository;
import com.mmo.shared.dal.UserRepository;
import com.mmo.shared.dal.WithdrawalRepository;
import com.mmo.shared.model.SellerBankInfo;
import com.mmo.shared.model.User;
import com.mmo.shared.model.Withdrawal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class WithdrawalService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WithdrawalRepository withdrawalRepository;

    @Autowired
    private SellerBankInfoRepository sellerBankInfoRepository;

    @Autowired
    private SystemConfigurationRepository systemConfigurationRepository;

    @Autowired
    private AuthenticationService authenticationService;

    /**
     * Thực hiện yêu cầu rút tiền của Seller bọc trong Transaction.
     */
    @Transactional
    public Map<String, Object> requestWithdrawal(Long userId, Long amount, String otp) {
        if (userId == null) {
            throw new IllegalArgumentException("Phiên đăng nhập không hợp lệ.");
        }

        User seller = userRepository.findByIdAndIsDeleteFalse(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin tài khoản."));

        if (seller.getWithdrawalLocked() != null && seller.getWithdrawalLocked()) {
            throw new IllegalArgumentException("Ví của bạn đang bị khóa do vi phạm. Không thể thực hiện rút tiền lúc này.");
        }

        // Load configurations dynamically
        double withdrawalFeePercent = systemConfigurationRepository.findByConfigKey("WITHDRAWAL_FEE_PERCENT")
                .map(c -> {
                    try { return Double.parseDouble(c.getConfigValue()); }
                    catch (NumberFormatException e) { return 1.5; }
                }).orElse(1.5);

        long minWithdrawFee = systemConfigurationRepository.findByConfigKey("MIN_WITHDRAW_FEE_VND")
                .map(c -> {
                    try { return Long.parseLong(c.getConfigValue()); }
                    catch (NumberFormatException e) { return 10000L; }
                }).orElse(10000L);

        long minWithdrawalLimit = systemConfigurationRepository.findByConfigKey("MIN_WITHDRAWAL_VND")
                .map(c -> {
                    try { return Long.parseLong(c.getConfigValue()); }
                    catch (NumberFormatException e) { return 50000L; }
                }).orElse(50000L);

        long maxWithdrawalLimit = systemConfigurationRepository.findByConfigKey("MAX_WITHDRAWAL_VND")
                .map(c -> {
                    try { return Long.parseLong(c.getConfigValue()); }
                    catch (NumberFormatException e) { return 50000000L; }
                }).orElse(50000000L);

        boolean requireWithdraw2FA = false; // Tắt yêu cầu OTP xác thực khi rút tiền

        // Validate amounts
        if (amount < minWithdrawalLimit) {
            throw new IllegalArgumentException("Số tiền rút tối thiểu phải là " + String.format("%,d", minWithdrawalLimit) + " VNĐ.");
        }
        if (amount > maxWithdrawalLimit) {
            throw new IllegalArgumentException("Số tiền rút tối đa phải là " + String.format("%,d", maxWithdrawalLimit) + " VNĐ.");
        }

        // Calculate fee
        long fee = (long) (amount * (withdrawalFeePercent / 100.0));
        if (fee < minWithdrawFee) {
            fee = minWithdrawFee;
        }

        long totalDeduction = amount + fee;
        if (seller.getBalanceVnd() == null || seller.getBalanceVnd() < totalDeduction) {
            throw new IllegalArgumentException("Số dư ví không đủ để thực hiện yêu cầu rút tiền này (cần " + String.format("%,d", totalDeduction) + " VNĐ bao gồm cả phí).");
        }

        // Enforce 2FA Verification if active
        if (requireWithdraw2FA) {
            if (otp == null || otp.trim().isEmpty()) {
                throw new IllegalArgumentException("Giao dịch rút tiền yêu cầu xác thực 2FA. Vui lòng nhập mã OTP.");
            }
            authenticationService.verifyWithdrawalOtp(seller.getId(), otp.trim());
        }

        SellerBankInfo bank = sellerBankInfoRepository.findByUserAndIsDeleteFalse(seller)
                .orElseThrow(() -> new IllegalArgumentException("Vui lòng cấu hình thông tin ngân hàng trước khi rút tiền."));

        // Deduct total balance (amount + fee) and save
        seller.setBalanceVnd(seller.getBalanceVnd() - totalDeduction);
        userRepository.save(seller);

        Withdrawal w = new Withdrawal();
        w.setSeller(seller);
        w.setBankInfo(bank);
        w.setAmountVnd(amount);
        w.setFeeVnd(fee);
        w.setStatus("Pending");
        w.setIsDelete(false);
        withdrawalRepository.save(w);

        Map<String, Object> result = new HashMap<>();
        result.put("newBalance", seller.getBalanceVnd());
        result.put("message", "Yêu cầu rút tiền đã được gửi thành công!");
        return result;
    }

    private String normalizeRole(String roleValue) {
        if (roleValue == null || roleValue.isBlank()) {
            return "customer";
        }
        return roleValue.toLowerCase(Locale.ROOT);
    }
}
