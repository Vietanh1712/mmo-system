# SPEC — Product Catalog & Reviews Management
> **Feature ID:** `feat-product`
> **UC Coverage:** UC-07, UC-08, UC-09, UC-23, UC-24
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

### 3.1 Đăng bán sản phẩm (Digital Assets Encryption & Shop Level Constraints)
| ID | EARS Requirement |
|:---|:---|
| FR-PROD-01 | WHEN a Seller uploads digital asset codes, THE SYSTEM SHALL encrypt the asset content using AES-256 before storing it. |
| FR-PROD-02 | THE SYSTEM SHALL prevent non-owner sellers from viewing or modifying a product's digital assets. |
| FR-PROD-03 | WHEN a Seller with Shop Level 1 or 0 has a negative wallet balance (`balanceVnd < 0`), THE SYSTEM SHALL block them from creating new products, creating new variants, or updating variant details. |
| FR-PROD-04 | WHEN a Seller is at Shop Level 1 (New Shop), THE SYSTEM SHALL restrict the price of any of their variants to a maximum of 200,000 VND. |
| FR-PROD-05 | WHEN a Seller is at Shop Level 0 (Warning), THE SYSTEM SHALL limit their active products display count to a maximum of 5 products. |

### 3.2 Khám phá sản phẩm & Follow Shop
| ID | EARS Requirement |
|:---|:---|
| FR-PROD-06 | WHEN a Guest searches or views products, THE SYSTEM SHALL only return active products (`isDelete = 0`) where the Seller's account is active and the Seller's `shopStatus` is NOT `Locked`, `Banned`, or `Pending`. |
| FR-PROD-07 | WHEN a Customer follows a Seller, THE SYSTEM SHALL check if a follow record exists; IF it exists, THE SYSTEM SHALL toggle `isDelete = 0` (soft-deleted recovery). |

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
      "description": "Mã giftcode Netflix chính hãng",
      "variants": [
        {
          "variantName": "Gói 1 tháng",
          "priceVnd": 95000,
          "imageUrl": "https://example.com/image.png"
        }
      ]
    }
    ```
*   **Response (200 OK):** Product details.

### `POST /api/seller/variants`
*   **Headers:** `Authorization: Bearer <Seller_JWT>`
*   **Request Body:**
    ```json
    {
      "productId": 1,
      "variantName": "Gói 3 tháng",
      "priceVnd": 270000,
      "imageUrl": "https://example.com/image3.png",
      "status": "Active"
    }
    ```
*   **Response (200 OK):** Variant created details.

### `PUT /api/seller/variants/{id}`
*   **Headers:** `Authorization: Bearer <Seller_JWT>`
*   **Request Body:**
    ```json
    {
      "variantName": "Gói 3 tháng giá rẻ",
      "priceVnd": 250000,
      "imageUrl": "https://example.com/image3_new.png",
      "status": "Active"
    }
    ```
*   **Response (200 OK):** Variant updated status.

### `POST /api/seller/digital-assets`
*   **Headers:** `Authorization: Bearer <Seller_JWT>`
*   **Request Body:**
    ```json
    {
      "variantId": 1,
      "assetType": "KEY",
      "assets": [
        {
          "keyCode": "NFTX-1M-ABCD-EFGH"
        },
        {
          "keyCode": "NFTX-1M-IJKL-MNOP"
        }
      ]
    }
    ```
*   **Response (200 OK):** Assets created count.