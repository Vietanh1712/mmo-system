# PLAN — Wallet & Financial Management (`feat-wallet`)

## 1. Mục tiêu (Goals)

Triển khai và quản lý luồng tài chính cốt lõi của MMO Market theo đặc tả `SPEC.md` (feat-wallet) bao gồm:
- **Nạp tiền (Top-up):** Tích hợp cổng chuyển tiền tự động thông qua SePay Webhook. Đảm bảo tính bất biến (Idempotence) để tránh xử lý trùng lặp giao dịch chuyển tiền.
- **Rút tiền (Withdrawal):** Người bán gửi yêu cầu rút tiền về tài khoản ngân hàng. Tiền rút được trừ từ số dư khả dụng (`available_balance`) và đóng băng ở số dư tạm giữ (`hold_balance`) trong khi chờ Staff/Admin phê duyệt.
- **Tuân thủ:** Đảm bảo chính xác về số tiền (chỉ dùng số nguyên lớn `Long` / `BIGINT`), tránh tranh chấp đồng thời thông qua Pessimistic Lock.

## 2. Kiến trúc & Công nghệ

- **Backend:** Java 17, Spring Boot 3.1, Spring Data JPA, SQL Server (T-SQL).
- **Frontend:** Thymeleaf template engine, JS thuần gọi REST API (`authFetch` truyền Authorization JWT header).
- **Tuân thủ:** Mô hình phân lớp Controller → Service → Repository → Entity; DTO Pattern cho Request/Response; Kiểm tra số dư bằng cơ chế khoá bi quan (Pessimistic Locking `@Lock(LockModeType.PESSIMISTIC_WRITE)`) để ngăn chặn tình trạng race-condition (như trừ tiền âm ví, nạp đúp).

## 3. Các thành phần Backend

### 3.1. Database Migration & Entities

- **Entity `TopupTransaction`** (bảng `TopupTransactions`):
  - Lưu mã giao dịch ngân hàng độc nhất `transactionCode`.
  - Quản lý trạng thái nạp tiền (`PENDING`, `SUCCESS`, `FAILED`).
- **Entity `Withdrawal`** (bảng `Withdrawals`):
  - Lưu thông tin yêu cầu rút tiền: `amount`, `bankName`, `accountNumber`, `accountName`, `status` (`PENDING`, `APPROVED`, `REJECTED`), lý do từ chối `rejectionReason`, thông tin staff duyệt.
- **Entity `WalletTransaction`** (bảng `WalletTransactions`):
  - Nhật ký ghi chép mọi biến động số dư: `amount`, `balanceBefore`, `balanceAfter`, `type` (`TOPUP`, `WITHDRAW`, `PURCHASE_ESCROW`, `REVERT`).
- **Bảng `Users`**:
  - Tách biệt hai trạng thái số dư: `availableBalance` (Số dư khả dụng) và `holdBalance` (Số dư tạm giữ/đóng băng). Định dạng `Long` (VNĐ).

### 3.2. Repositories (Spring Data JPA)

- `TopupTransactionRepository`:
  - `existsByTransactionCode(transactionCode)`: Đảm bảo tính bất biến (idempotent), kiểm tra giao dịch nạp tiền đã được xử lý trước đó chưa.
- `WithdrawalRepository`:
  - Tìm kiếm và lấy danh sách yêu cầu rút tiền phục vụ quản trị phân trang.
- `UserRepository` (Tái dùng với Locking):
  - `@Lock(LockModeType.PESSIMISTIC_WRITE)` phương thức `findByIdForUpdate(id)`: Khóa dòng dữ liệu User khi tiến hành cộng/trừ/đóng băng tiền.

### 3.3. DTOs

- Request: `WithdrawalRequest` (gồm số tiền rút `amount`, thông tin ngân hàng).
- Response: `WalletStatsResponse`, `TransactionResponse`, `WithdrawalResponse`. Mapping tại Service layer.

### 3.4. Services (Business Logic)

- **`TopupService`**:
  - Xử lý tin nhắn chuyển khoản từ SePay webhook.
  - Phân tích cú pháp tin nhắn để xác định User ID nạp tiền.
  - Thực hiện Pessimistic Lock lấy User, cộng tiền vào `availableBalance`, lưu `TopupTransaction` và `WalletTransaction`.
- **`WithdrawalService`**:
  - `requestWithdrawal(userId, amount, bankInfo)`: Khóa User, kiểm tra `availableBalance >= amount`. Nếu hợp lệ, giảm `availableBalance`, tăng `holdBalance`, tạo bản ghi `Withdrawal` ở dạng `PENDING`.
  - `approveWithdrawal(withdrawalId, staffId)`: Trừ số tiền tương ứng khỏi `holdBalance`. Chuyển trạng thái sang `APPROVED`.
  - `rejectWithdrawal(withdrawalId, staffId, reason)`: Giảm `holdBalance`, trả lại tiền về `availableBalance`, cập nhật lý do từ chối và đổi trạng thái sang `REJECTED`.
- **`WalletService`**:
  - Cung cấp lịch sử biến động số dư và thống kê ví.

### 3.5. Controllers & Security

- **`TopupController`**:
  - `POST /api/sepay/webhook`: Nhận webhook từ SePay (không yêu cầu đăng nhập, đảm bảo bảo mật bằng chữ ký số/token SePay).
- **`WalletController`**:
  - `GET /api/v1/wallet/stats` & `GET /api/v1/wallet/transactions`: Xem thông tin ví và lịch sử giao dịch cá nhân.
  - `POST /api/v1/wallet/withdraw`: Tạo yêu cầu rút tiền (`@PreAuthorize("hasRole('SELLER')")`).

---

## 4. Các thành phần Frontend

- **Màn hình Nạp tiền của Customer:**
  - File: `templates/wallet/topup.html` và JS `static/js/customer/account-topup.js`.
  - Hiển thị hướng dẫn cú pháp chuyển khoản và mã QR tự động chứa thông tin User ID.
- **Màn hình Ví & Lịch sử giao dịch:**
  - File: `templates/account/wallet.html` và JS `static/js/customer/account-wallet.js`.
- **Màn hình Yêu cầu rút tiền của Seller:**
  - File: `templates/seller/withdraw.html`.
- **Màn hình duyệt rút tiền của Staff:**
  - File: `templates/staff/withdrawals.html` và JS `static/js/staff/staff-withdrawals.js`.

---

## 5. Definition of Done

- Toàn bộ giao dịch ví bắt buộc dùng số nguyên lớn `Long` cho VNĐ.
- Tuyệt đối mọi thao tác thay đổi ví phải dùng `Pessimistic Lock` để tránh lỗi race condition.
- API SePay webhook phải trả về `200 OK` ngay lập tức nếu mã giao dịch chuyển tiền đã được xử lý trước đó (Idempotency).