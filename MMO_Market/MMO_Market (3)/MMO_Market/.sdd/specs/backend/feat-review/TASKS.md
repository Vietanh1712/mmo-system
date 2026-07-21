# TASKS — Product Reviews & Ratings (`feat-review`)

> **Feature ID:** `feat-review` | **UC Coverage:** UC-17 (Product Review & Rating)
> **Phiên bản:** 1.0 | **Cập nhật:** 2026-07-16

---

## Phase 1: Database & Entities

- [x] **1.1** Tạo bảng `Reviews` lưu thông tin đánh giá (product_id, user_id, rating, comment, media_url, transaction_id).
- [x] **1.2** JPA Entity `Review` — Map đúng các trường dữ liệu và cấu hình quan hệ khóa ngoại `ManyToOne` đến `Product` và `User`.

## Phase 2: Repositories

- [x] **2.1** `ReviewRepository` — Viết các phương thức truy vấn lấy đánh giá theo sản phẩm, seller và đếm trung bình cộng rating của sản phẩm.

## Phase 3: Business Logic & Validation

- [x] **3.1** Kiểm duyệt số sao (chỉ cho phép từ 1 đến 5 sao).
- [x] **3.2** Ràng buộc lịch sử mua hàng: Người dùng bắt buộc phải có đơn hàng hoàn thành (`Completed` hoặc `Held`) mới được quyền đánh giá.
- [x] **3.3** Kiểm soát tính duy nhất: Mỗi giao dịch chỉ được đánh giá duy nhất một lần.

## Phase 4: Controllers & Security

- [x] **4.1** `ProductSearchController` — API POST `/api/products/{productId}/reviews` lưu trữ đánh giá mới và trả về DTO Response.
- [x] **4.2** Tích hợp hiển thị trung bình đánh giá và tổng lượt đánh giá trong API tìm kiếm/chi tiết sản phẩm.
