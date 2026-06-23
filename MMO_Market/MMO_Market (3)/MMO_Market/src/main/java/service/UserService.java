package service;

import controller.dto.ProfileResponse;
import controller.dto.UpdateProfileRequest;
import dal.UserRepository;
import model.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.regex.Pattern;

import dal.SellerRegistrationRepository;
import dal.SystemConfigurationRepository;
import model.SellerRegistration;
import java.util.Map;
import java.time.LocalDateTime;

@Service
public class UserService {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^0\\d{9}$");

    private final UserRepository userRepository;
    private final SellerRegistrationRepository sellerRegistrationRepository;
    private final SystemConfigurationRepository systemConfigurationRepository;

    public UserService(UserRepository userRepository,
                       SellerRegistrationRepository sellerRegistrationRepository,
                       SystemConfigurationRepository systemConfigurationRepository) {
        this.userRepository = userRepository;
        this.sellerRegistrationRepository = sellerRegistrationRepository;
        this.systemConfigurationRepository = systemConfigurationRepository;
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

        return toProfileResponse(userRepository.save(user));
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
    public ProfileResponse registerShop(Long userId, Map<String, String> requestData) {
        User user = findActiveUser(userId);
        
        // 1. Kiểm tra trạng thái shop hiện tại
        if ("Approved".equals(user.getShopStatus()) || "Active".equals(user.getShopStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tài khoản của bạn đã là tài khoản người bán.");
        }
        
        // 2. Lấy cấu hình phí nâng cấp Seller
        long upgradeFee = systemConfigurationRepository.findByConfigKey("SELLER_UPGRADE_FEE_VND")
                .map(c -> {
                    try { return Long.parseLong(c.getConfigValue()); }
                    catch (NumberFormatException e) { return 50000L; }
                }).orElse(50000L);
                
        // 3. Kiểm tra số dư ví
        long userBalance = user.getBalanceVnd() != null ? user.getBalanceVnd() : 0L;
        if (userBalance < upgradeFee) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số dư tài khoản không đủ để thực hiện đăng ký shop (Phí nâng cấp: " + String.format("%,d", upgradeFee) + " VNĐ).");
        }
        
        String shopName = requestData.get("shopName");
        String description = requestData.get("description");
        if (shopName == null || shopName.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên cửa hàng không được để trống.");
        }
        
        // 4. Trừ tiền ví của user
        user.setBalanceVnd(userBalance - upgradeFee);
        userRepository.save(user);
        
        // 5. Lưu đăng ký shop (Tạo Pending -> chuyển thành Approved để kích hoạt trigger đổi role)
        SellerRegistration reg = SellerRegistration.builder()
                .user(user)
                .shopName(shopName.trim())
                .description(description != null ? description.trim() : "")
                .status("Pending")
                .isDelete(false)
                .build();
        
        reg = sellerRegistrationRepository.saveAndFlush(reg);
        
        // Cập nhật trạng thái thành Approved để trigger trg_UpdateShopStatus chạy
        reg.setStatus("Approved");
        sellerRegistrationRepository.saveAndFlush(reg);
        
        // Reload user mới để lấy thông tin đã cập nhật role và shopStatus bởi trigger
        User updatedUser = userRepository.findById(userId).orElse(user);
        
        return toProfileResponse(updatedUser);
    }

    private ProfileResponse toProfileResponse(User user) {
        return ProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .role(user.getRole())
                .shopStatus(user.getShopStatus())
                .balanceVnd(user.getBalanceVnd())
                .build();
    }
}
