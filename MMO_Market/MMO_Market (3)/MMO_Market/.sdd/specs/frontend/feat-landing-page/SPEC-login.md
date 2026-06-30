# SPEC — Trang Đăng Nhập (Login)

> **Feature ID:** `feat-auth` | **Page:** `Login`
> **Route:** `/login` | **Template:** `templates/auth/login.html`
> **CSS Script:** `static/css/customer/authen.css`
> **JS Script:** `static/js/auth.js`
> **Version:** 1.0 | **Status:** Draft
> **Backend ref:** `feat-auth/UC-01-authentication.md`

---

## 1. TỔNG QUAN TRANG

Trang đăng nhập dành cho mọi khách hàng và người bán. Mục tiêu: **xác thực danh tính người dùng qua tài khoản hệ thống hoặc tài khoản liên kết Google và cấp JWT Token**. Giao diện sử dụng dạng card trung tâm nổi bật trên nền mờ tối giản.

**Cấu trúc trang:**
1. **Logo & Header:** Nhãn MMO Market liên kết về trang chủ.
2. **Auth Card:** Form đăng nhập chính chứa email, mật khẩu (có nút hiển thị/ẩn mật khẩu), nút Đăng nhập và nút Google OAuth2.
3. **Liên kết chuyển đổi:** Dẫn đến trang đăng ký mới hoặc trang yêu cầu đặt lại mật khẩu.

---

## 2. DESIGN TOKENS ÁP DỤNG

```css
/* Màu sắc chủ đạo */
--ds-primary:         #2563eb; /* Xanh thương hiệu */
--ds-primary-hover:   #1d4ed8; /* Hover xanh */
--ds-bg:              #f8fafc; /* Nền xám nhạt */
--ds-card:            #ffffff; /* Nền card trắng */
--ds-text:            #1e293b; /* Màu chữ chính */
--ds-text-sub:        #64748b; /* Màu chữ phụ */
--ds-border:          #cbd5e1; /* Màu viền */
--ds-error:           #ef4444; /* Màu đỏ báo lỗi */

/* Shape & Spacing */
--ds-radius-md:       8px;
--ds-radius-lg:       12px;
--ds-radius-full:     9999px;
--ds-shadow:          0 10px 15px -3px rgba(0,0,0,0.1), 0 4px 6px -4px rgba(0,0,0,0.1);
```

---

## 3. LAYOUT TỔNG THỂ & MOCKUP

```
┌────────────────────────────────────────────────────────┐
│                        [Logo]                          │
│         ┌────────────────────────────────────┐         │
│         │         Đăng Nhập Sàn              │         │
│         ├────────────────────────────────────┤         │
│         │  Email:                            │         │
│         │  [ email@example.com           ]   │         │
│         │  Mật khẩu:                         │         │
│         │  [ **********                  ] 👁 │         │
│         │                                    │         │
│         │            [ ĐĂNG NHẬP ]           │         │
│         │                 hoặc               │         │
│         │        [ Tiếp tục với Google ]     │         │
│         └────────────────────────────────────┘         │
└────────────────────────────────────────────────────────┘
```

---

## 4. CÁC THÀNH PHẦN GIAO DIỆN CHÍNH

### 4.1 Input Mật Khẩu & Icon Mắt Hiển Thị — `.auth-password-wrapper`
* **Vị trí icon mắt (.toggle-password hoặc .auth-toggle-password):**
  * Nằm tuyệt đối ở phía bên phải bên trong input mật khẩu (`position: absolute`, `right: 12px`, `top: 50%`, `transform: translateY(-50%)`).
  * **CSS Rule quan trọng (Quy định khôi phục từ commit chuẩn):** Icon mắt có nền trong suốt hoàn toàn (không có vòng tròn màu xám đậm bao quanh), kích thước tự do theo font-size, màu xám mặc định `#64748b` ở trạng thái thường, và chuyển sang màu xanh chủ đạo `var(--ds-primary)` khi di chuột qua (hover).

### 4.2 Nút Đăng Nhập & Đăng Nhập Google
* **Nút Đăng Nhập (`.btn-submit`):** Chiều rộng 100%, bo tròn pill shape `var(--ds-radius-full)`, nền xanh thương hiệu, hiển thị chữ in hoa đậm. Trạng thái Loading sẽ hiển thị spinner xoay tròn và disable nút để tránh gửi trùng lặp request.
* **Nút Google (`.btn-google`):** Nền trắng, viền xám nhạt, logo Google màu sắc nguyên bản nằm bên trái text "Tiếp tục với Google".

---

## 5. LUỒNG XỬ LÝ JS & AJAX

1. **Lắng nghe sự kiện click Icon Mắt:**
   * JS thực hiện bắt sự kiện click trên `.toggle-password` / `.auth-toggle-password`:
     * Nếu loại input là `password` -> Đổi thành `text` và cập nhật biểu tượng mắt mở.
     * Nếu loại input là `text` -> Đổi lại thành `password` và cập nhật biểu tượng mắt nhắm.
2. **Gửi Request Đăng Nhập:**
   * Bắt sự kiện submit form, validate dữ liệu email không để trống và mật khẩu tối thiểu 6 ký tự.
   * Gửi API request qua AJAX:
     * **Endpoint:** `POST /api/auth/login`
     * **Content-Type:** `application/json`
     * **Payload:** `{ "email": "buyer@example.com", "password": "securepassword" }`
3. **Xử lý phản hồi:**
   * **Thành công (HTTP 200):** Nhận JSON chứa `accessToken` và `refreshToken`.
     * Lưu `accessToken` vào `sessionStorage` (hoặc cookie bảo mật).
     * Điều hướng người dùng về trang chủ `/` hoặc trang yêu cầu trước đó.
   * **Thất bại (HTTP 401/400):**
     * Hiển thị banner lỗi `.auth-error-banner` ở phía trên form với nội dung "Email hoặc mật khẩu không chính xác."
     * Gắn thêm class `.has-error` viền đỏ cho cả hai input Email và Mật khẩu.

---

## 6. RESPONSIVE

* **Viewport ≥ 576px:** Card đăng nhập hiển thị căn giữa màn hình với chiều rộng cố định `420px`.
* **Viewport < 576px:** Card đăng nhập tràn viền 2 bên (margin `16px`), padding bên trong card giảm xuống `20px` để tối ưu không gian hiển thị trên màn hình điện thoại.

---

## 7. ACCESSIBILITY

- Sử dụng `aria-invalid="true"` trên các input bị lỗi dữ liệu đầu vào.
- Nút toggle mật khẩu có thuộc tính `aria-label="Hiển thị mật khẩu"` / `"Ẩn mật khẩu"`.
- Thứ tự Focus Tab: Email -> Mật khẩu -> Icon Mắt -> Nút Đăng nhập -> Nút Google.

---

## 8. OUT OF SCOPE

- ❌ Nhớ mật khẩu (Remember Me) lưu trữ lâu dài.
- ❌ Xác thực vân tay (Biometrics Login).