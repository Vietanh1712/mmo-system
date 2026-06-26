# SPEC-03: Seller Core Flows

## 1. Context and Goal
**Goal:** Đặc tả quy trình dành cho Seller (Người bán hàng) trên nền tảng MMO Market.
**Context:** Sau khi được cấp quyền (thông qua duyệt KYC), Seller có thể tạo cửa hàng, đăng sản phẩm, thiết lập biến thể (variants), quản lý khiếu nại từ khách hàng và yêu cầu rút tiền từ ví ra tài khoản ngân hàng thật.

## 2. Actors
- **Primary:** Seller
- **Secondary:** Staff (Người duyệt lệnh rút tiền)

## 3. Functional Requirements (EARS)
- **FR-01 (Create Product):** NẾU người dùng có Role là Seller, hệ thống PHẢI cho phép họ tạo và chỉnh sửa thông tin Sản phẩm.
- **FR-02 (Data Encryption):** KHI Seller tải lên nội dung số (tài khoản, key, file mềm), hệ thống PHẢI mã hóa dữ liệu này trước khi lưu vào cơ sở dữ liệu.
- **FR-03 (Manage Variants):** MẶC ĐỊNH, hệ thống PHẢI cho phép Seller tạo nhiều Biến thể (Variants) cho một Sản phẩm (VD: Gói 1 tháng, Gói 1 năm) với giá và số lượng khác nhau.
- **FR-04 (Withdrawal Request):** KHI Seller có số dư `Available Balance` lớn hơn 50.000 VNĐ, họ ĐƯỢC PHÉP tạo lệnh Rút tiền (Withdraw) về tài khoản ngân hàng đã liên kết.
- **FR-05 (Withdrawal Freeze):** TRONG KHI lệnh rút tiền ở trạng thái `Pending`, hệ thống PHẢI trừ số dư `Available Balance` và cộng vào `Hold/Frozen Balance` của Seller.
- **FR-06 (Respond Complaint):** NẾU có khiếu nại từ Customer, Seller PHẢI có quyền phản hồi (kèm bằng chứng) trong thời gian quy định trước khi Staff vào phân xử.

## 4. Non-Functional Requirements
- **Security:** Nội dung sản phẩm không bao giờ được trả về frontend trừ khi giao dịch Mua hàng thành công.
- **Usability:** Giao diện quản lý doanh thu cần hiển thị trực quan số tiền đang bị giam (Escrow) so với số tiền có thể rút.

## 5. Data Model
- **Table `Products`:**
  - `product_id` (PK, BIGINT)
  - `seller_id` (FK to Users)
  - `category_id` (FK to Categories)
  - `name`, `description`, `images`
  - `is_delete` (BIT, Default 0)
- **Table `ProductVariants`:**
  - `variant_id` (PK, BIGINT)
  - `product_id` (FK to Products)
  - `variant_name` (NVARCHAR(100))
  - `price` (BIGINT)
  - `stock_quantity` (INT)
  - `encrypted_content` (VARCHAR(MAX)) -- Cực kỳ quan trọng
- **Table `Withdrawals`:**
  - `withdrawal_id` (PK, BIGINT)
  - `seller_id` (FK to Users)
  - `bank_name`, `bank_account_no`, `bank_account_name`
  - `amount` (BIGINT)
  - `status` (Enum: PENDING, APPROVED, REJECTED)
  - `created_at` (DATETIME)

## 6. API Specification
- **POST `/api/v1/seller/products`**
  - **Body:** Thông tin sản phẩm.
  - **Response:** 201 Created
- **POST `/api/v1/seller/variants`**
  - **Body:** `{ product_id, variant_name, price, stock, raw_content }`
  - **Response:** 201 Created (Backend tự động mã hóa `raw_content` thành `encrypted_content`).
- **POST `/api/v1/seller/withdrawals`**
  - **Body:** `{ amount, bank_id }`
  - **Response:** 201 Created (Tạo lệnh Pending)

## 7. Error Handling
- `400 Bad Request`: Rút tiền dưới hạn mức tối thiểu 50.000 VNĐ. Rút tiền vượt quá số dư khả dụng.
- `403 Forbidden`: User chưa phải là Seller (chưa pass KYC).

## 8. Acceptance Criteria
- **AC-01:** GIVEN Seller có 100.000 VNĐ Khả dụng, WHEN tạo lệnh rút 60.000 VNĐ, THEN Khả dụng còn 40.000 VNĐ, Đóng băng tăng thêm 60.000 VNĐ, và xuất hiện 1 bản ghi lệnh Rút tiền Pending.
- **AC-02:** GIVEN Seller đang thêm Variant, WHEN nhập `raw_content`, THEN cơ sở dữ liệu lưu chuỗi đã mã hóa chứ không phải plaintext.

## 9. Out of Scope
- Tự động giải ngân (Auto-payout) bằng API ngân hàng. Hiện tại Staff duyệt và chuyển khoản thủ công.
