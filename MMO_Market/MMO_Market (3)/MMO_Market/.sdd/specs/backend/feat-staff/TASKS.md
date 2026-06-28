# TASKS — Staff Operations & Approvals (`feat-staff`)

> **Feature ID:** `feat-staff`
> **Phiên bản:** 1.0 | **Cập nhật:** 2026-06-27

---

## Phase 1: Database & Entities

- [x] **1.1** Tạo bảng `ShopFlags` để nhân viên đánh dấu cửa hàng vi phạm.

## Phase 2: Repositories

- [x] **2.1** `ShopFlagRepository` — truy vấn danh sách cờ cảnh cáo.

## Phase 4: Business Logic (Services)

- [x] **4.1** Triển khai xử lý duyệt/từ chối KYC và các lệnh rút tiền mặt.

## Phase 5: Controllers & Security

- [x] **5.1** `StaffController` — cung cấp giao diện MVC và bảo vệ các API duyệt bằng quyền `ROLE_STAFF`.