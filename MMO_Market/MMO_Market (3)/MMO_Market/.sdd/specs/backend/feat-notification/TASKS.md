# TASKS — Account Notifications (`feat-notification`)

> **Feature ID:** `feat-notification`
> **Phiên bản:** 2.0 | **Cập nhật:** 2026-07-16

---

## Phase 1: Database & Entities

- [x] **1.1** Thiết kế bảng `Notifications` trong database hỗ trợ đầy đủ các trường `title`, `content`, `type`, `severity`, `target_url`, `isRead`, `isDelete`.

## Phase 2: Repositories

- [x] **2.1** `NotificationRepository` — Lấy danh sách thông báo cá nhân theo `userId` và query broadcast notifications từ Admin/Staff.

## Phase 3: Services (Business Logic)

- [x] **3.1** `NotificationService` — Thực hiện ghép dữ liệu cá nhân & broadcast, sắp xếp theo thời gian mới nhất (`getUserNotifications`).
- [x] **3.2** `NotificationService` — Thực hiện logic đánh dấu đã đọc (`markAsRead`) và đọc tất cả (`markAllAsRead`).

## Phase 4: Controllers & Security

- [x] **4.1** `NotificationController` — Cung cấp các API `/api/v1/notifications`, `/api/v1/notifications/{id}/read` và `/api/v1/notifications/mark-all-read` bảo vệ bằng JWT Token.

## Phase 5: Giao diện (Thymeleaf & JS)

- [x] **5.1** `header.html` — Hiển thị biểu tượng Chuông và tích hợp badge số thông báo chưa đọc tự động cập nhật.
- [x] **5.2** `account/notifications.html` — Dựng giao diện trang danh sách thông báo cá nhân, tích hợp bộ lọc, phân trang và modal xem chi tiết thông báo.
- [x] **5.3** `account-notifications.js` — Viết kịch bản xử lý sự kiện click đọc, gán class CSS theo severity, và gọi API đánh dấu đọc tất cả.