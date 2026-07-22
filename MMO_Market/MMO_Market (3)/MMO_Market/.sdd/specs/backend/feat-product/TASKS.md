# TASKS — Product & Categories Management (`feat-product`)

> **Feature ID:** `feat-product` | **UC Coverage:** UC-05 (Product Discovery), UC-06 (Shop Product Management), UC-11 (Feedback & Reviews)
> **Phiên bản:** 1.0 | **Cập nhật:** 2026-06-27

---

## Phase 1: Database & Entities

- [x] **1.1** Tạo lập các bảng `Categories`, `Products`, `ProductVariants`, `Reviews`, và `ShopFollowers` trong DB.
- [x] **1.2** JPA Entity `Product` và `ProductVariant` — map đúng quan hệ một-nhiều, quản lý tồn kho và giá tiền dạng `Long`.
- [x] **1.3** JPA Entity `DigitalAsset` — thiết lập trường `assetContent` lưu trữ nội dung nhạy cảm của sản phẩm kỹ thuật số.

## Phase 2: Repositories

- [x] **2.1** `ProductRepository` — hỗ trợ tìm kiếm nâng cao với `ProductSpecification` để lọc sản phẩm động.
- [x] **2.2** `ShopFollowerRepository` — truy vấn thông tin theo dõi cửa hàng để tránh lỗi trùng lặp khi theo dõi lại.

## Phase 3: DTOs & Validation

- [x] **3.1** `ProductRequestDTO` / `VariantRequestDTO` — cấu hình validation bắt buộc nhập tên sản phẩm, giá tiền lớn hơn 0.

## Phase 4: Business Logic (Services)

- [x] **4.1** `ProductService` — mã hóa đối xứng AES-256 các thông tin `DigitalAsset` trước khi ghi vào database nhằm bảo mật mã code sản phẩm.
- [x] **4.2** `ProductSearchService` — xử lý lọc sản phẩm chỉ trả về các bản ghi hoạt động (`isDelete = 0`).
- [x] **4.3** `ProductSpecification` — lọc bỏ và loại trừ sản phẩm của người bán có trạng thái `shopStatus` là `Locked`, `Banned`, hoặc `Pending`.
- [x] **4.4** `ProductRepository` & `ProductService` — loại trừ sản phẩm của shop bị Locked, Banned hoặc Pending khỏi danh sách sản phẩm nổi bật/bán chạy và fallback trang chủ.

## Phase 5: Controllers & Security

- [x] **5.1** `ProductSearchController` — các API tìm kiếm công khai và API follow Shop.
- [x] **5.2** Cấu hình bảo vệ quyền cập nhật sản phẩm chỉ cho phép Seller sở hữu sản phẩm đó thực hiện.
- [x] **5.3** `SellerController` (API `/products`, `/variants`, cập nhật biến thể) — chặn đăng bán đối với Shop Level 0/1 khi ví âm.
- [x] **5.4** `SellerController` — kiểm tra giới hạn giá trần 200,000 VNĐ đối với Shop Level 1 và giới hạn tối đa 5 sản phẩm hiển thị đối với Shop Level 0.

## Phase 6: Testing

- [x] **6.1** Unit Tests `ProductServiceTest` — kiểm chứng thành công chức năng mã hóa và giải mã thông tin tài sản số an toàn.