# PLAN — Authentication & Profile Management (`feat-auth`)

## 1. Mục tiêu (Goals)

Triển khai hệ thống xác thực người dùng dựa trên JWT bảo mật, cung cấp tính năng đăng ký có xác minh email qua OTP, đăng nhập bằng OAuth2 Google và quản lý thông tin hồ sơ của người dùng theo đặc tả `SPEC.md` (feat-auth). Các mục tiêu cụ thể:
- Xác thực và phân quyền bằng JWT Access Token & Refresh Token.
- Hỗ trợ đăng nhập linh hoạt (Hệ thống hoặc Google OAuth2).
- Bảo mật tài khoản bằng OTP 2FA tùy chọn.
- Quản lý hồ sơ (Profile) cá nhân của người dùng và cơ chế cập nhật thông tin/đổi mật khẩu.

## 2. Kiến trúc & Công nghệ

- **Backend:** Java 17, Spring Boot 3.1, Spring Security, JWT (Json Web Token), SQL Server (T-SQL).
- **Frontend:** Các trang Thymeleaf kết hợp JS thuần thực hiện đăng ký/đăng nhập bất đồng bộ và lưu trữ token ở `sessionStorage` / `localStorage`.
- **Tuân thủ:** Mô hình phân lớp Controller → Service → Repository → Entity; DTO Pattern (đóng gói Request/Response sạch sẽ, không trả thực thể JPA ra ngoài API); Toàn bộ mật khẩu người dùng được mã hóa bằng thuật toán `BCryptPasswordEncoder`.

## 3. Các thành phần Backend

### 3.1. Database Migration & Entities

- **Entity `User`** (bảng `Users`):
  - `email` (VARCHAR, UNIQUE): Tài khoản đăng nhập chính.
  - `password` (VARCHAR): Mật khẩu đã được hash bằng BCrypt.
  - `fullName`, `gender`, `address`, `phone`, `dateOfBirth`: Thông tin cá nhân.
  - `role` (NVARCHAR): Lưu vai trò định dạng JSON (Customer, Seller, Staff, Admin).
  - `isVerified` (BIT): Đánh dấu tài khoản đã được xác thực qua OTP.
  - `isLocked` (BIT): Trạng thái tài khoản (chặn truy cập nếu bị khóa).
  - `isDelete` (BIT): Soft delete flag.
- **Entity `Authentication`** (bảng `Authentications`):
  - `provider` (VARCHAR): Nguồn xác thực (`System` hoặc `Google`).
  - `refreshToken` (VARCHAR): Refresh Token để gia hạn Access Token.
  - `refreshTokenExpiryDate` (DATETIME): Thời hạn hết hạn của Refresh Token.
- **Entity `EmailVerification`** (bảng `EmailVerifications`):
  - `verificationCode` (VARCHAR): Mã OTP 6 chữ số.
  - `expiryDate` (DATETIME): Thời gian hết hạn của OTP.
  - `isUsed` (BIT): Đánh dấu đã sử dụng.

### 3.2. Repositories (Spring Data JPA)

- `UserRepository`:
  - `findByEmailAndIsDeleteFalse(email)`: Tìm kiếm tài khoản hoạt động.
- `EmailVerificationRepository` (đóng gói trong `EmailVerification`):
  - Kiểm tra và xác thực OTP dựa trên User và mã code chưa sử dụng và chưa hết hạn.
- `AuthenticationRepository` (đóng gói trong `Authentication`):
  - Tìm kiếm và quản lý Refresh Token của người dùng.

### 3.3. DTOs

- Request DTOs: `RegisterRequest`, `LoginRequest`, `Login2faRequest`, `VerifyOtpRequest`, `GoogleLoginRequest`, `ChangePasswordRequest`, `UpdateProfileRequest`.
- Response DTOs: `RegisterResponse`, `LoginResponse`, `VerifyOtpResponse`, `ProfileResponse`.

### 3.4. Services (Business Logic)

- **`AuthenticationService`**:
  - `register(request)`: Validate thông tin, mã hóa mật khẩu, tạo tài khoản ở trạng thái `isVerified = 0`, tạo OTP và gửi qua EmailService.
  - `verifyOtp(request)`: Xác nhận OTP, cập nhật `isVerified = 1`.
  - `login(request)`: Xác thực thông tin đăng nhập, kiểm tra tài khoản có bị khóa không. Nếu user kích hoạt 2FA, yêu cầu gửi thêm OTP. Cấp phát Access Token (JWT) và Refresh Token.
  - `loginWithGoogle(code)`: Đổi authorization code lấy thông tin Google User, tìm tài khoản tương ứng hoặc tự động tạo tài khoản mới nếu chưa tồn tại (OAuth2 Flow).
  - `refreshAccessToken(refreshToken)`: Kiểm tra tính hợp lệ của Refresh Token để cấp phát Access Token mới.
  - `logout(refreshToken)`: Hủy bỏ Refresh Token.
- **`UserService`**:
  - `getMyProfile(userId)`: Trả về thông tin chi tiết hồ sơ cá nhân.
  - `updateProfile(userId, request)`: Cập nhật thông tin `fullName`, `address`, `phone`, `dateOfBirth`.
  - `changePassword(userId, request)`: Xác minh mật khẩu cũ và cập nhật mật khẩu mới (hash qua BCrypt).

### 3.5. Controllers & Security

- **`AuthController`**:
  - Render giao diện Thymeleaf: `/login`, `/register`, `/verify-otp`, `/forgot-password`, `/reset-password`.
  - REST API Endpoints: `/api/auth/register`, `/api/auth/verify-otp`, `/api/auth/login`, `/api/auth/login-2fa`, `/api/auth/google`, `/api/auth/logout`, `/api/auth/refresh`, `/api/auth/forgot-password`, `/api/auth/reset-password`.
- **`ProfileController`** (`/api/v1/profile`):
  - `GET /`: Xem hồ sơ.
  - `PUT /`: Cập nhật thông tin cá nhân.
  - `POST /register-shop`: Chuyển đổi trạng thái đăng ký Shop (yêu cầu role `CUSTOMER`).
- **Security Configuration (`SecurityConfig` & `JwtAuthenticationFilter`):**
  - Chặn và giải mã token Bearer JWT trong Header ở mỗi request.
  - Thiết lập Context Authentication cho Spring Security, gán quyền dựa trên `ROLE_` + Role Name viết hoa.

---

## 4. Các thành phần Frontend

- **Trang Đăng ký / Đăng nhập:**
  - File: `templates/auth/login.html` & `templates/auth/register.html`.
  - Tích hợp nút Đăng nhập bằng Google (Google OAuth2).
  - Tự động lưu Token vào `sessionStorage` sau khi đăng nhập thành công.
- **Trang Xác thực OTP:**
  - File: `templates/auth/verify-otp.html`.
- **Trang Thông tin Cá nhân:**
  - File: `templates/profile.html` và JS `static/js/customer/profile.js`.

---

## 5. Definition of Done

- Toàn bộ mật khẩu của người dùng trong cơ sở dữ liệu bắt buộc phải được mã hóa bằng BCrypt.
- Các API trong class `ProfileController` (tiền tố `/api/v1/profile/**`) phải được bảo vệ bằng JWT và yêu cầu Header `Authorization: Bearer <token>`.
- Token Refresh Flow hoạt động trơn tru khi Access Token hết hạn.