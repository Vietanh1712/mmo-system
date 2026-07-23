# TASKS — System Administration & RBAC (`feat-admin`)

> **Feature ID:** `feat-admin`
> **Phiên bản:** 2.0 | **Cập nhật:** 2026-07-16

---

## Phase 1: Database & Entities

- [x] **1.1** Định nghĩa bảng `Permissions`, `UserPermissions`, `AuditLogs`, và `SystemConfigurations` trong database đồng bộ JPA Entity.
- [x] **1.2** Seed các quyền mặc định cho Staff như `APPROVE_KYC`, `APPROVE_WITHDRAWAL`, `RESOLVE_COMPLAINT`.

## Phase 2: Repositories

- [x] **2.1** `PermissionRepository` — lấy danh sách quyền.
- [x] **2.2** `AuditLogRepository` — ghi nhận các log hoạt động bất biến.
- [x] **2.3** `SystemConfigurationRepository` — truy xuất và lưu cấu hình hệ thống.

## Phase 3: Services (Business Logic)

- [x] **3.1** `StaffPermissionService` — xử lý gán (`assignPermissions`) và thu hồi (`revokePermissions`) quyền Staff hàng loạt.
- [x] **3.2** `AdminRevenueService` — tính toán báo cáo doanh thu ròng từ commissions, shop opening fees và withdrawal fees.
- [x] **3.3** `AdminUserManagementService` — khóa/mở khóa tài khoản thành viên (`toggleLock`) và CRUD tài khoản Staff.
- [x] **3.4** `SystemConfigurationService` — cập nhật cấu hình chung và biểu phí giao dịch.
- [x] **3.5** `NotificationService` — tạo thông báo broadcast hệ thống, toggle chế độ bảo trì, và ghi audit log.

## Phase 4: Controllers & Security

- [x] **4.1** Bảo mật các endpoint `/api/admin/**` qua bộ lọc bảo mật Spring Security và kiểm tra cờ `isLocked` thời gian thực.
- [x] **4.2** Cung cấp các REST API:
  - `AdminRevenueController` — thống kê doanh thu và xuất báo cáo CSV.
  - `SystemConfigurationController` — cập nhật cấu hình chung và phí.
  - `AdminUserManagementController` — quản lý tài khoản thành viên và Staff.
  - `AuditLogController` — phân trang logs và xuất CSV.
  - `StaffPermissionController` — API gán và thu hồi quyền Staff.
  - `NotificationController` — các API admin quản lý thông báo/bảo trì.

## Phase 5: Giao diện (Thymeleaf & JS)

- [x] **5.1** `users.html` — Thiết kế giao diện Admin Console duy nhất chứa các panel view động.
- [x] **5.2** `admin-console.js` — Viết script xử lý điều hướng tab trên sidebar và AJAX binding cho các panel view:
  - `dashboard`: Đọc tổng số liệu thống kê và vẽ biểu đồ SVG doanh thu tuần.
  - `audit-logs`: Bảng nhật ký hoạt động phân trang, tìm kiếm, lọc hành động, xem chi tiết JSON payload.
  - `revenue`: Thống kê nguồn thu và xem lịch sử dòng tiền.
  - `accounts`: CRUD Staff, danh sách thành viên và toggle-lock tài khoản.
  - `permissions`: Chọn nhiều nhân viên và quyền hạn để gán/thu hồi nhanh.
  - `system-config` / `commissions`: Nhập thông số cấu hình và biểu phí.
  - `notifications`: Soạn thảo thông báo hệ thống, toggle bảo trì, hiển thị banner cảnh báo.

## Phase 6: Testing

- [x] **6.1** Unit Test `AdminRevenueServiceTest` — kiểm chứng kết xuất doanh số.
- [x] **6.2** Unit Test `AdminUserManagementServiceTest` — verify luồng khóa/mở khóa tài khoản người dùng vi phạm.