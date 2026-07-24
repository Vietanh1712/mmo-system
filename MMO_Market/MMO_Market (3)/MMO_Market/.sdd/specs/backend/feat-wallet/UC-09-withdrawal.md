# UC-09 — Rút Tiền (Withdrawal Flow)

> **Feature:** `feat-wallet` | **Trạng thái:** Published
> **Tham chiếu FR:** FR-WALL-06 đến FR-WALL-10

---

## 1. Tổng Quan

| Thuộc tính | Nội dung |
|:---|:---|
| **Mã Use Case** | UC-09 |
| **Tên** | Rút Tiền từ Ví (Withdrawal) |
| **Tác nhân chính** | Người bán (Seller), Nhân viên (Staff), Quản trị viên (Admin) |
| **Mô tả ngắn** | Seller tạo yêu cầu rút tiền từ số dư ví (`balanceVnd`) về tài khoản ngân hàng của mình. Số tiền rút và phí rút được khấu trừ ngay lập tức. Lệnh rút ở trạng thái `Pending` sẽ chờ Staff/Admin xử lý (`Processing`), chuyển tiền bên ngoài hệ thống ngân hàng thực tế, và hoàn tất (`Completed`) bằng cách upload ảnh biên lai. Nếu bị từ chối/thất bại (`Rejected`/`Failed`), hệ thống tự động hoàn tiền gốc và phí về ví Seller. |
| **Độ ưu tiên** | Cao (P1) |

---

## 2. Tác Nhân & Điều Kiện

### 2.1 Tác Nhân

| Tác nhân | Vai trò |
|:---|:---|
| **Người bán (Seller)** | Gửi yêu cầu rút tiền, cấu hình ngân hàng thụ hưởng, nhận mã OTP qua email để xác thực 2FA. |
| **Nhân viên (Staff) / Admin** | Xem danh sách, tiếp nhận xử lý lệnh rút, chuyển khoản thực tế qua ngân hàng, tải lên ảnh biên lai `proofFile` và hoàn tất giao dịch hoặc từ chối kèm lý do. |

### 2.2 Điều Kiện Tiền Quyết (Preconditions)

- Người bán đã đăng nhập (`@AuthenticationPrincipal`).
- Ví của Seller không bị khóa tính năng rút tiền (`withdrawalLocked = false`) và không có số dư âm.
- Số dư ví (`balanceVnd`) đủ để chi trả cả số tiền rút và phí giao dịch (`amountVnd + feeVnd`).
- Số tiền rút phải nằm trong giới hạn cho phép (min, max cấu hình từ hệ thống, mặc định 50K - 50M VNĐ).
- Seller phải cấu hình thông tin tài khoản ngân hàng thụ hưởng trước khi thực hiện.

### 2.3 Hậu Điều Kiện (Postconditions)

- **Khi tạo lệnh:** Số dư của Seller bị trừ ngay lập tức một khoản bằng `amountVnd + feeVnd`. Bản ghi `Withdrawal` trạng thái `Pending` và bản ghi `WalletTransaction` loại `WITHDRAWAL` trạng thái `PENDING` được lưu thành công.
- **Khi duyệt (Completed):** Lệnh chuyển trạng thái sang `Processing` -> Staff thực hiện chuyển tiền ngoài ngân hàng -> Staff tải ảnh biên lai -> Trạng thái đổi thành `Completed`, bản ghi `WalletTransaction` gốc đổi thành `COMPLETED`.
- **Khi từ chối (Rejected/Failed):** Hoàn lại toàn bộ số tiền `amountVnd + feeVnd` vào ví Seller, trạng thái lệnh chuyển thành `Rejected` / `Failed`, bản ghi `WalletTransaction` gốc đổi thành `FAILED`, và hệ thống tự động sinh một giao dịch hoàn tiền `REFUND` trạng thái `COMPLETED` để cộng lại tiền ví. Gửi thông báo hệ thống cho Seller.

---

## 3. Luồng Xử Lý

### 3.1 Luồng Chính 1 — Seller Tạo Yêu Cầu Rút Tiền

```
Bước 1  [Seller]:     Vào mục "Rút tiền", hệ thống hiển thị thông tin ngân hàng thụ hưởng hiện tại và cấu hình hạn mức/phí rút tiền.
Bước 2  [Seller]:     Nếu chưa có ngân hàng, Seller cập nhật thông tin qua form cấu hình tài khoản thụ hưởng.
Bước 3  [Seller]:     Nhập số tiền muốn rút. Hệ thống tính phí rút dựa trên phần trăm (ví dụ: 1.5%).
Bước 4  [Seller]:     Bấm "Gửi mã OTP". Hệ thống gửi mã xác thực 6 số qua email.
Bước 5  [Seller]:     Nhập OTP và bấm xác nhận rút tiền.
Bước 6  [Backend]:    Validate: ví không bị khóa rút, số tiền nằm trong min/max limit, OTP đúng thời hạn và chưa sử dụng, số dư ví đủ.
Bước 7  [Backend]:    Trừ tổng số tiền (rút + phí) từ user.balanceVnd, tạo record Withdrawal trạng thái 'Pending', tạo record WalletTransaction trạng thái 'PENDING'.
Bước 8  [Backend]:    Gửi thông báo hệ thống tới Seller và toàn thể Staff có thẩm quyền APPROVE_WITHDRAWALS.
Bước 9  [Backend]:    Trả về kết quả thành công cho Client.
```

