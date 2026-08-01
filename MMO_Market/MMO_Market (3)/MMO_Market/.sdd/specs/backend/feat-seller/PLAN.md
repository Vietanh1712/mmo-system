# PLAN — Seller Console & Shop Management (`feat-seller`)

## 1. Mục tiêu (Goals)

Triển khai quy trình đăng ký gian hàng trực tuyến (Shop) tự động dành cho người dùng đã KYC (`isVerified = 1`) và xây dựng hệ thống quản lý kênh bán hàng toàn diện (Seller Console). Các nghiệp vụ hỗ trợ bao gồm:
- Tự động duyệt hồ sơ đăng ký mở gian hàng và nâng cấp tài khoản lên `SELLER`.
- Cung cấp Dashboard theo dõi doanh thu, khiếu nại, và đơn hàng hoàn thành.
- Quản lý kênh bán hàng: đăng bán và cập nhật sản phẩm số (Products), quản lý biến thể (Variants), và quản lý tồn kho (Digital Assets).
- Theo dõi giao dịch bán hàng (Transactions), số dư tạm giữ, và thống kê doanh thu (Statistics).
- Tích hợp tính năng tạo yêu cầu Rút tiền (Withdrawals) và yêu cầu gửi mã OTP bảo mật.
- Quản lý trạng thái hoạt động của Shop (`Active` / `Suspended`).
- Đọc thông báo lỗi/vi phạm từ Staff (Shop Flags) và xem các Đánh giá (Reviews), Khiếu nại (Complaints) của khách hàng.

## 2. Kiến trúc & Công nghệ

- **Backend:** Java 17, Spring Boot 3.1, Spring Data JPA, SQL Server (T-SQL).
- **Frontend:** Thymeleaf template engine kết hợp CSS & JS thuần (giao tiếp qua REST API bằng `authFetch`).
- **Tuân thủ:** Mô hình phân lớp Controller → Service → Repository → Entity; phân quyền nghiêm ngặt: chỉ tài khoản đã xác minh KYC (`isVerified = 1`) mới được đăng ký shop, và chỉ tài khoản có role `SELLER` mới được phép truy cập Seller Console.

## 3. Các thành phần Backend

### 3.1. Database Migration & Entities

- **Entity `SellerRegistration`** (bảng `SellerRegistrations`):
  - `userId` (ManyToOne -> User): Người gửi yêu cầu.
  - `shopName` (NVARCHAR): Tên gian hàng.
  - `description` (NVARCHAR): Mô tả gian hàng.
  - `status` (VARCHAR): Trạng thái xét duyệt (`APPROVED`).
- **Entity `SellerBankInfo`** (bảng `SellerBankInfo`):
  - Thông tin ngân hàng của Seller (`bankName`, `accountNumber`, `branch`).

### 3.2. Repositories (Spring Data JPA)

- `SellerRegistrationRepository`, `SellerBankInfoRepository`
- Các Repository phục vụ thống kê & quản lý tài nguyên Seller: `ProductRepository`, `ProductVariantRepository`, `DigitalAssetRepository`, `TransactionRepository`, `WithdrawalRepository`, `ComplaintRepository`, `ShopFlagRepository`, `ReviewRepository`, `ChatRepository`.

### 3.3. Services (Business Logic)

- **`ShopRegistrationService`**:
  - `submitRegistration(userId, request)`: Xác thực User đã KYC, tạo bản ghi `SellerRegistration` với trạng thái `APPROVED`, đồng thời cập nhật role `SELLER` và `shopStatus` thành `Active`.
  - Quản lý đóng/mở Shop tạm thời (`toggleShopStatus`, `updateShopStatus`) và cronjob khôi phục Shop bị khóa tạm thời (`autoRevertSuspendedShops`).
- **`WithdrawalService`** (từ `feat-wallet`): 
  - `requestWithdrawal(userId, amount, otp)`: Tạo yêu cầu rút tiền cho Seller.

### 3.4. Controllers & Security

- **`ShopRegistrationController`** (`/api/v1/shop-registrations`): API cho phép Customer tự động đăng ký mở shop.
- **`SellerController`** (`/api/seller/**`): Bao quát toàn bộ nghiệp vụ của Seller Console:
  - `/dashboard`, `/statistics`: Lấy thông số tổng quan và biểu đồ.
  - `/shop-info`, `/shop-status`: Quản lý thông tin và trạng thái cửa hàng.
  - `/products`, `/variants`, `/digital-assets`: Full CRUD cho quản lý danh mục sản phẩm số, biến thể, và kho tài khoản/key.
  - `/transactions`: Lịch sử giao dịch.
  - `/withdrawals`: Tạo yêu cầu rút tiền và lấy lịch sử rút tiền.
  - `/complaints`, `/reviews`, `/shop-flags`: Truy vấn dữ liệu tương tác với khách hàng và kiểm duyệt từ Admin.

## 4. Các thành phần Frontend

- **Trang đăng ký Shop của Customer:** Form đăng ký tự động xét duyệt (`POST /api/v1/shop-registrations`).
- **Giao diện Seller Console (Bảng điều khiển người bán):**
  - Các trang Dashboard, Quản lý sản phẩm, Rút tiền, Quản lý khiếu nại gọi trực tiếp đến API `/api/seller/**`.

## 5. Definition of Done

- Hệ thống đăng ký Shop duyệt tự động hoạt động ổn định và thay đổi chính xác Role người dùng.
- Tất cả API của `SellerController` (bao gồm CRUD Products, Variants, Assets, Withdrawals) được bảo mật chặt chẽ bằng Role `SELLER` và cơ chế kiểm tra sở hữu (Ownership check).