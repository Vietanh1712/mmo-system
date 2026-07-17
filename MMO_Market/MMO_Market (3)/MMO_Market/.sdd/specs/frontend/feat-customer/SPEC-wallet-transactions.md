# SPEC — Lịch Sử Giao Dịch Ví (Wallet Transactions)

> **Feature ID:** `feat-wallet` | **Page:** `Wallet Transactions`
> **Route:** `/wallet/transactions` | **Template:** `templates/account/wallet-transactions.html`
> **CSS Script:** `static/css/customer/style.css`
> **JS Script:** `static/js/wallet-transactions.js`
> **Version:** 1.0 | **Status:** Published

---

## 1. TỔNG QUAN TRANG

Trang Lịch sử giao dịch ví cho phép người dùng theo dõi toàn bộ biến động số dư, yêu cầu nạp tiền, thanh toán đơn hàng và các giao dịch rút tiền đang xử lý. Trang cung cấp bộ lọc chuyên sâu để tra cứu.

**Cấu trúc trang:**
1. **Sidebar điều hướng:** Menu bên trái (Lịch sử giao dịch ví đang active).
2. **Khu vực Nội dung chính:** 
   - Thẻ hiển thị Số dư nhanh.
   - Bốn khối thống kê số liệu (Tổng nạp, Tổng chi, Đang xử lý, Tổng giao dịch).
   - Bộ lọc giao dịch đa tiêu chí.
   - Bảng danh sách giao dịch chi tiết kèm phân trang.

---

## 2. LAYOUT TỔNG THỂ & MOCKUP

```text
┌─────────────────────────────────────────────────────────────────┐
│ [Logo] MMO Market                         [Search]      [User]  │
├─────────────────────────────────────────────────────────────────┤
│  ┌──────────────────────┐  ┌─────────────────────────────────┐  │
│  │ (User Card)          │  │ Ví & giao dịch                  │  │
│  │                      │  │ Lịch sử giao dịch ví            │  │
│  │ (Menu Sidebar)       │  │ Theo dõi toàn bộ biến động...   │  │
│  │ - Ví của tôi         │  │                 [Về ví] [Nạp tiền]│  │
│  │ - Nạp tiền           │  ├─────────────────────────────────┤  │
│  │ - Lịch sử GD (Active)│  │ [ Tổng nạp ] [ Tổng chi   ]     │  │
│  │                      │  │ [ 500.000đ ] [ 65.000 đ   ]     │  │
│  │                      │  │ [ Đang xử lý] [Tổng giao dịch]  │  │
│  │                      │  │ [ 0 GD      ] [ 2 giao dịch  ]  │  │
│  │                      │  ├─────────────────────────────────┤  │
│  │                      │  │ Bộ lọc giao dịch                │  │
│  │                      │  │ Tìm kiếm       Loại GD    Trạng thái│  │
│  │                      │  │ [ Mã GD/Mô tả] [Tất cả]   [Tất cả]│  │
│  │                      │  │ Thời gian                       │  │
│  │                      │  │ [dd/mm - dd/mm] [Làm mới] [Tìm] │  │
│  │                      │  ├─────────────────────────────────┤  │
│  │                      │  │ Danh sách giao dịch             │  │
│  │                      │  │ Hiển thị 1-2/2 giao dịch.       │  │
│  │                      │  │ STT | Mã GD | Loại | Mô tả | Tiền | Trạng thái │  │
│  │                      │  │ 1 | MMO-1 | Thanh toán | ... | -65k | Thành công │  │
│  │                      │  │ 2 | SP-99 | Nạp tiền   | ... | +500k| Thành công │  │
│  │                      │  │                                 │  │
│  │                      │  │  << <  [1]  > >>     [Hiển thị 5/trang]│  │
│  └──────────────────────┘  └─────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 3. CÁC THÀNH PHẦN GIAO DIỆN CHÍNH

### 3.1 Tiêu đề & Nút thao tác
* **Tiêu đề:** "Lịch sử giao dịch ví".
* **Nút bấm:** Nút `[ Về ví ]` (chuyển hướng `/wallet`) và `[ Nạp tiền ]` (chuyển hướng `/wallet/topup`) màu xanh dương nổi bật.

### 3.2 Khối Thống Kê Nhanh (Stats)
* Gọi lại API `GET /api/v1/wallet/stats` để lấy `totalTopup` (Tổng nạp), `totalSpent` (Tổng chi), và `pendingCount` (Đang xử lý).
* Riêng ô **Tổng giao dịch** sẽ được lấy từ thuộc tính `totalElements` của API trả về danh sách giao dịch.

### 3.3 Bộ Lọc Giao Dịch (Filters)
* **Tìm kiếm:** Input text (tìm theo Mã giao dịch hoặc Nội dung mô tả).
* **Loại giao dịch:** Dropdown (Tất cả, Nạp tiền, Rút tiền, Mua hàng, Hoàn tiền).
* **Trạng thái:** Dropdown (Tất cả, Thành công, Đang xử lý, Thất bại).
* **Khoảng thời gian:** Date Range Picker (Từ ngày - Đến ngày).
* Nút `[ Làm mới bộ lọc ]` (Clear filters) và `[ Tìm kiếm ]` (Submit filter).

### 3.4 Bảng Danh Sách Giao Dịch & Phân Trang
* Bảng gồm các cột: STT, Mã giao dịch, Loại, Mô tả, Số tiền, Trạng thái.
* **Số tiền:** Tô màu Xanh lá (`+`) cho dòng tiền vào, màu Đỏ (`-`) cho dòng tiền ra.
* **Trạng thái:** Sử dụng badge (Thành công - Xanh lá, Đang xử lý - Vàng/Cam, Thất bại - Đỏ).
* Có phân trang (Pagination) ở dưới cùng, hỗ trợ đổi số dòng hiển thị trên một trang (ví dụ: 5, 10, 20).

---

## 4. LUỒNG XỬ LÝ JS & AJAX

1. **Khi load trang:**
   * Gọi `GET /api/v1/wallet/stats` để đổ dữ liệu vào 3 ô thống kê đầu tiên.
   * Lấy các tham số filter trên URL (nếu có) để điền vào form bộ lọc.
   * Gọi `GET /api/v1/wallet/transactions?page=0&size=5` (hoặc kèm theo các tham số tìm kiếm).
2. **Khi nhận dữ liệu giao dịch:**
   * Lấy `data.totalElements` điền vào ô "Tổng giao dịch".
   * Render danh sách vào `<tbody>` của bảng.
   * Render các nút phân trang.
3. **Khi Submit Bộ lọc:**
   * Lấy dữ liệu từ các input filter.
   * Cập nhật URL trình duyệt (sử dụng `history.pushState` để có thể share link).
   * Gọi lại API danh sách giao dịch với tham số mới.
   * Reset `page=0`.
