# 📜 CONSTITUTION.md — MMO Market

**Ngày phê duyệt:** 2026-06-26 | **Nhóm:** MMO Market Dev Team | **Phiên bản:** 2.0

> **QUY TẮC TỐI THƯỢNG:** Mọi thay đổi đối với tài liệu này đều yêu cầu sự nhất trí (100% đồng thuận) của toàn bộ nhóm. Tất cả specs, features, và code phải tuân thủ nghiêm ngặt các quy tắc dưới đây.

---

## ĐIỀU 1 — STACK CÔNG NGHỆ (Không thể thay đổi)

| Tầng | Công nghệ | Ghi chú / Quy định |
| :--- | :--- | :--- |
| **Backend** | Java 17+ / Spring Boot 3.1+ | Sử dụng Spring Security, Maven quản lý dependency. |
| **Frontend** | Thymeleaf, HTML, CSS, Javascript thuần | Chỉ phục vụ trang/template. Gọi REST API cho data động. |
| **Database** | SQL Server (T-SQL) | Bắt buộc dùng cú pháp T-SQL, KHÔNG dùng MySQL/PostgreSQL. |
| **Migration** | Flyway hoặc Liquibase | Mọi thay đổi schema BẮT BUỘC có migration script T-SQL độc lập. |
| **Auth** | JWT + Google OAuth2 | Băm mật khẩu bằng `bcrypt` (rounds >= 12). |
| **Testing** | JUnit 5 + Mockito / Spring Boot Test | Viết unit/integration tests cho Controller, Service, Repositories. |
| **Styling** | Vanilla CSS | Sử dụng hệ thống Design System trong `docs/design-system/`. |

---

## ĐIỀU 2 — TIÊU CHUẨN CODE (Coding Standards)

### 2.1. Định dạng & Linting
*   **Frontend**: Cấu hình Prettier/ESLint cho Javascript và CSS.
*   **Backend**: Cấu hình Spotless/SonarLint hoặc cấu hình Format chuẩn của dự án trong IDE.
*   **Enforcement**: 0 warnings allowed trước khi merge PR.

### 2.2. Giới hạn độ dài (Enforcement Limits)
| Chỉ số | Giới hạn tối đa | Quy định |
|---|---|---|
| **Dòng code mỗi Method/Function** | **40 dòng** | Bắt buộc phải refactor, tách nhỏ nếu vượt quá. |
| **Dòng code mỗi File** | **300 dòng** | Phân rã Class/Component nếu vượt quá kích thước này. |
| **Kích thước mỗi Pull Request (PR)** | **400 dòng** | PR lớn hơn phải được chia nhỏ thành các nhánh con. |

### 2.3. Nguyên tắc Comments & Tài liệu hóa
*   Chỉ viết comment để giải thích **TẠI SAO** (Why - lý do thiết kế, quyết định đặc biệt) chứ **KHÔNG** giải thích **LÀM GÌ** (What - mô tả lại cú pháp lệnh).
*   Tuyệt đối không commit code chứa `console.log()` hoặc `System.out.println()`. Sử dụng Logger của hệ thống.

### 2.4. Tách biệt rõ ràng Frontend / Backend
*   **Backend chịu trách nhiệm TOÀN BỘ**: Business logic (ví dụ: tính phí giao dịch, escrow), Phân quyền & Authorization, Validation nghiệp vụ (validate input, check điều kiện ví).
*   **Frontend CHỈ được phép**: Render UI, Quản lý UI state (loading, error, toggle menu) và UX Validation cục bộ (ví dụ: kiểm tra định dạng email trước khi submit).
*   Frontend không được tự ý thực thi logic nghiệp vụ hoặc lưu dữ liệu quan trọng vào local storage/session storage thay thế cho backend.

---

## ĐIỀU 3 — CHÍNH SÁCH BẢO MẬT & RÀNG BUỘC TÀI CHÍNH

### 3.1. Xác thực & Phân quyền (Auth & AuthZ)
*   **Băm mật khẩu**: Sử dụng `bcrypt` với cost rate >= 12.
*   **Quản lý JWT**: JWT token có hạn ngắn, sử dụng refresh token bảo mật. Cần kiểm tra RBAC & ownership ở tầng Backend.

### 3.2. Bảo vệ dữ liệu (CORS, SQL Injection, Validate Input)
*   Ngăn chặn SQL Injection bằng cách sử dụng Parameterized Queries hoặc Spring Data JPA JPA/Hibernate. Cấm nối chuỗi SQL thủ công.
*   Validate dữ liệu đầu vào nghiêm ngặt bằng Jakarta Bean Validation trên DTO ở REST Controller.
*   Nội dung (content) của sản phẩm số yêu cầu thiết kế mã hóa một chiều để chống rò rỉ dữ liệu (Data Leak).

