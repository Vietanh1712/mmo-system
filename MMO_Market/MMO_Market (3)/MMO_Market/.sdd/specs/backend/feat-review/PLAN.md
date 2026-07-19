# PLAN — Product Reviews & Ratings (`feat-review`)

## 1. Mục tiêu (Goals)

Triển khai tính năng **Đánh giá & Phản hồi sản phẩm (Reviews & Ratings)** theo đặc tả `SPEC.md` (feat-review). Hệ thống hỗ trợ:
- Cho phép Người mua (Customer) gửi đánh giá số sao (1-5), nhận xét và tệp hình ảnh đính kèm sau khi hoàn tất mua hàng.
- Ngăn chặn đánh giá ảo bằng cách kiểm tra bắt buộc lịch sử giao dịch tương ứng phải ở trạng thái thành công (`Completed` hoặc `Held`) và thuộc sở hữu của người đánh giá.
- Tích hợp điểm đánh giá trung bình hiển thị trong danh sách sản phẩm và chi tiết sản phẩm.

---

## 2. Kiến trúc & Công nghệ

- **Backend:** Java 17, Spring Boot 3.1, Spring Data JPA, SQL Server (T-SQL).
- **Frontend:** AJAX gọi API từ trang lịch sử giao dịch và chi tiết đơn hàng để gửi đánh giá.
- **Tuân thủ:** Mô hình phân lớp Controller → Repository → Entity; DTO Pattern cho Request/Response; Kiểm tra quyền hạn chặt chẽ.

---

## 3. Các thành phần Backend

### 3.1. Database Model & Entity (`Review`)

- **Bảng `Reviews`** lưu thông tin:
  - `product` (ManyToOne -> Product): Sản phẩm được đánh giá.
  - `user` (ManyToOne -> User): Khách hàng viết đánh giá.
  - `transactionId` (Long): Mã giao dịch liên quan để kiểm tra tính duy nhất.
  - `rating` (INT): Số sao (1 đến 5).
  - `comment` (NVARCHAR): Nhận xét dạng chữ.
  - `mediaUrl` (NVARCHAR): Link hình ảnh/video bằng chứng (nếu có).
  - `createdAt` (LocalDateTime): Ngày viết đánh giá.
  - `isDelete` (BIT): Cờ xóa mềm.

### 3.2. Repositories (Spring Data JPA)

- **`ReviewRepository`**:
  - `findByProductIdAndIsDeleteFalse(productId)`: Lấy các đánh giá của sản phẩm.
  - `findReviewsBySellerId(sellerId)`: Lấy toàn bộ đánh giá của tất cả các sản phẩm thuộc về một Seller.
  - `findByTransactionIdAndIsDeleteFalse(transactionId)`: Tìm đánh giá đã viết của một giao dịch cụ thể.
  - `existsByTransactionIdAndIsDeleteFalse(transactionId)`: Kiểm tra xem giao dịch đã được đánh giá chưa.

### 3.3. DTOs

- **`ReviewRequestDTO`**: Gồm `rating`, `comment`, `mediaUrl`, `transactionId`.
- **`ReviewResponseDTO`**: Gồm `id`, `userName`, `rating`, `comment`, `mediaUrl`, `createdAt`.

### 3.4. Controllers & Security

- **`ProductSearchController`** (`/api/products/{productId}/reviews`):
  - `POST /`: Lưu đánh giá của người dùng.
    - Kiểm tra đăng nhập và lấy `userId`.
    - Kiểm tra `rating` bắt buộc từ 1 đến 5 sao.
    - Tìm và kiểm tra User, Product tồn tại.
    - Nếu có `transactionId`: Validate giao dịch thuộc về user, đúng sản phẩm, trạng thái `Completed` hoặc `Held` và chưa từng được đánh giá trước đó.
    - Nếu không truyền `transactionId` (chế độ cũ tương thích ngược): Kiểm tra sự tồn tại của bất kỳ giao dịch hoàn thành nào giữa user và sản phẩm đó (`existsCompletedPurchaseByCustomerAndProduct`).
    - Lưu thực thể `Review` vào CSDL và trả về `ReviewResponseDTO` (HTTP 200 OK).

---

## 4. Các thành phần Frontend

- **Giao diện Lịch sử đơn hàng / Chi tiết giao dịch:**
  - Hiển thị nút "Viết đánh giá" nếu đơn hàng đã hoàn tất thành công và chưa được đánh giá trước đó (`isReviewed == false`).
  - Mở form popup cho người dùng chọn số sao, viết nhận xét và chèn link ảnh, sau đó gọi AJAX `POST /api/products/{productId}/reviews`.
- **Giao diện Trang chi tiết sản phẩm (`product-detail.html`):**
  - Hiển thị danh sách đánh giá của người dùng phía dưới sản phẩm.
  - Hiển thị điểm số trung bình (ví dụ: `4.8 ★ (12 đánh giá)`).
