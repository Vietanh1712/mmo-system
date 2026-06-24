---
title: Architecture Overview
status: active
owner: Backend Team
last_updated: 2026-06-18
---

# Architecture Overview

## Kiến trúc hiện tại

MMO Market là Spring Boot monolith:

```text
Browser
  -> Spring MVC / REST Controller
  -> Service
  -> Spring Data Repository
  -> SQL Server
```

Frontend được render bằng Thymeleaf và gọi REST API bằng JavaScript.

Thymeleaf/MVC route chịu trách nhiệm trả page/template. Dữ liệu động, thao tác
lưu trữ và business action phải gọi REST API theo [API Guideline](../api/README.md).
Không dùng browser storage hoặc dữ liệu hard-code làm nguồn dữ liệu production.

## Thành phần

- `controller/`: REST endpoints.
- `controller/mvc/`: route trả Thymeleaf template.
- `service/`: business logic và transaction boundary.
- `dal/`: JPA repository và database access.
- `model/`: JPA entity.
- `security/`: JWT filter, token provider và SecurityConfig.
- `templates/`: Thymeleaf pages/fragments.
- `static/`: CSS, JavaScript và asset.
- `sql_scripts/`: migration, seed và script hỗ trợ.

## Authentication

- Login trả JWT cho frontend.
- Frontend lưu token trong session storage và gửi qua `Authorization`.
- Một số MVC route hiện được `permitAll` để browser tải HTML, còn JavaScript tự kiểm tra token.

Đây chưa phải mô hình RBAC hoàn chỉnh. JWT authentication hiện chưa cung cấp authorities đầy đủ cho Spring Security; cần hoàn thiện trước production.

## Financial Flow

- Tiền dùng VNĐ và `BIGINT`.
- Nạp tiền tích hợp SePay.
- Mua hàng tạo Transaction và escrow 72 giờ.
- Thay đổi số dư hoặc tồn kho phải có `@Transactional` và locking phù hợp.

## Data Lifecycle

- Thực thể cốt lõi dùng soft delete.
- Query hoạt động phải lọc `isDelete = 0`.
- Migration là cách thay đổi schema được khuyến nghị.
- `spring.jpa.hibernate.ddl-auto=update` chỉ phù hợp development; production nên chuyển sang migration + `validate`.

## Technical Debt Chính

- Một số Controller còn truy cập Repository trực tiếp.
- Nhiều màn Staff/Admin/Notification vẫn dùng mock frontend.
- RBAC MVC chưa hoàn chỉnh.
- Secret vẫn cần được chuyển hoàn toàn sang environment variables.
- `target/classes` đang được theo dõi bởi Git ở một số file; nên loại bỏ trong task riêng.
