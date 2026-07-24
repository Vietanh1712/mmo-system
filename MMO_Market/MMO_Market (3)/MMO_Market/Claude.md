# CLAUDE.md — MMO Market v1.0

## Sàn Giao Dịch Sản Phẩm Số C2C Bảo Mật Tích Hợp Ví Điện Tử

> **Mục đích**: Bản đồ địa hình — Kiến trúc, ADR, Lessons Learned, Anti-patterns
> **Đọc trước**: `.specify/memory/constitution.md` (stack, security, standards) | `AGENTS.md` (domain rules, golden patterns)

---

## TL;DR (60 giây)

### Tech Stack (Xem `.specify/memory/constitution.md`)
- **Backend**: Spring Boot 3.1, Java 17, Spring Security + JWT
- **Frontend**: Thymeleaf, HTML5, Vanilla CSS, Vanilla JS
- **Database**: SQL Server (T-SQL)
- **Integrations**: Cổng thanh toán tự động SePay, Gmail SMTP (gửi OTP/thông báo)

### Domain
- Sàn giao dịch sản phẩm số C2C (key game, giftcode, tài khoản game/Premium) chuyên biệt cho MMO.
- **4 Roles**: Customer (Người mua), Seller (Người bán), Staff (Nhân viên kiểm duyệt), Admin (Quản trị viên).
- **Security**: Mã hóa thông tin sản phẩm số (`DigitalAsset`) trên database để tránh rò rỉ dữ liệu.
- **Ví điện tử**: Tích hợp ví tiền tệ VNĐ trực tiếp, xử lý nạp/rút và cơ chế bảo vệ giao dịch.

### Key Rules
- ✅ Sử dụng đơn vị tiền tệ VNĐ dạng số nguyên lớn (`Long` / `BIGINT`).
- ✅ Ví người dùng phải tách biệt 2 trạng thái số dư: khả dụng (`available_balance`) và đóng băng (`hold_balance`).
- ✅ Giam tiền giao dịch trung gian (Escrow) 72 giờ mặc định (hoặc 168 giờ đối với shop mới/shop bị cảnh cáo/tỷ lệ khiếu nại cao).
- ✅ DTO Pattern bắt buộc cho API Request/Response.
- ✅ Soft Delete toàn hệ thống dùng cờ `isDelete = 1` cho các thực thể quan trọng.
- ✅ SQL Server Triggers bắt buộc phải xử lý set-based qua bảng ảo `inserted`/`deleted`.
- ❌ KHÔNG hardcode thông tin bảo mật hay credentials.
- ❌ KHÔNG trả JPA Entity trực tiếp ra API Controller.
- ❌ KHÔNG tính tiền hay phân quyền ở Frontend.
- ❌ KHÔNG dùng `System.out.println` hoặc `printStackTrace` (phải sử dụng SLF4J logger).

---

## KIẾN TRÚC HỆ THỐNG

### Sơ đồ tổng quan

```
┌────────────────────────────────────────────────────────────────────────┐
│                        FRONTEND PORTAL (Thymeleaf/HTML/CSS/JS)         │
│  - Render giao diện phía máy chủ bằng Thymeleaf.                       │
│  - Thực hiện các request REST API không đồng bộ qua JavaScript.        │
│  - Quản lý trạng thái giao diện và validate UX đầu vào.                │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │ (REST API / JSON)
                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        BACKEND (Spring Boot 3.1 + Java 17)             │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │  Controller Layer (@RestController)                              │  │
│  │  - Tiếp nhận request, phân quyền sơ bộ, validate đầu vào.        │  │
│  │  - Trả về DTO chuẩn và xử lý ngoại lệ qua @ControllerAdvice.      │  │
│  └───────────────────────────────┬──────────────────────────────────┘  │
│                                  │
│                                  ▼
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │  Service Layer (@Service / @Transactional)                       │  │
│  │  - Xử lý business logic nghiệp vụ chính (rút/nạp, mua bán, ví).  │  │
│  │  - Áp dụng Pessimistic Locking để tránh Race Condition số dư.     │  │
│  └───────────────────────────────┬──────────────────────────────────┘  │
│                                  │
│                                  ▼
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │  Repository Layer (@Repository)                                  │  │
│  │  - Spring Data JPA, Hibernate, custom native SQL/JPQL queries.   │  │
│  └───────────────────────────────┬──────────────────────────────────┘  │
│                                  │
│                                  ▼
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │  Entity Layer (@Entity)                                          │  │
│  │  - Ánh xạ cấu trúc bảng SQL Server và quản lý Soft Delete.       │  │
│  └──────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │
                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│                      DATABASE & SERVICES EXTERNAL                      │
│  - SQL Server (T-SQL): Cơ sở dữ liệu và trigger set-based.             │
│  - SePay Integration: Nhận cổng thanh toán tự động nạp tiền.           │
│  - Gmail SMTP: Gửi OTP xác thực và thông báo biến động số dư.          │
└────────────────────────────────────────────────────────────────────────┘
```

