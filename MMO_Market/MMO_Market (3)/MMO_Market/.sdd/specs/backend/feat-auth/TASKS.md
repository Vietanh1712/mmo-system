# TASKS — Authentication & Profile Management (`feat-auth`)

> **Feature ID:** `feat-auth` | **UC Coverage:** UC-01 (Authentication), UC-02 (User Profile), UC-04 (Account Security)

---

## Phase 1: Database & Entities

- [x] **1.1** Schema `MMO_System_Schema.sql` đã tạo đầy đủ bảng `Users`, `Authentications`, và `EmailVerifications`.
- [x] **1.2** JPA Entity `User` (`User.java`) — map trường `role` dạng JSON string, `balance_vnd`, `deposit_vnd`, `permissions`, `isVerified`, `isLocked`, `isDelete`, `failed_attempts`, `lock_time`, `is_2fa_enabled`, `shop_status`, `shop_level`, `withdrawal_locked`, `suspended_until`, `flag_3_count`.
- [x] **1.3** JPA Entity `Authentication` — thiết lập quan hệ `@ManyToOne` với `User`, lưu refresh token, `is_revoked`, `provider` (`System`/`Google`).
- [x] **1.4** JPA Entity `EmailVerification` — map mã OTP 6 số, `expiry_date`, `is_used`. Dùng chung cho OTP đăng ký, reset mật khẩu, 2FA login, bật 2FA, và OTP rút tiền.

## Phase 2: Repositories

- [x] **2.1** `UserRepository` — `findByEmail`, `existsByEmail`, `findByEmailAndIsDeleteFalse`.
- [x] **2.2** `EmailVerificationRepository` — `findByUserIdAndVerificationCodeAndIsUsedFalse` (xác thực OTP còn hiệu lực); `findByUserIdAndVerificationCode` (check reset OTP không đốt mã).
- [x] **2.3** `AuthenticationRepository` — `findByRefreshToken`; `findAllByUserIdAndIsRevokedFalse` (thu hồi hàng loạt khi Token Rotation).

## Phase 3: DTOs & Validation

- [x] **3.1** Request DTOs: `RegisterRequest`, `LoginRequest` — `@NotBlank`, `@Size` trên email và password.
- [x] **3.2** Request DTOs bổ sung: `Login2faRequest`, `GoogleLoginRequest`, `LogoutRequest`, `RefreshTokenRequest`, `VerifyOtpRequest`, `ChangePasswordRequest`, `UpdateProfileRequest`, `ShopRegistrationRequestDto`, `TwoFactorRequest`.
- [x] **3.3** Response DTOs: `RegisterResponse`, `LoginResponse` (gồm `accessToken`, `refreshToken`, `userId`, `email`, `fullName`, `role`, `balanceVnd`), `VerifyOtpResponse`, `ProfileResponse`, `LogoutResponse`.

## Phase 4: Business Logic (Services)

### AuthenticationService

- [x] **4.1** `register()` — check `ALLOW_REGISTER` config, BCrypt mật khẩu, tạo tài khoản `isVerified=false`, tạo OTP (thời hạn = `OTP_TIMEOUT_MINS`), gửi email.
- [x] **4.2** `verifyOtp()` — xác thực OTP (isUsed=false, chưa hết hạn), đốt mã (`isUsed=true`), cập nhật `isVerified=true`.
- [x] **4.3** `resendOtp()` — tạo OTP mới, lưu `EmailVerifications`, gửi email.
- [x] **4.4** `login()` — check `isLocked` + `lockTime` (tự mở khóa nếu hết hạn), xác thực BCrypt, tăng `failedAttempts`, khóa TK khi >= `MAX_LOGIN_RETRIES`, branch 2FA (gửi OTP → trả `requires2FA`), Token Rotation (thu hồi toàn bộ token cũ), cấp cặp token mới.
- [x] **4.5** `login2fa()` — xác thực lại email/password + validate OTP 2FA, đốt mã, Token Rotation, cấp token.
- [x] **4.6** `loginWithGoogle()` — check `ALLOW_GOOGLE_LOGIN` config, đổi `authCode` lấy `id_token` từ Google, decode lấy email/name, upsert User (`isVerified=true`), cấp token.
- [x] **4.7** `logout()` — tìm `Authentication` theo `refreshToken`, đánh dấu `isRevoked=true`.
- [x] **4.8** `refreshAccessToken()` — validate JWT + `isRevoked`, kiểm tra `refreshTokenExpiryDate`, Token Rotation (thu hồi cũ, cấp mới).
- [x] **4.9** `forgotPassword()` — tìm user theo email, tạo OTP reset, gửi email.
- [x] **4.10** `checkResetOtp()` — validate OTP (isUsed=false, chưa hết hạn) **mà không đốt mã** — dùng ở Bước 1 của luồng reset.
- [x] **4.11** `resetPassword()` — validate OTP lần cuối, đốt mã chính thức, BCrypt mật khẩu mới, lưu DB.
- [x] **4.12** `send2faOtp()` — tạo OTP kích hoạt 2FA, gửi email.
- [x] **4.13** `enable2fa()` — validate OTP, đốt mã, set `is2faEnabled=true`.
- [x] **4.14** `disable2fa()` — set `is2faEnabled=false` (không cần OTP).
- [x] **4.15** `sendWithdrawalOtp()` — tạo OTP xác thực rút tiền (cross-feature: gọi từ `feat-wallet`).
- [x] **4.16** `verifyWithdrawalOtp()` — validate và đốt OTP rút tiền.

