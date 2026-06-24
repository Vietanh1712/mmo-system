---
title: Local Development
status: active
owner: Engineering Team
last_updated: 2026-06-18
---

# Local Development

## Prerequisites

- JDK 17
- Maven 3.9+
- SQL Server
- Git
- Node.js chỉ cần khi kiểm tra JavaScript/tooling phụ trợ

## Database

Tạo database development và áp dụng migration trong `sql_scripts/` theo đúng thứ tự version.

Khuyến nghị cấu hình qua environment variables:

```powershell
$env:SPRING_DATASOURCE_URL='jdbc:sqlserver://localhost:1433;databaseName=MMO_System;trustServerCertificate=true'
$env:SPRING_DATASOURCE_USERNAME='sa'
$env:SPRING_DATASOURCE_PASSWORD='<local-password>'
$env:APP_JWTSECRET='<development-secret>'
```

SMTP, Google OAuth2 và SePay cũng phải dùng environment variables. Không dùng credential đã commit làm credential thật; các credential từng lộ phải được rotate.

## Build And Test

```powershell
mvn test
mvn package
```

Artifact WAR được tạo trong `target/`.

## Run

```powershell
mvn spring-boot:run
```

Hoặc:

```powershell
java -jar target/MMO_Market-1.0-SNAPSHOT.war
```

Mặc định:

- App: `http://localhost:8080`
- Health: `http://localhost:8080/api/auth/health`

## Useful Pages

- `/`
- `/login`
- `/register`
- `/search`
- `/profile`
- `/wallet`
- `/seller/dashboard`
- `/staff/dashboard`
- `/admin/users`

Các route role-specific chưa được xem là production-safe nếu RBAC backend chưa hoàn chỉnh.

## Verification

Sau thay đổi frontend:

1. Chạy `node --check` cho JavaScript đã sửa.
2. Chạy `mvn test`.
3. Chạy `mvn package`.
4. Khởi động app và smoke test route liên quan.
5. Kiểm tra source và `target/classes` nếu repository vẫn đang track artifact.

Sau migration:

1. Backup database.
2. Chạy migration trên database test.
3. Kiểm tra schema/index/constraint.
4. Chạy application và repository initialization.
5. Kiểm tra rollback hoặc recovery note.

## Common Issues

### Port đã được sử dụng

```powershell
Get-NetTCPConnection -LocalPort 8080 -State Listen
```

Chạy cổng khác:

```powershell
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

### Không kết nối SQL Server

- Kiểm tra SQL Server service.
- Kiểm tra TCP/IP và port 1433.
- Kiểm tra database name và credential.
- Kiểm tra `trustServerCertificate=true` cho local.

### Maven không tải dependency

- Kiểm tra mạng/proxy.
- Kiểm tra Maven Central.
- Không xóa cache Maven tùy tiện khi chưa xác định artifact lỗi.

## Production Notes

- Không dùng `ddl-auto=update`.
- Không bật DEBUG/TRACE cho Security và SQL.
- Không commit secret.
- Bắt buộc hoàn thiện RBAC cho Staff/Seller/Admin.
- Dùng HTTPS, secret manager, backup và monitoring.