---

## CORE MODULES

### Auth & KYC Module
- Đăng nhập/Đăng ký tài khoản (Guest, Customer, Staff, Admin).
- JWT Authentication + Google OAuth2.
- Hồ sơ định danh cá nhân (KYC) hỗ trợ tải ảnh CCCD/CMND.
- Quy trình duyệt KYC tự động hoặc thủ công từ Staff.

### Product Catalog & Shop Module
- Đăng ký mở shop kinh doanh (có thu phí mở shop).
- Đăng bán sản phẩm số (`Product`), quản lý biến thể (`ProductVariant`).
- Mã hóa dữ liệu số nhạy cảm (`DigitalAsset` - key, tài khoản) trước khi lưu trữ vào DB.
- Cảnh cáo cắm cờ (`ShopFlag`) và quản lý trạng thái Shop (Active, Inactive, Suspended, Locked, Banned).

### Wallet & Payment Escrow Module
- Nạp tiền tự động qua tích hợp cổng thanh toán SePay.
- Yêu cầu rút tiền (`Withdrawal`) của người bán, xác minh số dư khả dụng và đưa vào hàng đợi Pending chờ Staff duyệt.
- Xử lý Escrow (giam tiền) giao dịch trong ví trung gian hệ thống và lên lịch cộng tiền khả dụng sau thời hạn bảo lãnh.

### Order & Transaction Module
- Đặt hàng và thanh toán đơn hàng sản phẩm số.
- Giải mã thông tin sản phẩm và hiển thị giao hàng tức thì cho người mua khi đơn hàng thành công.
- Lưu nhật ký giao dịch tài chính (`WalletTransaction`) để theo dõi dòng tiền.
- Đặt trước sản phẩm (`PreOrder`) cho các mặt hàng chưa có sẵn.

### Chat & Support Module
- Chat trực tiếp 1-1 giữa người bán và người mua.
- Chat 3 bên khi có tranh chấp/khiếu nại (`Complaint`) với sự tham gia phân xử của Staff.
- Tạo thẻ yêu cầu hỗ trợ chung (`SupportTicket`).

---

## FLOWS

### Order Checkout & Asset Decryption Flow
```
Buyer → Frontend: Submit order checkout request
      → POST /api/v1/orders
      → Backend: OrderService.processCheckout() [Transactional]
           ├── Lock Buyer & Seller wallet details (Pessimistic Lock)
           ├── Check if Buyer available_balance >= order amount
           ├── Deduct Buyer available_balance -> Put to hold_balance (Escrow)
           ├── Retrieve and decrypt DigitalAsset (keys/accounts) from database
           ├── Update ProductVariant inventory/stock
           ├── Record WalletTransaction details
           └── Return decrypted credentials dynamically to Buyer
```

### Escrow Lock/Release Flow
```
Transaction Complete (Escrow Locked in hold_balance)
       ├── Schedule release timer based on Shop Level & warning status (72h or 168h)
       │     ├── IF Buyer confirms early completion OR Timer expires without dispute:
       │     │     └── Release funds from hold_balance to Seller available_balance
       │     │
       │     └── IF Buyer opens Complaint before release date:
       │           ├── Hold funds locked in hold_balance indefinitely
       │           └── Staff reviews complaint:
       │                 ├── Accept: Refund hold_balance to Buyer available_balance
       │                 └── Reject: Release hold_balance to Seller available_balance
```

