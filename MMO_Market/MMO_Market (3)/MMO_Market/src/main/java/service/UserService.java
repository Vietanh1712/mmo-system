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

@Service
public class UserService {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^0\\d{9}$");

    private final UserRepository userRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final dal.KycRequestRepository kycRequestRepository;

    public UserService(UserRepository userRepository, 
                       org.springframework.security.crypto.password.PasswordEncoder passwordEncoder,
                       dal.KycRequestRepository kycRequestRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.kycRequestRepository = kycRequestRepository;
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

    private ProfileResponse toProfileResponse(User user) {
        String kycStatus = null;
        java.util.List<model.KycRequest> kycRequests = kycRequestRepository.findByUser_IdAndIsDeleteFalseOrderByCreatedAtDesc(user.getId());
        if (!kycRequests.isEmpty()) {
            kycStatus = kycRequests.get(0).getStatus().name();
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
                .build();
    }
}
