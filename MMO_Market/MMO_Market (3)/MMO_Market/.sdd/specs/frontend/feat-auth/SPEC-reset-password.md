# SPEC — Trang Đặt Lại Mật Khẩu (Reset Password)

> **Feature ID:** `feat-auth` | **Page:** `Reset Password`
> **Route:** `/reset-password` | **Template:** `templates/auth/reset-password.html`
> **CSS:** `static/css/customer/authen.css`, `static/css/components/common.css`
> **JS:** `static/js/customer/auth.js`
> **Backend ref:** `feat-auth/UC-01-authentication.md` (Luồng 3.6 — Bước 2 và 3)

---

## 1. TỔNG QUAN TRANG

Trang Đặt Lại Mật Khẩu là **Bước 2 + 3** trong luồng khôi phục mật khẩu, được chia làm **2 step hiển thị inline** trên cùng một trang:

- **Step 1 (`#step-1`):** Nhập mã OTP nhận được qua email → Validate OTP (`POST /api/auth/check-reset-otp`)
- **Step 2 (`#step-2`):** Nhập mật khẩu mới và xác nhận → Lưu mật khẩu (`POST /api/auth/reset-password`)

Step 2 **ẩn mặc định** (`display: none`), chỉ hiện sau khi OTP được validate thành công.

---

## 2. LAYOUT TỔNG THỂ & MOCKUP

### Step 1 — Nhập OTP

```
┌────────────────────────────────┐
│  Đặt lại mật khẩu              │
│                                │
│  [reset-msg]                   │
│                                │
│  ┌── #step-1 ─────────────┐   │
│  │ Vui lòng nhập mã xác   │   │
│  │ thực (OTP) đã được gửi │   │
│  │ đến email của bạn.     │   │
│  │                        │   │
│  │ ┌──────────────────┐   │   │
│  │ │ ••••••           │   │   │  ← #reset-otp
│  │ └──────────────────┘   │   │
│  │ <reset-otp-error>      │   │
│  │                        │   │
│  │ [ TIẾP TỤC ]           │   │  ← #btnNextStep
│  │                        │   │
│  │ Không nhận được mã?    │   │
│  │ Gửi lại ← #resendOtp   │   │
│  └────────────────────────┘   │
│                                │
│  Quay lại Đăng nhập            │
│  ← Về trang chủ               │
└────────────────────────────────┘
```

### Step 2 — Nhập Mật Khẩu Mới (hiện sau khi OTP hợp lệ)

```
┌────────────────────────────────┐
│  Đặt lại mật khẩu              │
│                                │
│  [reset-msg]                   │
│                                │
│  ┌── #step-2 ─────────────┐   │
│  │ ✅ Mã hợp lệ. Hãy thiết│   │
│  │    lập mật khẩu mới.   │   │
│  │                        │   │
│  │ Mật khẩu mới:          │   │
│  │ ┌──────────────────┐ 👁│   │
│  │ │ Nhập mật khẩu mới│   │   │  ← #new-password
│  │ └──────────────────┘   │   │
│  │ <new-password-error>   │   │
│  │                        │   │
│  │ Xác nhận mật khẩu mới: │   │
│  │ ┌──────────────────┐ 👁│   │
│  │ │ Nhập lại MK mới  │   │   │  ← #confirm-password
│  │ └──────────────────┘   │   │
│  │ <confirm-password-error>│   │
│  │                        │   │
│  │ [ LƯU MẬT KHẨU ]       │   │  ← #btnSubmitReset
│  │                        │   │
│  │ ← Quay lại ← #btnBackToStep1│
│  └────────────────────────┘   │
│                                │
│  Quay lại Đăng nhập            │
└────────────────────────────────┘
```

---

## 3. CÁC THÀNH PHẦN GIAO DIỆN

### 3.1 Header
- Tiêu đề: `<h3>Đặt lại mật khẩu</h3>`
- *(Trang này không có auth-logo)*

### 3.2 Alert Message
- ID: `#reset-msg` | Class: `.message`

### 3.3 Form (`#resetPasswordForm`)

**Step 1 — `#step-1` (hiển thị mặc định):**

| Element | ID/Name | Type | Ghi chú |
|:---|:---|:---|:---|
| Input OTP | `#reset-otp` / `name="otp"` | `type="text"` | `class="otp-input"`, `maxlength="6"`, `autocomplete="off"`, `placeholder="••••••"` |
| Error OTP | `#reset-otp-error` | `<small class="field-error">` | |
| Nút tiếp tục | `#btnNextStep` | `type="button"` | Text: "TIẾP TỤC" — gọi `check-reset-otp` |
| Link gửi lại | `#resendOtp` | `<a href="#">` | Gọi `forgot-password` lại với email từ storage |

**Step 2 — `#step-2` (`display: none` mặc định, animation `fadeIn` khi hiện):**

