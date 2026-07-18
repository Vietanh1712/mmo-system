# SPEC — Quản Lý Thành Viên & Staff (Admin User & Staff Management Page)

> **Feature ID:** `feat-admin` | **Page:** `Admin Console (Accounts & Staff Form Sub-views)`
> **Route:** `/admin/users` | **Template:** `templates/admin/users.html`
> **CSS Script:** `static/css/admin/admin.css`
> **JS Script:** `static/js/admin-console.js` | **Prefix:** `ausr-`
> **Version:** 2.0 | **Status:** Active
> **Backend ref:** `feat-admin/UC-15-system-administration.md`
> **Last Updated:** 2026-07-16

---

## 1. TỔNG QUAN PHÂN VÙNG

Giao diện Quản lý thành viên cho phép Admin tra cứu thông tin toàn bộ người dùng của hệ thống, thực hiện khóa/mở khóa tài khoản vi phạm chính sách, thay đổi vai trò trực tiếp, và thực hiện thêm mới/sửa/xóa tài khoản nhân sự Staff.

---

## 2. DESIGN TOKENS ÁP DỤNG

```css
/* Color Palette */
--ds-primary:         #2563eb;
--ds-success:         #10b981;
--ds-danger:          #ef4444;
--ds-border:          #cbd5e1;

/* Avatar size */
--ds-avatar-sm:       32px;
```

---

## 3. LAYOUT TỔNG THỂ & MOCKUP

### 3.1 Màn hình Danh sách tài khoản (`#accountsView`)
```
┌────────────────────────────────────────────────────────────────────────┐
│  MMO Market Admin Console                                       [Admin]│
├────────────────────────────────────────────────────────────────────────┤
│  QUẢN LÝ TÀI KHOẢN                                     [+ Thêm Staff]  │
│                                                                        │
│  BỘ LỌC TÀI KHOẢN                                                      │
│  ┌───────────────────────────────┐ ┌────────────────────────────────┐  │
│  │[🔍 Tìm email, họ tên, sđt... ] │ │[Tất cả vai trò               ▾]│  │
│  └───────────────────────────────┘ └────────────────────────────────┘  │
│  [ Làm mới bộ lọc ]  [ Tìm kiếm ]                                      │
│                                                                        │
│  ┌─────┬──────┬─────────────────┬──────────┬──────────┬────────┬──────┐│
│  │ STT │ Mã   │ Thành viên      │ Vai trò  │ Wallet   │ T.Thái │ T.Tác││
│  ├─────┼──────┼─────────────────┼──────────┼──────────┼────────┼──────┤│
│  │ 1   │ #U102│ (A) user@mmo.com│ [Seller] │ ₫500.000 │ (o) ON │[Sửa] ││
│  └─────┴──────┴─────────────────┴──────────┴──────────┴────────┴──────┘│
│  [Trang trước]  [1] [2] [3]  [Trang sau]                               │
└────────────────────────────────────────────────────────────────────────┘
```

### 3.2 Màn hình Form thêm/sửa nhân viên Staff (`#accountFormView`)
```
┌────────────────────────────────────────────────────────────────────────┐
│  Thêm tài khoản nhân viên mới                                          │
├────────────────────────────────────────────────────────────────────────┤
│  Thông tin đăng nhập                                                   │
│  * Email:    [                     ]  * Mật khẩu: [                  ] │
│                                                                        │
│  Thông tin cá nhân                                                     │
│  * Họ tên:   [                     ]  Số điện thoại: [               ] │
│  Số CCCD:    [                     ]  Ngày sinh:   [                 ] │
│  Giới tính:  (●) Nam   ( ) Nữ                                          │
│  Địa chỉ:    [                                                       ] │
│                                                                        │
│  Trạng thái vận hành                                                   │
│  Đang hoạt động: (o) Bật để cho phép đăng nhập                         │
│                                                                        │
│                                           [ HỦY ]   [ ✓ TẠO NHÂN VIÊN ]│
└────────────────────────────────────────────────────────────────────────┘
```

---

## 4. CÁC THÀNH PHẦN GIAO DIỆN CHÍNH

### 4.1 Danh sách thành viên — `#usersBody`
* Bảng hiển thị thông tin STT, ID người dùng, Cột Thành viên (chứa ảnh đại diện avatar tròn cỡ nhỏ sinh từ chữ cái đầu tiên của họ tên, email, tên hiển thị), Vai trò (badge màu sắc), Cờ xác thực KYC, Số dư ví, Ngày tạo, Nút Toggle Switch trạng thái hoạt động của tài khoản (xanh lá nếu active, xám nếu bị khóa), và Nút hành động "Sửa" hoặc "Xóa".

### 4.2 Form thêm/sửa Staff — `#accountForm`
* Form điền thông tin đăng nhập (Email, Mật khẩu) và thông tin cá nhân (Họ tên, SĐT, CCCD, Ngày sinh, Giới tính, Địa chỉ).
* Khi sửa tài khoản nhân viên hiện tại, form hiển thị thêm vùng chứa danh sách Tag các quyền hệ thống đang sở hữu `#accountFormPermissionsList`.

---

## 5. LUỒNG XỬ LÝ JS & AJAX

### 5.1 Xử lý Danh sách tài khoản:
* Gửi AJAX tải danh sách thành viên phân trang:
  * **Endpoint:** `GET /api/admin/user-management/users`
  * **Query Params:** `email`/`phone`/`name` (từ `#searchInput`), `role` (từ `#roleFilter`), `page`, `size`.
  * **Response (200 OK):** Render HTML bảng `#usersBody` và thanh phân trang `#accountsPagination`.
* **Khóa/Mở khóa tài khoản nhanh:**
  * Khi Admin click vào nút toggle switch trạng thái ở một dòng:
  * Gửi AJAX: `POST /api/admin/user-management/users/{userId}/toggle-lock`.
  * Trả về thành công, thay đổi giao diện nút toggle switch sang trạng thái mới (màu sắc và chữ hiển thị) và phát sinh Audit Log hành động tương ứng.

### 5.2 Xử lý Form Staff:
* **Tạo nhân viên mới:**
  * Validate các trường bắt buộc (Email đúng định dạng, Mật khẩu >= 6 ký tự, Họ tên không trống).
  * Gửi AJAX: `POST /api/admin/user-management/staff` với payload DTO `StaffUpsertRequest`.
  * Trả về thành công, quay lại màn hình danh sách tài khoản, hiển thị toast thông báo thành công và reload danh sách.
* **Sửa nhân viên:**
  * Gọi API `GET /api/admin/user-management/users/{userId}` để lấy dữ liệu cũ và điền vào các ô nhập dữ liệu của form.
  * Khi gửi lưu, gọi AJAX: `PUT /api/admin/user-management/staff/{staffId}`.
* **Xóa nhân viên:**
  * Admin click nút "Xóa" trên dòng nhân viên.
  * Gửi AJAX: `DELETE /api/admin/user-management/staff/{staffId}`.
  * Bản ghi nhân viên được đánh dấu xóa mềm `isDelete = 1` trong DB.