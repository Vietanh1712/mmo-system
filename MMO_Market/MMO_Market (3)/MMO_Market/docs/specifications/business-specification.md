---
title: Business Specification
status: active
owner: Product Team
last_updated: 2026-06-18
---

# Business Specification

## Mục tiêu

MMO Market là nền tảng C2C cho sản phẩm số, tập trung vào:

- Giao dịch bằng VNĐ.
- Xác minh Seller.
- Escrow 72 giờ.
- Quản lý khiếu nại và vận hành Staff.
- Quản trị tài khoản, phí và hệ thống.

Tài liệu này mô tả hành vi mong muốn, không định nghĩa schema vật lý. Schema phải đối chiếu [Database Reference](../database/schema-reference.md), migration và database đang chạy.

## Roles

### Guest

- Xem trang chủ, danh mục, tìm kiếm và chi tiết sản phẩm.
- Đăng ký, đăng nhập và khôi phục mật khẩu.
- Chức năng yêu cầu tài khoản phải chuyển đến Login.

### Customer

- Quản lý hồ sơ, KYC, bảo mật và thông báo.
- Nạp tiền, xem ví và lịch sử giao dịch.
- Mua hàng, xem đơn, đánh giá và khiếu nại.
- Đăng ký mở Shop khi đủ điều kiện.

### Seller

- Có toàn bộ khả năng mua hàng của Customer.
- Truy cập Seller Portal.
- Quản lý Shop, sản phẩm, biến thể và kho tài sản số.
- Xem giao dịch bán, thống kê, khiếu nại và yêu cầu rút tiền.

### Staff

- Duyệt KYC.
- Xử lý khiếu nại.
- Kiểm tra giao dịch và yêu cầu rút tiền.
- Quản lý cờ cảnh báo Shop.
- Trao đổi với người dùng.

### Admin

- Quản lý tài khoản và Staff.
- Quản lý phân quyền, cấu hình, phí và hoa hồng.
- Xem audit log, doanh thu và dòng tiền.
- Phát thông báo và quản lý trạng thái vận hành.

## Authentication

- Đăng ký bằng email/password.
- Xác thực email bằng OTP.
- Đăng nhập hệ thống hoặc Google OAuth2.
- Cấp access token và refresh token.
- Quên mật khẩu phải xác minh OTP trước khi đặt lại.
- Backend phải kiểm tra authentication và authorization; frontend chỉ hỗ trợ UX.

## Seller Registration

1. Customer hoàn tất hồ sơ và KYC.
2. Customer gửi yêu cầu mở Shop.
3. Staff duyệt hoặc từ chối.
4. Khi được duyệt, tài khoản có khả năng Seller và mở Seller Portal.

Hiện màn đăng ký Shop vẫn là frontend mock; persistence và approval API cần hoàn thiện.

## Product Management

- Chỉ Seller được tạo hoặc sửa sản phẩm thuộc sở hữu của mình.
- Sản phẩm thuộc một Category và có nhiều ProductVariant.
- Giá dùng VNĐ (`BIGINT`).
- Stock không được âm.
- DigitalAsset phải gắn đúng Variant và chỉ được giao một lần.
- Xóa sản phẩm/variant là soft delete.

## Search And Discovery

- Search công khai theo keyword, category, price, stock và rating khi được hỗ trợ.
- Chỉ hiển thị sản phẩm chưa xóa và Shop được phép hoạt động.
- Homepage hiển thị sản phẩm nổi bật qua API.
- Card sản phẩm dẫn đến Product Detail; thao tác Cart không làm mất điều hướng.

## Purchase And Escrow

1. Customer chọn ProductVariant.
2. Hệ thống xác thực Customer, Seller, giá, stock và số dư.
3. Trong một transaction:
   - Lock dữ liệu cần thiết.
   - Trừ số dư Customer.
   - Giảm stock.
   - Tạo Transaction.
   - Gán `escrow_release_date = created_at + 72 giờ`.
4. Seller chưa nhận tiền khả dụng trước khi escrow được giải phóng.

Các lỗi chính:

- `INSUFFICIENT_FUNDS`
- `OUT_OF_STOCK`
- `PRODUCT_UNAVAILABLE`
- `FORBIDDEN`

## Wallet And Top-up

- Số dư dùng VNĐ.
- Top-up qua SePay phải chống xử lý webhook trùng.
- Mỗi thay đổi số dư phải có audit/history phù hợp.
- Không tin amount từ frontend khi xử lý callback thanh toán.

## Withdrawal

- Chỉ Seller có thông tin ngân hàng hợp lệ được gửi yêu cầu.
- Số tiền phải trong hạn mức và không vượt số dư khả dụng.
- Thay đổi số dư phải nằm trong transaction.
- Staff duyệt/từ chối và lưu bằng chứng chuyển khoản khi hoàn tất.

## Complaint

- Customer chỉ khiếu nại giao dịch thuộc sở hữu.
- Khiếu nại mở trước khi escrow được giải phóng phải giữ dòng tiền.
- Customer, Seller và Staff được trao đổi trong đúng complaint.
- Kết quả Staff:
  - Customer thắng: hoàn tiền.
  - Seller thắng: giải phóng escrow theo phí hệ thống.

## Notifications

- Notification Center hiển thị thông báo cá nhân và hệ thống.
- Chỉ đánh dấu đã đọc khi người dùng mở thông báo hoặc chọn “Đánh dấu tất cả”.
- Broadcast hiện là frontend mock dựa trên browser storage; production cần API và database.

## Non-functional Requirements

- Tác vụ tài chính đảm bảo ACID.
- Password dùng BCrypt.
- Secret lấy từ environment variables.
- API trả HTTP status và error body nhất quán.
- Mọi luồng nhạy cảm có RBAC và ownership.
- UI responsive, dùng Roboto và Design System chung.

## Known Gaps

- RBAC cho một số MVC route chưa hoàn chỉnh.
- Một số màn Staff/Admin/Seller vẫn là prototype.
- Notification broadcast và Shop registration chưa persistence đầy đủ.
- `ddl-auto=update` cần được thay bằng migration + validation trước production.
