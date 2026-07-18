# SPEC — Bảo Mật & Đổi Mật Khẩu (Security & Password)

> **Feature ID:** `feat-customer` | **Page:** `Security`
> **Route:** `/account/security` | **Template:** `templates/account/security.html`
> **CSS Script:** `static/css/customer/style.css`
> **JS Script:** `static/js/security.js`
> **Version:** 1.0 | **Status:** Draft
> **Backend ref:** `feat-auth/UC-01-authentication.md`

---

## 1. TỔNG QUAN TRANG

Trang bảo mật tài khoản cho phép người dùng thực hiện các thao tác liên quan đến bảo vệ tài khoản, cụ thể là Đổi mật khẩu.

**Cấu trúc trang:**
1. **Sidebar điều hướng:** Menu quản lý tài khoản bên trái (Hồ sơ, Rút tiền, KYC, Đơn hàng, Ví).
2. **Security Panel (Thành phần chính):** Form nhập mật khẩu hiện tại và mật khẩu mới để cập nhật bảo mật.

---

## 2. DESIGN TOKENS ÁP DỤNG

```css
/* Color Palette */
--ds-primary:         #2563eb;
--ds-primary-hover:   #1d4ed8;
--ds-bg:              #f8fafc;
--ds-card:            #ffffff;
--ds-text:            #1e293b;
--ds-text-sub:        #64748b;
--ds-border:          #e2e8f0;
--ds-error:           #ef4444;

/* Spacing & Borders */
--ds-radius-md:       8px;
--ds-radius-lg:       12px;
--ds-shadow:          0 1px 3px 0 rgba(0,0,0,0.1), 0 1px 2px 0 rgba(0,0,0,0.06);
```

---

## 3. LAYOUT TỔNG THỂ & MOCKUP

```
┌─────────────────────────────────────────────────────────┐
│ [Logo] MMO Market                         [Search]      │
├─────────────────────────────────────────────────────────┤
│  ┌─────────────────┐  ┌───────────────────────────────┐ │
│  │ (User Card)     │  │ Tài khoản của tôi             │ │
│  │                 │  │ BẢO MẬT & ĐỔI MẬT KHẨU        │ │
│  │ (Sidebar)       │  ├───────────────────────────────┤ │
│  │ - Thông tin     │  │ [!] Bạn nên bật 2FA để tăng...│ │
│  │ - Định danh     │  ├───────────────────────────────┤ │
│  │ - Bảo mật       │  │ Tổng quan bảo mật             │ │
│  │ - Ví của tôi    │  │ [ Mật khẩu: Đã thiết lập ]    │ │
│  │ - ...           │  │ [ 2FA: Chưa kích hoạt    ]    │ │
│  │                 │  │ [ Email: Đã xác thực     ]    │ │
│  │                 │  │ [ KYC: Chưa định danh    ]    │ │
│  │                 │  ├───────────────────────────────┤ │
│  │                 │  │ Đổi mật khẩu      Bảo mật 2FA │ │
│  │                 │  │ [ MK hiện tại ]   [Toggle Bật]│ │
│  │                 │  │ [ MK mới      ]               │ │
│  │                 │  │ [ Xác nhận MK ]   Phiên Login │ │
│  │                 │  │ [ ĐỔI MẬT KHẨU]   [Windows]   │ │
│  └─────────────────┘  └───────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
```

---

## 4. CÁC THÀNH PHẦN GIAO DIỆN CHÍNH

### 4.1 Khối Tổng quan bảo mật (Security Overview)
* Hiển thị trạng thái của 4 tiêu chí: Mật khẩu, Bảo mật 2 lớp (2FA), Email, KYC.
* Các badge trạng thái (OK, Khuyến nghị, Đã xác thực) sử dụng màu sắc tương ứng (Xanh lá, Cam nhạt).

### 4.2 Form Đổi Mật Khẩu — `.password-change-form`
* Nằm ở cột bên trái phía dưới.
* Gồm các trường: Mật khẩu hiện tại (`#currentPassword`), Mật khẩu mới (`#newPassword`), Xác nhận mật khẩu mới (`#confirmNewPassword`).
* Tích hợp icon con mắt để hiển thị/ẩn mật khẩu ở mỗi ô input.

### 4.3 Khối Bảo mật 2 lớp (2FA) & Phiên đăng nhập
* Nằm ở cột bên phải phía dưới.
* **Toggle 2FA:** Một nút gạt (Switch/Toggle). Mặc định là "Chưa kích hoạt". Khi gạt sang phải sẽ hiển thị form/modal yêu cầu OTP để kích hoạt.
* **Phiên đăng nhập hiện tại:** Hiển thị thiết bị/trình duyệt đang sử dụng (ví dụ: Windows). Có badge "Đang hoạt động".

---

## 5. LUỒNG XỬ LÝ JS & AJAX

1. **Thực hiện đổi mật khẩu:**
    * Kiểm tra mật khẩu mới trùng khớp mật khẩu xác nhận, có độ dài tối thiểu 6 ký tự, chứa ít nhất 1 chữ hoa và 1 ký tự đặc biệt.
    * Gửi API:
      * **Endpoint:** `PUT /api/v1/profile/password`
      * **Headers:** `Authorization: Bearer <token>`
      * **Payload:** `{ "currentPassword": "...", "newPassword": "..." }`
    * **Thành công:** Hiển thị Toast thành công và xóa sạch các ô input.
    * **Thất bại:** Báo lỗi dưới ô input.

2. **Kích hoạt Bảo mật 2 lớp (2FA):**
    * Khi click Toggle Bật 2FA -> Gửi API `POST /api/v1/profile/2fa/send-otp`
    * Hiển thị popup/modal yêu cầu nhập mã OTP gửi về Email.
    * Xác nhận OTP -> Gửi API `POST /api/v1/profile/2fa/enable` với payload `{ "otp": "..." }`
    * **Thành công:** Toggle chuyển sang trạng thái "Đã kích hoạt" màu xanh. Bảng tổng quan cập nhật trạng thái "OK".

3. **Tắt Bảo mật 2 lớp (2FA):**
    * Khi click Toggle Tắt 2FA -> Gửi API `POST /api/v1/profile/2fa/disable`
    * (Có thể yêu cầu confirm mật khẩu hoặc OTP trước khi tắt tùy luồng chi tiết).

---

## 6. RESPONSIVE

* **Viewport ≥ 768px:** Layout dạng Grid chia 2 cột: Sidebar trái chiếm 250px, Content phải chiếm phần còn lại.
* **Viewport < 768px:** Sidebar chuyển thành thanh Menu cuộn ngang (Horizontal scroll) ở phía trên cùng, Content chuyển xuống dưới dạng 1 cột dọc đầy đủ.

---

## 7. ACCESSIBILITY

- Sử dụng `aria-required="true"` cho tất cả các ô nhập liệu (Mật khẩu hiện tại, mới, xác nhận).
- Button toggle hiển thị mật khẩu cần có `aria-label="Hiện mật khẩu"` / `"Ẩn mật khẩu"`.
