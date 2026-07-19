# SPEC-07 — Account Notifications
> **Module:** Notification
> **Version:** 1.0 | **Status:** Active
> **Last Updated:** 2026-07-16

---

## 1. Context and Goal
Người dùng hệ thống (Customers, Sellers) cần một cách thức tập trung để nhận và quản lý các thông báo liên quan trực tiếp đến tài khoản của họ (biến động số dư ví, nạp/rút tiền, KYC, đơn hàng đã mua/bán, tin nhắn mới, hoặc thông tin bảo trì hệ thống từ Admin).

---

## 2. Actors
- **User (Customer / Seller)**: Nhận thông báo cá nhân, đánh dấu đã đọc hoặc đánh dấu đọc tất cả.
- **System**: Tự động tạo và gửi thông báo khi có các sự kiện nghiệp vụ xảy ra.

---

## 3. Functional Requirements
- **FR-NOTI-01**: THE SYSTEM SHALL automatically generate personal notifications for critical business events (e.g., successful top-ups, withdrawal status changes, KYC review results, order payments, complaint updates).
- **FR-NOTI-02**: THE SYSTEM SHALL support fetching a unified list of personal notifications and active system broadcast announcements for the logged-in user.
- **FR-NOTI-03**: WHEN a user reads a notification, THE SYSTEM SHALL support marking it as read (`isRead = 1`).
- **FR-NOTI-04**: THE SYSTEM SHALL allow the user to mark all their personal notifications as read in a single action.
- **FR-NOTI-05**: THE SYSTEM SHALL support soft-deleting notifications (`isDelete = 1`) when requested.

---

## 4. Non-Functional Requirements
- **Real-time Alerting**: Khi có thông báo mới, badge số đếm chưa đọc trên biểu tượng Chuông ở header phải được cập nhật ngay lập tức (thời gian thực qua SSE/WebSocket hoặc kéo định kỳ).
- **Security**: Người dùng chỉ có quyền xem và thao tác trên thông báo của chính tài khoản mình.

---

## 5. Data Model

```sql
CREATE TABLE Notifications (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title NVARCHAR(255) NOT NULL,
    content NVARCHAR(MAX) NULL,
    type VARCHAR(50) NOT NULL, -- info, warning, maintenance, policy, SYSTEM, ORDER, WALLET, KYC, SECURITY, COMPLAINT
    severity VARCHAR(50) NOT NULL DEFAULT 'INFO', -- INFO, WARNING, DANGER, SUCCESS
    target_url VARCHAR(500) NULL,
    isRead BIT DEFAULT 0,
    isDelete BIT DEFAULT 0,
    created_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_Notif_Users FOREIGN KEY(user_id) REFERENCES Users(id)
);
```

---

## 6. API Specification
- `GET /api/v1/notifications` -> 200 OK (Get merged list of notifications)
- `POST /api/v1/notifications/{id}/read` -> 200 OK (Mark notification as read)
- `POST /api/v1/notifications/mark-all-read` -> 200 OK (Mark all as read)

---

## 7. Error Handling
- `403 Forbidden`: Người dùng cố tình thao tác (đọc/xóa) thông báo của tài khoản khác.
- `404 Not Found`: Truy cập thông báo không tồn tại hoặc đã bị xóa.

---

## 8. Acceptance Criteria & Out of Scope
### Acceptance Criteria
- **AC-01**: Given a user has 3 unread notifications, when they click "Mark all as read", then all their notifications show as read and the header badge updates to 0.
- **AC-02**: Given a user receives a KYC rejection notice, when they view it, the notification has severity `DANGER` and clicking it redirects them to `/account/kyc`.

### Out of Scope
- Gửi thông báo qua kênh ngoài hệ thống (SMS, Email, Firebase FCM trên thiết bị di động).
