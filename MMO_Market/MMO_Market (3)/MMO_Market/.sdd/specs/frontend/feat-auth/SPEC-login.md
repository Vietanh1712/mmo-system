# SPEC — Trang Đăng Nhập (Login)

> **Feature ID:** `feat-auth` | **Page:** `Login`
> **Route:** `/login` | **Template:** `templates/auth/login.html`
> **CSS:** `static/css/customer/authen.css`, `static/css/components/common.css`
> **JS:** `static/js/customer/auth.js`
> **Backend ref:** `feat-auth/UC-01-authentication.md` (Luồng 3.2, 3.3, 3.4)

---

## 1. TỔNG QUAN TRANG

Trang đăng nhập hỗ trợ **2 luồng xác thực**:
1. **Standard Login**: Email + Mật khẩu → cấp token ngay hoặc yêu cầu 2FA OTP
2. **Google OAuth2**: Đăng nhập bằng tài khoản Google qua Google Identity Services

Trang có **2 trạng thái hiển thị**:
- **Trạng thái 1 (Mặc định):** Form email + password
- **Trạng thái 2 (2FA):** Ẩn form chính, hiện thêm ô nhập OTP 6 số

---

## 2. LAYOUT TỔNG THỂ & MOCKUP

### Trạng thái 1 — Form Đăng Nhập Thường

```
┌────────────────────────────────┐
│  💎 MMO Market                 │
│                                │
│  Đăng nhập                     │
│                                │
│  [alert-message]               │
│                                │
│  ┌──────────────────────────┐  │
│  │ 📧 Địa chỉ Email          │  │
│  └──────────────────────────┘  │
│  <email-error>                 │
│                                │
│  ┌──────────────────────┐ 👁  │
│  │ 🔒 Mật khẩu           │    │
│  └──────────────────────┘     │
│  <password-error>              │
│                                │
│         Quên mật khẩu?         │
│                                │
│  [ ĐĂNG NHẬP ]                 │
│                                │
│  ─────────── HOẶC ───────────  │
│                                │
│  [ 🌐 Đăng nhập với Google ]   │
│                                │
│  Chưa có tài khoản?            │
│  Đăng ký tại đây               │
│                                │
│  ← Về trang chủ               │
└────────────────────────────────┘
```

### Trạng thái 2 — Form 2FA (hiện thêm OTP group)

```
┌────────────────────────────────┐
│  💎 MMO Market                 │
│                                │
│  Đăng nhập                     │
│                                │
│  ┌──────────────────────────┐  │
│  │ 📧 Địa chỉ Email          │  │
│  └──────────────────────────┘  │
│  ┌──────────────────────┐ 👁  │
│  │ 🔒 Mật khẩu           │    │
│  └──────────────────────┘     │
│                                │
│  ┌──────────────────────────┐  │ ← #otpGroup (hiện khi 2FA)
│  │ Mã OTP (6 số)            │  │
│  └──────────────────────────┘  │
│  Mã OTP đã được gửi đến email  │
│  của bạn để xác minh 2 bước.  │
│                                │
│  [ ĐĂNG NHẬP ]                 │
│  ...                           │
└────────────────────────────────┘
```

---

## 3. CÁC THÀNH PHẦN GIAO DIỆN

### 3.1 Header Card
- Logo: `<div class="auth-logo"><span>💎</span> MMO Market</div>`
- Tiêu đề: `<h3>Đăng nhập</h3>`

### 3.2 Alert Message
- ID: `#alert-message` | Class: `.message`
- Hiển thị thông báo lỗi từ API (ví dụ: "Email hoặc mật khẩu không chính xác")

### 3.3 Form Đăng Nhập (`#loginForm`)

