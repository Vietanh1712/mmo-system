# SPEC — Wallet & Financial Management

> **Feature ID:** `feat-wallet`
> **UC Coverage:** UC-07 (Wallet Top-up), UC-09 (Withdrawal), UC-19 (Wallet Dashboard)

---

## 1. CONTEXT & GOAL (BỐI CẢNH & MỤC TIÊU)

### 1.1 Bối cảnh
Ví tài chính là cốt lõi của MMO Market. Người mua nạp tiền vào ví để thanh toán đơn hàng. Người bán thu tiền từ đơn hàng và yêu cầu rút tiền về tài khoản ngân hàng của họ.

### 1.2 Mục tiêu
- Cung cấp cổng nạp tiền tự động qua tích hợp SePay Webhook.
- Bảo vệ giao dịch an toàn thông qua tính bất biến (idempotence) để tránh xử lý trùng một giao dịch nạp tiền nhiều lần.
- Cho phép người dùng cấu hình thông tin ngân hàng.
- Cho phép Seller rút tiền về ngân hàng, khấu trừ phí rút tiền và kiểm soát hạn mức giao dịch động từ cấu hình hệ thống.
- Thực hiện hoàn tiền đầy đủ (bao gồm cả phí) nếu yêu cầu rút tiền bị từ chối hoặc thất bại.

### 1.3 Tại sao cần?
Đảm bảo an toàn số dư ví, chống gian lận nạp tiền đúp từ webhook ngân hàng, tự động hoàn trả số dư khi giao dịch lỗi, và lưu vết biến động số dư phục vụ đối soát tài chính.

---

## 2. ACTOR (TÁC NHÂN)

| Actor | Role | Điều kiện tiền quyết |
|---|---|---|
| **User** | Người nạp tiền | Đã đăng nhập, tài khoản hợp lệ |
| **Seller** | Người rút tiền | Đã đăng nhập, ví không bị khóa rút tiền, số dư khả dụng đủ |
| **Staff** | Người duyệt rút tiền / nạp thủ công | Đã đăng nhập với tài khoản có vai trò Staff/Admin |
| **SePay Webhook** | Hệ thống cổng thanh toán | Gọi API webhook khi có giao dịch chuyển khoản |

---

## 3. FUNCTIONAL REQUIREMENTS (Cú pháp EARS)

### 3.1 Nạp tiền tự động (Top-up)

| ID | EARS Requirement |
|:---|:---|
| FR-WALL-01 | WHEN SePay sends a webhook request with a unique transaction code, THE SYSTEM SHALL verify if it has been processed in `TopupTransactions`. |
| FR-WALL-02 | IF the transaction code is already processed, THEN THE SYSTEM SHALL ignore the callback and return 200 OK (Idempotency). |
| FR-WALL-03 | WHEN a new top-up transaction is validated, THE SYSTEM SHALL credit the amount to user's `balanceVnd`, update shop warning lock status, and save success top-up and wallet transaction records. |
| FR-WALL-04 | IF the top-up message format is invalid or amount is outside limits, THEN THE SYSTEM SHALL log it as a failed top-up transaction in `TopupTransactions`. |
| FR-WALL-05 | WHEN a Staff manually triggers a retry/approval for a failed top-up, THE SYSTEM SHALL credit the target user's `balanceVnd` and update transaction status to Success. |

### 3.2 Rút tiền (Withdrawal) & Cấu hình ngân hàng

| ID | EARS Requirement |
|:---|:---|
| FR-WALL-06 | WHEN a Seller requests bank info or updates bank info, THE SYSTEM SHALL retrieve/save the bank details in `SellerBankInfos`. |
| FR-WALL-07 | WHEN a Seller requests a withdrawal of `amount`, THE SYSTEM SHALL check that `withdrawalLocked = false`, `balanceVnd >= amount + fee`, and amount is within min/max limits. |
| FR-WALL-08 | IF withdrawal request is valid, THEN THE SYSTEM SHALL subtract `amount + fee` directly from user's `balanceVnd` and create a `Withdrawal` record with status `Pending`. |
| FR-WALL-09 | WHEN a Staff rejects a withdrawal, THE SYSTEM SHALL add `amount + fee` back to user's `balanceVnd` and record a `REFUND` transaction. |
| FR-WALL-10 | WHEN a Staff completes/approves a withdrawal, THE SYSTEM SHALL update status to Completed and require upload of transaction proof file. |

---

## 4. NON-FUNCTIONAL REQUIREMENTS

