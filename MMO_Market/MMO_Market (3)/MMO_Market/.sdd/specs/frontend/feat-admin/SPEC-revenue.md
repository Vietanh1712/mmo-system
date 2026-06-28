# SPEC — Admin Revenue Statistics Page
> **Route:** `/admin/users` (Tab: Revenue) | **Template:** `templates/admin/users.html`
> **JS Script:** `static/js/admin-console.js`

---

## 1. MÔ TẢ TRANG
Giao diện kết xuất báo cáo doanh số toàn sàn, tổng giá trị đơn hàng hoàn thành và hoa hồng hệ thống thu về dựa trên cài đặt biểu phí.

---

## 2. MOCKUP GIAO DIỆN (ASCII)
```
┌────────────────────────────────────────────────────────┐
│  Admin Console > Báo cáo doanh số                      │
├────────────────────────────────────────────────────────┤
│  Tổng giao dịch (VNĐ): 150.000.000đ                    │
│  Doanh số hoa hồng:    15.000.000đ  (Biểu phí: 10%)    │
│                                                        │
│  Biểu đồ phân bố doanh thu theo danh mục:              │
│  - Netflix Premium: 60% [██████░░░░]                   │
│  - Spotify Premium: 40% [████░░░░░░]                   │
└────────────────────────────────────────────────────────┘
```

---

## 3. LUỒNG TẢI DỮ LIỆU
1. Gọi API `GET /api/admin/revenue/summary` lấy số liệu tổng quan.
2. Điền dữ liệu thống kê vào các thẻ hiển thị trên màn hình.