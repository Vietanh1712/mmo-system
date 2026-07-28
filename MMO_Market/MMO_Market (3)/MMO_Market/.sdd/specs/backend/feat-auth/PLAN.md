# PLAN — Authentication & Profile Management (`feat-auth`)

## 1. Mục tiêu (Goals)

Triển khai hệ thống xác thực người dùng dựa trên JWT bảo mật, cung cấp tính năng đăng ký có xác minh email qua OTP, đăng nhập bằng OAuth2 Google và quản lý thông tin hồ sơ của người dùng. Các mục tiêu cụ thể:
- Xác thực và phân quyền bằng JWT Access Token & Refresh Token (Token Rotation).
- Hỗ trợ đăng nhập linh hoạt (Hệ thống hoặc Google OAuth2).
- Bảo mật tài khoản bằng OTP 2FA tùy chọn.
- Khóa tài khoản tạm thời sau nhiều lần nhập sai mật khẩu (configurable).
- Quản lý hồ sơ (Profile) cá nhân: cập nhật thông tin, đổi mật khẩu, bật/tắt 2FA, đăng ký mở Shop.
- Hỗ trợ OTP cho luồng rút tiền (tích hợp cross-feature với `feat-wallet`).

## 2. Kiến trúc & Công nghệ

- **Backend:** Java 17, Spring Boot 3.1, Spring Security, JWT (Json Web Token), SQL Server (T-SQL).
- **Frontend:** Các trang Thymeleaf kết hợp JS thuần thực hiện đăng ký/đăng nhập bất đồng bộ và lưu trữ token ở `localStorage`.
- **Tuân thủ:** Mô hình phân lớp Controller → Service → Repository → Entity; DTO Pattern (đóng gói Request/Response sạch sẽ, không trả thực thể JPA ra ngoài API); Toàn bộ mật khẩu người dùng được mã hóa bằng thuật toán `BCryptPasswordEncoder`.

## 3. Các thành phần Backend

### 3.1. Database Migration & Entities

- **Entity `User`** (bảng `Users`):
  - `email` (VARCHAR, UNIQUE): Tài khoản đăng nhập chính.
  - `password` (VARCHAR): Mật khẩu đã được hash bằng BCrypt.
  - `fullName`, `gender`, `address`, `phone`, `dateOfBirth`: Thông tin cá nhân.
  - `nationalId` (VARCHAR): Số CCCD/CMND.
  - `role` (NVARCHAR): Lưu vai trò định dạng JSON (Customer, Seller, Staff, Admin).
  - `isVerified` (BIT): Đánh dấu tài khoản đã được xác thực qua OTP.
  - `isLocked` (BIT): Trạng thái tài khoản (chặn truy cập nếu bị khóa).
  - `failedAttempts` (INT): Số lần nhập sai mật khẩu liên tiếp.
  - `lockTime` (DATETIME): Thời điểm hết hạn khóa tạm thời.
  - `is2faEnabled` (BIT): Tài khoản đã bật xác thực 2 lớp.
  - `shopStatus` (VARCHAR): Trạng thái shop (`Pending`, `Active`...).
  - `balanceVnd` (BIGINT): Số dư ví.
  - `depositVnd` (BIGINT): Tổng tiền đã nạp.
  - `withdrawalLocked` (BIT): Khóa chức năng rút tiền.
  - `suspendedUntil` (DATETIME): Tạm đình chỉ tài khoản đến thời điểm này.
  - `isDelete` (BIT): Soft delete flag.

- **Entity `Authentication`** (bảng `Authentications`):
  - `userId` (BIGINT, FK → Users): Liên kết với tài khoản người dùng.
  - `provider` (VARCHAR): Nguồn xác thực (`System` hoặc `Google`).
  - `refreshToken` (VARCHAR): Refresh Token để gia hạn Access Token.
  - `refreshTokenExpiryDate` (DATETIME): Thời hạn hết hạn của Refresh Token.
  - `isRevoked` (BIT): Token đã bị thu hồi.
  - `isDelete` (BIT): Soft delete flag.

- **Entity `EmailVerification`** (bảng `EmailVerifications`):
  - `userId` (BIGINT, FK → Users): Liên kết với tài khoản người dùng.
  - `verificationCode` (VARCHAR): Mã OTP 6 chữ số.
  - `expiryDate` (DATETIME): Thời gian hết hạn của OTP.
  - `isUsed` (BIT): Đánh dấu đã sử dụng (đốt mã).

### 3.2. Repositories (Spring Data JPA)

