# SPEC — Nhật Ký Hệ Thống (Admin Audit Logs View)

> **Feature ID:** `feat-admin` | **Page:** `Admin Console (Audit Logs Sub-view)`
> **Route:** `/admin/users#audit-logs` | **Template:** `templates/admin/users.html`
> **CSS Script:** `static/css/admin/admin.css`
> **JS Script:** `static/js/admin-console.js`
> **Version:** 2.1 | **Status:** Active
> **Backend ref:** `feat-admin/SPEC.md`
> **Last Updated:** 2026-07-23

---

## 1. TỔNG QUAN PHÂN VÙNG

Phân vùng Nhật ký hệ thống (Audit Logs) cung cấp cho Admin cổng giám sát toàn bộ các hành động nhạy cảm trong hệ thống do Admin hoặc Staff thực hiện. Dữ liệu logs được lưu trữ bất biến phục vụ kiểm toán bảo mật và giải quyết khiếu nại.

---

## 2. DESIGN TOKENS ÁP DỤNG

```css
/* Color Palette */
--ds-primary:         #2563eb;
--ds-border:          #cbd5e1;
--ds-text-muted:      #64748b;

/* Tags Action Color */
--badge-lock:         #ef4444; /* đỏ */
--badge-approve:      #10b981; /* xanh lá */
--badge-config:       #f59e0b; /* vàng */
```

---

## 3. LAYOUT TỔNG THỂ & MOCKUP

```
┌────────────────────────────────────────────────────────────────────────┐
│  MMO Market Admin Console                                       [Admin]│
├────────────────────────────────────────────────────────────────────────┤
│  GIÁM SÁT HOẠT ĐỘNG HỆ THỐNG                                           │
│                                                                        │
│  BỘ LỌC NHẬT KÝ                                                        │
│  ┌──────────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────┐   │
│  │[🔍 Tìm nhân viên]│ │[Nhóm tác vụ ▾]│ │[Hành động  ▾]│ │[Mới ➔ Cũ ▾]│   │
│  └──────────────────┘ └──────────────┘ └──────────────┘ └──────────┘   │
│  ┌──────────────────┐ ┌──────────────┐                                 │
│  │[Từ ngày        ] │ │[Đến ngày   ] │                                 │
│  └──────────────────┘ └──────────────┘                                 │
│  [ Làm mới bộ lọc ]  [ Tìm kiếm ]  [ 💾 Xuất Excel ]                   │
│                                                                        │
│  ┌─────┬──────┬──────────────┬──────────────┬──────────────┬────────┐  │
│  │ STT │ Mã   │ Thời gian    │ Nhân viên    │ Hành động    │Thao tác│  │
│  ├─────┼──────┼──────────────┼──────────────┼──────────────┼────────┤  │
│  │ 1   │ #L09 │ 12:30:15     │ admin@mmo.com│[Config_Update] [Xem]   │  │
│  └─────┴──────┴──────────────┴──────────────┴──────────────┴────────┘  │
│  [Trang trước]  [1] [2] [3]  [Trang sau]             Tổng số: 45 logs  │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 4. CÁC THÀNH PHẦN GIAO DIỆN CHÍNH

### 4.1 Thanh bộ lọc — `.ds-filter-panel`
* **Ô tìm kiếm từ khóa (`#logSearch`)**: Lọc theo email nhân viên hoặc mô tả hành động.
* **Dropdown Cascading Nhóm tác vụ (`#logCategoryFilter`)**: Lọc theo nhóm đại mục (`FINANCE`, `COMPLAINT`, `SHOP`, `USER_MGMT`, `SUPPORT`, `SYSTEM`). Khi thay đổi nhóm tác vụ, danh sách hành động con ở `#logActionFilter` tự động cập nhật lại tương ứng.
* **Dropdown Hành động con (`#logActionFilter`)**: Chọn chính xác hành động cụ thể trong nhóm.
* **Bộ lọc Khoảng thời gian (`#logStartDate`, `#logEndDate`)**: Lọc theo ngày tạo.
* **Thứ tự sắp xếp (`#logSortOrder`)**: Mới nhất trước (`DESC`) hoặc Cũ nhất trước (`ASC`).
* **Cơ chế tìm kiếm thủ công**: Đổi các giá trị trên **không tự động tải lại bảng**. Tìm kiếm chỉ kích hoạt khi người dùng nhấn nút **"Tìm kiếm"**, ấn **Enter** tại ô tìm kiếm, hoặc nhấn **"Làm mới bộ lọc"**.

### 4.2 Bảng dữ liệu nhật ký kiểm toán — `#auditLogsBody`
* Hiển thị thông tin STT, Mã log, Thời gian, Nhân viên thực hiện, Badge hành động, Mô tả rút gọn, và nút "Xem".
* **Lưu ý**: Thông tin địa chỉ IP được ẩn hoàn toàn để tối ưu cho môi trường local.

### 4.3 Trang xem chi tiết nhật ký — `#auditLogDetailView`
* Chuyển thành trang giao diện riêng đồng bộ với hệ thống. Hiển thị thông tin chi tiết của hành động, tài khoản thực hiện, thời gian, và chuỗi JSON thông số Payload/Diff.

---

## 5. LUỒNG XỬ LÝ JS & AJAX

1. **Tải và lọc dữ liệu phân trang:**
   * Chỉ gửi yêu cầu AJAX khi người dùng nhấn nút **"Tìm kiếm"**, ấn **Enter** hoặc đổi trang:
     * **Endpoint:** `GET /api/admin/audit-logs`
     * **Query Params:** `search`, `category`, `action`, `startDate`, `endDate`, `sort`, `page`, `size`.
     * **Response (200 OK):** Render bảng dữ liệu `#auditLogsBody` và dựng lại phân trang `#auditPagination`.
2. **Xem chi tiết nhật ký:**
   * Khi click nút "Xem", chuyển view sang trang chi tiết `#auditLogDetailView` hiển thị dữ liệu log đầy đủ.
3. **Xuất báo cáo CSV an toàn:**
   * Nhấn nút "Xuất Excel", gửi request xác thực `authFetch('/admin/audit-logs/export?...')` đính kèm Token Admin và tải về dạng file Blob URL `nhat-ky-he-thong-YYYY-MM-DD.csv`.
