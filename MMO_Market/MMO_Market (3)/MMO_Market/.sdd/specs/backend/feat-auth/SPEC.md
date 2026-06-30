# SPEC — Authentication & Profile Management
> **Feature ID:** `feat-auth`
> **UC Coverage:** UC-01 (Authentication), UC-02 (User Profile)
> **Version:** 1.0 | **Status:** Active
> **Author:** Team | **Last Updated:** 2026-06-27

---

## 1. CONTEXT & GOAL (BỐI CẢNH & MỤC TIÊU)

### 1.1 Bối cảnh
Hệ thống MMO Market yêu cầu kiểm soát định danh người dùng chính xác để thực hiện các giao dịch liên quan đến ví điện tử và chuyển tiền an toàn. Việc đăng ký, đăng nhập và bảo mật thông tin cá nhân là nền tảng của mọi giao dịch C2C sản phẩm số.

### 1.2 Mục tiêu
- Cung cấp cơ chế đăng ký tài khoản mới bằng Email/Password cùng mã xác thực OTP gửi qua email.
- Cho phép đăng nhập linh hoạt qua tài khoản hệ thống hoặc Google OAuth2.
- Cho phép người dùng chỉnh sửa thông tin cá nhân và thay đổi mật khẩu an toàn.
- Cấp và quản lý access token, refresh token bằng cơ chế JWT.

### 1.3 Tại sao cần?
Bảo vệ tài sản người dùng khỏi việc đánh cắp tài khoản, ngăn chặn truy cập trái phép vào các endpoint nhạy cảm (như mua hàng, rút tiền) và lưu vết thông tin người dùng phục vụ KYC.

---

## 2. ACTOR (TÁC NHÂN)
| Actor | Role | Điều kiện tiền quyết |
|:---|:---|:---|
| **Guest** | Khách vãng lai | Chưa có tài khoản hoặc chưa đăng nhập |
| **User** | Người dùng hệ thống | Tài khoản đã được kích hoạt (isVerified = 1, isLocked = 0) |

---

## 3. FUNCTIONAL REQUIREMENTS (Cú pháp EARS)

### 3.1 Đăng ký & Xác thực OTP
| ID | EARS Requirement |
|:---|:---|
| FR-AUTH-01 | WHEN a Guest submits registration details, THE SYSTEM SHALL validate inputs and generate a 6-digit OTP verification code. |
| FR-AUTH-02 | WHEN a Guest verifies the registration using the correct OTP code, THE SYSTEM SHALL mark the user account as verified (`isVerified = 1`). |
| FR-AUTH-03 | IF the OTP verification code is expired or invalid, THEN THE SYSTEM SHALL return a `400 Bad Request` with an appropriate message. |

### 3.2 Đăng nhập & JWT
| ID | EARS Requirement |
|:---|:---|
| FR-AUTH-04 | WHEN a Guest logs in with correct email and password, THE SYSTEM SHALL generate JWT Access Token and Refresh Token. |
| FR-AUTH-05 | IF user logs in via Google OAuth2, THEN THE SYSTEM SHALL find or auto-create the user profile using Google account info. |
| FR-AUTH-06 | WHILE the Access Token is expired but the Refresh Token is still valid, THE SYSTEM SHALL allow the user to exchange the Refresh Token for a new Access Token. |

### 3.3 Hồ sơ cá nhân
| ID | EARS Requirement |
|:---|:---|
| FR-AUTH-07 | WHEN an authenticated User requests their profile, THE SYSTEM SHALL return User detail along with active role. |
| FR-AUTH-08 | WHEN a User updates profile fields, THE SYSTEM SHALL validate inputs and update the `Users` database table. |

---

## 4. NON-FUNCTIONAL REQUIREMENTS
| ID | Category | Requirement |
|:---|:---|:---|
| NFR-AUTH-01 | Security | Toàn bộ mật khẩu phải được mã hóa bằng thuật toán `BCrypt`. |
| NFR-AUTH-02 | Security | Các endpoint `/api/v1/profile/**` bắt buộc xác thực JWT Bearer Token. |
| NFR-AUTH-03 | Performance | Thời gian phản hồi API đăng nhập và xác thực phải < 300ms (p95). |

---

## 5. DATA MODEL (Mô hình dữ liệu)

