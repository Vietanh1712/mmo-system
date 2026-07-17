# SPEC — Bảng Điều Khiển (Admin Dashboard View)

> **Feature ID:** `feat-admin` | **Page:** `Admin Console (Dashboard Sub-view)`
> **Route:** `/admin/users#dashboard` | **Template:** `templates/admin/users.html`
> **CSS Script:** `static/css/admin/admin.css`
> **JS Script:** `static/js/admin-console.js`
> **Version:** 2.0 | **Status:** Active
> **Backend ref:** `feat-admin/SPEC.md`
> **Last Updated:** 2026-07-16

---

## 1. TỔNG QUAN PHÂN VÙNG

Phân vùng Bảng điều khiển (Dashboard) cung cấp cho Admin cái nhìn toàn cảnh về hệ thống, bao gồm số lượng tài khoản theo trạng thái/vai trò, biểu đồ thống kê doanh thu tuần bằng đồ họa SVG trực quan, và danh sách các hoạt động vận hành gần đây nhất của hệ thống.

---

## 2. DESIGN TOKENS ÁP DỤNG

```css
/* Color Palette */
--ds-primary:         #2563eb;
--ds-success:         #10b981;
--ds-danger:          #ef4444;
--ds-warning:         #f59e0b;
--ds-info:            #0ea5e9;
--ds-border:          #cbd5e1;

/* Layout & Typography */
--ds-radius-md:       8px;
--ds-radius-lg:       12px;
--ds-shadow-sm:       0 1px 3px rgba(0,0,0,0.1);
--ds-shadow-md:       0 4px 6px rgba(0,0,0,0.05);
```

---

## 3. LAYOUT TỔNG THỂ & MOCKUP

```
┌────────────────────────────────────────────────────────────────────────┐
│  MMO Market Admin Console                                       [Admin]│
├────────────────────────────────────────────────────────────────────────┤
│  TỔNG QUAN HỆ THỐNG                                     [ ↻ Làm mới ]  │
│                                                                        │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐   │
│  │Tổng tài khoản│ │Đang hoạt động│ │Bị khóa       │ │Nhân viên     │   │
│  │ 150          │ │ 140          │ │ 10           │ │ 5            │   │
│  └──────────────┘ └──────────────┘ └──────────────┘ └──────────────┘   │
│  ┌──────────────┐ ┌──────────────┐                                     │
│  │Đã xác thực   │ │Người bán     │                                     │
│  │ 120          │ │ 35           │                                     │
│  └──────────────┘ └──────────────┘                                     │
│                                                                        │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │ BIỂU ĐỒ DOANH THU TUẦN (VNĐ)                                     │  │
│  │ Doanh thu (VNĐ)                                                  │  │
│  │   |    .                                                         │  │
│  │   |   / \_                                                       │  │
│  │   |  /    \___                                                   │  │
│  │   | /         \                                                  │  │
│  │   └─────────────── Ngày trong tuần                               │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│                                                                        │
│  HOẠT ĐỘNG GẦN ĐÂY                                                     │
│  ┌─────┬───────────┬──────────────┬──────────────────┬──────────────┐  │
│  │ STT │ Thời gian │ Nhân viên    │ Hành động        │ Mô tả        │  │
│  ├─────┼───────────┼──────────────┼──────────────────┼──────────────┤  │
│  │ 1   │ 12:30:15  │ admin@mmo.com│ [CONFIG_UPDATE]  │ Cấu hình phí │  │
│  └─────┴───────────┴──────────────┴──────────────────┴──────────────┘  │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 4. CÁC THÀNH PHẦN GIAO DIỆN CHÍNH

### 4.1 Thẻ số liệu thống kê — `.stat-card-modern`
* Thẻ hiển thị số lượng tài khoản theo màu sắc trạng thái:
  * `#statTotal`: Thẻ màu xanh dương đậm (mặc định).
  * `#statActive`: Thẻ màu xanh lá đại diện cho tài khoản hoạt động.
  * `#statLocked`: Thẻ màu đỏ đại diện cho tài khoản bị khóa.
  * `#statStaff`: Thẻ màu vàng đại diện cho tài khoản nhân viên.
  * `#statVerified`: Thẻ màu xanh da trời hiển thị tài khoản đã xác thực KYC.
  * `#statSeller`: Thẻ màu xanh da trời hiển thị tài khoản Seller.

### 4.2 Biểu đồ doanh thu tuần SVG — `#revenueChartSvg`
* Đồ thị được vẽ trực tiếp bằng JS bằng thẻ SVG với vùng bóng `linearGradient` mờ.
* Các mốc điểm tròn đại diện cho doanh số từng ngày trong tuần. Khi hover vào điểm tròn, thẻ Tooltip `#chartTooltip` hiển thị thông tin doanh thu VNĐ tương ứng.

### 4.3 Bảng nhật ký gần đây — `#dashLogsBody`
* Bảng kết xuất 5 hành động audit log mới nhất của hệ thống nhằm kiểm soát bảo mật nhanh.

---

## 5. LUỒNG XỬ LÝ JS & AJAX

1. **Khởi tạo dữ liệu khi tải view:**
   * Gửi AJAX lấy tổng quan số liệu tài khoản:
     * **Endpoint:** `GET /api/admin/user-management/summary`
     * **Response (200 OK):** Cập nhật các ô số đếm `#statTotal`, `#statActive`, `#statLocked`, `#statStaff`, `#statVerified`, `#statSeller`.
2. **Vẽ biểu đồ doanh thu tuần:**
   * Gọi API `GET /api/admin/revenue/summary` lấy dữ liệu doanh số thực tế để tính toán tọa độ SVG.
   * Tạo chuỗi SVG path `d="M ... L ..."` vẽ đường line (`#revenueChartPath`) và vùng bóng phủ mờ phía dưới (`#revenueChartArea`).
   * Gắn sự kiện lắng nghe chuột (Mouseover/Mouseout) trên các chấm tròn `#revenueChartDots` để di chuyển tọa độ hiển thị và điền text số tiền VNĐ vào Tooltip `#chartTooltip`.
3. **Hiển thị nhật ký gần đây:**
   * Gửi AJAX lấy danh sách log mới nhất:
     * **Endpoint:** `GET /api/admin/audit-logs?page=0&size=5`
     * **Response (200 OK):** Render HTML vào bảng `#dashLogsBody`.
