# TASKS — Customer Portal (`feat-customer`)

> **Feature ID:** `feat-customer` | **Trực thuộc:** Frontend | **UC Coverage:** UC-02, UC-03, UC-07, UC-08, UC-10, UC-19

---

## Phase 1: HTML Layout & Templates

- [x] **1.1** Xây dựng trang Hồ sơ cá nhân `profile/index.html` tích hợp sidebar điều hướng tài khoản.
- [x] **1.2** Xây dựng trang Bảo mật tài khoản `account/security.html` hiển thị form đổi mật khẩu và nút bật/tắt bảo mật 2 lớp (2FA).
- [x] **1.3** Xây dựng trang Định danh KYC `account/kyc.html` với Stepper tiến trình, bảng lịch sử định danh và form nhập thông tin CCCD + tải lên 3 ảnh.
- [x] **1.4** Xây dựng trang Ví của tôi `account/wallet.html` hiển thị số dư ví, các ô stats dòng tiền và danh sách 5 giao dịch gần đây.
- [x] **1.5** Xây dựng trang Nạp tiền `account/topup.html` hiển thị VietQR tự sinh động dựa trên thông tin ngân hàng SePay.
- [x] **1.6** Xây dựng trang Lịch sử giao dịch ví `account/transactions.html` có thanh lọc tìm kiếm và bảng danh sách giao dịch phân trang.
- [x] **1.7** Xây dựng trang Đơn hàng đã mua `account/orders.html` hiển thị danh sách đơn hàng đã mua kèm trạng thái và ngày giờ.
- [x] **1.8** Xây dựng trang Chi tiết đơn hàng `account/order-detail.html` hiển thị chi tiết sản phẩm, giá tiền và nút kích hoạt giải mã code sản phẩm số.
- [x] **1.9** Xây dựng trang Đăng ký mở Shop `account/register-shop.html` cho phép điền thông tin mở Shop.
- [x] **1.10** Xây dựng trang Yêu cầu hỗ trợ `account/tickets.html` hiển thị danh sách và gửi support ticket.
- [x] **1.11** Xây dựng trang Thông báo hệ thống `account/notifications.html`.
- [x] **1.12** Xây dựng trang Khiếu nại đơn hàng `account/complaints.html` và chi tiết khiếu nại `account/complaint-detail.html`.
- [x] **1.13** Thiết kế trang đánh giá sản phẩm `account/leave-feedback.html` với form rating sao và nhận xét.

## Phase 2: JavaScript & Business Integration

- [x] **2.1** `account-sidebar.js` — render sidebar điều hướng tài khoản đồng bộ thông tin ví (`balanceVnd`) và tên hiển thị tự động lấy từ thông tin profile.
- [x] **2.2** `profile.js` — gọi API `GET /api/v1/profile` để prefill dữ liệu và `PUT /api/v1/profile` để lưu cập nhật thông tin cá nhân.
- [x] **2.3** `account-security.js` — gọi API đổi mật khẩu `PUT /api/v1/profile/password`, bật 2FA bằng cách xác thực OTP kích hoạt `POST /api/v1/profile/2fa/enable`, gửi OTP kích hoạt 2FA `POST /api/v1/profile/2fa/send-otp` và tắt 2FA `POST /api/v1/profile/2fa/disable`.
- [x] **2.4** `account-kyc.js` — gọi API `GET /api/v1/kyc/me` hiển thị lịch sử/trạng thái và `POST /api/v1/kyc` (gửi FormData multipart chứa 3 tệp hình ảnh) để nộp hồ sơ định danh.
- [x] **2.5** `account-wallet.js` — tải stats ví `GET /api/v1/wallet/stats` và tải 5 giao dịch gần đây `GET /api/v1/wallet/transactions?page=0&size=5`.
- [x] **2.6** `account-topup.js` — tải thông tin tài khoản ngân hàng SePay công khai `GET /api/sepay/config` và tự sinh mã QR VietQR động.
- [x] **2.7** `account-transactions.js` — tải toàn bộ lịch sử giao dịch ví và thực hiện bộ lọc tìm kiếm (loại giao dịch, trạng thái, khoảng thời gian) và phân trang thuần client-side.
- [x] **2.8** `account-register-shop.js` — gọi API `PUT /api/v1/profile/bank-info` cấu hình ngân hàng Seller và `POST /api/v1/profile/register-shop` đăng ký mở Shop bán hàng.
- [x] **2.9** `account-orders.js` — tải danh sách các đơn hàng đã mua của khách hàng.
- [x] **2.10** `account-order-detail.js` — tải chi tiết đơn hàng, xử lý giải mã client-side để hiển thị thông tin code sản phẩm số đã mua.
- [x] **2.11** `account-notifications.js` — gọi API thông báo hệ thống và xử lý đánh dấu đã đọc.
- [x] **2.12** `account-tickets.js` — gọi API để tạo ticket hỗ trợ và theo dõi phản hồi từ Staff/Admin.

## Phase 3: Testing & Polish

- [ ] **3.1** Kiểm thử tính đúng đắn của CSS responsive trên các thiết bị di động (Mobile, Tablet) cho toàn bộ trang con trong Customer Portal.
- [ ] **3.2** Kiểm thử kiểm soát lỗi nhập liệu (validate form) phía Client: họ tên, CCCD 9-12 số, định dạng ngày sinh, dung lượng file ảnh nộp KYC.
- [ ] **3.3** Kiểm thử giải mã dữ liệu an toàn phía Client đối với các đơn hàng chứa tài khoản số.