# SPEC — Yêu Cầu Rút Tiền Về Ngân Hàng (Seller Withdrawal Panel)

> **Feature ID:** `feat-seller` | **Page:** `SellerWithdrawals`
> **Route:** `/seller/withdrawals` | **Template:** `templates/seller/withdrawals.html`
> **CSS Script:** `static/css/customer/style.css`
> **JS Script:** `static/js/seller-withdrawals.js`
> **Version:** 1.0 | **Status:** Draft
> **Backend ref:** `feat-wallet/UC-09-withdrawal.md`

---

## 1. TỔNG QUAN TRANG

Trang Yêu cầu rút tiền cung cấp giao diện cho phép người bán (Seller) thực hiện chuyển tiền doanh thu tích lũy từ ví khả dụng (`available_balance`) về tài khoản ngân hàng thụ hưởng đã đăng ký liên kết. Để bảo đảm an ninh tài chính, mọi yêu cầu rút tiền đều yêu cầu xác thực bảo mật bằng mã OTP gửi về hòm thư điện tử của chủ cửa hàng.

**Cấu trúc trang:**
1. **Sidebar điều hướng Kênh Người Bán:** Dashboard, Quản lý kho hàng (Inventory), Lịch sử đơn hàng bán, Yêu cầu rút tiền (Withdrawals).
2. **Khối biểu mẫu rút tiền (Withdrawal Form):**
   * Hiển thị số dư khả dụng và thông tin tài khoản ngân hàng thụ hưởng (ở dạng chỉ đọc để bảo vệ).
   * Ô nhập số tiền muốn rút, hiển thị phí rút tiền hệ thống và số tiền thực nhận sau phí.
   * Nút xác nhận gửi yêu cầu rút tiền.
3. **Bảng lịch sử yêu cầu rút tiền:** Danh sách các lệnh rút tiền quá khứ và hiện tại kèm trạng thái kiểm duyệt đã được Việt hóa (`Chờ xử lý`, `Đang xử lý`, `Hoàn tất`, `Bị từ chối`).
4. **Chi tiết rút tiền:** Bấm biểu tượng con mắt để xem chi tiết, hiển thị thông tin ngân hàng và hóa đơn/biên lai chuyển khoản (có thể là ảnh chụp hoặc file PDF tải xuống) mà Nhân viên Kế toán tải lên khi giao dịch ở trạng thái `Hoàn tất`.

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
│                 │  ┌────────────────────────────────────┐  │
│                 │  │#W101 | 1.500K | VCB | Chờ xử lý    │  │
│                 │  └────────────────────────────────────┘  │
│                 └────────────────────────────────────┘ │
└────────────────────────────────────────────────────────┘
```

---

## 4. CÁC THÀNH PHẦN GIAO DIỆN CHÍNH

### 4.1 Khối Thông Tin Tài Khoản Thụ Hưởng — `.swth-bank-box`
* Nền xám nhạt bo góc. Hiển thị thông tin Tên Ngân Hàng, Số Tài Khoản và Tên Chủ Tài Khoản đã được liên kết khi đăng ký mở shop.
* **Quy tắc bảo mật:** Khối này hoàn toàn khóa chỉnh sửa (`read-only`) để chống việc kẻ xấu chiếm quyền tài khoản và đổi bank thụ hưởng hòng rút ruột tiền ví.

### 4.2 Modal Xác Nhận Rút Tiền (Custom HTML Modal)
* Khung Modal popup hiển thị thay thế cho `window.confirm` mặc định của trình duyệt để tăng tính thẩm mỹ và độ chuyên nghiệp.
* Hiển thị rõ số tiền yêu cầu rút, phí rút (nếu có, từ backend config) và tổng số tiền sẽ trừ khỏi ví.
* Nút bấm xác nhận có trạng thái loading (spinner vô hiệu hóa nút) chống việc click liên tục sinh ra request rác (spam).

---

## 5. LUỒNG XỬ LÝ JS & AJAX

1. **Khởi tạo trang:**
   * Tải cấu hình rút tiền từ API (`/withdrawals/config`) để lấy hạn mức min/max và phần trăm phí rút tiền.
   * Tải số dư khả dụng hiện tại từ API số dư ví.
   * Tải lịch sử các yêu cầu rút tiền trước đây bằng API.
   * Kết xuất lịch sử vào bảng.
2. **Submit yêu cầu rút tiền (Hiển thị Modal):**
   * Validate local: Số tiền rút tối thiểu theo cấu hình (ví dụ `10,000` đ) và không vượt quá số dư khả dụng hiện tại.
   * Tính toán phí rút (nếu có) và hiển thị Custom HTML Modal Popup xác nhận tổng tiền.
   * Gửi API khi ấn xác nhận trên Modal:
     * **Endpoint:** `POST /api/v1/seller/withdrawals`
     * **Headers:** `Content-Type: application/json`, `Authorization: Bearer <token>`
     * **Payload:** `{ "amountVnd": 100000 }`
   * **Thành công (HTTP 200):**
     * Ẩn Modal Popup.
     * Hiển thị Toast thông báo tạo yêu cầu rút tiền thành công.
     * Trình duyệt tự động reload trang sau 1.5s để làm mới số dư và hiển thị dòng lịch sử lệnh rút vừa tạo với trạng thái `Chờ xử lý`.

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