- **`UserRepository`**:
  - `existsByEmail(email)`: Kiểm tra email đã tồn tại.
  - `findByEmail(email)`: Tìm tài khoản theo email.
  - `findByEmailAndIsDeleteFalse(email)`: Tìm tài khoản hoạt động theo email.

- **`EmailVerificationRepository`**:
  - `findByUserIdAndVerificationCodeAndIsUsedFalse(userId, code)`: Tìm OTP còn hiệu lực (dùng cho xác thực tài khoản, 2FA login, withdrawal OTP).
  - `findByUserIdAndVerificationCode(userId, code)`: Tìm OTP không kèm điều kiện `isUsed` (dùng cho luồng reset password).

- **`AuthenticationRepository`**:
  - `findByRefreshToken(refreshToken)`: Tìm phiên đăng nhập theo refresh token.
  - `findAllByUserIdAndIsRevokedFalse(userId)`: Lấy toàn bộ token còn hiệu lực của user (để thu hồi hàng loạt — Token Rotation).

### 3.3. DTOs

- **Request DTOs:** `RegisterRequest`, `LoginRequest`, `Login2faRequest`, `VerifyOtpRequest`, `GoogleLoginRequest`, `LogoutRequest`, `RefreshTokenRequest`, `ChangePasswordRequest`, `UpdateProfileRequest`, `ShopRegistrationRequestDto`, `TwoFactorRequest`.
- **Response DTOs:** `RegisterResponse`, `LoginResponse`, `VerifyOtpResponse`, `LogoutResponse`, `ProfileResponse`.

### 3.4. Services (Business Logic)

- **`AuthenticationService`**:
  - `register(request)`: Kiểm tra `ALLOW_REGISTER` config, validate, mã hóa mật khẩu, tạo tài khoản `isVerified = false`, tạo OTP và gửi qua EmailService.
  - `verifyOtp(request)`: Xác nhận OTP (isUsed=false, chưa hết hạn), đốt mã, cập nhật `isVerified = true`.
  - `checkResetOtp(request)`: Kiểm tra OTP reset mật khẩu hợp lệ mà **không đốt mã** — dùng ở Bước 1 của luồng reset.
  - `resendOtp(email)`: Tạo OTP mới, lưu DB và gửi email.
  - `forgotPassword(email)`: Kiểm tra email tồn tại, tạo OTP reset và gửi email.
  - `resetPassword(email, otp, newPassword)`: Validate OTP lần cuối, đốt mã chính thức, lưu mật khẩu mới (BCrypt).
  - `login(request)`: Kiểm tra `isLocked` và `lockTime`, xác thực BCrypt, đếm `failedAttempts`, khóa tài khoản khi vượt `MAX_LOGIN_RETRIES`. Nếu 2FA bật → gửi OTP và trả `requires2FA=true`. Nếu pass → Token Rotation + cấp token.
  - `login2fa(request)`: Xác thực lại email/password + validate OTP 2FA, đốt mã, cấp token.
  - `loginWithGoogle(authCode)`: Kiểm tra `ALLOW_GOOGLE_LOGIN` config, đổi code lấy id_token từ Google, decode lấy email/name, upsert User, cấp token.
  - `logout(refreshToken)`: Đánh dấu `isRevoked = true` cho refreshToken.
  - `refreshAccessToken(refreshToken)`: Kiểm tra tính hợp lệ, thu hồi token cũ (Token Rotation), cấp cặp token mới.
  - `send2faOtp(userId)`: Tạo OTP kích hoạt 2FA, lưu DB, gửi email.
  - `enable2fa(userId, otp)`: Validate OTP, đốt mã, set `is2faEnabled = true`.
  - `disable2fa(userId)`: Set `is2faEnabled = false` (không cần OTP).
  - `sendWithdrawalOtp(user)`: Tạo OTP xác thực rút tiền (cross-feature với `feat-wallet`).
  - `verifyWithdrawalOtp(userId, otp)`: Validate và đốt OTP rút tiền.

- **`UserService`**:
  - `getMyProfile(userId)`: Trả về `ProfileResponse` thông tin chi tiết hồ sơ.
  - `updateMyProfile(userId, request)`: Cập nhật `fullName`, `gender`, `address`, `phone`, `nationalId`, `dateOfBirth`.
  - `changePassword(userId, currentPassword, newPassword)`: Xác minh mật khẩu cũ bằng BCrypt, lưu mật khẩu mới (hash).
  - `registerShop(userId, request)`: Tạo yêu cầu đăng ký mở Shop (lưu vào `SellerRegistrations`).

### 3.5. Controllers & Security

