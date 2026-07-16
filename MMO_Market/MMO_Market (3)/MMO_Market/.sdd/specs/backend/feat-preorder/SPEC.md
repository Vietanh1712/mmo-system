# SPEC — Pre-Orders
> **Feature ID:** `feat-preorder`
> **UC Coverage:** UC-16 (Pre-order)
> **Version:** 1.0 | **Status:** Active
> **Author:** Team | **Last Updated:** 2026-07-16

---

## 1. CONTEXT & GOAL (BỐI CẢNH & MỤC TIÊU)
Cho phép khách hàng gửi yêu cầu đặt hàng trước (Pre-order / Hộp thư đăng ký nhu cầu) đối với các sản phẩm số đang tạm thời hết hàng hoặc chưa có sẵn trên sàn. Qua đó, Seller tiếp nhận nhu cầu đặt mua (số lượng, tổng giá kỳ vọng, ghi chú) để chủ động bổ sung kho hàng, tối ưu hóa doanh thu khi hết hàng tạm thời.

---

## 5. DATA MODEL (Mô hình dữ liệu)

Cấu trúc bảng `PreOrders` được triển khai trong cơ sở dữ liệu SQL Server:

```sql
CREATE TABLE PreOrders (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    expected_price_vnd BIGINT NOT NULL,
    quantity INT DEFAULT 1,
    status VARCHAR(20) DEFAULT 'Pending',
    notes NVARCHAR(MAX),
    created_at DATETIME DEFAULT GETDATE(),
    isDelete BIT DEFAULT 0,
    CONSTRAINT FK_PreOrder_Customer FOREIGN KEY (customer_id) REFERENCES Users(id) ON DELETE NO ACTION,
    CONSTRAINT FK_PreOrder_Product FOREIGN KEY (product_id) REFERENCES Products(id) ON DELETE NO ACTION
);
```

---

## 6. API SPEC (Đặc tả API)

### 6.1. Gửi yêu cầu đặt mua trước
*   **Endpoint:** `POST /api/v1/pre-orders`
*   **Headers:** `Authorization: Bearer <Access_Token>`
*   **Request Body (JSON):**
    ```json
    {
      "productId": 2,
      "quantity": 1,
      "expectedPriceVnd": 150000,
      "notes": "Cần gấp tài khoản Premium trong ngày"
    }
    ```
*   **Response (201 Created):**
    ```json
    {
      "success": true,
      "message": "Đã gửi yêu cầu đặt trước thành công.",
      "id": 1,
      "productId": 2,
      "productName": "Tài khoản Netflix Premium",
      "quantity": 1,
      "expectedPriceVnd": 150000,
      "status": "Pending",
      "notes": "Cần gấp tài khoản Premium trong ngày",
      "createdAt": "2026-07-16 17:30:00"
    }
    ```

### 6.2. Xem danh sách yêu cầu đặt trước cá nhân của người mua
*   **Endpoint:** `GET /api/v1/pre-orders`
*   **Headers:** `Authorization: Bearer <Access_Token>`
*   **Response (200 OK):**
    ```json
    [
      {
        "success": true,
        "id": 1,
        "productId": 2,
        "productName": "Tài khoản Netflix Premium",
        "quantity": 1,
        "expectedPriceVnd": 150000,
        "status": "Pending",
        "notes": "Cần gấp tài khoản Premium trong ngày",
        "createdAt": "2026-07-16 17:30:00"
      }
    ]
    ```