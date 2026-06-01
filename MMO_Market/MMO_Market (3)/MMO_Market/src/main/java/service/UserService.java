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

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
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
