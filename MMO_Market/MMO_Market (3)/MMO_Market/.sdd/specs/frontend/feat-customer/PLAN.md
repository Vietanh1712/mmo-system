# PLAN — Customer Portal (`feat-customer`)

## 1. Mục tiêu (Goals)

Triển khai và tích hợp toàn bộ giao diện trang cá nhân khách hàng (Customer Portal) sử dụng Thymeleaf + Javascript thuần kết nối với REST API backend qua `authFetch` bảo mật (JWT):
- **Quản lý Hồ sơ & Thông tin:** Cho phép cập nhật thông tin cá nhân.
- **Bảo mật tài khoản:** Đổi mật khẩu và quản lý bảo mật 2 lớp (2FA).
- **Xác thực định danh (KYC):** Form nộp hồ sơ KYC (CCCD, CMND, PASSPORT, DRIVER_LICENSE) kèm 3 ảnh tải lên và xem trạng thái/lịch sử.
- **Ví & Tài chính:** Màn hình ví tổng quan, nạp tiền tự động ( VietQR ), lịch sử giao dịch và rút tiền ( cấu hình ngân hàng Seller ).
- **Đăng ký Shop:** Yêu cầu chuyển tài khoản thành Seller để mở Shop bán hàng.
- **Quản lý Đơn hàng:** Xem danh sách đơn hàng đã mua và giải mã chi tiết đơn hàng (lấy thông tin tài khoản sản phẩm số đã mua).
- **Hỗ trợ & Khiếu nại:** Tạo yêu cầu hỗ trợ (Support Tickets) và quản lý khiếu nại (Complaints).
- **Thông báo hệ thống:** Xem danh sách thông báo và đánh dấu đã đọc.
- **Đánh giá sản phẩm:** Để lại feedback/đánh giá sản phẩm sau khi hoàn tất đơn hàng.

## 2. Giao diện & Các tệp Template (Thymeleaf templates/...)

- **Hồ sơ cá nhân:** `templates/profile/index.html` (Index chính của portal).
- **Bảo mật tài khoản:** `templates/account/security.html`.
- **Xác thực KYC:** `templates/account/kyc.html`.
- **Ví & Giao dịch:** `templates/account/wallet.html`, `templates/account/topup.html`, `templates/account/transactions.html`.
- **Đăng ký mở Shop:** `templates/account/register-shop.html`.
- **Đơn hàng & Giải mã:** `templates/account/orders.html`, `templates/account/order-detail.html`.
- **Hỗ trợ & Tickets:** `templates/account/tickets.html`.
- **Thông báo hệ thống:** `templates/account/notifications.html`.
- **Khiếu nại:** `templates/account/complaints.html`, `templates/account/complaint-detail.html`.
- **Đánh giá sản phẩm:** `templates/account/leave-feedback.html`.

## 3. Các kịch bản JavaScript (static/js/customer/...)

- **Khung giao diện chung (Sidebar):** `account-sidebar.js` (quản lý render layout sidebar đồng bộ thông tin ví và profile của khách hàng).
- **Hồ sơ cá nhân:** `profile.js`.
- **Bảo mật & 2FA:** `account-security.js`.
- **Nộp hồ sơ KYC:** `account-kyc.js`.
- **Ví & Tài chính:** `account-wallet.js`, `account-topup.js`, `account-transactions.js`.
- **Đăng ký mở Shop:** `account-register-shop.js`.
- **Đơn hàng & Giải mã:** `account-orders.js`, `account-order-detail.js`.
- **Hỗ trợ & Tickets:** `account-tickets.js`.
- **Thông báo hệ thống:** `account-notifications.js`.

## 4. Các tài liệu đặc tả liên quan

- [SPEC.md](./SPEC.md) — Đặc tả tổng quan Customer Portal.
- [SPEC-profile.md](./SPEC-profile.md) — Đặc tả Hồ sơ cá nhân.
- [SPEC-security.md](./SPEC-security.md) — Đặc tả Bảo mật & 2FA.
- [SPEC-kyc.md](./SPEC-kyc.md) — Đặc tả Định danh KYC.
- [SPEC-wallet.md](./SPEC-wallet.md) — Đặc tả Ví tổng quan.
- [SPEC-wallet-topup.md](./SPEC-wallet-topup.md) — Đặc tả Nạp tiền QR.
- [SPEC-wallet-transactions.md](./SPEC-wallet-transactions.md) — Đặc tả Lịch sử giao dịch ví.
- [SPEC-register-shop.md](./SPEC-register-shop.md) — Đặc tả Đăng ký mở Shop.
- [SPEC-orders.md](./SPEC-orders.md) — Đặc tả Danh sách đơn hàng.
- [SPEC-order-detail.md](./SPEC-order-detail.md) — Đặc tả Chi tiết & Giải mã đơn hàng.
- [SPEC-leave-feedback.md](./SPEC-leave-feedback.md) — Đặc tả Đánh giá sản phẩm.
- [SPEC-notifications.md](./SPEC-notifications.md) — Đặc tả Thông báo.