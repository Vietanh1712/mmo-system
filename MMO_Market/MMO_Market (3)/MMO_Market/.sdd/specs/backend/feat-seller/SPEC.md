# SPEC — Seller Console & Shop Management
> **Feature ID:** `feat-seller`
> **UC Coverage:** UC-04 (Seller Registration), UC-05 (Seller Console)
> **Version:** 2.0 | **Status:** Active
> **Author:** Team | **Last Updated:** 2026-06-27

---

## 1. CONTEXT & GOAL (BỐI CẢNH & MỤC TIÊU)

### 1.1 Bối cảnh
Người dùng đã hoàn thành KYC thành công có nhu cầu kinh doanh sản phẩm số trên MMO Market cần được nâng cấp tài khoản và tạo hồ sơ cửa hàng (Shop) cùng thông tin thanh toán rút tiền ngân hàng. Sau khi được duyệt, Seller cần toàn bộ công cụ để vận hành gian hàng.

### 1.2 Mục tiêu
- Cung cấp cổng đăng ký mở Shop cho người dùng đã KYC.
- Xây dựng Seller Console đầy đủ: quản lý sản phẩm (CRUD), quản lý biến thể, quản lý tồn kho tài sản số (`DigitalAsset`), theo dõi giao dịch/đơn hàng, xem thống kê doanh thu, rút tiền (với 2FA OTP).
- Đảm bảo an toàn tài chính qua cấu hình tài khoản ngân hàng chính chủ.
- Bảo vệ nghiêm ngặt: mọi endpoint Seller Console yêu cầu role `SELLER`.

---

## 2. ACTOR (TÁC NHÂN)

| Actor | Role | Điều kiện tiền quyết |
|---|---|---|
| **Customer/User** | Đăng ký mở shop | Đã hoàn tất phê duyệt KYC (`isVerified = 1`) |
| **Seller** | Người quản lý gian hàng qua Seller Console | Đăng ký gian hàng được Staff/Admin duyệt (`status = APPROVED`, role = `SELLER`) |
| **Staff/Admin** | Xem danh sách & duyệt hồ sơ đăng ký shop | Có role `STAFF` hoặc `ADMIN` |

---

## 3. FUNCTIONAL REQUIREMENTS (Cú pháp EARS)

| ID | EARS Requirement |
|---|---|
| **FR-SELL-01** | WHEN a verified User (`isVerified = 1`) submits a shop registration with name and bank account, THE SYSTEM SHALL save a `SellerRegistration` record with status `PENDING` and a `SellerBankInfo` record. |
| **FR-SELL-02** | THE SYSTEM SHALL prevent duplicate registrations: IF a `PENDING` or `APPROVED` registration already exists for the user, THE SYSTEM SHALL reject the new submission with an error. |
| **FR-SELL-03** | WHEN a User queries `GET /me`, THE SYSTEM SHALL return the current status of their shop registration (`NOT_SUBMITTED`, `PENDING`, `APPROVED`, `REJECTED`). |
| **FR-SELL-04** | WHEN Staff/Admin approves a registration, THE SYSTEM SHALL update the user's role to `SELLER` and set `registration.status = APPROVED`. |
| **FR-SELL-05** | WHEN Staff/Admin rejects a registration, THE SYSTEM SHALL set `registration.status = REJECTED`; the user MAY re-submit a new registration. |
| **FR-SELL-06** | WHILE user is a Seller, THE SYSTEM SHALL allow full CRUD access to their own Products and Product Variants via `/api/seller/products/**` and `/api/seller/variants/**`. |
| **FR-SELL-07** | THE SYSTEM SHALL enforce ownership: A Seller MUST only be able to read, update, or delete Products, Variants, and DigitalAssets that belong to them (`product.seller_id = currentUser.id`). |
| **FR-SELL-08** | WHEN a Seller creates a Product, THE SYSTEM SHALL require at least 1 Variant; each Variant MUST have a `variantName`, `priceVnd` (BIGINT > 0), and `imageUrl`. |
| **FR-SELL-09** | WHEN a Seller uploads DigitalAssets (batch) for a Variant of type `ACCOUNT`, THE SYSTEM SHALL require `accountUsername` and `accountPassword`. For type `KEY`, THE SYSTEM SHALL require `keyCode` and check for duplicates within the Variant. For type `GAME_CARD`, THE SYSTEM SHALL require `cardCode`. |
| **FR-SELL-10** | AFTER creating or deleting a DigitalAsset, THE SYSTEM SHALL automatically recalculate `variant.stock` = count of unused, non-deleted assets for that Variant (for non-SERVICE products). |
| **FR-SELL-11** | WHEN a Seller requests a withdrawal, THE SYSTEM SHALL validate: balance ≥ `amountVnd` + fee, `amountVnd` ≥ `MIN_WITHDRAWAL_VND`, `amountVnd` ≤ `MAX_WITHDRAWAL_VND`. IF `REQUIRE_WITHDRAW_2FA = true`, THE SYSTEM SHALL require a valid OTP sent to seller's email. |
| **FR-SELL-12** | WHEN a Seller views their dashboard, THE SYSTEM SHALL return: `completedSales`, `totalRevenue` (net), `activeProductsCount`, `openComplaintsCount`, and the last 5 recent transactions. |
| **FR-SELL-13** | WHEN a Seller views their statistics, THE SYSTEM SHALL return: weekly net revenue chart (last 7 days), total completed sales count, escrow balance, and top 10 products by sold count. |
| **FR-SELL-14** | THE SYSTEM SHALL allow a Seller to view complaints raised against them, view complaint details (with chat history), and send chat messages within an active complaint. |
| **FR-SELL-15** | THE SYSTEM SHALL allow a Seller to view all reviews left on their products (read-only). |
| **FR-SELL-16** | THE SYSTEM SHALL allow a Seller to view all shop flags (violations) recorded against their shop (read-only). |
| **FR-SELL-17** | WHEN a Seller queries `GET /api/v1/seller/shop-info`, THE SYSTEM SHALL return current shop status, shop level, bank info, and suspension deadline (`suspendedUntil`), automatically resetting `shopStatus` to `Active` when `suspendedUntil` has expired. |
| **FR-SELL-18** | WHEN a Seller requests `PUT /api/v1/seller/shop-status`, THE SYSTEM SHALL allow toggling between `Active` and `Suspended`, while denying status changes if the shop status is `Banned` or `Locked`. |
| **FR-SELL-19** | THE SYSTEM SHALL use safest-record query methods (`findFirstByUserAndIsDeleteFalseOrderByIdDesc`) when reading seller registrations or bank info to prevent duplicate result exceptions. |

