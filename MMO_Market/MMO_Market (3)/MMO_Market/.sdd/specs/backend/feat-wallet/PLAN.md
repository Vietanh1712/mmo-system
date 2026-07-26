# PLAN — Wallet & Financial Management (`feat-wallet`)

## 1. Mục tiêu (Goals)

Triển khai và quản lý luồng tài chính cốt lõi của MMO Market theo đặc tả `SPEC.md` (feat-wallet) bao gồm:
- **Nạp tiền (Top-up):** Tích hợp cổng chuyển tiền tự động thông qua SePay Webhook. Đảm bảo tính bất biến (Idempotence) để tránh xử lý trùng lặp giao dịch chuyển tiền. Hỗ trợ Staff phê duyệt nạp tiền thủ công nếu giao dịch lỗi cú pháp.
- **Rút tiền (Withdrawal):** Cho phép người bán gửi yêu cầu rút tiền về tài khoản ngân hàng đã cấu hình. Tiền rút và phí rút được trừ trực tiếp khỏi số dư khả dụng (`balanceVnd`). Nếu bị từ chối/thất bại, tiền được hoàn trả đầy đủ về ví (`REFUND` transaction).
- **Cấu hình ngân hàng:** Quản lý thông tin tài khoản ngân hàng của Seller (`SellerBankInfo`).
- **Tuân thủ:** Đảm bảo chính xác về số tiền (chỉ dùng số nguyên lớn `Long` / `BIGINT` cho VNĐ), tránh tranh chấp đồng thời bằng Pessimistic Lock, và ghi nhật ký giao dịch tài chính (`WalletTransaction`) và Audit Logs đầy đủ.

## 2. Kiến trúc & Công nghệ

- **Backend:** Java 17, Spring Boot 3.1, Spring Data JPA, SQL Server (T-SQL).
- **Frontend:** Thymeleaf template engine, JS thuần gọi REST API (`authFetch` truyền Authorization JWT header).
- **Tuân thủ:** Mô hình phân lớp Controller → Service → Repository → Entity; DTO Pattern cho Request/Response; Kiểm tra số dư bằng cơ chế khoá bi quan (Pessimistic Locking `@Lock(LockModeType.PESSIMISTIC_WRITE)`) để ngăn chặn race-condition (như trừ tiền âm ví, nạp đúp).

## 3. Các thành phần Backend

### 3.1. Database Migration & Entities

- **Entity `TopupTransaction`** (bảng `TopupTransactions`):
  - Lưu mã giao dịch SePay `sepayCode`.
  - Quản lý trạng thái nạp tiền (`Success`, `Failed`, `Pending`).
  - Ghi nhận `balanceBefore`, `balanceAfter`, nội dung chuyển khoản `transferContent`, lý do lỗi `failureReason` và thông tin Staff duyệt thủ công (nếu có).
- **Entity `Withdrawal`** (bảng `Withdrawals`):
  - Lưu thông tin yêu cầu rút tiền: `amountVnd`, `feeVnd`, `bankInfo` (FK → `SellerBankInfo`), `status` (`Pending`, `Processing`, `Completed`, `Rejected`, `Failed`), lý do từ chối `rejectionReason`, ảnh minh chứng chuyển khoản `proofFile`, thông tin staff duyệt.
- **Entity `WalletTransaction`** (bảng `WalletTransactions`):
  - Nhật ký biến động số dư: `amountVnd`, `balanceAfter`, `type` (`TOPUP`, `WITHDRAWAL`, `REFUND`, `PURCHASE_ESCROW`...), `status` (`SUCCESS`, `PENDING`, `COMPLETED`, `FAILED`).
- **Entity `SellerBankInfo`** (bảng `SellerBankInfos`):
  - Lưu thông tin ngân hàng của người bán: `bankName`, `accountNumber`, `branch`, liên kết với bảng `Users`.
- **Bảng `Users`**:
  - Quản lý số dư qua cột `balance_vnd` và tổng số tiền nạp qua `deposit_vnd`. Định dạng `Long` (VNĐ). Không sử dụng các cột tách biệt `availableBalance` / `holdBalance`.

### 3.2. Repositories (Spring Data JPA)

- **`TopupTransactionRepository`**:
  - `findBySepayCode(sepayCode)`: Đảm bảo tính bất biến (idempotency) của Webhook.
  - `searchTopups(status, keyword, id, userId, pageable)`: Hỗ trợ tìm kiếm, lọc và phân trang giao dịch nạp tiền phục vụ Staff.
- **`WithdrawalRepository`**:
  - `findBySellerAndIsDeleteFalseOrderByCreatedAtDesc(user)`: Lấy lịch sử rút tiền của Seller.
  - `searchWithdrawals(status, keyword, minAmount, pageable)`: Hỗ trợ Staff tìm kiếm và duyệt lệnh rút tiền.
- **`UserRepository`**:
  - `@Lock(LockModeType.PESSIMISTIC_WRITE)`: Khóa bản ghi User khi thực hiện cộng/trừ tiền để đảm bảo tính nhất quán dữ liệu ví.

### 3.3. DTOs

- **Request DTOs:** `TopupRetryRequestDto` (duyệt nạp tiền thủ công).
- **Response DTOs:** `WalletStatsDto`, `WalletTransactionDto`, `TopupResponseDto`.

