# SPEC — Thiết Lập Hồ Sơ Cá Nhân (Customer Profile Settings)

> **Feature ID:** `feat-customer` | **Page:** `Profile`
> **Route:** `/profile` | **Template:** `templates/profile/index.html`
> **CSS Script:** `static/css/customer/style.css`
> **JS Script:** `static/js/profile.js`
> **Version:** 1.0 | **Status:** Draft
> **Backend ref:** `feat-auth/UC-02-user-profile.md`

---

## 1. TỔNG QUAN TRANG

Trang quản lý hồ sơ cá nhân cho phép người dùng thay đổi thông tin định danh (họ tên, số điện thoại, địa chỉ) và thực hiện đổi mật khẩu đăng nhập tài khoản.

**Cấu trúc trang:**
1. **Sidebar điều hướng:** Menu quản lý tài khoản bên trái (Hồ sơ, Rút tiền, KYC, Đơn hàng, Ví).
2. **Profile Panel (Thành phần chính):** Form chỉnh sửa thông tin cá nhân hiện tại.
3. **Change Password Panel:** Form nhập mật khẩu cũ và mật khẩu mới để cập nhật bảo mật.

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
┌────────────────────────────────────────────────────────┐
│ [Logo] MMO Market                              [User]  │
├────────────────────────────────────────────────────────┤
│  ┌───────────┐  ┌────────────────────────────────────┐ │
│  │  Sidebar  │  │  HỒ SƠ CÁ NHÂN                     │ │
│  │  - Hồ sơ  │  │  Họ và tên: [ Nguyễn Văn A     ]   │ │
│  │  - Ví tiền│  │  Số điện thoại: [ 0987654321   ]   │ │
│  │  - KYC    │  │  [ CẬP NHẬT ]                      │ │
│  │  - Orders │  ├────────────────────────────────────┤ │
│  └───────────┘  │  ĐỔI MẬT KHẨU                      │ │
│                 │  Mật khẩu cũ: [ ****** ]           │ │
│                 │  Mật khẩu mới: [ ****** ]          │ │
│                 │  [ ĐỔI MẬT KHẨU ]                  │ │
│                 └────────────────────────────────────┘ │
└────────────────────────────────────────────────────────┘
```

---

## 4. CÁC THÀNH PHẦN GIAO DIỆN CHÍNH

### 4.1 Form Chỉnh Sửa Hồ Sơ — `.profile-form`
* Gồm các trường: Họ và tên (`#fullName`), Số điện thoại (`#phone`), Địa chỉ (`#address`), Email (chỉ đọc, `#email` mang thuộc tính `disabled`).
* Các trường input có chiều cao `46px`, bo góc `var(--ds-radius-md)`. Khi focus có viền xanh thương hiệu.
* Nút cập nhật `.btn-update-profile` có hiệu ứng di chuột chuyển màu xanh đậm hơn.

### 4.2 Form Đổi Mật Khẩu — `.password-change-form`
* Gồm các trường: Mật khẩu cũ (`#oldPassword`), Mật khẩu mới (`#newPassword`), Xác nhận mật khẩu mới (`#confirmNewPassword`).
* Tích hợp icon hiển thị/ẩn mật khẩu ở mỗi ô input.

---

## 5. LUỒNG XỬ LÝ JS & AJAX

1. **Tải thông tin hồ sơ hiện tại:**
   * Khi load trang, JS thực hiện gọi API:
     * **Endpoint:** `GET /api/v1/profile`
     * **Headers:** `Authorization: Bearer <token>`
   * **Thành công (HTTP 200):** Điền dữ liệu nhận được vào các ô input tương ứng.
2. **Cập nhật thông tin hồ sơ:**
   * Lắng nghe click nút "Cập nhật". Kiểm tra định dạng số điện thoại (`^0[0-9]{9}$`).
   * Gửi API:
     * **Endpoint:** `PUT /api/v1/profile`
     * **Payload:** `{ "fullName": "Nguyen Van A", "phone": "0987654321", "address": "Hanoi" }`
   * **Thành công (HTTP 200):** Hiển thị Toast thông báo cập nhật hồ sơ thành công.
3. **Thực hiện đổi mật khẩu:**
    * Kiểm tra mật khẩu mới trùng khớp mật khẩu xác nhận, có độ dài tối thiểu 6 ký tự, chứa ít nhất 1 chữ cái viết hoa và ít nhất 1 ký tự đặc biệt.
   * Gửi API:
     * **Endpoint:** `POST /api/v1/profile/change-password`
     * **Payload:** `{ "oldPassword": "...", "newPassword": "..." }`
   * **Thành công (HTTP 200):** Hiển thị Toast thông báo đổi mật khẩu thành công và xóa sạch các ô input mật khẩu.
   * **Thất bại (HTTP 400):** Hiển thị thông báo lỗi cụ thể dưới input tương ứng.

---

## 6. RESPONSIVE

* **Viewport ≥ 768px:** Layout dạng Grid chia 2 cột: Sidebar trái chiếm 250px, Content phải chiếm phần còn lại.
* **Viewport < 768px:** Sidebar chuyển thành thanh Menu cuộn ngang (Horizontal scroll) ở phía trên cùng, Content chuyển xuống dưới dạng 1 cột dọc đầy đủ.

---

## 7. ACCESSIBILITY

- Sử dụng `aria-required="true"` cho các ô nhập liệu bắt buộc như Họ tên, Mật khẩu cũ/mới.
- Input email mang thuộc tính `aria-disabled="true"`.

---

## 8. OUT OF SCOPE

- ❌ Tải ảnh đại diện trực tiếp trên trang (avatar upload) — Chỉ hiển thị ảnh avatar mặc định.
- ❌ Thay đổi địa chỉ email tài khoản (Email gắn cố định với tài khoản đăng ký).