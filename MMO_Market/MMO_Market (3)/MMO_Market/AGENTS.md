# AGENTS.md - MMO Market

> Version: 1.2
> Updated: 2026-06-18
> Scope: toàn bộ repository

## Project Context

MMO Market là web application và REST API cho sàn giao dịch sản phẩm số C2C. Hệ thống sử dụng:

- Java 17, Spring Boot 3.1, Spring Security, Spring Data JPA và Hibernate.
- Thymeleaf, HTML, CSS và JavaScript thuần cho frontend.
- SQL Server và T-SQL.
- JWT, Google OAuth2 và Gmail SMTP.
- SePay cho luồng nạp tiền.

Không mô tả dự án này là React/Vite trừ khi một module riêng thực sự sử dụng stack đó.

## Source Of Truth

Khi tài liệu mâu thuẫn, ưu tiên theo thứ tự:

1. Schema SQL Server đang chạy và migration đã áp dụng.
2. Source code và test đang chạy.
3. [Business Specification](docs/specifications/business-specification.md).
4. [Database Reference](docs/database/schema-reference.md).
5. Tài liệu module và runbook.
6. Tài liệu trong `docs/archive/` chỉ dùng để tra cứu lịch sử.

Không suy luận schema hiện tại từ tài liệu archive.

## Architecture

- Luồng chuẩn: Controller -> Service -> Repository.
- Controller chỉ xử lý HTTP, validation đầu vào và mapping response.
- Business logic đặt ở Service.
- Repository chỉ truy cập dữ liệu.
- Không thêm business logic tài chính trực tiếp vào Controller.
- Thymeleaf/MVC Controller chỉ phục vụ page/template. Dữ liệu động, persistence
  và business action của frontend phải đi qua REST API; không dùng
  `localStorage`, `sessionStorage` hoặc dữ liệu hard-code thay cho backend thật,
  trừ prototype được ghi chú rõ.

## API Rules

- Mọi REST API phải tuân theo [API Guideline](docs/api/README.md) và được liệt
  kê trong [API Reference](docs/api/api-reference.md).
- API mới hoặc API thay đổi chưa được xem là hoàn thành nếu tài liệu chưa cập
  nhật method, path, authentication/role, request, response, status code và
  controller/service liên quan.
- Route mới dùng kebab-case và ưu tiên versioning `/api/v1/...`. Route legacy
  được giữ để tránh breaking change nhưng phải được đánh dấu trong tài liệu.
- Request/response dùng DTO rõ ràng; không trả JPA entity trực tiếp cho API mới.
- API phải trả HTTP status đúng ngữ nghĩa và error body nhất quán; không trả
  `200 OK` cho nghiệp vụ thất bại.
- Authentication, role và ownership phải được kiểm tra ở backend. Frontend ẩn
  nút/menu không thay thế authorization.
- Danh sách API có phân trang phải tài liệu hóa page, size, sort, filter và giới
  hạn tối đa.
- Không đưa secret, token thật, password, dữ liệu KYC hoặc credential tài sản
  số vào ví dụ request/response hay log.

## Database Rules

- Dự án áp dụng Database First: schema SQL Server đang chạy và migration đã áp
  dụng là nguồn chuẩn; JPA entity/repository phải được đồng bộ theo schema,
  không dùng entity để tự ý sinh hoặc thay đổi schema.
- Chỉ dùng SQL Server/T-SQL, không dùng cú pháp MySQL hoặc PostgreSQL.
- Mọi task cần thay đổi database phải cung cấp cho người dùng một script T-SQL
  hoàn chỉnh, có thể lưu thành migration và chạy độc lập; không chỉ đưa snippet
  rời rạc hoặc mô tả bằng lời.
- Script thay đổi database phải gồm, khi áp dụng:
  - Kiểm tra object/column/index/constraint đã tồn tại để chạy an toàn.
  - Phần thay đổi schema và chuyển đổi dữ liệu cần thiết.
  - Query verification để xác nhận kết quả.
  - Rollback note hoặc rollback script cho thay đổi có rủi ro.
- Phải hiển thị hoặc bàn giao toàn bộ script để người dùng review. Không tự chạy
  migration hoặc thay đổi database nếu người dùng chưa yêu cầu rõ ràng.