| Element | ID/Name | Type | Ghi chú |
|:---|:---|:---|:---|
| Thông báo thành công | `<p style="color: #2ecc71">` | | `<i class="fas fa-check-circle">` |
| Input MK mới | `#new-password` / `name="newPassword"` | `type="password"` | |
| Error MK mới | `#new-password-error` | `<small class="field-error">` | |
| Toggle MK mới | `.toggle-password` | `type="button"` | `data-toggle-password="new-password"` |
| Input xác nhận MK | `#confirm-password` | `type="password"` | |
| Error xác nhận MK | `#confirm-password-error` | `<small class="field-error">` | |
| Toggle xác nhận MK | `.toggle-password` | `type="button"` | `data-toggle-password="confirm-password"` |
| Nút lưu | `#btnSubmitReset` | `type="button"` | Text: "LƯU MẬT KHẨU" |
| Nút quay lại Step 1 | `#btnBackToStep1` | `<a>` | Hiện lại step-1, ẩn step-2 |

### 3.4 Footer Links
- `#login-link-container`: "Quay lại" → `<a href="/login">Đăng nhập</a>`
- `<div class="back-home"><a href="/">← Về trang chủ</a></div>`

---

## 4. LUỒNG XỬ LÝ JS (auth.js)

### 4.1 Khởi tạo

```javascript
// Đọc email từ storage (lưu bởi trang forgot-password)
const email = sessionStorage.getItem('resetEmail');
```

### 4.2 Bước 1 — Validate OTP (`#btnNextStep`)

1. Lắng nghe click `#btnNextStep`
2. Validate OTP client-side (không rỗng, 6 số)
3. Gọi API:

```
POST /api/auth/check-reset-otp
Body: { email: <từ storage>, otp: <giá trị #reset-otp> }
```

4. **Xử lý response:**
   - `response.success === true`:
     - Ẩn `#step-1` (`display: none`)
     - Hiện `#step-2` (`display: block`) — animation fadeIn 0.4s
   - `response.success === false`:
     - Hiển thị lỗi trong `#reset-otp-error` hoặc `#reset-msg`

### 4.3 Gửi Lại OTP (`#resendOtp`)

```
POST /api/auth/forgot-password
Body: { email: <từ storage> }
```

- Thành công: Hiển thị toast "Mã khôi phục mới đã được gửi"

### 4.4 Bước 2 — Lưu Mật Khẩu Mới (`#btnSubmitReset`)

1. Lắng nghe click `#btnSubmitReset`
2. Validate:
   - MK mới không rỗng, tối thiểu 6 ký tự
   - Xác nhận MK khớp với MK mới
3. Gọi API:

```
POST /api/auth/reset-password
Body: {
  email:       <từ storage>,
  otp:         <giá trị #reset-otp (step 1)>,
  newPassword: <giá trị #new-password>
}
```

4. **Xử lý response:**
   - Thành công (HTTP 200):
     - Xóa email khỏi storage
     - Hiển thị thông báo thành công
     - Redirect sau 1.5s: `window.location.href = '/login'`
   - Thất bại:
     - Hiển thị `response.message` trong `#reset-msg`

### 4.5 Quay Lại Step 1 (`#btnBackToStep1`)

```javascript
document.getElementById('btnBackToStep1').addEventListener('click', () => {
    document.getElementById('step-2').style.display = 'none';
    document.getElementById('step-1').style.display = 'block';
});
```

---

## 5. ANIMATION

```css
.step-container {
    animation: fadeIn 0.4s ease-in-out;
}
@keyframes fadeIn {
    from { opacity: 0; transform: translateY(10px); }
    to   { opacity: 1; transform: translateY(0); }
}
```

---

## 6. VALIDATION PHÍA CLIENT

| Field | Điều kiện | Thông báo lỗi |
|:---|:---|:---|
| OTP | Không rỗng, 6 ký tự số | "Vui lòng nhập mã OTP 6 số" |
| MK mới | Không rỗng, ≥ 6 ký tự | "Mật khẩu phải từ 6 ký tự trở lên" |
| Xác nhận MK | Khớp với MK mới | "Mật khẩu xác nhận không khớp" |

---

## 7. ACCESSIBILITY

- Nút toggle mật khẩu mới: `aria-label="Hiển thị mật khẩu"`
- Nút toggle xác nhận: `aria-label="Hiển thị mật khẩu xác nhận"`
- Ẩn nút mắt mặc định của trình duyệt Edge/IE:
  ```css
  input[type="password"]::-ms-reveal,
  input[type="password"]::-ms-clear { display: none !important; }
  ```

---

## 8. OUT OF SCOPE

- ❌ Cooldown đếm ngược giữa các lần gửi lại OTP
- ❌ Progress bar thể hiện bước 1/2