### 3.2 Luồng Chính 2 — Staff Xử Lý & Giải Ngân (Thủ công 2 bước)

```
Bước 1  [Staff]:      Nhân viên xem danh sách các yêu cầu Rút tiền ở trạng thái Pending.
Bước 2  [Staff]:      Click xem chi tiết yêu cầu rút tiền của Seller.
Bước 3  [Staff]:      Kiểm tra thông tin tài khoản thụ hưởng, click "Tiếp nhận xử lý". Lệnh chuyển thành 'Processing'.
Bước 4  [Staff]:      Thực hiện chuyển khoản số tiền thực nhận (sau khi trừ phí) ngoài hệ thống ngân hàng. Chụp lại ảnh màn hình chuyển khoản thành công.
Bước 5  [Staff]:      Staff tải lên ảnh biên lai chuyển khoản (proofFile).
Bước 6  [Staff]:      Bấm "Hoàn tất chuyển tiền". Hệ thống kiểm tra đã có ảnh biên lai chưa, lưu ảnh, đổi trạng thái lệnh thành 'Completed', đổi trạng thái WalletTransaction gốc thành 'COMPLETED', ghi nhận log hệ thống và gửi thông báo kết quả cho Seller.
```

### 3.3 Các Ngoại Lệ (Exceptions)

- **Staff từ chối yêu cầu (Rejected / Failed):**
  - Nếu thông tin ngân hàng sai hoặc giao dịch bị lỗi, Staff click "Từ chối" và nhập lý do.
  - Hệ thống cộng lại tiền `amountVnd + feeVnd` vào ví Seller, đổi trạng thái lệnh rút thành `Rejected` (hoặc `Failed`), đổi trạng thái WalletTransaction gốc thành `FAILED`, tạo giao dịch ví `REFUND` `COMPLETED` và tự động kiểm tra mở/khóa shop tùy theo công nợ. Gửi thông báo lý do từ chối cho Seller.
- **Duyệt đúp lệnh rút:**
  - Nếu lệnh rút đã ở trạng thái khác `Pending` hoặc `Processing`, hệ thống chặn không cho chỉnh sửa tiếp (ném lỗi `IllegalStateException`).

---

## 4. Tham Chiếu API

### 4.1 Dành cho Seller (Tác vụ ví)

| Phương thức | Endpoint | Mô tả |
|:---|:---|:---|
| `GET` | `/api/seller/withdrawals/config` hoặc `/api/v1/wallet/withdrawals/config` | Lấy hạn mức Min/Max rút tiền, phí rút tiền (`WITHDRAWAL_FEE_PERCENT`) và yêu cầu 2FA. |
| `GET` | `/api/seller/withdrawals` hoặc `/api/v1/wallet/withdrawals` | Lấy danh sách lịch sử rút tiền của Seller. |
| `GET` | `/api/seller/withdrawals/{id}` | Lấy chi tiết một lệnh rút tiền cụ thể của Seller. |
| `POST` | `/api/seller/withdrawals/send-otp` | Yêu cầu gửi mã OTP rút tiền về Email của Seller. |
| `POST` | `/api/seller/withdrawals` hoặc `/api/v1/wallet/withdrawals` | Gửi yêu cầu rút tiền (gồm `amountVnd` và `otp`). |
| `GET` | `/api/v1/wallet/bank-info` | Lấy thông tin tài khoản ngân hàng thụ hưởng đã lưu. |
| `PUT` | `/api/v1/wallet/bank-info` | Cập nhật thông tin ngân hàng của Seller (`SellerBankInfo`). |

### 4.2 Dành cho Staff/Admin (Cổng quản trị)

| Phương thức | Endpoint | Mô tả |
|:---|:---|:---|
| `GET` | `/staff/withdrawals` (MVC Page) | Xem danh sách toàn bộ các yêu cầu rút tiền toàn hệ thống kèm thống kê các trạng thái. |
| `GET` | `/staff/withdrawals/detail?id={id}` (MVC Page) | Xem chi tiết thông tin chuyển khoản thụ hưởng của Seller. |
| `POST` | `/staff/withdrawals/update-status` (MVC Form) | Chuyển trạng thái sang `Processing` hoặc `Completed` (yêu cầu gửi kèm `proofFile` dạng file upload). |
| `POST` | `/staff/withdrawals/reject` (MVC Form) | Từ chối lệnh rút tiền (chuyển sang `Rejected`) và gửi kèm lý do từ chối `reason`. |