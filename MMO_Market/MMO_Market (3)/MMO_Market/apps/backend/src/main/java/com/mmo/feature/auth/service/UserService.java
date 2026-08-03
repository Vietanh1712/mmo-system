package com.mmo.feature.auth.service;
import com.mmo.shared.model.Transaction;
import com.mmo.shared.dal.KycRequestRepository;
import com.mmo.shared.model.KycStatus;
import com.mmo.shared.model.KycRequest;

import com.mmo.shared.dto.ProfileResponse;
import com.mmo.shared.dto.UpdateProfileRequest;
import com.mmo.shared.dto.ShopRegistrationRequestDto;
import com.mmo.shared.dal.UserRepository;
import com.mmo.shared.model.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.regex.Pattern;

import com.mmo.shared.dal.SellerRegistrationRepository;
import com.mmo.shared.dal.SystemConfigurationRepository;
import com.mmo.shared.dal.WalletTransactionRepository;
import com.mmo.shared.model.SellerRegistration;
import com.mmo.shared.model.WalletTransaction;
import java.time.LocalDateTime;

@Service
public class UserService {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^0\\d{9}$");

    private final UserRepository userRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final com.mmo.shared.dal.KycRequestRepository kycRequestRepository;
    private final SellerRegistrationRepository sellerRegistrationRepository;
    private final SystemConfigurationRepository systemConfigurationRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    public UserService(UserRepository userRepository, 
                       org.springframework.security.crypto.password.PasswordEncoder passwordEncoder,
                       com.mmo.shared.dal.KycRequestRepository kycRequestRepository,
                       SellerRegistrationRepository sellerRegistrationRepository,
                       SystemConfigurationRepository systemConfigurationRepository,
                       WalletTransactionRepository walletTransactionRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.kycRequestRepository = kycRequestRepository;
        this.sellerRegistrationRepository = sellerRegistrationRepository;
        this.systemConfigurationRepository = systemConfigurationRepository;
        this.walletTransactionRepository = walletTransactionRepository;
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public ProfileResponse getMyProfile(Long userId) {
        return toProfileResponse(findActiveUser(userId));
    }

    @Transactional
    public ProfileResponse updateMyProfile(Long userId, UpdateProfileRequest request) {
        User user = findActiveUser(userId);
        String fullName = request.getFullName() == null ? "" : request.getFullName().trim();
        String phone = normalizePhone(request.getPhone());

        if (fullName.length() < 3 || fullName.length() > 255) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Họ tên phải từ 3 đến 255 ký tự"
            );
        }

        user.setFullName(fullName);
        user.setPhone(phone);

        if (request.getGender() != null) {
            user.setGender(request.getGender().trim());
        } else {
            user.setGender(null);
        }

        if (request.getNationalId() != null) {
            user.setNationalId(request.getNationalId().trim());
        } else {
            user.setNationalId(null);
        }

        if (request.getAddress() != null) {
            user.setAddress(request.getAddress().trim());
        } else {
            user.setAddress(null);
        }

        if (request.getDateOfBirth() != null && !request.getDateOfBirth().isBlank()) {
            try {
                user.setDateOfBirth(java.time.LocalDate.parse(request.getDateOfBirth()));
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngày sinh không hợp lệ");
            }
        } else {
            user.setDateOfBirth(null);
        }

