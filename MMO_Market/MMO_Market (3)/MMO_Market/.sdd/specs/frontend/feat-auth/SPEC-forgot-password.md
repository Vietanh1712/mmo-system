# SPEC — Trang Quên Mật Khẩu (Forgot Password)

> **Feature ID:** `feat-auth` | **Page:** `Forgot Password`
> **Route:** `/forgot-password` | **Template:** `templates/auth/forgot-password.html`
> **CSS:** `static/css/customer/authen.css`, `static/css/components/common.css`
> **JS:** `static/js/customer/auth.js`
> **Backend ref:** `feat-auth/UC-01-authentication.md` (Luồng 3.6 — Bước 1)

---

## 1. TỔNG QUAN TRANG

Trang Quên Mật Khẩu là **Bước 1** trong luồng khôi phục mật khẩu. Người dùng nhập email đã đăng ký; hệ thống gửi OTP về email và tự động chuyển hướng sang trang `/reset-password` để nhập OTP và mật khẩu mới.

---

## 2. LAYOUT TỔNG THỂ & MOCKUP

```
┌────────────────────────────────┐
│  💎 MMO Market                 │
│                                │
│  Quên mật khẩu                 │
│                                │
│  Nhập email của bạn để nhận   │
│  mã khôi phục.                 │
│                                │
│  [forgot-msg]                  │
│                                │
│  ┌──────────────────────────┐  │
│  │ 📧 Địa chỉ email của bạn  │  │
│  └──────────────────────────┘  │
│  <forgot-email-error>          │
│                                │
│  [ GỬI MÃ KHÔI PHỤC ]         │
│                                │
│  Quay lại Đăng nhập            │
│                                │
│  ← Về trang chủ               │
└────────────────────────────────┘
```

---

## 3. CÁC THÀNH PHẦN GIAO DIỆN

### 3.1 Header Card
- Logo: `<div class="auth-logo"><span>💎</span> MMO Market</div>`
- Tiêu đề: `<h3>Quên mật khẩu</h3>`
- Mô tả: `<p style="text-align:center; ...">Nhập email của bạn để nhận mã khôi phục.</p>`

### 3.2 Alert Message
- ID: `#forgot-msg` | Class: `.message`

### 3.3 Form Quên Mật Khẩu (`#forgotPasswordForm`)

| Element | ID/Name | Type | Ghi chú |
|:---|:---|:---|:---|
| Input email | `#forgot-email` / `name="email"` | `type="email"` | `autocomplete="off"`, placeholder: "Địa chỉ email của bạn" |
| Error email | `#forgot-email-error` | `<small class="field-error">` | |
| Nút gửi | `#btnForgot` | `type="submit"` | Text: "GỬI MÃ KHÔI PHỤC" (all caps) |

### 3.4 Footer Links
- "Quay lại" → `<a href="/login">Đăng nhập</a>`
- `<div class="back-home"><a href="/">← Về trang chủ</a></div>`

---

## 4. LUỒNG XỬ LÝ JS (auth.js)

1. Lắng nghe `submit` trên `#forgotPasswordForm`
2. Validate email không rỗng và đúng định dạng
3. Gọi API:

```
POST /api/auth/forgot-password
Body: { email: <giá trị #forgot-email> }
```

4. **Xử lý response:**
   - Thành công (HTTP 200):
     - Lưu email vào `sessionStorage` để dùng ở trang `/reset-password`
     - Redirect: `window.location.href = '/reset-password'`
   - Thất bại:
     - Hiển thị `response.message` trong `#forgot-msg`

---

## 5. VALIDATION PHÍA CLIENT

| Field | Điều kiện | Thông báo lỗi |
|:---|:---|:---|
| Email | Không rỗng | "Vui lòng nhập email" |
| Email | Đúng định dạng | "Email không đúng định dạng" |

---

## 6. RESPONSIVE

- **≥ 768px:** Card căn giữa, chiều rộng ~420px
- **< 768px:** Card full width

---

## 7. OUT OF SCOPE

- ❌ Kiểm tra email có tồn tại trong hệ thống ở phía client (để bảo mật, backend không tiết lộ email có tồn tại hay không trong response lỗi)
