# SPEC — Customer Portal Dashboard
> **Feature ID:** `feat-customer`
> **UC Coverage:** UC-02 (User Profile), UC-03 (KYC Verification), UC-07 (Wallet Top-up), UC-08 (Order Purchase & Escrow), UC-10 (Complaints)
> **Version:** 2.0 | **Status:** Active
> **Author:** Team | **Last Updated:** 2026-06-27

---

## 1. CONTEXT & GOAL (BỐI CẢNH & MỤC TIÊU)
Người mua (Customer) cần có giao diện để cập nhật thông tin cá nhân, gửi yêu cầu định danh KYC, nạp tiền ngân hàng thông qua SePay, theo dõi lịch sử giao dịch ví và quản lý đơn hàng số đã mua để nhận code giải mã.

---

## 2. DANH SÁCH FILE THỰC TẾ & MAPPING
Toàn bộ các tệp tin dưới đây là thực tế trong thư mục `apps/frontend/`:

- **Hồ sơ cá nhân (`/profile`)**:
  * View: `templates/profile/index.html`
  * Script: `static/js/profile.js`
- **Định danh KYC (`/account/kyc`)**:
  * View: `templates/account/kyc.html`
  * Script: `static/js/account-kyc.js`
- **Bảo mật & Đổi mật khẩu (`/account/security`)**:
  * View: `templates/account/security.html`
  * Script: `static/js/account-security.js`
- **Đăng ký bán hàng (`/account/register-shop`)**:
  * View: `templates/account/register-shop.html`
  * Script: `static/js/account-register-shop.js`
- **Thông tin ví tài chính (`/wallet`)**:
  * View: `templates/account/wallet.html`
  * Script: `static/js/account-wallet.js`
- **Nạp tiền quét QR (`/wallet/topup`)**:
  * View: `templates/account/topup.html`
  * Script: `static/js/account-topup.js`
- **Biến động số dư ví (`/wallet/transactions`)**:
  * View: `templates/account/transactions.html`
  * Script: `static/js/account-transactions.js`
- **Đơn hàng đã mua (`/account/orders`)**:
  * View: `templates/account/orders.html`
  * Script: `static/js/account-orders.js`
- **Chi tiết đơn hàng (`/account/orders/{orderCode}`)**:
  * View: `templates/account/order-detail.html`
  * Script: `static/js/account-order-detail.js`
- **Đánh giá sản phẩm (`/account/orders/{orderCode}/feedback`)**:
  * View: `templates/account/leave-feedback.html`
- **Xem thông báo (`/account/notifications`)**:
  * View: `templates/account/notifications.html`
  * Script: `static/js/account-notifications.js`
- **Support Ticket (`/account/tickets`)**:
  * View: `templates/account/tickets.html`
  * Script: `static/js/account-tickets.js`

---

## 3. STATE & DATA BINDING (sessionStorage)
- **accessToken**: Đọc từ `sessionStorage.getItem('accessToken')` để đính vào header AJAX.
- **Form KYC**: CCCD số, File ảnh mặt trước, mặt sau và chân dung selfie. Gửi Multipart qua fetch API tới `/api/kyc/submit`.

---

## 4. FUNCTIONAL REQUIREMENTS (EARS)
| ID | EARS Requirement |
|---|---|
| FR-CUST-01 | WHEN a Customer uploads KYC documents, THE SYSTEM SHALL send a POST multipart form request to `/api/kyc/submit`. |
| FR-CUST-02 | WHEN a Customer views an order detail, THE SYSTEM SHALL retrieve the encrypted code and allow client-side decryption. |