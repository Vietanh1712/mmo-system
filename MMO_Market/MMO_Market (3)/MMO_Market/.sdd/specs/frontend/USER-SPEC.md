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

---

## 4. BỘ LỌC SẢN PHẨM & ĐÁNH GIÁ (RATING FILTER)
- **Cơ chế lọc theo sao**:
  - Các mức lọc sao (`5 sao`, `4 sao`, `3 sao`) hoạt động theo các khoảng điểm trung bình độc lập:
    - `5 sao` -> Điểm trung bình `>= 5.0`
    - `4 sao` -> Điểm trung bình `[4.0, 5.0)` (tương đương 4.x sao)
    - `3 sao` -> Điểm trung bình `[3.0, 4.0)` (tương đương 3.x sao)
  - Khi người dùng tick chọn nhiều checkbox cùng lúc, hệ thống thực hiện gom các khoảng lọc bằng phép toán hợp `OR`.
- **Hành vi trên trang chi tiết sản phẩm (`product-detail.html`)**:
  - Các bộ lọc ở sidebar bên phải trang chi tiết sản phẩm sẽ không tự động kích hoạt chuyển hướng trang khi thay đổi tiêu chí (chọn select, tick checkbox hay kéo thanh trượt giá).
  - Trình duyệt chỉ thực hiện chuyển hướng sang trang kết quả khi người dùng nhấn nút **"Áp dụng bộ lọc"** (submit form).

---

## 5. QUY TRÌNH ĐẶT TRƯỚC SẢN PHẨM (PRE-ORDER REQUEST)
- **Giao diện yêu cầu đặt trước (`pre-order-request.html`)**:
  - Khi sản phẩm hết hàng, nút "Đặt trước" sẽ chuyển người dùng sang trang gửi yêu cầu đặt trước sản phẩm. Tiêu đề hiển thị chuẩn hóa là **"Đặt trước sản phẩm"** (không chứa chữ "Màn hình").
  - Giao diện, màu sắc, cỡ chữ và các hiệu ứng hover của nút bấm, các ô nhập liệu, bảng tổng tiền trên trang đặt trước được thiết kế đồng bộ với trang thanh toán (`checkout.html`), sử dụng màu cam thương hiệu (`#fd761a`) làm chủ đạo cho các nút bấm hành động chính.
- **Trang danh sách đơn đặt trước (`pre-orders.html`)**:
  - Hiển thị danh sách các yêu cầu đặt mua trước của người dùng.
  - Các bước tiến trình (workflow timeline) ở trạng thái hoạt động (active/đang xử lý) sử dụng màu cam thương hiệu (`#ea580c`) để đồng bộ hoàn toàn nhận diện giao diện khách hàng.