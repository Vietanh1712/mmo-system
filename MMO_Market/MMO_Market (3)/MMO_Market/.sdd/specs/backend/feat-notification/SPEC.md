# SPEC — Notifications & Maintenance Mode
> **Feature ID:** `feat-notification`
> **Version:** 1.0 | **Status:** Active
> **Author:** Team | **Last Updated:** 2026-06-27

---

## 1. CONTEXT & GOAL (BỐI CẢNH & MỤC TIÊU)
Gửi thông báo biến động tài khoản và thông tin đơn hàng tới người dùng, quản trị chế độ bảo trì hệ thống.

---

## 5. DATA MODEL (Mô hình dữ liệu)

```sql
CREATE TABLE Notifications (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NULL, -- NULL nghĩa là thông báo Global toàn hệ thống
    message NVARCHAR(MAX) NOT NULL,
    is_read BIT DEFAULT 0,
    is_global BIT DEFAULT 0,
    created_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_Noti_User FOREIGN KEY(user_id) REFERENCES Users(id)
);
```

---

## 6. API SPEC (Đặc tả API)

### `GET /api/v1/notifications`
*   **Response (200 OK):** Danh sách thông báo.