---

## ADR (Architecture Decision Records)

### ADR-001: Spring Boot 3.1 + Java 17
- **Trạng thái**: ✅ Active
- **Quyết định**: Sử dụng Java 17 kết hợp Spring Boot 3.1.
- **Lý do**: Cung cấp hiệu năng cao, bảo mật mạnh mẽ và khả năng hỗ trợ hệ sinh thái thư viện doanh nghiệp ổn định cho các hệ thống ví tài chính.

### ADR-002: Database First & T-SQL (SQL Server)
- **Trạng thái**: ✅ Active
- **Quyết định**: Schema SQL Server là nguồn chân lý duy nhất. Mọi thay đổi cấu trúc bảng phải thực hiện qua migration script SQL. Thực thể JPA Entity được ánh xạ thủ công từ database, cấm sử dụng cơ chế sinh tự động `ddl-auto: update`.

### ADR-003: Cơ chế Escrow (Giam tiền) Động
- **Trạng thái**: ✅ Active
- **Quyết định**: Tiền giao dịch bị tạm giam trong ví hệ thống. Thời gian giam tiền tính toán động:
  - **168 giờ (7 ngày)**: Shop bị cảnh cáo (Level 0), shop mới dưới 20 đơn thành công (Level 1), hoặc shop có tỷ lệ khiếu nại đúng `>= 2%`.
  - **72 giờ (3 ngày)**: Mặc định đối với các cửa hàng bình thường khác.

### ADR-004: Soft Delete Toàn Hệ Thống
- **Trạng thái**: ✅ Active
- **Quyết định**: Không xóa vật lý dữ liệu quan trọng (Users, Products, Orders). Sử dụng cờ `isDelete = 1` và luôn lọc dữ liệu qua cờ này khi truy vấn.

### ADR-005: DTO Pattern Bắt Buộc
- **Trạng thái**: ✅ Active
- **Quyết định**: Controller chỉ nhận và trả về các lớp DTO. Ánh xạ (mapping) giữa Entity và DTO phải diễn ra ở Service Layer.

---

## LESSONS LEARNED

### LESSON-001: Tách biệt available_balance và hold_balance
- **Bài học**: Không gộp chung số dư tài khoản. Việc tách biệt giúp hệ thống đóng băng tiền an toàn khi có khiếu nại hoặc lệnh rút tiền đang xử lý, ngăn chặn việc rút khống/tiêu lặp tiền (double spending).

### LESSON-002: Cấu hình template path động
- **Bài học**: Tránh cấu hình tuyệt đối ổ đĩa cứng nhắc (`d:/mmo-system/...`) trong file cấu hình. Cần sửa về đúng thư mục làm việc của máy chạy (`c:/Users/...`) hoặc dùng đường dẫn tương đối để đảm bảo khả năng di động của ứng dụng khi chạy local.

### LESSON-003: Tránh lỗi kiểu SpEL ternary null-to-boolean
- **Bài học**: Sử dụng biểu thức ternary dạng `${isSellerView ? ...}` trong Thymeleaf sẽ gây ra lỗi `SpelEvaluationException` nếu thuộc tính đó là `null`. Giải pháp là so sánh tường minh `${isSellerView == true ? ...}`.

### LESSON-004: Khai báo th:fragment trực tiếp trên container HTML
- **Bài học**: Khai báo fragment trên thẻ `th:block` bao ngoài của mã giao diện có thể khiến Thymeleaf kết xuất (render) thẻ đó hai lần trong luồng trang. Khai báo trực tiếp thuộc tính `th:fragment` lên thẻ container chính (`div`) để giao diện hiển thị chính xác.

---

## ANTI-PATTERNS

### Code & DB Anti-Patterns

