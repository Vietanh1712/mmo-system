# PLAN — Customer Portal (`feat-customer`)

## 1. Mục tiêu (Goals)
Triển khai giao diện trang cá nhân khách hàng (Customer Portal), bao gồm:
- Trang hồ sơ và thông tin cá nhân.
- Trang nạp tiền quét mã QR và xem biến động số dư ví.
- Trang danh sách đơn hàng đã mua và chi tiết đơn hàng (giải mã code).
- Trang gửi đánh giá & phản hồi sản phẩm (`leave-feedback.html`).

## 2. Giao diện & Các tệp Template

- **Hồ sơ cá nhân:** `apps/frontend/templates/profile/index.html`
- **Ví & Giao dịch:** `templates/account/wallet.html`, `transactions.html`, `topup.html`
- **Đơn hàng & Giải mã:** `templates/account/orders.html`, `order-detail.html`
- **Đánh giá sản phẩm:** `templates/account/leave-feedback.html`

## 3. Các kịch bản JavaScript (Static Scripts)

- **KYC Submission:** `static/js/customer/account-kyc.js`
- **Wallet & Topup:** `static/js/customer/account-wallet.js`, `account-topup.js`, `account-transactions.js`
- **Orders List & Decryption:** `static/js/customer/account-orders.js`, `account-order-detail.js`

## 4. Các tài liệu đặc tả liên quan

- [SPEC-profile.md](./SPEC-profile.md)
- [SPEC-kyc.md](./SPEC-kyc.md)
- [SPEC-wallet.md](./SPEC-wallet.md)
- [SPEC-orders.md](./SPEC-orders.md)
- [SPEC-order-detail.md](./SPEC-order-detail.md)
- [SPEC-leave-feedback.md](./SPEC-leave-feedback.md)