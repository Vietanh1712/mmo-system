# SPEC-04 — Marketplace & Customer Operations
> **Module:** Customer / Purchase
> **Version:** 1.0 | **Status:** Active

---

## 1. Context and Goal
Customer sử dụng Marketplace để tìm kiếm sản phẩm số, xem đánh giá, và thực hiện mua hàng tự động. Hệ thống phải đảm bảo giao dịch tức thì (giao tài sản số ngay khi thanh toán), đồng thời cung cấp chức năng khiếu nại (Complaint) nếu tài sản số bị lỗi.

---

## 2. Actors
- **Guest**: Xem danh sách sản phẩm.
- **Customer**: Mua sản phẩm, đánh giá, tạo khiếu nại.
- **System**: Tự động giao hàng (phân bổ DigitalAsset cho đơn hàng).

---

## 3. Functional Requirements
- **FR-CUST-01**: WHEN a Customer views the marketplace, THE SYSTEM SHALL display active products with pagination and category filters.
- **FR-CUST-02**: WHEN a Customer submits a purchase request, THE SYSTEM SHALL check wallet balance, deduct the amount, and create an Order.
- **FR-CUST-02a**: IF the requested purchase quantity exceeds the available stock:
  - THE SYSTEM SHALL present a choices dialog offering:
    - **Option A (Buy remaining stock)**: Purchase only the available `stock` quantity immediately.
    - **Option B (Buy remaining + Pre-order deficit)**: Purchase `stock` immediately AND submit a Pre-Order request for the deficit `(quantity - stock)`.
    - **Option C (Pre-order full quantity)**: Transfer the entire `quantity` into a Pre-Order request.
    - **Cancel**: Close the modal without performing any transactions.
- **FR-CUST-03**: AFTER an Order is created, THE SYSTEM SHALL assign exactly the purchased quantity of unused `DigitalAsset`s to the order item and mark them as used.
- **FR-CUST-04**: IF the digital asset is invalid, THE SYSTEM SHALL allow the Customer to open a Complaint within 3 days (Escrow period).
- **FR-CUST-05**: WHEN an Order is successfully completed without complaint, THE SYSTEM SHALL allow the Customer to leave a 1 to 5 star Review.

---

## 4. Non-Functional Requirements
- **Concurrency**: Khi nhiều người cùng mua 1 tài sản cuối cùng, hệ thống phải xử lý khóa dòng để chỉ 1 người mua thành công (Pessimistic Locking / Optimistic Locking).
- **Performance**: Việc tìm kiếm tài sản số chưa sử dụng để giao cho khách phải có độ trễ < 100ms.
- **UX/Responsiveness**: Trải nghiệm chọn lựa mua/pre-order khi kho hàng thiếu hụt phải được thông báo trực quan qua Dialog/Modal không gây gián đoạn khó hiểu cho người dùng.

---

## 5. Data Model
```sql
CREATE TABLE Orders (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    total_amount BIGINT NOT NULL,
    status VARCHAR(50), -- COMPLETED, CANCELLED, REFUNDED, DISPUTED
    created_at DATETIME DEFAULT GETDATE()
);

CREATE TABLE OrderItems (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    order_id BIGINT NOT NULL,
    variant_id BIGINT NOT NULL,
    asset_id BIGINT NOT NULL, -- The specific DigitalAsset delivered
    price_vnd BIGINT NOT NULL
);
```

---

## 6. API Specification
- `GET /api/public/products` (Params: categoryId, search, page, size) -> 200 OK
- `POST /api/orders` (Body: variantId) -> 200 OK
- `POST /api/complaints` (Body: orderId, reason) -> 200 OK
- `POST /api/reviews` (Body: productId, rating, comment) -> 200 OK

---

## 7. Error Handling
- `400 Bad Request`: "Sản phẩm này đã hết hàng."
- `400 Bad Request`: "Số dư của bạn không đủ để mua sản phẩm này."
- `403 Forbidden`: "Bạn không thể đánh giá sản phẩm chưa mua."

---

## 8. Acceptance Criteria & Out of Scope
### Acceptance Criteria
- **AC-01**: Given an item with 1 stock, when two customers try to buy at the exact same millisecond, then only one succeeds and the other receives an "Out of stock" error.
- **AC-02**: Given a delivered order, when the customer views the order details, then the system displays the username/password or license key of the digital asset.

### Out of Scope
- Giỏ hàng (Shopping Cart): Ở phiên bản này, khách hàng mua trực tiếp từng sản phẩm (Buy Now) chứ không dùng giỏ hàng nhiều món để tránh phức tạp việc hold stock.
