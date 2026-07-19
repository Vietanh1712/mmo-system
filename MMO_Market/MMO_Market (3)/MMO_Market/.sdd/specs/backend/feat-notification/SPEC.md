# SPEC — Account Notifications
> **Feature ID:** `feat-notification`
> **Version:** 2.0 | **Status:** Active
> **Author:** Team | **Last Updated:** 2026-07-16

---

## 1. CONTEXT & GOAL (BỐI CẢNH & MỤC TIÊU)

### 1.1 Bối cảnh
Người dùng cần được cập nhật tức thời khi có các biến động số dư ví (nạp tiền, rút tiền, thanh toán), thay đổi trạng thái đơn hàng (đã thanh toán, đã hoàn thành), trạng thái kiểm duyệt (KYC, đăng ký bán hàng) và các chính sách, cảnh báo từ hệ thống.

### 1.2 Mục tiêu
- Cung cấp danh sách thông báo cá nhân chi tiết (đơn hàng, ví, kyc, bảo mật) và các thông báo broadcast từ hệ thống.
- Hỗ trợ đánh dấu đã đọc (`isRead = 1`) cho từng thông báo hoặc toàn bộ danh sách thông báo.
- Hiển thị badge số đếm thông báo chưa đọc trên biểu tượng Chuông ở header.

---

## 5. DATA MODEL (Mô hình dữ liệu)

```sql
CREATE TABLE Notifications (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL, -- ID người nhận thông báo cá nhân hoặc người tạo (Admin/Staff đối với tin broadcast)
    title NVARCHAR(255) NOT NULL,
    content NVARCHAR(MAX) NULL,
    type VARCHAR(50) NOT NULL, -- info, warning, maintenance, policy, SYSTEM, ORDER, WALLET, KYC, SECURITY, COMPLAINT
    severity VARCHAR(50) NOT NULL DEFAULT 'INFO', -- INFO, WARNING, DANGER, SUCCESS
    target_url VARCHAR(500) NULL, -- URL liên kết điều hướng khi click
    isRead BIT DEFAULT 0, -- trạng thái đã đọc của user
    isDelete BIT DEFAULT 0, -- soft delete flag
    created_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_Notif_Users FOREIGN KEY(user_id) REFERENCES Users(id)
);
```

---

## 6. API SPEC (Đặc tả API)

### `GET /api/v1/notifications`
*   **Description**: Lấy danh sách thông báo của người dùng hiện tại (bao gồm các thông báo cá nhân của user và các thông báo hệ thống/broadcast từ Admin/Staff).
*   **Headers:** `Authorization: Bearer <User_JWT_Token>`
*   **Response (200 OK):**
    ```json
    [
      {
        "id": "12",
        "type": "WALLET",
        "title": "Nạp tiền thành công",
        "message": "Số dư ví khả dụng của bạn đã được cộng 100,000 VNĐ.",
        "status": "UNREAD",
        "severity": "SUCCESS",
        "createdAt": "21:30:15 16/07/2026",
        "targetUrl": "/wallet/transactions",
        "isBroadcast": false
      },
      {
        "id": "SYS-2026-07-16T12:00:00Z",
        "type": "SYSTEM",
        "title": "Bảo trì hệ thống định kỳ",
        "message": "Hệ thống sẽ bảo trì nâng cấp từ 2h đến 4h sáng mai.",
        "status": "UNREAD",
        "severity": "DANGER",
        "createdAt": "12:00:00 16/07/2026",
        "targetUrl": "/account/notifications",
        "isBroadcast": true
      }
    ]
    ```

### `POST /api/v1/notifications/{id}/read`
*   **Description**: Đánh dấu đã đọc cho một thông báo cá nhân cụ thể.
*   **Headers:** `Authorization: Bearer <User_JWT_Token>`
*   **Response (200 OK):**
    ```json
    {
      "success": true,
      "message": "Đã đánh dấu thông báo là đã đọc."
    }
    ```

### `POST /api/v1/notifications/mark-all-read`
*   **Description**: Đánh dấu đã đọc cho tất cả thông báo cá nhân hiện có của người dùng.
*   **Headers:** `Authorization: Bearer <User_JWT_Token>`
*   **Response (200 OK):**
    ```json
    {
      "success": true,
      "message": "Đã đánh dấu tất cả thông báo là đã đọc."
    }
    ```

---

## 7. ERROR HANDLING (Xử lý lỗi)
| HTTP Code | Error Code | Message | Lý do kích hoạt |
|---|---|---|---|
| 403 | FORBIDDEN | "Không có quyền thực hiện thao tác này." | Đánh dấu đọc thông báo của tài khoản khác |
| 404 | NOT_FOUND | "Không tìm thấy thông báo." | Gửi ID thông báo không tồn tại |