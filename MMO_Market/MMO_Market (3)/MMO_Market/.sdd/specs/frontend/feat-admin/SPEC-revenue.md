# SPEC — Doanh Thu & Dòng Tiền (Admin Revenue Statistics Page)

> **Feature ID:** `feat-admin` | **Page:** `Admin Console (Revenue Sub-view)`
> **Route:** `/admin/users#revenue` | **Template:** `templates/admin/users.html`
> **CSS Script:** `static/css/admin/admin.css`
> **JS Script:** `static/js/admin-console.js`
> **Version:** 2.1 | **Status:** Active
> **Backend ref:** `feat-admin/SPEC.md`
> **Last Updated:** 2026-07-23

---

## 1. TỔNG QUAN PHÂN VÙNG

Phân vùng Doanh thu & Dòng tiền cung cấp cho Admin báo cáo tổng quát về 3 nguồn thu tài chính thực tế của sàn (phí hoa hồng C2C từ đơn hàng hoàn thành, phí mở Shop của Seller, và phí thu được từ các lệnh rút tiền) kèm bảng lịch sử giao dịch dòng tiền toàn sàn.

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
│  │[🔍 Tìm mã GD...]│ │[Tất cả nguồn  ▾]│ │[Trạng thái  ▾]│ │[Mới ➔ Cũ▾]│ │
│  └─────────────────┘ └─────────────────┘ └──────────────┘ └──────────┘ │
│  ┌─────────────────┐ ┌─────────────────┐                               │
│  │[Từ ngày       ] │ │[Đến ngày      ] │                               │
│  └─────────────────┘ └─────────────────┘                               │
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
  * `#revCommissions`: Tổng hoa hồng thu được từ Transactions (`C2C_Purchase`).
  * `#revBuyerFees`: Tổng phí mở shop thu được từ SellerRegistrations (`Shop_Opening`).
  * `#revWithdrawalFees`: Tổng phí rút tiền thu được từ Withdrawals (`Withdrawal`).
  * `#revNetTotal`: Doanh thu ròng tổng cộng (tổng của 3 nguồn thu trên).

### 4.2 Bộ lọc dòng tiền — `.ds-filter-panel`
* Tìm kiếm từ khóa theo mã giao dịch hoặc email người dùng `#revKeywordFilter`.
* Lọc nguồn doanh thu `#revTypeFilter`: `C2C_Purchase` (Giao dịch C2C), `Shop_Opening` (Phí mở shop), `Withdrawal` (Rút tiền).
* Lọc trạng thái `#revStatusFilter`: `Completed`, `Pending`, `Held`, `Failed`.
* Lọc theo ngày bắt đầu `#revStartDate` và ngày kết thúc `#revEndDate`.
* Sắp xếp `#revSortOrder`: Mới nhất trước (`DESC`) hoặc Cũ nhất trước (`ASC`).
* **Cơ chế tìm kiếm thủ công**: Đổi lựa chọn không tự động lọc; chỉ chạy khi ấn **"Tìm kiếm"**, ấn **Enter** hoặc **"Làm mới bộ lọc"**.

### 4.3 Danh sách giao dịch dòng tiền — `#revTransactionsBody`
* Bảng hiển thị thông tin STT, Mã giao dịch, Thời gian, Email tài khoản thực hiện, Loại giao dịch (badge màu), Số tiền giao dịch, Phí sàn thu được, và Trạng thái giao dịch.

---

## 5. LUỒNG XỬ LÝ JS & AJAX

1. **Tải thông số thống kê doanh thu:**
   * Gửi AJAX lấy dữ liệu tổng quan:
     * **Endpoint:** `GET /api/admin/revenue/summary`
     * **Response (200 OK):** Format số tiền sang VNĐ và chèn vào các thẻ `#revCommissions`, `#revBuyerFees`, `#revWithdrawalFees`, `#revNetTotal`.
2. **Tải danh sách giao dịch dòng tiền phân trang:**
   * Gửi AJAX khi người dùng click nút "Tìm kiếm":
     * **Endpoint:** `GET /api/admin/revenue/transactions`
     * **Query Params:** `keyword`, `type`, `status`, `startDate`, `endDate`, `sort`, `page`, `size`.
     * **Response (200 OK):** Render dữ liệu vào `#revTransactionsBody` và dựng thanh phân trang `#revenuePagination`.
3. **Xuất báo cáo doanh thu CSV an toàn:**
   * Khi nhấn "Xuất Excel", gửi request xác thực `authFetch('/admin/revenue/export?...')` đính kèm Token và tải về file CSV `bao-cao-doanh-thu-YYYY-MM-DD.csv`.