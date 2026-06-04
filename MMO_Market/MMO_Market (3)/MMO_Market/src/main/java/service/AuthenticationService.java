package service;

import controller.dto.*;
import dal.AuthenticationRepository;
import dal.EmailVerificationRepository;
import dal.UserRepository;
import lombok.extern.slf4j.Slf4j;
import model.Authentication;
import model.EmailVerification;
import model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import security.JwtTokenProvider;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;

@Service
@Slf4j
public class AuthenticationService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthenticationRepository authenticationRepository;

    @Autowired
    private EmailVerificationRepository emailVerificationRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Đăng ký người dùng mới
     */
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            return RegisterResponse.builder()
                    .success(false)
                    .message("Email này đã được đăng ký")
                    .build();
        }

        String roleJson = "{\"role\": \"Customer\"}";
        if ("Seller".equalsIgnoreCase(request.getRole())) {
            roleJson = "{\"role\": \"Seller\"}";
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .role(roleJson)
                .isVerified(false)
                .balanceVnd(0L)
                .isDelete(false)
                .shopStatus("Pending")
                .build();

        User savedUser = userRepository.save(user);
        log.info("Tạo người dùng mới thành công: {}", savedUser.getEmail());

        String otp = generateOtp();

        EmailVerification emailVerification = EmailVerification.builder()
                .userId(savedUser.getId())
                .verificationCode(otp)
                .expiryDate(LocalDateTime.now().plusMinutes(5))
                .isUsed(false)
                .build();
        emailVerificationRepository.save(emailVerification);

        log.info("Đã sinh mã OTP [{}] cho user ID [{}]", otp, savedUser.getId());

        return RegisterResponse.builder()
                .success(true)
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .fullName(savedUser.getFullName())
                .message("Đăng ký thành công. Vui lòng kiểm tra email để nhận mã OTP xác thực tài khoản.")
                .build();
    }

    /**
     * Xác thực OTP luồng Đăng ký tài khoản (Đốt mã ngay lập tức và active user)
     */
    @Transactional
    public VerifyOtpResponse verifyOtp(VerifyOtpRequest request) {
        Optional<User> userOptional = userRepository.findByEmail(request.getEmail());

        if (userOptional.isEmpty()) {
            return VerifyOtpResponse.builder().success(false).message("Không tìm thấy thông tin người dùng với email này").build();
        }

        User user = userOptional.get();

        Optional<EmailVerification> otpOptional = emailVerificationRepository
                .findByUserIdAndVerificationCodeAndIsUsedFalse(user.getId(), request.getOtp());

        if (otpOptional.isEmpty()) {
            return VerifyOtpResponse.builder().success(false).message("Mã OTP không hợp lệ hoặc đã được sử dụng").build();
        }

        EmailVerification emailVerification = otpOptional.get();

        if (emailVerification.getExpiryDate().isBefore(LocalDateTime.now())) {
            return VerifyOtpResponse.builder().success(false).message("Mã OTP đã hết hạn").build();
        }

        // Đánh dấu OTP đã sử dụng
        emailVerification.setIsUsed(true);
        emailVerificationRepository.save(emailVerification);

        user.setIsVerified(true);
        userRepository.save(user);

        log.info("Xác thực email thành công cho user: {}", user.getEmail());
        return VerifyOtpResponse.builder().success(true).message("Xác thực tài khoản thành công").build();
    }

    /**
     * =================================================================================
     * KIỂM TRA MÃ OTP KHÔI PHỤC MẬT KHẨU (BƯỚC 1 CỦA GIAO DIỆN)
     * Chỉ kiểm tra tính hợp lệ, TUYỆT ĐỐI KHÔNG ĐỐT MÃ (isUsed = true) ở đây
     * =================================================================================
     */
    @Transactional(readOnly = true)
    public VerifyOtpResponse checkResetOtp(VerifyOtpRequest request) {
        Optional<User> userOptional = userRepository.findByEmail(request.getEmail());

        if (userOptional.isEmpty()) {
            return VerifyOtpResponse.builder().success(false).message("Không tìm thấy thông tin người dùng với email này").build();
        }

        User user = userOptional.get();

        // Tìm mã OTP
        Optional<EmailVerification> otpOptional = emailVerificationRepository
                .findByUserIdAndVerificationCode(user.getId(), request.getOtp());

        if (otpOptional.isEmpty()) {
            return VerifyOtpResponse.builder().success(false).message("Mã OTP không hợp lệ").build();
        }

        EmailVerification emailVerification = otpOptional.get();

        // Kiểm tra xem đã bị dùng chưa
        if (emailVerification.getIsUsed()) {
            return VerifyOtpResponse.builder().success(false).message("Mã OTP này đã được sử dụng").build();
        }

        // Kiểm tra thời hạn
        if (emailVerification.getExpiryDate().isBefore(LocalDateTime.now())) {
            return VerifyOtpResponse.builder().success(false).message("Mã OTP đã hết hạn").build();
        }

        log.info("Mã OTP đặt lại mật khẩu hợp lệ cho email: {}", user.getEmail());
        return VerifyOtpResponse.builder().success(true).message("Mã OTP hợp lệ").build();
    }

    /**
     * Gửi lại mã OTP (Tạo mã mới và lưu vào Database)
     */
    @Transactional
    public void resendOtp(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            throw new RuntimeException("Không tìm thấy thông tin người dùng với email này");
        }
        User user = userOptional.get();

        String newOtp = generateOtp();

        EmailVerification emailVerification = EmailVerification.builder()
                .userId(user.getId())
                .verificationCode(newOtp)
                .expiryDate(LocalDateTime.now().plusMinutes(5))
                .isUsed(false)
                .build();
        emailVerificationRepository.save(emailVerification);

        System.out.println("===============================================");
        System.out.println("MÃ OTP MỚI CỦA TÀI KHOẢN [" + email + "] LÀ: " + newOtp);
        System.out.println("===============================================");
    }

    /**
     * Quên mật khẩu: Kiểm tra email, sinh OTP và lưu Database
     */
    @Transactional
    public void forgotPassword(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            throw new RuntimeException("Email chưa được đăng ký trong hệ thống");
        }
        User user = userOptional.get();

        String newOtp = generateOtp();

        EmailVerification emailVerification = EmailVerification.builder()
                .userId(user.getId())
                .verificationCode(newOtp)
                .expiryDate(LocalDateTime.now().plusMinutes(5))
                .isUsed(false)
                .build();
        emailVerificationRepository.save(emailVerification);

        System.out.println("===============================================");
        System.out.println("MÃ OTP KHÔI PHỤC MẬT KHẨU CỦA [" + email + "] LÀ: " + newOtp);
        System.out.println("===============================================");
    }

    /**
     * =================================================================================
     * LƯU MẬT KHẨU MỚI (BƯỚC 2 CỦA GIAO DIỆN)
     * Xác thực OTP lần cuối, lưu mật khẩu và CHÍNH THỨC ĐỐT MÃ OTP TẠI ĐÂY
     * =================================================================================
     */
    @Transactional
    public void resetPassword(String email, String otp, String newPassword) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            throw new RuntimeException("Lỗi hệ thống: Không tìm thấy người dùng");
        }
        User user = userOptional.get();

        // Sử dụng findByUserIdAndVerificationCode (Không kèm điều kiện isUsedFalse ở tên hàm)
        Optional<EmailVerification> otpOptional = emailVerificationRepository
                .findByUserIdAndVerificationCode(user.getId(), otp);

        if (otpOptional.isEmpty()) {
            throw new RuntimeException("Mã OTP không hợp lệ.");
        }

        EmailVerification emailVerification = otpOptional.get();

        // Kiểm tra thủ công xem đã bị dùng chưa
        if (emailVerification.getIsUsed()) {
            throw new RuntimeException("Mã OTP này đã được sử dụng.");
        }

        if (emailVerification.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Mã OTP đã hết hạn. Vui lòng yêu cầu mã mới.");
        }

        // Mã hóa mật khẩu mới và lưu
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // CHÍNH THỨC TIÊU HỦY MÃ OTP
        emailVerification.setIsUsed(true);
        emailVerificationRepository.save(emailVerification);

        log.info("Người dùng {} đã đặt lại mật khẩu thành công", email);
    }

    /**
     * Đăng nhập người dùng
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        Optional<User> userOptional = userRepository.findByEmailAndIsDeleteFalse(request.getEmail());

        if (userOptional.isEmpty() || !passwordEncoder.matches(request.getPassword(), userOptional.get().getPassword())) {
            log.warn("Email hoặc mật khẩu không chính xác cho: {}", request.getEmail());
            return LoginResponse.builder().message("Email hoặc mật khẩu không chính xác").build();
        }

        User user = userOptional.get();

        if (!user.getIsVerified()) {
            log.warn("User chưa xác thực email: {}", request.getEmail());
            return LoginResponse.builder().message("Vui lòng xác thực email (OTP) trước khi đăng nhập").build();
        }

        revokeAllUserTokens(user.getId());

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getEmail());
        LocalDateTime refreshTokenExpiryDate = jwtTokenProvider.getExpiryDateFromToken(refreshToken);

        Authentication auth = Authentication.builder()
                .userId(user.getId())
                .provider("System")
                .refreshToken(refreshToken)
                .refreshTokenExpiryDate(refreshTokenExpiryDate)
                .isRevoked(false)
                .isDelete(false)
                .build();
        authenticationRepository.save(auth);

        log.info("Đăng nhập thành công: {}", user.getEmail());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .balanceVnd(user.getBalanceVnd())
                .redirectPath(resolveRedirectPath(user.getRole()))
                .message("Đăng nhập thành công")
                .build();
    }

    /**
     * Đăng xuất người dùng
     */
    @Transactional
    public LogoutResponse logout(String refreshToken) {
        Optional<Authentication> authOptional = authenticationRepository.findByRefreshToken(refreshToken);

        if (authOptional.isEmpty()) {
            return LogoutResponse.builder().success(false).message("Refresh token không hợp lệ").build();
        }

        Authentication auth = authOptional.get();
        auth.setIsRevoked(true);
        authenticationRepository.save(auth);

        log.info("Đăng xuất thành công - User ID: {}", auth.getUserId());
        return LogoutResponse.builder().success(true).message("Đăng xuất thành công").build();
    }

    /**
     * Làm mới Access Token từ Refresh Token
     */
    @Transactional
    public LoginResponse refreshAccessToken(String refreshToken) {
        Optional<Authentication> authOptional = authenticationRepository.findByRefreshToken(refreshToken);

        if (authOptional.isEmpty() || authOptional.get().getIsRevoked()) {
            return LoginResponse.builder().message("Refresh token không hợp lệ hoặc đã bị thu hồi").build();
        }

        LocalDateTime expiryDate = authOptional.get().getRefreshTokenExpiryDate();
        if (expiryDate != null && expiryDate.isBefore(LocalDateTime.now())) {
            return LoginResponse.builder().message("Refresh token đã hết hạn").build();
        }

        if (!jwtTokenProvider.validateToken(refreshToken) || !jwtTokenProvider.isRefreshToken(refreshToken)) {
            return LoginResponse.builder().message("Refresh token không hợp lệ").build();
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(userId).orElse(null);

        if (user == null || user.getIsDelete()) {
            return LoginResponse.builder().message("User không tồn tại").build();
        }

        Authentication oldAuth = authOptional.get();
        oldAuth.setIsRevoked(true);
        authenticationRepository.save(oldAuth);

        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getEmail());
        LocalDateTime newRefreshTokenExpiryDate = jwtTokenProvider.getExpiryDateFromToken(newRefreshToken);

        Authentication newAuth = Authentication.builder()
                .userId(user.getId())
                .provider("System")
                .refreshToken(newRefreshToken)
                .refreshTokenExpiryDate(newRefreshTokenExpiryDate)
                .isRevoked(false)
                .isDelete(false)
                .build();
        authenticationRepository.save(newAuth);

        log.info("Làm mới token thành công và áp dụng token rotation cho user: {}", user.getEmail());

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .balanceVnd(user.getBalanceVnd())
                .redirectPath(resolveRedirectPath(user.getRole()))
                .message("Làm mới token thành công")
                .build();
    }

    private String resolveRedirectPath(String roleValue) {
        return switch (canonicalRole(roleValue)) {
            case "Admin" -> "/admin/users";
            case "Seller", "Customer_Seller" -> "/seller/dashboard";
            default -> "/";
        };
    }

    private String canonicalRole(String roleValue) {
        if (roleValue == null || roleValue.isBlank()) {
            return "Customer";
        }
        String raw = roleValue.trim();
        if (raw.startsWith("{") && raw.contains("role")) {
            int start = raw.indexOf("\"role\"");
            if (start >= 0) {
                int colon = raw.indexOf(':', start);
                if (colon >= 0) {
                    int firstQuote = raw.indexOf('"', colon + 1);
                    int secondQuote = raw.indexOf('"', firstQuote + 1);
                    if (firstQuote >= 0 && secondQuote > firstQuote) {
                        raw = raw.substring(firstQuote + 1, secondQuote);
                    }
                }
            }
        }
        raw = raw.replace("\"", "").trim();
        String normalized = raw.toLowerCase(Locale.ROOT);
        if (normalized.contains("admin")) {
            return "Admin";
        }
        if (normalized.contains("staff")) {
            return "Staff";
        }
        if (normalized.equals("customer_seller")) {
            return "Customer_Seller";
        }
        if (normalized.contains("seller")) {
            return "Seller";
        }
        return "Customer";
    }

    private void revokeAllUserTokens(Long userId) {
        List<Authentication> validUserTokens = authenticationRepository.findAllByUserIdAndIsRevokedFalse(userId);
        if (validUserTokens.isEmpty()) {
            return;
        }
        validUserTokens.forEach(token -> token.setIsRevoked(true));
        authenticationRepository.saveAll(validUserTokens);
    }

    private String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }
}