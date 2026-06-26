# MMO Market Constitution
**Version:** 1.0
**Project:** MMO Market

This document records the foundational, non-negotiable architectural and operational decisions for the MMO Market project. All subsequent specifications, features, and code MUST strictly adhere to these rules. Any changes to this document require overarching consensus and major version bump.

## 1. System Roles (4 Roles)
- **Guest / Customer (Khách / Khách hàng):** Duyệt sản phẩm, đăng ký, đăng nhập, nạp tiền, mua hàng, gửi khiếu nại.
- **Seller (Người bán):** Đăng ký mở shop, đăng bán sản phẩm (mã hóa số), phản hồi khiếu nại, rút tiền.
- **Staff (Nhân viên vận hành):** Duyệt KYC, phân xử khiếu nại (hoàn/chuyển tiền), giám sát giao dịch, cắm cờ gian lận.
- **Admin (Quản trị viên):** Quản lý cấu hình toàn cục (phí, hoa hồng, bảo trì), phân quyền Staff, xem báo cáo doanh thu.

## 2. Core Tech Stack
- **Frontend Layer:** Hệ thống Client-Side sử dụng giao diện Web/REST API cho các Portal.
- **Backend Stack:** Java 17+ kết hợp Spring Boot 3.x.
- **Database Engine:** **SQL Server (T-SQL)**. Tuyệt đối KHÔNG viết cú pháp của MySQL hay PostgreSQL.
- **Authentication & AuthZ:** Cơ chế JWT Token và Google OAuth2. 
- **Notification:** Dịch vụ SMTP qua Gmail (để gửi OTP).

## 3. Financial & Currency Constraints (MUST OBEY)
- **Currency:** Sử dụng VNĐ. KHÔNG sử dụng các hệ thống Coin ảo, xu, hoặc điểm thưởng trung gian.
- **Data Type:** Giá trị tiền tệ sử dụng kiểu `BIGINT` trong CSDL.
- **Balance Separation:** Bảng `Users` (Wallet) phải tách biệt rõ hai cột: `Available Balance` (Khả dụng) và `Hold/Frozen Balance` (Đóng băng).

## 4. Architectural Rules
- **Clean Architecture:** Áp dụng nghiêm ngặt Controller - Service - Repository (3-layer architecture).
- **No Soft Deletion Bypassing:** Mọi thực thể quan trọng (Users, Products, Categories, v.v.) phải dùng cờ `isDelete BIT DEFAULT 0`.
- **Database Transaction (ACID):** Mọi tác vụ liên quan đến số dư ví (nạp, rút, mua, hoàn tiền) hoặc thay đổi stock kho hàng PHẢI được bọc trong `@Transactional`. Tránh Race Condition.
- **Escrow Mechanism (Giam tiền):** Tiền thanh toán luôn bị giam trong 3 ngày (72 giờ). Trường `escrow_release_date` của `Transactions` quy định mốc thời gian này. Hệ thống không cộng tiền ngay cho Seller.
- **SQL Server Triggers:** Sử dụng cấu trúc set-based kết hợp hai bảng ảo `inserted` và `deleted`. KHÔNG dùng row-by-row triggers.
- **Data Security:** Nội dung (content) của sản phẩm số yêu cầu thiết kế mã hóa một chiều để chống Data Leak.

## 5. Security & Configuration
- **No Hardcoded Secrets:** Mọi Key, Secret, Client ID, DB Connection String đều phải đọc qua Environment Variables.
- **Endpoint Securing:** Tất cả endpoints liên quan đến tài chính và thông tin cá nhân cần check RBAC rõ ràng.