| ID | Category | Requirement |
|---|---|---|
| NFR-WALL-01 | Security | Số dư ví bắt buộc sử dụng kiểu số nguyên lớn (`Long` trong Java / `BIGINT` trong DB) để tính toán chính xác, tuyệt đối không dùng số thực. |
| NFR-WALL-02 | Security | Các giao dịch thay đổi số dư ví phải được bảo vệ bằng cơ chế `Pessimistic Lock` (`findByIdForUpdate` trên User) chống race condition. |
| NFR-WALL-03 | Compliance | Webhook SePay phải thực hiện check chữ ký số/token SePay hoặc kiểm tra cú pháp an toàn để tránh ghi nhận giao dịch giả mạo. |

---

## 5. DATA MODEL (Mô hình dữ liệu)

```sql
CREATE TABLE TopupTransactions (
    id                    BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id               BIGINT NOT NULL,                  -- = 0 nếu không parse được cú pháp UserID
    amount_vnd            BIGINT NOT NULL,
    sepay_code            VARCHAR(255) NOT NULL UNIQUE,     -- ID giao dịch từ SePay
    transfer_content      VARCHAR(500) NULL,
    balance_before        BIGINT NULL,
    balance_after         BIGINT NULL,
    status                VARCHAR(20) DEFAULT 'Pending',    -- Success | Failed | Pending
    failure_reason        VARCHAR(500) NULL,
    staff_note            VARCHAR(500) NULL,
    processed_by_staff_id BIGINT NULL,
    created_at            DATETIME DEFAULT GETDATE(),
    isDelete              BIT DEFAULT 0
);

CREATE TABLE WalletTransactions (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    amount_vnd      BIGINT NOT NULL,                        -- Dương nếu nạp/hoàn tiền, âm nếu rút/mua hàng
    balance_after   BIGINT NOT NULL,
    type            VARCHAR(50) NOT NULL,                   -- TOPUP | WITHDRAWAL | REFUND | PAYMENT...
    status          VARCHAR(20) NOT NULL,                   -- SUCCESS | PENDING | COMPLETED | FAILED
    description     VARCHAR(255) NULL,
    reference_code  VARCHAR(100) NULL,
    transaction_type VARCHAR(50) NOT NULL,                  -- Trùng với type
    reference_id    BIGINT NULL,                            -- ID liên kết (Topup ID hoặc Withdrawal ID)
    created_at      DATETIME DEFAULT GETDATE(),
    isDelete        BIT DEFAULT 0,
    CONSTRAINT FK_WT_User FOREIGN KEY(user_id) REFERENCES Users(id)
);

CREATE TABLE SellerBankInfos (
    id             BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id        BIGINT NOT NULL,
    bank_name      NVARCHAR(100) NOT NULL,
    account_number VARCHAR(50) NOT NULL,
    branch         NVARCHAR(255) NULL,
    created_at     DATETIME DEFAULT GETDATE(),
    isDelete       BIT DEFAULT 0,
    CONSTRAINT FK_Bank_Users FOREIGN KEY(user_id) REFERENCES Users(id)
);

CREATE TABLE Withdrawals (
    id               BIGINT IDENTITY(1,1) PRIMARY KEY,
    seller_id        BIGINT NOT NULL,
    bank_info_id     BIGINT NOT NULL,
    amount_vnd       BIGINT NOT NULL,
    fee_vnd          BIGINT DEFAULT 0,
    status           VARCHAR(20) DEFAULT 'Pending',         -- Pending | Processing | Completed | Rejected | Failed
    proof_file       VARCHAR(255) NULL,                     -- URL ảnh chụp biên lai chuyển tiền
    rejection_reason NVARCHAR(MAX) NULL,
    reviewed_by      BIGINT NULL,
    reviewed_at      DATETIME NULL,
    created_at       DATETIME DEFAULT GETDATE(),
    isDelete         BIT DEFAULT 0,
    CONSTRAINT FK_With_Seller FOREIGN KEY(seller_id) REFERENCES Users(id),
    CONSTRAINT FK_With_Bank   FOREIGN KEY(bank_info_id) REFERENCES SellerBankInfos(id),
    CONSTRAINT FK_With_Staff  FOREIGN KEY(reviewed_by) REFERENCES Users(id)
);
```

---

## 6. API SPEC (Đặc tả API)

### `POST /api/sepay/webhook`
- **Description**: Nhận thông báo chuyển tiền từ SePay (nạp tiền).
- **Request Body (JSON):**
  ```json
  { "id": 857201, "transferType": "in", "transferAmount": 200000, "content": "MMO TOPUP 12" }
  ```
- **Response 200 OK:** `"success"` (hoặc boolean true/false)

---

### `GET /api/v1/wallet/stats`
- **Headers:** `Authorization: Bearer <JWT_Token>`
- **Response 200:** `{ "balanceVnd": 150000, "depositVnd": 500000 }`

---

### `GET /api/v1/wallet/transactions`
- **Headers:** `Authorization: Bearer <JWT_Token>`
- **Response 200:** `Page<WalletTransactionDto>`

