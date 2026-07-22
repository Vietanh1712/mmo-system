# PLAN — Product Catalog & Reviews Management (`feat-product`)

## 1. Mục tiêu (Goals)

Triển khai danh mục sản phẩm số (Product Catalog), tìm kiếm lọc phân trang và luồng đánh giá (Reviews) sau mua hàng theo đặc tả `SPEC.md` (feat-product). Hệ thống cho phép:
- Quản lý danh mục (Categories), sản phẩm (Products) và biến thể sản phẩm (Product Variants) như gói tháng, gói năm.
- Cho phép khách xem hàng và tìm kiếm nâng cao (theo từ khóa, giá cả, danh mục, xếp hạng).
- Theo dõi/bỏ theo dõi cửa hàng (Follow Shop) để cập nhật tin tức.
- Viết đánh giá kèm sao (1-5) và bình luận từ Khách hàng. Đảm bảo ràng buộc nghiệp vụ: chỉ những tài khoản đã mua sản phẩm thành công (đơn hàng ở trạng thái `Completed` hoặc `Held`) mới được phép đánh giá, và mỗi giao dịch mua chỉ được đánh giá một lần.
- Kiểm soát phân quyền đăng bán dựa trên **Shop Level và Số dư ví**:
  - Chặn đăng bán sản phẩm mới, thêm biến thể hoặc cập nhật biến thể đối với Shop Level 0 và 1 nếu số dư ví âm.
  - Giới hạn giá trần biến thể tối đa 200,000 VNĐ cho Shop Level 1 (Shop Mới).
  - Giới hạn tối đa 5 sản phẩm active hiển thị đồng thời cho Shop Level 0 (Cảnh cáo).
- Tự động ẩn sản phẩm của các shop bị Locked (khóa do ví âm), Banned (bị cấm), hoặc Pending (chờ duyệt) khỏi catalog công khai và các danh sách sản phẩm nổi bật/fallback trang chủ.

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
  - `getFeaturedProducts(limit)`: Trích xuất các sản phẩm nổi bật dựa trên số lượng đơn hàng hoàn thành thực tế từ DB, tự động lọc bỏ các shop có trạng thái Locked, Banned hoặc Pending.
- **`ProductSearchService`** (Sử dụng `ProductSpecification`):
  - `searchProducts(...)`: Tìm kiếm phân trang lọc nâng cao theo từ khóa, mức giá, danh mục, xếp hạng. Tự động loại trừ các shop bị Locked, Banned hoặc Pending.

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

- **`SellerController`** (`/api/seller`):
  - API cần xác thực JWT và vai trò Seller:
    - `POST /products`: Đăng sản phẩm mới và tạo các biến thể ban đầu. Kiểm tra giới hạn ví âm của Level 0/1, giới hạn số lượng sản phẩm hoạt động của Level 0 (< 5), và giới hạn giá biến thể của Level 1 (<= 200,000 VNĐ).
    - `POST /variants`: Thêm biến thể cho sản phẩm hiện có. Kiểm tra giới hạn ví âm và giới hạn giá Level 1.
    - `PUT /variants/{id}`: Cập nhật thông tin biến thể. Kiểm tra giới hạn ví âm và giới hạn giá Level 1.
    - `POST /digital-assets`: Nhập kho tài nguyên kỹ thuật số theo lô. Tính toán lại tồn kho khả dụng cho biến thể (trừ loại dịch vụ SERVICE).

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
- Chặn hành động đăng bán sản phẩm mới, thêm biến thể hoặc cập nhật biến thể của các Shop Level 0/1 có số dư ví âm.
- Giới hạn giá trần biến thể tối đa 200,000 VNĐ cho Shop Level 1 và chặn đăng bán nếu vi phạm.
- Giới hạn tối đa 5 sản phẩm hiển thị hoạt động đồng thời cho Shop Level 0 và chặn đăng bán nếu vượt quá.
- Lọc bỏ và ẩn toàn bộ sản phẩm của các shop bị Locked (khóa ví âm), Banned (cấm hoạt động) hoặc Pending (chờ duyệt) khỏi kết quả tìm kiếm, catalog, trang chủ và các danh sách sản phẩm nổi bật/bán chạy.