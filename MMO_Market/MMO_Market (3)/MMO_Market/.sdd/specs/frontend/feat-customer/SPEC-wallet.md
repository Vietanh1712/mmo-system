# SPEC — Bảng Điều Khiển Ví (Wallet Dashboard)

> **Feature ID:** `feat-wallet` | **Page:** `Wallet`
> **Route:** `/wallet` | **Template:** `templates/account/wallet.html`
> **CSS Script:** `static/css/customer/style.css`
> **JS Script:** `static/js/customer/account-wallet.js`

---

## 1. TỔNG QUAN TRANG

Trang Ví cá nhân cung cấp cái nhìn tổng quan về tài sản của người dùng. Khác với phiên bản cũ, trang Dashboard Ví mới tập trung vào trải nghiệm người dùng với các khối thống kê (Thao tác nhanh) và bảng liệt kê chi tiết dòng tiền (Nạp, Chi, Rút).
Trang sử dụng 2 API chính: `/stats` và `/transactions` từ `WalletController`.

**Cấu trúc trang:**
1. **Sidebar điều hướng:** Nằm bên trái.
2. **Khu vực Nội dung (Main):** Gồm Thẻ Số Dư, Thống Kê (Thao tác nhanh), Khuyến nghị KYC, Lịch sử giao dịch và Hướng dẫn nạp tiền.

---

## 2. LAYOUT TỔNG THỂ & MOCKUP

```text
┌─────────────────────────────────────────────────────────────────┐
│ [Logo] MMO Market                         [Search]      [User]  │
├─────────────────────────────────────────────────────────────────┤
│  ┌──────────────────────┐  ┌─────────────────────────────────┐  │
│  │ (User Card)          │  │ Ví & giao dịch                  │  │
│  │                      │  │ Ví của tôi                      │  │
│  │ (Menu Sidebar)       │  │ Theo dõi số dư, giao dịch...    │  │
│  │ - Ví của tôi (Active)│  │                      [Số dư]    │  │
│  │ - Nạp tiền           │  │                      [934.000 đ]│  │
│  │ - Lịch sử giao dịch  │  ├─────────────────────────────────┤  │
│  │                      │  │ Thao tác nhanh                  │  │
│  │                      │  │ [ Tổng đã nạp ] [ Tổng đã chi ] │  │
│  │                      │  │ [ 500.000 đ   ] [ 65.000 đ    ] │  │
│  │                      │  │ [ Đang xử lý  ] [ Đang giữ..  ] │  │
│  │                      │  │ [ 0 giao dịch ] [ 0 đ         ] │  │
│  │                      │  ├─────────────────────────────────┤  │
│  │                      │  │ Xác minh TK để dùng ví an toàn  │  │
│  │                      │  │ (Cảnh báo KYC)      [Xem KYC]   │  │
│  │                      │  ├─────────────────────────────────┤  │
│  │                      │  │ Giao dịch gần đây    [Xem tất cả] │  │
│  │                      │  │ Mã GD | Loại | Số tiền | T.Thái │  │
│  │                      │  │ MMO-1 | Mua  | -65.000đ| OK     │  │
│  │                      │  │ SP-99 | Nạp  | 500.000đ| OK     │  │
│  │                      │  ├─────────────────────────────────┤  │
│  │                      │  │ Quy trình nạp tiền tự động      │  │
│  │                      │  │ 1. Tạo yêu cầu...               │  │
│  └──────────────────────┘  └─────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 3. CÁC THÀNH PHẦN GIAO DIỆN CHÍNH

### 3.1 Thẻ Số Dư & Header
* **Header:** Tiêu đề "Ví của tôi" và mô tả ngắn.
* **Số dư khả dụng:** Nằm bên phải, hiển thị nổi bật số dư VNĐ lấy từ thông tin Profile của User.

### 3.2 Khối Thao tác nhanh (Wallet Stats)
Dữ liệu được cấp bởi API `GET /api/v1/wallet/stats`
* **Tổng đã nạp (`totalTopup`):** Tổng tiền user đã nạp vào hệ thống.
* **Tổng đã chi (`totalSpent`):** Tổng tiền user đã chi tiêu/rút.
* **Đang xử lý (`pendingCount`):** Số lượng giao dịch đang chờ xử lý.
* **Đang giữ escrow (`escrowAmount`):** Số tiền đang bị khóa tạm thời.

### 3.3 Khối Xác Minh Tài Khoản (KYC Warning)
* **Logic hiển thị:** Nếu tài khoản có trạng thái KYC chưa hoàn tất (`APPROVED`), khối này sẽ hiển thị cảnh báo "Xác minh tài khoản để dùng ví an toàn hơn".
* **Nút bấm:** `[ Xem KYC ]` điều hướng người dùng tới trang `/account/kyc`.

### 3.4 Khối Giao Dịch Gần Đây (Recent Transactions)
Dữ liệu được cấp bởi API `GET /api/v1/wallet/transactions?page=0&size=5`
* Liệt kê tối đa 5 giao dịch gần nhất.
* **Cột hiển thị:** Mã giao dịch, Loại giao dịch (Nạp tiền, Thanh toán, Rút tiền...), Số tiền (Xanh lá nếu cộng, Đỏ nếu trừ), Trạng thái, Thời gian.
* Có nút `[ Xem tất cả ]` điều hướng sang trang chuyên biệt `/wallet/history`.

### 3.5 Khối Hướng Dẫn
* Hiển thị văn bản tĩnh giới thiệu "Quy trình nạp tiền tự động".

---

## 4. LUỒNG XỬ LÝ JS & AJAX

1. **Khởi tạo dữ liệu khi Load trang:**
   * Script gọi API `GET /api/v1/wallet/stats`.
   * Gán kết quả vào 4 box của Khối Thao tác nhanh.
   * Nếu call API lỗi, hiển thị "0 đ" mặc định.
2. **Khởi tạo bảng Lịch sử (Recent Transactions):**
   * Script gọi API `GET /api/v1/wallet/transactions?page=0&size=5`.
   * Xóa dòng text "Dữ liệu hiện là mock frontend..." và render dữ liệu thực tế vào thẻ `<tbody>` của bảng.
   * Nếu danh sách rỗng, hiển thị dòng chữ "Chưa có giao dịch nào".