- Sau khi schema được chấp thuận hoặc áp dụng, mới đồng bộ entity, repository,
  service và test; không để `ddl-auto=update` thay thế migration.
- Trigger phải xử lý set-based qua `inserted` và `deleted`.
- Thực thể cốt lõi phải soft delete qua `isDelete`; không hard delete tùy tiện.
- Truy vấn dữ liệu hoạt động phải lọc `isDelete = 0`.
- Tiền tệ dùng VNĐ với kiểu `BIGINT`; không tạo coin hoặc point trung gian.
- Thay đổi số dư ví hoặc tồn kho phải nằm trong `@Transactional`.
- Luồng cạnh tranh tiền/kho phải có chiến lược locking phù hợp.
- Escrow của giao dịch mua hàng là 72 giờ qua `escrow_release_date`.

## Authentication And Authorization

- Kiểm tra authentication, role và ownership ở backend.
- Không xem việc ẩn menu frontend là authorization.
- Route Staff, Seller và Admin phải có RBAC tương ứng trước khi production.
- Không hardcode secret, password, token hoặc client secret trong source.
- Secret phải được lấy từ environment variables hoặc secret manager.

## Naming

- Controller: `PascalCaseController`.
- Service: `PascalCaseService`.
- Repository: `PascalCaseRepository`.
- API route: kebab-case, ưu tiên versioning `/api/v1/...`.
- Tên bảng phải khớp schema SQL Server.
- Markdown dùng lowercase kebab-case, ngoại trừ `README.md`, `AGENTS.md`, `CHANGELOG.md`.

## Design System

- Mọi thay đổi UI mới hoặc chỉnh sửa UI hiện có phải tuân theo
  [Design System](docs/design-system/README.md),
  [UI Guideline](docs/design-system/ui-guideline.md) và
  [UX/UI Sync Guideline](docs/design-system/ux-ui-sync-guideline.md).
- Trước khi tạo CSS, component hoặc interaction mới, phải kiểm tra và tái sử
  dụng token, class `.ds-*`, fragment và JavaScript component dùng chung.
- Thymeleaf page có UI phải import `fragments/design-system-styles`; CSS riêng
  của page được import sau Design System và chỉ chứa phần đặc thù của page.
- Không hardcode màu, spacing, typography, radius, shadow hoặc trạng thái UI
  nếu Design System đã có token/component tương ứng.
- Component mới phải có các state phù hợp: hover, focus, disabled, loading,
  empty và error; đồng thời đáp ứng responsive và accessibility.
- Không xem việc ẩn menu hoặc button theo role là authorization; backend vẫn
  phải kiểm tra RBAC và ownership.
- Khi sửa một màn hình legacy, phải áp dụng Design System cho phần nằm trong
  phạm vi thay đổi và không mở rộng thành redesign toàn màn hình nếu task không
  yêu cầu.

## Definition Of Done

- [ ] RBAC và ownership đã được kiểm tra.
- [ ] Tác vụ tài chính có transaction boundary và xử lý race condition.
- [ ] Normal flow, edge case và exception có test phù hợp.
- [ ] HTTP status và error response đúng ngữ nghĩa.
- [ ] API mới/thay đổi đã được liệt kê trong API Reference và mô tả đủ contract.
- [ ] Không đưa secret mới vào Git.
- [ ] UI thay đổi đã tái sử dụng Design System và vượt qua UX/UI review checklist.
- [ ] Thay đổi database đã có script T-SQL hoàn chỉnh, verification và rollback note.
- [ ] Tài liệu module/API/schema liên quan đã cập nhật.
- [ ] `mvn test` và `mvn package` thành công.

## Git Convention

- Branch: `feat/...`, `fix/...`, `spec/...`.
- Commit: `[type]: [module] - [mô tả ngắn bằng tiếng Việt]`.
- Không trộn refactor, migration và thay đổi tài liệu không liên quan vào cùng commit.

## Documentation

- Bắt đầu tại [Documentation Index](docs/index.md).
- Tài liệu đang dùng phải có metadata `status`, `owner`, `last_updated`.
- Báo cáo triển khai cũ và tài liệu lỗi thời chuyển vào `docs/archive/`.
- Không tạo thêm thư mục `markdown/`.