### UserService

- [x] **4.17** `getMyProfile()` — trả về `ProfileResponse` chi tiết hồ sơ.
- [x] **4.18** `updateMyProfile()` — validate, cập nhật `fullName`, `gender`, `address`, `phone`, `nationalId`, `dateOfBirth`.
- [x] **4.19** `changePassword()` — xác minh mật khẩu cũ bằng BCrypt, lưu mật khẩu mới (hash).
- [x] **4.20** `registerShop()` — tạo yêu cầu đăng ký mở Shop, lưu vào `SellerRegistrations`.

## Phase 5: Controllers & Security

### AuthController (MVC + REST)

- [x] **5.1** MVC: render Thymeleaf cho `GET /login`, `/register`, `/verify-otp`, `/forgot-password`, `/reset-password`.
- [x] **5.2** REST `/api/auth/register` — đăng ký tài khoản.
- [x] **5.3** REST `/api/auth/verify-otp` — xác thực OTP đăng ký.
- [x] **5.4** REST `/api/auth/resend-otp` — gửi lại OTP.
- [x] **5.5** REST `/api/auth/login` — đăng nhập (có branch 2FA).
- [x] **5.6** REST `/api/auth/login-2fa` — đăng nhập bước 2 (xác thực OTP 2FA).
- [x] **5.7** REST `/api/auth/google` — đăng nhập Google OAuth2.
- [x] **5.8** REST `/api/auth/logout` — đăng xuất (thu hồi refresh token).
- [x] **5.9** REST `/api/auth/refresh` — làm mới token (Token Rotation).
- [x] **5.10** REST `/api/auth/forgot-password` — yêu cầu OTP reset mật khẩu.
- [x] **5.11** REST `/api/auth/check-reset-otp` — kiểm tra OTP reset (không đốt mã).
- [x] **5.12** REST `/api/auth/reset-password` — đặt mật khẩu mới (đốt mã OTP chính thức).
- [x] **5.13** REST `/api/auth/health` — health check endpoint.

### ProfileController (`/api/v1/profile/**`)

- [x] **5.14** `GET /` — xem hồ sơ cá nhân. Yêu cầu JWT.
- [x] **5.15** `PUT /` — cập nhật thông tin cá nhân. Yêu cầu JWT.
- [x] **5.16** `PUT /password` — đổi mật khẩu. Yêu cầu JWT.
- [x] **5.17** `POST /2fa/send-otp` — gửi OTP kích hoạt 2FA. Yêu cầu JWT.
- [x] **5.18** `POST /2fa/enable` — bật 2FA bằng OTP. Yêu cầu JWT.
- [x] **5.19** `POST /2fa/disable` — tắt 2FA. Yêu cầu JWT.
- [x] **5.20** `POST /register-shop` — đăng ký mở Shop. Yêu cầu JWT + role Customer.

### Security Infrastructure

- [x] **5.21** `JwtAuthenticationFilter` — chặn mọi request, giải mã Bearer JWT, set `SecurityContext`.
- [x] **5.22** `SecurityConfig` — whitelist các endpoint public (`/api/auth/**`, trang MVC), bảo vệ `/api/v1/profile/**`.

## Phase 6: Testing

- [x] **6.1** Unit Tests `UserServiceTest` — kiểm thử luồng cập nhật thông tin cá nhân và thay đổi mật khẩu.
- [x] **6.2** Integration Tests — kiểm tra tính đúng đắn của token JWT khi gửi yêu cầu lên các API được bảo vệ.

## Phase 7: Final Review

- [x] **7.1** Đảm bảo mật khẩu không bao giờ được ghi vào nhật ký hệ thống (logs) ở dạng thô.
- [x] **7.2** Check quy tắc code gọn gàng, không thừa dòng code test.
- [x] **7.3** Xác nhận `EmailVerifications` dùng chung cho tất cả luồng OTP mà không gây xung đột (phân biệt bằng context endpoint, không cần column type riêng).
- [x] **7.4** Xác nhận Token Rotation hoạt động đúng: `revokeAllUserTokens` được gọi trước mỗi lần cấp token mới (login, login2fa, loginWithGoogle, refreshAccessToken).