        return toProfileResponse(userRepository.save(user));
    }

    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = findActiveUser(userId);
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Mật khẩu hiện tại không chính xác"
            );
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    private User findActiveUser(Long userId) {
        return userRepository.findByIdAndIsDeleteFalse(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy tài khoản"
                ));
    }

    private String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }

        String normalizedPhone = phone.trim();
        if (!PHONE_PATTERN.matcher(normalizedPhone).matches()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Số điện thoại phải gồm 10 chữ số và bắt đầu bằng số 0"
            );
        }
        return normalizedPhone;
    }

    @Transactional
    public ProfileResponse registerShop(Long userId, ShopRegistrationRequestDto requestData) {
        User user = findActiveUser(userId);
        
        // 1. Kiểm tra trạng thái shop hiện tại
        if ("Approved".equals(user.getShopStatus()) || "Active".equals(user.getShopStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tài khoản của bạn đã là tài khoản người bán.");
        }

        if (!hasApprovedKyc(user.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bạn cần hoàn tất xác minh danh tính (KYC) trước khi đăng ký Shop.");
        }
        
        // 2. Lấy cấu hình phí mở Shop
        long shopOpeningFee = systemConfigurationRepository.findByConfigKey("SHOP_OPENING_FEE_VND")
                .map(c -> {
                    try { return Long.parseLong(c.getConfigValue()); }
                    catch (NumberFormatException e) { return 50000L; }
                }).orElse(50000L);
                
        // 3. Kiểm tra số dư ví
        long userBalance = user.getBalanceVnd() != null ? user.getBalanceVnd() : 0L;
        if (userBalance < shopOpeningFee) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số dư tài khoản không đủ để thực hiện đăng ký shop (Phí mở shop: " + String.format("%,d", shopOpeningFee) + " VNĐ).");
        }
        
        String shopName = requestData.getShopName();
        String description = requestData.getDescription();
        String category = requestData.getCategory();
        String supportEmail = requestData.getSupportEmail();
        String supportPhone = requestData.getSupportPhone();
        if (shopName == null || shopName.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên cửa hàng không được để trống.");
        }
        
        // 4. Trừ tiền ví của user và cập nhật Role/Status
        long newBalance = userBalance - shopOpeningFee;
        user.setBalanceVnd(newBalance);
        user.setDepositVnd(shopOpeningFee); // Lưu phí mở shop vào depositVnd để hoàn lại khi đóng shop
        user.setRole("{\"role\": \"Seller\"}");
        user.setShopStatus("Active");
        userRepository.save(user);

        // Ghi WalletTransaction cho phí mở shop
        WalletTransaction feeTx = WalletTransaction.builder()
                .user(user)
                .type("SHOP_OPEN_FEE")
                .transactionType("SHOP_OPEN_FEE")
                .amountVnd(shopOpeningFee)
                .balanceAfter(newBalance)
                .status("SUCCESS")
                .description("Phí mở Shop: " + shopName.trim())
                .referenceCode("SHOP_OPEN_FEE_USER_" + userId)
                .createdAt(LocalDateTime.now())
                .isDelete(false)
                .build();
        walletTransactionRepository.save(feeTx);

        // 5. Quy tắc: 1 tài khoản chỉ được mở 1 shop.
        //    Nếu đã có bản ghi cũ (WITHDRAWN / bất kỳ trạng thái) → cập nhật lại bản ghi đó
        //    thay vì tạo bản ghi mới → tránh tình trạng 1 tài khoản có nhiều shop.
        java.util.List<SellerRegistration> allUserRegs = sellerRegistrationRepository
                .findAllByIsDeleteFalseOrderByCreatedAtDesc()
                .stream()
                .filter(r -> r.getUser() != null && r.getUser().getId().equals(userId))
                .collect(java.util.stream.Collectors.toList());

        SellerRegistration reg;
        if (!allUserRegs.isEmpty()) {
            // Tái sử dụng bản ghi gần nhất, đánh dấu các bản ghi thừa là xóa mềm
            reg = allUserRegs.get(0);
            for (int i = 1; i < allUserRegs.size(); i++) {
                allUserRegs.get(i).setIsDelete(true);
                sellerRegistrationRepository.save(allUserRegs.get(i));
            }
        } else {
            reg = new SellerRegistration();
            reg.setUser(user);
        }

        reg.setShopName(shopName.trim());
        reg.setDescription(description != null ? description.trim() : "");
        reg.setCategory(category != null ? category.trim() : "");
        reg.setSupportEmail(supportEmail != null ? supportEmail.trim() : "");
        reg.setSupportPhone(supportPhone != null ? supportPhone.trim() : "");
        reg.setStatus("Approved");
        reg.setIsDelete(false);

        sellerRegistrationRepository.saveAndFlush(reg);

        User updatedUser = userRepository.findById(userId).orElse(user);
        return toProfileResponse(updatedUser);
    }

    private ProfileResponse toProfileResponse(User user) {
        String kycStatus = null;
        java.util.List<com.mmo.shared.model.KycRequest> kycRequests = kycRequestRepository.findByUser_IdAndIsDeleteFalseOrderByCreatedAtDesc(user.getId());
        if (!kycRequests.isEmpty()) {
            kycStatus = kycRequests.stream()
                    .anyMatch(kycRequest -> kycRequest.getStatus() == KycStatus.APPROVED)
                    ? KycStatus.APPROVED.name()
                    : kycRequests.get(0).getStatus().name();
        }

        return ProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .gender(user.getGender())
                .nationalId(user.getNationalId())
                .phone(user.getPhone())
                .role(user.getRole())
                .shopStatus(user.getShopStatus())
                .balanceVnd(user.getBalanceVnd())
                .address(user.getAddress())
                .is2faEnabled(user.getIs2faEnabled())
                .kycStatus(kycStatus)
                .dateOfBirth(user.getDateOfBirth() != null ? user.getDateOfBirth().toString() : null)
                .avatar(user.getAvatar())
                .build();
    }

    @Transactional
    public String uploadAvatar(Long userId, org.springframework.web.multipart.MultipartFile file) throws java.io.IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File không được rỗng");
        }

        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("Kích thước file tối đa là 5MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("image/jpeg") && !contentType.equals("image/png") && !contentType.equals("image/gif") && !contentType.equals("image/webp"))) {
            throw new IllegalArgumentException("Chỉ chấp nhận các định dạng ảnh: JPEG, PNG, GIF, WEBP");
        }

        User user = findActiveUser(userId);

        java.nio.file.Path uploadPath = java.nio.file.Paths.get("uploads/avatars").toAbsolutePath().normalize();
        if (!java.nio.file.Files.exists(uploadPath)) {
            java.nio.file.Files.createDirectories(uploadPath);
        }

        String originalFilename = file.getOriginalFilename();
        String extension = ".jpg";
        if (originalFilename != null && originalFilename.lastIndexOf(".") != -1) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String filename = userId + "_" + System.currentTimeMillis() + extension;
        java.nio.file.Path targetLocation = uploadPath.resolve(filename);
        java.nio.file.Files.copy(file.getInputStream(), targetLocation, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        String avatarUrl = "/uploads/avatars/" + filename;
        user.setAvatar(avatarUrl);
        userRepository.save(user);

        return avatarUrl;
    }

    private boolean hasApprovedKyc(Long userId) {
        return kycRequestRepository.findByUser_IdAndIsDeleteFalseOrderByCreatedAtDesc(userId)
                .stream()
                .anyMatch(kycRequest -> kycRequest.getStatus() == KycStatus.APPROVED);
    }
}