```sql
CREATE TABLE Users (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NULL,
    full_name NVARCHAR(255) NULL,
    gender NVARCHAR(20) NULL,
    address NVARCHAR(500) NULL,
    national_id VARCHAR(20) NULL,
    date_of_birth DATE NULL,
    role NVARCHAR(MAX) NOT NULL, -- Format JSON: {"role": "Customer"}, {"role": "Seller"}, etc.
    phone VARCHAR(20) NULL,
    shop_status VARCHAR(20) DEFAULT 'Pending',
    balance_vnd BIGINT DEFAULT 0,
    permissions NVARCHAR(MAX) NULL,
    isVerified BIT DEFAULT 0,
    isLocked BIT DEFAULT 0,
    created_at DATETIME DEFAULT GETDATE(),
    updated_at DATETIME DEFAULT GETDATE(),
    isDelete BIT DEFAULT 0
);

CREATE TABLE Authentications (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    provider VARCHAR(50) NOT NULL, -- 'System' hoặc 'Google'
    third_party_token VARCHAR(255) NULL,
    refresh_token VARCHAR(512) NULL,
    refresh_token_expiry_date DATETIME NULL,
    is_revoked BIT DEFAULT 0,
    created_at DATETIME DEFAULT GETDATE(),
    isDelete BIT DEFAULT 0,
    CONSTRAINT FK_Auth_Users FOREIGN KEY (user_id) REFERENCES Users(id)
);

CREATE TABLE EmailVerifications (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    verification_code VARCHAR(6) NOT NULL,
    expiry_date DATETIME NOT NULL,
    is_used BIT DEFAULT 0,
    CONSTRAINT FK_Email_Users FOREIGN KEY (user_id) REFERENCES Users(id)
);
```

---

## 6. API SPEC (Đặc tả API)

### `POST /api/auth/register`
*   **Description**: Đăng ký tài khoản người dùng mới (yêu cầu mật khẩu có độ dài tối thiểu 6 ký tự, chứa ít nhất 1 chữ viết hoa và ít nhất 1 ký tự đặc biệt).
*   **Request Body (JSON):**
    ```json
    {
      "email": "customer@example.com",
      "password": "Password123",
      "fullName": "Nguyen Van A"
    }
    ```
*   **Response (201 Created):**
    ```json
    {
      "success": true,
      "message": "Đăng ký tài khoản thành công. Vui lòng kiểm tra mã OTP gửi đến email."
    }
    ```

### `POST /api/auth/verify-otp`
*   **Request Body (JSON):**
    ```json
    {
      "email": "customer@example.com",
      "otp": "123456"
    }
    ```
*   **Response (200 OK):**
    ```json
    {
      "success": true,
      "message": "Xác thực OTP thành công."
    }
    ```

### `POST /api/auth/login`
*   **Request Body (JSON):**
    ```json
    {
      "email": "customer@example.com",
      "password": "Password123"
    }
    ```
*   **Response (200 OK):**
    ```json
    {
      "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
      "refreshToken": "d7b9265f...",
      "tokenType": "Bearer",
      "email": "customer@example.com",
      "role": "Customer",
      "id": 1
    }
    ```

### `GET /api/v1/profile`
*   **Headers:** `Authorization: Bearer <JWT_Token>`
*   **Response (200 OK):**
    ```json
    {
      "id": 1,
      "email": "customer@example.com",
      "fullName": "Nguyen Van A",
      "phone": "0987654321",
      "address": "Ha Noi, Viet Nam",
      "balanceVnd": 150000,
      "role": "Customer",
      "isVerified": true
    }
    ```

---

## 7. ERROR HANDLING (Xử lý lỗi)
| HTTP Code | Error Code | Message | Lý do kích hoạt |
|---|---|---|---|
| 400 | BAD_REQUEST | "Email đã tồn tại" | Đăng ký tài khoản với email đã có sẵn |
| 400 | BAD_REQUEST | "OTP không đúng hoặc đã hết hạn" | Xác thực OTP sai mã hoặc quá thời gian |
| 401 | UNAUTHORIZED | "Thông tin đăng nhập không hợp lệ" | Sai tài khoản hoặc mật khẩu |

---

## 8. ACCEPTANCE CRITERIA (Tiêu chí nghiệm thu)
| ID | Scenario | Given (Bối cảnh) | When (Hành động) | Then (Kết quả) |
|---|---|---|---|---|
| AC-AUTH-01 | Đăng ký thành công | Khách truy cập ở trang Đăng ký | Điền đầy đủ thông tin email/mật khẩu và bấm nút "Đăng ký" | Hệ thống gửi OTP xác thực đến email và yêu cầu nhập mã |
| AC-AUTH-02 | Đăng nhập thành công | Người dùng ở trang Đăng nhập | Nhập chính xác email, mật khẩu và bấm "Đăng nhập" | Trả về JWT Token và chuyển hướng vào trang chủ |