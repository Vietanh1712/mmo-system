# PLAN — System Administration & RBAC (`feat-admin`)

## 1. Mục tiêu (Goals)

Triển khai các tính năng quản lý hệ thống, phân quyền hạn nhân viên (RBAC), cấu hình tham số hoạt động của sàn, quản trị thông báo toàn sàn & bảo trì và báo cáo doanh thu dành riêng cho Quản trị viên (Admin) theo đặc tả `SPEC.md` (feat-admin). Các nghiệp vụ bao gồm:
- Xem các thống kê doanh thu ròng (Commissions, Shop opening fees, Withdrawal fees, Net total) và vẽ biểu đồ dòng tiền SVG tuần.
- Quản lý danh sách người dùng, thay đổi vai trò (Role), khóa/mở khóa tài khoản người dùng vi phạm (chặn qua JWT filter).
- Phân quyền hạn chi tiết (Permissions) cho tài khoản Staff (Assign / Revoke).
- Cấu hình các tham số hệ thống chung và phí & hoa hồng.
- Soạn thảo và phát hành thông báo hệ thống / quản lý trạng thái bảo trì sàn.
- Giám sát nhật ký hoạt động bất biến (Audit Logs) của Admin và Staff.

## 2. Kiến trúc & Công nghệ

- **Backend:** Java 17, Spring Boot 3.1, Spring Data JPA, Spring Security, SQL Server (T-SQL).
- **Frontend:** Single Page Console (`templates/admin/users.html`) kết hợp script `static/js/admin-console.js` sử dụng REST API (`authFetch` truyền token Bearer JWT) để chuyển đổi nhanh giữa các view panel động.
- **Tuân thủ:** Mô hình phân lớp Controller → Service → Repository → Entity; DTO Pattern cho Request/Response; Bảo vệ chặt chẽ: toàn bộ các API quản trị bắt đầu bằng `/api/admin/**` bắt buộc có vai trò hợp lệ.

## 3. Các thành phần Backend

### 3.1. Database Migration & Entities

- **Entity `Permission`** (bảng `Permissions`):
  - `id` (INT), `name` (VARCHAR, UNIQUE), `groupName` (VARCHAR), `description` (NVARCHAR), `createdAt`.
- **Entity `AuditLog`** (bảng `AuditLogs`):
  - Nhật ký hoạt động bất biến ghi nhận: `id`, `userId` (NOT NULL), `action`, `details` (NVARCHAR(MAX) chứa JSON diff), `targetUserId`, `targetId`, `targetType`, `createdAt`.
- **Entity `SystemConfiguration`** (bảng `SystemConfigurations`):
  - `id` (INT), `configKey` (VARCHAR, UNIQUE), `configValue` (NVARCHAR(MAX)), `description`, `updatedBy`, `updatedAt`.
- **Entity `Notification`** (bảng `Notifications`):
  - Dùng để lưu trữ thông báo broadcast hệ thống do Admin/Staff phát ra.

### 3.2. Repositories (Spring Data JPA)

- `SystemConfigurationRepository`: Lấy và lưu các cấu hình theo key.
- `AuditLogRepository`: Lưu và tìm kiếm phân trang log hoạt động (chỉ INSERT).
- `PermissionRepository`: Quản lý các quyền hạn hệ thống.
- `UserRepository`: Truy cập thông tin, phân trang và tìm kiếm thành viên.

### 3.3. DTOs

- Request: `SystemConfigUpdateRequest`, `CommissionsUpdateRequest`, `StaffUpsertRequest`, `AssignRequest`, `RevokeRequest`, `NotificationCreateRequest`.
- Response: `RevenueSummaryResponse`, `AdminUserResponse`, `SystemConfigResponse`.

### 3.4. Services (Business Logic)

- **`AdminRevenueService`**: Tính toán doanh thu ròng từ commissions, shop opening fees, và withdrawal fees. Quản lý danh sách giao dịch dòng tiền.
- **`AdminUserManagementService`**:
  - `toggleLock(operatorId, targetUserId)`: Đổi trạng thái khóa tài khoản người dùng.
  - `createStaff` / `updateStaff` / `deleteStaff`: Quản trị tài khoản nhân viên.
- **`StaffPermissionService`**:
  - `assignPermissions` / `revokePermissions`: Gán và thu hồi các quyền cho Staff.
- **`SystemConfigurationService`**: Cập nhật cấu hình chung và phí hệ thống, lưu Audit Log.
- **`NotificationService`**: Tạo/phát thông báo hệ thống và toggle trạng thái bảo trì.

### 3.5. Controllers & Security

- **`AdminUserManagementController`** (`/api/admin/user-management/**`)
- **`AdminRevenueController`** (`/api/admin/revenue/**`)
- **`SystemConfigurationController`** (`/api/admin/system-config/**`)
- **`AuditLogController`** (`/api/admin/audit-logs/**`)
- **`StaffPermissionController`** (`/api/admin/staff-permissions/**`)
- **`NotificationController`** (`/api/admin/notifications/**` - các endpoint quản trị)

---

## 4. Các thành phần Frontend

- **Trang điều khiển duy nhất Admin Console:**
  - File: `templates/admin/users.html` và kịch bản `static/js/admin-console.js`.
  - Quản lý chuyển đổi các panel view phụ thông qua hàm `switchAdminView(viewId)`:
    - `dashboard`: Thống kê tài khoản và vẽ biểu đồ SVG doanh thu tuần.
    - `audit-logs`: Danh sách logs, tìm kiếm, lọc nhóm tác vụ/hành động con, loại bỏ cột Chi tiết để tối ưu không gian hiển thị, Việt hóa 100%.
    - `revenue`: Biến động số dư và nút xuất báo cáo Excel/CSV.
    - `accounts`: Bộ lọc đa năng 6 trường (`search`, `role`, `status`, `startDate`, `endDate`, `sortOrder`), danh sách user (đã bỏ cột "Xác thực"), nút khóa/mở khóa, và form CRUD nhân viên Staff.
    - `permissions`: Dropdown multiselect để gán/thu hồi quyền của Staff.
    - `system-config` / `commissions`: Biểu mẫu cập nhật cấu hình (đã chuẩn hóa nhãn "Thời gian hết hạn đăng nhập (phút)", kết nối 100% Backend Java thực tế) và biểu phí hoa hồng lưu CSDL.
    - `notifications`: Form phát thông báo toàn sàn, xem chi tiết qua Popup Modal, toggle bảo trì, banner cảnh báo.

---

## 5. Definition of Done

- Toàn bộ dữ liệu trong bảng `AuditLogs` là bất biến (chỉ cho phép INSERT, chặn UPDATE/DELETE).
- JWT Filter hoạt động động để từ chối truy cập của các tài khoản có `isLocked = 1` ngay lập tức.
- Validate chặt chẽ các giá trị cấu hình đầu vào trước khi lưu vào CSDL SQL Server và có hiệu lực thời gian thực ở Backend.