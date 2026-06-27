# SPEC — Pre-Orders
> **Feature ID:** `feat-preorder`
> **UC Coverage:** UC-16 (Pre-order)
> **Version:** 1.0 | **Status:** Active
> **Author:** Team | **Last Updated:** 2026-06-27

---

## 1. CONTEXT & GOAL (BỐI CẢNH & MỤC TIÊU)
Cho phép khách hàng đặt mua các sản phẩm số chưa có sẵn hoặc hết hàng và nhận hàng ngay khi người bán cập nhật hàng mới.

---

## 5. DATA MODEL (Mô hình dữ liệu)

```sql
CREATE TABLE PreOrders (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING_RELEASE',
    created_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_PO_User FOREIGN KEY(user_id) REFERENCES Users(id),
    CONSTRAINT FK_PO_Prod FOREIGN KEY(product_id) REFERENCES Products(id)
);
```

---

## 6. API SPEC (Đặc tả API)

### `POST /api/v1/pre-orders`
*   **Request Body (JSON):**
    ```json
    {
      "productId": 2
    }
    ```
*   **Response (200 OK):** Thành công.