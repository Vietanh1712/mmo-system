# SPEC — Support Ticket Management
> **Feature ID:** `feat-support`
> **Version:** 1.0 | **Status:** Active
> **Author:** Team | **Last Updated:** 2026-06-27

---

## 1. CONTEXT & GOAL (BỐI CẢNH & MỤC TIÊU)
Hỗ trợ người dùng gửi yêu cầu trợ giúp kỹ thuật hoặc giao dịch đến hệ thống.

---

## 5. DATA MODEL (Mô hình dữ liệu)

```sql
CREATE TABLE SupportTickets (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    category VARCHAR(50) NOT NULL, -- TECHNICAL, TRANSACTION, ACCOUNT, OTHER
    title NVARCHAR(255) NOT NULL,
    description NVARCHAR(MAX) NOT NULL,
    status VARCHAR(50) DEFAULT 'Open', -- Open, Processing, Resolved
    resolution NVARCHAR(MAX) NULL,
    isDelete BIT DEFAULT 0,
    created_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_ST_User FOREIGN KEY (user_id) REFERENCES Users(id)
);
```

---

## 6. API SPEC (Đặc tả API)

### `POST /api/support-tickets`
*   **Description**: Người dùng gửi yêu cầu hỗ trợ mới (chỉ CUSTOMER, SELLER).
*   **Request Body (JSON):**
    ```json
    {
      "category": "TECHNICAL",
      "title": "Không nhận được OTP email",
      "description": "Tôi đăng ký tài khoản từ 10 phút trước nhưng chưa nhận được mã."
    }
    ```
*   **Response (200 OK):** Thông tin ticket đã tạo.

### `GET /api/support-tickets`
*   **Description**: Người dùng xem danh sách ticket cá nhân.
*   **Response (200 OK):** Danh sách ticket.

### `GET /api/support-tickets/all`
*   **Description**: Staff/Admin xem danh sách ticket toàn hệ thống.
*   **Response (200 OK):** Danh sách tất cả ticket.

### `GET /api/support-tickets/{id}`
*   **Description**: Xem chi tiết ticket (Chủ sở hữu hoặc Staff/Admin).
*   **Response (200 OK):** Chi tiết ticket.

### `PUT /api/support-tickets/{id}/status`
*   **Description**: Staff/Admin cập nhật trạng thái/phản hồi giải pháp cho ticket.
*   **Request Body (JSON):**
    ```json
    {
      "status": "Resolved",
      "resolution": "Đã cấu hình lại dịch vụ SMTP, email đã được gửi thành công."
    }
    ```
*   **Response (200 OK):** Thông tin ticket sau cập nhật.

---

## 7. BẢN ĐỊA HÓA (LOCALIZATION)
*   Việt hóa toàn bộ thuật ngữ hiển thị trên giao diện liên quan đến "Ticket" / "Ticket hỗ trợ" thành "Phiếu hỗ trợ" (ví dụ: "Ticket Hỗ Trợ" -> "Phiếu Hỗ Trợ", "Ticket của tôi" -> "Phiếu hỗ trợ của tôi", "Mã Ticket" -> "Mã Phiếu", v.v.) để tăng tính thân thiện và dễ hiểu đối với người dùng Việt Nam.

