# SPEC — Thống Kê Doanh Thu & Kênh Người Bán (Seller Dashboard Metrics)

> **Feature ID:** `feat-seller` | **Page:** `SellerDashboard`
> **Route:** `/seller` | **Template:** `templates/seller/dashboard.html`
> **CSS Script:** `static/css/customer/style.css`
> **JS Script:** `static/js/seller-console.js`
> **Version:** 1.0 | **Status:** Draft
> **Backend ref:** `feat-seller/UC-04-seller-registration.md`

---

## 1. TỔNG QUAN TRANG

Trang tổng quan người bán hiển thị các số liệu thống kê đo lường tài chính quan trọng của Shop bao gồm: Số dư khả dụng có thể rút, Số dư đang bị tạm giữ trong quỹ Escrow bảo vệ người mua, tổng số đơn hàng đã bán và danh sách các đơn hàng mới nhất đang chờ xử lý.

**Cấu trúc trang:**
1. **Sidebar điều hướng Kênh Người Bán:** Dashboard, Quản lý kho hàng (Inventory), Lịch sử đơn hàng bán, Yêu cầu rút tiền (Withdrawals).
2. **Khối thẻ chỉ số thống kê (Metrics Grid):**
   * *Thẻ khả dụng (Available Balance Card):* Số tiền hiện có thể tạo yêu cầu rút về ngân hàng.
   * *Thẻ tạm giữ (Hold Balance Card):* Tổng doanh thu đang giam 72h.
   * *Thẻ đơn hàng (Total Orders Card):* Số lượng đơn hàng đã phát sinh.
3. **Bảng đơn hàng bán gần đây:** Danh sách tóm tắt các giao dịch bán hàng mới nhất.

---

## 2. DESIGN TOKENS ÁP DỤNG

```css
/* Color System */
--ds-primary:         #2563eb;
--ds-primary-hover:   #1d4ed8;
--ds-bg:              #f8fafc;
--ds-card:            #ffffff;
--ds-border:          #cbd5e1;
--ds-error:           #ef4444;
--ds-success:         #10b981;
--ds-warning:         #f59e0b;

/* Shape & Spacing */
--ds-radius-md:       8px;
--ds-radius-lg:       12px;
--ds-shadow:          0 1px 3px 0 rgba(0,0,0,0.1);
```

---

## 3. LAYOUT TỔNG THỂ & MOCKUP

```
┌────────────────────────────────────────────────────────┐
│ [Logo] MMO Market Kênh Người Bán               [Shop]  │
├────────────────────────────────────────────────────────┤
│  ┌───────────┐  ┌────────────────────────────────────┐ │
│  │  Sidebar  │  │  TỔNG QUAN KÊNH NGƯỜI BÁN          │ │
│  │  - Dashbd │  │  ┌──────────────┐ ┌──────────────┐  │ │
│  │  - Kho    │  │  │ Doanh thu    │ │ Đang tạm giữ │  │ │
│  │  - Orders │  │  │ 2.500.000đ   │ │ 500.000đ     │  │ │
│  │  - Rúttiền│  │  └──────────────┘ └──────────────┘  │ │
│  └───────────┘  │  ĐƠN HÀNG BÁN GẦN ĐÂY              │ │
│                 │  ┌──────────────────────────────┐  │ │
│                 │  │#OR98 | Netflix | 50K | ESCROW│  │ │
│                 │  └──────────────────────────────┘  │ │
│                 └────────────────────────────────────┘ │
└────────────────────────────────────────────────────────┘
```

---

## 4. CÁC THÀNH PHẦN GIAO DIỆN CHÍNH

### 4.1 Thẻ Chỉ Số Thống Kê — `.sdsh-metric-card`
* Bo tròn `var(--ds-radius-lg)`, shadow nhẹ. Tiêu đề chỉ số viết nhỏ xám, giá trị số liệu to đậm nổi bật.
* Có icon đại diện ở góc phải (ví dụ: Biểu tượng ví tiền, biểu tượng xe đẩy giỏ hàng).

### 4.2 Bảng Đơn Hàng Gần Đây — `.sdsh-recent-table`
* Bảng hiển thị thông tin: Mã đơn hàng, Tên sản phẩm, Số tiền, Trạng thái đơn hàng, Thời gian giao dịch.
* Trạng thái đơn hàng hiển thị dạng badge màu sắc tương ứng: xanh cho `Completed`, vàng cho `Escrow`, đỏ cho `Complaint`.

---

## 5. LUỒNG XỬ LÝ JS & AJAX

1. **Khởi tạo dữ liệu khi load trang:**
   * Gửi API lấy số liệu thống kê:
     * **Endpoint:** `GET /api/v1/seller/dashboard/metrics`
     * **Headers:** `Authorization: Bearer <token>`
   * **Thành công (HTTP 200):** Cập nhật dữ liệu vào các thẻ chỉ số.
   * Gửi API lấy danh sách giao dịch gần đây:
     * **Endpoint:** `GET /api/v1/seller/dashboard/recent-orders`
     * **Headers:** `Authorization: Bearer <token>`
   * **Thành công (HTTP 200):** Kết xuất mảng dữ liệu chèn vào bảng `.sdsh-recent-table`.

---

## 6. RESPONSIVE

* **Viewport ≥ 992px:** Thẻ chỉ số hiển thị dạng lưới 3 cột. Layout chia 2 cột lớn (Sidebar trái, Content phải).
* **Viewport < 992px:** Sidebar chuyển thành menu cuộn ngang trên cùng. Thẻ chỉ số hiển thị dạng lưới 2 cột hoặc 1 cột dọc tùy kích thước màn hình.

---

## 7. ACCESSIBILITY

- Sử dụng các thẻ HTML5 semantic thích hợp (`<nav>` cho sidebar, `<main>` cho content).
- Các bảng số liệu có đầy đủ cấu trúc `<thead>`, `<tbody>` và thẻ `<th>` kèm thuộc tính `scope`.

---

## 8. OUT OF SCOPE

- ❌ Vẽ biểu đồ đồ họa (Line/Bar Chart) thời gian thực sử dụng thư viện ngoài Chart.js (Chỉ hiển thị con số và bảng dữ liệu tĩnh).