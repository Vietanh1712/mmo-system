# SPEC — Trang Đăng Ký Tài Khoản (Register)

> **Feature ID:** `feat-auth` | **Page:** `Register`
> **Route:** `/register` | **Template:** `templates/auth/register.html`
> **CSS Script:** `static/css/customer/authen.css`
> **JS Script:** `static/js/auth.js`
> **Version:** 1.0 | **Status:** Draft
> **Backend ref:** `feat-auth/UC-01-authentication.md`

---

## 1. TỔNG QUAN TRANG

Trang đăng ký cho phép khách vãng lai (Guest) tạo tài khoản mới bằng Email và Mật khẩu. Hệ thống sẽ gửi một mã xác thực OTP về email của người dùng và chuyển hướng sang trang xác minh OTP để kích hoạt tài khoản.

**Cấu trúc trang:**
1. **Logo & Header:** Nhãn thương hiệu dẫn về trang chủ công khai.
2. **Auth Card:** Form điền thông tin đăng ký bao gồm Email, Mật khẩu, Nhập lại mật khẩu (có tích hợp nút hiện/ẩn mật khẩu ở cả 2 ô mật khẩu) và nút Đăng ký.
3. **Liên kết điều hướng:** Dẫn đến trang Đăng nhập đối với người dùng đã có tài khoản.

---

## 2. DESIGN TOKENS ÁP DỤNG

```css
/* Color System */
--ds-primary:         #2563eb; /* Màu xanh nhấn */
--ds-primary-hover:   #1d4ed8;
--ds-bg:              #f8fafc;
--ds-card:            #ffffff;
--ds-text:            #1e293b;
--ds-text-sub:        #64748b;
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
│         │         Đăng Ký Tài Khoản          │         │
│         ├────────────────────────────────────┤         │
│         │  Email:                            │         │
│         │  [ email@example.com           ]   │         │
│         │  Mật khẩu:                         │         │
│         │  [ **********                  ] 👁 │         │
│         │  Nhập lại mật khẩu:                │         │
│         │  [ **********                  ] 👁 │         │
│         │                                    │         │
│         │             [ ĐĂNG KÝ ]            │         │
│         ├────────────────────────────────────┤         │
│         │   Đã có tài khoản? Đăng nhập ngay  │         │
│         └────────────────────────────────────┘         │
└────────────────────────────────────────────────────────┘
```

---

## 4. CÁC THÀNH PHẦN GIAO DIỆN CHÍNH

### 4.1 Ô Mật Khẩu Kép & Nút Toggle Hiển Thị
* Cả hai ô nhập Mật khẩu (`#password`) và Nhập lại mật khẩu (`#confirmPassword`) đều được bao bọc bởi class `.auth-password-wrapper`.
* Tích hợp biểu tượng hiển thị/ẩn mật khẩu ở cạnh phải bên trong ô input:
  * Nền trong suốt hoàn toàn, màu xám mặc định `#64748b` chuyển sang màu xanh chủ đạo `var(--ds-primary)` khi hover.
  * Click vào nút sẽ thay đổi thuộc tính `type` của input tương ứng từ `password` sang `text` (và ngược lại) để hiện/ẩn mật mã.

### 4.2 Nút Submit Đăng Ký — `.btn-submit`
* Chiều rộng 100%, border-radius `var(--ds-radius-full)`.
* Khi click đăng ký, nút chuyển sang trạng thái Loading (hiển thị spinner xoay tròn, đổi text thành "Đang xử lý...") và tạm thời disable hành vi click tiếp theo.

---

## 5. LUỒNG XỬ LÝ JS & AJAX

1. **Kiểm tra tính hợp lệ dữ liệu Frontend (Client-side Validation):**
   * Kiểm tra định dạng Email hợp lệ bằng regex.
    * Kiểm tra mật khẩu phải có độ dài tối thiểu 6 ký tự, chứa ít nhất 1 chữ cái viết hoa và ít nhất 1 ký tự đặc biệt.
   * So sánh Mật khẩu và Nhập lại mật khẩu trùng khớp nhau. Nếu không khớp, hiển thị thông báo lỗi ngay dưới ô nhập mật khẩu thứ hai.
2. **Gửi Request Đăng Ký:**
   * Gửi API qua AJAX:
     * **Endpoint:** `POST /api/auth/register`
     * **Content-Type:** `application/json`
     * **Payload:** `{ "email": "user@example.com", "password": "mypassword" }`
3. **Xử lý phản hồi:**
   * **Thành công (HTTP 200):**
     * Trả về thông điệp báo mã OTP đã gửi thành công.
     * Lưu Email đăng ký vào `sessionStorage` để phục vụ xác thực OTP ở bước kế tiếp.
     * Điều hướng người dùng sang trang `/verify-otp`.
   * **Thất bại (HTTP 400):**
     * Nhận phản hồi báo lỗi từ backend (ví dụ: "Email đã tồn tại").
     * Hiển thị banner lỗi `.auth-error-banner` ở phía trên form và tô viền đỏ input vi phạm.

---

## 6. RESPONSIVE

* **Viewport ≥ 576px:** Chiều rộng card đăng ký cố định `420px`.
* **Viewport < 576px:** Card đăng ký chiếm toàn bộ chiều rộng màn hình (chừa lề `16px` mỗi bên), các padding co hẹp để tăng diện tích hiển thị form.

---

## 7. ACCESSIBILITY

- Áp dụng `aria-invalid="true"` khi các trường nhập dữ liệu vi phạm kiểm tra frontend hoặc backend.
- Các nút hiển thị/ẩn mật khẩu có nhãn mô tả `aria-label` tương ứng.
- Focus Tab tự nhiên: Email -> Mật khẩu -> Icon Mắt 1 -> Nhập lại mật khẩu -> Icon Mắt 2 -> Nút Đăng ký -> Link đăng nhập.

---

## 8. OUT OF SCOPE

- ❌ Đăng ký bằng số điện thoại di động gửi SMS.
- ❌ Đăng ký nhanh qua tài khoản mạng xã hội Facebook.