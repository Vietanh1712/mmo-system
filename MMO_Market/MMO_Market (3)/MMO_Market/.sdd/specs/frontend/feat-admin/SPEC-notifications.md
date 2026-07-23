# SPEC — Quản Lý Thông Báo & Bảo Trì (Admin Notification & Maintenance Management)

> **Feature ID:** `feat-admin` | **Page:** `Admin Console (Notifications Sub-panel)`
> **Route:** `/admin/users#notifications` | **Template:** `templates/admin/users.html`
> **CSS Script:** `static/css/admin/admin.css`
> **JS Script:** `static/js/admin/admin-console.js`
> **Version:** 2.2 | **Status:** Active
> **Backend ref:** `feat-admin/SPEC.md`
> **Last Updated:** 2026-07-23

---

## 1. TỔNG QUAN PHÂN VÙNG

Phân vùng Quản lý Thông báo và Bảo trì hệ thống cung cấp giao diện dành riêng cho Admin/Staff để soạn thảo bản nháp, phát hành thông báo toàn sàn, gỡ/xóa bản nháp, bật/tắt thủ công trạng thái bảo trì hệ thống và kiểm tra danh sách lịch sử thông báo.

Sidebar Admin thiết kế menu dạng Dropdown với 2 mục con:
* 📢 **Quản lý thông báo** (Item chính): Mở trang hiển thị **Tất cả thông báo** (cả Bản nháp lẫn Đã phát hành).
  * 🚀 **Thông báo phát hành**: Lọc danh sách các thông báo đã gửi cho người dùng.
  * 📝 **Quản lý bản nháp**: Lọc danh sách các bản nháp đang lưu.

---

## 2. DESIGN TOKENS ÁP DỤNG

```css
/* Color Palette */
--ds-primary:         #2563eb;
--ds-success:         #10b981;
--ds-warning:         #f59e0b;
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
│  │[Tất cả trạng thái            ▾] │ │[Từ ngày -> Đến ngày          ]│  │
│  └───────────────────────────────┘ └────────────────────────────────┘  │
│  [ Làm mới bộ lọc ]  [ Tìm kiếm ]                                      │
│                                                                        │
│  ┌─────┬───────────┬───────────────────┬──────────┬─────────────┬──────┐  │
│  │ STT │ Thời gian │ Tiêu đề & Nội dung│ Loại     │ Trạng thái  │Thao tác│
│  ├─────┼───────────┼───────────────────┼──────────┼─────────────┼──────┤  │
│  │ 1   │ 12:30:15  │ Cảnh báo lừa đảo  │ warning  │🟢 Đã phát   │ [👁️] │  │
│  │ 2   │ 10:00:00  │ Lịch bảo trì T8   │maint     │🟡 Bản nháp   │[👁️✏️🚀🗑️]│
│  └─────┴───────────┴───────────────────┴──────────┴─────────────┴──────┘  │
│  [Trang trước]  [1] [2] [3]  [Trang sau]                               │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 4. CÁC THÀNH PHẦN GIAO DIỆN CHÍNH & QUY TẮC NGHIỆP VỤ

### 4.1 Banner cảnh báo bảo trì — `#maintActiveBanner`
* Xuất hiện nổi bật phía trên cùng khi chế độ bảo trì `MAINTENANCE_MODE` được bật.

### 4.2 Bộ lọc thông báo — `.ds-filter-panel`
* Ô tìm kiếm từ khóa `#notifSearch`.
* Dropdown phân loại `#notifTypeFilter`: `info`, `warning`, `maintenance`, `policy`.
* Dropdown trạng thái `#notifStatusFilter`: `ALL` (Tất cả trạng thái), `PUBLISHED` (Đã phát hành), `DRAFT` (Bản nháp).
* Khoảng thời gian `#notifStartDate`, `#notifEndDate`.
* Sắp xếp `#notifSortOrder`: `DESC` / `ASC`.
* **Cơ chế tìm kiếm thủ công**: Đổi lựa chọn không tự động lọc; chỉ chạy khi ấn **"Tìm kiếm"**, ấn **Enter** hoặc **"Làm mới bộ lọc"**.


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

