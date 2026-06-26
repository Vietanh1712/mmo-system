# SPEC-02: Customer Core Flows

## 1. Context and Goal
**Goal:** Đặc tả các luồng nghiệp vụ cốt lõi dành cho Customer (Khách hàng) trong MMO Market.
**Context:** Khách hàng cần Nạp tiền vào ví (qua Webhook tích hợp Sepay), Mua sản phẩm số, Tải xuống thông tin đơn hàng, Đánh giá sản phẩm và Gửi khiếu nại nếu sản phẩm lỗi.

## 2. Actors
- **Primary:** Customer
- **Secondary:** Sepay Payment Gateway

## 3. Functional Requirements (EARS)
- **FR-01 (Top-up):** KHI (Event-driven) Customer yêu cầu nạp tiền, hệ thống PHẢI sinh ra mã QR chuyển khoản chứa cú pháp giao dịch duy nhất.
- **FR-02 (Sepay Webhook):** KHI hệ thống nhận được Webhook từ Sepay xác nhận đã nhận tiền, hệ thống PHẢI tự động cộng số dư vào `Available Balance` của Customer.
- **FR-03 (Purchase):** TRONG KHI (State-driven) Customer bấm mua hàng, NẾU số dư khả dụng đủ lớn, hệ thống PHẢI trừ tiền, chuyển trạng thái đơn hàng thành Completed, và gửi thông tin sản phẩm tức thì.
- **FR-04 (Insufficient Balance):** TRONG KHI Customer bấm mua hàng, NẾU số dư không đủ, hệ thống PHẢI từ chối giao dịch và báo lỗi.
- **FR-05 (Escrow):** KHI đơn hàng được tạo thành công, hệ thống PHẢI ghi nhận một khoản tiền tương ứng vào trạng thái Giam tiền (Escrow) trong 72 giờ trước khi trả cho Seller.
- **FR-06 (Complaint):** MẶC ĐỊNH, Customer PHẢI có quyền tạo Khiếu nại cho một đơn hàng đã mua trong vòng 3 ngày kể từ ngày mua.
- **FR-07 (Review):** NẾU đơn hàng không bị khiếu nại, Customer ĐƯỢC PHÉP đánh giá sản phẩm (Rate 1-5 sao và comment).

## 4. Non-Functional Requirements
- **Performance:** Giao dịch mua hàng phải được xử lý tức thời (dưới 1 giây) để trải nghiệm mua nội dung số không bị gián đoạn.
- **Concurrency:** Transaction cộng/trừ tiền phải chặn lỗi Race Condition (sử dụng Optimistic/Pessimistic Locking hoặc Serializable Isolation Level).
- **Security:** Mã QR nạp tiền không được chứa thông tin nhạy cảm có thể giả mạo.

## 5. Data Model
- **Table `Transactions`:**
  - `transaction_id` (PK, BIGINT)
  - `user_id` (FK to Users)
  - `amount` (BIGINT)
  - `type` (Enum: DEPOSIT, PURCHASE, WITHDRAW, REFUND, FEE, ESCROW_RELEASE)
  - `status` (Enum: PENDING, COMPLETED, FAILED, HOLD)
  - `escrow_release_date` (DATETIME, Nullable)
  - `created_at` (DATETIME)
- **Table `Orders`:**
  - `order_id` (PK, BIGINT)
  - `customer_id` (FK to Users)
  - `product_variant_id` (FK to ProductVariants)
  - `price` (BIGINT)
  - `status` (Enum: COMPLETED, COMPLAINING, REFUNDED)
  - `delivered_content` (VARCHAR(MAX))
- **Table `Complaints`:**
  - `complaint_id` (PK, BIGINT)
  - `order_id` (FK to Orders)
  - `reason` (NVARCHAR(500))
  - `status` (Enum: OPEN, SELLER_RESPONDED, RESOLVED_REFUND, RESOLVED_REJECTED)

## 6. API Specification
- **POST `/api/v1/wallet/topup/qr`**
  - **Body:** `{ amount }`
  - **Response:** 200 OK `{ qr_code_url, transfer_syntax }`
- **POST `/api/v1/webhook/sepay`** (From Sepay)
  - **Body:** Dữ liệu chuẩn của Sepay Webhook
  - **Response:** 200 OK (Xử lý cộng tiền ngầm)
- **POST `/api/v1/orders/purchase`**
  - **Body:** `{ product_variant_id, quantity }`
  - **Response:** 200 OK `{ order_id, delivered_content }`
- **POST `/api/v1/complaints`**
  - **Body:** `{ order_id, reason, proof_images }`
  - **Response:** 201 Created

## 7. Error Handling
- `400 Bad Request`: Mua hàng khi số dư không đủ. Gửi khiếu nại khi đã quá thời hạn 3 ngày.
- `404 Not Found`: Sản phẩm không tồn tại hoặc đã hết hàng (Out of stock).

## 8. Acceptance Criteria
- **AC-01:** GIVEN Khách hàng có 100,000 VNĐ, WHEN mua sản phẩm giá 150,000 VNĐ, THEN hệ thống báo lỗi không đủ số dư.
- **AC-02:** GIVEN Khách hàng vừa mua hàng, WHEN gọi API tạo khiếu nại hợp lệ, THEN trạng thái đơn hàng chuyển sang `COMPLAINING` và số tiền giam (Escrow) của Seller cho đơn hàng này bị đóng băng chờ xử lý.

## 9. Out of Scope
- Tích hợp cổng thanh toán khác như VNPay, MoMo (Chỉ dùng chuyển khoản qua Sepay).
- Tính năng giỏ hàng (Cart) — MMO Market mua ngay (Buy Now) cho từng sản phẩm.
