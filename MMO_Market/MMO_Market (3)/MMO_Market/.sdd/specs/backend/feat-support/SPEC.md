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
    subject NVARCHAR(255) NOT NULL,
    message NVARCHAR(MAX) NOT NULL,
    status VARCHAR(50) DEFAULT 'OPEN', -- OPEN, RESOLVED, CLOSED
    created_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_ST_User FOREIGN KEY (user_id) REFERENCES Users(id)
);
```

---

## 6. API SPEC (Đặc tả API)

### `POST /api/support-tickets`
*   **Request Body (JSON):**
    ```json
    {
      "subject": "Không nhận được OTP email",
      "message": "Tôi đăng ký tài khoản từ 10 phút trước nhưng chưa nhận được mã."
    }
    ```
*   **Response (200 OK):** Thành công.

---

## 7. BẢN ĐỊA HÓA (LOCALIZATION)
*   Việt hóa toàn bộ thuật ngữ hiển thị trên giao diện liên quan đến "Ticket" / "Ticket hỗ trợ" thành "Phiếu hỗ trợ" (ví dụ: "Ticket Hỗ Trợ" -> "Phiếu Hỗ Trợ", "Ticket của tôi" -> "Phiếu hỗ trợ của tôi", "Mã Ticket" -> "Mã Phiếu", v.v.) để tăng tính thân thiện và dễ hiểu đối với người dùng Việt Nam.