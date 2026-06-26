# SPEC-01: Authentication & Authorization

## 1. Context and Goal
**Goal:** Xây dựng hệ thống định danh, xác thực và quản lý tài khoản cho dự án MMO Market.
**Context:** Hệ thống phục vụ 4 vai trò (Guest/Customer, Seller, Staff, Admin) với các yêu cầu bảo mật khác nhau. Customer có thể đăng ký bằng Email hoặc Google. Mọi thay đổi quan trọng như lấy lại mật khẩu cần OTP qua Email. Người dùng muốn đăng bán sản phẩm phải gửi yêu cầu định danh (KYC).

## 2. Actors
- **Primary:** Guest, Customer, Seller
- **Secondary:** Google OAuth2 Service, Gmail SMTP Service

## 3. Functional Requirements (EARS)
- **FR-01 (Register):** MẶC ĐỊNH (Ubiquitous), hệ thống PHẢI cho phép người dùng đăng ký tài khoản bằng Email và mật khẩu.
- **FR-02 (OTP):** KHI (Event-driven) người dùng đăng ký hoặc quên mật khẩu, hệ thống PHẢI tạo và gửi mã OTP 6 số qua Email.
- **FR-03 (Google Auth):** KHI (Event-driven) người dùng chọn đăng nhập bằng Google, hệ thống PHẢI xác thực qua Google OAuth2 và tự động tạo tài khoản nếu chưa tồn tại.
- **FR-04 (Login):** MẶC ĐỊNH, hệ thống PHẢI cấp phát JWT token khi người dùng cung cấp đúng thông tin đăng nhập.
- **FR-05 (KYC):** KHI người dùng có nhu cầu mở Shop, họ PHẢI gửi yêu cầu định danh (CCCD/CMND) và chờ Staff duyệt.

## 4. Non-Functional Requirements
- **Security:** Mật khẩu phải được băm bằng BCrypt trước khi lưu.
- **Security:** JWT token có thời gian hết hạn (ví dụ: 1 giờ cho Access Token, 7 ngày cho Refresh Token).
- **Usability:** Mã OTP phải hết hạn sau 5 phút.

## 5. Data Model
- **Table `Users`:**
  - `user_id` (PK, BIGINT, Identity)
  - `email` (VARCHAR(100), Unique, Not Null)
  - `password_hash` (VARCHAR(255), Nullable for Google accounts)
  - `role` (INT / Enum)
  - `is_active` (BIT, Default 1)
  - `is_delete` (BIT, Default 0)
- **Table `KYC_Requests`:**
  - `kyc_id` (PK, BIGINT)
  - `user_id` (FK to Users)
  - `id_card_front_url` (VARCHAR(MAX))
  - `id_card_back_url` (VARCHAR(MAX))
  - `status` (Enum: Pending, Approved, Rejected)
  - `created_at` (DATETIME)

## 6. API Specification
- **POST `/api/v1/auth/register`**
  - **Body:** `{ email, password, full_name }`
  - **Response:** 201 Created (Kèm thông báo kiểm tra email)
- **POST `/api/v1/auth/verify-otp`**
  - **Body:** `{ email, otp_code }`
  - **Response:** 200 OK (Kích hoạt tài khoản thành công)
- **POST `/api/v1/auth/login`**
  - **Body:** `{ email, password }`
  - **Response:** 200 OK `{ access_token, refresh_token, role }`
- **POST `/api/v1/kyc/submit`** (Requires: Authentication)
  - **Body:** FormData với ảnh mặt trước và mặt sau CCCD.
  - **Response:** 201 Created

## 7. Error Handling
- `400 Bad Request`: Email sai định dạng, mật khẩu yếu.
- `401 Unauthorized`: Sai tài khoản/mật khẩu, JWT token hết hạn hoặc không hợp lệ.
- `403 Forbidden`: Tài khoản bị khóa (is_active = 0).
- `404 Not Found`: Email không tồn tại trong hệ thống khi quên mật khẩu.
- `409 Conflict`: Email đã tồn tại.

## 8. Acceptance Criteria
- **AC-01:** GIVEN người dùng ở trang đăng ký, WHEN nhập email chưa tồn tại và mật khẩu hợp lệ, THEN hệ thống tạo tài khoản trạng thái inactive và gửi OTP.
- **AC-02:** GIVEN người dùng có tài khoản đã đăng ký qua Google, WHEN cố gắng đăng nhập bằng mật khẩu, THEN hệ thống báo lỗi không hỗ trợ phương thức này cho tài khoản Google.
- **AC-03:** GIVEN User có Role Customer, WHEN nộp hồ sơ KYC hợp lệ, THEN trạng thái KYC thành Pending và chờ Staff duyệt.

## 9. Out of Scope
- Tích hợp đăng nhập bằng Facebook, Apple ID.
- Xác thực 2 bước (2FA) bằng Authenticator App (chỉ dùng OTP qua Email).
