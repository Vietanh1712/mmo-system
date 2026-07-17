package com.mmo.feature.seller.service;

import com.mmo.shared.dal.ComplaintRepository;
import com.mmo.shared.dal.TransactionRepository;
import com.mmo.shared.dal.UserRepository;
import com.mmo.shared.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service tự động đánh giá và cập nhật cấp độ gian hàng (Shop Level) theo nghiệp vụ:
 * - Level 0 (Cảnh Cáo): Tỷ lệ khiếu nại đúng tổng thể >= 2%
 * - Level 1 (Mới):      Tuổi shop < 30 ngày HOẶC số đơn thành công < 20
 * - Level 2 (Uy Tín):   Tuổi >= 30 ngày VÀ đơn >= 20 VÀ tỷ lệ lỗi < 2%
 *
 * Công thức: tỷ lệ lỗi = (Số đơn bị khiếu nại đúng / Tổng đơn đã bán) × 100
 */
@Service
@Slf4j
public class ShopLevelService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    /**
     * Đánh giá lại cấp độ gian hàng cho một Seller cụ thể.
     * Gọi sau mỗi lần Staff resolve khiếu nại hoặc theo cron hàng ngày.
     */
    @Transactional
    public void evaluateSellerLevel(Long sellerId) {
        User seller = userRepository.findByIdAndIsDeleteFalse(sellerId)
                .orElseThrow(() -> new IllegalArgumentException("Seller not found: " + sellerId));

        if (seller.getRole() == null || !seller.getRole().toLowerCase().contains("seller")) {
            return;
        }

        // --- Tính toán các chỉ số ---
        long totalSold = transactionRepository.countTotalSalesBySeller(seller);
        long resolvedComplaints = complaintRepository.countResolvedComplaintsBySeller(seller);
        long completedCount = transactionRepository.countCompletedSalesBySeller(seller);

        double disputeRate = totalSold > 0 ? (double) resolvedComplaints / totalSold : 0.0;

        boolean isNewByAge = seller.getCreatedAt() != null &&
                java.time.Duration.between(seller.getCreatedAt(), LocalDateTime.now()).toDays() < 30;
        boolean isNewByOrders = completedCount < 20;

        // --- Xác định Level mới ---
        int newLevel;
        if (disputeRate >= 0.02) {
            newLevel = 0; // Level 0: Tỷ lệ lỗi >= 2%
        } else if (isNewByAge || isNewByOrders) {
            newLevel = 1; // Level 1: Chưa đủ điều kiện uy tín
        } else {
            newLevel = 2; // Level 2: Đủ điều kiện Uy Tín
        }

        int currentLevel = seller.getShopLevel() != null ? seller.getShopLevel() : 1;

        if (newLevel != currentLevel) {
            seller.setShopLevel(newLevel);
            userRepository.save(seller);
            log.info("Shop Level thay đổi: Seller ID {} | {} → Level {} (disputeRate={:.2f}%, completedOrders={}, ageDays={})",
                    sellerId, currentLevel, newLevel,
                    disputeRate * 100, completedCount,
                    seller.getCreatedAt() != null
                            ? java.time.Duration.between(seller.getCreatedAt(), LocalDateTime.now()).toDays()
                            : -1);
        }

        // Cập nhật trạng thái khóa/mở khóa dựa trên cấp độ và số dư mới
        updateShopLockStatus(sellerId);
    }

    /**
     * Cron job chạy hàng ngày lúc 00:00 để đánh giá lại toàn bộ Seller.
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void evaluateAllSellers() {
        List<User> users = userRepository.findAll();
        int updated = 0;
        for (User user : users) {
            if (user.getRole() != null
                    && user.getRole().toLowerCase().contains("seller")
                    && Boolean.FALSE.equals(user.getIsDelete())) {
                try {
                    evaluateSellerLevel(user.getId());
                    updated++;
                } catch (Exception e) {
                    log.error("Lỗi khi đánh giá Seller ID {}: {}", user.getId(), e.getMessage());
                }
            }
        }
        log.info("Đánh giá Shop Level hoàn tất. Số Seller được xử lý: {}", updated);
    }

    @Transactional
    public void updateShopLockStatus(Long sellerId) {
        User seller = userRepository.findByIdAndIsDeleteFalse(sellerId).orElse(null);
        if (seller == null) {
            return;
        }

        if (seller.getRole() == null || !seller.getRole().toLowerCase().contains("seller")) {
            return;
        }

        long balance = seller.getBalanceVnd() != null ? seller.getBalanceVnd() : 0L;
        int level = seller.getShopLevel() != null ? seller.getShopLevel() : 1;

        if (balance < 0) {
            if (level == 0 || level == 1) {
                if (!"Locked".equalsIgnoreCase(seller.getShopStatus())) {
                    seller.setShopStatus("Locked");
                    userRepository.save(seller);
                    log.info("Ví âm (balance={}) đối với Shop Level {}: Tự động KHÓA shop Seller ID {}", balance, level, sellerId);
                }
            } else if (level == 2) {
                if (seller.getWithdrawalLocked() == null || !seller.getWithdrawalLocked()) {
                    seller.setWithdrawalLocked(true);
                    userRepository.save(seller);
                    log.info("Ví âm (balance={}) đối với Shop Level 2: Tự động KHÓA rút tiền Seller ID {}", balance, sellerId);
                }
            }
        } else {
            if ("Locked".equalsIgnoreCase(seller.getShopStatus())) {
                seller.setShopStatus("Active");
                userRepository.save(seller);
                log.info("Ví hết âm (balance={}): Tự động MỞ KHÓA shop Seller ID {}", balance, sellerId);
            }
            if (level == 2 && Boolean.TRUE.equals(seller.getWithdrawalLocked())) {
                seller.setWithdrawalLocked(false);
                userRepository.save(seller);
                log.info("Ví hết âm (balance={}) đối với Shop Level 2: Tự động MỞ KHÓA rút tiền Seller ID {}", balance, sellerId);
            }
        }
    }

    // Giữ lại method cũ để không phá vỡ các caller còn sót
    @Transactional
    @Deprecated
    public void evaluateSellerFlags(Long sellerId) {
        evaluateSellerLevel(sellerId);
    }
}
