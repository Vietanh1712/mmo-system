# MMO Market - Specification Index

Tài liệu này là mục lục hệ thống các luồng chức năng của dự án MMO Market. Các tài liệu được viết theo tiêu chuẩn EARS (Easy Approach to Requirements Syntax).

## 1. Core Documentation
- [Constitution (Quy định bất di bất dịch)](./00_CONSTITUTION.md)

## 2. Feature Specifications (EARS Style)
Hệ thống được chia thành 5 module chính dựa trên User Roles và Luồng nghiệp vụ:

### [01_AUTH: Authentication & Authorization](./01_AUTH/SPEC-01.md)
- Đăng ký (Local & Google OAuth2)
- Đăng nhập
- Xác thực OTP
- Quên mật khẩu
- Gửi yêu cầu định danh (KYC)

### [02_CUSTOMER: Customer Core Flows](./02_CUSTOMER/SPEC-02.md)
- Mua hàng (Purchase Digital Products)
- Nạp tiền ví (Top-up Wallet)
- Quản lý đơn hàng (My Orders)
- Đánh giá sản phẩm (Feedback/Review)
- Gửi khiếu nại (Create Complaint)

### [03_SELLER: Seller Core Flows](./03_SELLER/SPEC-03.md)
- Đăng ký mở Shop
- Quản lý Sản phẩm & Kho hàng (Mã hóa nội dung)
- Tạo Biến thể (Variants)
- Rút tiền (Withdraw)
- Phản hồi khiếu nại khách hàng

### [04_STAFF: Staff Operations](./04_STAFF/SPEC-04.md)
- Phân xử khiếu nại (Complaint Management)
- Giám sát giao dịch (Transaction Monitoring)
- Duyệt lệnh rút tiền (Withdrawal Management)
- Duyệt hồ sơ định danh (KYC Management)
- Cắm cờ gian lận (Flag Management)

### [05_ADMIN: System Administration](./05_ADMIN/SPEC-05.md)
- Cấu hình phí giao dịch & hoa hồng (Fees & Commissions)
- Bật/Tắt bảo trì hệ thống
- Thống kê doanh thu & tăng trưởng
- Quản lý phân quyền Staff (RBAC)
