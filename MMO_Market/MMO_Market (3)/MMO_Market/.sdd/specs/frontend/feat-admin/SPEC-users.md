# SPEC — Admin User Management Page
> **Route:** `/admin/users` | **Template:** `templates/admin/users.html`
> **JS Script:** `static/js/admin-console.js` | **Prefix:** `ausr-`

---

## 1. MÔ TẢ TRANG
Giao diện quản lý toàn bộ thành viên trên hệ thống. Admin có thể tra cứu thông tin người dùng, thực hiện khóa (block) tài khoản vi phạm và phân quyền cho Staff.

---

## 2. MOCKUP GIAO DIỆN (ASCII)
```
┌────────────────────────────────────────────────────────┐
│  Admin Console > Thành viên hệ thống                   │
├────────────────────────────────────────────────────────┤
│  [ 🔍 Tìm kiếm theo email... ]                         │
│                                                        │
│  ┌──────┬──────────────────────┬─────────┬──────────┐  │
│  │ ID   │ Email                │ Vai trò │ Thao tác │  │
│  ├──────┼──────────────────────┼─────────┼──────────┤  │
│  │ 12   │ customer@gmail.com   │ Customer│ [Khóa]   │  │
│  │ 13   │ staff1@mmo.com       │ Staff   │ [Quyền]  │  │
│  └──────┴──────────────────────┴─────────┴──────────┘  │
└────────────────────────────────────────────────────────┘
```

---

## 3. LUỒNG XỬ LÝ PHÂN QUYỀN STAFF (RBAC)
1. Admin click button "Quyền" cạnh tài khoản Staff.
2. Hiển thị modal danh sách quyền hạn.
3. Admin tick chọn các checkbox quyền (ví dụ: `APPROVE_KYC`, `APPROVE_WITHDRAWALS`).
4. Bấm "Cập nhật" -> Gửi request `POST /api/admin/staff-permissions/assign` để lưu cấu hình.