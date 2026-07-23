package com.mmo.feature.wallet.controller.api;

import com.mmo.shared.dto.WalletStatsDto;
import com.mmo.shared.dto.WalletTransactionDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.mmo.feature.wallet.service.WalletService;
import com.mmo.feature.wallet.service.WithdrawalService;
import com.mmo.shared.dal.SellerBankInfoRepository;
import com.mmo.shared.dal.SystemConfigurationRepository;
import com.mmo.shared.dal.UserRepository;
import com.mmo.shared.dal.WithdrawalRepository;
import com.mmo.shared.model.SellerBankInfo;
import com.mmo.shared.model.User;
import com.mmo.shared.model.Withdrawal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/wallet")
public class WalletController {

    @Autowired
    private WalletService walletService;

    @Autowired
    private WithdrawalService withdrawalService;

    @Autowired
    private SellerBankInfoRepository sellerBankInfoRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WithdrawalRepository withdrawalRepository;

    @Autowired
    private SystemConfigurationRepository systemConfigurationRepository;

    @GetMapping("/stats")
    public ResponseEntity<?> getWalletStats(@AuthenticationPrincipal Long userId) {
        if (userId == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        WalletStatsDto stats = walletService.getWalletStats(userId);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/transactions")
    public ResponseEntity<?> getWalletTransactions(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        if (userId == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        
        Pageable pageable = PageRequest.of(page, size);
        Page<WalletTransactionDto> transactions = walletService.getTransactions(userId, pageable);
        return ResponseEntity.ok(transactions);
    }

    // --- BANK INFO ---

    @GetMapping("/bank-info")
    public ResponseEntity<?> getBankInfo(@AuthenticationPrincipal Long userId) {
        if (userId == null) return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        try {
            User user = userRepository.findByIdAndIsDeleteFalse(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng."));
            SellerBankInfo bank = sellerBankInfoRepository.findFirstByUserAndIsDeleteFalseOrderByIdDesc(user).orElse(null);

            Map<String, Object> result = new HashMap<>();
            result.put("bankName", bank != null ? bank.getBankName() : "");
            result.put("accountNumber", bank != null ? bank.getAccountNumber() : "");
            result.put("accountHolder", user.getFullName().toUpperCase());
            result.put("branch", bank != null ? bank.getBranch() : "");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/bank-info")
    public ResponseEntity<?> updateBankInfo(@AuthenticationPrincipal Long userId, @RequestBody Map<String, String> request) {
        if (userId == null) return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        try {
            User user = userRepository.findByIdAndIsDeleteFalse(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng."));
            String bankName = request.get("bankName");
            String accountNumber = request.get("accountNumber");
            String branch = request.get("branch");

            if (bankName == null || bankName.trim().isEmpty() || accountNumber == null || accountNumber.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Tên ngân hàng và số tài khoản không được để trống."));
            }

            SellerBankInfo bank = sellerBankInfoRepository.findFirstByUserAndIsDeleteFalseOrderByIdDesc(user)
                    .orElse(new SellerBankInfo());
            bank.setUser(user);
            bank.setBankName(bankName);
            bank.setAccountNumber(accountNumber);
            bank.setBranch(branch);
            sellerBankInfoRepository.save(bank);

            return ResponseEntity.ok(Map.of("message", "Cập nhật thông tin ngân hàng thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // --- WITHDRAWALS ---

    @GetMapping("/withdrawals")
    public ResponseEntity<?> getWithdrawals(@AuthenticationPrincipal Long userId) {
        if (userId == null) return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        try {
            User user = userRepository.findByIdAndIsDeleteFalse(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng."));
            List<Withdrawal> withdrawals = withdrawalRepository.findBySellerAndIsDeleteFalseOrderByCreatedAtDesc(user);

            List<Map<String, Object>> result = withdrawals.stream().map(w -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", w.getId());
                map.put("amountVnd", w.getAmountVnd());
                map.put("feeVnd", w.getFeeVnd());
                map.put("bankName", w.getBankInfo().getBankName());
                map.put("accountNumber", w.getBankInfo().getAccountNumber());
                map.put("status", w.getStatus());
                map.put("proofFile", w.getProofFile() != null ? w.getProofFile() : "");
                map.put("createdAt", w.getCreatedAt().toString());
                return map;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/withdrawals")
    public ResponseEntity<?> requestWithdrawal(@AuthenticationPrincipal Long userId, @RequestBody Map<String, Object> request) {
        if (userId == null) return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        try {
            Object amountObj = request.get("amountVnd");
            if (amountObj == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Vui lòng nhập số tiền rút."));
            }
            long amount = Long.parseLong(amountObj.toString());
            Object otpObj = request.get("otp");
            String otp = otpObj != null ? otpObj.toString() : null;

            Map<String, Object> result = withdrawalService.requestWithdrawal(userId, amount, otp);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/withdrawals/config")
    public ResponseEntity<?> getWithdrawalConfig(@AuthenticationPrincipal Long userId) {
        if (userId == null) return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        try {
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

            return ResponseEntity.ok(Map.of(
                    "withdrawalFeePercent", withdrawalFeePercent,
                    "minWithdrawFee", minWithdrawFee,
                    "minWithdrawalLimit", minWithdrawalLimit,
                    "maxWithdrawalLimit", maxWithdrawalLimit,
                    "requireWithdraw2FA", false
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
