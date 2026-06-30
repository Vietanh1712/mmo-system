# CLAUDE.md — MMO Market Architecture Map & Guidelines

Tài liệu này chi tiết hóa bản đồ kiến trúc hệ thống, danh sách màn hình UI, các quyết định kiến trúc (ADR), bài học kinh nghiệm và Anti-patterns của hệ thống **MMO Market**. Hãy sử dụng tài liệu này làm cơ sở định hướng thiết kế và phát triển.

---

## 1. TỔNG QUAN KIẾN TRÚC & PHÂN TẦNG

Hệ thống áp dụng kiến trúc phân lớp tiêu chuẩn (**3-Layer Architecture**) cho Backend Spring Boot và phân tách rõ ràng với Frontend.

### 1.1. Sơ đồ Kiến trúc Phân tầng
```
┌────────────────────────────────────────────────────────────────────────┐
│                        FRONTEND PORTAL (Thymeleaf/HTML/CSS/JS)         │
│  - Render UI từ Thymeleaf template.                                     │
│  - Gọi REST API không đồng bộ qua JavaScript (Fetch/Axios).            │
│  - UX validation, UI state management.                                  │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │ (REST API / JSON)
                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        BACKEND (Spring Boot 3.1)                       │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │  Controller Layer (@RestController)                              │  │
│  │  - Tiếp nhận request, phân quyền sơ bộ, validate dữ liệu đầu vào. │  │
│  │  - Trả về ApiResponse<DTO> chuẩn, map lỗi qua Exception Handler.  │  │
│  └───────────────────────────────┬──────────────────────────────────┘  │
│                                  │
│                                  ▼
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │  Service Layer (@Service)                                        │  │
│  │  - Xử lý business logic nghiệp vụ chính (nạp/rút, escrow, mua bán).│  │
│  │  - Quản lý Transaction (@Transactional), kiểm tra quyền sở hữu.    │  │
│  └───────────────────────────────┬──────────────────────────────────┘  │
│                                  │
│                                  ▼
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │  Repository Layer (@Repository)                                  │  │
│  │  - Giao tiếp với cơ sở dữ liệu qua Spring Data JPA / Hibernate.   │  │
│  └───────────────────────────────┬──────────────────────────────────┘  │
│                                  │
│                                  ▼
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │  Entity Layer (@Entity)                                          │  │
│  │  - Ánh xạ trực tiếp với bảng cơ sở dữ liệu SQL Server.            │  │
│  │  - Tích hợp cờ soft delete isDelete.                             │  │
│  └──────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │
                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│                      DATABASE & SERVICES EXTERNAL                      │
│   - SQL Server (T-SQL) - Cơ sở dữ liệu quan hệ chính.                  │
│   - SePay Integration (Cổng tự động nhận tiền nạp).                    │
│   - Gmail SMTP (Gửi OTP, thông báo biến động tài khoản).              │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 2. CẤU TRÚC THƯ MỤC CODE THỰC TẾ

```
c:\Users\pc\MMO_new1\MMO_Market\MMO_Market (3)\MMO_Market\
├── apps/
│   ├── backend/                      # Source code Spring Boot (Java 17, Maven)
│   │   ├── src/main/java/com/mmo/
│   │   │   ├── MMOMarketApplication.java # Entry point (tự động Component Scan)
│   │   │   ├── shared/               # Lớp dùng chung (shared layer)
│   │   │   │   ├── model/            # Entities
│   │   │   │   ├── dal/              # Repositories (Spring Data JPA)
│   │   │   │   ├── security/         # Security configs, JWT filters
│   │   │   │   ├── config/           # Cấu hình hệ thống (WebConfig, JpaConfig)
│   │   │   │   ├── dto/              # DTOs dùng chung
│   │   │   │   └── mvc/              # Shared Thymeleaf MVC controllers
│   │   │   └── feature/              # Feature modules (phân chia theo lát cắt nghiệp vụ)
│   │   │       ├── auth/             # Login, register, profile
│   │   │       ├── kyc/              # KYC requests verification
│   │   │       ├── seller/           # Seller shop registration & dashboard
│   │   │       ├── product/          # Product list, categories, reviews, flags
│   │   │       ├── wallet/           # Topup, wallet, withdrawal
│   │   │       ├── order/            # Purchase orders, checkouts
│   │   │       ├── preorder/         # Pre-ordering features
│   │   │       ├── complaint/        # Transaction complaints
│   │   │       ├── support/          # Support tickets management
│   │   │       ├── chat/             # Direct messaging
│   │   │       ├── notification/     # In-app & email notifications
│   │   │       ├── staff/            # Staff moderation
│   │   │       ├── admin/            # Admin revenue reports & configurations
│   │   │       └── upload/           # File attachments upload
│   │   └── src/main/resources/       # application.properties
│   └── frontend/                     # Mã nguồn giao diện chính
│       ├── templates/                # Các file Thymeleaf (HTML)
│       └── static/                   # Assets (CSS, JS, Images)
├── docs/                             # Tài liệu đặc tả và thiết kế
└── .sdd/                             # Quy tắc phát triển và đặc tả SDD (SPEC, PLAN, TASKS)
```

---

## 3. CORE USE CASES & LUỒNG NGHIỆP VỤ

### Nghiệp vụ Khách hàng (Customer)
*   **Đăng ký (Register)**: Hỗ trợ Email/Password (+ OTP) hoặc Google OAuth2.
*   **Mua hàng (Purchase)**: Chọn sản phẩm -> Chọn biến thể -> Bấm mua -> Hệ thống kiểm tra số dư ví -> Nếu đủ tiền: Trừ ví, Giao thông tin sản phẩm tức thì, Đổi trạng thái đơn thành Completed. Nếu thiếu tiền: Báo lỗi.
*   **Nạp tiền (Top-up)**: Nhập số tiền -> Tạo QR Code/Lệnh thanh toán -> Thanh toán qua Sepay -> Hệ thống nhận Callback từ Sepay -> Tự động cộng tiền vào ví.
*   **Khiếu nại (Complaint)**: Tạo khiếu nại đơn hàng -> Tạm giữ tiền -> Chờ Seller phản hồi -> Staff phân xử (Hoàn tiền hoặc Chuyển cho Seller).

### Nghiệp vụ Người bán (Seller)
*   **Quản lý sản phẩm**: Tạo sản phẩm -> Upload dữ liệu số -> Hệ thống mã hóa bảo mật -> Đăng bán.
*   **Rút tiền (Withdraw)**: Nhập số tiền (> 50.000) -> Kiểm tra số dư khả dụng -> Trừ số dư ví, Đưa vào trạng thái Pending -> Staff duyệt -> Cập nhật trạng thái Completed.

### Nghiệp vụ Vận hành (Staff & Admin)
*   **Duyệt KYC**: Xác minh giấy tờ tùy thân của User để cấp quyền bán hàng / nạp rút số lượng lớn.
*   **Quản lý Cờ (Flag Management)**: Khóa/Cảnh báo các Shop có hành vi gian lận.
*   **Admin Setup**: Cấu hình phí giao dịch, phí hoa hồng (Commission rate), quản lý Role của Staff.

---

## 4. DANH SÁCH MÀN HÌNH (Screens Flow)

Hệ thống được chia thành 4 Portal tương ứng với các nhóm người dùng chính:

### 4.1. Guest + Customer + User (24 Screens)
1.  **Homepage** (Trang chủ)
2.  **Sign In** (Đăng nhập)
3.  **Forgot Password** (Quên mật khẩu)
4.  **Sign Up** (Đăng ký)
5.  **OTP Verification** (Xác thực OTP)
6.  **Contact** (Liên hệ)
7.  **My Profile** (Hồ sơ cá nhân)
8.  **Change Information** (Đổi thông tin)
9.  **My KYC** (Hồ sơ định danh)
10. **Send KYC** (Gửi yêu cầu định danh)
11. **My Order** (Đơn hàng của tôi)
12. **Feedback** (Đánh giá)
13. **Order Detail** (Chi tiết đơn hàng)
14. **My Wishlist** (Sản phẩm yêu thích)
15. **My Complaint** (Khiếu nại của tôi)
16. **Complaint Detail** (Chi tiết khiếu nại)
17. **My Notification** (Thông báo)
18. **Register Shop** (Đăng ký mở Shop)
19. **Top Up** (Nạp tiền ví)
20. **Category** (Danh mục sản phẩm)
21. **Product Detail** (Chi tiết sản phẩm)
22. **Confirm Order** (Xác nhận đặt hàng)
23. **Chat** (Nhắn tin trực tiếp)
24. **Shop** (Trang gian hàng Seller)

### 4.2. Seller (21 Screens)
1.  **Shop Dashboard** (Tổng quan doanh thu, đơn hàng)
2.  **Shop Info** (Cài đặt thông tin Shop)
3.  **Close Shop** (Đóng cửa gian hàng)
4.  **Top Up Money** (Nạp tiền vào tài khoản Seller)
5.  **Shop Flag** (Nhận cảnh báo vi phạm)
6.  **Withdraw** (Yêu cầu rút tiền)
7.  **Withdrawal History Detail** (Chi tiết lịch sử rút tiền)
8.  **Verify OTP** (Xác nhận OTP)
9.  **Products** (Quản lý sản phẩm & Kho hàng)
10. **Edit Product** (Chỉnh sửa sản phẩm)
11. **Create Product** (Đăng bán sản phẩm mới)
12. **Product Detail Preview** (Xem trước sản phẩm)
13. **Create Variant** (Tạo biến thể giá/số lượng)
14. **Update Variant** (Sửa biến thể)
15. **Add Account** (Thêm tài khoản ngân hàng nhận tiền)
16. **Update Account** (Cập nhật tài khoản)
17. **Transaction** (Lịch sử giao dịch/Bán hàng)
18. **Complaint Management** (Quản lý khiếu nại từ khách)
19. **Complaint Detail** (Chi tiết/Phản hồi khiếu nại)
20. **Review** (Quản lý đánh giá từ khách)
21. **Chat** (Chat với Customer)

### 4.3. Staff (12 Screens)
1.  **Staff Dashboard** (Tổng quan vận hành)
2.  **Withdrawal Management** (Danh sách yêu cầu rút tiền)
3.  **Withdrawal Detail** (Chi tiết lệnh rút & Tải biên lai)
4.  **Transaction Management** (Giám sát dòng tiền)
5.  **Transaction Detail** (Chi tiết giao dịch)
6.  **Complaint Management** (Danh sách khiếu nại chờ xử lý)
7.  **Complaint Detail** (Phân xử khiếu nại/Hoàn tiền)
8.  **KYC Management** (Danh sách KYC chờ duyệt)
9.  **KYC Detail** (Chi tiết hình ảnh CCCD & Duyệt)
10. **Flag Management** (Danh sách Shop bị cắm cờ)
11. **Flag Detail** (Cắm cờ vi phạm Shop/Sản phẩm)
12. **Chat** (Hỗ trợ người dùng)

### 4.4. Admin (13 Screens)
1.  **Admin Dashboard** (Tổng quan toàn hệ thống)
2.  **Manage System Configuration** (Bảo trì hệ thống)
3.  **Setup Transaction Fees** (Cài đặt phí giao dịch)
4.  **Setup Commission Rates** (Cài đặt phần trăm hoa hồng)
5.  **Manage Maintenance Mode** (Bật/Tắt bảo trì)
6.  **System Statistics** (Thống kê hệ thống)
7.  **View Revenue Reports** (Báo cáo doanh thu)
8.  **View Cash Flow** (Dòng tiền)
9.  **View Growth Charts** (Biểu đồ tăng trưởng)
10. **Manage Accounts** (Quản lý/Khóa User)
11. **Manage Staff Permissions** (Quản lý phân quyền)
12. **Assign Permissions** (Cấp quyền Staff)
13. **Edit Permissions** (Chỉnh sửa quyền)

---

## 5. ADR (Architecture Decision Records)

Quy trình ghi lại các quyết định kiến trúc quan trọng để định hình hệ thống lâu dài.

### ADR-01: Sử dụng T-SQL và Database First (SQL Server)
*   **Trạng thái**: Active | **Ngày cập nhật**: 2026-06-18
*   **Bối cảnh**: Hệ thống C2C đòi hỏi tính nhất quán giao dịch cực cao, sử dụng các stored procedure và trigger phức tạp trên SQL Server.
*   **Quyết định**: Áp dụng Database First. Schema SQL Server là nguồn chân lý duy nhất. JPA Entity phải được đồng bộ thủ công theo Database Schema, cấm `ddl-auto=update` sinh tự động.

### ADR-02: Cơ chế Escrow (Giam tiền) 72 giờ
*   **Trạng thái**: Active | **Ngày cập nhật**: 2026-06-18
*   **Bối cảnh**: Bảo vệ Customer tránh bị lừa đảo khi mua sản phẩm số lỗi.
*   **Quyết định**: Tiền thanh toán của Customer sẽ bị giam 72 giờ (quy định bởi `escrow_release_date` của Transaction). Chỉ sau thời gian này hoặc khi Customer xác nhận thủ công, số dư khả dụng của Seller mới tăng lên.

---

## 6. LESSONS LEARNED & SYSTEM SAFEGUARDS

*   **Tránh lưu file lớn trực tiếp dạng BLOB**: Làm phình to database SQL Server và làm chậm thời gian query. Bắt buộc lưu đường dẫn file trên Server hoặc S3, database chỉ lưu chuỗi đường dẫn.
*   **Bảo vệ tiền ví của User**: Luôn dùng `@Transactional` ở Service layer khi thực hiện rút tiền, nạp tiền hoặc mua sản phẩm. Đảm bảo locking thích hợp để tránh double spending (tiêu lặp tiền).
*   **Trigger an toàn set-based**: Trigger trên SQL Server phải luôn hoạt động trên tập hợp dòng dữ liệu (qua bảng `inserted` / `deleted`), cấm giả định chỉ có 1 row bị thay đổi tại một thời điểm.

---

## 7. ANTI-PATTERNS (Các mẫu thiết kế tồi cần tránh)

### 7.1. Database Anti-patterns
*   **Hard Delete dữ liệu quan trọng**: Xóa vật lý dòng bản ghi (ví dụ: orders, users). Khắc phục bằng cờ `isDelete = 1` (Soft Delete).
*   **Tính số dư bằng cách SUM lịch sử giao dịch mỗi lần hiển thị**: Gây quá tải database khi số giao dịch tăng. Phải lưu số dư hiện tại trong ví và cập nhật đồng thời khi có giao dịch phát sinh.

### 7.2. Code & Spring Boot Anti-patterns
*   **Bypass DTO Pattern**: Trả JPA Entity trực tiếp ra API bên ngoài. Điều này làm lộ cấu trúc CSDL và dễ gây lỗi vòng lặp tuần tự hóa (circular serialization).
*   **Thực hiện business logic nghiệp vụ tài chính ở Controller**: Gây khó khăn cho viết Unit Test và dễ bỏ sót các bước validate/transaction.

### 7.3. Frontend & Thymeleaf Anti-patterns
*   **Lưu thông tin số dư ví vào LocalStorage và tin tưởng nó**: Dữ liệu phía client có thể bị chỉnh sửa dễ dàng. Số dư ví và phân quyền phải luôn được xác thực lại ở Backend trên mỗi request gọi API.

<!-- SPECKIT START -->
For additional context about technologies to be used, project structure,
shell commands, and other important information, read the current plan
<!-- SPECKIT END -->
