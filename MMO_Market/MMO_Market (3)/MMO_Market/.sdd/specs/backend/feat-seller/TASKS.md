# TASKS — Seller Console & Shop Management (`feat-seller`)

> **Feature ID:** `feat-seller` | **UC Coverage:** UC-04 (Seller Registration)
> **Phiên bản:** 1.0 | **Cập nhật:** 2026-06-27

---

## Phase 1: Database & Entities

- [x] **1.1** Database schema tạo các bảng `SellerRegistrations` và `SellerBankInfo` liên kết khóa ngoại tới `Users`.
- [x] **1.2** JPA Entities `SellerRegistration` và `SellerBankInfo` — map đúng thông tin tên cửa hàng `shop_name`, mô tả và chi tiết ngân hàng để rút tiền.

## Phase 2: Repositories

- [x] **2.1** `SellerRegistrationRepository` — tìm kiếm hồ sơ cửa hàng theo người dùng `findByUserId`.
- [x] **2.2** `SellerBankInfoRepository` — lấy cấu hình ngân hàng của Seller.

## Phase 3: DTOs & Validation

- [x] **3.1** `ShopRegistrationRequest` — validate bắt buộc có tên Shop và số tài khoản ngân hàng.

## Phase 4: Business Logic (Services)

- [x] **4.1** `ShopRegistrationService.registerShop()` — xác minh tài khoản đã được KYC thành công trước khi phê duyệt nâng cấp vai trò lên `Seller`.

## Phase 5: Controllers & Security

- [x] **5.1** `ShopRegistrationController` — API đăng ký `POST /api/v1/shop-registrations`.
- [x] **5.2** `SellerController` — cung cấp cổng thông tin Seller Console chỉ truy cập được bởi người dùng có vai trò `Seller`.

## Phase 6: Testing

- [x] **6.1** Viết kiểm thử tự động xác minh người dùng chưa KYC sẽ bị hệ thống từ chối đăng ký shop.