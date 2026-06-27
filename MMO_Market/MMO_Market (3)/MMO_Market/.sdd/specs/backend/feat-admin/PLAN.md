# PLAN — System Administration & RBAC (`feat-admin`)

## 1. Mục tiêu (Goals)

Triển khai các tính năng quản lý hệ thống, phân quyền hạn nhân viên (RBAC), cấu hình tham số hoạt động của sàn và báo cáo doanh thu dành riêng cho Quản trị viên (Admin) theo đặc tả `SPEC.md` (feat-admin). Các nghiệp vụ bao gồm:
- Xem các thống kê doanh số, doanh thu, lợi nhuận, số lượng đơn hàng thành công theo thời gian và danh mục.
- Quản lý danh sách người dùng, thay đổi vai trò (Role), khóa/mở khóa tài khoản người dùng vi phạm.
- Phân quyền hạn chi tiết (Permissions) cho tài khoản Staff.
- Cấu hình các tham số hệ thống (như phí giao dịch %, chế độ bảo trì, hạn mức).
- Giám sát nhật ký hoạt động bất biến (Audit Logs) của Admin và Staff.

## 2. Kiến trúc & Công nghệ

- **Backend:** Java 17, Spring Boot 3.1, Spring Data JPA, Spring Security, SQL Server (T-SQL).
- **Frontend:** Các trang Thymeleaf kết hợp JS thuần sử dụng REST API (`authFetch` truyền token Bearer JWT).
- **Tuân thủ:** Mô hình phân lớp Controller → Service → Repository → Entity; DTO Pattern cho Request/Response; Bảo vệ chặt chẽ: toàn bộ các API quản trị bắt đầu bằng `/api/admin/**` hoặc kiểm duyệt ở service layer bắt buộc có role `ADMIN`.

## 3. Các thành phần Backend

### 3.1. Database Migration & Entities

- **Entity `Permission`** (bảng `Permissions`):
  - `name` (VARCHAR, UNIQUE): Tên quyền hạn (ví dụ: `APPROVE_KYC`, `APPROVE_WITHDRAWAL`).
  - `description` (NVARCHAR): Mô tả quyền hạn.
- **Entity `UserPermission`**:
  - Mối quan hệ nhiều-nhiều tự thiết lập nối `User` và `Permission` phục vụ RBAC cho Staff.
- **Entity `AuditLog`** (bảng `AuditLogs`):
  - Nhật ký hoạt động bất biến ghi nhận: `action`, `userId`, `targetType`, `targetId`, `details` (dưới dạng JSON lưu diff của cấu hình hoặc mô tả chi tiết), `createdAt`.
- **Entity `SystemConfiguration`** (bảng `SystemConfigurations`):
  - `configKey` (VARCHAR, UNIQUE): Khóa cấu hình (ví dụ: `TRANSACTION_FEE_PERCENTAGE`).
  - `configValue` (VARCHAR): Giá trị cấu hình.
  - `description` (NVARCHAR): Mô tả tác dụng.

### 3.2. Repositories (Spring Data JPA)

- `SystemConfigurationRepository`:
  - `findByConfigKey(key)`: Lấy cấu hình.
- `AuditLogRepository`:
  - `save(auditLog)`: Lưu nhật ký (chỉ cho phép ghi mới, chặn cập nhật/xóa để đảm bảo tính bất biến).
  - Tìm kiếm và lấy danh sách log phân trang.
- `PermissionRepository`:
  - Tìm kiếm, nạp danh sách quyền hạn phục vụ gán/hủy quyền.

### 3.3. DTOs

- Request: `PermissionAssignRequest`, `SystemConfigRequest`, `UserRoleUpdateRequest`.
- Response: `RevenueSummaryResponse`, `UserDetailResponse`, `AuditLogResponse`.

### 3.4. Services (Business Logic)

- **`AdminRevenueService`**:
  - Tính toán tổng doanh thu từ các đơn hàng đã hoàn thành, tổng hoa hồng giữ lại của hệ thống và tổng số đơn hàng.
- **`AdminUserManagementService`**:
  - `lockUser(userId, lock)`: Đặt cờ `isLocked = 1` hoặc `0` cho User, hủy phiên làm việc.
  - `updateUserRole(userId, newRole)`: Thay đổi quyền của tài khoản (VD: Customer -> Seller).
  - `assignPermissionToStaff(staffId, permissionId)`: Thêm quyền cho nhân viên.
- **`AuditLogService`**:
  - Ghi nhật ký các tác vụ hành chính nhạy cảm.
- **`SystemConfigurationService`**:
  - Cập nhật các giá trị cấu hình hệ thống và ghi nhận lịch sử vào `AuditLog`.

### 3.5. Controllers & Security

- **`AdminPageController`** (MVC `@RequestMapping("/admin")`):
  - Render giao diện Thymeleaf: Quản trị người dùng (`admin/users`), Quản trị phân quyền Staff (`admin/permissions`), Dashboard Admin (`admin/dashboard`), Xem Audit Logs (`admin/audit-logs`).
- **REST REST API Controllers**:
  - `/api/admin/revenue/summary` (`AdminRevenueController`)
  - `/api/admin/users/**` (`AdminUserManagementController`)
  - `/api/admin/system-config` (`SystemConfigurationController`)
  - `/api/admin/audit-logs` (`AuditLogController`)
  - Toàn bộ được kiểm duyệt bảo vệ bằng Spring Security: chỉ tài khoản có role `ADMIN` mới được gọi.

---

## 4. Các thành phần Frontend

- **Bảng điều khiển Admin Console:**
  - File: `templates/admin/dashboard.html`.
- **Trang quản trị người dùng & phân quyền:**
  - File: `templates/admin/users.html` và JS `static/js/admin/admin-users.js`.
  - Hỗ trợ Popup gán quyền chi tiết cho Staff, nút Khóa/Mở khóa.
- **Trang giám sát log:**
  - File: `templates/admin/audit-logs.html` và JS `static/js/admin/admin-audit-logs.js`.

---

## 5. Definition of Done

- Toàn bộ dữ liệu trong bảng `AuditLogs` là bất biến (chỉ được thực hiện thao tác INSERT, chặn tất cả API/hàm liên quan đến UPDATE, DELETE đối với bảng này).
- Endpoint `/api/admin/**` bắt buộc bảo vệ chặt chẽ bằng quyền `ROLE_ADMIN`.
- Việc thay đổi cấu hình hệ thống phải được xác thực giá trị đầu vào (ví dụ phí phần trăm phải nằm trong khoảng từ 0 đến 100).