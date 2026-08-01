# SPEC-02 — Wallet & Payment
> **Module:** Wallet
> **Version:** 1.0 | **Status:** Active

---

## 1. Context and Goal
MMO Market yêu cầu một hệ thống ví điện tử (Wallet) để quản lý số dư của người dùng. Mục tiêu là cho phép người dùng nạp tiền (Top-up qua SePay), rút tiền (Withdrawal) và quản lý số dư bị đóng băng (Escrow) trong quá trình giao dịch nhằm bảo vệ cả người mua và người bán.

---

## 2. Actors
- **Customer / Seller**: Nạp tiền, rút tiền, xem lịch sử giao dịch.
- **System**: Tự động xác nhận nạp tiền qua webhook của SePay, chạy tác vụ quét định kỳ mỗi 60 giây để tự động giải phóng tiền Escrow sau thời gian chờ.
- **Staff / Admin**: Duyệt yêu cầu rút tiền thủ công.

---

## 3. Functional Requirements
- **FR-WALL-01**: WHEN a User tops up via SePay webhook, THE SYSTEM SHALL automatically add funds to their available balance and record a Transaction.
- **FR-WALL-02**: WHEN a Customer buys a product, THE SYSTEM SHALL deduct the `priceVnd` from their available balance and add the `netRevenue` to the Seller's `escrowBalance`.
- **FR-WALL-03**: WHEN the escrow period expires (default 3 days / 72 hours, or 7 days / 168 hours based on shop level and dispute rate), THE SYSTEM SHALL automatically scan and transfer funds from the Seller's escrow (Held status) to their available balance and mark the transaction as Completed.
- **FR-WALL-04**: WHEN a Seller submits a withdrawal request, THE SYSTEM SHALL verify OTP and sufficient balance, deduct the balance, and create a `PENDING` withdrawal record.
- **FR-WALL-05**: WHEN Staff approves a withdrawal, THE SYSTEM SHALL update status to `COMPLETED` and record the bank transfer detail.

---

## 4. Non-Functional Requirements
- **Reliability**: Hệ thống ví phải đảm bảo ACID transactions (sử dụng `@Transactional`). Không được phép có race condition khi nhiều giao dịch xảy ra cùng lúc (sử dụng Optimistic Locking).
- **Auditability**: Mọi thay đổi số dư phải sinh ra một bản ghi trong bảng `Transactions` để đối soát.

---

## 5. Data Model
```sql
CREATE TABLE Wallets (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT UNIQUE NOT NULL,
    available_balance BIGINT DEFAULT 0,
    escrow_balance BIGINT DEFAULT 0,
    version INT DEFAULT 0 -- For optimistic locking
);

CREATE TABLE Transactions (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    wallet_id BIGINT NOT NULL,
    amount BIGINT NOT NULL,
    transaction_type VARCHAR(50), -- DEPOSIT, WITHDRAWAL, ESCROW_HOLD, ESCROW_RELEASE
    status VARCHAR(20),
    created_at DATETIME DEFAULT GETDATE()
);
```

---

## 6. API Specification
- `POST /api/webhook/sepay` (Webhook body) -> 200 OK
- `GET /api/wallet/me` -> 200 OK (Wallet details)
- `POST /api/wallet/withdraw` (Body: amount, otp) -> 200 OK
- `GET /api/wallet/transactions` -> 200 OK (List of transactions)

---

## 7. Error Handling
- `400 Bad Request`: "Số dư không đủ để thực hiện giao dịch."
- `400 Bad Request`: "Mã OTP không hợp lệ."
- `500 Internal Server Error`: Rollback giao dịch nếu có lỗi.

---

## 8. Acceptance Criteria & Out of Scope
### Acceptance Criteria
- **AC-01**: Given a user with 50,000 VND, when they try to withdraw 100,000 VND, then the system prevents it.
- **AC-02**: Given a valid SePay webhook payload, when the webhook is called, the user's balance increases by the exact amount.

### Out of Scope
- Tích hợp cổng thanh toán khác ngoài SePay (VnPay, MoMo) chưa được hỗ trợ trong v1.
