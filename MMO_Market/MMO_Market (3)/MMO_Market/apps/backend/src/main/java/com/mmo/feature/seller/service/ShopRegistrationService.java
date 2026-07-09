package com.mmo.feature.seller.service;
import com.mmo.shared.model.Transaction;
import com.mmo.shared.model.Category;
import com.mmo.shared.model.Review;

import com.mmo.shared.dto.ShopRegistrationRequestDto;
import com.mmo.shared.dto.ShopRegistrationResponseDto;
import com.mmo.shared.dto.ShopRegistrationReviewDto;
import com.mmo.shared.dal.KycRequestRepository;
import com.mmo.shared.dal.SellerRegistrationRepository;
import com.mmo.shared.dal.UserRepository;
import com.mmo.shared.model.KycStatus;
import com.mmo.shared.model.SellerRegistration;
import com.mmo.shared.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ShopRegistrationService {

    @Autowired
    private SellerRegistrationRepository sellerRegistrationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private KycRequestRepository kycRequestRepository;

    @Transactional
    public ShopRegistrationResponseDto submitRegistration(Long userId, ShopRegistrationRequestDto request) {
        User user = userRepository.findByIdAndIsDeleteFalse(userId)
                .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại."));

        // Validate KYC Approved
        boolean hasApprovedKyc = kycRequestRepository.findByUser_IdAndIsDeleteFalseOrderByCreatedAtDesc(userId)
                .stream()
                .anyMatch(kycRequest -> kycRequest.getStatus() == KycStatus.APPROVED);
        if (!hasApprovedKyc) {
            throw new IllegalStateException("Bạn phải hoàn tất xác minh danh tính (KYC) trước khi đăng ký Shop.");
        }

        SellerRegistration registration = sellerRegistrationRepository.findByUserAndIsDeleteFalse(user)
                .orElse(new SellerRegistration());

        if ("APPROVED".equals(registration.getStatus())) {
            throw new IllegalStateException("Bạn đã có Shop đang hoạt động.");
        }

        registration.setUser(user);
        registration.setShopName(request.getShopName());
        registration.setDescription(request.getDescription());
        registration.setCategory(request.getCategory());
        registration.setSupportEmail(request.getSupportEmail());
        registration.setSupportPhone(request.getSupportPhone());
        registration.setStatus("PENDING");

        SellerRegistration saved = sellerRegistrationRepository.save(registration);
        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public ShopRegistrationResponseDto getMyRegistration(Long userId) {
        User user = userRepository.findByIdAndIsDeleteFalse(userId)
                .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại."));

        return sellerRegistrationRepository.findByUserAndIsDeleteFalse(user)
                .map(this::mapToDto)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<ShopRegistrationResponseDto> getAllPendingRegistrations() {
        return sellerRegistrationRepository.findAllByIsDeleteFalseOrderByCreatedAtDesc().stream()
                .filter(reg -> "PENDING".equals(reg.getStatus()))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<ShopRegistrationResponseDto> getAllRegistrations(String status, String shopStatus, String keyword, int page, int size) {
        org.springframework.data.domain.Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        String cleanStatus = (status == null || status.isBlank()) ? null : status.trim();
        String cleanShopStatus = (shopStatus == null || shopStatus.isBlank()) ? null : shopStatus.trim();
        String cleanKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();

        Long searchId = null;
        if (cleanKeyword != null) {
            String kwUpper = cleanKeyword.toUpperCase();
            if (kwUpper.startsWith("SHOP-")) {
                try {
                    searchId = Long.parseLong(kwUpper.substring(5).trim());
                } catch (NumberFormatException ignored) {}
            } else {
                try {
                    searchId = Long.parseLong(cleanKeyword);
                } catch (NumberFormatException ignored) {}
            }
        }

        Page<SellerRegistration> regPage = sellerRegistrationRepository.searchRegistrations(cleanStatus, cleanShopStatus, cleanKeyword, searchId, pageable);
        return regPage.map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getRegistrationStats() {
        Map<String, Long> stats = new HashMap<>();

        long totalShops = userRepository.countTotalShops();
        long activeShops = userRepository.countActiveShops();
        long bannedShops = userRepository.countBannedShops();
        long totalDeposit = userRepository.sumTotalDeposit();

        stats.put("totalShops", totalShops);
        stats.put("activeShops", activeShops);
        stats.put("bannedShops", bannedShops);
        stats.put("totalDeposit", totalDeposit);
        return stats;
    }

    @Transactional
    public ShopRegistrationResponseDto reviewRegistration(Long registrationId, ShopRegistrationReviewDto review) {
        SellerRegistration registration = sellerRegistrationRepository.findById(registrationId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu đăng ký Shop."));

        if (!"PENDING".equals(registration.getStatus())) {
            throw new IllegalStateException("Chỉ có thể duyệt yêu cầu ở trạng thái PENDING.");
        }

        if (review.isApproved()) {
            registration.setStatus("APPROVED");

            // Nâng cấp role thành Seller
            User user = registration.getUser();
            String currentRole = user.getRole();
            if (currentRole == null || currentRole.contains("\"Customer\"")) {
                user.setRole("{\"role\": \"Seller\"}");
                userRepository.save(user);
            }
        } else {
            registration.setStatus("REJECTED");
            registration.setRejectionReason(review.getReason());
        }

        SellerRegistration updated = sellerRegistrationRepository.save(registration);
        return mapToDto(updated);
    }

    private ShopRegistrationResponseDto mapToDto(SellerRegistration registration) {
        User user = registration.getUser();
        return ShopRegistrationResponseDto.builder()
                .id(registration.getId())
                .status(registration.getStatus())
                .code("SHOP-" + String.format("%06d", registration.getId() != null ? registration.getId() : 0))
                .submittedAt(registration.getCreatedAt() != null ? registration.getCreatedAt().toString() : null)
                .shopName(registration.getShopName())
                .category(registration.getCategory())
                .description(registration.getDescription())
                .supportEmail(registration.getSupportEmail())
                .supportPhone(registration.getSupportPhone())
                .rejectionReason(registration.getRejectionReason())
                .shopStatus(user != null ? user.getShopStatus() : null)
                .depositVnd(user != null ? user.getDepositVnd() : 0L)
                .balanceVnd(user != null ? user.getBalanceVnd() : 0L)
                .ownerName(user != null ? user.getFullName() : null)
                .build();
    }

    @Transactional(readOnly = true)
    public List<String> getDistinctShopStatuses() {
        return userRepository.findDistinctShopStatuses();
    }

    @Transactional(readOnly = true)
    public List<String> getDistinctStatuses() {
        return sellerRegistrationRepository.findDistinctStatuses();
    }

    @Transactional
    public ShopRegistrationResponseDto toggleShopStatus(Long registrationId, boolean active) {
        SellerRegistration registration = sellerRegistrationRepository.findById(registrationId)
                .orElseThrow(() -> new IllegalArgumentException("Yêu cầu mở Shop không tồn tại."));
        
        User user = registration.getUser();
        if (user == null) {
            throw new IllegalArgumentException("Người dùng liên kết không tồn tại.");
        }
        
        if (active) {
            user.setShopStatus("Active");
        } else {
            user.setShopStatus("Banned");
        }
        
        userRepository.save(user);
        return mapToDto(registration);
    }
}
