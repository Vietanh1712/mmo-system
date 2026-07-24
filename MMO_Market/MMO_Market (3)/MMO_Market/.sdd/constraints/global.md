# Constraints — Global (Technical)

> **Phạm vi**: Ràng buộc kỹ thuật toàn hệ thống cho dự án MMO Market.
> Đây là các ràng buộc cứng về stack, môi trường, convention và hiệu năng —
> mọi spec, code, test phải tuân thủ.
> Liên quan: [`business.md`](./business.md) (ràng buộc nghiệp vụ), [`safety.md`](./safety.md) (ràng buộc an toàn vận hành), [`constitution.md`](../constitution.md).

---

## 1. Tech Stack (Bắt buộc)

| Layer | Công nghệ | Phiên bản | Ghi chú |
|-------|-----------|-----------|---------|
| **Backend** | Java + Spring Boot | Java 17+ / Spring Boot 3.1+ | Sử dụng Spring Security, Maven quản lý dependency |
| **Frontend** | Thymeleaf, HTML, CSS, JS thuần | HTML5, JS ES6+ | Gọi REST API cho dữ liệu động |
| **Database** | Microsoft SQL Server | 2019 hoặc cao hơn | Sử dụng cú pháp T-SQL, cấm dùng MySQL/Postgres |
| **Migration** | Flyway hoặc Liquibase | Theo Spring Boot 3.1+ | Mọi schema change bắt buộc có script T-SQL di chuyển |
| **Auth** | JWT + Google OAuth2 | bcrypt (cost **≥ 12**) | Mã hóa mật khẩu người dùng |
| **Testing** | JUnit 5 + Mockito | Theo Spring Boot 3.1+ | Unit/Integration testing cho Service/API |
| **Styling** | Vanilla CSS | Hỗ trợ CSS Custom Properties | Thiết kế theo hệ thống Design System chung của dự án |
| **Logging** | SLF4J (Logback) | Theo Spring Boot 3.1+ | Cấm dùng `System.out.println()` ở Backend |

> ⚠️ **Cấm thay thế hoặc bổ sung thư viện cốt lõi ngoài danh sách trên** khi chưa được duyệt thông qua sửa đổi Hiến pháp dự án.

---

## 2. Ràng buộc Kiến trúc

| ID | Rule | Rationale |
|----|------|-----------|
| GLOB-ARCH-01 | Kiến trúc bắt buộc: **`Controller → Service → Repository → Entity`**. Không cho phép gọi tắt qua layer (ví dụ: Controller gọi thẳng Repository). | Phân tách trách nhiệm (Separation of concerns) rõ ràng. |
| GLOB-ARCH-02 | REST Controller chỉ nhận/trả **DTO** (Request/Response) — Entity JPA không bao giờ được trả trực tiếp qua API. | Ngăn chặn rò rỉ cấu trúc dữ liệu và lỗi lazy-loading. |
| GLOB-ARCH-03 | Toàn bộ Logic nghiệp vụ và kiểm tra phân quyền bắt buộc thực hiện tại **Service layer**. Frontend chỉ thực hiện render UI và UX validation thô. | Chống sửa đổi điểm/tiền/quyền ở client. |
| GLOB-ARCH-04 | Sử dụng cơ chế xử lý lỗi tập trung thông qua `@ControllerAdvice` và `@ExceptionHandler` để trả về JSON thống nhất. | Đồng bộ cấu trúc phản hồi lỗi khi tích hợp. |
| GLOB-ARCH-05 | Tách biệt hoàn toàn logic: Frontend gọi API bất đồng bộ (AJAX/Fetch) để xử lý dữ liệu động, Thymeleaf chỉ render khung layout tĩnh. | Đảm bảo tính mở và khả năng tái sử dụng API. |

---

## 3. Ràng buộc API

| ID | Rule | Ví dụ |
|----|------|-------|
| GLOB-API-01 | REST API công cộng bắt đầu bằng `/api/search/`; API cần xác thực bắt đầu bằng `/api/v1/` hoặc `/api/auth/`. | `/api/search/products`, `/api/v1/profile` |
| GLOB-API-02 | Resource name dạng **plural, kebab-case** | `/api/v1/wallet/withdrawals`, `/api/transactions` |
| GLOB-API-03 | nested resource để thể hiện quan hệ | `/api/complaints/{id}/chats`, `/api/seller/products/{id}/details` |
| GLOB-API-04 | Trả HTTP Status code đúng ngữ cảnh (200 OK, 201 Created, 400 Validation, 401 Unauthorized, 403 Forbidden, 409 Conflict, 422 Business, 500 System). | HTTP 201 cho đăng ký/tạo đơn thành công |

---

## 4. Ràng buộc Bảo mật

