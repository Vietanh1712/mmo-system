---
title: Database Schema Reference
status: active
owner: Backend Team
last_updated: 2026-06-18
source_of_truth: sql-server-and-migrations
---

# Database Schema Reference

## Phạm vi

Database sử dụng SQL Server. Tài liệu này là bản tham chiếu theo entity và script trong repository, không thay thế việc kiểm tra database đang chạy.

Dự án áp dụng **Database First**. Schema SQL Server và các migration đã áp dụng
là nguồn chuẩn. JPA entity, repository và code backend phải bám theo schema;
không dùng Hibernate để tự quyết định hoặc âm thầm thay đổi cấu trúc database.

Thứ tự xác minh schema:

1. SQL Server đang chạy.
2. Migration đã áp dụng trong `sql_scripts/`.
3. JPA entity/repository.
4. Tài liệu này.

Không ghi username, password hoặc secret vào tài liệu.

## Quy ước

- Primary key: `BIGINT`.
- Tiền: `BIGINT`, đơn vị VNĐ.
- Thời gian: `DATETIME`/`DATETIME2`.
- Soft delete: `isDelete BIT`.
- Tên bảng dùng PascalCase theo schema.
- Trigger xử lý set-based với `inserted` và `deleted`.

## Identity And Security

### Users

Thông tin tài khoản, role, trạng thái Shop và số dư.

Các trường quan trọng:

- `id`
- `email`
- `password`
- `full_name`
- `role`
- `phone`
- `shop_status`
- `balance_vnd`
- `permissions`
- `isVerified`
- `created_at`
- `isDelete`

### Authentications

Liên kết provider đăng nhập, FK tới `Users`.

### EmailVerifications

OTP xác thực email/khôi phục tài khoản, FK tới `Users`.

## Seller And KYC

### SellerRegistrations

Yêu cầu mở Shop:

- FK `user_id -> Users(id)`.
- Trạng thái: `Pending`, `Approved`, `Rejected`.
- Dùng soft delete.

### SellerBankInfo

Thông tin ngân hàng của Seller, FK tới `Users`.

## Catalog

### Categories

Danh mục sản phẩm; hỗ trợ cấu trúc cha/con nếu migration tương ứng đã áp dụng.

### Products

- FK tới Seller và Category.
- Tên, mô tả, hình ảnh/loại sản phẩm.
- Dùng soft delete.

### ProductVariants

- FK `product_id -> Products(id)`.
- `variant_name`, `price_vnd`, `stock`, `status`.
- Dùng soft delete.

### DigitalAssets

Kho tài sản số như account, key hoặc game card:

- FK tới ProductVariant.
- Có thể gắn Transaction khi đã giao.
- Không lưu hoặc trả credential nhạy cảm dưới dạng plaintext trong production.

## Finance

### TopupTransactions

Giao dịch nạp tiền qua SePay:

- FK tới User.
- `amount_vnd`, mã SePay và trạng thái.
- Mã callback phải unique/idempotent.

### Transactions

Giao dịch mua bán:

- FK Customer, Seller, Product và Variant.
- `amount_vnd`, commission, status.
- `escrow_release_date`.
- Trạng thái code hiện dùng gồm `Pending`, `Held`, `Completed`, `Refunded`, `Cancelled`, `Disputed`.

### Withdrawals

Yêu cầu rút tiền Seller:

- FK Seller và thông tin ngân hàng.
- Số tiền, trạng thái và proof.
- Hạn mức là cấu hình nghiệp vụ, không nên hardcode ở nhiều nơi.

### WalletTransactions

Lịch sử biến động số dư. Nếu bảng chưa tồn tại trong database mục tiêu, phải bổ sung qua migration trước khi code phụ thuộc.

## Operations

### Complaints

Khiếu nại gắn Transaction, Customer và Seller.

### Chats

Tin nhắn thường hoặc tin nhắn complaint giữa các bên.

### ShopFlags

Cảnh báo/khóa Shop do Staff xử lý.

### Reviews

Đánh giá sản phẩm, rating 1-5, FK User và Product.

### PreOrders

Yêu cầu đặt trước sản phẩm.

### Wishlists

