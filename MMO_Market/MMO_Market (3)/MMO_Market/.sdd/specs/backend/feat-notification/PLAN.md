# PLAN — Notifications & Maintenance (`feat-notification`)

## 1. Mục tiêu (Goals)

Triển khai luồng thông báo biến động số dư tài khoản, thông tin đơn hàng tới người dùng, phát thông báo Global toàn hệ thống, và quản lý chế độ bảo trì hệ thống (Maintenance Mode) theo đặc tả `SPEC.md` (feat-notification). Hệ thống cho phép:
- Admin/Staff tạo, xóa các thông báo hệ thống (Broadcast/Global) và bật/tắt chế độ bảo trì.
- Người dùng (Customer/Seller) nhận các thông báo cá nhân, đọc thông báo Global và đánh dấu đã đọc.
- Chặn truy cập hoặc hiển thị màn hình bảo trì khi chế độ bảo trì đang bật.

## 2. Kiến trúc & Công nghệ

- **Backend:** Java 17, Spring Boot 3.1, Spring Data JPA, SQL Server (T-SQL).
- **Frontend:** Thymeleaf template engine, CSS thuần, Javascript thuần (`authFetch` truyền JWT Token qua header).
- **Tuân thủ:** Controller → Service → Repository → Entity; DTO Pattern (đóng gói Request/Response sạch sẽ); ghi nhận lịch sử thay đổi cấu hình hệ thống bằng Audit Log.

## 3. Các thành phần Backend

### 3.1. Database Migration & Entities

- **Entity `Notification`** (bảng `Notifications`):
  - `userId` (BIGINT): ID người nhận. `NULL` đại diện cho thông báo Global (Broadcast) toàn hệ thống.
  - `title`, `content` (NVARCHAR): Tiêu đề và nội dung thông báo.
  - `type` (VARCHAR): Loại thông báo (`info`, `warning`, `maintenance`, `policy`).
  - `severity` (VARCHAR): Mức độ cảnh báo (`INFO`, `WARNING`, `DANGER`).
  - `isRead` (BIT): Trạng thái đã đọc (chỉ áp dụng với thông báo cá nhân).
  - `isDelete` (BIT): Đánh dấu xóa mềm.
- **Entity `SystemConfiguration`** (bảng `SystemConfigurations`):
  - Dùng để lưu trữ cấu hình bảo trì hệ thống qua key `MAINTENANCE_MODE` (giá trị `TRUE` / `FALSE`).
- **Entity `AuditLog`** (bảng `AuditLogs`):
  - Ghi nhận lại các thao tác nhạy cảm của Admin/Staff như: tạo/xóa thông báo, thay đổi chế độ bảo trì.

### 3.2. Repositories (Spring Data JPA)

- **`NotificationRepository`**:
  - `searchNotifications(type, search, pageable)`: Tìm kiếm và phân trang thông báo hệ thống (do Admin/Staff phát ra) phục vụ màn hình Console.
  - `findAllByUserIdAndIsDeleteFalseOrderByCreatedAtDesc(userId)`: Lấy các thông báo cá nhân của người dùng.
  - `findAllBroadcastNotifications()`: Lấy các thông báo Global phát từ hệ thống/Admin.

### 3.3. DTOs

- Request: `NotificationCreateRequest` (chứa thông tin `title`, `content`, `type`, `activateMaintenance` phát đi từ Admin).
- Response: `AdminActionResponse` (chứa kết quả thao tác của Admin/Staff).

### 3.4. Services (Business Logic)

- **`NotificationService`**:
  - `getNotifications(search, type, page, size)`: Tìm kiếm phân trang thông báo hệ thống của Admin.
  - `createNotification(operatorId, title, content, type, activateMaintenance, ipAddress)`:
    - Xác thực quyền hạn Admin/Staff.
    - Lưu thông báo Broadcast mới.
    - Nếu bật bảo trì (`activateMaintenance = true` và `type = 'maintenance'`), tự động cập nhật cấu hình `MAINTENANCE_MODE = TRUE`.
    - Ghi nhận thao tác vào `AuditLog`.
  - `deleteNotification(operatorId, notifId, ipAddress)`: Xóa mềm thông báo hệ thống, ghi nhận `AuditLog`.
  - `toggleMaintenance(operatorId, active, ipAddress)`: Bật/tắt chế độ bảo trì hệ thống và ghi nhận `AuditLog`.
  - `getUserNotifications(userId)`: Trả về danh sách kết hợp gồm thông báo cá nhân và thông báo Broadcast hệ thống, sắp xếp theo thời gian mới nhất.
  - `markAsRead(userId, notifId)` & `markAllAsRead(userId)`: Đánh dấu đã đọc thông báo.

### 3.5. Controllers & Security

- **`NotificationController`**:
  - Các API của Admin/Staff (yêu cầu phân quyền Admin/Staff ở Service):
    - `GET /api/notifications`: Xem danh sách thông báo hệ thống.
    - `POST /api/admin/notifications`: Tạo thông báo mới.
    - `POST /api/admin/notifications/toggle-maintenance`: Bật/tắt bảo trì.
    - `DELETE /api/admin/notifications/{id}`: Xóa thông báo.
    - `GET /api/admin/notifications/maintenance-status`: Lấy trạng thái bảo trì hiện tại.
  - Các API của người dùng (Customer/Seller):
    - `GET /api/v1/notifications`: Lấy danh sách thông báo cá nhân và global.
    - `POST /api/v1/notifications/{id}/read`: Đọc 1 thông báo.
    - `POST /api/v1/notifications/mark-all-read`: Đọc tất cả thông báo.

---

## 4. Các thành phần Frontend

- **Trang danh sách thông báo của người dùng:**
  - File: `templates/account/notifications.html` và file JS `static/js/customer/account-notifications.js`.
  - Hiển thị danh sách thông báo dạng feed, phân biệt màu sắc/icon theo `severity` (đỏ với bảo trì, vàng với cảnh báo, xanh/xám với thông báo thường).
  - Có nút "Đánh dấu tất cả đã đọc".
- **Trang quản trị thông báo & bảo trì (Admin Console):**
  - File: `templates/admin/notifications.html` và file JS `static/js/admin/admin-notifications.js`.
  - Form tạo thông báo hệ thống kèm checkbox "Kích hoạt chế độ bảo trì".
  - Nút chuyển đổi nhanh chế độ bảo trì hệ thống (Maintenance Toggle).

---

## 5. Definition of Done

- Toàn bộ các API nhạy cảm của Admin/Staff liên quan đến hệ thống phải được chặn phân quyền chặt chẽ ở Service layer (`requireAdminOrStaff`).
- Mọi thay đổi về cấu hình hệ thống (bảo trì) hoặc phát thông báo phải được ghi nhận chi tiết địa chỉ IP và lịch sử thay đổi vào bảng `AuditLogs`.
- Giao diện người dùng phải hiển thị trực quan thông báo Global mới nhất nếu hệ thống sắp tiến hành bảo trì.