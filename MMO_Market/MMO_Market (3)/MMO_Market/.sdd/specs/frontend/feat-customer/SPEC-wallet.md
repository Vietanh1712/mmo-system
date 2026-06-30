# SPEC — Ví Cá Nhân & Nạp Tiền (Customer Wallet & Deposit)

> **Feature ID:** `feat-wallet` | **Page:** `Wallet`
> **Route:** `/wallet` | **Template:** `templates/account/wallet.html`
> **CSS Script:** `static/css/customer/style.css`
> **JS Script:** `static/js/account-wallet.js`
> **Version:** 1.0 | **Status:** Draft
> **Backend ref:** `feat-wallet/UC-07-wallet-topup.md`

---

## 1. TỔNG QUAN TRANG

Trang Ví cá nhân cho phép người mua theo dõi biến động số dư và thực hiện nạp tiền tự động qua VietQR liên kết với SePay Webhook.

Số dư ví được chia làm hai cột rõ rệt:
* **Số dư khả dụng (`available_balance`):** Sử dụng để mua sắm sản phẩm hoặc tạo yêu cầu rút tiền mặt.
* **Số dư tạm giữ (`hold_balance`):** Số dư đóng băng phục vụ quy trình ký quỹ Escrow 72h đơn hàng hoặc lệnh rút đang chờ duyệt.

**Cấu trúc trang:**
1. **Sidebar điều hướng:** Menu quản lý tài khoản bên trái.
2. **Khối hiển thị số dư (Balance Cards):** Hai thẻ hiển thị hai số dư dạng số nguyên VNĐ.
3. **Bảng lịch sử giao dịch ví:** Danh sách các biến động số dư (Nạp tiền, Rút tiền, Mua hàng, Hoàn tiền).
4. **Modal Nạp tiền tự động:** Hộp thoại sinh mã QR VietQR động và lắng nghe trạng thái thanh toán chuyển khoản thời gian thực.

---

## 2. DESIGN TOKENS ÁP DỤNG

```css
/* Color Palette */
--ds-primary:         #2563eb;
--ds-primary-hover:   #1d4ed8;
--ds-bg:              #f8fafc;
--ds-card:            #ffffff;
--ds-border:          #cbd5e1;
--ds-error:           #ef4444;
--ds-success:         #10b981;
--ds-gradient-wallet: linear-gradient(135deg, #2563eb 0%, #06b6d4 100%);

/* Layout & Shadow */
--ds-radius-md:       8px;
--ds-radius-lg:       12px;
--ds-radius-xl:       16px;
--ds-shadow:          0 4px 6px -1px rgba(0,0,0,0.1);
```

---

## 3. LAYOUT TỔNG THỂ & MOCKUP

```
┌────────────────────────────────────────────────────────┐
│ [Logo] MMO Market                              [User]  │
├────────────────────────────────────────────────────────┤
│  ┌───────────┐  ┌────────────────────────────────────┐ │
│  │  Sidebar  │  │  VÍ CỦA TÔI / QUẢN LÝ SỐ DƯ        │ │
│  │  - Hồ sơ  │  │  ┌──────────────┐ ┌──────────────┐  │ │
│  │  - Ví tiền│  │  │ Số dư khả    │ │ Số dư tạm giữ│  │ │
│  │  - KYC    │  │  │ 1.200.000đ   │ │ 300.000đ     │  │ │
│  │  - Orders │  │  │ [➕ Nạp Tiền]│ │ (Đang giữ)   │  │ │
│  └───────────┘  │  └──────────────┘ └──────────────┘  │ │
│                 │  LỊCH SỬ GIAO DỊCH VÍ              │ │
│                 │  ┌──────────────────────────────┐  │ │
│                 │  │#GD01 | Nạp tiền | +500K | OK │  │ │
│                 │  └──────────────────────────────┘  │ │
│                 └────────────────────────────────────┘ │
└────────────────────────────────────────────────────────┘
```