---

## 4. BUSINESS RULES (Ràng buộc nghiệp vụ)

| Rule | Mô tả |
|---|---|
| **BR-SELL-01** | Chỉ User có `isVerified = 1` mới được đăng ký shop. |
| **BR-SELL-02** | Không cho phép đăng ký mới khi đã có hồ sơ `PENDING` hoặc `APPROVED`. |
| **BR-SELL-03** | Giá sản phẩm (`priceVnd`) phải lưu kiểu `BIGINT` (VNĐ nguyên). |
| **BR-SELL-04** | Soft delete bắt buộc: không xóa vật lý Product, Variant, DigitalAsset, SellerRegistration. Dùng `isDelete = 1`. |
| **BR-SELL-05** | Ownership validation: Mọi thao tác ghi đều phải kiểm tra `seller.id == currentUser.id` tại Service/Controller. |
| **BR-SELL-06** | Mã key (`keyCode`) phải duy nhất trong phạm vi một Variant. |
| **BR-SELL-07** | Tính phí rút tiền theo `WITHDRAWAL_FEE_PERCENT` và `MIN_WITHDRAW_FEE_VND` từ `SystemConfiguration`. |
| **BR-SELL-08** | Rút tiền cần OTP email khi `REQUIRE_WITHDRAW_2FA = true` (đọc từ `SystemConfiguration`). |

---

## 5. DATA MODEL (Mô hình dữ liệu)

```sql
-- Bảng hồ sơ đăng ký gian hàng
CREATE TABLE SellerRegistrations (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    shop_name       NVARCHAR(255) NOT NULL,
    description     NVARCHAR(MAX) NULL,
    contract        VARCHAR(255) NULL,
    signed_contract VARCHAR(255) NULL,
    status          VARCHAR(20) DEFAULT 'Pending',  -- Pending | Approved | Rejected
    created_at      DATETIME DEFAULT GETDATE(),
    isDelete        BIT DEFAULT 0,
    CONSTRAINT FK_Reg_Users FOREIGN KEY (user_id) REFERENCES Users(id)
);

-- Bảng thông tin ngân hàng của Seller
CREATE TABLE SellerBankInfo (
    id             BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id        BIGINT NOT NULL,
    bank_name      NVARCHAR(100) NOT NULL,
    account_number VARCHAR(50) NOT NULL,
    branch         NVARCHAR(100) NULL,
    created_at     DATETIME DEFAULT GETDATE(),
    isDelete       BIT DEFAULT 0,
    CONSTRAINT FK_Bank_Users FOREIGN KEY (user_id) REFERENCES Users(id)
);

-- Bảng sản phẩm (dùng chung cho toàn platform)
-- productType: ACCOUNT | KEY | GAME_CARD | SERVICE
-- Seller quản lý sản phẩm qua /api/seller/products/**

-- Bảng tài sản số (DigitalAssets)
-- assetType: ACCOUNT | KEY | GAME_CARD
-- isUsed: 0 (chưa bán) | 1 (đã bán cho khách)
-- Stock của Variant = COUNT(assets WHERE isUsed=0 AND isDelete=0)
```

