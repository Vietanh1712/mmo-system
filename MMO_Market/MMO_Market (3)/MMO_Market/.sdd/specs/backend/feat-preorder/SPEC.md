# SPEC — Pre-Orders
> **Feature ID:** `feat-preorder`
> **UC Coverage:** UC-16 (Pre-order)
> **Version:** 2.0 | **Status:** Active
> **Author:** Team | **Last Updated:** 2026-07-31

---

## 1. CONTEXT & GOAL (BỐI CẢNH & MỤC TIÊU)
Cho phép khách hàng gửi yêu cầu đặt hàng trước (Pre-order / Hộp thư đăng ký nhu cầu) đối với các sản phẩm số đang tạm thời hết hàng hoặc chưa có sẵn trên sàn. Qua đó, Seller tiếp nhận nhu cầu đặt mua (số lượng, tổng giá kỳ vọng, ghi chú) để chủ động bổ sung kho hàng, tối ưu hóa doanh thu khi hết hàng tạm thời.

---

## 2. ACTORS (TÁC NHÂN)
*   **Customer**: Người mua cần đặt trước sản phẩm số.
*   **Seller**: Người bán nhận thông tin đặt trước và giao hàng.
*   **System**: Hệ thống tự động phân bổ tài sản số khi Seller cập nhật kho.

---

## 3. FUNCTIONAL REQUIREMENTS (Yêu cầu chức năng)
| ID | EARS Requirement |
|---|---|
| FR-PRE-01 | WHEN a Customer creates a Pre-order, THE SYSTEM SHALL verify that the customer has `balanceVnd >= expectedPriceVnd`. |
| FR-PRE-02 | WHEN a Customer creates a Pre-order, THE SYSTEM SHALL deduct `expectedPriceVnd` from the customer's wallet balance and record a `"PREORDER"` ledger. |
| FR-PRE-03 | WHEN a Customer cancels a pending Pre-order, THE SYSTEM SHALL refund `expectedPriceVnd` back to their wallet balance, update status to `"CANCELLED"`, and record a `"REFUND"` ledger. |
| FR-PRE-04 | WHEN a Pre-order is fulfilled (manually or auto-completed), THE SYSTEM SHALL instantiate a real Transaction in `"Held"` (escrow) state, calculate dynamic hold hours based on seller level/disputes, and associate the delivered digital assets. |

---

## 4. BUSINESS RULES (Luật nghiệp vụ)
*   **BR-PRE-01**: Số dư khả dụng của khách hàng bắt buộc phải lớn hơn hoặc bằng tổng giá trị đặt trước khi gửi yêu cầu.
*   **BR-PRE-02**: Hủy đơn đặt trước chỉ được chấp nhận đối với các đơn ở trạng thái `"Pending"` hoặc `"Chờ xử lý"`.
*   **BR-PRE-03**: Khi bàn giao hàng cho đơn Pre-order, người bán chưa nhận được tiền khả dụng ngay mà tiền phải được chuyển vào ví đóng băng bảo lãnh (Escrow) theo quy tắc giam tiền 3 ngày / 7 ngày.

---

## 5. DATA MODEL (Mô hình dữ liệu)

Cấu trúc bảng `PreOrders` được triển khai trong cơ sở dữ liệu SQL Server:

```sql
CREATE TABLE PreOrders (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    variant_id BIGINT,
    expected_price_vnd BIGINT NOT NULL,
    quantity INT DEFAULT 1,
    status VARCHAR(20) DEFAULT 'Pending',
    notes NVARCHAR(MAX),
    delivery_data NVARCHAR(MAX),
    proof_image VARCHAR(500),
    created_at DATETIME DEFAULT GETDATE(),
    isDelete BIT DEFAULT 0,
    CONSTRAINT FK_PreOrder_Customer FOREIGN KEY (customer_id) REFERENCES Users(id) ON DELETE NO ACTION,
    CONSTRAINT FK_PreOrder_Product FOREIGN KEY (product_id) REFERENCES Products(id) ON DELETE NO ACTION,
    CONSTRAINT FK_PreOrder_Variant FOREIGN KEY (variant_id) REFERENCES ProductVariants(id) ON DELETE NO ACTION
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
      "variantId": 3,
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

### 6.3. Hủy đơn đặt trước và hoàn tiền
*   **Endpoint:** `PUT /api/v1/pre-orders/{id}/cancel`
*   **Headers:** `Authorization: Bearer <Access_Token>`
*   **Response (200 OK):**
    ```json
    {
      "success": true,
      "message": "Hủy đơn đặt trước và hoàn tiền thành công.",
      "id": 1,
      "status": "CANCELLED"
    }
    ```