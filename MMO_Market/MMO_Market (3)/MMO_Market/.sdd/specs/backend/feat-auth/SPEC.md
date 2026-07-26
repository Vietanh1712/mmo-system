# SPEC — Authentication & Profile Management

> **Feature ID:** `feat-auth`
> **UC Coverage:** UC-01 (Authentication), UC-02 (User Profile), UC-04 (Account Security)

---

## 1. CONTEXT & GOAL (BỐI CẢNH & MỤC TIÊU)

### 1.1 Bối cảnh
Hệ thống MMO Market yêu cầu kiểm soát định danh người dùng chính xác để thực hiện các giao dịch liên quan đến ví điện tử và chuyển tiền an toàn. Việc đăng ký, đăng nhập và bảo mật thông tin cá nhân là nền tảng của mọi giao dịch C2C sản phẩm số.

### 1.2 Mục tiêu
- Cung cấp cơ chế đăng ký tài khoản mới bằng Email/Password cùng mã xác thực OTP gửi qua email.
- Cho phép đăng nhập linh hoạt qua tài khoản hệ thống hoặc Google OAuth2.
- Bảo mật tài khoản bằng OTP 2FA tùy chọn.
- Khóa tài khoản tạm thời sau nhiều lần đăng nhập sai (configurable).
- Cấp và quản lý access token, refresh token bằng cơ chế JWT Token Rotation.
- Cho phép người dùng chỉnh sửa thông tin cá nhân, thay đổi mật khẩu, bật/tắt 2FA, đăng ký mở Shop.
- Hỗ trợ OTP cho luồng rút tiền (cross-feature với `feat-wallet`).

### 1.3 Tại sao cần?
Bảo vệ tài sản người dùng khỏi việc đánh cắp tài khoản, ngăn chặn truy cập trái phép vào các endpoint nhạy cảm (như mua hàng, rút tiền) và lưu vết thông tin người dùng phục vụ KYC.

---

## 2. ACTOR (TÁC NHÂN)

| Actor | Role | Điều kiện tiền quyết |
|:---|:---|:---|
| **Guest** | Khách vãng lai | Chưa có tài khoản hoặc chưa đăng nhập |
| **User** | Người dùng hệ thống | Tài khoản đã được kích hoạt (`isVerified = 1`, `isLocked = 0`) |
| **Google OAuth2** | Bên thứ ba | Xác thực danh tính qua Google account |
| **SystemConfiguration** | Cấu hình hệ thống | Kiểm soát hành vi: `ALLOW_REGISTER`, `ALLOW_GOOGLE_LOGIN`, `OTP_TIMEOUT_MINS`, `MAX_LOGIN_RETRIES`, `LOCK_DURATION_MINS` |

---

## 3. FUNCTIONAL REQUIREMENTS (Cú pháp EARS)

### 3.1 Đăng ký & Xác thực OTP

| ID | EARS Requirement |
|:---|:---|
| FR-AUTH-01 | WHEN a Guest submits registration details, THE SYSTEM SHALL check `ALLOW_REGISTER` config, validate inputs and generate a 6-digit OTP verification code stored in `EmailVerifications`. |
| FR-AUTH-02 | WHEN a Guest verifies the registration using the correct, non-expired OTP code, THE SYSTEM SHALL mark the OTP as used and update the user account to `isVerified = true`. |
| FR-AUTH-03 | IF the OTP verification code is expired or already used, THEN THE SYSTEM SHALL return a `400 Bad Request` with an appropriate message. |
| FR-AUTH-04 | WHEN a Guest requests to resend the OTP, THE SYSTEM SHALL generate a new OTP record and send it via email. |

### 3.2 Đăng nhập & JWT

