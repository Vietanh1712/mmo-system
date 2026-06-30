# MMO Market

MMO Market là hệ thống thương mại điện tử C2C dành cho sản phẩm số, hỗ trợ quản lý tài khoản, sản phẩm, ví VNĐ, giao dịch, escrow, KYC, khiếu nại và vận hành Seller/Staff/Admin.

## Tech Stack

- Java 17
- Spring Boot 3.1.5
- Spring MVC, Thymeleaf
- Spring Security, JWT
- Spring Data JPA, Hibernate
- SQL Server
- HTML, CSS, JavaScript
- Maven

## Quick Start

Yêu cầu:

- JDK 17
- Maven 3.9+
- SQL Server
- Database đã được tạo và áp dụng migration cần thiết

Chạy kiểm tra:

```powershell
mvn test
mvn package
```

Chạy ứng dụng:

```powershell
mvn spring-boot:run
```

Mặc định ứng dụng chạy tại `http://localhost:8080`.

Không commit credential thật. Cấu hình database, JWT, SMTP, OAuth2 và SePay nên được truyền qua environment variables. Xem [Local Development](docs/runbooks/local-development.md).

## Main Areas

- Customer: profile, KYC, security, wallet, orders, notifications.
- Seller: shop, products, inventory, transactions, withdrawals, complaints.
- Staff: KYC, complaints, transactions, withdrawals, flags.
- Admin: accounts, permissions, audit, configuration, fees, notifications.

Một số màn hình vận hành hiện vẫn là frontend prototype và chưa có API/database hoàn chỉnh. Trạng thái từng module được ghi trong tài liệu tương ứng.

## Documentation

- [Documentation Index](docs/index.md)
- [Business Specification](docs/specifications/business-specification.md)
- [Architecture Overview](docs/architecture/overview.md)
- [Database Reference](docs/database/schema-reference.md)
- [Search Module](docs/modules/search.md)
- [Design System](docs/design-system/README.md)
- [Local Development](docs/runbooks/local-development.md)
- [Change History](CHANGELOG.md)

## Repository Rules

Đọc [AGENTS.md](AGENTS.md) trước khi thay đổi code, database hoặc tài liệu.
