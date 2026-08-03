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
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.mmo.shared.dal.AuditLogRepository;
import com.mmo.shared.model.AuditLog;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Service
@EnableScheduling
@Slf4j
public class ShopRegistrationService {

    @Autowired
    private AuditLogRepository auditLogRepository;

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

    @jakarta.annotation.PostConstruct
    public void autoApproveExistingRegistrations() {
        try {
            List<SellerRegistration> pendingList = sellerRegistrationRepository.findAllByIsDeleteFalseOrderByCreatedAtDesc().stream()
                    .filter(r -> r.getStatus() != null && "PENDING".equalsIgnoreCase(r.getStatus().trim()))
                    .collect(Collectors.toList());
            for (SellerRegistration reg : pendingList) {
                reg.setStatus("APPROVED");
                sellerRegistrationRepository.save(reg);
                User u = reg.getUser();
                if (u != null) {
                    if (u.getRole() == null || u.getRole().contains("\"Customer\"")) {
                        u.setRole("{\"role\": \"Seller\"}");
                    }
                    u.setShopStatus("Active");
                    userRepository.save(u);
                }
            }

            // Fix missing registrations for existing Sellers
            List<User> allUsers = userRepository.findAll();
            for (User u : allUsers) {
                if (u.getRole() != null && u.getRole().contains("Seller")) {
                    boolean hasReg = sellerRegistrationRepository.findByUserAndIsDeleteFalse(u).isPresent();
                    if (!hasReg) {
                        SellerRegistration reg = new SellerRegistration();
                        reg.setUser(u);
                        reg.setShopName(u.getFullName() != null ? u.getFullName() + " Shop" : "Shop " + u.getId());
                        reg.setStatus("APPROVED");
                        reg.setCategory("Chung");
                        reg.setDescription("Shop được tạo tự động từ hệ thống.");
                        reg.setFeeVnd(500000L);
                        sellerRegistrationRepository.save(reg);
                    }
                }
            }
        } catch (Exception ignored) {}
    }

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

        SellerRegistration registration = sellerRegistrationRepository.findFirstByUserAndIsDeleteFalseOrderByIdDesc(user)
                .orElse(new SellerRegistration());

        if ("APPROVED".equals(registration.getStatus())) {
            throw new IllegalStateException("Bạn đã có Shop đang hoạt động.");
        }

        long shopOpeningFee = systemConfigurationRepository.findByConfigKey("SHOP_OPENING_FEE_VND")
                .map(c -> {
                    try { return Long.parseLong(c.getConfigValue()); }
                    catch (NumberFormatException e) { return 500000L; }
                }).orElse(500000L);

        // Tự động duyệt mở shop sau khi đã xác minh KYC
        registration.setUser(user);
        registration.setShopName(request.getShopName());
        registration.setDescription(request.getDescription());
        registration.setCategory(request.getCategory());
        registration.setSupportEmail(request.getSupportEmail());
        registration.setSupportPhone(request.getSupportPhone());
        registration.setStatus("APPROVED");
        if (registration.getFeeVnd() == null) {
            registration.setFeeVnd(shopOpeningFee);
        }

        // Nâng cấp vai trò người dùng thành Seller và kích hoạt shopStatus
        String currentRole = user.getRole();
        if (currentRole == null || currentRole.contains("\"Customer\"")) {
            user.setRole("{\"role\": \"Seller\"}");
        }
        user.setShopStatus("Active");
        userRepository.save(user);

        SellerRegistration saved = sellerRegistrationRepository.save(registration);