| ID | EARS Requirement |
|:---|:---|
| FR-AUTH-05 | WHEN a Guest logs in with correct email and password, THE SYSTEM SHALL revoke all existing tokens (Token Rotation) and generate a new JWT Access Token and Refresh Token. |
| FR-AUTH-06 | IF a user has `is2faEnabled = true` and login credentials are correct, THE SYSTEM SHALL send a 2FA OTP to email and return `requires2FA: true` without issuing tokens. |
| FR-AUTH-07 | WHEN a Guest completes 2FA by submitting the correct OTP, THE SYSTEM SHALL revoke old tokens and issue a new token pair. |
| FR-AUTH-08 | IF user logs in via Google OAuth2 and `ALLOW_GOOGLE_LOGIN = true`, THE SYSTEM SHALL find or auto-create the user profile using Google account info (with `isVerified = true`). |
| FR-AUTH-09 | WHILE the Access Token is expired but the Refresh Token is still valid, THE SYSTEM SHALL revoke the old Refresh Token and issue a new token pair (Token Rotation). |
| FR-AUTH-10 | IF a user enters incorrect password, THE SYSTEM SHALL increment `failedAttempts`. When `failedAttempts >= MAX_LOGIN_RETRIES`, the account SHALL be locked for `LOCK_DURATION_MINS` minutes. |
| FR-AUTH-11 | IF a locked account's `lockTime` has passed, THE SYSTEM SHALL automatically unlock it (`isLocked = false`, `failedAttempts = 0`) on the next login attempt. |
| FR-AUTH-12 | WHEN a User logs out by providing a valid Refresh Token, THE SYSTEM SHALL mark the token as `isRevoked = true`. |

### 3.3 Quên & Đặt Lại Mật Khẩu

| ID | EARS Requirement |
|:---|:---|
| FR-AUTH-13 | WHEN a Guest submits a forgot-password request with a registered email, THE SYSTEM SHALL generate a reset OTP and send it via email. |
| FR-AUTH-14 | WHEN a Guest validates the reset OTP via `check-reset-otp`, THE SYSTEM SHALL verify OTP validity WITHOUT marking it as used. |
| FR-AUTH-15 | WHEN a Guest submits a new password along with the valid OTP, THE SYSTEM SHALL BCrypt-encode and save the new password, then mark the OTP as used. |

### 3.4 Hồ sơ & Bảo mật tài khoản

| ID | EARS Requirement |
|:---|:---|
| FR-AUTH-16 | WHEN an authenticated User requests their profile, THE SYSTEM SHALL return User detail along with active role and balance. |
| FR-AUTH-17 | WHEN a User updates profile fields (`fullName`, `gender`, `address`, `phone`, `nationalId`, `dateOfBirth`), THE SYSTEM SHALL validate inputs and update the `Users` table. |
| FR-AUTH-18 | WHEN a User changes password by providing the current password and a new password, THE SYSTEM SHALL verify the current password via BCrypt before saving the new one. |
| FR-AUTH-19 | WHEN a User enables 2FA by sending OTP and verifying it, THE SYSTEM SHALL set `is2faEnabled = true`. |
| FR-AUTH-20 | WHEN a User disables 2FA, THE SYSTEM SHALL set `is2faEnabled = false` without requiring OTP. |

---

## 4. NON-FUNCTIONAL REQUIREMENTS

| ID | Category | Requirement |
|:---|:---|:---|
| NFR-AUTH-01 | Security | Toàn bộ mật khẩu phải được mã hóa bằng thuật toán `BCrypt`. |
| NFR-AUTH-02 | Security | Các endpoint `/api/v1/profile/**` bắt buộc xác thực JWT Bearer Token. |
| NFR-AUTH-03 | Performance | Thời gian phản hồi API đăng nhập và xác thực phải < 300ms (p95). |
| NFR-AUTH-04 | Security | Refresh Token phải bị thu hồi ngay sau khi sử dụng (Token Rotation). |
| NFR-AUTH-05 | Configurability | Các tham số bảo mật (`MAX_LOGIN_RETRIES`, `LOCK_DURATION_MINS`, `OTP_TIMEOUT_MINS`) phải đọc động từ `SystemConfigurations`, không hardcode. |

---

## 5. DATA MODEL (Mô hình dữ liệu)

