# SPEC — Quản Lý Thông Báo & Bảo Trì (Admin Notification & Maintenance Management)

> **Feature ID:** `feat-admin` | **Page:** `Admin Console (Notifications Sub-panel)`
> **Route:** `/admin/users#notifications` | **Template:** `templates/admin/users.html`
> **CSS Script:** `static/css/admin/admin.css`
> **JS Script:** `static/js/admin-console.js`
> **Version:** 2.1 | **Status:** Active
> **Backend ref:** `feat-admin/SPEC.md`
> **Last Updated:** 2026-07-23

---

## 1. TỔNG QUAN PHÂN VÙNG

Phân vùng Quản lý Thông báo và Bảo trì hệ thống cung cấp giao diện dành riêng cho Admin/Staff để phát hành thông báo toàn sàn, bật/tắt thủ công trạng thái bảo trì hệ thống và kiểm tra danh sách lịch sử thông báo đã phát.

---

## 2. DESIGN TOKENS ÁP DỤNG

```css
/* Color Palette */
--ds-primary:         #2563eb;
--ds-danger:          #ef4444;
--ds-danger-bg:       rgba(239, 68, 68, 0.08);
--ds-border:          #cbd5e1;
```

---

## 3. LAYOUT TỔNG THỂ & MOCKUP

```
┌────────────────────────────────────────────────────────────────────────┐
│  MMO Market Admin Console                                       [Admin]│
├────────────────────────────────────────────────────────────────────────┤
│  QUẢN LÝ THÔNG BÁO & BẢO TRÌ                             [📢 Soạn tin]  │
│                                                                        │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │ ⚠️ HỆ THỐNG ĐANG TRONG CHẾ ĐỘ BẢO TRÌ!                          │  │
│  │ Thông báo: MMO Market bảo trì từ 2h-4h sáng mai.                 │  │
│  │                                            [ Tắt bảo trì ngay ]  │
│  └──────────────────────────────────────────────────────────────────┘  │
│                                                                        │
│  BỘ LỌC THÔNG BÁO                                                      │
│  ┌───────────────────────────────┐ ┌────────────────────────────────┐  │
│  │[🔍 Tìm tiêu đề, nội dung...   ] │ │[Tất cả phân loại             ▾]│  │
│  └───────────────────────────────┘ └────────────────────────────────┘  │
│  ┌───────────────────────────────┐ ┌────────────────────────────────┐  │
│  │[Từ ngày                       ] │ │[Đến ngày                     ]│  │
│  └───────────────────────────────┘ └────────────────────────────────┘  │
│  [ Làm mới bộ lọc ]  [ Tìm kiếm ]                                      │
│                                                                        │
│  ┌─────┬───────────┬────────────────────────────┬──────────┬────────┐  │
│  │ STT │ Thời gian │ Tiêu đề & Nội dung         │ Loại     │Thao tác│  │
│  ├─────┼───────────┼────────────────────────────┼──────────┼────────┤  │
│  │ 1   │ 12:30:15  │ Cảnh báo lừa đảo thẻ game  │ warning  │[Xóa]   │  │
│  └─────┴───────────┴──────────────┴─────────────┴──────────┴────────┘  │
│  [Trang trước]  [1] [2] [3]  [Trang sau]                               │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 4. CÁC THÀNH PHẦN GIAO DIỆN CHÍNH

### 4.1 Banner cảnh báo bảo trì — `#maintActiveBanner`
* Xuất hiện nổi bật phía trên cùng khi chế độ bảo trì `MAINTENANCE_MODE` được bật. Banner có nền đỏ mờ, viền đỏ đậm, hiển thị nội dung thông báo bảo trì `#maintActiveMessage` và nút hành động "Tắt bảo trì ngay".

### 4.2 Bộ lọc thông báo — `.ds-filter-panel`
* Ô tìm kiếm từ khóa `#notifSearch`.
* Dropdown phân loại `#notifTypeFilter` lọc theo loại thông báo: `info`, `warning`, `maintenance`, `policy`.
* Khoảng thời gian `#notifStartDate`, `#notifEndDate`.
* Sắp xếp `#notifSortOrder`: Mới nhất trước (`DESC`) hoặc Cũ nhất trước (`ASC`).
* **Cơ chế tìm kiếm thủ công**: Đổi lựa chọn không tự động lọc; chỉ chạy khi ấn **"Tìm kiếm"**, ấn **Enter** hoặc **"Làm mới bộ lọc"**.

### 4.3 Bảng lịch sử thông báo hệ thống — `#notifHistoryBody`
* Hiển thị danh sách các thông báo broadcast đã phát hành gồm STT, thời gian tạo, tiêu đề & nội dung rút gọn, phân loại, người phát hành, và nút "Xóa" để thực hiện soft delete.

---

## 5. LUỒNG XỬ LÝ JS & AJAX

1. **Kiểm tra trạng thái bảo trì:**
   * Gọi `GET /api/admin/notifications/maintenance-status`.
   * Nếu bảo trì đang hoạt động (`active: true`), xóa class `ds-hidden` để hiển thị banner `#maintActiveBanner` và chèn nội dung vào `#maintActiveMessage`.
2. **Tải danh sách thông báo phân trang:**
   * Gửi AJAX khi người dùng click nút "Tìm kiếm" hoặc chuyển trang:
     * **Endpoint:** `GET /notifications` (hoặc `/api/notifications`)
     * **Query Params:** `search`, `type`, `startDate`, `endDate`, `sort`, `page`, `size`.
     * **Response (200 OK):** Kết xuất dữ liệu vào bảng `#notifHistoryBody`.
3. **Phát hành thông báo mới:**
   * Khi nhấn "Soạn thông báo", hiển thị modal soạn tin.
   * Admin nhập tiêu đề, nội dung, chọn type. Nhấn gửi, gọi AJAX:
     * **Endpoint:** `POST /api/admin/notifications`
     * **Payload:** `{ "title": "...", "content": "...", "type": "...", "activateMaintenance": true/false }`
     * **Thành công (HTTP 200):** Đóng modal, reload lại danh sách và kiểm tra lại trạng thái bảo trì để hiển thị banner. Ghi nhận Audit Log hành động.
4. **Tắt chế độ bảo trì nhanh:**
   * Admin nhấn "Tắt bảo trì ngay" trên banner, gửi AJAX:
     * **Endpoint:** `POST /api/admin/notifications/toggle-maintenance`
     * **Payload:** `{ "active": false }`
     * **Thành công (HTTP 200):** Ẩn banner `#maintActiveBanner`, hiển thị thông báo thành công và ghi nhận Audit Log.
5. **Xóa thông báo:**
   * Admin click nút "Xóa" trên dòng thông báo ở bảng.
   * Gửi AJAX: `DELETE /api/admin/notifications/{id}`.
   * Cập nhật `isDelete = 1` trong DB, reload danh sách tại client và hiện toast. Ghi nhận Audit Log hành động.

