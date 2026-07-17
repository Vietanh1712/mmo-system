# SPEC — Nhật Ký Hệ Thống (Admin Audit Logs View)

> **Feature ID:** `feat-admin` | **Page:** `Admin Console (Audit Logs Sub-view)`
> **Route:** `/admin/users#audit-logs` | **Template:** `templates/admin/users.html`
> **CSS Script:** `static/css/admin/admin.css`
> **JS Script:** `static/js/admin-console.js`
> **Version:** 2.0 | **Status:** Active
> **Backend ref:** `feat-admin/SPEC.md`
> **Last Updated:** 2026-07-16

---

## 1. TỔNG QUAN PHÂN VÙNG

Phân vùng Nhật ký hệ thống (Audit Logs) cung cấp cho Admin cổng giám sát toàn bộ các hành động nhạy cảm trong hệ thống do Admin hoặc Staff thực hiện. Dữ liệu logs được lưu trữ bất biến phục vụ kiểm toán bảo mật.

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
│  ┌───────────────────────────────┐ ┌────────────────────────────────┐  │
│  │[🔍 Tìm nhân viên, IP, mô tả ] │ │[Tất cả hành động             ▾]│  │
│  └───────────────────────────────┘ └────────────────────────────────┘  │
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
* Ô tìm kiếm từ khóa `#logSearch` dùng để lọc nhanh theo email nhân viên, địa chỉ IP hoặc nội dung chi tiết.
* Dropdown `#logActionFilter` chứa danh sách các hành động có sẵn (như `KYC_Approve`, `Fund_Withdraw`, `Lock_User`, `Config_Update`, `Maintenance_Toggle`).

### 4.2 Bảng dữ liệu nhật ký kiểm toán — `#auditLogsBody`
* Hiển thị danh sách log hệ thống:
  * Cột Hành động: Hiển thị các badge màu sắc tương ứng với mức độ nhạy cảm của tác vụ.
  * Cột Thao tác: Chứa nút "Xem" để kích hoạt Modal xem chi tiết JSON payload.

### 4.3 Modal xem chi tiết log — `#notifDetailModal`
* Hộp thoại bật lên hiển thị cấu trúc dữ liệu JSON thô đã lưu trong DB (chứa thông tin địa chỉ IP, mô tả và đối tượng diff thay đổi dữ liệu cấu hình).

---

## 5. LUỒNG XỬ LÝ JS & AJAX

1. **Khởi tạo dữ liệu phân trang:**
   * Gửi yêu cầu lấy danh sách log khi click "Tìm kiếm" hoặc đổi trang:
     * **Endpoint:** `GET /api/admin/audit-logs`
     * **Query Params:** `search` (từ ô `#logSearch`), `action` (từ `#logActionFilter`), `page`, `size`.
     * **Response (200 OK):** Trả về trang dữ liệu, thực hiện render HTML dòng bảng vào `#auditLogsBody` và tạo thanh phân trang `#auditPagination`.
2. **Xem chi tiết JSON Payload:**
   * Khi click nút "Xem", lấy trường `details` của dòng log tương ứng.
   * Parse chuỗi JSON đó và hiển thị định dạng đẹp (pretty-print) vào `#modalNotifContent` bên trong modal `#notifDetailModal`.
3. **Xuất báo cáo CSV:**
   * Click "Xuất Excel", hệ thống điều hướng trực tiếp hoặc gọi `GET /api/admin/audit-logs/export?search=...&action=...` để tải tệp CSV về máy.
