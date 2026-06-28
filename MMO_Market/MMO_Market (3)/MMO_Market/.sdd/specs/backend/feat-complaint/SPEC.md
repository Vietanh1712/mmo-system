# SPEC — Complaint Management
> **Feature ID:** `feat-complaint`
> **UC Coverage:** UC-10 (Complaints)
> **Version:** 1.0 | **Status:** Active
> **Author:** Team | **Last Updated:** 2026-06-27

---

## 1. CONTEXT & GOAL (BỐI CẢNH & MỤC TIÊU)
Hỗ trợ người mua tạo khiếu nại đối với đơn hàng gặp lỗi trong thời hạn 72h escrow để hệ thống đóng băng tiền và chờ Staff xử lý.

---

## 5. DATA MODEL (Mô hình dữ liệu)

```sql
CREATE TABLE Complaints (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    transaction_id BIGINT NOT NULL,
    title NVARCHAR(255) NOT NULL,
    description NVARCHAR(MAX) NOT NULL,
    status VARCHAR(50) DEFAULT 'OPEN', -- OPEN, RESOLVED, CLOSED
    resolution NVARCHAR(MAX) NULL,
    resolved_by BIGINT NULL,
    resolved_at DATETIME NULL,
    created_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_Comp_Trans FOREIGN KEY (transaction_id) REFERENCES Transactions(id)
);
```

---

## 6. API SPEC (Đặc tả API)

### `POST /api/complaints`
*   **Request Body (JSON):**
    ```json
    {
      "transactionId": 12,
      "title": "Tài khoản bị sai mật khẩu",
      "description": "Tôi không đăng nhập được vào Netflix sau khi mua."
    }
    ```
*   **Response (200 OK):** Khiếu nại được tiếp nhận.