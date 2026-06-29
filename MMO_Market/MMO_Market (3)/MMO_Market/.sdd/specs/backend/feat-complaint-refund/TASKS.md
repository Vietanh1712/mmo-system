# TASKS — Complaint Refund Notification (`feat-complaint-refund`)

> **Feature ID:** `feat-complaint-refund`
> **Phiên bản:** 1.1 | **Cập nhật:** 2026-06-29

---

## Phase 1: Database Verification
- [x] **1.1** Xác thực migration `20260629_001_enhance_wallet_transactions_for_complaints.sql` đã định nghĩa các cột `balance_after`, `transaction_type`, `reference_id` thành công.

## Phase 2: Backend Service Layer
- [ ] **2.1** Sửa lỗi biên dịch trong `ComplaintServiceImpl.java`:
  - [ ] Thay đổi lời gọi `walletService.recordTransactionWithReference` thành `walletService.recordTransaction` cho cả trường hợp hoàn tiền (Resolved) và giải ngân (Rejected).
- [ ] **2.2** Tích hợp Notification trong `ComplaintServiceImpl.java`:
  - [ ] Import `com.mmo.shared.dal.NotificationRepository` và `com.mmo.shared.model.Notification`.
  - [ ] Autowire `NotificationRepository`.
  - [ ] Khi khiếu nại chuyển sang `Resolved`:
    - [ ] Tạo và lưu `Notification` thông báo hoàn tiền gửi đến Buyer (type="WALLET", severity="SUCCESS").
    - [ ] Tạo và lưu `Notification` thông báo đơn hàng bị hoàn tiền gửi đến Seller (type="WALLET", severity="WARNING").
  - [ ] Khi khiếu nại chuyển sang `Rejected`:
    - [ ] Tạo và lưu `Notification` thông báo giải ngân thành công gửi đến Seller (type="WALLET", severity="SUCCESS").
    - [ ] Tạo và lưu `Notification` thông báo khiếu nại bị từ chối gửi đến Buyer (type="WALLET", severity="DANGER").
- [ ] **2.3** Cập nhật logic tính toán ví trong `WalletService.java`:
  - [ ] Trong hàm `getWalletStats()`, thay đổi điều kiện tính `totalTopup` để bao gồm cả giao dịch `REFUND` thành công.

## Phase 3: Frontend Development
- [ ] **3.1** Cập nhật logic hiển thị trên giao diện Lịch sử giao dịch (`account-transactions.js`):
  - [ ] Trong hàm `renderSummary()`, cập nhật điều kiện tính `summary.topup` để bao gồm các giao dịch có type là `REFUND` và status là `SUCCESS`.

## Phase 4: Verification & Testing
- [ ] **4.1** Thực hiện biên dịch Maven để đảm bảo sửa hết lỗi compile:
  - [ ] `mvn clean compile` (sử dụng đường dẫn maven chính xác).
- [ ] **4.2** Kiểm tra thủ công luồng xử lý khiếu nại và giao dịch hoàn tiền:
  - [ ] Xác nhận giao dịch hoàn tiền được tạo thành công với reference code đúng định dạng.
  - [ ] Xác nhận thông báo (Notification) được tạo và lưu trữ đúng cho cả Buyer và Seller.
  - [ ] Xác nhận số liệu "Tổng nạp" trong trang Lịch sử giao dịch ví hiển thị khớp và đúng đắn.