| Anti-Pattern | Mô tả | Cách khắc phục |
|---|---|---|
| **Hard Delete** | Xóa trực tiếp dữ liệu khỏi DB | Thay bằng Soft Delete (`isDelete = 1`). |
| **Summing Ledger on View** | Tính số dư ví bằng cách SUM lịch sử giao dịch mỗi lần hiển thị | Lưu trữ trường `balance_vnd` trong bảng Users và cập nhật qua transaction. |
| **Row-by-Row Trigger** | Trigger SQL Server viết theo kiểu lặp từng dòng | Bắt buộc xử lý tập hợp (set-based) qua bảng ảo `inserted`/`deleted`. |
| **Hardcoded Credentials** | Lưu mật khẩu, khóa bí mật trực tiếp trong code | Sử dụng Environment Variables thông qua cấu hình Spring Boot. |

### Thymeleaf & Frontend Anti-Patterns

| Anti-Pattern | Mô tả | Cách khắc phục |
|---|---|---|
| **Client-Side Authorization** | Ẩn/hiển thị nút chức năng ở frontend để phân quyền | Chỉ xem đó là UX. Backend phải xác thực quyền hạn (Role/Ownership) trên mỗi request. |
| **Client-Side Wallet Check** | Kiểm tra số dư ví ở phía client trước khi mua hàng | Backend Service Layer bắt buộc phải khóa ví và kiểm tra lại số dư trước khi trừ tiền. |
| **Static HTML Hardcoding** | Viết trực tiếp tin nhắn mockup tĩnh trên HTML | Sử dụng trạng thái trống (`messages-empty-state`) và tải động dữ liệu qua JS. |

---

## FILE STRUCTURE

### Backend (Spring Boot)
```
apps/backend/
├── src/main/java/com/mmo/
│   ├── MMOMarketApplication.java # Entry point
│   ├── shared/                   # Thành phần dùng chung toàn dự án
│   │   ├── model/                # JPA Entities
│   │   ├── dal/                  # JPA Repositories (Spring Data JPA)
│   │   ├── security/             # Cấu hình Spring Security & JWT Filter
│   │   ├── config/               # WebConfig, JpaConfig
│   │   ├── dto/                  # Shared DTOs
│   │   └── mvc/                  # Shared MVC Controllers (Thymeleaf views mapping)
│   └── feature/                  # Phân chia theo mô-đun nghiệp vụ chính
│       ├── auth/                 # Xác thực & Quản lý thông tin cá nhân
│       ├── kyc/                  # Yêu cầu KYC và phê duyệt tài liệu
│       ├── seller/               # Đăng ký shop, cấu hình shop & seller dashboard
│       ├── product/              # Tìm kiếm sản phẩm, danh mục, đánh giá, cắm cờ
│       ├── wallet/               # Nạp tiền (SePay), rút tiền, ví điện tử
│       ├── order/                # Giỏ hàng, thanh toán và xử lý đơn hàng
│       ├── preorder/             # Đặt hàng trước
│       ├── complaint/            # Tranh chấp & Khiếu nại
│       ├── support/              # Thẻ hỗ trợ người dùng
│       ├── chat/                 # Chat trực tiếp 1-1
│       ├── notification/         # Gửi thông báo email và in-app
│       ├── staff/                # Giao diện và nghiệp vụ của Nhân viên
│       ├── admin/                # Giao diện và nghiệp vụ của Quản trị viên
│       └── upload/               # Quản lý tải lên tệp tin đính kèm
└── src/main/resources/
    └── application.properties    # Cấu hình kết nối DB, Thymeleaf path
```

### Frontend (Thymeleaf)
```
apps/frontend/
├── templates/                    # Tệp HTML Thymeleaf
│   ├── account/                  # Lịch sử đơn hàng, giao dịch ví của User
│   ├── admin/                    # Trang quản lý của Admin
│   ├── auth/                     # Đăng ký, đăng nhập, quên mật khẩu
│   ├── fragments/                # Khối giao diện dùng chung (header, footer, sidebar)
│   ├── seller/                   # Console quản lý của Seller
│   ├── staff/                    # Trang kiểm duyệt của Staff
│   ├── home.html                 # Trang chủ
│   ├── messages.html             # Trang chat trực tiếp
│   └── search-results.html       # Kết quả tìm kiếm sản phẩm
└── static/                       # Tài sản tĩnh
    ├── css/                      # Stylesheet phân chia theo feature
    └── js/                       # Script xử lý gọi API & render động
```