### 3.3. Ràng buộc Tài chính & Tiền tệ (Bắt buộc tuân thủ)
*   **Tiền tệ**: Sử dụng đơn vị VNĐ. KHÔNG tạo coin ảo hoặc điểm thưởng trung gian.
*   **Kiểu dữ liệu**: Giá trị tiền tệ sử dụng kiểu `BIGINT` trong CSDL.
*   **Phân chia ví (Balance Separation)**: Bảng ví `Users` bắt buộc tách biệt rõ: `Available Balance` (Khả dụng) và `Hold/Frozen Balance` (Đóng băng/Giam giữ).
*   **Escrow (Giam tiền)**: Tiền thanh toán mua hàng được giam giữ trong 72 giờ qua cột `escrow_release_date` trong database trước khi giải phóng cho Seller.
*   **Quản lý Transaction**: Mọi tác vụ thay đổi số dư ví hoặc tồn kho phải được thực hiện trong `@Transactional` và xử lý chống tranh chấp (race condition).

### 3.4. Quản lý File Upload
*   Chỉ cho phép upload file có đuôi nằm trong whitelist và giới hạn kích thước tối đa (ví dụ: < 10MB).
*   Lưu trữ file trong thư mục an toàn `/uploads` hoặc cloud storage, không lưu trực tiếp dạng BLOB vào cơ sở dữ liệu.

---

## ĐIỀU 4 — QUY TRÌNH GIT & COMMIT WORKFLOW

### 4.1. Quy tắc đặt tên Branch
Tên các nhánh làm việc phải tuân theo tiền tố tương ứng với loại nhiệm vụ:
*   `feat/[tên-nhánh]` : Phát triển tính năng mới.
*   `fix/[tên-nhánh]` : Vá lỗi.
*   `chore/[tên-nhánh]` : Cấu hình hệ thống, update dependency.
*   `spec/[tên-nhánh]` : Viết/cập nhật tài liệu đặc tả tính năng.

### 4.2. Định dạng Commit Message
Tuân thủ Conventional Commits với ngôn ngữ tiếng Việt:
*   `[type]: [module] - [mô tả ngắn gọn bằng tiếng Việt]` (Ví dụ: `feat: auth - thêm OTP xác thực email`)
*   Các type chính: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`.

### 4.3. Quy tắc Pull Request (PR) & Merge
*   Mọi PR phải có ít nhất 1-2 lượt phê duyệt (approve) từ thành viên khác.
*   Bắt buộc vượt qua mọi bài kiểm tra tự động của CI (Build thành công, 100% test pass, không cảnh báo Linting).

---

## ĐIỀU 5 — YÊU CẦU KIỂM THỬ (TESTING RULES)

*   **Coverage tối thiểu cho Business Logic**: Phải đạt tối thiểu **80%** độ bao phủ dòng code cho các lớp Service chứa nghiệp vụ chính.
*   **Coverage tối thiểu cho REST API**: Phải đạt **100%** độ bao phủ (Integration test) cho tất cả các endpoint API mới hoặc được chỉnh sửa.
*   **Data integrity**: Các bài test liên quan đến cơ sở dữ liệu phải rollback sau khi chạy, không gây ảnh hưởng đến dữ liệu kiểm thử chung.

---

## ĐIỀU 6 — QUY TẮC PHỐI HỢP VỚI AI AGENT

1.  **Đọc trước khi làm**: Trước khi bắt đầu thực hiện bất kỳ nhiệm vụ lập trình nào, AI Agent bắt buộc phải đọc qua 3 tài liệu cốt lõi: `.sdd/constitution.md`, `CLAUDE.md` và `AGENTS.md`.
2.  **Đặc tả trước khi Code (Specify First)**: Tuyệt đối không viết code khi chưa có file spec tương ứng trong `.sdd/specs/` được duyệt. File Spec BẮT BUỘC phải viết theo tiêu chuẩn SDD và Speckit, tuân thủ style của EARS với đầy đủ 8 thành phần: (1) Context and Goal, (2) Actors, (3) Functional Requirements, (4) Non-Functional Requirements, (5) Data Model, (6) API Spec, (7) Error Handling, (8) Acceptance Criteria & Out of Scope.
3.  **Lập kế hoạch (Plan Review)**: AI Agent phải đề xuất Implementation Plan chi tiết. Lập trình viên (Con người) phải review và duyệt kế hoạch này trước khi AI tiến hành code.
4.  **Bảo toàn di sản**: AI Agent không được tự ý xóa hoặc thay đổi các đoạn code/comments hiện có trừ khi đó là yêu cầu trực tiếp của task.
