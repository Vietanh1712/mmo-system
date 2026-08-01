package com.mmo.feature.auth.service;
import com.mmo.shared.model.Transaction;
import com.mmo.shared.dal.SystemConfigurationRepository;

import com.mmo.shared.dto.*;
import com.mmo.shared.dal.AuthenticationRepository;
import com.mmo.shared.dal.EmailVerificationRepository;
import com.mmo.shared.dal.UserRepository;
import lombok.extern.slf4j.Slf4j;
import com.mmo.shared.model.Authentication;
import com.mmo.shared.model.EmailVerification;
import com.mmo.shared.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.mmo.shared.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.util.LinkedMultiValueMap;

import java.time.LocalDateTime;
import java.util.List;
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

    @Autowired
    private EmailService emailService;

    @Autowired
    private com.mmo.shared.dal.SystemConfigurationRepository systemConfigurationRepository;

    @Value("${google.oauth2.client-id}${google.oauth2.client-id-suffix:}")
    private String googleClientId;

    @Value("${google.oauth2.client-secret}")
    private String googleClientSecret;

    /**
     * Đăng ký người dùng mới
     */
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        boolean allowRegister = systemConfigurationRepository.findByConfigKey("ALLOW_REGISTER")
                .map(c -> "true".equalsIgnoreCase(c.getConfigValue()) || "1".equals(c.getConfigValue()))
                .orElse(true);
        if (!allowRegister) {
            return RegisterResponse.builder()
                    .success(false)
                    .message("Hệ thống hiện tại đang tạm khóa chức năng đăng ký tài khoản mới.")
                    .build();
        }

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
                .expiryDate(LocalDateTime.now().plusMinutes(getOtpTimeoutMins()))
                .isUsed(false)
                .build();
        emailVerificationRepository.save(emailVerification);

        log.info("Đã sinh mã OTP cho user ID [{}]", savedUser.getId());
        emailService.sendOtpEmail(savedUser.getEmail(), otp);

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

        int otpTimeout = getOtpTimeoutMins();

        // Kiểm tra giới hạn tần suất gửi lại mã (Rate Limiting 60s)
        emailVerificationRepository.findFirstByUserIdOrderByExpiryDateDesc(user.getId())
                .ifPresent(lastOtp -> {
                    if (lastOtp.getExpiryDate().isAfter(LocalDateTime.now().plusMinutes(otpTimeout - 1))) {
                        throw new RuntimeException("Vui lòng chờ ít nhất 60 giây trước khi yêu cầu gửi lại mã.");
                    }
                });

        String newOtp = generateOtp();

        EmailVerification emailVerification = EmailVerification.builder()
                .userId(user.getId())
                .verificationCode(newOtp)
                .expiryDate(LocalDateTime.now().plusMinutes(otpTimeout))
                .isUsed(false)
                .build();
        emailVerificationRepository.save(emailVerification);

        emailService.sendOtpEmail(email, newOtp);
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

        int otpTimeout = getOtpTimeoutMins();

        // Kiểm tra giới hạn tần suất yêu cầu khôi phục (Rate Limiting 60s)
        emailVerificationRepository.findFirstByUserIdOrderByExpiryDateDesc(user.getId())
                .ifPresent(lastOtp -> {
                    if (lastOtp.getExpiryDate().isAfter(LocalDateTime.now().plusMinutes(otpTimeout - 1))) {
                        throw new RuntimeException("Vui lòng chờ ít nhất 60 giây trước khi yêu cầu khôi phục mật khẩu.");
                    }
                });

        String newOtp = generateOtp();

        EmailVerification emailVerification = EmailVerification.builder()
                .userId(user.getId())
                .verificationCode(newOtp)
                .expiryDate(LocalDateTime.now().plusMinutes(otpTimeout))
                .isUsed(false)
                .build();
        emailVerificationRepository.save(emailVerification);

        emailService.sendResetPasswordOtpEmail(email, newOtp);
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
     * Gửi mã OTP để bật 2FA
     */
    @Transactional
    public void send2faOtp(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        
        String otp = generateOtp();
        EmailVerification emailVerification = EmailVerification.builder()
                .userId(user.getId())
                .verificationCode(otp)
                .expiryDate(LocalDateTime.now().plusMinutes(5))
                .isUsed(false)
                .build();
        emailVerificationRepository.save(emailVerification);
        
        emailService.sendOtpEmail(user.getEmail(), otp);
        log.info("Đã gửi OTP kích hoạt 2FA cho user: {}", user.getEmail());
    }

    /**
     * Bật 2FA
     */
    @Transactional
    public void enable2fa(Long userId, String otp) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        
        Optional<EmailVerification> otpOptional = emailVerificationRepository
                .findByUserIdAndVerificationCodeAndIsUsedFalse(userId, otp);
                
        if (otpOptional.isEmpty()) {
            throw new RuntimeException("Mã OTP không hợp lệ hoặc đã được sử dụng.");
        }
        
        EmailVerification emailVerification = otpOptional.get();
        if (emailVerification.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Mã OTP đã hết hạn.");
        }
        
        emailVerification.setIsUsed(true);
        emailVerificationRepository.save(emailVerification);
        
        user.setIs2faEnabled(true);
        userRepository.save(user);
        log.info("Đã bật 2FA cho user: {}", user.getEmail());
    }

    /**
     * Tắt 2FA (Không cần OTP)
     */
    @Transactional
    public void disable2fa(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        user.setIs2faEnabled(false);
        userRepository.save(user);
        log.info("Đã tắt 2FA cho user: {}", user.getEmail());
    }

    /**
     * Đăng nhập người dùng
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        Optional<User> userOptional = userRepository.findByEmailAndIsDeleteFalse(request.getEmail());

        if (userOptional.isEmpty()) {
            return LoginResponse.builder().message("Email hoặc mật khẩu không chính xác").build();
        }

        User user = userOptional.get();

        // 1. Kiểm tra trạng thái khóa
        if (Boolean.TRUE.equals(user.getIsLocked())) {
            if (user.getLockTime() != null) {
                if (user.getLockTime().isAfter(LocalDateTime.now())) {
                    log.warn("Tài khoản {} đang bị khóa tạm thời.", request.getEmail());
                    java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");
                    String unlockTimeStr = user.getLockTime().format(formatter);
                    return LoginResponse.builder()
                            .message("Tài khoản bị khóa tạm thời do nhập sai quá nhiều lần. Sẽ mở khóa vào: " + unlockTimeStr)
                            .build();
                } else {
                    // Quá hạn khóa tạm thời -> Tự động mở khóa
                    user.setIsLocked(false);
                    user.setFailedAttempts(0);
                    user.setLockTime(null);
                    userRepository.save(user);
                    log.info("Tài khoản {} đã hết hạn khóa tạm thời, tự động mở khóa.", request.getEmail());
                }
            } else {
                // Khóa vĩnh viễn bởi Admin
                log.warn("Tài khoản đã bị khóa cố gắng đăng nhập: {}", request.getEmail());
                return LoginResponse.builder().message("Tài khoản của bạn đã bị khóa. Vui lòng liên hệ Admin.").build();
            }
        }

        // 2. Kiểm tra mật khẩu
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            int attempts = (user.getFailedAttempts() == null ? 0 : user.getFailedAttempts()) + 1;
            user.setFailedAttempts(attempts);
            
            int maxRetries = getMaxLoginRetries();
            int lockDuration = getLockDurationMins();
            
            if (attempts >= maxRetries) {
                user.setIsLocked(true);
                user.setLockTime(LocalDateTime.now().plusMinutes(lockDuration));
                userRepository.save(user);
                
                log.warn("Tài khoản {} bị khóa tạm thời trong {} phút do nhập sai {} lần.", 
                        request.getEmail(), lockDuration, attempts);
                return LoginResponse.builder()
                        .message("Bạn đã nhập sai mật khẩu quá " + maxRetries + " lần. Tài khoản bị khóa tạm thời trong " + lockDuration + " phút.")
                        .build();
            } else {
                userRepository.save(user);
                log.warn("Email hoặc mật khẩu không chính xác cho: {}. Lần thử: {}/{}", 
                        request.getEmail(), attempts, maxRetries);
                return LoginResponse.builder()
                        .message("Email hoặc mật khẩu không chính xác. Bạn còn " + (maxRetries - attempts) + " lần thử.")
                        .build();
            }
        }

        // 3. Kiểm tra xác thực email
        if (!user.getIsVerified()) {
            log.warn("User chưa xác thực email: {}", request.getEmail());
            return LoginResponse.builder().message("Vui lòng xác thực email (OTP) trước khi đăng nhập").build();
        }

        // Đăng nhập thành công -> Reset failed attempts
        user.setFailedAttempts(0);
        user.setLockTime(null);
        userRepository.save(user);

        if (Boolean.TRUE.equals(user.getIs2faEnabled())) {
            // Gửi OTP và yêu cầu 2FA
            String otp = generateOtp();
            EmailVerification emailVerification = EmailVerification.builder()
                    .userId(user.getId())
                    .verificationCode(otp)
                    .expiryDate(LocalDateTime.now().plusMinutes(5))
                    .isUsed(false)
                    .build();
            emailVerificationRepository.save(emailVerification);
            
            emailService.sendOtpEmail(user.getEmail(), otp);
            log.info("Yêu cầu 2FA cho user: {}", user.getEmail());
            
            return LoginResponse.builder()
                    .requires2FA(true)
                    .message("Vui lòng nhập mã OTP để tiếp tục")
                    .build();
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
                .message("Đăng nhập thành công")
                .build();
    }

    /**
     * Đăng nhập người dùng (bước 2FA)
     */
    @Transactional
    public LoginResponse login2fa(Login2faRequest request) {
        Optional<User> userOptional = userRepository.findByEmailAndIsDeleteFalse(request.getEmail());

        if (userOptional.isEmpty() || !passwordEncoder.matches(request.getPassword(), userOptional.get().getPassword())) {
            return LoginResponse.builder().message("Email hoặc mật khẩu không chính xác").build();
        }

        User user = userOptional.get();

        if (Boolean.TRUE.equals(user.getIsLocked())) {
            return LoginResponse.builder().message("Tài khoản của bạn đã bị khóa. Vui lòng liên hệ Admin.").build();
        }

        if (!user.getIsVerified()) {
            return LoginResponse.builder().message("Vui lòng xác thực email (OTP) trước khi đăng nhập").build();
        }

        Optional<EmailVerification> otpOptional = emailVerificationRepository
                .findByUserIdAndVerificationCodeAndIsUsedFalse(user.getId(), request.getOtp());

        if (otpOptional.isEmpty()) {
            return LoginResponse.builder().message("Mã OTP không hợp lệ hoặc đã được sử dụng").build();
        }

        EmailVerification emailVerification = otpOptional.get();

        if (emailVerification.getExpiryDate().isBefore(LocalDateTime.now())) {
            return LoginResponse.builder().message("Mã OTP đã hết hạn").build();
        }

        emailVerification.setIsUsed(true);
        emailVerificationRepository.save(emailVerification);

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

        log.info("Đăng nhập 2FA thành công: {}", user.getEmail());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .balanceVnd(user.getBalanceVnd())
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

        if (Boolean.TRUE.equals(user.getIsLocked())) {
            return LoginResponse.builder().message("Tài khoản của bạn đã bị khóa. Vui lòng liên hệ Admin.").build();
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
                .message("Làm mới token thành công")
                .build();
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

    @Transactional
    public LoginResponse loginWithGoogle(String authCode) {
        boolean allowGoogle = systemConfigurationRepository.findByConfigKey("ALLOW_GOOGLE_LOGIN")
                .map(c -> "true".equalsIgnoreCase(c.getConfigValue()) || "1".equals(c.getConfigValue()))
                .orElse(true);
        if (!allowGoogle) {
            throw new RuntimeException("Chức năng đăng nhập bằng Google hiện đang bị khóa.");
        }
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("code", authCode);
            map.add("client_id", googleClientId);
            map.add("client_secret", googleClientSecret);
            map.add("redirect_uri", "postmessage");
            map.add("grant_type", "authorization_code");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
            ResponseEntity<java.util.Map> response = restTemplate.postForEntity(
                    "https://oauth2.googleapis.com/token", request, java.util.Map.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new RuntimeException("Failed to exchange authorization code with Google");
            }

            java.util.Map<String, Object> body = response.getBody();
            String idTokenStr = (String) body.get("id_token");
            if (idTokenStr == null) {
                throw new RuntimeException("Google did not return an ID token");
            }

            // Decode the id_token payload (second part of JWT)
            String[] parts = idTokenStr.split("\\.");
            if (parts.length < 2) {
                throw new RuntimeException("Invalid ID Token format");
            }
            String payloadJson = new String(java.util.Base64.getUrlDecoder().decode(parts[1]), java.nio.charset.StandardCharsets.UTF_8);

            // Parse email and name from payload JSON
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode payload = mapper.readTree(payloadJson);

            String email = payload.has("email") ? payload.get("email").asText() : null;
            String name = payload.has("name") ? payload.get("name").asText() : "Google User";

            if (email == null || email.trim().isEmpty()) {
                throw new RuntimeException("Failed to retrieve email from Google ID Token");
            }

            // Look up or register the user in the database
            Optional<User> userOptional = userRepository.findByEmailAndIsDeleteFalse(email);
            User user;
            if (userOptional.isEmpty()) {
                // Register a new customer
                String roleJson = "{\"role\": \"Customer\"}";
                user = User.builder()
                        .email(email)
                        .fullName(name)
                        .role(roleJson)
                        .isVerified(true) // Verified by Google OAuth
                        .balanceVnd(0L)
                        .isDelete(false)
                        .shopStatus("Pending")
                        .build();
                user = userRepository.save(user);
                log.info("Registered new Google user: {}", email);
            } else {
                user = userOptional.get();
                if (Boolean.TRUE.equals(user.getIsLocked())) {
                    if (user.getLockTime() != null) {
                        if (user.getLockTime().isAfter(LocalDateTime.now())) {
                            log.warn("Tài khoản Google {} đang bị khóa tạm thời.", email);
                            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");
                            String unlockTimeStr = user.getLockTime().format(formatter);
                            throw new RuntimeException("Tài khoản bị khóa tạm thời do nhập sai quá nhiều lần. Sẽ mở khóa vào: " + unlockTimeStr);
                        } else {
                            // Quá hạn khóa tạm thời -> Tự động mở khóa
                            user.setIsLocked(false);
                            user.setFailedAttempts(0);
                            user.setLockTime(null);
                            user = userRepository.save(user);
                            log.info("Tài khoản Google {} đã hết hạn khóa tạm thời, tự động mở khóa.", email);
                        }
                    } else {
                        log.warn("Tài khoản Google đã bị khóa cố gắng đăng nhập: {}", email);
                        throw new RuntimeException("Tài khoản của bạn đã bị khóa. Vui lòng liên hệ Admin.");
                    }
                }
                log.info("Google user logged in: {}", email);
            }

            // Standard login token generation
            revokeAllUserTokens(user.getId());

            String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail());
            String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getEmail());
            LocalDateTime refreshTokenExpiryDate = jwtTokenProvider.getExpiryDateFromToken(refreshToken);

            Authentication auth = Authentication.builder()
                    .userId(user.getId())
                    .provider("Google")
                    .refreshToken(refreshToken)
                    .refreshTokenExpiryDate(refreshTokenExpiryDate)
                    .isRevoked(false)
                    .isDelete(false)
                    .build();
            authenticationRepository.save(auth);

            return LoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .userId(user.getId())
                    .email(user.getEmail())
                    .fullName(user.getFullName())
                    .role(user.getRole())
                    .balanceVnd(user.getBalanceVnd())
                    .message("Đăng nhập bằng Google thành công")
                    .build();

        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            String responseBody = e.getResponseBodyAsString();
            log.error("Google login HTTP error (Status {}): {}", e.getStatusCode(), responseBody, e);
            throw new RuntimeException("Đăng nhập bằng Google thất bại: " + e.getStatusCode() + " - " + responseBody);
        } catch (Exception e) {
            log.error("Google login failed: {}", e.getMessage(), e);
            throw new RuntimeException("Đăng nhập bằng Google thất bại: " + e.getMessage());
        }
    }

    private int getMaxLoginRetries() {
        return systemConfigurationRepository.findByConfigKey("MAX_LOGIN_RETRIES")
                .map(c -> {
                    try { return Integer.parseInt(c.getConfigValue()); }
                    catch (NumberFormatException e) { return 5; }
                }).orElse(5);
    }

    private int getLockDurationMins() {
        return systemConfigurationRepository.findByConfigKey("LOCK_DURATION_MINS")
                .map(c -> {
                    try { return Integer.parseInt(c.getConfigValue()); }
                    catch (NumberFormatException e) { return 15; }
                }).orElse(15);
    }

    private int getOtpTimeoutMins() {
        return systemConfigurationRepository.findByConfigKey("OTP_TIMEOUT_MINS")
                .map(c -> {
                    try { return Integer.parseInt(c.getConfigValue()); }
                    catch (NumberFormatException e) { return 5; }
                }).orElse(5);
    }

    @Transactional
    public void sendWithdrawalOtp(User user) {
        String otp = generateOtp();
        int otpTimeout = getOtpTimeoutMins();

        EmailVerification emailVerification = EmailVerification.builder()
                .userId(user.getId())
                .verificationCode(otp)
                .expiryDate(LocalDateTime.now().plusMinutes(otpTimeout))
                .isUsed(false)
                .build();
        emailVerificationRepository.save(emailVerification);

        log.info("Đã sinh mã OTP xác thực rút tiền cho user ID [{}]", user.getId());
        emailService.sendWithdrawalOtpEmail(user.getEmail(), otp, otpTimeout);
    }

    @Transactional
    public void verifyWithdrawalOtp(Long userId, String otp) {
        Optional<EmailVerification> otpOpt = emailVerificationRepository
                .findByUserIdAndVerificationCodeAndIsUsedFalse(userId, otp);
        if (otpOpt.isEmpty()) {
            throw new IllegalArgumentException("Mã xác thực OTP không chính xác hoặc đã được sử dụng.");
        }
        EmailVerification verification = otpOpt.get();
        if (verification.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Mã xác thực OTP đã hết hạn. Vui lòng yêu cầu mã mới.");
        }
        verification.setIsUsed(true);
        emailVerificationRepository.save(verification);
    }
}