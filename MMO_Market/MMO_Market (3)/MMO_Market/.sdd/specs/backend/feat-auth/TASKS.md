# TASKS — Authentication & Profile Management (`feat-auth`)

> **Feature ID:** `feat-auth` | **UC Coverage:** UC-01 (Authentication), UC-02 (User Profile)
> **Phiên bản:** 1.0 | **Cập nhật:** 2026-06-27

---

## Phase 1: Database & Entities

- [x] **1.1** Schema `MMO_System_Schema.sql` đã tạo đầy đủ bảng `Users`, `Authentications`, và `EmailVerifications`.
- [x] **1.2** JPA Entity `User` (`User.java`) — map trường `role` dạng JSON string, trường `balance_vnd` số nguyên lớn, `permissions`, và trạng thái `isVerified`, `isLocked`, `isDelete`.
- [x] **1.3** JPA Entity `Authentication` — thiết lập quan hệ `@ManyToOne` với `User` và lưu vết JWT refresh token.
- [x] **1.4** JPA Entity `EmailVerification` — map đúng mã OTP 6 số và trường hạn dùng `expiry_date`.

## Phase 2: Repositories

- [x] **2.1** `UserRepository` — cung cấp các truy vấn tìm kiếm `findByEmail` và kiểm tra tồn tại `existsByEmail`.
- [x] **2.2** `EmailVerificationRepository` — truy vấn mã OTP hợp lệ qua `findByUserIdAndVerificationCodeAndIsUsedFalse`.
- [x] **2.3** `AuthenticationRepository` — tìm kiếm refresh token qua `findByRefreshToken`.

## Phase 3: DTOs & Validation

- [x] **3.1** `RegisterRequest` / `LoginRequest` — khai báo các ràng buộc `@NotBlank`, `@Size` trên email và password.
- [x] **3.2** `ProfileResponse` — ánh xạ sạch sẽ thông tin người dùng mà không bao gồm trường mật khẩu thô.
- [x] **3.3** `UpdateProfileRequest` / `ChangePasswordRequest` — validate đầy đủ đầu vào.

## Phase 4: Business Logic (Services)

- [x] **4.1** `AuthenticationService.register()` — thực hiện mã hóa mật khẩu qua `BCryptPasswordEncoder`, tạo tài khoản dạng chưa xác thực, và sinh mã OTP gửi mail.
- [x] **4.2** `AuthenticationService.verifyOtp()` — xác thực mã OTP chính xác và chuyển trạng thái `isVerified = true`.
- [x] **4.3** `AuthenticationService.login()` — xác thực credentials và trả về cặp AccessToken/RefreshToken.
- [x] **4.4** `UserService` — thực hiện cập nhật hồ sơ cá nhân và thay đổi mật khẩu an toàn.

## Phase 5: Controllers & Security

- [x] **5.1** `AuthController` — xử lý các trang MVC `/login`, `/register` và các API công khai `/api/auth/**`.
- [x] **5.2** `ProfileController` — bảo vệ API `/api/v1/profile/**` yêu cầu quyền người dùng đăng nhập hợp lệ qua cấu hình Spring Security.

## Phase 6: Testing

- [x] **6.1** Unit Tests `UserServiceTest` — kiểm thử thành công luồng cập nhật thông tin cá nhân và thay đổi mật khẩu.
- [x] **6.2** Integration Tests — kiểm tra tính đúng đắn của token JWT khi gửi yêu cầu lên các API được bảo vệ.

## Phase 7: Final Review

- [x] **7.1** Đảm bảo mật khẩu không bao giờ được ghi vào nhật ký hệ thống (logs) ở dạng thô.
- [x] **7.2** Check quy tắc code gọn gàng, không thừa dòng code test.