# UC-09 — Rút Tiền (Withdrawal Flow)

> **Feature:** `feat-wallet` | **Phiên bản:** 1.0 | **Trạng thái:** Published
> **Tham chiếu FR:** FR-WALL-05
> **Cập nhật:** 2026-07-23

---

## 1. Tổng Quan

| Thuộc tính | Nội dung |
|:---|:---|
| **Mã Use Case** | UC-09 |
| **Tên** | Rút Tiền từ Ví (Withdrawal) |
| **Tác nhân chính** | Người bán (Seller), Nhân viên (Staff) |
| **Mô tả ngắn** | Seller tạo yêu cầu rút tiền từ số dư khả dụng về tài khoản ngân hàng. Yêu cầu sẽ được tạo ở trạng thái `PENDING` và chờ Staff phê duyệt để giải ngân hoặc từ chối. |
| **Độ ưu tiên** | Cao (P1) |

---

## 2. Tác Nhân & Điều Kiện

### 2.1 Tác Nhân

| Tác nhân | Vai trò |
|:---|:---|
| **Người bán (Seller)** | Gửi yêu cầu rút tiền, nhận mã OTP qua email để xác thực bảo mật. |
| **Nhân viên (Staff)** | Xem danh sách yêu cầu rút tiền, kiểm tra chứng từ (nếu có) và thực hiện phê duyệt (Approved/Completed) hoặc từ chối (Rejected). |

### 2.2 Điều Kiện Tiền Quyết (Preconditions)

- Người bán đã đăng nhập (`@AuthenticationPrincipal`).
- Số dư khả dụng của Seller phải $\ge$ số tiền muốn rút.
- Số tiền rút phải nằm trong giới hạn cho phép (min, max cấu hình từ hệ thống).

### 2.3 Hậu Điều Kiện (Postconditions)

- **Khi tạo lệnh:** Số dư của Seller bị trừ ngay lập tức, tạo record Withdrawal trạng thái `Pending` và record `WalletTransaction` tương ứng.
- **Khi duyệt (Completed):** Quy trình chuyển tiền thủ công hoàn tất qua 2 bước (Staff chuyển trạng thái sang `Processing` -> Staff chuyển khoản ngoài hệ thống -> Staff tải lên ảnh biên lai `proofFile` để đóng lệnh sang `Completed`). Gửi thông báo hệ thống cho Seller.
- **Khi từ chối (Rejected):** Hoàn lại số tiền vào số dư khả dụng (cộng lại tiền), ghi nhận lý do từ chối, sinh giao dịch Refund, gửi thông báo hệ thống cho Seller. Lệnh bị huỷ có thể xảy ra ở bất kỳ bước nào trước khi hoàn tất.
- **Ghi chú hiển thị:** Dù lưu trong CSDL là các trạng thái Tiếng Anh (`Pending`, `Processing`, `Completed`, `Rejected`), trên giao diện Frontend toàn bộ hệ thống (Staff và Seller) sẽ hiển thị Tiếng Việt (`Chờ xử lý`/`Chờ duyệt`, `Đang xử lý`, `Hoàn tất`, `Bị từ chối`).

---

## 3. Luồng Xử Lý

### 3.1 Luồng Chính 1 — Seller Tạo Yêu Cầu Rút Tiền

```
Bước 1  [Seller]:     Vào mục "Rút tiền", hệ thống hiển thị thông tin ngân hàng thụ hưởng hiện tại.
Bước 2  [Seller]:     Nhập số tiền muốn rút (vd: 100,000đ).
Bước 3  [Seller]:     Nhấn nút xác nhận rút tiền.
Bước 4  [Backend]:    Validate hạn mức và số dư. Trừ tiền số dư, tạo lệnh rút tiền (PENDING).
Bước 5  [Backend]:    Trả về kết quả thành công cho Client.
```

### 3.2 Luồng Chính 2 — Staff Xử Lý & Giải Ngân (Thủ công 2 bước)

```
Bước 1  [Staff]:      Nhân viên xem danh sách các yêu cầu Rút tiền ở trạng thái `Pending`.
Bước 2  [Staff]:      Click xem chi tiết yêu cầu rút tiền của Seller.
Bước 3  [Staff]:      Kiểm tra thông tin tài khoản thụ hưởng, click "Tiếp nhận xử lý". Trạng thái chuyển thành `Processing`.
Bước 4  [Staff]:      Đăng nhập vào ứng dụng Internet Banking của ngân hàng ngoài hệ thống và thực hiện chuyển khoản số tiền (sau khi trừ phí) đến số tài khoản thụ hưởng của Seller. Chụp lại ảnh màn hình chuyển khoản thành công.
Bước 5  [Staff]:      Tại giao diện chi tiết, Staff chọn upload ảnh biên lai (proof) vừa chụp.
Bước 6  [Staff]:      Click "Hoàn tất chuyển tiền". Hệ thống kiểm tra đã có ảnh biên lai chưa, nếu có thì lưu ảnh, đổi trạng thái lệnh thành `Completed` và gửi thông báo cho Seller.
```

### 3.3 Các Ngoại Lệ (Exceptions)

- **Staff từ chối yêu cầu (Rejected):**
  - Nếu số tài khoản thụ hưởng không hợp lệ hoặc sai tên chủ tài khoản, Staff chọn "Từ chối" và nhập lý do.
  - Giao dịch (`Transaction`) trạng thái Pending sẽ bị huỷ, tiến hành sinh giao dịch Hoàn tiền (Refund) và cộng lại số dư khả dụng cho Seller. Lệnh rút tiền được đổi trạng thái thành `Rejected`.
- **Huỷ lệnh trong khi đang xử lý (Processing):** 
  - Nếu Staff đã "Tiếp nhận xử lý" nhưng không thể chuyển khoản, Staff vẫn có thể bấm "Từ chối" và nhập lý do để hoàn tiền lại cho Seller.

---

## 4. Tham Chiếu API

| Phương thức | Endpoint | Mô tả | Vai trò |
|:---|:---|:---|:---|
| `GET` | `/withdrawals/config` | Lấy hạn mức Min/Max rút tiền, phí rút tiền (`WITHDRAWAL_FEE_PERCENT`). | Seller |
| `GET` | `/withdrawals` | Lấy danh sách lịch sử rút tiền của Seller. | Seller |
| `GET` | `/withdrawals/{id}` | Lấy chi tiết lệnh rút tiền (bao gồm `proofFile`). | Seller |
| `POST` | `/withdrawals/send-otp` | Yêu cầu gửi mã OTP rút tiền về Email của Seller. | Seller |
| `POST` | `/withdrawals` | Gửi yêu cầu rút tiền (gồm `amountVnd` và `otp`). | Seller |
| `GET` | `/staff/withdrawals` | Phân trang danh sách toàn bộ các yêu cầu rút tiền kèm thống kê. | Staff |
| `POST` | `/staff/withdrawals/update-status` | Thay đổi trạng thái (`Processing`, `Completed`), yêu cầu multipart/form-data upload ảnh khi `Completed`. | Staff |
| `POST` | `/staff/withdrawals/reject` | Từ chối rút tiền (Rejected) kèm lý do. | Staff |