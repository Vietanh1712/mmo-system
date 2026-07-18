# SPEC — Yêu Cầu Rút Tiền Về Ngân Hàng (Seller Withdrawal Panel)

> **Feature ID:** `feat-seller` | **Page:** `SellerWithdrawals`
> **Route:** `/seller/withdrawals` | **Template:** `templates/seller/withdrawals.html`
> **CSS Script:** `static/css/customer/style.css`
> **JS Script:** `static/js/seller-withdrawals.js`
> **Version:** 1.0 | **Status:** Draft
> **Backend ref:** `feat-staff/UC-14-staff-operations.md`

---

## 1. TỔNG QUAN TRANG

Trang Yêu cầu rút tiền cung cấp giao diện cho phép người bán (Seller) thực hiện chuyển tiền doanh thu tích lũy từ ví khả dụng (`available_balance`) về tài khoản ngân hàng thụ hưởng đã đăng ký liên kết. Để bảo đảm an ninh tài chính, mọi yêu cầu rút tiền đều yêu cầu xác thực bảo mật bằng mã OTP gửi về hòm thư điện tử của chủ cửa hàng.

**Cấu trúc trang:**
1. **Sidebar điều hướng Kênh Người Bán:** Dashboard, Quản lý kho hàng (Inventory), Lịch sử đơn hàng bán, Yêu cầu rút tiền (Withdrawals).
2. **Khối biểu mẫu rút tiền (Withdrawal Form):**
   * Hiển thị số dư khả dụng và thông tin tài khoản ngân hàng thụ hưởng (ở dạng chỉ đọc để bảo vệ).
   * Ô nhập số tiền muốn rút, hiển thị phí rút tiền hệ thống và số tiền thực nhận sau phí.
   * Khung xác thực OTP email: Nút "Gửi mã OTP" kèm bộ đếm ngược chống spam 60s và ô nhập 6 chữ số OTP.
3. **Bảng lịch sử yêu cầu rút tiền:** Danh sách các lệnh rút tiền quá khứ và hiện tại kèm trạng thái kiểm duyệt (`Pending`, `Approved`, `Rejected`).

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
│  │  Sidebar  │  │  RÚT TIỀN VỀ NGÂN HÀNG             │ │
│  │  - Dashbd │  │  Số dư khả dụng: 3.500.000 VNĐ     │ │
│  │  - Kho    │  │  Số tiền rút: [ 1.500.000      ]   │ │
│  │  - Orders │  │  Tài khoản: VCB | 001100... | NG A │ │
│  │  - Rúttiền│  │  Nhập OTP:    [ 456123 ] [Gửi OTP] │ │
│  └───────────┘  │  [ XÁC NHẬN YÊU CẦU RÚT TIỀN ]     │ │
│                 │  LỊCH SỬ RÚT TIỀN:                 │ │
│                 │  ┌──────────────────────────────┐  │ │
│                 │  │#W101 | 1.500K | VCB | PENDING  │  │ │
│                 │  └──────────────────────────────┘  │ │
│                 └────────────────────────────────────┘ │
└────────────────────────────────────────────────────────┘
```

---

## 4. CÁC THÀNH PHẦN GIAO DIỆN CHÍNH

### 4.1 Khối Thông Tin Tài Khoản Thụ Hưởng — `.swth-bank-box`
* Nền xám nhạt bo góc. Hiển thị thông tin Tên Ngân Hàng, Số Tài Khoản và Tên Chủ Tài Khoản đã được liên kết khi đăng ký mở shop.
* **Quy tắc bảo mật:** Khối này hoàn toàn khóa chỉnh sửa (`read-only`) để chống việc kẻ xấu chiếm quyền tài khoản và đổi bank thụ hưởng hòng rút ruột tiền ví.

### 4.2 Bộ Nhập Mã OTP Đếm Ngược — `.swth-otp-block`
* Nút "Gửi mã OTP" (`#btnSendOtp`): Nhấp vào sẽ gửi OTP đến email.
* Kích hoạt đếm ngược 60s trên nút, đổi màu xám mờ và hiển thị: "Gửi lại sau (59s)".

---

## 5. LUỒNG XỬ LÝ JS & AJAX

1. **Khởi tạo trang:**
   * Tải số dư khả dụng hiện tại từ API số dư ví.
   * Tải lịch sử các yêu cầu rút tiền trước đây bằng API:
     * **Endpoint:** `GET /api/v1/seller/withdrawals/history`
     * **Headers:** `Authorization: Bearer <token>`
   * Kết xuất lịch sử vào bảng.
2. **Gửi mã OTP về email:**
   * Khi người dùng nhấp "Gửi mã OTP":
     * Gửi request `POST /api/v1/seller/withdrawals/send-otp` qua AJAX.
     * **Thành công (HTTP 200):** Bắt đầu đếm ngược 60s trên nút gửi OTP, hiển thị Toast thông báo mã đã được gửi về hòm thư điện tử.
3. **Submit yêu cầu rút tiền:**
   * Validate local: Số tiền rút tối thiểu `50,000` VNĐ và không vượt quá số dư khả dụng hiện tại. Ô nhập OTP phải đủ 6 ký tự số.
   * Gửi API:
     * **Endpoint:** `POST /api/v1/seller/withdrawals/submit`
     * **Headers:** `Content-Type: application/json`, `Authorization: Bearer <token>`
     * **Payload:** `{ "amount": 1500000, "otp": "456123" }`
   * **Thành công (HTTP 200):**
     * Trừ ngay lập tức số dư khả dụng trên UI và chuyển sang số dư tạm giữ hold_balance.
     * Thêm dòng mới vào bảng lịch sử yêu cầu rút tiền với trạng thái `Pending`.
     * Reset ô nhập số tiền và ô nhập OTP, kết thúc đếm ngược.
     * Hiển thị Toast thông báo tạo yêu cầu rút tiền thành công, đang chờ duyệt từ nhân viên Staff.

---

## 6. RESPONSIVE

* Bố cục thích nghi tốt trên thiết bị di động. Bảng lịch sử yêu cầu rút tiền thu gọn bớt các trường (Ngân hàng thụ hưởng, mã GD) khi bề rộng màn hình nhỏ hơn 768px.

---

## 7. ACCESSIBILITY

- Input số tiền có `aria-describedby` hướng dẫn giới hạn rút tối thiểu và phí rút tiền.
- Bảng lịch sử có cấu trúc HTML chuẩn hỗ trợ accessibility tốt.

---

## 8. OUT OF SCOPE

- ❌ Thay đổi trực tiếp thông tin ngân hàng tại trang này (Việc thay đổi bank phải thực hiện qua quy trình cập nhật thông tin shop riêng biệt).