        // 1. Tạo thông báo cho Customer
        Notification customerNotif = Notification.builder()
                .userId(user.getId())
                .title("Đăng ký mở Shop thành công")
                .content(String.format("Chúc mừng! Shop \"%s\" của bạn đã được khởi tạo thành công. Bạn có thể bắt đầu sử dụng giao diện người bán ngay.", saved.getShopName()))
                .type("SYSTEM")
                .severity("SUCCESS")
                .isRead(false)
                .isDelete(false)
                .targetUrl("/seller/dashboard")
                .build();
        notificationRepository.save(customerNotif);

        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public ShopRegistrationResponseDto getMyRegistration(Long userId) {
        User user = userRepository.findByIdAndIsDeleteFalse(userId)
                .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại."));

        return sellerRegistrationRepository.findFirstByUserAndIsDeleteFalseOrderByIdDesc(user)
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

        Page<SellerRegistration> regPage = sellerRegistrationRepository.searchRegistrations(cleanStatus, cleanShopStatus, cleanKeyword, pageable);
        return regPage.map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getRegistrationStats() {
        Map<String, Long> stats = new HashMap<>();

        List<SellerRegistration> list = sellerRegistrationRepository.findAllByIsDeleteFalseOrderByCreatedAtDesc();

        long totalShops = list.size();
        long activeShops = 0;
        long permanentBannedShops = 0;
        long indefiniteLockedShops = 0;
        long temporarySuspendedShops = 0;
        long withdrawnShops = 0;
        long totalDeposit = 0;

        for (SellerRegistration reg : list) {
            User user = reg.getUser();
            if (user != null) {
                if (user.getDepositVnd() != null) {
                    totalDeposit += user.getDepositVnd();
                }
                String st = user.getShopStatus();
                String stUpper = (st != null) ? st.trim().toUpperCase() : "ACTIVE";

                if (stUpper.equals("BANNED") || stUpper.equals("PERMANENT_BANNED")) {
                    permanentBannedShops++;
                } else if (stUpper.equals("LOCKED") || stUpper.equals("INDEFINITE_LOCKED") || stUpper.equals("CLOSED")) {
                    indefiniteLockedShops++;
                } else if (stUpper.equals("SUSPENDED") || stUpper.equals("TEMP_LOCKED") || stUpper.equals("TEMP_SUSPENDED") || stUpper.equals("TEMPORARILY_CLOSED")) {
                    temporarySuspendedShops++;
                } else if (stUpper.equals("WITHDRAWN") || stUpper.equals("DELETED")) {
                    withdrawnShops++;
                } else {
                    activeShops++;
                }
            } else {
                activeShops++;
            }
        }

        stats.put("totalShops", totalShops);
        stats.put("activeShops", activeShops);
        stats.put("bannedShops", permanentBannedShops + indefiniteLockedShops + temporarySuspendedShops);
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

        // Ghi AuditLog
        Map<String, Object> diff = new HashMap<>();
        diff.put("registrationId", registrationId);
        diff.put("shopName", updated.getShopName());
        diff.put("status", updated.getStatus());
        if (updated.getRejectionReason() != null) {
            diff.put("reason", updated.getRejectionReason());
        }
        String action = review.isApproved() ? "Shop_Approve" : "Shop_Reject";
        String actionDesc = (review.isApproved() ? "Duyệt mở shop thành công: " : "Từ chối đăng ký mở shop: ") + updated.getShopName();
        saveAuditLog(user, action, actionDesc, diff);

        return mapToDto(updated);
    }

    private void saveAuditLog(User operator, String action, String desc, Map<String, Object> diff) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("desc", desc);
            payload.put("diff", diff);
            String jsonDetails = new ObjectMapper().writeValueAsString(payload);
            auditLogRepository.save(AuditLog.builder()
                    .userId(operator != null ? operator.getId() : 1L)
                    .action(action)
                    .details(jsonDetails)
                    .build());
        } catch (Exception e) {
            log.error("Failed to save audit log: {}", e.getMessage());
        }
    }

    private ShopRegistrationResponseDto mapToDto(SellerRegistration registration) {
        User user = registration.getUser();
        if (user != null && user.getShopStatus() != null &&
                ("Suspended".equalsIgnoreCase(user.getShopStatus()) || "TEMP_LOCKED".equalsIgnoreCase(user.getShopStatus())) &&
                user.getSuspendedUntil() != null &&
                LocalDateTime.now().isAfter(user.getSuspendedUntil())) {
            user.setShopStatus("Active");
            user.setSuspendedUntil(null);
            userRepository.save(user);
        }

        SellerBankInfo bank = null;
        if (user != null) {
            bank = sellerBankInfoRepository.findFirstByUserAndIsDeleteFalseOrderByIdDesc(user).orElse(null);
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
                .suspendedUntil(user != null && user.getSuspendedUntil() != null ? user.getSuspendedUntil().toString() : null)
                .depositVnd(user != null && user.getDepositVnd() != null ? user.getDepositVnd() : 0L)
                .balanceVnd(user != null && user.getBalanceVnd() != null ? user.getBalanceVnd() : 0L)
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
        
        user.setShopStatus(active ? "Active" : "Banned");
        user.setSuspendedUntil(null);
        userRepository.save(user);

        // Tạo thông báo cho Customer/Seller về việc khóa/mở khóa Shop
        Notification statusNotif = Notification.builder()
                .userId(user.getId())
                .title(active ? "Shop của bạn đã được mở khóa" : "Shop của bạn đã bị khóa")
                .content(active ?
                        String.format("Shop \"%s\" của bạn đã được mở khóa và hoạt động trở lại bình thường.", registration.getShopName()) :
                        String.format("Shop \"%s\" của bạn đã bị tạm khóa do vi phạm chính sách sàn. Vui lòng liên hệ hỗ trợ để biết thêm chi tiết.", registration.getShopName()))
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
    public ShopRegistrationResponseDto updateShopStatus(Long registrationId, String shopStatus, String suspendedUntilStr) {
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
        if (("Suspended".equalsIgnoreCase(shopStatus) || "TEMP_LOCKED".equalsIgnoreCase(shopStatus))
                && suspendedUntilStr != null && !suspendedUntilStr.isBlank()) {
            try {
                user.setSuspendedUntil(java.time.LocalDateTime.parse(suspendedUntilStr.trim()));
            } catch (Exception e) {
                try {
                    user.setSuspendedUntil(java.time.LocalDateTime.parse(suspendedUntilStr.trim().replace(" ", "T")));
                } catch (Exception ex) {
                    user.setSuspendedUntil(null);
                }
            }
        } else {
            user.setSuspendedUntil(null);
        }
        userRepository.save(user);
        return mapToDto(registration);
    }

    @Scheduled(fixedRate = 30000)
    @Transactional
    public void autoRevertSuspendedShops() {
        try {
            List<User> suspendedUsers = userRepository.findAll().stream()
                    .filter(u -> u.getShopStatus() != null &&
                            ("Suspended".equalsIgnoreCase(u.getShopStatus()) || "TEMP_LOCKED".equalsIgnoreCase(u.getShopStatus())) &&
                            u.getSuspendedUntil() != null &&
                            java.time.LocalDateTime.now().isAfter(u.getSuspendedUntil()))
                    .collect(Collectors.toList());

            for (User u : suspendedUsers) {
                u.setShopStatus("Active");
                u.setSuspendedUntil(null);
                userRepository.save(u);
            }
        } catch (Exception ignored) {}
    }
}