- **`AuthController`** (`@Controller`):
  - Render giao diện Thymeleaf: `GET /login`, `GET /register`, `GET /verify-otp`, `GET /forgot-password`, `GET /reset-password`.
  - REST API Endpoints:
    - `POST /api/auth/register`
    - `POST /api/auth/verify-otp`
    - `POST /api/auth/resend-otp`
    - `POST /api/auth/login`
    - `POST /api/auth/login-2fa`
    - `POST /api/auth/google`
    - `POST /api/auth/logout`
    - `POST /api/auth/refresh`
    - `POST /api/auth/forgot-password`
    - `POST /api/auth/check-reset-otp`
    - `POST /api/auth/reset-password`
    - `GET /api/auth/health`

- **`ProfileController`** (`@RestController`, prefix `/api/v1/profile`):
  - `GET /` — Xem hồ sơ (`getMyProfile`)
  - `PUT /` — Cập nhật thông tin cá nhân (`updateMyProfile`)
  - `PUT /password` — Đổi mật khẩu (`changePassword`)
  - `POST /2fa/send-otp` — Gửi OTP kích hoạt 2FA (`send2faOtp`)
  - `POST /2fa/enable` — Bật 2FA (`enable2fa`)
  - `POST /2fa/disable` — Tắt 2FA (`disable2fa`)
  - `POST /register-shop` — Đăng ký mở Shop (`registerShop`)

- **Security Configuration (`SecurityConfig` & `JwtAuthenticationFilter`):**
  - Chặn và giải mã token Bearer JWT trong Header ở mỗi request.
  - Thiết lập Context Authentication cho Spring Security, gán quyền dựa trên `ROLE_` + Role Name viết hoa.
  - Các endpoint `/api/v1/profile/**` yêu cầu `Authorization: Bearer <token>`.

---

## 4. Các thành phần Frontend

- **Trang Đăng ký:** `templates/auth/register.html` + JS `static/js/customer/auth.js`
- **Trang Đăng nhập:** `templates/auth/login.html` + JS `static/js/customer/auth.js`
  - Tích hợp nút Đăng nhập bằng Google (Google Identity Services).
  - Xử lý luồng 2FA inline (hiện thêm ô OTP sau khi server trả `requires2FA=true`).
- **Trang Xác thực OTP:** `templates/auth/verify-otp.html` + JS `static/js/customer/auth.js`
- **Trang Quên Mật Khẩu:** `templates/auth/forgot-password.html` + JS `static/js/customer/auth.js`
- **Trang Đặt Lại Mật Khẩu:** `templates/auth/reset-password.html` + JS `static/js/customer/auth.js`
  - Giao diện 2 bước inline trên cùng 1 trang (`#step-1`, `#step-2`).
- **Trang Hồ Sơ Cá Nhân:** `templates/profile/index.html` + JS `static/js/customer/profile.js`
- **Lưu trữ Token:** `localStorage.setItem('accessToken', ...)` và `localStorage.setItem('refreshToken', ...)`

---

## 5. Tham Số Cấu Hình (SystemConfigurations)

| Config Key | Mô tả | Default |
|:---|:---|:---|
| `ALLOW_REGISTER` | Bật/tắt chức năng đăng ký | `true` |
| `ALLOW_GOOGLE_LOGIN` | Bật/tắt đăng nhập Google | `true` |
| `OTP_TIMEOUT_MINS` | Thời gian hết hạn OTP (phút) | `5` |
| `MAX_LOGIN_RETRIES` | Số lần sai mật khẩu trước khi khóa | `5` |
| `LOCK_DURATION_MINS` | Thời gian khóa tạm thời (phút) | `15` |

---

## 6. Definition of Done

- Toàn bộ mật khẩu của người dùng trong cơ sở dữ liệu bắt buộc phải được mã hóa bằng BCrypt.
- Các API trong class `ProfileController` (tiền tố `/api/v1/profile/**`) phải được bảo vệ bằng JWT và yêu cầu Header `Authorization: Bearer <token>`.
- Token Refresh Flow hoạt động trơn tru khi Access Token hết hạn (Token Rotation: token cũ bị thu hồi, cặp token mới được cấp).
- Tài khoản bị khóa tạm thời sau `MAX_LOGIN_RETRIES` lần sai mật khẩu và tự động mở khóa khi hết `LOCK_DURATION_MINS`.
- OTP đăng ký tài khoản và OTP reset mật khẩu dùng chung bảng `EmailVerifications` nhưng xử lý theo luồng riêng (đốt mã ở bước khác nhau).