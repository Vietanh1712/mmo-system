# SPEC — Customer Portal Dashboard

> **Feature ID:** `feat-customer`
> **UC Coverage:** UC-02 (User Profile), UC-03 (KYC Verification), UC-07 (Wallet Top-up), UC-08 (Order Purchase & Escrow), UC-10 (Complaints), UC-19 (Wallet Dashboard)

---

## 1. CONTEXT & GOAL (BỐI CẢNH & MỤC TIÊU)

Người mua (Customer) cần có giao diện trực quan để cập nhật thông tin cá nhân, thay đổi mật khẩu và cài đặt bảo mật 2 lớp, gửi yêu cầu định danh KYC, nạp tiền ngân hàng qua SePay, theo dõi số dư ví và lịch sử giao dịch, quản lý đơn hàng số đã mua để giải mã code sản phẩm, tạo ticket hỗ trợ và gửi feedback sản phẩm.

---

## 2. DANH SÁCH FILE THỰC TẾ & MAPPING

Toàn bộ các tệp tin dưới đây là thực tế trong thư mục `apps/frontend/`:

- **Hồ sơ cá nhân (`/profile`)**:
  * View: `templates/profile/index.html`
  * Script: `static/js/customer/profile.js`
- **Định danh KYC (`/account/kyc`)**:
  * View: `templates/account/kyc.html`
  * Script: `static/js/customer/account-kyc.js`
- **Bảo mật & Đổi mật khẩu (`/account/security`)**:
  * View: `templates/account/security.html`
  * Script: `static/js/customer/account-security.js`
- **Đăng ký bán hàng (`/account/register-shop`)**:
  * View: `templates/account/register-shop.html`
  * Script: `static/js/customer/account-register-shop.js`
- **Thông tin ví tài chính (`/wallet`)**:
  * View: `templates/account/wallet.html`
  * Script: `static/js/customer/account-wallet.js`
- **Nạp tiền quét QR (`/wallet/topup`)**:
  * View: `templates/account/topup.html`
  * Script: `static/js/customer/account-topup.js`
- **Biến động số dư ví (`/wallet/transactions`)**:
  * View: `templates/account/transactions.html`
  * Script: `static/js/customer/account-transactions.js`
- **Đơn hàng đã mua (`/account/orders`)**:
  * View: `templates/account/orders.html`
  * Script: `static/js/customer/account-orders.js`
- **Chi tiết đơn hàng (`/account/orders/{orderCode}`)**:
  * View: `templates/account/order-detail.html`
  * Script: `static/js/customer/account-order-detail.js`
- **Đánh giá sản phẩm (`/account/orders/{orderCode}/feedback`)**:
  * View: `templates/account/leave-feedback.html`
- **Xem thông báo (`/account/notifications`)**:
  * View: `templates/account/notifications.html`
  * Script: `static/js/customer/account-notifications.js`
- **Support Ticket (`/account/tickets`)**:
  * View: `templates/account/tickets.html`
  * Script: `static/js/customer/account-tickets.js`

---

## 3. STATE & DATA BINDING (localStorage & sessionStorage)

- **accessToken / refreshToken**: Token đồng bộ ở cả `localStorage` và `sessionStorage` để phục vụ tự động đăng nhập khi mở tab mới hoặc giữ phiên trong phiên làm việc. Mã token đính vào header AJAX qua Bearer token.
- **Form KYC**: Đọc thông tin cá nhân và nộp tệp ảnh mặt trước, mặt sau và ảnh chân dung. Gửi Multipart qua fetch API trực tiếp tới `/api/v1/kyc`.

---

## 4. FUNCTIONAL REQUIREMENTS (EARS)

| ID | EARS Requirement |
|---|---|
| FR-CUST-01 | WHEN a Customer uploads KYC documents, THE SYSTEM SHALL send a POST multipart form request to `/api/v1/kyc`. |
| FR-CUST-02 | WHEN a Customer views an order detail, THE SYSTEM SHALL retrieve the encrypted code and allow client-side decryption. |
| FR-CUST-03 | WHEN a Customer creates or updates a Seller bank account, THE SYSTEM SHALL send PUT request to `/api/v1/wallet/bank-info`. |