### 3.4. Services (Business Logic)

- **`TopupService`**:
  - `processSepayWebhook(request)`: Nhận webhook, parse cú pháp `MMO-TOPUP-<userId>`, kiểm tra hạn mức tối thiểu/tối đa (`MIN_DEPOSIT_LIMIT_VND`, `MAX_DEPOSIT_LIMIT_VND`), cộng tiền vào `balanceVnd` và cập nhật lock/unlock shop của Seller.
  - `retryTopup(topupId, dto, staffId)`: Hỗ trợ Staff duyệt giao dịch lỗi cú pháp thủ công (cộng tiền vào tài khoản chỉ định và cập nhật trạng thái `Success`).
  - `getTopupStats()`: Tính toán thống kê tổng số lượng và số dư nạp.
- **`WithdrawalService`**:
  - `requestWithdrawal(userId, amount, otp)`: Kiểm tra ví không bị khóa (`withdrawalLocked`), kiểm tra số dư đủ (`balanceVnd >= amount + fee`), trừ trực tiếp số tiền này từ ví, chuyển lệnh sang `Pending`. Gửi OTP 2FA nếu config `REQUIRE_WITHDRAW_2FA = true`.
  - `updateWithdrawalStatus(id, newStatus, reviewerId, rejectionReason, proofFile)`: Xử lý duyệt rút tiền.
    - Nếu `Approved`/`Completed`: Chuyển trạng thái giao dịch rút tiền gốc thành `COMPLETED`.
    - Nếu `Rejected`/`Failed`: Hoàn lại tiền (`amountVnd + feeVnd`) về ví của Seller và tạo giao dịch `REFUND` trạng thái `COMPLETED`.
- **`WalletService`**:
  - `recordTransaction(user, type, amount, status, description, refId, balanceAfter)`: Ghi nhận lịch sử biến động số dư.

### 3.5. Controllers & Security

- **`TopupController`** (REST API `/api/sepay/webhook`):
  - Nhận webhook từ SePay (không yêu cầu đăng nhập).
- **`WalletController`** (REST API `/api/v1/wallet`):
  - `GET /stats` — Xem số dư và tổng tiền nạp.
  - `GET /transactions` — Lịch sử giao dịch ví của user (phân trang).
  - `GET /bank-info` — Lấy thông tin tài khoản ngân hàng của Seller.
  - `PUT /bank-info` — Cập nhật thông tin ngân hàng.
  - `GET /withdrawals` — Lịch sử yêu cầu rút tiền của Seller.
  - `POST /withdrawals` — Tạo yêu cầu rút tiền mới (`amountVnd`, `otp`).
  - `GET /withdrawals/config` — Lấy thông tin cấu hình rút tiền từ config (phí %, hạn mức min/max, yêu cầu 2FA).
- **`StaffTopupController`** (REST API `/api/v1/staff/topups`):
  - `@PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")` bảo vệ.
  - `GET /` — Phân trang danh sách nạp tiền.
  - `GET /stats` — Xem thống kê nạp tiền của Staff.
  - `GET /{id}` — Xem chi tiết giao dịch nạp tiền.
  - `POST /{id}/retry` — Staff duyệt thủ công lệnh nạp tiền.
- **`StaffController`** (MVC Cổng quản trị Staff):
  - `GET /staff/withdrawals` — Trang danh sách yêu cầu rút tiền.
  - `GET /staff/withdrawals/detail` — Xem chi tiết yêu cầu rút tiền và nhập minh chứng/lý do từ chối.
  - `POST /staff/withdrawals/update-status` — Duyệt hoàn tất lệnh rút tiền (đính kèm file ảnh minh chứng).
  - `POST /staff/withdrawals/reject` — Từ chối lệnh rút tiền.

---

## 4. Các thành phần Frontend

- **Màn hình Nạp tiền của Customer:**
  - File: `templates/wallet/topup.html` và JS `static/js/customer/account-topup.js`.
  - Hiển thị hướng dẫn cú pháp chuyển khoản và mã QR tự động chứa thông tin User ID.
- **Màn hình Ví & Lịch sử giao dịch:**
  - File: `templates/account/wallet.html` và JS `static/js/customer/account-wallet.js`.
- **Màn hình Yêu cầu rút tiền & Cấu hình ngân hàng của Seller:**
  - File: `templates/seller/withdraw.html` (kết hợp form yêu cầu rút tiền và quản lý thông tin ngân hàng).
- **Màn hình duyệt rút tiền của Staff:**
  - File: `templates/staff/withdrawals.html` và `templates/staff/withdrawal-detail.html` render qua Thymeleaf.

---

## 5. Definition of Done

- Toàn bộ giao dịch ví bắt buộc dùng số nguyên lớn `Long` cho VNĐ.
- Tuyệt đối mọi thao tác thay đổi ví phải dùng `Pessimistic Lock` (`findByIdForUpdate` trên User) để tránh lỗi race condition.
- API SePay webhook phải trả về `200 OK` ngay lập tức nếu mã giao dịch chuyển tiền đã được xử lý trước đó (Idempotency).
- Lệnh rút tiền bị từ chối/thất bại bắt buộc phải hoàn trả toàn bộ số tiền gốc và phí rút về ví của Seller và ghi nhận dưới dạng giao dịch `REFUND`.