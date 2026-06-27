# SPEC — Guest Register Page
> **Route:** `/register` | **Template:** `templates/auth/register.html`
> **JS Script:** `static/js/auth.js`

---

## 1. MÔ TẢ TRANG
Trang cho phép khách vãng lai đăng ký tài khoản mới trên sàn MMO Market bằng email cá nhân.

---

## 2. LUỒNG XỬ LÝ OTP XÁC MINH
1. Nhập thông tin đăng ký (email, mật khẩu).
2. Click "Đăng ký" -> Hệ thống gửi OTP xác minh về email và lưu tạm thông tin đăng ký lên DB.
3. Chuyển hướng người dùng sang trang `/verify-otp`.