| Element | ID/Name | Type | Ghi chú |
|:---|:---|:---|:---|
| Input email | `#email` / `name="email"` | `type="email"` | Bắt buộc |
| Error email | `#email-error` | `<small class="field-error">` | |
| Input mật khẩu | `#password` / `name="password"` | `type="password"` | `autocomplete="current-password"` |
| Nút hiện/ẩn mật khẩu | `.toggle-password` | `type="button"` | `data-toggle-password="password"` |
| Error mật khẩu | `#password-error` | `<small class="field-error">` | |
| OTP Group (2FA) | `#otpGroup` | `<div hidden>` | Ẩn mặc định, hiện khi server trả `requires2FA=true` |
| Input OTP | `#loginOtp` / `name="loginOtp"` | `type="text"` | Chỉ hiện ở trạng thái 2FA |
| Error OTP | `#otp-error` | `<small class="field-error">` | |
| Link quên mật khẩu | `.forgot-link` | `<a href="/forgot-password">` | |
| Nút đăng nhập | `#btnLogin` | `type="submit"` | Text: "Đăng nhập" |

### 3.4 Divider
- `<div class="divider">HOẶC</div>`

### 3.5 Google Login Button
- ID: `#googleLoginButton` | Class: `.btn.btn-google`
- Load Google Identity Services script: `https://accounts.google.com/gsi/client`

### 3.6 Footer Links
- "Chưa có tài khoản?" → `<a href="/register">Đăng ký tại đây</a>`
- `<div class="back-home"><a href="/">← Về trang chủ</a></div>`

---

## 4. LUỒNG XỬ LÝ JS (auth.js)

### 4.1 Submit Form Đăng Nhập Thường

1. Lắng nghe `submit` trên `#loginForm`
2. Validate phía client: email không rỗng, password không rỗng
3. Lấy giá trị `#loginOtp` (nếu `#otpGroup` đang hiển thị → đang ở bước 2FA)
4. Gọi API:

**Bước 1 (chưa có OTP / chưa 2FA):**
```
POST /api/auth/login
Body: { email, password }
```

**Xử lý response:**
- `response.requires2FA === true` → Hiện `#otpGroup` (remove `hidden`), focus vào `#loginOtp`
- `response.accessToken` tồn tại → Lưu token, redirect
- Còn lại → Hiện lỗi trong `#alert-message`

**Bước 2 (đã có OTP):**
```
POST /api/auth/login-2fa
Body: { email, password, otp }
```

**Xử lý response:**
- `response.accessToken` tồn tại → Lưu token, redirect
- Còn lại → Hiện lỗi

### 4.2 Lưu Token và Redirect

```javascript
localStorage.setItem('accessToken', response.accessToken);
localStorage.setItem('refreshToken', response.refreshToken);
// Redirect theo role
const role = JSON.parse(response.role)?.role;
if (role === 'Admin')   window.location.href = '/admin/dashboard';
if (role === 'Seller')  window.location.href = '/seller/dashboard';
if (role === 'Staff')   window.location.href = '/staff/dashboard';
else                    window.location.href = '/'; // Customer
```

### 4.3 Google Login

1. Lắng nghe click `#googleLoginButton`
2. Khởi tạo Google Identity Services (`google.accounts.oauth2.initCodeClient`)
3. Nhận `authCode` từ Google callback
4. Gọi API:
```
POST /api/auth/google
Body: { code: authCode }
```
5. Xử lý response như đăng nhập thường (lưu token, redirect)

---

## 5. VALIDATION PHÍA CLIENT

| Field | Điều kiện | Thông báo lỗi |
|:---|:---|:---|
| Email | Không rỗng | "Vui lòng nhập email" |
| Password | Không rỗng | "Vui lòng nhập mật khẩu" |
| OTP (2FA) | 6 chữ số khi otpGroup visible | "Vui lòng nhập mã OTP" |

---

## 6. RESPONSIVE

- **≥ 768px:** Card `.login-wrap` căn giữa, chiều rộng cố định ~420px
- **< 768px:** Card chiếm toàn bộ chiều rộng với padding nhỏ

---

## 7. ACCESSIBILITY

- Nút toggle mật khẩu có `aria-label="Hiển thị mật khẩu"`
- Icon mắt: `<i class="fa fa-eye" aria-hidden="true">`
- `autocomplete="current-password"` trên input mật khẩu

---

## 8. OUT OF SCOPE

- ❌ Trang 2FA riêng biệt — 2FA được xử lý inline trên cùng trang login
- ❌ "Nhớ tôi" (Remember me) / persistent session
