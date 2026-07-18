# SPEC — Product Catalog & Reviews Management
> **Feature ID:** `feat-product`
> **UC Coverage:** UC-05 (Product Discovery), UC-06 (Shop Product Management), UC-11 (Feedback & Reviews)
> **Version:** 1.0 | **Status:** Active
> **Author:** Team | **Last Updated:** 2026-06-27

---

## 1. CONTEXT & GOAL (BỐI CẢNH & MỤC TIÊU)

### 1.1 Bối cảnh
Người bán cần đăng bán các tài nguyên số (account, key game, giftcode), trong khi người mua muốn duyệt danh mục, tìm kiếm lọc sản phẩm, theo dõi Shop bán hàng và đánh giá chất lượng sản phẩm sau khi mua.

### 1.2 Mục tiêu
- CRUD sản phẩm, biến thể sản phẩm (Variants) và danh mục (Categories).
- Bảo mật tài sản kỹ thuật số bằng cách mã hóa nội dung nhạy cảm của tệp tin/mã code bán trước khi lưu trữ vào SQL Server.
- Cho phép khách hàng tìm kiếm phân trang nâng cao và để lại đánh giá đánh giá.

---

## 2. ACTOR (TÁC NHÂN)
| Actor | Role | Điều kiện tiền quyết |
|---|---|---|
| **Guest** | Khách xem hàng | Không |
| **Customer** | Người mua hàng | Đã đăng nhập, đã mua thành công sản phẩm để viết review |
| **Seller** | Người bán hàng | Đã có Shop hoạt động |

---

## 3. FUNCTIONAL REQUIREMENTS (Cú pháp EARS)

### 3.1 Đăng bán sản phẩm (Digital Assets Encryption)
| ID | EARS Requirement |
|:---|:---|
| FR-PROD-01 | WHEN a Seller uploads digital asset codes, THE SYSTEM SHALL encrypt the asset content using AES-256 before storing it. |
| FR-PROD-02 | THE SYSTEM SHALL prevent non-owner sellers from viewing or modifying a product's digital assets. |

### 3.2 Khám phá sản phẩm & Follow Shop
| ID | EARS Requirement |
|:---|:---|
| FR-PROD-03 | WHEN a Guest searches products, THE SYSTEM SHALL return active products (`isDelete = 0`) matching keywords and category filters. |
| FR-PROD-04 | WHEN a Customer follows a Seller, THE SYSTEM SHALL check if a follow record exists; IF it exists, THE SYSTEM SHALL toggle `isDelete = 0` (soft-deleted recovery). |

---

## 5. DATA MODEL (Mô hình dữ liệu)

```sql
CREATE TABLE Categories (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(255) NOT NULL,
    description NVARCHAR(MAX) NULL,
    isDelete BIT DEFAULT 0
);

CREATE TABLE Products (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    seller_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    name NVARCHAR(255) NOT NULL,
    description NVARCHAR(MAX) NULL,
    price BIGINT NOT NULL,
    isDelete BIT DEFAULT 0,
    CONSTRAINT FK_Prod_Seller FOREIGN KEY(seller_id) REFERENCES Users(id),
    CONSTRAINT FK_Prod_Cat FOREIGN KEY(category_id) REFERENCES Categories(id)
);

CREATE TABLE ProductVariants (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    product_id BIGINT NOT NULL,
    name NVARCHAR(255) NOT NULL,
    price BIGINT NOT NULL,
    stock INT DEFAULT 0,
    isDelete BIT DEFAULT 0,
    CONSTRAINT FK_Var_Prod FOREIGN KEY(product_id) REFERENCES Products(id)
);

CREATE TABLE Reviews (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    transaction_id BIGINT NULL,
    product_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    rating INT CHECK(rating BETWEEN 1 AND 5),
    comment NVARCHAR(MAX) NULL,
    media_url NVARCHAR(500) NULL,
    created_at DATETIME DEFAULT GETDATE(),
    isDelete BIT DEFAULT 0,
    CONSTRAINT FK_Rev_Prod FOREIGN KEY(product_id) REFERENCES Products(id),
    CONSTRAINT FK_Rev_User FOREIGN KEY(user_id) REFERENCES Users(id)
);
```

---

## 6. API SPEC (Đặc tả API)

### `GET /api/search/products`
*   **Params**: `query`, `categoryId`, `minPrice`, `maxPrice`, `page`, `size`
*   **Response (200 OK):**
    ```json
    {
      "content": [
        {
          "id": 1,
          "name": "Tài khoản Netflix Premium 1 tháng",
          "price": 50000,
          "sellerName": "Netflix Shop",
          "rating": 4.8
        }
      ],
      "totalElements": 1
    }
    ```

### `POST /api/seller/products`
*   **Headers:** `Authorization: Bearer <Seller_JWT>`
*   **Request Body:**
    ```json
    {
      "name": "Netflix Giftcode 100k",
      "categoryId": 2,
      "price": 95000,
      "description": "Mã giftcode Netflix chính hãng"
    }
    ```
*   **Response (200 OK):** Product details.