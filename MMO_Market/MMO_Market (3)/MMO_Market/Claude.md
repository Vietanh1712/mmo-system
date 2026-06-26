# Tài liệu Đặc tả Use Case & Luồng Màn Hình (Dành cho Claude)

Tài liệu này chi tiết hóa các luồng Use Case, các tác nhân (Actors) và danh sách cấu trúc 50 màn hình UI của hệ thống MMO Market. Hãy sử dụng tài liệu này làm cơ sở thiết kế UI/UX, API, và logic Frontend/Backend.

## 1. Core Use Cases (Luồng nghiệp vụ chính)

### Nghiệp vụ Khách hàng (Customer)
- **Đăng ký (Register):** Hỗ trợ Email/Password (+ OTP) hoặc Google OAuth2.
- **Mua hàng (Purchase):** Chọn sản phẩm -> Chọn biến thể -> Bấm mua -> Hệ thống kiểm tra số dư ví (Coin) -> Nếu đủ tiền: Trừ ví, Giao thông tin sản phẩm tức thì, Đổi trạng thái đơn thành Completed. Nếu thiếu tiền: Báo lỗi.
- **Nạp tiền (Top-up):** Nhập số tiền -> Tạo QR Code/Lệnh thanh toán -> Thanh toán qua Sepay -> Hệ thống nhận Callback từ Sepay -> Tự động cộng tiền vào ví.
- **Khiếu nại (Complaint):** Tạo khiếu nại đơn hàng -> Tạm giữ tiền -> Chờ Seller phản hồi -> Staff phân xử (Hoàn tiền hoặc Chuyển cho Seller).

### Nghiệp vụ Người bán (Seller)
- **Quản lý sản phẩm:** Tạo sản phẩm -> Upload dữ liệu số -> Hệ thống mã hóa bảo mật -> Đăng bán.
- **Rút tiền (Withdraw):** Nhập số tiền (> 50.000) -> Kiểm tra số dư khả dụng -> Trừ số dư ví, Đưa vào trạng thái Pending -> Staff duyệt -> Cập nhật trạng thái Completed.

### Nghiệp vụ Vận hành (Staff & Admin)
- **Duyệt KYC:** Xác minh giấy tờ tùy thân của User để cấp quyền bán hàng / nạp rút số lượng lớn.
- **Quản lý Cờ (Flag Management):** Khóa/Cảnh báo các Shop có hành vi gian lận.
- **Admin Setup:** Cấu hình phí giao dịch, phí hoa hồng (Commission rate), quản lý Role của Staff.

## 2. Danh sách Màn Hình (50 Screens Flow)

Hệ thống được chia thành 4 Portal tương ứng với 4 nhóm người dùng:

### 2.1. Guest + Customer + User (Màu vàng/cam) - 24 Screens
1. Homepage (Trang chủ)
2. Sign In (Đăng nhập)
3. Forgot Password (Quên mật khẩu)
4. Sign Up (Đăng ký)
5. OTP Verification (Xác thực OTP)
6. Contact (Liên hệ)
7. My Profile (Hồ sơ cá nhân)
8. Change Information (Đổi thông tin)
9. My KYC (Hồ sơ định danh)
10. Send KYC (Gửi yêu cầu định danh)
11. My Order (Đơn hàng của tôi)
12. Feedback (Đánh giá)
13. Order Detail (Chi tiết đơn hàng)
14. My Wishlist (Sản phẩm yêu thích)
15. My Complaint (Khiếu nại của tôi)
16. Complaint Detail (Chi tiết khiếu nại)
17. My Notification (Thông báo)
18. Register Shop (Đăng ký mở Shop)
19. Top Up (Nạp tiền ví)
20. Category (Danh mục sản phẩm)
21. Product Detail (Chi tiết sản phẩm)
22. Confirm Order (Xác nhận đặt hàng)
23. Chat (Nhắn tin trực tiếp)
24. Shop (Trang gian hàng Seller)

### 2.2. Seller (Màu hồng) - 21 Screens
1. Shop Dashboard (Tổng quan doanh thu, đơn hàng)
2. Shop Info (Cài đặt thông tin Shop)
3. Close Shop (Đóng cửa gian hàng)
4. Top Up Money (Nạp tiền vào tài khoản Seller)
5. Shop Flag (Nhận cảnh báo vi phạm)
6. Withdraw (Yêu cầu rút tiền)
7. Withdrawal History Detail (Chi tiết lịch sử rút tiền)
8. Verify OTP (Xác nhận OTP)
9. Products (Quản lý sản phẩm & Kho hàng)
10. Edit Product (Chỉnh sửa sản phẩm)
11. Create Product (Đăng bán sản phẩm mới)
12. Product Detail (Xem trước sản phẩm)
13. Create Variant (Tạo biến thể giá/số lượng)
14. Update Variant (Sửa biến thể)
15. Add Account (Thêm tài khoản ngân hàng nhận tiền)
16. Update Account (Cập nhật tài khoản)
17. Transaction (Lịch sử giao dịch/Bán hàng)
18. Complaint Management (Quản lý khiếu nại từ khách)
19. Complaint Detail (Chi tiết/Phản hồi khiếu nại)
20. Review (Quản lý đánh giá từ khách)
21. Chat (Chat với Customer)

### 2.3. Staff (Màu xanh) - 12 Screens
1. Staff Dashboard (Tổng quan vận hành)
2. Withdrawal Management (Danh sách yêu cầu rút tiền)
3. Withdrawal Detail (Chi tiết lệnh rút & Tải biên lai)
4. Transaction Management (Giám sát dòng tiền)
5. Transaction Detail (Chi tiết giao dịch)
6. Complaint Management (Danh sách khiếu nại chờ xử lý)
7. Complaint Detail (Phân xử khiếu nại/Hoàn tiền)
8. KYC Management (Danh sách KYC chờ duyệt)
9. KYC Detail (Chi tiết hình ảnh CCCD & Duyệt)
10. Flag Management (Danh sách Shop bị cắm cờ)
11. Flag Detail (Cắm cờ vi phạm Shop/Sản phẩm)
12. Chat (Hỗ trợ người dùng)

### 2.4. Admin (Màu hồng nhạt) - 13 Screens
1. Admin Dashboard (Tổng quan toàn hệ thống)
2. Manage System Configuration (Bảo trì hệ thống)
3. Setup Transaction Fees (Cài đặt phí giao dịch)
4. Setup Commission Rates (Cài đặt phần trăm hoa hồng)
5. Manage Maintenance Mode (Bật/Tắt bảo trì)
6. System Statistics (Thống kê hệ thống)
7. View Revenue Reports (Báo cáo doanh thu)
8. View Cash Flow (Dòng tiền)
9. View Growth Charts (Biểu đồ tăng trưởng)
10. Manage Accounts (Quản lý/Khóa User)
11. Manage Staff Permissions (Quản lý phân quyền)
12. Assign Permissions (Cấp quyền Staff)
13. Edit Permissions (Chỉnh sửa quyền)

## 3. Kiến trúc Database khuyến nghị (Database Constraints)
- **Users Table:** Liên kết với bảng Ví (Wallet). Tách biệt số dư khả dụng (Available Balance) và số dư đóng băng (Hold/Frozen Balance) cho các khoản tiền đang bị khiếu nại hoặc chờ rút.
- **Products Table:** Yêu cầu thiết kế mã hóa một chiều cho nội dung sản phẩm số để tránh rò rỉ dữ liệu (Data Leak).
- **Transactions Table:** Ghi lại mọi biến động ví (Deposit, Withdraw, Purchase, Refund, Fee) dưới dạng Immutable ledger.
