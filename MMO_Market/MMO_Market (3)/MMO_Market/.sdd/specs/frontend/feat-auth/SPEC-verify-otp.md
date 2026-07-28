# SPEC — Trang Xác Thực OTP (Verify OTP)

> **Feature ID:** `feat-auth` | **Page:** `Verify OTP`
> **Route:** `/verify-otp` | **Template:** `templates/auth/verify-otp.html`
> **CSS:** `static/css/customer/authen.css`, `static/css/components/common.css`
> **JS:** `static/js/customer/auth.js`
> **Backend ref:** `feat-auth/UC-01-authentication.md` (Luồng 3.1 — Bước 5–8, Luồng 3.5)

---

## 1. TỔNG QUAN TRANG

Trang xác thực OTP được dùng sau bước đăng ký thành công. Người dùng nhập mã 6 số nhận được qua email để kích hoạt tài khoản. Trang cũng có chức năng **gửi lại OTP** và **đổi email** (quay lại trang đăng ký).

Email của người dùng được đọc từ `localStorage`/`sessionStorage` (do trang Register lưu lại trước khi redirect).

---

## 2. LAYOUT TỔNG THỂ & MOCKUP

```
┌────────────────────────────────┐
│  💎 MMO Market                 │
│                                │
│  Xác thực tài khoản            │
│                                │
│  Chúng tôi đã gửi mã gồm 6   │
│  chữ số đến                   │
│  user@example.com              │  ← #displayEmail (lấy từ storage)
│                                │
│  [alert-message]               │
│                                │
│  ┌──────────────────────────┐  │
│  │ ••••••                   │  │  ← #otp, class="otp-input"
│  └──────────────────────────┘  │
│  <otp-error>                   │
│                                │
│  [ XÁC THỰC ]                  │
│                                │
│  Không nhận được mã?           │
│  Gửi lại  ← #resendOtp         │
│  hoặc Thay đổi email ← /register│
│                                │
│  ← Về trang chủ               │
└────────────────────────────────┘
```

---

## 3. CÁC THÀNH PHẦN GIAO DIỆN

### 3.1 Header Card
- Logo: `<div class="auth-logo"><span>💎</span> MMO Market</div>`
- Tiêu đề: `<h3>Xác thực tài khoản</h3>`
- Mô tả email: `<p class="verify-text">Chúng tôi đã gửi mã gồm 6 chữ số đến <br><span class="display-email" id="displayEmail">email của bạn</span></p>`

### 3.2 Alert Message
- ID: `#alert-message` | Class: `.message`

### 3.3 Form Xác Thực OTP (`#verifyOtpForm`)

| Element | ID/Name | Type | Ghi chú |
|:---|:---|:---|:---|
| Input OTP | `#otp` / `name="otp"` | `type="text"` | `class="otp-input"`, `maxlength="6"`, `autocomplete="off"`, `placeholder="••••••"` |
| Error OTP | `#otp-error` | `<small class="field-error">` | |
| Nút xác thực | `#btnVerify` | `type="submit"` | Text: "Xác thực" |

### 3.4 Khu vực Gửi Lại OTP (`.resend-text`)

| Element | ID | Hành vi |
|:---|:---|:---|
| Link gửi lại | `#resendOtp` | Gọi `POST /api/auth/resend-otp` |
| Link đổi email | `.change-email-link` | `href="/register"` |

**Inline style cho trạng thái disabled (đang đếm ngược):**
```css
.resend-text a.disabled {
    color: #94a3b8 !important;
    pointer-events: none;
    cursor: default;
}
```

---

## 4. LUỒNG XỬ LÝ JS (auth.js)

### 4.1 Khởi tạo trang

```javascript
// Đọc email từ storage (đã lưu bởi trang register)
const email = localStorage.getItem('registerEmail') || sessionStorage.getItem('pendingEmail');
document.getElementById('displayEmail').textContent = email || 'email của bạn';
```

### 4.2 Submit Xác Thực OTP

1. Lắng nghe `submit` trên `#verifyOtpForm`
2. Validate: OTP không rỗng, đúng 6 ký tự số
3. Gọi API:

```
POST /api/auth/verify-otp
Body: { email: <lấy từ storage>, otp: <giá trị #otp> }
```

4. **Xử lý response:**
   - `response.success === true` (HTTP 200):
     - Xóa email khỏi storage
     - Redirect: `window.location.href = '/login'`
   - `response.success === false` (HTTP 400):
     - Hiển thị `response.message` trong `#alert-message`

### 4.3 Gửi Lại OTP (`#resendOtp`)

1. Lắng nghe click `#resendOtp`
2. Gọi API:

```
POST /api/auth/resend-otp
Body: { email: <lấy từ storage> }
```

3. **Xử lý response:**
   - Thành công: Hiển thị toast/alert "Mã OTP mới đã được gửi"
   - Thất bại: Hiển thị lỗi trong `#alert-message`

---

## 5. VALIDATION PHÍA CLIENT

| Field | Điều kiện | Thông báo lỗi |
|:---|:---|:---|
| OTP | Không rỗng, 6 ký tự | "Vui lòng nhập mã OTP 6 số" |
| OTP | Chỉ chứa chữ số | "Mã OTP chỉ gồm chữ số" |

---

## 6. RESPONSIVE

- **≥ 768px:** Card căn giữa, chiều rộng ~420px
- **< 768px:** Card full width

---

## 7. ACCESSIBILITY

- Input OTP: `autocomplete="off"` để tránh trình duyệt tự điền sai

---

## 8. OUT OF SCOPE

- ❌ Cooldown đếm ngược 60s giữa các lần gửi lại OTP (chưa implement trong code)
- ❌ Input OTP kiểu 6 ô riêng biệt — hiện dùng 1 input duy nhất