| ID | Rule | Rationale |
|----|------|-----------|
| GLOB-SEC-01 | Mọi REST API (ngoại trừ auth login/register/forgot-password và public search) phải được bảo vệ bởi Spring Security JWT filter. | Ngăn chặn truy cập bất hợp pháp. |
| GLOB-SEC-02 | Khóa JWT Secret và các thông tin ngân hàng, Google Client Secrets bắt buộc đọc từ biến môi trường (Environment Variables). Cấm hardcode trong code. | Tránh lộ lọt credentials khi mã nguồn bị công khai. |
| GLOB-SEC-03 | Validate dữ liệu đầu vào chặt chẽ qua Jakarta Bean Validation (ví dụ: `@NotNull`, `@Min`, `@Size`, `@Email`) tại REST Controller. | Ngăn chặn tấn công Injection và dữ liệu rác. |
| GLOB-SEC-04 | Sanitize tên file và lọc extension khi thực hiện tải lên tập tin. File phải lưu tại thư mục an toàn `/uploads`, không lưu BLOB trong DB. | Tránh path traversal, thực thi mã độc và phình DB. |
| GLOB-SEC-05 | Mã hóa đối xứng thông tin nhạy cảm của tài sản số (tài khoản/mật khẩu/key bán ra) trước khi lưu trữ vào DB và giải mã động khi bàn giao. | Bảo mật thông tin hàng hóa kỹ thuật số. |

---

## 5. Ràng buộc Database (T-SQL)

| ID | Rule | Rationale |
|----|------|-----------|
| GLOB-DB-01 | Hệ quản trị cơ sở dữ liệu: **Microsoft SQL Server**. Cấm sử dụng các tính năng đặc thù của MySQL hay Postgres. | Đồng bộ thiết kế hạ tầng. |
| GLOB-DB-02 | Table name và Column name trong DB phải sử dụng dạng **snake_case** và ở dạng số nhiều đối với tên bảng. | `users`, `digital_assets`, `created_at` |
| GLOB-DB-03 | Mọi thay đổi cấu trúc bảng phải thực hiện qua migration script (T-SQL) của Flyway/Liquibase. Cấm tự ý sửa trực tiếp DB production. | Đồng bộ và có khả năng khôi phục (reproducible deployments). |
| GLOB-DB-04 | Mọi bảng nghiệp vụ quan trọng phải có các cột audit: `created_at`, `updated_at`, `created_by`, `is_delete` (hoặc `status`). | Phục vụ truy vết và xóa mềm dữ liệu. |
| GLOB-DB-05 | Cấm sử dụng row-by-row cursor logic trong Trigger. Mọi Trigger bắt buộc xử lý dạng set-based qua bảng ảo `inserted` và `deleted`. | Đảm bảo hiệu năng DB SQL Server khi insert/update hàng loạt. |

---

## 6. Ràng buộc Naming Convention

### 6.1. Java Backend

| Loại | Convention | Ví dụ |
|------|------------|-------|
| Class / Interface | PascalCase | `TransactionService.java`, `UserRepository.java` |
| Package | lowercase | `com.mmo.feature.order`, `com.mmo.shared.dto` |
| Method / Variable | camelCase | `purchaseProduct()`, `balanceVnd` |
| Constant | UPPER_SNAKE_CASE | `FLAT_BUYER_FEE_VND`, `DEFAULT_ESCROW_HOURS` |
| Enum value | UPPER_SNAKE_CASE | `OrderStatus.HELD`, `TransactionType.PAYMENT` |
| DB Table | snake_case, plural | `users`, `topup_transactions`, `products` |
| DB Column | snake_case | `balance_vnd`, `deposit_vnd`, `is_delete` |
| DTO Request | PascalCase + `Request` suffix | `UpdateProfileRequest`, `KycRequestDto` |
| DTO Response | PascalCase + `Response` / `DTO` suffix | `ProfileResponse`, `ProductDetailDTO` |

### 6.2. Frontend Thymeleaf / JS / CSS

| Loại | Convention | Ví dụ |
|------|------------|-------|
| Static files (JS/CSS) | kebab-case | `main-style.css`, `wallet-stats.js` |
| HTML Templates | kebab-case / camelCase | `homepage.html`, `seller-dashboard.html` |
| CSS Class name | kebab-case | `.btn-primary`, `.wallet-balance-card` |

---

## 7. Ràng buộc Logging & Code Quality Limits

### 7.1. Ràng buộc Logging
*   **SLF4J (Logback)** là thư viện log duy nhất được dùng ở Backend. Cấm dùng `System.out.println()` hoặc `printStackTrace()`.
*   Log phải có cấu trúc và ghi nhận các tham số context rõ ràng (`userId`, `orderId`, `status`). Không log dữ liệu nhạy cảm (mật khẩu, JWT token, PII).

### 7.2. Giới hạn độ dài & Chất lượng code (Constitution Enforcement)
*   **Dòng code mỗi Method/Function**: Tối đa **40 dòng**.
*   **Dòng code mỗi File/Class**: Tối đa **300 dòng**.
*   **Kích thước mỗi Pull Request**: Tối đa **400 dòng** code thay đổi.
*   **Coverage tối thiểu cho Business Logic**: **≥ 80%** độ bao phủ dòng code tại Service Layer.
*   **Coverage tối thiểu cho REST API**: **100%** integration test cho tất cả các endpoint REST API mới hoặc chỉnh sửa.
*   Không được phép tồn tại TODO comments trong nhánh code chính thức (`main` / `master`).

---

## Tham chiếu

| Nguồn | Nội dung liên quan |
|-------|---------------------|
| `.sdd/constitution.md` | Điều 1 (Tech Stack), Điều 2 (Coding Standards), Điều 5 (Testing Rules) |
| `AGENTS.md` | Mục 3 (Forbidden Patterns), Mục 5 (Definition of Done) |
| `constraints/business.md` | Ràng buộc nghiệp vụ MMO Market |
| `constraints/safety.md` | Ràng buộc an toàn vận hành |
