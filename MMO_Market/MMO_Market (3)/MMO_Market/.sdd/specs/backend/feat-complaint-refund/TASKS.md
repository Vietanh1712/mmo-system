# TASKS — Complaint Refund Notification (`feat-complaint-refund`)

> **Feature ID:** `feat-complaint-refund`
> **Phiên bản:** 1.2 | **Cập nhật:** 2026-07-16

---

## Phase 1: Database Verification
- [x] **1.1** Xác thực migration `20260629_001_enhance_wallet_transactions_for_complaints.sql` đã định nghĩa các cột `balance_after`, `transaction_type`, `reference_id` thành công trong CSDL.

## Phase 2: Backend Service Layer
- [x] **2.1** Khắc phục lời gọi hàm ghi giao dịch ví `walletService.recordTransaction` trong `ComplaintServiceImpl.java` cho cả hai trường hợp hoàn tiền (`Resolved`) và giải ngân (`Rejected`).
- [x] **2.2** Tích hợp Notification trong `ComplaintServiceImpl.java`:
  - [x] Autowire `NotificationRepository`.
  - [x] Khi khiếu nại chuyển sang `Resolved`:
    - [x] Tạo và lưu `Notification` thông báo hoàn tiền gửi đến Buyer (type="WALLET", severity="SUCCESS").
    - [x] Tạo và lưu `Notification` thông báo đơn hàng bị hoàn tiền gửi đến Seller (type="WALLET", severity="WARNING").
  - [x] Khi khiếu nại chuyển sang `Rejected`:
    - [x] Tạo và lưu `Notification` thông báo giải ngân thành công gửi đến Seller (type="WALLET", severity="SUCCESS").
    - [x] Tạo và lưu `Notification` thông báo khiếu nại bị từ chối gửi đến Buyer (type="WALLET", severity="DANGER").
- [x] **2.3** Cập nhật logic tính toán ví trong `WalletService.java`:
  - [x] Trong hàm `getWalletStats()`, thay đổi điều kiện tính `totalTopup` để bao gồm cả giao dịch `REFUND` thành công.

## Phase 3: Frontend Development
- [x] **3.1** Cập nhật logic hiển thị trên giao diện Lịch sử giao dịch (`account-transactions.js`):
  - [x] Trong hàm `renderSummary()`, cập nhật điều kiện tính `summary.topup` để bao gồm các giao dịch có type là `REFUND` và status là `SUCCESS`.

## Phase 4: Verification & Testing
- [x] **4.1** Thực hiện biên dịch Maven để đảm bảo sửa hết lỗi compile:
  - [x] `mvn clean compile` thành công.
- [x] **4.2** Kiểm tra thủ công luồng xử lý khiếu nại và giao dịch hoàn tiền:
  - [x] Xác nhận giao dịch hoàn tiền được tạo thành công với reference code đúng định dạng.
  - [x] Xác nhận thông báo (Notification) được tạo và lưu trữ đúng cho cả Buyer và Seller.
  - [x] Xác nhận số liệu "Tổng nạp" trong trang Lịch sử giao dịch ví hiển thị khớp và đúng đắn.
