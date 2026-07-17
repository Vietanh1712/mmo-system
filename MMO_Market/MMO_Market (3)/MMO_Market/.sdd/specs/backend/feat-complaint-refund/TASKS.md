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
- [x] **2.4** Triển khai cơ chế quản lý ví âm và Khóa/Ẩn shop theo phân cấp Shop Level:
  - [x] Thêm hàm `updateShopLockStatus` trong `ShopLevelService.java` để thực hiện khóa shop (Level 0/1) hoặc khóa rút tiền (Level 2) khi ví âm.
  - [x] Gọi cập nhật trạng thái khóa/mở khóa shop trong `ComplaintServiceImpl.java` sau khi khấu trừ hoặc giải ngân tiền.
  - [x] Gọi cập nhật trạng thái khóa/mở khóa shop trong `TopupService.java` sau khi nạp tiền thành công.
  - [x] Gọi cập nhật trạng thái khóa/mở khóa shop trong `WithdrawalService.java` sau khi hoàn trả tiền rút bị từ chối.
  - [x] Chặn rút tiền khi ví Seller có số dư âm trong `WithdrawalService.java`.
  - [x] Chặn đăng sản phẩm mới, thêm biến thể hoặc cập nhật biến thể đối với Shop Level 0/1 khi ví âm trong `SellerController.java`.
  - [x] Ẩn sản phẩm của shop bị Locked, Banned hoặc Pending trong `ProductSpecification.java`, `ProductRepository.java` (hàm native query), và `ProductService.java` (hàm fallback).

## Phase 3: Frontend Development
- [x] **3.1** Cập nhật logic hiển thị trên giao diện Lịch sử giao dịch (`account-transactions.js`):
  - [x] Trong hàm `renderSummary()`, cập nhật điều kiện tính `summary.topup` để bao gồm các giao dịch có type là `REFUND` và status là `SUCCESS`.

## Phase 4: Verification & Testing
- [x] **4.1** Thực hiện biên dịch Maven để đảm bảo sửa hết lỗi compile:
  - [x] Chạy biên dịch sạch sẽ (`mvn clean compile` thành công).
- [x] **4.2** Kiểm tra thủ công luồng xử lý khiếu nại và giao dịch hoàn tiền:
  - [x] Xác nhận giao dịch hoàn tiền được tạo thành công với reference code đúng định dạng.
  - [x] Xác nhận thông báo (Notification) được tạo và lưu trữ đúng cho cả Buyer và Seller.
  - [x] Xác nhận số liệu "Tổng nạp" trong trang Lịch sử giao dịch ví hiển thị khớp và đúng đắn.
- [x] **4.3** Kiểm tra cơ chế tự động Khóa/Mở khóa và Ẩn sản phẩm khi ví âm:
  - [x] Xác nhận shop tự động chuyển sang trạng thái Locked khi ví âm (đối với shop level 0/1).
  - [x] Xác nhận các sản phẩm của shop Locked không còn hiển thị trên chợ/catalog/featured.
  - [x] Xác nhận hệ thống chặn tính năng đăng bán mới/thêm hoặc sửa biến thể khi ví âm.
  - [x] Xác nhận tính năng tự động mở khóa hoạt động bình thường khi nạp tiền hết nợ.
