# SPEC — Doanh Thu & Dòng Tiền (Admin Revenue Statistics Page)

> **Feature ID:** `feat-admin` | **Page:** `Admin Console (Revenue Sub-view)`
> **Route:** `/admin/users#revenue` | **Template:** `templates/admin/users.html`
> **CSS Script:** `static/css/admin/admin.css`
> **JS Script:** `static/js/admin-console.js`
> **Version:** 2.0 | **Status:** Active
> **Backend ref:** `feat-admin/UC-15-system-administration.md`
> **Last Updated:** 2026-07-16

---

## 1. TỔNG QUAN PHÂN VÙNG

Phân vùng Doanh thu & Dòng tiền cung cấp cho Admin báo cáo tổng quát về các nguồn thu tài chính của sàn (hoa hồng C2C từ đơn hàng hoàn thành, phí mở Shop của Seller, và phí thu được từ các lệnh rút tiền đã thực hiện) kèm bảng lịch sử giao dịch dòng tiền toàn sàn.

---

## 2. DESIGN TOKENS ÁP DỤNG

```css
/* Color Palette */
--ds-primary:         #2563eb;
--ds-success:         #10b981;
--ds-warning:         #f59e0b;
--ds-border:          #cbd5e1;

/* Font Rules */
--ds-money-font:      "Roboto Mono", monospace;
```

---

## 3. LAYOUT TỔNG THỂ & MOCKUP

```
┌────────────────────────────────────────────────────────────────────────┐
│  MMO Market Admin Console                                       [Admin]│
├────────────────────────────────────────────────────────────────────────┤
│  BÁO CÁO DOANH THU & DÒNG TIỀN                                         │
│                                                                        │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐   │
│  │Hoa hồng      │ │Phí mở shop   │ │Phí rút tiền  │ │Doanh thu ròng│   │
│  │ ₫15.000.000  │ │ ₫5.000.000   │ │ ₫2.000.000   │ │ ₫22.000.000  │   │
│  └──────────────┘ └──────────────┘ └──────────────┘ └──────────────┘   │
│                                                                        │
│  BỘ LỌC GIAO DỊCH                                                      │
│  ┌─────────────────┐ ┌─────────────────┐ ┌──────────────┐ ┌──────────┐ │
│  │[🔍 Tìm mã GD...]│ │[Tất cả loại  ▾] │ │[Từ ngày    ] │ │[Đến ngày]│ │
│  └─────────────────┘ └─────────────────┘ └──────────────┘ └──────────┘ │
│  [ Làm mới bộ lọc ]  [ Tìm kiếm ]  [ 💾 Xuất Excel ]                   │
│                                                                        │
│  ┌─────┬─────────┬──────────┬─────────────────┬──────────┬──────────┐  │
│  │ STT │ Mã GD   │Thời gian │ Tài khoản       │ Số tiền  │Phí sàn   │  │
│  ├─────┼─────────┼──────────┼─────────────────┼──────────┼──────────┤  │
│  │ 1   │ #TX102  │12:30:15  │ seller@gmail.com│₫500.000  │₫25.000   │  │
│  └─────┴─────────┴──────────┴─────────────────┴──────────┴──────────┘  │
│  [Trang trước]  [1] [2] [3]  [Trang sau]                               │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 4. CÁC THÀNH PHẦN GIAO DIỆN CHÍNH

### 4.1 Khối thẻ doanh thu ròng — `.stats-grid-4`
* Bốn thẻ hiển thị doanh thu ròng lấy trực tiếp từ hệ thống:
  * `#revCommissions`: Tổng hoa hồng thu được từ Transactions (Completed, Held).
  * `#revBuyerFees`: Tổng phí mở shop thu được từ SellerRegistrations (Approved).
  * `#revWithdrawalFees`: Tổng phí rút tiền thu được từ Withdrawals (Completed).
  * `#revNetTotal`: Doanh thu ròng tổng cộng (tổng của 3 nguồn thu trên).
* Chữ số tiền tệ được định dạng dạng VNĐ đẹp và căn chỉnh bằng font `var(--ds-money-font)`.

### 4.2 Bộ lọc dòng tiền — `.ds-filter-panel`
* Tìm kiếm từ khóa theo mã giao dịch hoặc email người dùng `#revKeywordFilter`.
* Lọc loại giao dịch `#revTypeFilter` (Nạp tiền, Rút tiền, Mua hàng).
* Lọc theo ngày bắt đầu `#revStartDate` và ngày kết thúc `#revEndDate`.

### 4.3 Danh sách giao dịch dòng tiền — `#revTransactionsBody`
* Bảng hiển thị thông tin STT, Mã giao dịch, Thời gian, Email tài khoản thực hiện, Loại giao dịch (định dạng badge màu), Số tiền giao dịch, Phí sàn thu được, và Trạng thái giao dịch.

---

## 5. LUỒNG XỬ LÝ JS & AJAX

1. **Tải thông số thống kê doanh thu:**
   * Gửi AJAX tải dữ liệu tổng quan:
     * **Endpoint:** `GET /api/admin/revenue/summary`
     * **Response (200 OK):** Format số tiền sang VNĐ và chèn vào các thẻ `#revCommissions`, `#revBuyerFees`, `#revWithdrawalFees`, `#revNetTotal`.
2. **Tải danh sách giao dịch dòng tiền phân trang:**
   * Gửi AJAX khi tải trang hoặc khi người dùng click nút "Tìm kiếm":
     * **Endpoint:** `GET /api/admin/revenue/transactions`
     * **Query Params:** `keyword`, `type`, `startDate`, `endDate`, `page`, `size`.
     * **Response (200 OK):** Kết xuất dữ liệu dòng bảng vào `#revTransactionsBody` và dựng thanh phân trang `#revenuePagination`.
3. **Xuất báo cáo doanh thu CSV:**
   * Khi nhấn nút "Xuất Excel", gọi trực tiếp URL `GET /api/admin/revenue/export` kèm theo các param lọc để tải tệp CSV về máy.