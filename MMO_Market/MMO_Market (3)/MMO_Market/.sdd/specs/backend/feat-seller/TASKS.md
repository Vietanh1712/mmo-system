# TASKS — Seller Console & Shop Management (`feat-seller`)

> **Feature ID:** `feat-seller` | **UC Coverage:** UC-04 (Seller Registration), UC-05 (Seller Console)
> **Phiên bản:** 2.0 | **Cập nhật:** 2026-07-24

---

## Phase 1: Seller Registration (Đăng ký Shop)

- [x] **1.1** Entity & Repository: Tạo `SellerRegistration`, `SellerBankInfo` và các interface repository liên quan.
- [x] **1.2** Business Logic: Cài đặt luồng Auto-Approve trong `ShopRegistrationService.submitRegistration()` kiểm tra trạng thái KYC, tự động cập nhật User Role thành `SELLER`.
- [x] **1.3** API Endpoint: Viết `ShopRegistrationController` cho API `POST /api/v1/shop-registrations`.

## Phase 2: Seller Dashboard & Info

- [x] **2.1** Tích hợp hàm xem số liệu thống kê `/api/seller/dashboard` (đếm completedSales, totalRevenue, activeProductsCount, openComplaints).
- [x] **2.2** API quản lý Thông tin Shop (`/api/seller/shop-info`).
- [x] **2.3** Cài đặt chức năng Tạm đóng/Mở cửa hàng (`/api/seller/shop-status`).

## Phase 3: Quản lý Sản phẩm & Biến thể (Products & Variants)

- [x] **3.1** API Danh sách và Chi tiết sản phẩm (`GET /api/seller/products`, `/api/seller/products/{id}`).
- [x] **3.2** API CRUD Sản phẩm (`POST`, `PUT`, `DELETE /api/seller/products`). Ràng buộc theo cấp độ Shop Level và tài khoản âm (`balanceVnd < 0`).
- [x] **3.3** API CRUD Biến thể (`POST`, `PUT`, `DELETE /api/seller/variants`).

## Phase 4: Quản lý Kho tài sản số (Digital Assets)

- [x] **4.1** API Upload Hàng loạt Tài sản số (`POST /api/seller/digital-assets`). Hỗ trợ loại ACCOUNT, KEY, GAME_CARD.
- [x] **4.2** API Liệt kê và Xóa tài sản (`GET`, `DELETE`).
- [x] **4.3** Logic tự động cập nhật số lượng tồn kho `variant.stock` khi thêm/xóa tài sản.

## Phase 5: Giao dịch & Rút tiền (Transactions & Withdrawals)

- [x] **5.1** API Lịch sử giao dịch bán hàng (`GET /api/seller/transactions`).
- [x] **5.2** API Danh sách yêu cầu rút tiền (`GET /api/seller/withdrawals`).
- [x] **5.3** API Yêu cầu rút tiền mới (`POST /api/seller/withdrawals`) tích hợp với `WithdrawalService`.

## Phase 6: Hỗ trợ, Đánh giá & Báo cáo (Complaints, Reviews, Flags)

- [x] **6.1** API Thống kê chuyên sâu (`GET /api/seller/statistics`) - trả về biểu đồ doanh thu tuần, top sản phẩm.
- [x] **6.2** API Quản lý Khiếu nại (`GET /api/seller/complaints`, trả lời chat `POST /api/seller/complaints/{id}/chat`).
- [x] **6.3** API Xem đánh giá khách hàng (`GET /api/seller/reviews`) và cờ vi phạm Shop (`GET /api/seller/shop-flags`).