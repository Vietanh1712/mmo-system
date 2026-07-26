# SPEC — Frontend Authentication (feat-auth)

> **Feature ID:** `feat-auth` | **Trạng thái:** Published
> **Backend ref:** `.sdd/specs/backend/feat-auth/UC-01-authentication.md`

---

## 1. TỔNG QUAN FEATURE

Feature xác thực bao gồm toàn bộ luồng đăng ký, đăng nhập, xác thực OTP, đăng nhập Google OAuth2, và khôi phục mật khẩu. Tất cả các trang auth dùng **chung một layout** (`auth-layout`) và **một file JS duy nhất** (`auth.js`).

## 2. CẤU TRÚC FILE

```
templates/auth/
├── login.html           ← Trang đăng nhập
├── register.html        ← Trang đăng ký tài khoản
├── verify-otp.html      ← Trang xác thực OTP đăng ký
├── forgot-password.html ← Trang nhập email quên mật khẩu
└── reset-password.html  ← Trang đặt lại mật khẩu (2 bước)

static/css/customer/
└── authen.css           ← CSS riêng cho auth layout

static/css/components/
└── common.css           ← CSS dùng chung

static/js/customer/
└── auth.js              ← Logic JS cho toàn bộ các trang auth
```

## 3. LAYOUT AUTH CHUNG

Tất cả trang auth dùng cùng một layout glassmorphism với background mờ:

```html
<body class="auth-layout">
  <div class="bg-blur"></div>      <!-- Nền mờ blur -->
  <div class="bg-overlay"></div>   <!-- Overlay tối -->
  <div class="container">
    <div class="login-wrap">       <!-- Card trung tâm -->
      <!-- Nội dung trang -->
    </div>
  </div>
</body>
```

## 4. DESIGN TOKENS

```css
/* Brand */
--brand-orange:     #ea580c;   /* Màu cam thương hiệu */
--brand-orange-dis: #94a3b8;   /* Cam disabled (đếm ngược OTP) */

/* Text */
--ds-text-muted:    #475569;   /* Văn bản phụ */
--ds-text-secondary:#555;      /* Văn bản mô tả */

/* State */
--ds-error:         #ef4444;   /* Lỗi validation */
--ds-success:       #2ecc71;   /* Thành công (OTP hợp lệ) */
```

## 5. PAGES TRONG FEATURE

| Trang | Route | File Template | Spec Chi Tiết |
|:---|:---|:---|:---|
| Đăng nhập | `/login` | `auth/login.html` | [SPEC-login.md](SPEC-login.md) |
| Đăng ký | `/register` | `auth/register.html` | [SPEC-register.md](SPEC-register.md) |
| Xác thực OTP | `/verify-otp` | `auth/verify-otp.html` | [SPEC-verify-otp.md](SPEC-verify-otp.md) |
| Quên mật khẩu | `/forgot-password` | `auth/forgot-password.html` | [SPEC-forgot-password.md](SPEC-forgot-password.md) |
| Đặt lại mật khẩu | `/reset-password` | `auth/reset-password.html` | [SPEC-reset-password.md](SPEC-reset-password.md) |

## 6. OUT OF SCOPE

- ❌ Đăng nhập bằng Facebook, Apple ID.
- ❌ Cooldown đếm ngược 60s khi gửi lại OTP (chưa implement trong code).
- ❌ Trang 2FA riêng biệt — luồng 2FA được xử lý inline trên trang `/login`.
