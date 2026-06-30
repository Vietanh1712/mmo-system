# TASKS — System Administration & RBAC (`feat-admin`)

> **Feature ID:** `feat-admin`
> **Phiên bản:** 1.0 | **Cập nhật:** 2026-06-27

---

## Phase 1: Database & Entities
- [x] **1.1** Định nghĩa bảng `Permissions`, `UserPermissions`, `AuditLogs`, và `SystemConfigurations` đồng bộ JPA Entity.
- [x] **1.2** Seed các quyền mặc định cho Staff như `APPROVE_KYC`, `APPROVE_WITHDRAWAL`, `RESOLVE_COMPLAINT`.

## Phase 2: Repositories
- [x] **2.1** `PermissionRepository` — lấy danh sách quyền.
- [x] **2.2** `AuditLogRepository` — ghi nhận các log hoạt động.

## Phase 4: Business Logic (Services)
- [x] **4.1** `StaffPermissionService` — xử lý gán và thu hồi quyền Staff.
- [x] **4.2** `AdminRevenueService` — tính toán báo cáo doanh thu động.

## Phase 5: Controllers & Security
- [x] **5.1** Cấu hình phân quyền `@PreAuthorize("hasRole('ADMIN')")` bảo vệ các endpoint API quản trị.
- [x] **5.2** `AdminPageController` — định hướng giao diện Thymeleaf Admin Dashboard.

## Phase 6: Testing
- [x] **6.1** Unit Test `AdminRevenueServiceTest` — kiểm chứng kết xuất doanh số.
- [x] **6.2** Unit Test `AdminUserManagementServiceTest` — verify luồng khóa/mở khóa tài khoản người dùng vi phạm.