# SPEC-03 — Seller Console & Shop Management
> **Module:** Seller Operations
> **Version:** 1.0 | **Status:** Active

---

## 1. Context and Goal
Seller Console là trung tâm quản lý dành cho người bán (Seller) trên MMO Market. Mục tiêu là cho phép Seller đăng ký mở shop, đăng bán sản phẩm số (Product), quản lý các phân loại (Variant), và kho tài nguyên (Digital Assets), cũng như theo dõi giao dịch và doanh thu.

---

## 2. Actors
- **Customer**: Đăng ký trở thành Seller (yêu cầu đã KYC).
- **Seller**: Quản lý cửa hàng, đăng sản phẩm, trả lời khiếu nại.
- **Staff**: Duyệt hồ sơ mở shop của Customer.

---

## 3. Functional Requirements
- **FR-SELL-01**: WHEN a Customer submits a Shop Registration, THE SYSTEM SHALL save it as APPROVED automatically if KYC is verified.
- **FR-SELL-02**: WHEN the registration is created, THE SYSTEM SHALL change the user's role to SELLER and shop status to Active.
- **FR-SELL-03**: WHILE user is a Seller, THE SYSTEM SHALL allow full CRUD access to their own Products and Product Variants.
- **FR-SELL-04**: WHEN a Seller creates a Product, THE SYSTEM SHALL require at least 1 Variant with `priceVnd` > 0.
- **FR-SELL-05**: WHEN a Seller uploads DigitalAssets (batch) for a Variant, THE SYSTEM SHALL update the variant's `stock` automatically.
- **FR-SELL-06**: WHEN a Seller views their dashboard, THE SYSTEM SHALL return total revenue, completed sales, and active products.

---

## 4. Non-Functional Requirements
- **Security**: Mọi thao tác ghi/đọc tài sản của Seller phải có cơ chế Ownership check (Id của Seller = userId đang đăng nhập).
- **Usability**: Quá trình upload hàng loạt tài sản số (Account, Key) phải hỗ trợ dán nhiều dòng (bulk import).

---

## 5. Data Model
```sql
CREATE TABLE Products (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    seller_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    name NVARCHAR(255) NOT NULL,
    product_type VARCHAR(50), -- ACCOUNT, KEY, GAME_CARD
    isDelete BIT DEFAULT 0,
    created_at DATETIME DEFAULT GETDATE()
);

CREATE TABLE ProductVariants (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    product_id BIGINT NOT NULL,
    variant_name NVARCHAR(255) NOT NULL,
    price_vnd BIGINT NOT NULL,
    stock INT DEFAULT 0,
    isDelete BIT DEFAULT 0
);
```

---

## 6. API Specification
- `POST /api/seller/shop-registrations` -> 200 OK
- `POST /api/seller/products` (Body: name, variants[]) -> 200 OK
- `POST /api/seller/digital-assets` (Body: variantId, assets[]) -> 200 OK
- `GET /api/seller/dashboard` -> 200 OK

---

## 7. Error Handling
- `403 Forbidden`: "Bạn không có quyền thao tác trên sản phẩm này."
- `400 Bad Request`: "Sản phẩm phải có ít nhất 1 phân loại (Variant)."

---

## 8. Acceptance Criteria & Out of Scope
### Acceptance Criteria
- **AC-01**: Given a Seller, when they upload 5 accounts to a Variant, then the Variant's stock increases by 5.
- **AC-02**: Given a Customer without KYC, when they try to register a shop, then the system returns a 400 error.

### Out of Scope
- Các sản phẩm vật lý (giao hàng) không được hỗ trợ. Chỉ hỗ trợ sản phẩm số.
