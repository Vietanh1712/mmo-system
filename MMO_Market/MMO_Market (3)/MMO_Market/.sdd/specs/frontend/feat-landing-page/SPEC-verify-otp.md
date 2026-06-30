# SPEC — Xác Thực Kích Hoạt OTP (OTP Verification)

> **Feature ID:** `feat-auth` | **Page:** `VerifyOtp`
> **Route:** `/verify-otp` | **Template:** `templates/auth/verify-otp.html`
> **CSS Script:** `static/css/customer/authen.css`
> **JS Script:** `static/js/auth.js`
> **Version:** 1.0 | **Status:** Draft
> **Backend ref:** `feat-auth/UC-01-authentication.md`

---

## 1. TỔNG QUAN TRANG

Trang xác minh OTP tiếp nhận mã kích hoạt 6 chữ số được gửi qua email cho người dùng để chuyển trạng thái tài khoản từ `enabled = 0` sang `enabled = 1`.

**Cấu trúc trang:**
1. **Logo & Header:** Tiêu đề dẫn về trang chủ.
2. **Auth Card:** Hướng dẫn nhập OTP, ô nhập mã OTP (gồm 6 ô nhập chữ số riêng biệt hoặc 1 ô số lớn cách quãng) và nút Xác thực.
3. **Bộ đếm thời gian:** Liên kết gửi lại OTP hỗ trợ đếm ngược 60 giây để chống spam.

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

/* Sizing & Borders */
--ds-radius-md:       8px;
--ds-radius-lg:       12px;
--ds-radius-full:     9999px;
--ds-shadow:          0 10px 15px -3px rgba(0,0,0,0.1);
```

---

## 3. LAYOUT TỔNG THỂ & MOCKUP

```
┌────────────────────────────────────────────────────────┐
│                        [Logo]                          │
│         ┌────────────────────────────────────┐         │
│         │        Xác Thực Tài Khoản          │         │
│         ├────────────────────────────────────┤         │
│         │ Nhập mã OTP đã gửi đến email của bạn│         │
│         │                                    │         │
│         │   [ 1 ] [ 2 ] [ 3 ] [ 4 ] [ 5 ] [ ]│ <-- OTP Input Groups
│         │                                    │         │
│         │             [ XÁC NHẬN ]           │         │
│         │                                    │         │
│         │     Gửi lại mã OTP (Đếm ngược: 45s)│ <-- Resend timer
│         └────────────────────────────────────┘         │
└────────────────────────────────────────────────────────┘
```

---

## 4. CÁC THÀNH PHẦN GIAO DIỆN CHÍNH

### 4.1 Nhóm Ô Nhập Mã OTP — `.otp-input-group`
* Gồm 6 ô input đơn lẻ, mỗi ô chỉ chứa tối đa 1 chữ số (`maxlength="1"`, `pattern="[0-9]*"`).
* **CSS:** Mỗi ô rộng `48px`, cao `54px`, căn giữa chữ số, font-size `24px` đậm. Border-radius `var(--ds-radius-md)`.
* **JS hành vi tự động chuyển đổi Focus:**
  * Khi người dùng nhập 1 số, tiêu điểm (Focus) tự động nhảy sang ô kế tiếp bên phải.
  * Khi nhấn Backspace, tiêu điểm tự động quay lại ô bên trái trước đó.

### 4.2 Bộ Đếm Ngược Gửi Lại Mã — `.resend-timer-wrapper`
* Hiển thị dòng chữ: "Không nhận được mã? Gửi lại mã sau {ss}s" (ss giảm dần từ 60 về 0).
* Trong thời gian đếm ngược, liên kết "Gửi lại mã" bị vô hiệu hóa (`pointer-events: none`, màu xám mờ).
* Khi đếm ngược về 0, hiển thị liên kết "Gửi lại mã" ở trạng thái hoạt động bình thường, màu xanh thương hiệu.

---

## 5. LUỒNG XỬ LÝ JS & AJAX

1. **Khởi tạo:**
   * Đọc Email cần xác thực từ `sessionStorage.getItem('registerEmail')`. Nếu không tồn tại Email, chuyển hướng người dùng quay lại trang đăng ký `/register`.
   * Bắt đầu chạy bộ đếm ngược 60 giây cho nút gửi lại OTP.
2. **Gửi Request Xác Minh OTP:**
   * Người dùng nhập đủ 6 chữ số và nhấn "Xác nhận":
     * **Endpoint:** `POST /api/auth/verify-otp`
     * **Content-Type:** `application/json`
     * **Payload:** `{ "email": "user@example.com", "otpCode": "123456" }`
   * **Xử lý phản hồi thành công (HTTP 200):**
     * Hiển thị thông báo thành công.
     * Xóa `registerEmail` trong `sessionStorage`.
     * Tự động điều hướng người dùng sang trang `/login` sau 2 giây.
   * **Xử lý phản hồi thất bại (HTTP 400):**
     * Hiển thị thông báo "Mã xác minh không chính xác hoặc đã hết hạn".
     * Tô viền đỏ các ô input OTP bằng class `.has-error`.
3. **Gửi lại mã OTP:**
   * Khi người dùng click vào "Gửi lại mã" sau khi hết thời gian đếm ngược:
     * Gửi request `POST /api/auth/forgot-password` (hoặc endpoint tương đương gửi lại mã kích hoạt).
     * Khởi động lại bộ đếm ngược 60 giây.

---

## 6. RESPONSIVE

* Cả trên di động lẫn máy tính, khối ô nhập 6 chữ số được co giãn nhẹ để không bị tràn màn hình (độ rộng tối thiểu của viewport chứa 6 ô OTP là `320px`).

---

## 7. ACCESSIBILITY

- Sử dụng thuộc tính `autocomplete="one-time-code"` cho các ô nhập OTP để hỗ trợ tự động điền trên trình duyệt di động.
- Gắn thẻ `aria-live="polite"` cho bộ đếm thời gian gửi lại mã để thông báo số giây còn lại.

---

## 8. OUT OF SCOPE

- ❌ Xác minh tự động qua giọng nói (Voice OTP).
- ❌ Hỗ trợ đăng nhập nhanh bằng đường dẫn ma thuật (Magic Link).