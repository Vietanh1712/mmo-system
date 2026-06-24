---
title: Bổ sung hồ sơ người dùng Admin
status: archived
owner: Admin Team
last_updated: 2026-06-18
---

# Bổ sung hồ sơ người dùng - Admin (2026-06-04)

## Mục đích

Mở rộng màn thêm Staff và chi tiết tài khoản với:

- Địa chỉ
- Giới tính
- CCCD
- Ngày sinh
- Trạng thái hoạt động

## Database

Các trường liên quan:

- `gender`
- `address`
- `national_id`
- `date_of_birth`
- `isLocked`
- `isDelete`

Migration thực tế phải được xác minh trong `sql_scripts/` trước khi áp dụng.

## API

- `GET /api/admin/user-management/users/{userId}`
- `DELETE /api/admin/user-management/users/{userId}` theo soft delete
- Request tạo/cập nhật Staff hỗ trợ các trường hồ sơ mở rộng.

## Frontend

- Bảng tài khoản dùng toggle trạng thái.
- Action dùng icon xem và xóa theo Design System.
- Chi tiết và tạo tài khoản dùng view trong Admin workspace thay cho popup cũ.
