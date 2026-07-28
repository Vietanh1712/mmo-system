# SPEC — Trang Đăng Ký (Register)

> **Feature ID:** `feat-auth` | **Page:** `Register`
> **Route:** `/register` | **Template:** `templates/auth/register.html`
> **CSS:** `static/css/customer/authen.css`, `static/css/components/common.css`
> **JS:** `static/js/customer/auth.js`
> **Backend ref:** `feat-auth/UC-01-authentication.md` (Luồng 3.1)

---

## 1. TỔNG QUAN TRANG

Trang đăng ký cho phép khách vãng lai tạo tài khoản mới với email, mật khẩu, họ tên và số điện thoại (tùy chọn). Sau khi submit thành công, hệ thống gửi OTP đến email và tự động chuyển hướng sang trang `/verify-otp`.

---

## 2. LAYOUT TỔNG THỂ & MOCKUP

```
┌────────────────────────────────┐
│  💎 MMO Market                 │
│                                │
│  Tạo tài khoản                 │
│  Sau khi tạo tài khoản, hệ    │
│  thống sẽ gửi mã OTP để xác   │
│  thực email của bạn.           │
│                                │
│  [alert-message]               │
│                                │
│  ┌──────────────────────────┐  │
│  │ Họ và tên                │  │
│  └──────────────────────────┘  │
│  <fullName-error>              │
│                                │
│  ┌──────────────────────────┐  │
│  │ 📧 Địa chỉ Email          │  │
│  └──────────────────────────┘  │
│  <email-error>                 │
│                                │
│  ┌──────────────────────────┐  │
│  │ 📞 Số điện thoại (Tùy chọn)│ │
│  └──────────────────────────┘  │
│  <phone-error>                 │
│                                │
│  ┌──────────────────────┐ 👁  │
│  │ 🔒 Mật khẩu           │    │
│  └──────────────────────┘     │
│  Tối thiểu 6 ký tự, 1 chữ hoa,│
│  1 ký tự đặc biệt.            │
│  <password-error>              │
│                                │
│  ┌──────────────────────┐ 👁  │
│  │ 🔒 Nhập lại mật khẩu  │    │
│  └──────────────────────┘     │
│  <confirmPassword-error>       │
│                                │
│  [ TẠO TÀI KHOẢN ]            │
│                                │
│  Đã có tài khoản?              │
│  Đăng nhập ngay                │
│                                │
│  ← Về trang chủ               │
└────────────────────────────────┘
```

---

## 3. CÁC THÀNH PHẦN GIAO DIỆN

### 3.1 Header Card
- Logo: `<div class="auth-logo"><span>💎</span> MMO Market</div>`
- Tiêu đề: `<h3>Tạo tài khoản</h3>`
- Subtitle: `<p class="auth-subtitle">Sau khi tạo tài khoản, hệ thống sẽ gửi mã OTP để xác thực email của bạn.</p>`

### 3.2 Alert Message
- ID: `#alert-message` | Class: `.message`

### 3.3 Form Đăng Ký (`#registerForm`)

| Element | ID/Name | Type | Bắt buộc | Ghi chú |
|:---|:---|:---|:---|:---|
| Input họ tên | `#fullName` / `name="fullName"` | `type="text"` | ✅ | |
| Error họ tên | `#fullName-error` | `<small class="field-error">` | | |
| Input email | `#email` / `name="email"` | `type="email"` | ✅ | |
| Error email | `#email-error` | `<small class="field-error">` | | |
| Input SĐT | `#phone` / `name="phone"` | `type="tel"` | ❌ | Placeholder: "Số điện thoại (Tùy chọn)" |
| Error SĐT | `#phone-error` | `<small class="field-error">` | | |
| Input mật khẩu | `#password` / `name="password"` | `type="password"` | ✅ | `autocomplete="new-password"` |
| Hint mật khẩu | `.auth-field-hint` | `<small>` | | "Tối thiểu 6 ký tự, gồm 1 chữ hoa và 1 ký tự đặc biệt." |
| Error mật khẩu | `#password-error` | `<small class="field-error">` | | |
| Nút toggle pass | `.toggle-password` | `type="button"` | | `data-toggle-password="password"` |
| Input xác nhận MK | `#confirmPassword` / `name="confirmPassword"` | `type="password"` | ✅ | `autocomplete="new-password"` |
| Error xác nhận MK | `#confirmPassword-error` | `<small class="field-error">` | | |
| Nút toggle confirm | `.toggle-password` | `type="button"` | | `data-toggle-password="confirmPassword"` |
| Nút đăng ký | `#btnRegister` | `type="submit"` | | Text: "Tạo tài khoản" |

### 3.4 Footer Links
- "Đã có tài khoản?" → `<a href="/login">Đăng nhập ngay</a>`
- `<div class="back-home"><a href="/">← Về trang chủ</a></div>`

---

## 4. LUỒNG XỬ LÝ JS (auth.js)

1. Lắng nghe `submit` trên `#registerForm`
2. Validate phía client (xem Section 5)
3. Gọi API:

```
POST /api/auth/register
Body: {
  email:    string,
  password: string,
  fullName: string,
  phone:    string | undefined
}
```

4. **Xử lý response:**
   - `response.success === true` (HTTP 201):
     - Lưu email vào `sessionStorage` hoặc `localStorage` để dùng ở trang `/verify-otp`
     - Redirect: `window.location.href = '/verify-otp'`
   - `response.success === false` (HTTP 400):
     - Hiển thị `response.message` trong `#alert-message`

---

## 5. VALIDATION PHÍA CLIENT

| Field | Điều kiện | Thông báo lỗi |
|:---|:---|:---|
| Họ tên | Không rỗng | "Vui lòng nhập họ và tên" |
| Email | Không rỗng, đúng định dạng | "Vui lòng nhập email hợp lệ" |
| Mật khẩu | Tối thiểu 6 ký tự | "Mật khẩu phải từ 6 ký tự trở lên" |
| Xác nhận MK | Khớp với `#password` | "Mật khẩu xác nhận không khớp" |
| SĐT | Tùy chọn, nếu nhập thì đúng định dạng | "Số điện thoại không hợp lệ" |

> **Lưu ý:** Hint hiển thị "Tối thiểu 6 ký tự, gồm 1 chữ hoa và 1 ký tự đặc biệt" nhưng backend chỉ validate tối thiểu 6 ký tự. Đây là yêu cầu client-only.

---

## 6. RESPONSIVE

- **≥ 768px:** Card `.login-wrap` căn giữa, chiều rộng cố định ~420px
- **< 768px:** Card chiếm toàn bộ chiều rộng với padding nhỏ

---

## 7. ACCESSIBILITY

- Nút toggle mật khẩu:
  - Pass: `aria-label="Hiển thị mật khẩu"`
  - Confirm: `aria-label="Hiển thị mật khẩu xác nhận"`
- `autocomplete="new-password"` cho cả 2 field mật khẩu

---

## 8. OUT OF SCOPE

- ❌ Đăng ký với vai trò Seller trực tiếp — phải đăng ký Customer trước, sau đó mở Shop
- ❌ Đăng ký bằng Google trên trang này (Google login chỉ ở trang `/login`)