---

## 6. API SPEC (Đặc tả API)

### 6.1 Shop Registration (`/api/v1/shop-registrations`)

#### `POST /api/v1/shop-registrations`
- **Auth:** `CUSTOMER` hoặc `SELLER`
- **Request Body:**
  ```json
  {
    "shopName": "Cửa Hàng MMO Uy Tín",
    "description": "Chuyên bán tài khoản Netflix, Spotify giá rẻ",
    "bankName": "Vietcombank",
    "accountNumber": "001100123456",
    "branch": "Chi nhánh Hà Nội"
  }
  ```
- **Response (200 OK):** `ShopRegistrationResponseDto` với `status = PENDING`.
- **Response (400):** Đã có hồ sơ `PENDING`/`APPROVED`, hoặc User chưa KYC.

#### `GET /api/v1/shop-registrations/me`
- **Auth:** `CUSTOMER` hoặc `SELLER`
- **Response (200 OK):** `ShopRegistrationResponseDto` hoặc `{ "status": "NOT_SUBMITTED" }`.

#### `GET /api/v1/shop-registrations`
- **Auth:** `STAFF` hoặc `ADMIN`
- **Response (200 OK):** `List<ShopRegistrationResponseDto>` — danh sách hồ sơ chờ duyệt.

#### `PUT /api/v1/shop-registrations/{id}/review`
- **Auth:** `STAFF` hoặc `ADMIN`
- **Request Body:** `ShopRegistrationReviewDto` (`{ "status": "Approved"|"Rejected", "note": "..." }`).
- **Response (200 OK):** `ShopRegistrationResponseDto` đã cập nhật trạng thái; nếu Approved → role User = `SELLER`.

---

### 6.2 Seller Console (`/api/seller/**`) — Yêu cầu role `SELLER`

#### Dashboard & Thông tin cửa hàng

| Method | Endpoint | Mô tả |
|---|---|---|
| `GET` | `/api/seller/dashboard` | Trả về: `completedSales`, `totalRevenue`, `activeProductsCount`, `openComplaintsCount`, `recentTransactions` (5 gần nhất). |
| `GET` | `/api/seller/shop-info` | Thông tin cửa hàng: `shopName`, `description`, `bankName`, `accountNumber`, `branch`, `accountHolder`. |
| `PUT` | `/api/seller/shop-info` | Cập nhật `shopName`, `description`, và/hoặc thông tin ngân hàng. |
| `GET` | `/api/seller/categories` | Danh sách category con (để dùng trong dropdown tạo sản phẩm). |
| `GET` | `/api/seller/statistics` | Biểu đồ doanh thu 7 ngày, top 10 sản phẩm bán chạy, escrow balance. |

#### Quản lý sản phẩm

| Method | Endpoint | Mô tả |
|---|---|---|
| `GET` | `/api/seller/products` | Danh sách sản phẩm của Seller (kèm `variantCount`, `totalStock`, `unusedAssetsCount`). |
| `GET` | `/api/seller/products/{id}` | Chi tiết sản phẩm + danh sách Variants. |
| `POST` | `/api/seller/products` | Tạo sản phẩm mới. Bắt buộc: `name`, `categoryId`, ít nhất 1 variant có `variantName`, `priceVnd`, `imageUrl`. |
| `PUT` | `/api/seller/products/{id}` | Cập nhật `name`, `description`, `userGuide`, `categoryId`. |
| `PUT` | `/api/seller/products/{id}/details` | Cập nhật `productType` (`ACCOUNT`/`KEY`/`GAME_CARD`/`SERVICE`) và ảnh đại diện sản phẩm. |
| `DELETE` | `/api/seller/products/{id}` | Soft delete sản phẩm và cascade soft delete toàn bộ Variants. |
| `POST` | `/api/seller/upload-image` | Upload ảnh base64 → lưu vào `/uploads/`, trả về URL. |

#### Quản lý biến thể (Variant)

| Method | Endpoint | Mô tả |
|---|---|---|
| `GET` | `/api/seller/variants/{id}` | Chi tiết Variant: `variantName`, `priceVnd`, `stock`, `status`, `imageUrl`. |
| `POST` | `/api/seller/variants` | Tạo Variant mới cho sản phẩm. Bắt buộc: `productId`, `variantName`, `priceVnd`, `imageUrl`. |
| `PUT` | `/api/seller/variants/{id}` | Cập nhật `variantName`, `priceVnd`, `stock`, `status`, `imageUrl`. |
| `DELETE` | `/api/seller/variants/{id}` | Soft delete Variant + cascade soft delete toàn bộ DigitalAssets của Variant đó. |

