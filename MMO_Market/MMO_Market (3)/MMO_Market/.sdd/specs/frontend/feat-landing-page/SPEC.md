# SPEC — Landing Page & Storefront
> **Feature ID:** `feat-landing-page`
> **Version:** 2.0 | **Status:** Active
> **Author:** Team | **Last Updated:** 2026-06-27

---

## 1. CONTEXT & GOAL (BỐI CẢNH & MỤC TIÊU)
Trang chủ và các trang hiển thị sản phẩm công khai dành cho khách vãng lai, người dùng nạp giỏ hàng và thực hiện đăng nhập.

---

## 2. DANH SÁCH FILE THỰC TẾ & MAPPING
Các tệp tin thực tế trong dự án:

- **Trang chủ (`/`)**:
  * View: `templates/home.html`
- **Catalog tìm kiếm sản phẩm (`/products`)**:
  * View: `templates/products.html`
- **Tìm kiếm theo từ khóa (`/search`)**:
  * View: `templates/search-results.html`
- **Chi tiết sản phẩm (`/products/{productId}`)**:
  * View: `templates/product-detail.html`
- **Gian hàng người bán (`/shop/{sellerId}`)**:
  * View: `templates/shop.html`
- **Giỏ hàng (`/cart`)**:
  * View: `templates/cart.html`
- **Thanh toán đơn hàng (`/checkout`)**:
  * View: `templates/checkout.html`
- **Trực tiếp gửi ticket hỗ trợ (`/support`)**:
  * View: `templates/support.html`
  * Script: `static/js/support.js`
- **Tin nhắn chat (`/messages`)**:
  * View: `templates/messages.html`
- **Xác thực (`/login`, `/register`, `/verify-otp`, `/forgot-password`, `/reset-password`)**:
  * Views: Nằm dưới thư mục `templates/auth/`
  * Script: `static/js/auth.js`

---

## 3. FUNCTIONAL REQUIREMENTS (EARS)
| ID | EARS Requirement |
|---|---|
| FR-LAND-01 | WHEN a Guest completes checkout, THE SYSTEM SHALL deduct funds from available balance and transfer to Escrow. |