```sql
CREATE TABLE Users (
    id                BIGINT IDENTITY(1,1) PRIMARY KEY,
    email             VARCHAR(255) NOT NULL UNIQUE,
    password          VARCHAR(255) NULL,               -- NULL nếu đăng ký qua Google
    full_name         NVARCHAR(255) NULL,
    gender            NVARCHAR(20) NULL,
    address           NVARCHAR(500) NULL,
    national_id       VARCHAR(20) NULL,
    date_of_birth     DATE NULL,
    phone             VARCHAR(20) NULL,
    role              NVARCHAR(MAX) NOT NULL,           -- JSON: {"role": "Customer|Seller|Staff|Admin"}
    shop_status       VARCHAR(20) DEFAULT 'Pending',
    balance_vnd       BIGINT DEFAULT 0,
    deposit_vnd       BIGINT DEFAULT 0,
    permissions       NVARCHAR(MAX) NULL,
    isVerified        BIT DEFAULT 0,
    isLocked          BIT DEFAULT 0,
    failed_attempts   INT DEFAULT 0,                   -- Đếm số lần nhập sai mật khẩu
    lock_time         DATETIME NULL,                   -- Thời điểm hết hạn khóa tạm thời
    is_2fa_enabled    BIT DEFAULT 0,
    shop_level        INT NULL,
    flag_3_count      INT DEFAULT 0,
    withdrawal_locked BIT DEFAULT 0,
    suspended_until   DATETIME NULL,
    created_at        DATETIME DEFAULT GETDATE(),
    updated_at        DATETIME DEFAULT GETDATE(),
    isDelete          BIT DEFAULT 0
);

CREATE TABLE Authentications (
    id                         BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id                    BIGINT NOT NULL,
    provider                   VARCHAR(50) NOT NULL,    -- 'System' hoặc 'Google'
    refresh_token              VARCHAR(512) NULL,
    refresh_token_expiry_date  DATETIME NULL,
    is_revoked                 BIT DEFAULT 0,
    created_at                 DATETIME DEFAULT GETDATE(),
    isDelete                   BIT DEFAULT 0,
    CONSTRAINT FK_Auth_Users FOREIGN KEY (user_id) REFERENCES Users(id)
);

CREATE TABLE EmailVerifications (
    id                 BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id            BIGINT NOT NULL,
    verification_code  VARCHAR(6) NOT NULL,
    expiry_date        DATETIME NOT NULL,
    is_used            BIT DEFAULT 0,
    CONSTRAINT FK_Email_Users FOREIGN KEY (user_id) REFERENCES Users(id)
);
```

> **Lưu ý:** Bảng `EmailVerifications` dùng chung cho OTP đăng ký, OTP reset mật khẩu, OTP 2FA login, OTP bật 2FA, và OTP rút tiền. Phân biệt bằng context xử lý (endpoint gọi), không phải column riêng.

---

## 6. API SPEC (Đặc tả API)

### `POST /api/auth/register`
- **Request Body:**
  ```json
  { "email": "user@example.com", "password": "Pass123!", "fullName": "Nguyen Van A", "phone": "0987654321" }
  ```
- **Response 201:**
  ```json
  { "success": true, "userId": 1, "email": "user@example.com", "fullName": "Nguyen Van A", "message": "Đăng ký thành công. Vui lòng kiểm tra email để nhận mã OTP xác thực tài khoản." }
  ```
- **Response 400:** `{ "success": false, "message": "Email này đã được đăng ký" }`

---

### `POST /api/auth/verify-otp`
- **Request Body:** `{ "email": "user@example.com", "otp": "123456" }`
- **Response 200:** `{ "success": true, "message": "Xác thực tài khoản thành công" }`
- **Response 400:** `{ "success": false, "message": "Mã OTP không hợp lệ hoặc đã được sử dụng" }`

---

### `POST /api/auth/resend-otp`
- **Request Body:** `{ "email": "user@example.com" }`
- **Response 200:** `{ "success": true, "message": "Một mã OTP mới đã được gửi" }`

---

### `POST /api/auth/login`
- **Request Body:** `{ "email": "user@example.com", "password": "Pass123!" }`
- **Response 200 (đăng nhập thành công):**
  ```json
  { "accessToken": "eyJ...", "refreshToken": "abc...", "userId": 1, "email": "user@example.com", "fullName": "Nguyen Van A", "role": "{\"role\": \"Customer\"}", "balanceVnd": 150000, "message": "Đăng nhập thành công" }
  ```
- **Response 200 (yêu cầu 2FA):**
  ```json
  { "requires2FA": true, "message": "Vui lòng nhập mã OTP để tiếp tục" }
  ```
