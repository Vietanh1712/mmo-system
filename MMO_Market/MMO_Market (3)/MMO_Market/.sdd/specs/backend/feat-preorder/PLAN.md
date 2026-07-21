# PLAN — Pre-Orders (`feat-preorder`)

## 1. Mục tiêu (Goals)

Triển khai luồng đặt hàng trước (Pre-order) dành cho các sản phẩm số chưa có sẵn trong kho hàng hoặc tạm thời hết hàng theo đặc tả `SPEC.md` (feat-preorder). Hệ thống hỗ trợ:
- Cho phép Người mua (Customer) gửi yêu cầu đặt hàng trước với số lượng và mức giá dự kiến kèm theo ghi chú.
- Người bán (Seller) tiếp nhận thông tin yêu cầu đặt hàng trước từ khách hàng để chuẩn bị hàng hóa và bổ sung tồn kho phù hợp.
- Hệ thống ghi nhận và cung cấp màn hình xem danh sách các đơn đặt trước để khách hàng theo dõi.

---

## 2. Kiến trúc & Công nghệ

- **Backend:** Java 17, Spring Boot 3.1, Spring Data JPA, SQL Server (T-SQL).
- **Frontend:** Thymeleaf template engine kết hợp CSS & JS thuần (gọi REST API qua `fetch` kèm Header `Authorization`).
- **Tuân thủ:** Mô hình phân lớp Controller → Service → Repository → Entity; DTO Pattern cho Request/Response; Bảo vệ chặt chẽ quyền sở hữu (chỉ người mua mới được đặt và xem pre-order cá nhân).

---

## 3. Các thành phần Backend

### 3.1. Database Model & Entity (`PreOrder`)

- **Bảng `PreOrders`** chứa cấu trúc dữ liệu sau:
  - `id` (BIGINT IDENTITY): Khóa chính tự tăng.
  - `customer_id` (BIGINT): Liên kết ManyToOne với entity `User` (Khách hàng tạo pre-order).
  - `product_id` (BIGINT): Liên kết ManyToOne với entity `Product` (Sản phẩm đặt trước).
  - `expected_price_vnd` (BIGINT): Tổng giá dự kiến bằng VNĐ (sử dụng số nguyên lớn).
  - `quantity` (INT): Số lượng đặt mua.
  - `status` (VARCHAR): Trạng thái đơn đặt trước. Các trạng thái được xử lý trên UI gồm:
    - `"Pending"`: Đang chờ duyệt/tiếp nhận.
    - `"Approved"` / `"Accepted"`: Đã được duyệt, Seller đang chuẩn bị hàng.
    - `"Completed"`: Đã bàn giao hàng thành công.
    - `"Rejected"`: Bị từ chối.
    - `"Cancelled"`: Đơn đặt trước đã bị khách hàng hủy.
  - `notes` (NVARCHAR(MAX)): Ghi chú bổ sung của khách hàng gửi cho Seller.
  - `created_at` (DATETIME): Thời điểm tạo yêu cầu.
  - `isDelete` (BIT): Cờ xóa mềm.

### 3.2. Repositories (Spring Data JPA)

- **`PreOrderRepository`**:
  - `findByCustomerAndIsDeleteFalse(customer)`: Lấy các pre-order cá nhân chưa bị xóa của khách hàng.
  - `findByCustomerAndIsDeleteFalseOrderByCreatedAtDesc(customer)`: Lấy danh sách pre-order cá nhân, sắp xếp theo thời gian mới nhất để phục vụ hiển thị.

### 3.3. DTOs

- **`PreOrderRequest`**: Gồm `productId` (NotNull), `quantity` (NotNull, Min=1), `expectedPriceVnd` (NotNull, Min=1), `notes` (Size max=2000).
- **`PreOrderResponse`**: Chứa thông tin chi tiết trạng thái pre-order bao gồm: `success`, `message`, `id`, `productId`, `productName`, `quantity`, `expectedPriceVnd`, `status`, `notes`, `createdAt` (dạng chuỗi định dạng `yyyy-MM-dd HH:mm:ss`).

### 3.4. Services (Business Logic)

- **`PreOrderService`**:
  - `createPreOrder(customerId, request)`: Validate tham số (productId, quantity, expectedPriceVnd); tìm User và Product; khởi tạo và lưu đối tượng `PreOrder` vào CSDL với `status` mặc định là `"Pending"`.
  - `getPreOrdersByCustomer(customerId)`: Tìm các đơn đặt trước của khách hàng qua `PreOrderRepository`, ánh xạ sang DTO response để trả về cho Client.

### 3.5. Controllers & Security

- **`PreOrderController`** (`/api/v1/pre-orders`):
  - `POST /`: Tạo mới pre-order (yêu cầu vai trò `Customer` hoặc người dùng đã đăng nhập).
  - `GET /`: Xem danh sách pre-order cá nhân.
- **`PreOrderPageController`** (MVC Controller phục vụ giao diện):
  - `GET /pre-orders/new`: Phục vụ trang tạo yêu cầu đặt trước (`pre-order-request.html`).
  - `GET /pre-orders`: Phục vụ trang danh sách lịch sử đặt trước (`pre-orders.html`).

---

## 4. Các thành phần Frontend

Nằm trực tiếp trong thư mục `apps/frontend/templates/`:
- **Trang tạo yêu cầu đặt hàng trước:** `pre-order-request.html`. Giao diện hiển thị tóm tắt sản phẩm đang hết hàng, tự động tính tổng giá dự kiến (`giá sản phẩm x số lượng`), lấy thông tin tham số qua URL và gửi Ajax `POST /api/v1/pre-orders`.
- **Trang danh sách đơn hàng đã đặt trước:** `pre-orders.html`. Fetch dữ liệu từ `GET /api/v1/pre-orders` và render giao diện sơ đồ tiến trình (workflow step) của đơn đặt hàng dựa theo trạng thái (`Pending`, `Approved`, `Completed`, `Cancelled`, `Rejected`).

---

## 5. Definition of Done

- Toàn bộ giá tiền pre-order (`expectedPriceVnd`) phải sử dụng kiểu dữ liệu số nguyên lớn (`Long` / `BIGINT` trong database).
- Quyền sở hữu (Ownership validation) được kiểm soát chặt chẽ thông qua việc trích xuất `customerId` trực tiếp từ thông tin xác thực Token của Spring Security (`@AuthenticationPrincipal`), không cho phép giả mạo ID người khác để đăng ký hoặc xem danh sách.