#### Quản lý tài sản số (DigitalAsset / Kho hàng)

| Method | Endpoint | Mô tả |
|---|---|---|
| `GET` | `/api/seller/variants/{variantId}/assets` | Danh sách DigitalAssets của Variant (xem toàn bộ kho, kể cả đã bán). |
| `POST` | `/api/seller/digital-assets` | Thêm tài sản số (batch). Body: `{ variantId, assetType, assets: [...] }`. Tự động cập nhật `variant.stock` sau khi thêm. |
| `DELETE` | `/api/seller/digital-assets/{id}` | Soft delete 1 DigitalAsset. Tự động cập nhật `variant.stock` sau khi xóa. |

**Cấu trúc `assets[]` theo `assetType`:**

| assetType | Trường bắt buộc | Trường tùy chọn |
|---|---|---|
| `ACCOUNT` | `accountUsername`, `accountPassword` | `notes` |
| `KEY` | `keyCode` (unique trong Variant) | `notes` |
| `GAME_CARD` | `cardCode` | `cardPin`, `notes` |

#### Giao dịch & Rút tiền

| Method | Endpoint | Mô tả |
|---|---|---|
| `GET` | `/api/seller/transactions` | Lịch sử giao dịch bán hàng: `productName`, `variantName`, `amountVnd`, `commissionVnd`, `netEarningVnd`, `status`, `escrowReleaseDate`. |
| `GET` | `/api/seller/withdrawals` | Danh sách yêu cầu rút tiền. |
| `GET` | `/api/seller/withdrawals/{id}` | Chi tiết 1 yêu cầu rút tiền. |
| `GET` | `/api/seller/withdrawals/config` | Cấu hình rút tiền: `withdrawalFeePercent`, `minWithdrawFee`, `minWithdrawalLimit`, `maxWithdrawalLimit`, `requireWithdraw2FA`. |
| `POST` | `/api/seller/withdrawals/send-otp` | Gửi OTP xác thực rút tiền về email Seller. |
| `POST` | `/api/seller/withdrawals` | Tạo yêu cầu rút tiền: `{ amountVnd, otp }`. Kiểm tra số dư, giới hạn, OTP (nếu bắt buộc). |

#### Khiếu nại, Đánh giá & Cờ vi phạm

| Method | Endpoint | Mô tả |
|---|---|---|
| `GET` | `/api/seller/complaints` | Danh sách khiếu nại liên quan đến Seller. |
| `GET` | `/api/seller/complaints/{id}` | Chi tiết khiếu nại + lịch sử chat. |
| `POST` | `/api/seller/complaints/{id}/chat` | Gửi tin nhắn trong khiếu nại (Seller → Customer, Staff giám sát). |
| `GET` | `/api/seller/reviews` | Danh sách đánh giá của khách trên sản phẩm Seller (read-only). |
| `GET` | `/api/seller/shop-flags` | Danh sách cờ vi phạm shop: `flagLevel`, `reason`, `staffName`, `complaintId` (read-only). |

---

## 7. ERROR HANDLING (Xử lý lỗi)

| Tình huống | HTTP Status | Message |
|---|---|---|
| Không có role `SELLER` khi gọi `/api/seller/**` | `403 Forbidden` | `"Tài khoản không có quyền truy cập Seller Portal."` |
| Truy cập Product/Variant/Asset không thuộc về Seller | `403 Forbidden` | `"Bạn không có quyền thao tác trên sản phẩm/biến thể/tài sản này."` |
| Đăng ký shop khi đã có hồ sơ PENDING/APPROVED | `400 Bad Request` | `IllegalStateException` message |
| Tạo sản phẩm không có Variant | `400 Bad Request` | `"Sản phẩm phải có ít nhất 1 biến thể."` |
| Tạo KEY trùng lặp trong Variant | `400 Bad Request` | `"Mã key này đã tồn tại trong kho."` |
| Rút tiền không đủ số dư / sai OTP | `400 Bad Request` | Thông báo cụ thể từ `WithdrawalService` |

---

## 8. SECURITY (Phân quyền)

```
/api/v1/shop-registrations          → CUSTOMER, SELLER (POST, GET /me)
/api/v1/shop-registrations          → STAFF, ADMIN (GET all, PUT review)
/api/seller/**                      → SELLER only
```

- Ownership check bắt buộc tại Controller/Service cho mọi thao tác ghi trên Product, Variant, DigitalAsset, Withdrawal.
- OTP 2FA cho rút tiền được điều khiển qua `SystemConfiguration.REQUIRE_WITHDRAW_2FA`.