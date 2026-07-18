# SPEC — Phân Quyền Nhân Viên (Admin Staff Permissions View)

> **Feature ID:** `feat-admin` | **Page:** `Admin Console (Permissions Sub-view)`
> **Route:** `/admin/users#permissions` | **Template:** `templates/admin/users.html`
> **CSS Script:** `static/css/admin/admin.css`
> **JS Script:** `static/js/admin-console.js` (and Staff permission logic)
> **Version:** 2.0 | **Status:** Active
> **Backend ref:** `feat-admin/SPEC.md`
> **Last Updated:** 2026-07-16

---

## 1. TỔNG QUAN PHÂN VÙNG

Phân vùng Phân quyền Nhân viên cho phép Admin gán hoặc thu hồi các quyền hạn chi tiết (KYC, rút tiền, giải quyết khiếu nại, hỗ trợ...) đối với đội ngũ nhân viên hệ thống (Staff) một cách hàng loạt hoặc cá nhân.

---

## 2. DESIGN TOKENS ÁP DỤNG

```css
/* Color Palette */
--ds-primary:         #2563eb;
--ds-primary-hover:   #1d4ed8;
--ds-border:          #cbd5e1;

/* Badges & Tags */
--ds-tag-bg:          rgba(37, 99, 235, 0.08);
--ds-tag-text:        #2563eb;
```

---

## 3. LAYOUT TỔNG THỂ & MOCKUP

```
┌────────────────────────────────────────────────────────────────────────┐
│  MMO Market Admin Console                                       [Admin]│
├────────────────────────────────────────────────────────────────────────┤
│  QUẢN LÝ & PHÂN QUYỀN NHÂN VIÊN                                        │
│                                                                        │
│  BỘ LỌC QUYỀN & GÁN NHÂN VIÊN                                          │
│  ┌───────────────────────────────┐ ┌────────────────────────────────┐  │
│  │[Tất cả nhóm quyền           ▾]│ │[-- Chọn quyền --               ▾]│  │
│  ├───────────────────────────────┤ ├────────────────────────────────┤  │
│  │[-- Chọn nhân viên --        ▾]│ │                                │  │
│  └───────────────────────────────┘ └────────────────────────────────┘  │
│  [➕ Gán quyền cho nhân viên đã chọn]                                   │
│                                                                        │
│  DANH SÁCH PHÂN QUYỀN THỰC TẾ                                          │
│  ┌─────┬──────────────┬───────────────────────────────┬─────────────┐  │
│  │ STT │ Nhân viên    │ Quyền hạn đang sở hữu         │ Thao tác    │  │
│  ├─────┼──────────────┼───────────────────────────────┼─────────────┤  │
│  │ 1   │ staff@mmo.com│ [APPROVE_KYC (x)]             │ [Thu hồi]   │  │
│  └─────┴──────────────┴───────────────────────────────┴─────────────┘  │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 4. CÁC THÀNH PHẦN GIAO DIỆN CHÍNH

### 4.1 Bộ lọc nhóm quyền và Chọn đối tượng — `.ds-filter-panel`
* Dropdown nhóm quyền `#permGroupFilter` hỗ trợ lọc nhanh theo các mảng công việc nghiệp vụ (Kiểm duyệt, Tài chính, Vận hành).
* Dropdown chọn nhiều quyền `#permMultiselectTrigger`: Custom Multiselect dropdown hỗ trợ tìm kiếm nhanh, tick chọn nhiều checkbox quyền đồng thời.
* Dropdown chọn nhiều nhân viên `#multiselectTrigger`: Custom Multiselect dropdown chứa danh sách checkbox tài khoản vai trò `Staff`.

### 4.2 Khối mô tả quyền hạn — `#selectedPermDescBox`
* Thẻ hiển thị mô tả ngắn gọn về quyền đang được chọn để Admin tham chiếu nhanh tác dụng của quyền đó trước khi gán.

### 4.3 Bảng danh sách phân quyền thực tế
* Liệt kê các tài khoản Staff. Cột quyền hạn hiển thị các tag màu xanh dương đại diện cho các quyền. Mỗi tag có icon dấu (x) bên cạnh để Admin click thu hồi nhanh.

---

## 5. LUỒNG XỬ LÝ JS & AJAX

1. **Khởi tạo danh sách quyền và Staff:**
   * Gọi `GET /api/admin/staff-permissions/permissions` để tải tất cả danh sách các quyền của hệ thống chèn vào dropdown.
   * Gọi `GET /api/admin/user-management/users?role=Staff` để tải danh sách các tài khoản nhân viên.
   * Gọi `GET /api/admin/staff-permissions/all-assigned` để lấy dữ liệu phân quyền thực tế vẽ bảng danh sách phân quyền của nhân viên Staff.
2. **Gán quyền hàng loạt:**
   * Admin tick chọn danh sách các quyền và danh sách nhân viên Staff muốn gán.
   * Nhấn nút "Gán quyền...", gửi AJAX:
     * **Endpoint:** `POST /api/admin/staff-permissions/assign`
     * **Payload:**
       ```json
       {
         "userIds": [3, 4],
         "permissionNames": ["APPROVE_WITHDRAW", "RESOLVE_COMPLAINT"]
       }
       ```
     * **Thành công (HTTP 200):** Reload lại bảng danh sách phân quyền và làm sạch các checkbox đã chọn. Ghi nhận Audit Log hành động.
3. **Thu hồi quyền trực tiếp:**
   * Admin click vào nút (x) cạnh tag quyền của nhân viên trên bảng hoặc nhấn "Thu hồi".
   * Gửi AJAX:
     * **Endpoint:** `POST /api/admin/staff-permissions/revoke`
     * **Payload:** `{ "userId": 3, "permissionNames": ["APPROVE_WITHDRAW"] }`
     * **Thành công (HTTP 200):** Loại bỏ tag quyền đó trên giao diện bảng, reload số liệu và hiển thị toast thành công. Ghi nhận Audit Log hành động.
