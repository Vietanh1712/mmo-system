# SPEC — Staff Operations & Approvals
> **Feature ID:** `feat-staff`
> **Version:** 1.0 | **Status:** Active
> **Author:** Team | **Last Updated:** 2026-06-27

---

## 1. CONTEXT & GOAL (BỐI CẢNH & MỤC TIÊU)
Hỗ trợ Staff kiểm duyệt KYC, phê duyệt lệnh rút tiền ngân hàng của người bán, và đặt cờ cảnh báo Shop vi phạm.

---

## 5. DATA MODEL (Mô hình dữ liệu)

```sql
CREATE TABLE ShopFlags (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    shop_id BIGINT NOT NULL,
    reason NVARCHAR(MAX) NOT NULL,
    created_by BIGINT NOT NULL,
    created_at DATETIME DEFAULT GETDATE(),
    isDelete BIT DEFAULT 0,
    CONSTRAINT FK_Flag_Shop FOREIGN KEY(shop_id) REFERENCES Users(id),
    CONSTRAINT FK_Flag_Creator FOREIGN KEY(created_by) REFERENCES Users(id)
);
```

---

## 6. API SPEC (Đặc tả API)

### `POST /api/staff/kyc/approve/{kycId}`
*   **Response (200 OK):** Thành công.

### `POST /api/staff/withdrawals/approve/{withdrawalId}`
*   **Response (200 OK):** Phê duyệt lệnh rút tiền và giải phóng hold balance.