# SPEC — Lịch Sử Đơn Hàng Đã Mua (Customer Order History)

> **Feature ID:** `feat-order` | **Page:** `OrderHistory`
> **Route:** `/account/orders` | **Template:** `templates/account/orders.html`
> **CSS Script:** `static/css/customer/style.css`
> **JS Script:** `static/js/customer/account-orders.js`
> **Backend ref:** `feat-order/UC-20-order-history.md`

---

## 1. TỔNG QUAN TRANG

Trang quản lý đơn hàng cho phép người mua (Customer) xem lại toàn bộ các sản phẩm số đã mua, theo dõi trạng thái thanh toán và tiến trình xử lý (Escrow).
Khác với thiết kế cũ dạng dạng lưới thẻ (Cards), thiết kế mới sử dụng **Bảng (Table)** chuyên nghiệp kèm **Bộ lọc đa tiêu chí** và các **Block thống kê**.

**Cấu trúc trang:**
1. **Sidebar điều hướng:** Menu quản lý tài khoản bên trái (Đơn hàng của tôi đang active).
2. **Khu vực Nội dung chính:**
   - Tiêu đề & Nút "Tiếp tục mua hàng".
   - Bốn khối thống kê số liệu (Tổng đơn, Hoàn tất, Đang xử lý, Tranh chấp).
   - Bộ lọc đơn hàng đa tiêu chí.
   - Bảng danh sách đơn hàng chi tiết kèm phân trang.

---

## 2. LAYOUT TỔNG THỂ & MOCKUP

```text
┌─────────────────────────────────────────────────────────────────┐
│ [Logo] MMO Market                         [Search]      [User]  │
├─────────────────────────────────────────────────────────────────┤
│  ┌──────────────────────┐  ┌─────────────────────────────────┐  │
│  │ (User Card)          │  │ Mua hàng                        │  │
│  │                      │  │ Đơn hàng của tôi                │  │
│  │ (Menu Sidebar)       │  │ Theo dõi các sản phẩm số...     │  │
│  │ - Ví của tôi         │  │                 [Tiếp tục mua...] │  │
│  │ - Nạp tiền           │  ├─────────────────────────────────┤  │
│  │ - Đơn hàng (Active)  │  │ [ Tổng đơn ] [ Hoàn tất   ]     │  │
│  │                      │  │ [ 2 đơn    ] [ 1 đơn      ]     │  │
│  │                      │  │ [ Đang xử lý] [ Tranh chấp  ]   │  │
│  │                      │  │ [ 0 đơn     ] [ 0 đơn       ]   │  │
│  │                      │  ├─────────────────────────────────┤  │
│  │                      │  │ Bộ lọc đơn hàng                 │  │
│  │                      │  │ Tìm kiếm       Trạng thái  T.Toán │  │
│  │                      │  │ [ Mã đơn/Tên ] [Tất cả]   [Tất cả]│  │
│  │                      │  │ Thời gian                       │  │
│  │                      │  │ [dd/mm - dd/mm] [Làm mới] [Tìm] │  │
│  │                      │  ├─────────────────────────────────┤  │
│  │                      │  │ Danh sách đơn hàng              │  │
│  │                      │  │ Hiển thị 1-2/2 đơn hàng.        │  │
│  │                      │  │ Mã đơn| Sản phẩm | Tiền | T.Thái | T.Toán | Ngày | Thao tác│  │
│  │                      │  │ ORD-2 | Netflix..| 100k | Xong   | Đã trả | 9/7  | [Eye]   │  │
│  │                      │  │ ORD-1 | Netflix..| 65k  | Tạm giữ| Đã trả | 25/6 | [Eye]   │  │
│  │                      │  │                                 │  │
│  │                      │  │  << <  [1]  > >>                │  │
│  └──────────────────────┘  └─────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 3. CÁC THÀNH PHẦN GIAO DIỆN CHÍNH

### 3.1 Tiêu đề & Nút thao tác
* **Tiêu đề:** "Đơn hàng của tôi".
* **Nút bấm:** Nút `[ Tiếp tục mua hàng ]` chuyển hướng người dùng ra trang chủ `/`.

### 3.2 Khối Thống Kê Nhanh (Stats)
* Do API `/api/transactions/me` trả về toàn bộ mảng dữ liệu, JS ở frontend sẽ dùng hàm `.reduce()` hoặc `.filter().length` để đếm tự động:
  * **Tổng đơn:** `.length`
  * **Hoàn tất:** Đếm các đơn có `status == 'COMPLETED'`
  * **Đang xử lý:** Đếm các đơn có `status == 'PENDING'` hoặc `ESCROW` (Tạm giữ)
  * **Tranh chấp:** Đếm các đơn có `status == 'COMPLAINT'`

### 3.3 Bộ Lọc Đơn Hàng (Filters)
* Quá trình filter được thực hiện cục bộ (Local Filter) trên biến mảng Javascript đang lưu kết quả từ API.
* **Tìm kiếm:** Theo Mã đơn hàng, Tên sản phẩm, hoặc Tên người bán.
* **Trạng thái đơn:** (Tất cả, Hoàn tất, Tạm giữ, Tranh chấp, Hủy).
* **Thanh toán:** (Tất cả, Đã thanh toán, Chờ thanh toán, Hoàn tiền).
* **Khoảng thời gian:** So sánh chuỗi ngày tháng.

### 3.4 Bảng Danh Sách Đơn Hàng & Phân Trang
* **Các cột:**
  - `Mã đơn`: Tiền tố `MMO-ORD-` + ID giao dịch.
  - `Sản phẩm`: Tên sản phẩm + Phân loại (Variant) + Tên Shop (Người bán).
  - `Số tiền`: Giá trị đơn hàng (VND).
  - `Trạng thái`: Dùng badge (Hoàn tất - Xanh lá, Tạm giữ - Xanh biển nhạt, Tranh chấp - Cam/Đỏ).
  - `Thanh toán`: Badge `Đã thanh toán` (Xanh lá).
  - `Ngày mua`: `HH:mm:ss dd/MM/yyyy`.
  - `Thao tác`: Icon con mắt 👁️ màu xanh lam có vòng tròn bao quanh (`.btn-view-order`). Khi click sẽ điều hướng sang trang chi tiết đơn hàng `/account/orders/{id}` hoặc bật Modal tùy thiết kế.

---

## 4. LUỒNG XỬ LÝ JS & AJAX

1. **Khi load trang:**
   * Script gọi `GET /api/transactions/me`.
   * Backend trả về 1 mảng JSON chứa tất cả đơn hàng. Script gán vào biến toàn cục `window.allOrders = data`.
2. **Cập nhật UI:**
   * Tính toán 4 khối thống kê từ `window.allOrders`.
   * Gán `window.filteredOrders = window.allOrders`.
   * Gọi hàm `renderTable(pageNumber)` để cắt mảng `filteredOrders` (ví dụ cắt 10 dòng đầu) và hiển thị ra `<tbody>`.
3. **Khi Submit Bộ lọc:**
   * Xóa bỏ dữ liệu cũ trên bảng, chạy hàm `.filter()` trên `window.allOrders` để tạo ra mảng `filteredOrders` mới.
   * Chạy lại hàm `renderTable(1)`. Trang tự động chuyển về trang 1.