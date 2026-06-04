# Bổ sung hồ sơ người dùng — Admin (2026-06-04)

## Mục đích

Hỗ trợ màn **Thêm nhân viên** và **Chi tiết tài khoản** với các trường: địa chỉ, giới tính, CCCD, ngày sinh, trạng thái (toggle).

## Thay đổi cơ sở dữ liệu

**File migration:** `sql_scripts/2026-06-04_admin_user_profile_columns.sql`

| Cột | Kiểu | Mô tả |
|-----|------|--------|
| `gender` | `NVARCHAR(20)` | Nam / Nữ (đã có trên entity, script bổ sung nếu DB cũ thiếu) |
| `address` | `NVARCHAR(500)` | Địa chỉ liên hệ |
| `national_id` | `VARCHAR(20)` | Số CCCD |
| `date_of_birth` | `DATE` | Ngày sinh |

Trạng thái hoạt động vẫn dùng `isLocked` (`0` = đang hoạt động, `1` = tạm dừng). Xóa tài khoản dùng soft delete `isDelete = 1`.

## API bổ sung

| Method | Endpoint | Mô tả |
|--------|----------|--------|
| `GET` | `/api/admin/user-management/users/{userId}` | Lấy chi tiết một tài khoản |
| `DELETE` | `/api/admin/user-management/users/{userId}` | Xóa mềm (mọi vai trò trừ Admin) |

`StaffUpsertRequest` mở rộng: `gender`, `address`, `nationalId`, `dateOfBirth`, `active`.

## Frontend

- Bảng tài khoản: cột trạng thái = `ds-toggle`; thao tác = eye + trash (theo `table-example.html`).
- Popup chi tiết / modal Staff đã bỏ; dùng view `#account-detail?id={id}` chung layout với thêm nhân viên.
