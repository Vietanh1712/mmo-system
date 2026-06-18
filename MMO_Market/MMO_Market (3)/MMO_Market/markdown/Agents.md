# AGENTS.md — Ngữ cảnh Dự án dành cho AI Agents
# Phiên bản: 1.1 | Cập nhật: 2026-05-28 | Dự án: MMO Market

## 1. TỔNG QUAN DỰ ÁN
Tên dự án: MMO Market (Hệ thống Sàn giao dịch sản phẩm số C2C)
Loại hình: Web Application / REST API
Lĩnh vực: Thương mại điện tử sản phẩm số (Peer-to-Peer)
Giai đoạn: Đang phát triển hệ thống lõi (Core Development)

## 2. CÔNG NGHỆ SỬ DỤNG (TECH STACK — BẮT BUỘC TUÂN THỦ)
- **Backend Stack**: Java 17+ + Spring Boot 3.x (Spring Security, Spring Data JPA, Hibernate)
- **Cơ sở dữ liệu**: **SQL Server (T-SQL)**. Tuyệt đối KHÔNG viết cú pháp của MySQL hay PostgreSQL.
- **Xác thực & Ủy quyền**: JWT Token, Google OAuth2 làm cơ chế đăng nhập nhanh.
- **Dịch vụ thông báo**: Gmail SMTP (Gửi mã xác thực OTP).
- **Tích hợp ngoài**: Cổng thanh toán Sepay Gateway (Tiếp nhận Webhook nạp tiền tự động và gọi API giải ngân rút tiền VNĐ).

## 3. NGUYÊN TẮC KIẾN TRÚC & PHÂN PHỐI (ARCHITECTURE PRINCIPLES)
- **Mô hình kiến trúc**: Tuân thủ nghiêm ngặt mô hình 3 lớp Clean Architecture: Controller - Service - Repository.
- **Database Triggers**: SQL Server xử lý theo cơ chế tập dữ liệu (Set-based), không có cơ chế dòng lẻ `FOR EACH ROW`. Khi tạo trigger, PHẢI kết hợp với hai bảng ảo `inserted` và `deleted`.
- **Cơ chế Xóa mềm (Soft Delete)**: Tuyệt đối không dùng lệnh `DELETE` cứng đối với các thực thể cốt lõi (`Users`, `Products`, `ProductVariants`, `Categories`, `SellerBankInfo`). Luôn sử dụng cờ `isDelete BIT DEFAULT 0` và tự động thêm điều kiện `WHERE isDelete = 0` vào các câu lệnh `SELECT`.
- **Cơ chế Giam tiền (Escrow Hold)**: Tiền thanh toán đơn hàng sẽ bị hệ thống đóng băng trong bảng `Transactions` thông qua trường `escrow_release_date` đúng 3 ngày (72 giờ) để phòng ngừa rủi ro tranh chấp.

## 4. CẤU TRÚC THƯ MỤC & QUY TẮC ĐẶT TÊN (FILE NAMING & STRUCTURE)
- **Controllers**: PascalCase và có hậu tố Controller (VD: `TransactionController.java`)
- **Services**: PascalCase và có hậu tố Service (VD: `TransactionService.java`)
- **Repositories**: PascalCase và có hậu tố Repository (VD: `TransactionRepository.java`)
- **Đường dẫn API (Routes)**: Sử dụng kebab-case (VD: `/api/v1/product-variants`)
- **Tên bảng CSDL**: PascalCase trùng khớp với kịch bản T-SQL (VD: `ProductVariants`, `KYC_Requests`)

## 5. CÁC MẪU BỊ CẤM (FORBIDDEN PATTERNS — TUYỆT ĐỐI KHÔNG)
- KHÔNG BAO GIỜ sử dụng hệ thống Coin ảo hoặc điểm thưởng trung gian. Mọi giá trị giao dịch tính trực tiếp bằng tiền thật **VNĐ** với kiểu dữ liệu `BIGINT`.
- KHÔNG BAO GIỜ thực hiện các hàm thay đổi số dư ví (`wallet_balance`) hoặc số lượng kho hàng (`stock`) mà không có `@Transactional` để tránh lỗi Race Condition.
- KHÔNG BAO GIỜ hardcode các khóa bảo mật, API Key, Client Secret vào mã nguồn. Tất cả phải được đọc thông qua Biến môi trường (Environment Variables).

## 6. TIÊU CHÍ HOÀN THÀNH (DEFINITION OF DONE PER TASK)
- [ ] Phân quyền người dùng (RBAC) đã được kiểm tra rõ ràng tại tầng Controller hoặc Service.
- [ ] Các tác vụ tài chính được bọc trong Database Transaction để đảm bảo tính ACID.
- [ ] Đã viết Unit Test (JUnit 5 + Mockito) bao phủ các luồng Normal Flow và Exceptions.
- [ ] Các trường hợp lỗi biên (Edge Cases) được bắt ngoại lệ và trả về mã trạng thái HTTP chuẩn.

## 7. QUY TẮC GIT (GIT CONVENTIONS)
- Nhánh (Branch): `feat/[tên-tính-năng]` | `fix/[tên-lỗi]` | `spec/[tên-đặc-tả]`
- Định dạng commit: `[loại]: [phân-hệ] - [mô tả ngắn bằng tiếng Việt]`
- *Ví dụ: `feat: transaction - bo sung logic locking ngan chan race condition khi tru tien vi`*

## 8. NGỮ CẢNH SPRINT HIỆN TẠI (CURRENT SPRINT CONTEXT)
- **Sprint hoạt động**: Sprint 1
- **Trọng tâm**: Phát triển lõi hệ thống - Luồng Mua hàng, Giam tiền Escrow và Xử lý tranh chấp.
- **File đặc tả kích hoạt**: `.sdd/specs/feat-product-buying/SPEC.md`
