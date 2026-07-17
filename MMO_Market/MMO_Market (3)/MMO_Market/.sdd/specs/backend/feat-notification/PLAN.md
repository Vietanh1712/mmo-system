# PLAN — Account Notifications (`feat-notification`)

## 1. Mục tiêu (Goals)

Triển khai luồng thông báo biến động số dư tài khoản, thông tin giao dịch ví, thông tin đơn hàng tới người dùng cá nhân, và hiển thị các thông báo Broadcast hệ thống từ Admin theo đặc tả `SPEC.md` (feat-notification). Hệ thống cho phép:
- Người dùng (Customer, Seller, Staff, Admin) nhận các thông báo cá nhân, đọc thông báo Global và đánh dấu đã đọc.
- Hiển thị badge số lượng thông báo chưa đọc trên biểu tượng Chuông ở Header.
- Đánh dấu đã đọc cho từng thông báo cá nhân hoặc toàn bộ danh sách thông báo.

## 2. Kiến trúc & Công nghệ

- **Backend:** Java 17, Spring Boot 3.1, Spring Data JPA, SQL Server (T-SQL).
- **Frontend:** Thymeleaf template engine, CSS thuần, Javascript thuần (`authFetch` truyền JWT Token qua header).
- **Tuân thủ:** Controller → Service → Repository → Entity; DTO Pattern (đóng gói Request/Response sạch sẽ).

## 3. Các thành phần Backend

### 3.1. Database Migration & Entities

- **Entity `Notification`** (bảng `Notifications`):
  - `userId` (BIGINT): ID người nhận/người tạo.
  - `title`, `content` (NVARCHAR): Tiêu đề và nội dung thông báo.
  - `type` (VARCHAR): Loại thông báo cá nhân (`SYSTEM`, `ORDER`, `WALLET`, `KYC`, `SECURITY`, `COMPLAINT`) hoặc broadcast (`info`, `warning`, `maintenance`, `policy`).
  - `severity` (VARCHAR): Mức độ cảnh báo (`INFO`, `WARNING`, `DANGER`, `SUCCESS`).
  - `isRead` (BIT): Trạng thái đã đọc (chỉ áp dụng với thông báo cá nhân).
  - `targetUrl` (VARCHAR): Đường dẫn điều hướng người dùng khi click.
  - `isDelete` (BIT): Đánh dấu xóa mềm.

### 3.2. Repositories (Spring Data JPA)

- **`NotificationRepository`**:
  - `findAllByUserIdAndIsDeleteFalseOrderByCreatedAtDesc(userId)`: Lấy các thông báo cá nhân của người dùng.
  - `findAllBroadcastNotifications()`: Lấy các thông báo Global/Broadcast phát từ hệ thống (do Admin/Staff tạo và thuộc các loại 'info', 'warning', 'maintenance', 'policy').

### 3.3. Services (Business Logic)

- **`NotificationService`**:
  - `getUserNotifications(userId)`: Trả về danh sách kết hợp gồm thông báo cá nhân và thông báo Broadcast hệ thống, sắp xếp theo thời gian mới nhất.
  - `markAsRead(userId, notifId)`: Đánh dấu đã đọc thông báo cá nhân thuộc sở hữu của người dùng hiện tại.
  - `markAllAsRead(userId)`: Đánh dấu đã đọc toàn bộ thông báo cá nhân chưa đọc của người dùng.

### 3.4. Controllers & Security

- **`NotificationController`**:
  - Các API của người dùng đã đăng nhập:
    - `GET /api/v1/notifications`: Lấy danh sách thông báo cá nhân và global.
    - `POST /api/v1/notifications/{id}/read`: Đọc 1 thông báo cá nhân.
    - `POST /api/v1/notifications/mark-all-read`: Đọc tất cả thông báo cá nhân.

---

## 4. Các thành phần Frontend

- **Trang danh sách thông báo của người dùng:**
  - File: `templates/account/notifications.html` và file JS `static/js/customer/account-notifications.js`.
  - Hiển thị danh sách thông báo dạng feed, phân biệt màu sắc/icon theo `severity` (đỏ với bảo trì, vàng với cảnh báo, xanh/xám với thông báo thường).
  - Có nút "Đánh dấu tất cả đã đọc".
- **Biểu tượng chuông trên Header:**
  - File: `templates/fragments/header.html`.
  - Hiển thị badge số thông báo chưa đọc, tự động gọi hàm `refreshHeaderNotifBadge()` để cập nhật số liệu.

---

## 5. Definition of Done

- API truy vấn và thao tác trên thông báo cá nhân phải kiểm tra quyền sở hữu (`userId` tương ứng với tài khoản đăng nhập) tránh ID harvesting.
- Badge đếm số thông báo trên header hoạt động nhất quán giữa các trang.