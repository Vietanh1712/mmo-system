# SPEC — Orders & Escrow Purchase
> **Feature ID:** `feat-order`
> **UC Coverage:** UC-08 (Order Purchase & Escrow)
> **Version:** 1.0 | **Status:** Active
> **Author:** Team | **Last Updated:** 2026-06-27

---

## 1. CONTEXT & GOAL (BỐI CẢNH & MỤC TIÊU)

### 1.1 Bối cảnh
Người mua sản phẩm số cần được đảm bảo sẽ nhận được hàng hoạt động đúng mô tả. Người bán cũng cần đảm bảo nhận được tiền khi bàn giao thành công. Sàn giao dịch đóng vai trò tạm giữ tiền (Escrow) để trung chuyển an toàn.

### 1.2 Mục tiêu
- Khi giao dịch được tạo, số tiền thanh toán của người mua được khóa vào quỹ tạm giữ Escrow.
- Hệ thống tự động trích xuất nội dung sản phẩm số (đã được mã hóa) bàn giao cho người mua.
- Tự động đóng băng tiền trong 72 giờ trước khi cộng số dư khả dụng cho ví của Seller.

---

## 2. ACTOR (TÁC NHÂN)
| Actor | Role | Điều kiện tiền quyết |
|---|---|---|
| **Customer** | Người mua sản phẩm | Số dư khả dụng lớn hơn hoặc bằng giá đơn hàng |

---

## 3. FUNCTIONAL REQUIREMENTS (Cú pháp EARS)
| ID | EARS Requirement |
|---|---|
| FR-ORD-01 | WHEN a Customer purchases a product variant, THE SYSTEM SHALL verify that available stock exists. |
| FR-ORD-02 | WHEN purchase is executed, THE SYSTEM SHALL subtract the amount from buyer's available balance and store it in Escrow. |
| FR-ORD-03 | THE SYSTEM SHALL set `escrow_release_date` dynamically: 168 hours (7 days) if the seller has warning Level 0, completed orders < 20 (successfully passed lock with no dispute), or dispute rate >= 2%; and 72 hours (3 days) standard otherwise. |
| FR-ORD-04 | WHEN the escrow release date is reached, THE SYSTEM SHALL run a background scheduled task every 60 seconds to automatically transfer the funds (minus commission) to the seller's available balance and mark the transaction as Completed. |
| FR-ORD-05 | THE SYSTEM SHALL NOT charge any buyer service fee (flat buyer fee is 0 VND) from the buyer's balance. |

---

## 5. DATA MODEL (Mô hình dữ liệu)

```sql
CREATE TABLE Transactions (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    buyer_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    variant_id BIGINT NOT NULL,
    amount BIGINT NOT NULL,
    status VARCHAR(50) DEFAULT 'Pending',
    payment_method VARCHAR(50) NULL,
    escrow_release_date DATETIME NOT NULL,
    created_at DATETIME DEFAULT GETDATE(),
    isDelete BIT DEFAULT 0,
    CONSTRAINT FK_Trans_Buyer FOREIGN KEY(buyer_id) REFERENCES Users(id),
    CONSTRAINT FK_Trans_Seller FOREIGN KEY(seller_id) REFERENCES Users(id)
);

CREATE TABLE DigitalAssets (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    variant_id BIGINT NOT NULL,
    transaction_id BIGINT NULL,
    asset_content VARCHAR(MAX) NOT NULL, -- encrypted
    is_sold BIT DEFAULT 0,
    created_at DATETIME DEFAULT GETDATE(),
    isDelete BIT DEFAULT 0,
    CONSTRAINT FK_Asset_Var FOREIGN KEY(variant_id) REFERENCES ProductVariants(id)
);
```

---

## 6. API SPEC (Đặc tả API)

### `POST /api/transactions/purchase`
*   **Request Body (JSON):**
    ```json
    {
      "productId": 3,
      "variantId": 6,
      "quantity": 1
    }
    ```
*   **Response (200 OK):**
    ```json
    {
      "transactionId": 142,
      "status": "Escrow",
      "amount": 95000,
      "escrowReleaseDate": "2026-06-30T08:00:00"
    }
    ```