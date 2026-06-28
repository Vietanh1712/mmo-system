# SPEC — Seller Console & Shop Management
> **Feature ID:** `feat-seller`
> **UC Coverage:** UC-04 (Seller Registration)
> **Version:** 1.0 | **Status:** Active
> **Author:** Team | **Last Updated:** 2026-06-27

---

## 1. CONTEXT & GOAL (BỐI CẢNH & MỤC TIÊU)

### 1.1 Bối cảnh
Người dùng đã hoàn thành KYC thành công có nhu cầu kinh doanh sản phẩm số trên MMO Market cần được nâng cấp tài khoản và tạo hồ sơ cửa hàng (Shop) cùng thông tin thanh toán rút tiền ngân hàng.

### 1.2 Mục tiêu
- Cung cấp cổng đăng ký mở Shop cho người dùng KYC.
- Tạo Seller Console giúp người bán quản lý sản phẩm, tồn kho, đơn hàng và rút tiền kiếm được.
- Đảm bảo an toàn tài chính qua cấu hình tài khoản ngân hàng chính chủ.

---

## 2. ACTOR (TÁC NHÂN)
| Actor | Role | Điều kiện tiền quyết |
|---|---|---|
| **User** | Đăng ký mở shop | Đã hoàn tất phê duyệt KYC (`isVerified = 1`) |
| **Seller** | Người quản lý shop | Đăng ký gian hàng thành công |

---

## 3. FUNCTIONAL REQUIREMENTS (Cú pháp EARS)
| ID | EARS Requirement |
|---|---|
| FR-SELL-01 | WHEN a verified User registers a shop with name and bank account, THE SYSTEM SHALL save the registration and update role to 'Seller'. |
| FR-SELL-02 | WHILE user is a Seller, THE SYSTEM SHALL allow access to `/api/seller/**` management endpoints. |

---

## 5. DATA MODEL (Mô hình dữ liệu)

```sql
CREATE TABLE SellerRegistrations (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    shop_name NVARCHAR(255) NOT NULL,
    description NVARCHAR(MAX) NULL,
    contract VARCHAR(255) NULL,
    signed_contract VARCHAR(255) NULL,
    status VARCHAR(20) DEFAULT 'Pending', -- Pending, Approved, Rejected
    created_at DATETIME DEFAULT GETDATE(),
    isDelete BIT DEFAULT 0,
    CONSTRAINT FK_Reg_Users FOREIGN KEY (user_id) REFERENCES Users(id)
);

CREATE TABLE SellerBankInfo (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    bank_name NVARCHAR(100) NOT NULL,
    account_number VARCHAR(50) NOT NULL,
    branch NVARCHAR(100) NULL,
    created_at DATETIME DEFAULT GETDATE(),
    isDelete BIT DEFAULT 0,
    CONSTRAINT FK_Bank_Users FOREIGN KEY (user_id) REFERENCES Users(id)
);
```

---

## 6. API SPEC (Đặc tả API)

### `POST /api/v1/shop-registrations`
*   **Request Body (JSON):**
    ```json
    {
      "shopName": "Cửa Hàng MMO Uy Tín",
      "description": "Chuyên bán tài khoản Netflix, Spotify giá rẻ",
      "bankName": "Vietcombank",
      "accountNumber": "001100123456"
    }
    ```
*   **Response (200 OK):** Thành công.