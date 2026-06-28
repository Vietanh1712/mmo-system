# PLAN — Product Catalog & Reviews Management (`feat-product`)

## 1. Mục tiêu (Goals)

Triển khai danh mục sản phẩm số (Product Catalog), tìm kiếm lọc phân trang và luồng đánh giá (Reviews) sau mua hàng theo đặc tả `SPEC.md` (feat-product). Hệ thống cho phép:
- Quản lý danh mục (Categories), sản phẩm (Products) và biến thể sản phẩm (Product Variants) như gói tháng, gói năm.
- Cho phép khách xem hàng và tìm kiếm nâng cao (theo từ khóa, giá cả, danh mục, xếp hạng).
- Theo dõi/bỏ theo dõi cửa hàng (Follow Shop) để cập nhật tin tức.
- Viết đánh giá kèm sao (1-5) và bình luận từ Khách hàng. Đảm bảo ràng buộc nghiệp vụ: chỉ những tài khoản đã mua sản phẩm thành công (đơn hàng ở trạng thái `Completed` hoặc `Held`) mới được phép đánh giá, và mỗi giao dịch mua chỉ được đánh giá một lần.

## 2. Kiến trúc & Công nghệ

- **Backend:** Java 17, Spring Boot 3.1, Spring Data JPA, SQL Server (T-SQL).
- **Frontend:** Thymeleaf template engine kết hợp CSS & JS thuần (gọi REST API bằng `authFetch`).
- **Bảo mật:** Mã hóa thông tin nhạy cảm của tài nguyên số (Key game, Giftcode, Account) trước khi lưu trữ vào cơ sở dữ liệu.

## 3. Các thành phần Backend

### 3.1. Database Migration & Entities

- **Entity `Category`** (bảng `Categories`):
  - `name`, `description`.
  - Soft delete qua cờ `isDelete`.
- **Entity `Product`** (bảng `Products`):
  - `seller` (ManyToOne -> User): Shop đăng bán.
  - `category` (ManyToOne -> Category): Danh mục.
  - `name`, `description`, `price` (giá thấp nhất), `image`, `userGuide` (hướng dẫn sử dụng).
- **Entity `ProductVariant`** (bảng `ProductVariants`):
  - `product` (ManyToOne -> Product): Sản phẩm cha.
  - `variantName` (NVARCHAR): Tên biến thể (VD: 1 Tháng, 12 Tháng).
  - `priceVnd` (BIGINT): Giá của biến thể.
  - `stock` (INT): Số lượng tồn kho.
- **Entity `Review`** (bảng `Reviews`):
  - `product` (ManyToOne -> Product): Sản phẩm được đánh giá.
  - `user` (ManyToOne -> User): Người viết đánh giá.
  - `transactionId` (BIGINT): Mã giao dịch tương ứng.
  - `rating` (INT): Xếp hạng từ 1 đến 5 sao.
  - `comment` (NVARCHAR): Bình luận của khách.
  - `mediaUrl` (NVARCHAR): Link ảnh/video đính kèm.
- **Entity `ShopFollower`** (bảng `ShopFollowers`):
  - `follower` (ManyToOne -> User): Khách theo dõi.
  - `seller` (ManyToOne -> User): Shop được theo dõi.
  - Hỗ trợ cờ `isDelete` để bật/tắt theo dõi (soft-delete recovery) tránh vi phạm Unique Key Constraint `(follower_id, seller_id)`.

### 3.2. Repositories (Spring Data JPA)

- `ProductRepository`:
  - `findByIdAndIsDeleteFalse(id)`: Lấy chi tiết sản phẩm hoạt động.
- `ReviewRepository`:
  - `findAverageRatingByProductId(productId)`: Tính điểm đánh giá trung bình.
  - `countByProductIdAndIsDeleteFalse(productId)`: Tổng số lượng review.
  - `existsByTransactionIdAndIsDeleteFalse(transactionId)`: Kiểm tra giao dịch đã được đánh giá chưa.
- `ShopFollowerRepository`:
  - `findByFollowerIdAndSellerId(followerId, sellerId)`: Lấy bản ghi follow bao gồm cả các bản ghi đã xóa mềm để phục vụ toggle.

### 3.3. DTOs

- Request DTOs: `ReviewRequestDTO`.
- Response DTOs: `FeaturedProductDTO`, `ProductSearchResultDTO`, `ProductDetailDTO`, `ReviewResponseDTO`.

### 3.4. Services (Business Logic)

- **`ProductService`**:
  - CRUD sản phẩm và biến thể.
  - `getFeaturedProducts(limit)`: Trích xuất các sản phẩm nổi bật dựa trên số lượng đơn hàng hoàn thành thực tế từ DB.
- **`ProductSearchService`**:
  - `searchProducts(...)`: Tìm kiếm phân trang lọc nâng cao theo từ khóa, mức giá, danh mục, xếp hạng.

### 3.5. Controllers & Security

- **`ProductSearchController`** (`/api/search`):
  - API công khai (không cần đăng nhập):
    - `GET /products/featured`: Lấy sản phẩm nổi bật.
    - `GET /products`: Tìm kiếm sản phẩm.
    - `GET /products/{productId}`: Chi tiết sản phẩm.
    - `GET /products/{productId}/reviews`: Danh sách review.
    - `GET /categories`: Danh sách danh mục.
    - `GET /seller/{sellerId}`: Profile gian hàng của Seller.
  - API cần xác thực JWT:
    - `POST /products/{productId}/reviews`: Gửi đánh giá mới. Validate quyền sở hữu và trạng thái hoàn thành của đơn hàng.
    - `POST /seller/{sellerId}/follow`: Theo dõi / bỏ theo dõi cửa hàng.

---

## 4. Các thành phần Frontend

- **Trang chủ (Homepage):**
  - File: `templates/home.html`. Hiển thị sản phẩm nổi bật.
- **Trang tìm kiếm sản phẩm:**
  - File: `templates/products.html` và JS `static/js/customer/products.js` (Hộp lọc và tìm kiếm động).
- **Trang chi tiết sản phẩm:**
  - File: `templates/product-detail.html`. Hiển thị ảnh, mô tả, biến thể, tồn kho và danh sách đánh giá.
- **Trang xem Shop đối tác:**
  - File: `templates/shop.html`. Hiển thị thông tin gian hàng và nút Theo dõi/Follow.

---

## 5. Definition of Done

- Chỉ cho phép đánh giá khi giao dịch mua sản phẩm này ở trạng thái `Completed` hoặc `Held` và giao dịch đó chưa từng được đánh giá trước đây.
- Khi người dùng bấm Theo dõi / Bỏ theo dõi, hệ thống không insert dòng mới liên tục mà thực hiện kiểm tra bản ghi cũ trong DB (bao gồm cả dòng đã xóa mềm `isDelete = 1`) để toggle cờ nhằm tránh vi phạm ràng buộc UNIQUE của cơ sở dữ liệu.
- Giá của sản phẩm và biến thể bắt buộc là kiểu số nguyên lớn `Long` / `BIGINT`.