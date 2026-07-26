# Tiêu chuẩn và Ngữ cảnh Dự án MMO Market (Dành cho AI Agent)

## 1. Tổng quan hệ thống (System Overview)
Hệ thống MMO Market là một sàn giao dịch thương mại điện tử chuyên dụng cho việc mua bán các sản phẩm và dịch vụ số (như tài khoản email, mạng xã hội, dịch vụ buff view, v.v.). Hệ thống được thiết kế để cung cấp môi trường giao dịch an toàn (trung gian Escrow), tự động hóa quá trình vận hành, và tích hợp chặt chẽ quy trình nạp/rút tiền tự động thông qua cổng thanh toán Sepay.

## 2. Phân quyền và vai trò (Roles)
Hệ thống bao gồm 4 Role chính với quyền hạn và nghiệp vụ riêng biệt:
1. **Admin (Quản trị viên):** Quản trị cấp cao. Phụ trách quản lý cấu hình hệ thống (bảo trì), cài đặt phí giao dịch/hoa hồng, quản lý tài khoản người dùng, phân quyền cho Staff và xem các báo cáo thống kê tăng trưởng, dòng tiền toàn hệ thống.
2. **Staff (Nhân viên vận hành):** Xử lý nghiệp vụ thực thi hàng ngày. Bao gồm quản lý/duyệt KYC người dùng, xử lý lệnh nạp/rút tiền thủ công, tiếp nhận và phân xử các khiếu nại (dispute) giữa Customer và Seller, và cắm cờ (Flag) các shop vi phạm.
3. **Seller (Người bán):** Đăng bán và quản lý sản phẩm số, cấu hình gian hàng, xem thống kê doanh thu, quản lý ví (rút tiền về ngân hàng), cập nhật KYC và tương tác/giải quyết khiếu nại với khách hàng.
4. **Customer (Khách hàng):** Tìm kiếm sản phẩm, quản lý giỏ hàng, nạp tiền vào ví, thanh toán đơn hàng, gửi đánh giá (feedback) và báo cáo (report) khiếu nại sản phẩm/shop.

Ngoài ra còn có **Guest** (Khách vãng lai chưa đăng nhập) và các hệ thống bên ngoài (**Google OAuth2**, **Sepay**, **Gmail**).

## 3. Các bài toán kinh tế & Vận hành giải quyết
- **Bảo vệ an toàn dòng tiền:** Hệ thống đóng vai trò trung gian giữ tiền (Escrow) để chống gian lận thẻ (người mua nạp tiền rồi đòi lại) và chống lừa đảo từ người bán (bán thẻ/tài khoản đã qua sử dụng).
- **Tự động hóa (Automation):** Giảm tải 90% thời gian nhân sự can thiệp thủ công, hỗ trợ giao hàng tức thì 24/7.
- **Data Security:** Mã hóa một chiều toàn bộ thông tin sản phẩm số trong Database, tự động ghi Audit Logs chi tiết để giám sát.
- **Xác thực (KYC) & Chống rửa tiền:** Yêu cầu xác minh CCCD/Bằng lái xe với người dùng có giao dịch hạn mức lớn.
- **RBAC (Phân quyền):** Phân tách quyền hạn nghiêm ngặt, ngăn chặn nhân viên lạm quyền gian lận số dư.

## 4. Các module hệ thống nền (Non-UI Functions)
- **Authentication & Security:** Login/Logout, Google OAuth2, Quản lý Session, Mã hóa mật khẩu, 2FA, Phục hồi mật khẩu.
- **Payment & Wallet:** Tích hợp Sepay (Deposit/Withdrawal), Xử lý giao dịch nội bộ, Refund.
- **Notification:** Gửi thông báo hệ thống, Gửi Email qua SMTP/Third-party.
- **KYC & Withdrawals:** Xử lý luồng duyệt KYC và Rút tiền.
- **Dispute & Support:** Hệ thống Ticket khiếu nại, Chat thời gian thực.
- **Monitoring:** Nhật ký hệ thống (Audit), Phát hiện gian lận (Fraud Detection), Tự động dọn dẹp dữ liệu rác (Scheduled Cleanup).

## 5. Hướng dẫn lập trình (Guidelines cho Agent)
- **Giao dịch tài chính:** Bắt buộc phải kiểm tra số dư ví (Balance Validation) trước khi thực hiện mua hàng, trừ tiền hoặc tạo lệnh rút. Đảm bảo tính nhất quán dữ liệu (Data consistency).
- **Security:** Luôn áp dụng phân quyền API theo Role. Mọi hành động của Admin và Staff cần ghi lại vào hệ thống Log (Audit Logging Service).
- **External API:** Xử lý callback từ Sepay cần validate cẩn thận để tránh fake request.
