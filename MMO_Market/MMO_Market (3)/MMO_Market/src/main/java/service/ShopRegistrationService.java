package service;

import controller.dto.ShopRegistrationRequestDto;
import controller.dto.ShopRegistrationResponseDto;
import controller.dto.ShopRegistrationReviewDto;
import dal.KycRequestRepository;
import dal.SellerRegistrationRepository;
import dal.UserRepository;
import model.KycRequest;
import model.KycStatus;
import model.SellerRegistration;
import model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
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
        Optional<KycRequest> activeKyc = kycRequestRepository.findByActiveUserId(userId);
        if (activeKyc.isEmpty() || activeKyc.get().getStatus() != KycStatus.APPROVED) {
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
                .build();
    }
}
