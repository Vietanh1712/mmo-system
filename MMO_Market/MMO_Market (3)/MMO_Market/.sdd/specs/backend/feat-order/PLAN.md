# PLAN — Orders & Escrow Purchase (`feat-order`)

## 1. Mục tiêu (Goals)

Triển khai quy trình mua hàng số, tạm giữ tiền Escrow (trong 72 giờ) và tự động bàn giao tài nguyên số (giftcode/tài khoản) cho Người mua theo đặc tả `SPEC.md` (feat-order). Các nghiệp vụ chính:
- Khách hàng (Customer) mua biến thể sản phẩm số (Product Variant) sử dụng số dư ví khả dụng.
- Hệ thống tự động trừ tiền ví khả dụng của Người mua, giảm số lượng tồn kho sản phẩm.
- Hệ thống tạo giao dịch tạm giữ với trạng thái `Held` (Escrow) và tự động tính toán thời hạn giải phóng (`escrowReleaseDate` mặc định là 72 giờ sau).
- Tự động truy xuất tài nguyên số rảnh rỗi (`DigitalAsset` có `isUsed = false`) của biến thể tương ứng, đánh dấu đã sử dụng (`isUsed = true`) và gán cho giao dịch để bàn giao cho Người mua.
- Ghi nhận lịch sử giao dịch ví và gửi thông báo biến động số dư.

## 2. Kiến trúc & Công nghệ

- **Backend:** Java 17, Spring Boot 3.1, Spring Data JPA, SQL Server (T-SQL).
- **Frontend:** Thymeleaf template engine kết hợp CSS & JS thuần (giao tiếp qua REST API bằng `authFetch`).
- **Tuân thủ:** Mô hình phân lớp Controller → Service → Repository → Entity; DTO Pattern (PurchaseRequest, PurchaseResponse, OrderDto). Áp dụng khóa bi quan Pessimistic Lock trên tài khoản ví khi thực hiện giao dịch tài chính để tránh tình trạng race-condition (Over-purchasing / double-spending).

## 3. Các thành phần Backend

### 3.1. Database Migration & Entities

- **Entity `Transaction`** (bảng `Transactions`):
  - `customer` (ManyToOne -> User): Người mua hàng.
  - `seller` (ManyToOne -> User): Người bán hàng.
  - `product` (ManyToOne -> Product): Sản phẩm được mua.
  - `variant` (ManyToOne -> ProductVariant): Biến thể được mua.
  - `amountVnd` (BIGINT): Giá bán thực tế (số nguyên lớn).
  - `commissionVnd` (BIGINT): Phí hoa hồng hệ thống giữ lại.
  - `status` (VARCHAR): Trạng thái đơn hàng (`Held`, `Completed`, `Refunded`, `Cancelled`, `Disputed`).
  - `escrowReleaseDate` (DATETIME): Thời gian giải phóng Escrow.
  - `isDelete` (BIT): Soft delete.
- **Entity `DigitalAsset`** (bảng `DigitalAssets`):
  - `variant` (ManyToOne -> ProductVariant): Biến thể sở hữu tài nguyên này.
  - `transaction` (ManyToOne -> Transaction): Giao dịch đã mua tài nguyên này.
  - `assetType` (VARCHAR): Loại tài nguyên (`KEY`, `GAME_CARD`, `ACCOUNT`).
  - `keyCode`, `cardCode`, `accountUsername`, `accountPassword` (VARCHAR): Nội dung nhạy cảm được mã hóa AES-256.
  - `isUsed` (BIT): Đã bán hay chưa.
  - `isDelete` (BIT): Soft delete flag.

### 3.2. Repositories (Spring Data JPA)

- `TransactionRepository`:
  - `findByCustomerAndIsDeleteFalseOrderByCreatedAtDesc(customer)`: Lấy các đơn hàng đã mua của khách.
  - `countByProductIdAndIsDeleteFalse(productId)`: Đếm số lượng sản phẩm đã bán.
- `DigitalAssetRepository`:
  - `findByVariantAndIsUsedFalseAndIsDeleteFalse(variant)`: Tìm tài nguyên số rảnh rỗi để bán.
  - `findByTransactionAndIsDeleteFalse(transaction)`: Lấy tài nguyên bàn giao theo đơn hàng.

### 3.3. DTOs

- Request DTOs: `PurchaseRequest` (gồm `productId`, `variantLabel`).
- Response DTOs: `PurchaseResponse` (chứa `transactionCode`, `orderCode`, `finalBalance`, `productName`, `amount`, `credentials`, `transactionId`).
- `OrderDto` (truyền thông tin đơn hàng đầy đủ ra frontend).

### 3.4. Services (Business Logic)

- **`TransactionService`**:
  - `purchaseProduct(userId, productId, variantLabel)` (Được đánh dấu `@Transactional`):
    - Đọc cấu hình phí cố định người mua `FLAT_BUYER_FEE_VND` (nếu có, mặc định là 0 VNĐ) và kiểm tra số dư ví.
    - Áp dụng Pessimistic Lock trên tài khoản người mua.
    - Khấu trừ số dư ví người mua, giảm số lượng tồn kho variant đi 1.
    - Tính toán hoa hồng sàn dựa trên cấu hình `DEFAULT_COMMISSION_PERCENT`.
    - Tạo `Transaction` ở trạng thái `Held` và thiết lập `escrowReleaseDate`.
    - Tìm và gán `DigitalAsset` cho `Transaction` đó, đổi cờ `isUsed = true`.
    - Ghi nhận Wallet Ledger biến động số dư qua `WalletService`.
  - `getMyOrders(userId)`: Lấy lịch sử mua hàng.
  - `getTransactionDetail(transactionId, userId)`: Lấy chi tiết đơn hàng (xác thực ownership).

### 3.5. Controllers & Security

- **`TransactionController`** (`/api/transactions`):
  - `POST /purchase`: Thực hiện thanh toán (`@PreAuthorize("hasRole('CUSTOMER')")`). Chặn các tài khoản Admin/Staff không được mua hàng.
  - `GET /me`: Lấy danh sách đơn hàng đã mua (`@PreAuthorize("hasRole('CUSTOMER')")`).
  - `GET /{id}`: Xem chi tiết đơn hàng kèm thông tin tài khoản/key đã mua (`@PreAuthorize("hasRole('CUSTOMER')")`).

---

## 4. Các thành phần Frontend

- **Trang mua hàng / Thanh toán (Checkout):**
  - File: `templates/checkout.html`. Màn hình xác nhận đơn hàng trước khi thanh toán.
- **Trang lịch sử mua hàng:**
  - File: `templates/account/orders.html` và JS `static/js/customer/account-orders.js`.
  - Hiển thị danh sách đơn hàng đã mua, trạng thái (Held/Completed/Disputed) và nút "Xác nhận nhận hàng" hoặc "Khiếu nại".
- **Trang chi tiết đơn hàng:**
  - File: `templates/account/order-detail.html`. Hiển thị thông tin tài khoản/mật mã hoặc key game được bàn giao sau khi thanh toán thành công.

---

## 5. Definition of Done

- Toàn bộ dòng tiền và giá trị đơn hàng bắt buộc sử dụng kiểu dữ liệu số nguyên lớn `Long` / `BIGINT`.
- API `/api/transactions/{id}` lấy chi tiết đơn hàng phải bắt buộc kiểm tra quyền sở hữu (Customer ID gửi yêu cầu phải trùng khớp với `buyer_id` trong DB) để chống lỗ hổng bảo mật IDOR.
- Quản trị viên (Admin) và Nhân viên vận hành (Staff) không được phép thực hiện chức năng mua hàng.