# PLAN — Seller Console & Shop Management (`feat-seller`)

## 1. Mục tiêu (Goals)

Triển khai quy trình đăng ký gian hàng trực tuyến (Shop) dành cho người dùng đã KYC (`isVerified = 1`) và xây dựng hệ thống quản lý kênh bán hàng (Seller Console) theo đặc tả `SPEC.md` (feat-seller). Các nghiệp vụ hỗ trợ:
- Gửi hồ sơ đăng ký mở gian hàng (tên Shop, mô tả, thông tin ngân hàng thụ hưởng).
- Quản lý kênh bán hàng (Seller Console): đăng bán và cập nhật sản phẩm số, quản lý tồn kho, quản lý đơn hàng bán được, xem thống kê doanh thu và quản lý thông tin ví (rút tiền).

## 2. Kiến trúc & Công nghệ

- **Backend:** Java 17, Spring Boot 3.1, Spring Data JPA, SQL Server (T-SQL).
- **Frontend:** Thymeleaf template engine kết hợp CSS & JS thuần (giao tiếp qua REST API bằng `authFetch`).
- **Tuân thủ:** Mô hình phân lớp Controller → Service → Repository → Entity; DTO Pattern cho dữ liệu đăng ký; phân quyền nghiêm ngặt: chỉ tài khoản đã xác minh KYC (`isVerified = 1`) mới được đăng ký shop, và chỉ tài khoản có role `SELLER` mới được phép truy cập Seller Console.

## 3. Các thành phần Backend

### 3.1. Database Migration & Entities

- **Entity `SellerRegistration`** (bảng `SellerRegistrations`):
  - `userId` (ManyToOne -> User): Người gửi yêu cầu.
  - `shopName` (NVARCHAR): Tên gian hàng.
  - `description` (NVARCHAR): Mô tả gian hàng.
  - `status` (VARCHAR): Trạng thái xét duyệt (`PENDING`, `APPROVED`, `REJECTED`).
  - `isDelete` (BIT): Soft delete.
- **Entity `SellerBankInfo`** (bảng `SellerBankInfo`):
  - `userId` (ManyToOne -> User): Liên kết tới Seller.
  - `bankName` (NVARCHAR): Tên ngân hàng thụ hưởng.
  - `accountNumber` (VARCHAR): Số tài khoản ngân hàng.

### 3.2. Repositories (Spring Data JPA)

- `SellerRegistrationRepository`:
  - `findByUserIdAndIsDeleteFalse(userId)`: Lấy thông tin đăng ký của user.
  - `existsByUserIdAndStatusAndIsDeleteFalse(userId, status)`: Kiểm tra đăng ký trùng lặp.
- `SellerBankInfoRepository`:
  - `findByUserIdAndIsDeleteFalse(userId)`: Tìm cấu hình tài khoản ngân hàng của Seller.

### 3.3. DTOs

- Request: `ShopRegistrationRequest` (gồm `shopName`, `description`, `bankName`, `accountNumber`).
- Response: `ShopRegistrationResponse`. Mapping ở Service.

### 3.4. Services (Business Logic)

- **`ShopRegistrationService`**:
  - `registerShop(userId, request)`:
    - Xác thực User đã KYC thành công (`isVerified = 1`).
    - Kiểm tra xem đã có hồ sơ đăng ký shop nào đang chờ xử lý (`PENDING`) hoặc đã được duyệt (`APPROVED`) không.
    - Lưu thông tin đăng ký `SellerRegistration` và thông tin ngân hàng `SellerBankInfo`.
    - Trừ phí đăng ký shop nếu có quy định, cập nhật vai trò người dùng thành `SELLER` khi duyệt thành công.

### 3.5. Controllers & Security

- **`ShopRegistrationController`** (`/api/v1/shop-registrations`):
  - `POST /`: Đăng ký mở shop (`@PreAuthorize("hasAuthority('ROLE_CUSTOMER')")`).
  - `GET /me`: Lấy thông tin đăng ký shop cá nhân.
- **`SellerController`** (MVC `@RequestMapping("/seller")`):
  - Render giao diện Seller Console (`/dashboard`, `/products`, `/orders`, `/wallet`, `/settings`).
  - Được bảo vệ bằng `@PreAuthorize("hasRole('SELLER')")` tại class level hoặc method level.

---

## 4. Các thành phần Frontend

- **Trang đăng ký Shop của Customer:**
  - File: `templates/account/register-shop.html` và JS `static/js/customer/account-register-shop.js`.
  - Hiển thị form điền tên Shop và thông tin tài khoản ngân hàng. Gọi API `POST /api/v1/shop-registrations` (thực tế thông qua endpoint `/v1/shop-registrations` trên `authFetch`).
- **Giao diện Seller Console (Bảng điều khiển người bán):**
  - File: `templates/seller/dashboard.html`, `templates/seller/products.html` và các template liên quan.

---

## 5. Definition of Done

- Chỉ cho phép người dùng có `isVerified = 1` đăng ký Shop.
- Khi phê duyệt đăng ký thành công, role của User trong DB phải được cập nhật sang `SELLER` và xoá cache / cập nhật Token tương ứng.
- Toàn bộ endpoint `/seller/**` bắt buộc phải kiểm duyệt quyền hạn `ROLE_SELLER` chặt chẽ.