Sản phẩm yêu thích của Customer. Nên có unique constraint `(customer_id, product_id)`.

## System

Các bảng được đặc tả hoặc dự kiến:

- `Notifications`
- `AuditLogs`
- `SystemConfigurations`
- `Commissions`

Phải xác minh tồn tại thực tế trước khi triển khai API phụ thuộc.

## Constraints Bắt Buộc

- Unique email cho User.
- Check amount và price không âm.
- Check stock không âm.
- Check rating từ 1 đến 5.
- FK phải có hành vi delete/update rõ ràng.
- Unique/idempotency key cho webhook thanh toán.
- Index cho FK và các trường search/filter thường dùng.

## Migration Rules

- Mỗi yêu cầu thay đổi database phải bàn giao một script T-SQL hoàn chỉnh, có
  thể chạy độc lập và lưu trực tiếp thành migration. Không bàn giao riêng các
  câu `ALTER` rời rạc thiếu ngữ cảnh.
- Script hoàn chỉnh phải có cấu trúc phù hợp với mức độ thay đổi:
  1. Header mô tả mục đích, phạm vi và dependency.
  2. Pre-check cho table, column, index, constraint hoặc dữ liệu liên quan.
  3. Schema change bằng T-SQL và data migration/backfill nếu cần.
  4. Verification query để kiểm tra schema và dữ liệu sau khi chạy.
  5. Rollback note hoặc rollback script cho thay đổi có rủi ro.
- Script phải được hiển thị hoặc gửi đầy đủ cho người review trước. Việc tạo
  script không đồng nghĩa với quyền chạy migration; chỉ thực thi khi task yêu
  cầu rõ ràng và đúng môi trường.
- Sau khi migration được chấp thuận hoặc áp dụng, entity/repository phải được
  đồng bộ theo schema mới. Không dựa vào `ddl-auto=update` để bù cho migration
  còn thiếu.
- Migration thay đổi schema đặt trong `sql_scripts/migration/` theo format:
  `YYYYMMDD_NNN_description.sql`.
  - `YYYYMMDD`: ngày tạo migration.
  - `NNN`: số thứ tự tăng dần trong ngày, bắt đầu từ `001`.
  - `description`: mô tả ngắn bằng tiếng Anh, viết lowercase `snake_case`.
- Tên file chỉ dùng chữ cái ASCII, chữ số và dấu gạch dưới; không dùng khoảng
  trắng, dấu tiếng Việt hoặc ký tự đặc biệt.
- Ví dụ hợp lệ:
  - `20260618_001_create_kyc_requests.sql`
  - `20260618_002_add_kyc_status_index.sql`
  - `20260618_003_drop_redundant_kyc_columns.sql`
  - `20260619_001_add_reviewed_at_to_kyc_requests.sql`
- Ví dụ không hợp lệ:
  - `more KYCRequest staff.sql`
  - `20260618-AddKYC.sql`
  - `fix database.sql`
- Script seed đặt tên `SEED_YYYYMMDD_NNN_description.sql`; script sửa dữ liệu
  có chủ đích đặt tên `FIX_YYYYMMDD_NNN_description.sql`. Seed và fix không
  được dùng thay cho migration thay đổi schema.
- Không đổi tên hoặc sửa nội dung migration đã được áp dụng ở môi trường dùng
  chung. Nếu cần điều chỉnh, tạo migration mới với version kế tiếp.
- Thứ tự chạy được xác định theo `YYYYMMDD_NNN`; không tạo hai file trùng
  version.
- Migration phải idempotent hoặc có điều kiện tồn tại rõ ràng.
- Không dùng `DROP` dữ liệu production nếu chưa có kế hoạch backup.
- Có query verification và rollback note khi thay đổi rủi ro cao.
- Production dùng migration và `ddl-auto=validate`; không dựa vào `ddl-auto=update`.

## Known Differences

- Business specification từng dùng tên `KYC_Requests`; code hiện dùng `SellerRegistrations`.
- Tên cột cũ như `wallet_balance` có thể khác entity hiện tại `balance_vnd`.
- Database name trong từng môi trường có thể khác; không hardcode tên môi trường vào tài liệu nghiệp vụ.
