# USER-SPEC — Đặc tả giao diện người dùng Guest & Customer
> **Phiên bản:** 1.0 | **Cập nhật:** 2026-06-27

---

## 1. LUỒNG ĐĂNG KÝ VÀ XÁC THỰC (auth.js)
1. Khách truy cập vào `/register`. Điền thông tin gồm Email, Mật khẩu, Họ tên.
2. Bấm "Đăng ký" -> JavaScript gọi API `POST /api/auth/register`.
3. Nếu thành công, JavaScript tự động lưu email vào bộ nhớ tạm và chuyển hướng người dùng sang `/verify-otp`.
4. Người dùng điền mã OTP 6 số nhận từ email -> Gọi `POST /api/auth/verify-otp`. Kích hoạt tài khoản thành công và chuyển hướng tới `/login`.

---

## 2. TRANG HỒ SƠ CÁ NHÂN VÀ ĐỊNH DANH KYC
- **Trang Profile (`/profile/index.html`)**: Hiển thị thông tin email, họ tên, số dư ví hiện có (`available_balance` và `hold_balance` đóng băng).
- **Yêu cầu KYC (`account-kyc.js`)**: Cho phép người dùng chọn tệp ảnh CCCD (Mặt trước, mặt sau và chân dung) và nhập mã định danh để thực hiện tải lên yêu cầu xác minh danh tính.

---

## 3. MUA HÀNG VÀ CHỜ ESCROW
- **Chi tiết sản phẩm (`product-detail.html`)**: Cho phép người dùng bấm "Mua ngay".
- Khi bấm mua hàng -> Gọi `POST /api/transactions/purchase` kèm ID biến thể. Hệ thống hiển thị Modal thông báo tiền đã được đưa vào Escrow giam giữ 72h để an toàn giao dịch.