# SPEC — Seller Console Dashboard & Shop Management
> **Feature ID:** `feat-seller`
> **UC Coverage:** UC-04 (Seller Registration), UC-06 (Shop Product Management), UC-11 (Feedback & Reviews)
> **Version:** 2.0 | **Status:** Active
> **Author:** Team | **Last Updated:** 2026-06-27

---

## 1. CONTEXT & GOAL (BỐI CẢNH & MỤC TIÊU)
Người bán (Seller) sử dụng cổng Console để theo dõi doanh số, CRUD sản phẩm, thêm biến thể sản phẩm, và thực hiện rút tiền khả dụng tích lũy được từ đơn hàng.

---

## 2. DANH SÁCH FILE THỰC TẾ & MAPPING
Các tệp tin thực tế trong dự án:

- **Tổng quan người bán (`/seller` hoặc `/seller/dashboard`)**:
  * View: `templates/seller/dashboard.html`
  * Script: `static/js/seller-console.js`
- **Quản lý kho hàng (`/seller/inventory`)**:
  * View: `templates/seller/inventory.html`
  * Script: `static/js/seller-console.js`
- **Thêm sản phẩm (`/seller/products/new`)**:
  * View: `templates/seller/product-add.html`
  * Script: `static/js/seller-console.js`
- **Chỉnh sửa sản phẩm (`/seller/products/edit`)**:
  * View: `templates/seller/product-edit.html`
  * Script: `static/js/seller-console.js`
- **Thêm/Sửa biến thể (`/seller/variants/new` & `/seller/variants/edit`)**:
  * View: `templates/seller/variant-form.html`
  * Script: `static/js/seller-console.js`
- **Giao dịch người bán (`/seller/transactions`)**:
  * View: `templates/seller/transactions.html`
  * Script: `static/js/seller-console.js`
- **Danh sách rút tiền (`/seller/withdrawals`)**:
  * View: `templates/seller/withdrawals.html`
  * Script: `static/js/seller-console.js`
- **Chi tiết rút tiền (`/seller/withdrawals/detail`)**:
  * View: `templates/seller/withdrawal-detail.html`
  * Script: `static/js/seller-console.js`
- **Thống kê doanh số (`/seller/statistics`)**:
  * View: `templates/seller/statistics.html`
  * Script: `static/js/seller-console.js`
- **Danh sách đánh giá (`/seller/reviews`)**:
  * View: `templates/seller/reviews.html`
  * Script: `static/js/seller-console.js`
- **Khiếu nại đơn hàng (`/seller/complaints`)**:
  * View: `templates/seller/complaints.html`
  * Script: `static/js/seller-console.js`
- **Chi tiết khiếu nại (`/seller/complaints/detail`)**:
  * View: `templates/seller/complaint-detail.html`
  * Script: `static/js/seller-console.js`
- **Cấu hình Shop (`/seller/shop-info`)**:
  * View: `templates/seller/shop-info.html`
  * Script: `static/js/seller-shop-settings.js`
- **Đóng cửa hàng (`/seller/close-shop`)**:
  * View: `templates/seller/close-shop.html`
  * Script: `static/js/seller-close-shop.js`

---

## 3. FUNCTIONAL REQUIREMENTS (EARS)
| ID | EARS Requirement |
|---|---|
| FR-SELL-01 | WHEN a Seller requests withdrawal, THE SYSTEM SHALL require input amount >= 50,000 VND and a valid OTP code. |
| FR-SELL-02 | WHEN a Seller deletes a product, THE SYSTEM SHALL call the soft-delete API to mark `isDelete = 1`. |
| FR-SELL-03 | WHEN Staff suspends a shop, THE SYSTEM SHALL display the "Tạm ngưng" badge and status description on the Seller's Shop Info page (`/seller/shop-info`), accompanied by a highlighted alert card containing a real-time countdown timer ("Tự động mở lại sau") showing remaining Days, Hours, Minutes, and Seconds until automatic reinstatement. |
| FR-SELL-04 | THE SYSTEM SHALL NOT display the manual "Tạm đóng cửa hàng" toggle button on `/seller/shop-info`. |
| FR-SELL-05 | THE SYSTEM SHALL display all shop operating status badges and sidebar status texts in 100% Vietnamese (Hoạt động, Tạm ngưng, Tạm khóa, Khóa vĩnh viễn, Đã đóng Shop, Chờ duyệt) across the Seller Console and Customer Profile pages (`/profile`). |
| FR-SELL-06 | WHEN a Seller whose shop is in "Tạm khóa" (Locked) status attempts to access any Seller Console route (`/seller/*`), THE SYSTEM SHALL intercept the request and render a full-screen blurred modal overlay (`showShopLockedOverlay`) displaying the exact unlock deadline timestamp (e.g., `HH:mm dd/MM/yyyy`) and preventing access until the lock period expires. |
| FR-SELL-07 | WHEN a Seller whose shop is in "Tạm ngưng" (Suspended) status accesses the Seller Console (`/seller/*`), THE SYSTEM SHALL allow access to view dashboard metrics and order history, but SHALL prevent submitting or updating products (`/seller/products/new`, `POST /api/seller/products`). |
| FR-SELL-08 | WHEN a Seller whose shop is in "Khóa vĩnh viễn" (Banned) status attempts to access any Seller Console route (`/seller/*`), THE SYSTEM SHALL render a dedicated full-screen banned overlay (`showShopBannedOverlay`) informing the user that the shop is permanently banned and blocking access to the Seller Dashboard. |