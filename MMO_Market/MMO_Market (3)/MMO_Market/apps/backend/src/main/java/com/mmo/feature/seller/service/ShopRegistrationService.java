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
import com.mmo.shared.dal.NotificationRepository;
import com.mmo.shared.model.KycStatus;
import com.mmo.shared.model.SellerRegistration;
import com.mmo.shared.model.User;
import com.mmo.shared.model.SellerBankInfo;
import com.mmo.shared.dal.SellerBankInfoRepository;
import com.mmo.shared.model.Notification;
import com.mmo.shared.dal.SystemConfigurationRepository;
import com.mmo.feature.wallet.service.WalletService;
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

    @Autowired
    private SellerBankInfoRepository sellerBankInfoRepository;

    @Autowired
    private SystemConfigurationRepository systemConfigurationRepository;

    @Autowired
    private WalletService walletService;

    @Autowired
    private NotificationRepository notificationRepository;

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

        // 1. Tạo thông báo cho Customer
        Notification customerNotif = Notification.builder()
                .userId(user.getId())
                .title("Đăng ký mở Shop thành công")
                .content(String.format("Yêu cầu đăng ký mở Shop \"%s\" của bạn đã được gửi thành công và đang chờ duyệt.", saved.getShopName()))
                .type("SYSTEM")
                .severity("INFO")
                .isRead(false)
                .isDelete(false)
                .targetUrl("/account/register-shop")
                .build();
        notificationRepository.save(customerNotif);

        // 2. Tạo thông báo cho toàn bộ Staff & Admin
        List<User> staffAndAdmins = userRepository.findStaffAndAdmins();
        for (User staff : staffAndAdmins) {
            if (staff.getId().equals(user.getId())) {
                continue;
            }
            Notification staffNotif = Notification.builder()
                    .userId(staff.getId())
                    .title("Yêu cầu mở Shop mới")
                    .content(String.format("Có yêu cầu mở Shop mới \"%s\" từ %s (%s) cần phê duyệt.", saved.getShopName(), user.getFullName(), user.getEmail()))
                    .type("SYSTEM")
                    .severity("WARNING")
                    .isRead(false)
                    .isDelete(false)
                    .targetUrl("/staff/shop-registrations")
                    .build();
            notificationRepository.save(staffNotif);
        }

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
    public ShopRegistrationResponseDto getRegistrationById(Long id) {
        SellerRegistration registration = sellerRegistrationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu đăng ký Shop."));
        return mapToDto(registration);
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
        long permanentBannedShops = userRepository.countPermanentBannedShops();
        long indefiniteLockedShops = userRepository.countIndefiniteLockedShops();
        long temporarySuspendedShops = userRepository.countTemporarySuspendedShops();
        long withdrawnShops = userRepository.countWithdrawnShops();

        stats.put("totalShops", totalShops);
        stats.put("activeShops", activeShops);
        stats.put("bannedShops", bannedShops);
        stats.put("totalDeposit", totalDeposit);
        stats.put("permanentBannedShops", permanentBannedShops);
        stats.put("indefiniteLockedShops", indefiniteLockedShops);
        stats.put("temporarySuspendedShops", temporarySuspendedShops);
        stats.put("withdrawnShops", withdrawnShops);
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

        // Tạo thông báo kết quả cho Customer/Seller
        User user = updated.getUser();
        String title = "";
        String content = "";
        String severity = "INFO";
        String targetUrl = "/account/register-shop";
        if ("APPROVED".equals(updated.getStatus())) {
            title = "Yêu cầu mở Shop đã được duyệt";
            content = String.format("Chúc mừng! Yêu cầu đăng ký mở Shop \"%s\" của bạn đã được phê duyệt thành công. Vui lòng đăng nhập lại để kích hoạt giao diện bán hàng.", updated.getShopName());
            severity = "SUCCESS";
            targetUrl = "/seller/dashboard";
        } else if ("REJECTED".equals(updated.getStatus())) {
            title = "Yêu cầu mở Shop bị từ chối";
            content = String.format("Yêu cầu đăng ký mở Shop \"%s\" của bạn bị từ chối. Lý do: %s", updated.getShopName(), updated.getRejectionReason() != null ? updated.getRejectionReason() : "Hồ sơ không hợp lệ");
            severity = "DANGER";
        }

        Notification resultNotif = Notification.builder()
                .userId(user.getId())
                .title(title)
                .content(content)
                .type("SYSTEM")
                .severity(severity)
                .isRead(false)
                .isDelete(false)
                .targetUrl(targetUrl)
                .build();
        notificationRepository.save(resultNotif);

        return mapToDto(updated);
    }

    private ShopRegistrationResponseDto mapToDto(SellerRegistration registration) {
        User user = registration.getUser();
        SellerBankInfo bank = null;
        if (user != null) {
            bank = sellerBankInfoRepository.findByUserAndIsDeleteFalse(user).orElse(null);
        }
        return ShopRegistrationResponseDto.builder()
                .id(registration.getId())
                .status(registration.getStatus())
                .code("SHOP-" + (registration.getId() != null ? registration.getId() : ""))
                .submittedAt(registration.getCreatedAt() != null ? registration.getCreatedAt().toString() : null)
                .shopName(registration.getShopName())
                .category(registration.getCategory())
                .description(registration.getDescription())
                .supportEmail(user != null ? user.getEmail() : null)
                .supportPhone(user != null ? user.getPhone() : null)
                .rejectionReason(registration.getRejectionReason())
                .shopStatus(user != null ? user.getShopStatus() : null)
                .depositVnd(user != null ? user.getDepositVnd() : 0L)
                .balanceVnd(user != null ? user.getBalanceVnd() : 0L)
                .ownerName(user != null ? user.getFullName() : null)
                .bankAccountNumber(bank != null ? bank.getAccountNumber() : null)
                .bankName(bank != null ? bank.getBankName() : null)
                .bankBranch(bank != null ? bank.getBranch() : null)
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

        // Gửi thông báo cho Seller
        Notification statusNotif = Notification.builder()
                .userId(user.getId())
                .title(active ? "Hoạt động Shop đã được kích hoạt" : "Cảnh báo: Shop đã bị khóa")
                .content(active ? String.format("Tài khoản Shop \"%s\" của bạn đã được kích hoạt hoạt động trở lại.", registration.getShopName()) 
                               : String.format("Tài khoản Shop \"%s\" của bạn đã bị khóa tạm thời do vi phạm điều khoản quy định.", registration.getShopName()))
                .type("SYSTEM")
                .severity(active ? "SUCCESS" : "DANGER")
                .isRead(false)
                .isDelete(false)
                .targetUrl(active ? "/seller/dashboard" : "/profile")
                .build();
        notificationRepository.save(statusNotif);

        return mapToDto(registration);
    }

    @Transactional
    public ShopRegistrationResponseDto updateShopStatus(Long registrationId, String shopStatus) {
        SellerRegistration registration = sellerRegistrationRepository.findById(registrationId)
                .orElseThrow(() -> new IllegalArgumentException("Yêu cầu mở Shop không tồn tại."));
        
        User user = registration.getUser();
        if (user == null) {
            throw new IllegalArgumentException("Người dùng liên kết không tồn tại.");
        }
        
        if (shopStatus == null || shopStatus.isBlank()) {
            throw new IllegalArgumentException("Trạng thái không hợp lệ.");
        }
        
        user.setShopStatus(shopStatus);
        userRepository.save(user);
        return mapToDto(registration);
    }
}
