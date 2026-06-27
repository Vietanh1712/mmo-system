# PLAN — Pre-Orders (`feat-preorder`)

## 1. Mục tiêu (Goals)

Triển khai luồng đặt hàng trước (Pre-order) dành cho các sản phẩm số chưa có sẵn trong kho hàng hoặc tạm thời hết hàng theo đặc tả `SPEC.md` (feat-preorder). Hệ thống hỗ trợ:
- Cho phép Người mua (Customer) gửi yêu cầu đặt hàng trước với số lượng và mức giá kỳ vọng kèm theo ghi chú.
- Người bán (Seller) tiếp nhận thông tin yêu cầu đặt hàng trước từ khách hàng để chuẩn bị hàng hóa và bổ sung tồn kho phù hợp.
- Hệ thống ghi nhận trạng thái và tự động cập nhật khi có hàng mới.

## 2. Kiến trúc & Công nghệ

- **Backend:** Java 17, Spring Boot 3.1, Spring Data JPA, SQL Server (T-SQL).
- **Frontend:** Thymeleaf template engine kết hợp CSS & JS thuần (gọi REST API bằng `authFetch`).
- **Tuân thủ:** Mô hình phân lớp Controller → Service → Repository → Entity; DTO Pattern cho Request/Response; Bảo vệ chặt chẽ quyền sở hữu (chỉ người mua mới được đặt và xem pre-order cá nhân).

## 3. Các thành phần Backend

### 3.1. Database Migration & Entities

- **Entity `PreOrder`** (bảng `PreOrders`):
  - `customer` (ManyToOne -> User): Người mua tạo pre-order.
  - `product` (ManyToOne -> Product): Sản phẩm được đặt trước.
  - `quantity` (INT): Số lượng đặt mua.
  - `expectedPriceVnd` (BIGINT): Tổng giá kỳ vọng bằng VNĐ (sử dụng số nguyên lớn).
  - `status` (VARCHAR): Trạng thái của đơn đặt trước (`PENDING_RELEASE`, `RELEASED`, `CANCELLED`).
  - `notes` (NVARCHAR): Ghi chú bổ sung từ khách hàng.
  - `isDelete` (BIT): Soft delete.

### 3.2. Repositories (Spring Data JPA)

- **`PreOrderRepository`**:
  - `findByCustomerAndIsDeleteFalse(customer)`: Lấy các pre-order cá nhân chưa bị xóa của khách hàng.
  - `findByProductAndIsDeleteFalse(product)`: Tìm kiếm yêu cầu pre-order theo sản phẩm để Seller chuẩn bị.

### 3.3. DTOs

- Request: `PreOrderRequest` (gồm `productId`, `quantity`, `expectedPriceVnd`, `notes`).
- Response: `PreOrderResponse` (gồm thông tin chi tiết trạng thái pre-order, tên sản phẩm và thông báo phản hồi).

### 3.4. Services (Business Logic)

- **`PreOrderService`**:
  - `createPreOrder(customerId, request)`:
    - Tìm và kiểm tra User, Product tồn tại.
    - Validate dữ liệu đầu vào (số lượng phải lớn hơn 0, tổng giá phải lớn hơn 0).
    - Lưu pre-order mới với trạng thái mặc định `PENDING_RELEASE`.
  - `getPreOrdersByCustomer(customerId)`: Lấy lịch sử đặt trước của người mua.

### 3.5. Controllers & Security

- **`PreOrderController`** (`/api/v1/pre-orders`):
  - `POST /`: Tạo mới pre-order (`@PreAuthorize("hasRole('CUSTOMER')")`).
  - `GET /`: Xem danh sách pre-order cá nhân (`@PreAuthorize("hasRole('CUSTOMER')")`).
- **`PreOrderPageController`** (hoặc MVC Controller phục vụ giao diện):
  - Phục vụ render giao diện đặt trước trên trình duyệt `/pre-orders/new`.

---

## 4. Các thành phần Frontend

- **Form Đặt Trước của Khách Hàng:**
  - File: `templates/pre-orders/new.html` (form điền số lượng và ghi chú).
- **Trang quản lý đơn đặt trước:**
  - File: `templates/account/pre-orders.html` hiển thị danh sách pre-order của khách hàng.

---

## 5. Definition of Done

- Toàn bộ giá tiền pre-order (`expectedPriceVnd`) phải sử dụng kiểu dữ liệu số nguyên lớn (`Long` / `BIGINT`).
- Quyền sở hữu (Ownership validation) được kiểm soát chặt chẽ để người dùng chỉ được phép truy xuất danh sách và chi tiết pre-order của chính họ.