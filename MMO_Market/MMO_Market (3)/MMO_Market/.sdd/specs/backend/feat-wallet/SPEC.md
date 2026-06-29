# SPEC — Wallet & Financial Management
> **Feature ID:** `feat-wallet`
> **UC Coverage:** UC-07 (Wallet Top-up), UC-09 (Withdrawal)
> **Version:** 1.0 | **Status:** Active
> **Author:** Team | **Last Updated:** 2026-06-27

---

## 1. CONTEXT & GOAL (BỐI CẢNH & MỤC TIÊU)

### 1.1 Bối cảnh
Ví tài chính là cốt lõi của MMO Market. Người mua nạp tiền vào ví để thanh toán đơn hàng. Người bán thu tiền từ đơn hàng và yêu cầu rút tiền về tài khoản ngân hàng của họ.

### 1.2 Mục tiêu
- Cung cấp cổng nạp tiền tự động qua tích hợp SePay Webhook.
- Bảo vệ giao dịch an toàn thông qua tính bất biến (idempotence) để tránh xử lý trùng một giao dịch nạp tiền nhiều lần.
- Đóng băng tiền rút tạm thời (`hold_balance`) và trừ khỏi số dư khả dụng (`available_balance`) trong khi lệnh rút chờ duyệt.

### 1.3 Tại sao cần?
Đảm bảo an toàn số dư ví, chống gian lận nạp tiền đúp từ webhook ngân hàng và đảm bảo hệ thống không bị thất thoát tài chính.

---

## 2. ACTOR (TÁC NHÂN)
| Actor | Role | Điều kiện tiền quyết |
|---|---|---|
| **User** | Người nạp tiền | Đã xác thực OTP tài khoản |
| **Seller** | Người rút tiền | Tài khoản Seller có số dư khả dụng và tài khoản ngân hàng |

---

## 3. FUNCTIONAL REQUIREMENTS (Cú pháp EARS)

### 3.1 Nạp tiền tự động (Top-up)
| ID | EARS Requirement |
|:---|:---|
| FR-WALL-01 | WHEN SePay sends a webhook request with a unique transaction code, THE SYSTEM SHALL verify if it has been processed. |
| FR-WALL-02 | IF the transaction code is already processed, THEN THE SYSTEM SHALL ignore the callback and return 200 OK (Idempotency). |
| FR-WALL-03 | WHEN a new transaction is validated, THE SYSTEM SHALL credit the amount to user's `available_balance` and log the transaction. |

### 3.2 Rút tiền (Withdrawal)
| ID | EARS Requirement |
|:---|:---|
| FR-WALL-04 | WHEN a Seller requests a withdrawal of `amount`, THE SYSTEM SHALL verify that `available_balance >= amount`. |
| FR-WALL-05 | IF withdrawal request is valid, THEN THE SYSTEM SHALL subtract `amount` from `available_balance` and add to `hold_balance` (No OTP required). |
| FR-WALL-06 | WHEN a Staff rejects a withdrawal, THE SYSTEM SHALL subtract the amount from `hold_balance` and return to `available_balance`. |

---

## 4. NON-FUNCTIONAL REQUIREMENTS
| ID | Category | Requirement |
|---|---|---|
| NFR-WALL-01 | Security | Số dư ví bắt buộc sử dụng kiểu số nguyên lớn (`Long` trong Java / `BIGINT` trong DB) để tính toán chính xác, tuyệt đối không dùng số thực. |
| NFR-WALL-02 | Security | Các giao dịch thay đổi số dư ví phải được bảo vệ bằng cơ chế `Pessimistic Lock` chống race condition. |

---

## 5. DATA MODEL (Mô hình dữ liệu)

```sql
CREATE TABLE TopupTransactions (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    amount BIGINT NOT NULL,
    transaction_date DATETIME NOT NULL,
    transaction_code VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(50) DEFAULT 'Pending',
    isDelete BIT DEFAULT 0,
    CONSTRAINT FK_Topup_User FOREIGN KEY(user_id) REFERENCES Users(id)
);

CREATE TABLE WalletTransactions (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    amount BIGINT NOT NULL,
    balance_before BIGINT NOT NULL,
    balance_after BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL, -- TOPUP, WITHDRAW, PURCHASE_ESCROW, REVERT
    order_id VARCHAR(100) NULL,
    created_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_WT_User FOREIGN KEY(user_id) REFERENCES Users(id)
);

CREATE TABLE Withdrawals (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    amount BIGINT NOT NULL,
    bank_name NVARCHAR(100) NOT NULL,
    account_number VARCHAR(50) NOT NULL,
    account_name NVARCHAR(255) NOT NULL,
    status VARCHAR(20) DEFAULT 'Pending', -- Pending, Approved, Rejected
    rejection_reason NVARCHAR(MAX) NULL,
    reviewed_by BIGINT NULL,
    reviewed_at DATETIME NULL,
    created_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_With_User FOREIGN KEY(user_id) REFERENCES Users(id)
);
```

---

## 6. API SPEC (Đặc tả API)

### `POST /api/sepay/webhook`
*   **Description**: Nhận tin nhắn thông báo chuyển tiền từ SePay.
*   **Request Body:**
    ```json
    {
      "id": 857201,
      "gateway": "Vietcombank",
      "transactionDate": "2026-06-27 08:00:00",
      "accountNumber": "001100123456",
      "transferAmount": 200000,
      "content": "NAPTIEN 12"
    }
    ```
*   **Response (200 OK):** `"success"`

### `POST /api/seller/withdrawals`
*   **Request Body:**
    ```json
    {
      "amountVnd": 500000
    }
    ```
*   **Response (200 OK):**
    ```json
    {
      "withdrawalId": 45,
      "status": "Pending",
      "message": "Lệnh rút tiền đã được tạo thành công và đang chờ nhân viên duyệt."
    }
    ```

---

## 7. ERROR HANDLING
| HTTP Code | Error Code | Message | Lý do kích hoạt |
|---|---|---|---|
| 422 | INSUFFICIENT_BALANCE | "Số dư khả dụng không đủ" | Số tiền rút lớn hơn số dư ví khả dụng |

---

## 8. ACCEPTANCE CRITERIA
| ID | Scenario | Given (Bối cảnh) | When (Hành động) | Then (Kết quả) |
|---|---|---|---|---|
| AC-WALL-01 | Nạp tiền tự động thành công | User gửi tiền với cú pháp "NAPTIEN 12" | Webhook SePay gọi API với mã giao dịch mới | Ví khả dụng của User ID 12 được cộng tiền và ghi transaction |
| AC-WALL-02 | Rút tiền bị đóng băng | Seller có sẵn 1,000,000đ | Tạo lệnh rút 300,000đ | khả dụng giảm còn 700,000đ, hold_balance tăng 300,000đ |