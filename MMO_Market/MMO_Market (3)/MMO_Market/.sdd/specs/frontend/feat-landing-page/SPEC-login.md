# SPEC — Guest Login Page
> **Route:** `/login` | **Template:** `templates/auth/login.html`
> **JS Script:** `static/js/auth.js`

---

## 1. MÔ TẢ TRANG
Giao diện đăng nhập tài khoản hệ thống của khách hàng. Hỗ trợ đăng nhập email/password truyền thống và Google OAuth2.

---

## 2. MOCKUP GIAO DIỆN (ASCII)
```
┌────────────────────────────────────────────────────────┐
│  Đăng nhập MMO Market                                  │
├────────────────────────────────────────────────────────┤
│  Email:       [ customer@gmail.com ]                   │
│  Mật khẩu:    [ **********         ]                   │
│                                                        │
│                   [ Đăng Nhập ]                        │
│                                                        │
│                [ Đăng nhập với Google ]                │
└────────────────────────────────────────────────────────┘
```

---

## 3. LUỒNG XỬ LÝ AJAX TOKENS
1. Khi click "Đăng Nhập", validate định dạng đầu vào.
2. Gửi request `POST /api/auth/login`.
3. Nhận phản hồi chứa `accessToken` và `refreshToken`.
4. Ghi các token vào `sessionStorage` để phục vụ các cuộc gọi API tiếp theo:
   ```javascript
   sessionStorage.setItem('accessToken', data.accessToken);
   sessionStorage.setItem('refreshToken', data.refreshToken);
   ```
5. Chuyển hướng người dùng về trang `/` hoặc trang trước đó.