- **Response 401:** `{ "message": "Email hoặc mật khẩu không chính xác. Bạn còn N lần thử." }`
- **Response 200 (tài khoản bị khóa):** `{ "message": "Tài khoản bị khóa tạm thời do nhập sai quá nhiều lần. Sẽ mở khóa vào: HH:mm:ss dd/MM/yyyy" }`

---

### `POST /api/auth/login-2fa`
- **Request Body:** `{ "email": "user@example.com", "password": "Pass123!", "otp": "123456" }`
- **Response 200:** *(giống response đăng nhập thành công)*

---

### `POST /api/auth/google`
- **Request Body:** `{ "code": "<google_authorization_code>" }`
- **Response 200:** *(giống response đăng nhập thành công, message: "Đăng nhập bằng Google thành công")*
- **Response 400:** `{ "success": false, "message": "Chức năng đăng nhập bằng Google hiện đang bị khóa." }`

---

### `POST /api/auth/logout`
- **Request Body:** `{ "refreshToken": "abc..." }`
- **Response 200:** `{ "success": true, "message": "Đăng xuất thành công" }`

---

### `POST /api/auth/refresh`
- **Request Body:** `{ "refreshToken": "abc..." }`
- **Response 200:** *(giống response đăng nhập thành công, message: "Làm mới token thành công")*
- **Response 401:** `{ "message": "Refresh token không hợp lệ hoặc đã bị thu hồi" }`

---

### `POST /api/auth/forgot-password`
- **Request Body:** `{ "email": "user@example.com" }`
- **Response 200:** `{ "success": true, "message": "Mã khôi phục đã được gửi" }`

---

### `POST /api/auth/check-reset-otp`
- **Request Body:** `{ "email": "user@example.com", "otp": "123456" }`
- **Response 200:** `{ "success": true, "message": "Mã OTP hợp lệ" }`
- **Response 400:** `{ "success": false, "message": "Mã OTP đã hết hạn" }`

---

### `POST /api/auth/reset-password`
- **Request Body:** `{ "email": "user@example.com", "otp": "123456", "newPassword": "NewPass123!" }`
- **Response 200:** `{ "success": true, "message": "Đổi mật khẩu thành công" }`

---

### `GET /api/v1/profile`
- **Headers:** `Authorization: Bearer <JWT_Token>`
- **Response 200:**
  ```json
  { "id": 1, "email": "user@example.com", "fullName": "Nguyen Van A", "phone": "0987654321", "gender": "Nam", "address": "Ha Noi", "dateOfBirth": "2000-01-01", "nationalId": "001090123456", "balanceVnd": 150000, "role": "{\"role\": \"Customer\"}", "isVerified": true, "is2faEnabled": false, "shopStatus": "Pending" }
  ```

---

### `PUT /api/v1/profile`
- **Headers:** `Authorization: Bearer <JWT_Token>`
- **Request Body:** `{ "fullName": "Nguyen Van B", "phone": "0912345678", "gender": "Nam", "address": "Ha Noi", "nationalId": "001090123456", "dateOfBirth": "2000-01-01" }`
- **Response 200:** *(ProfileResponse giống GET)*

---

### `PUT /api/v1/profile/password`
- **Headers:** `Authorization: Bearer <JWT_Token>`
- **Request Body:** `{ "currentPassword": "OldPass123!", "newPassword": "NewPass456!" }`
- **Response 200:** `{ "success": true, "message": "Đổi mật khẩu thành công" }`
- **Response 400:** `{ "success": false, "message": "Mật khẩu hiện tại không đúng" }`

---

### `POST /api/v1/profile/2fa/send-otp`
- **Headers:** `Authorization: Bearer <JWT_Token>`
- **Response 200:** `{ "success": true, "message": "Mã OTP đã được gửi về email của bạn" }`

---

### `POST /api/v1/profile/2fa/enable`
- **Headers:** `Authorization: Bearer <JWT_Token>`
- **Request Body:** `{ "otp": "123456" }`
- **Response 200:** `{ "success": true, "message": "Đã bật bảo mật 2 lớp thành công" }`

---

### `POST /api/v1/profile/2fa/disable`
- **Headers:** `Authorization: Bearer <JWT_Token>`
- **Response 200:** `{ "success": true, "message": "Đã tắt bảo mật 2 lớp" }`