---

## DEVELOPMENT WORKFLOW

```
┌─────────────────────────────────────────────────────────┐
│  specify → plan → implement → test → review → deploy    │
│  Define     Plan     Build       Verify   Review        │
└─────────────────────────────────────────────────────────┘
```

### Phase Commands

| Giai đoạn | Lệnh thực hiện |
|---|---|
| **Build & Compile** | `mvn clean compile` |
| **Run Backend** | Chạy `run_project.bat` từ gốc thư mục |
| **Kiểm thử (Test)** | `mvn test` (bỏ qua kiểm thử bằng `-Dmaven.test.skip=true`) |
| **Đồng bộ CSDL** | Thực thi migration scripts trong thư mục `/database/sql_scripts/migration/` |

---

## DOMAIN MODEL

### Core Entities & Relationships
- **User**: Thực thể người dùng hệ thống (`role` là CUSTOMER, SELLER, STAFF, ADMIN; trạng thái `isDelete`).
- **SellerRegistration / Shop**: Hồ sơ đăng ký gian hàng liên kết 1-1 với User.
- **Product**: Sản phẩm đăng bán, thuộc về một Shop và nằm trong một Category.
- **ProductVariant**: Biến thể của sản phẩm (theo phân loại thuộc tính, giá tiền, số lượng tồn kho).
- **DigitalAsset**: Tài sản số thực tế (key game, giftcode, tài khoản) được mã hóa bảo mật, liên kết trực tiếp với ProductVariant.
- **Order / OrderDetail**: Đơn hàng mua sản phẩm và chi tiết các biến thể sản phẩm đã chọn mua.
- **WalletTransaction**: Nhật ký giao dịch ví điện tử của người dùng (ghi nhận biến động nạp, rút, mua, giam tiền, hoàn tiền).
- **Complaint**: Tranh chấp khiếu nại đơn hàng giữa người mua và người bán.
- **SupportTicket**: Yêu cầu hỗ trợ gửi đến nhân viên hệ thống.

---

## NAMING QUICK REFERENCE

| Loại đối tượng | Quy tắc đặt tên | Ví dụ |
|---|---|---|
| Java Class | PascalCase | `WalletService`, `ChatController` |
| Java Method | camelCase | `transferToEscrow()`, `getUserById()` |
| Java Constant | UPPER_SNAKE_CASE | `TRANSACTION_LIMIT`, `MAX_RETRIES` |
| Bảng CSDL / Cột | snake_case, PascalCase | Bảng `Users`, Cột `balance_vnd`, `isDelete` |
| Đường dẫn API | kebab-case, số nhiều | `/api/v1/shop-registrations`, `/api/v1/orders` |
| Tệp HTML / CSS / JS | kebab-case | `messages.html`, `seller-console.js` |

---

## ADR STATUS TABLE

| Ký hiệu ADR | Tên quyết định | Trạng thái | Ngày kiểm duyệt |
|---|---|---|---|
| ADR-001 | Spring Boot 3.1 + Java 17 | ✅ Active | 2026-07-24 |
| ADR-002 | Database First & T-SQL | ✅ Active | 2026-07-24 |
| ADR-003 | Cơ chế Escrow (Giam tiền) Động | ✅ Active | 2026-07-24 |
| ADR-004 | Soft Delete Toàn Hệ Thống | ✅ Active | 2026-07-24 |
| ADR-005 | DTO Pattern Bắt Buộc | ✅ Active | 2026-07-24 |
| ADR-006 | SePay Auto Top-up Webhook | ✅ Active | 2026-07-24 |

---


<!-- SPECKIT START -->
For additional context about technologies to be used, project structure,
shell commands, and other important information, read the current plan
<!-- SPECKIT END -->
