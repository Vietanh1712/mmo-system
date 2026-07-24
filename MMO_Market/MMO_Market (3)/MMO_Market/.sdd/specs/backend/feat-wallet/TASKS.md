# TASKS — Wallet & Financial Management (`feat-wallet`)

> **Feature ID:** `feat-wallet` | **UC Coverage:** UC-07 (Wallet Top-up), UC-09 (Withdrawal), UC-19 (Wallet Dashboard)

---

## Phase 1: Database & Entities

- [x] **1.1** Schema tạo lập các bảng: `TopupTransactions`, `WalletTransactions`, `Withdrawals`, và `SellerBankInfos`.
- [x] **1.2** Cột số dư `balance_vnd` và số tiền nạp `deposit_vnd` kiểu số nguyên lớn (`Long`/`BIGINT`) trên bảng `Users`. Không sử dụng cột `available_balance` hay `hold_balance` tách biệt.
- [x] **1.3** Map JPA Entity `TopupTransaction` chứa thông tin giao dịch SePay (`sepay_code`, `status`, `transfer_content`, `balance_before`, `balance_after`, `failure_reason`, `staff_note`, `processed_by_staff_id`).
- [x] **1.4** Map JPA Entity `Withdrawal` chứa thông tin rút tiền và liên kết với `SellerBankInfo`.
- [x] **1.5** Map JPA Entity `WalletTransaction` chứa thông tin lịch sử giao dịch.
- [x] **1.6** Map JPA Entity `SellerBankInfo` chứa cấu hình ngân hàng người bán.

## Phase 2: Repositories

- [x] **2.1** `TopupTransactionRepository` — truy vấn kiểm tra mã giao dịch SePay `sepayCode` tránh trùng lặp; truy vấn tổng số lượt nạp tiền và tìm kiếm phân trang.
- [x] **2.2** `WithdrawalRepository` — truy vấn danh sách yêu cầu rút tiền của user; truy vấn tìm kiếm phân trang lệnh rút.
- [x] **2.3** `WalletTransactionRepository` — truy vấn lịch sử giao dịch và tìm theo reference code.
- [x] **2.4** `SellerBankInfoRepository` — truy vấn bank info hoạt động của user.
- [x] **2.5** `UserRepository` — sử dụng `@Lock(LockModeType.PESSIMISTIC_WRITE)` để khóa dòng dữ liệu User khi thay đổi số dư ví.

## Phase 3: DTOs & Validation

- [x] **3.1** `SePayWebhookRequest` — DTO ánh xạ đúng cấu trúc JSON truyền về từ SePay Webhook.
- [x] **3.2** `TopupRetryRequestDto` — DTO Staff gửi lên để duyệt nạp tiền thủ công.
- [x] **3.3** DTOs Response: `WalletStatsDto`, `WalletTransactionDto`, `TopupResponseDto`.

## Phase 4: Business Logic (Services)

- [x] **4.1** `WalletService.recordTransaction()` — tạo bản ghi biến động số dư cho ví của người dùng.
- [x] **4.2** `TopupService.processSepayWebhook()` — xử lý webhook SePay, kiểm tra trùng lặp (`idempotency`), parse cú pháp `MMO-TOPUP-<userId>`, kiểm tra hạn mức min/max, thực hiện Pessimistic Lock bọc trong Transaction cộng tiền vào `balanceVnd` và cập nhật khóa cảnh báo shop Seller.
- [x] **4.3** `TopupService.initTopupData()` — đồng bộ dữ liệu `WalletTransaction` dạng TOPUP sang `TopupTransaction` khi ứng dụng khởi động.
- [x] **4.4** `TopupService.retryTopup()` — xử lý Staff duyệt thủ công các giao dịch nạp tiền lỗi cú pháp.
- [x] **4.5** `WithdrawalService.requestWithdrawal()` — kiểm tra khóa rút tiền, kiểm tra số dư đủ (`balanceVnd >= amount + fee`), trừ tiền trực tiếp khỏi tài khoản người dùng, tạo bản ghi `Withdrawal` dạng `Pending` và gửi thông báo hệ thống đến user và Staff.
- [x] **4.6** `WithdrawalService.updateWithdrawalStatus()` — xử lý thay đổi trạng thái yêu cầu rút tiền:
  - Duyệt hoàn tất: Đổi trạng thái giao dịch ví gốc thành `COMPLETED`, cập nhật `proofFile`.
  - Từ chối/Thất bại: Cộng hoàn lại tiền gốc + phí về ví của Seller và tạo giao dịch ví `REFUND` `COMPLETED`.

## Phase 5: Controllers & Security

- [x] **5.1** `TopupController` — endpoint nhận webhook công khai `/api/sepay/webhook` (không yêu cầu đăng nhập).
- [x] **5.2** `WalletController` (REST API `/api/v1/wallet`):
  - `GET /stats` & `GET /transactions`: Xem số dư, thống kê ví và lịch sử giao dịch ví cá nhân.
  - `GET /bank-info` & `PUT /bank-info`: Xem và cập nhật cấu hình tài khoản ngân hàng.
  - `GET /withdrawals` & `POST /withdrawals`: Lịch sử rút tiền và tạo yêu cầu rút tiền mới.
  - `GET /withdrawals/config`: Lấy thông tin cấu hình rút tiền (phí, hạn mức, yêu cầu 2FA).
- [x] **5.3** `StaffTopupController` (REST API `/api/v1/staff/topups`):
  - `@PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")` bảo vệ.
  - Hỗ trợ lấy danh sách nạp tiền phân trang, thống kê nạp tiền, chi tiết giao dịch, và API duyệt thủ công (`POST /{id}/retry`).
- [x] **5.4** `StaffController` (MVC `@Controller`):
  - `GET /staff/withdrawals` & `GET /staff/withdrawals/detail` — quản lý, lọc và xem chi tiết yêu cầu rút tiền của Seller.
  - `POST /withdrawals/update-status` & `POST /withdrawals/reject` — Staff thực hiện duyệt hoàn tất (kèm proof file) hoặc từ chối yêu cầu rút tiền.

## Phase 6: Testing

- [ ] **6.1** Unit test mô phỏng nạp tiền trùng lặp gửi từ webhook để xác minh hệ thống chỉ cộng tiền một lần duy nhất (chưa có trong source code test).
- [ ] **6.2** Unit test kiểm tra luồng hoàn tiền đầy đủ khi Staff từ chối yêu cầu rút tiền (chưa có trong source code test).

## Phase 7: Final Review

- [x] **7.1** Đảm bảo không thể nạp tiền âm hoặc rút tiền âm/quá hạn mức qua kiểm tra đầu vào tại tầng Service.
- [x] **7.2** Xác minh mọi biến động ví đều qua `recordTransaction` để đảm bảo lưu vết tài chính đầy đủ.