---

## 4. CÁC THÀNH PHẦN GIAO DIỆN CHÍNH

### 4.1 Thẻ Số Dư Khả Dụng — `.cwlt-balance-card`
* Nền sử dụng gradient bắt mắt `var(--ds-gradient-wallet)`. Chữ số dư to đậm màu trắng.
* Tích hợp nút hành động nạp tiền `[➕ Nạp Tiền]` dạng nút trong suốt có viền trắng.

### 4.2 Modal Nạp Tiền — `.cwlt-topup-modal`
* Khi click "Nạp tiền", modal hiển thị giữa màn hình với nền mờ phía sau (Overlay).
* **Bước 1 (Nhập tiền):** Ô nhập số tiền cần nạp, nút xác nhận.
* **Bước 2 (Hiển thị mã thanh toán):**
  * Ảnh mã QR VietQR động (chứa thông tin SePay chuyển tiền).
  * Ô hiển thị Nội dung chuyển khoản (bắt buộc) kèm một nút icon **Sao chép (Copy to clipboard)** kế bên.
  * Hiển thị trạng thái xoay vòng `.cwlt-spinner` với dòng chữ: "Chờ thanh toán..."

---

## 5. LUỒNG XỬ LÝ JS & AJAX

1. **Khởi tạo dữ liệu ví:**
   * Gọi API `GET /api/v1/wallet/balance` lấy thông tin và hiển thị số dư ví của tôi lên các ô text tương ứng.
   * Gọi API `GET /api/v1/wallet/transactions` tải lịch sử biến động số dư ví đưa vào bảng.
2. **Khởi tạo nạp tiền:**
   * Người dùng nhập số tiền nạp (validate số tiền tối thiểu `10,000` VNĐ) và nhấn "Tiếp tục".
   * Gửi AJAX:
     * **Endpoint:** `POST /api/v1/wallet/topup/init`
     * **Payload:** `{ "amount": 100000 }`
   * **Thành công (HTTP 200):** Nhận JSON trả về `{ "transactionCode": "MMO12345", "qrUrl": "https://..." }`.
     * Cập nhật URL ảnh mã QR và điền mã chuyển khoản vào ô text.
     * **Bắt đầu Polling kiểm tra:** Kích hoạt một bộ hẹn giờ `setInterval` cứ mỗi 5 giây gửi một cuộc gọi API kiểm tra: `GET /api/v1/wallet/topup/status?code=MMO12345`.
3. **Xử lý thành công nạp tiền:**
   * Khi kết quả API polling trả về trạng thái giao dịch nạp tiền thành công (`status = 'Success'`):
     * Dừng bộ hẹn giờ `setInterval` (Clear Interval).
     * Đóng modal nạp tiền.
     * Kích hoạt Toast thông báo "Đã cộng tiền thành công".
     * Tự động cập nhật số dư mới trên giao diện mà không cần tải lại toàn bộ trang.

---

## 6. RESPONSIVE

* **Viewport ≥ 768px:** Hai thẻ số dư xếp hàng ngang chia đều 2 cột. Bảng lịch sử hiển thị đầy đủ các trường thông tin.
* **Viewport < 768px:** Hai thẻ số dư xếp chồng dọc. Bảng lịch sử ẩn bớt các cột không quan trọng (Ngày tạo, Loại giao dịch phụ) để vừa khít chiều ngang màn hình điện thoại di động.

---

## 7. ACCESSIBILITY

- Modal nạp tiền sử dụng `role="dialog"` và `aria-modal="true"`.
- Nút sao chép nội dung chuyển khoản có nhãn `aria-label="Sao chép nội dung chuyển khoản"`.

---

## 8. OUT OF SCOPE

- ❌ Liên kết tài khoản thẻ tín dụng tự động nạp định kỳ.
- ❌ Hủy nạp tiền bằng tay (giao dịch hết hạn sau 15 phút sẽ tự động đóng).