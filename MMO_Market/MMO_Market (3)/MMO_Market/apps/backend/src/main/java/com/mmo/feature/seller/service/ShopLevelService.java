package com.mmo.feature.seller.service;

import com.mmo.shared.dal.ComplaintRepository;
import com.mmo.shared.dal.UserRepository;
import com.mmo.shared.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class ShopLevelService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ComplaintRepository complaintRepository;

    // Logic to evaluate a single seller's flags (can be called manually or after a complaint is resolved)
    @Transactional
    public void evaluateSellerFlags(Long sellerId) {
        User seller = userRepository.findByIdAndIsDeleteFalse(sellerId)
                .orElseThrow(() -> new IllegalArgumentException("Seller not found"));

        if (seller.getRole() == null || !seller.getRole().toLowerCase().contains("seller")) {
            return;
        }

        LocalDate now = LocalDate.now();
        long complaintsThisMonth = complaintRepository.countComplaintsBySellerAndMonth(seller, now.getYear(), now.getMonthValue());

        // Default Reset
        seller.setWithdrawalLocked(false);
        if (seller.getShopLevel() != null && seller.getShopLevel() == 0) {
            // Check if they escape Level 0 (for simplicity here, if complaints drop or we just let staff handle escape)
            // User requested: escape when defect rate drops. We don't have defect rate easily calculated here, 
            // but we can remove restrictions if complaints <= 5.
            if (complaintsThisMonth <= 5) {
                seller.setShopLevel(1); // Return to Level 1
            }
        }

        // Apply Flags based on complaints
        if (complaintsThisMonth > 10) {
            // Flag 2: Temporary wallet freeze, cannot post products (level 0)
            seller.setWithdrawalLocked(true);
            seller.setShopLevel(0);
        } else if (complaintsThisMonth > 5) {
            // Flag 1: Warning, restrict withdrawal
            seller.setWithdrawalLocked(true);
            // Optional: keep shopLevel 1 or 2, but restricted
        }

        // Flag 3 logic (Permanent Ban): Usually handled by Staff manually when they see the flags, 
        // but if we automate: e.g. >15 complaints = 3 flags
        if (complaintsThisMonth > 15) {
            seller.setFlag3Count((seller.getFlag3Count() != null ? seller.getFlag3Count() : 0) + 1);
            if (seller.getFlag3Count() > 1) {
                seller.setIsLocked(true);
                seller.setShopStatus("Banned");
            } else {
                seller.setIsLocked(true);
                seller.setShopStatus("Banned"); // First time 3 flags, still locked, but can be unlocked by admin
            }
        }

        userRepository.save(seller);
    }

    // Cron job to evaluate all sellers (e.g. daily at midnight)
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void evaluateAllSellers() {
        // Fetch all sellers
        // Note: For large systems, this should be paginated
        List<User> users = userRepository.findAll(); // Simplified for now
        for (User user : users) {
            if (user.getRole() != null && user.getRole().toLowerCase().contains("seller") && !user.getIsDelete()) {
                evaluateSellerFlags(user.getId());
            }
        }
    }
}