---

### `GET /api/v1/wallet/bank-info`
- **Headers:** `Authorization: Bearer <JWT_Token>`
- **Response 200:** `{ "bankName": "Vietcombank", "accountNumber": "001100123456", "accountHolder": "NGUYEN VAN SELLER", "branch": "Ha Noi" }`

---

### `PUT /api/v1/wallet/bank-info`
- **Headers:** `Authorization: Bearer <JWT_Token>`
- **Request Body:** `{ "bankName": "Vietcombank", "accountNumber": "001100123456", "branch": "Ha Noi" }`
- **Response 200:** `{ "message": "Cập nhật thông tin ngân hàng thành công!" }`

---

### `GET /api/v1/wallet/withdrawals`
- **Headers:** `Authorization: Bearer <JWT_Token>`
- **Response 200:**
  ```json
  [
    { "id": 1, "amountVnd": 500000, "feeVnd": 7500, "bankName": "VivaldiBank", "accountNumber": "123...", "status": "Pending", "proofFile": "", "createdAt": "..." }
  ]
  ```

---

### `POST /api/v1/wallet/withdrawals`
- **Headers:** `Authorization: Bearer <JWT_Token>`
- **Request Body:** `{ "amountVnd": 500000, "otp": "123456" }`
- **Response 200:** `{ "newBalance": 492500, "message": "Yêu cầu rút tiền đã được gửi thành công!" }`

---

### `GET /api/v1/wallet/withdrawals/config`
- **Headers:** `Authorization: Bearer <JWT_Token>`
- **Response 200:** `{ "withdrawalFeePercent": 1.5, "minWithdrawalLimit": 50000, "maxWithdrawalLimit": 50000000, "requireWithdraw2FA": false }`

---

### `GET /api/v1/staff/topups`
- **Auth:** `ROLE_STAFF` hoặc `ROLE_ADMIN`
- **Response 200:** `Page<TopupResponseDto>`

---

### `POST /api/v1/staff/topups/{id}/retry`
- **Auth:** `ROLE_STAFF` hoặc `ROLE_ADMIN`
- **Request Body:** `{ "targetUserId": 12, "skipMinCheck": true, "staffNote": "Duyệt thủ công lỗi cú pháp" }`
- **Response 200:** `TopupResponseDto` thành công.

---

## 7. ERROR HANDLING

| HTTP Code | Tình huống | Message |
|---|---|---|
| 400 | Số dư không đủ rút tiền | "Số dư ví không đủ để thực hiện yêu cầu rút tiền này..." |
| 400 | Ví bị khóa rút tiền | "Ví của bạn đang bị khóa do vi phạm. Không thể thực hiện rút tiền lúc này." |
| 400 | Số tiền rút ngoài hạn mức | "Số tiền rút tối thiểu phải là ... VNĐ." / "Số tiền rút tối đa phải là ... VNĐ." |
| 400 | Chưa cấu hình tài khoản ngân hàng | "Vui lòng cấu hình thông tin ngân hàng trước khi rút tiền." |
| 400 | Duyệt lại giao dịch đã thành công trước đó | "Giao dịch nạp tiền này đã thành công trước đó, không thể kích hoạt lại." |
| 401 | Gọi API không đính kèm token | "Unauthorized" |

---

## 8. ACCEPTANCE CRITERIA

| ID | Scenario | Given | When | Then |
|---|---|---|---|---|
| AC-WALL-01 | Nạp tiền tự động thành công | User cú pháp `MMO TOPUP 12` | SePay webhook gọi API | `balanceVnd` của user 12 được cộng tiền, ghi nhận `SUCCESS` giao dịch |
| AC-WALL-02 | Chặn nạp đúp giao dịch | Webhook SePay gửi lại mã giao dịch cũ | Webhook SePay gọi API | Hệ thống nhận dạng sepayCode đã tồn tại, bỏ qua xử lý và trả về 200 OK |
| AC-WALL-03 | Yêu cầu rút tiền (trừ ví trực tiếp) | Seller có ví 1,000,000đ, phí 1.5% | Tạo lệnh rút 500,000đ | Ví bị trừ 507,500đ, lệnh rút tạo ở trạng thái `Pending` |
| AC-WALL-04 | Staff từ chối rút tiền (Hoàn tiền) | Lệnh rút 500,000đ, phí 7,500đ đang `Pending` | Staff từ chối lệnh rút | Ví Seller được cộng lại 507,500đ, ghi giao dịch `REFUND` `COMPLETED` |
| AC-WALL-05 | Staff duyệt rút tiền | Lệnh rút ở trạng thái `Pending` | Staff phê duyệt và đính kèm ảnh proof | Lệnh chuyển thành `Completed` (hoặc `Approved`), ghi giao dịch gốc là `COMPLETED` |