---

### `POST /api/v1/profile/register-shop`
- **Headers:** `Authorization: Bearer <JWT_Token>`
- **Request Body:** `{ "shopName": "...", "description": "...", "category": "...", "supportEmail": "...", "supportPhone": "..." }`
- **Response 200:** *(ProfileResponse)*

---

## 7. ERROR HANDLING (Xử lý lỗi)

| HTTP Code | Tình huống | Message |
|---|---|---|
| 400 | Email đã tồn tại khi đăng ký | "Email này đã được đăng ký" |
| 400 | OTP sai hoặc đã dùng | "Mã OTP không hợp lệ hoặc đã được sử dụng" |
| 400 | OTP hết hạn | "Mã OTP đã hết hạn" |
| 400 | Đăng ký bị tắt | "Hệ thống hiện tại đang tạm khóa chức năng đăng ký tài khoản mới." |
| 400 | Google login bị tắt | "Chức năng đăng nhập bằng Google hiện đang bị khóa." |
| 200* | Sai mật khẩu (chưa đủ lần) | "Email hoặc mật khẩu không chính xác. Bạn còn N lần thử." |
| 200* | Tài khoản bị khóa tạm thời | "Tài khoản bị khóa tạm thời. Sẽ mở khóa vào: HH:mm:ss dd/MM/yyyy" |
| 200* | Tài khoản bị khóa vĩnh viễn | "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ Admin." |
| 200* | Chưa xác thực OTP | "Vui lòng xác thực email (OTP) trước khi đăng nhập" |
| 401 | Refresh token hết hạn/bị thu hồi | "Refresh token không hợp lệ hoặc đã bị thu hồi" |
| 400 | Mật khẩu hiện tại sai | "Mật khẩu hiện tại không đúng" |

> *HTTP 200 được dùng thay 401 cho một số trường hợp đăng nhập thất bại trong code hiện tại.

---

## 8. ACCEPTANCE CRITERIA (Tiêu chí nghiệm thu)

| ID | Scenario | Given | When | Then |
|---|---|---|---|---|
| AC-AUTH-01 | Đăng ký thành công | Email chưa tồn tại, `ALLOW_REGISTER=true` | POST `/api/auth/register` với email/password hợp lệ | HTTP 201, OTP gửi đến email |
| AC-AUTH-02 | Đăng ký với email trùng | Email đã tồn tại | POST `/api/auth/register` cùng email | HTTP 400, message "Email này đã được đăng ký" |
| AC-AUTH-03 | Xác thực OTP thành công | OTP hợp lệ, chưa hết hạn | POST `/api/auth/verify-otp` | HTTP 200, `isVerified = true` |
| AC-AUTH-04 | Đăng nhập thành công (không 2FA) | Tài khoản đã xác thực, không bật 2FA | POST `/api/auth/login` đúng email/pass | HTTP 200, `accessToken` + `refreshToken` |
| AC-AUTH-05 | Đăng nhập kích hoạt 2FA | Tài khoản bật `is2faEnabled=true` | POST `/api/auth/login` đúng email/pass | HTTP 200, `requires2FA: true`, OTP gửi email |
| AC-AUTH-06 | Khóa tài khoản sau nhiều lần sai | `MAX_LOGIN_RETRIES=5`, chưa bị khóa | Sai mật khẩu 5 lần liên tiếp | Tài khoản bị khóa, response có thời điểm mở khóa |
| AC-AUTH-07 | Chưa xác thực OTP không thể login | `isVerified=false` | POST `/api/auth/login` đúng email/pass | HTTP 200, message yêu cầu xác thực OTP |
| AC-AUTH-08 | Refresh token rotation | Refresh token hợp lệ | POST `/api/auth/refresh` | Token cũ bị revoke, cặp token mới được cấp |
| AC-AUTH-09 | Đổi mật khẩu đúng MK cũ | User đã đăng nhập | PUT `/api/v1/profile/password` với MK cũ đúng | HTTP 200, mật khẩu được cập nhật |
| AC-AUTH-10 | Reset password 2 bước | OTP reset hợp lệ | Bước 1: `check-reset-otp` → Bước 2: `reset-password` | OTP không bị đốt ở bước 1; bị đốt sau bước 2 |