# SPEC — Guest OTP Email Activation Page
> **Route:** `/verify-otp` | **Template:** `templates/auth/verify-otp.html`
> **JS Script:** `static/js/auth.js`

---

## 1. MÔ TẢ TRANG
Trang nhập mã xác thực OTP gửi qua email nhằm kích hoạt chính thức tài khoản người dùng đăng ký (`isVerified = 1`).

---

## 2. MOCKUP GIAO DIỆN (ASCII)
```
┌────────────────────────────────────────────────────────┐
│  Xác thực OTP đăng ký tài khoản                        │
├────────────────────────────────────────────────────────┤
│  Mã OTP (6 chữ số): [ 123456 ]                         │
│                                                        │
│                   [ Kích Hoạt Tài Khoản ]              │
│                                                        │
│  [ Gửi lại mã OTP ] (Đếm ngược: 45s)                   │
└────────────────────────────────────────────────────────┘
```