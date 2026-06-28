# TASKS — Wallet & Financial Management (`feat-wallet`)

> **Feature ID:** `feat-wallet` | **UC Coverage:** UC-07 (Wallet Top-up), UC-09 (Withdrawal)
> **Phiên bản:** 1.0 | **Cập nhật:** 2026-06-27

---

## Phase 1: Database & Entities

- [x] **1.1** Schema tạo lập các bảng `TopupTransactions`, `WalletTransactions`, và `Withdrawals`.
- [x] **1.2** Cột `available_balance` và `hold_balance` được cấu hình độc lập trên bảng `Users`.

## Phase 2: Repositories

- [x] **2.1** `TopupTransactionRepository` — truy vấn kiểm tra mã giao dịch SePay tránh trùng lặp.
- [x] **2.2** `WithdrawalRepository` — lưu trữ và truy vấn danh sách yêu cầu rút tiền.

## Phase 3: DTOs & Validation

- [x] **3.1** `SePayWebhookRequest` — cấu hình ánh xạ đúng định dạng dữ liệu truyền về từ SePay.
- [x] **3.2** `WithdrawRequest` — validate số tiền rút tối thiểu là 50,000đ.

## Phase 4: Business Logic (Services)

- [x] **4.1** `WalletService.transferToEscrow()` — thực hiện trừ số dư khả dụng và tạo bản ghi biến động số dư. Sử dụng `Pessimistic Write Lock` để chống race condition.
- [x] **4.2** `TopupService` — xử lý nạp tiền tự động từ SePay webhook, kiểm tra trùng lặp dựa trên mã giao dịch duy nhất để bảo đảm idempotency.

## Phase 5: Controllers & Security

- [x] **5.1** `TopupController` — endpoint nhận webhook công khai `/api/sepay/webhook`.
- [x] **5.2** `WalletController` — endpoint lấy lịch sử ví yêu cầu xác thực JWT.

## Phase 6: Testing

- [x] **6.1** Kiểm thử mô phỏng nạp tiền trùng lặp gửi từ webhook để xác minh hệ thống chỉ cộng tiền một lần duy nhất.