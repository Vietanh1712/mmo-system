# SPEC — Thiết Lập Hồ Sơ Cá Nhân (Customer Profile Settings)

> **Feature ID:** `feat-customer` | **Page:** `Profile`
> **Route:** `/profile` | **Template:** `templates/profile/index.html`
> **CSS Script:** `static/css/customer/style.css`
> **JS Script:** `static/js/profile.js`
> **Version:** 1.0 | **Status:** Draft
> **Backend ref:** `feat-auth/UC-02-user-profile.md`

---

## 1. TỔNG QUAN TRANG

Trang quản lý hồ sơ cá nhân cho phép người dùng thay đổi thông tin cá nhân cơ bản (họ tên, số điện thoại, địa chỉ, giới tính, số CCCD/CMND, ngày sinh).

**Cấu trúc trang:**
1. **Sidebar điều hướng:** Menu quản lý tài khoản bên trái (Hồ sơ, Rút tiền, KYC, Đơn hàng, Ví).
2. **Profile Panel (Thành phần chính):** Form chỉnh sửa thông tin cá nhân hiện tại.

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

Trang Profile hỗ trợ 2 trạng thái: **Chế độ xem (View Mode)** và **Chế độ chỉnh sửa (Edit Mode)** (khi có URL param `?mode=edit`).

### 3.1 Chế độ xem (View Mode)
```
┌─────────────────────────────────────────────────────────────────┐
│ [Logo] MMO Market                         [Search]      [User]  │
├─────────────────────────────────────────────────────────────────┤
│  ┌──────────────────────┐  ┌─────────────────────────────────┐  │
│  │ (N) Nguyễn Văn Khách │  │ Thông tin cá nhân               │  │
│  │     cus...mail.com   │  │ Xem và cập nhật thông tin...    │  │
│  │                      │  ├─────────────────────────────────┤  │
│  │ Số dư: 934.000 đ [Nạp]  │ Họ và tên     Nguyễn Văn Khách  │  │
│  ├──────────────────────┤  │ Ngày sinh     15/05/1996        │  │
│  │ TÀI KHOẢN            │  │ Giới tính     Nam               │  │
│  │ - Thông tin cá nhân  │  │ Email         cus***@gmail.com  │  │
│  │ - Định danh KYC      │  │ Số điện thoại ******4321        │  │
│  │ - Bảo mật & đổi MK   │  │ Số CCCD/CMND  *******1234       │  │
│  │ - Đăng ký mở Shop    │  │ Địa chỉ       Hà Nội            │  │
│  │                      │  │ Vai trò       Customer          │  │
│  │ VÍ & GIAO DỊCH       │  │ Số dư ví      934.000 đ         │  │
│  │ - Ví của tôi         │  │                                 │  │
│  │ - Nạp tiền           │  │ [ Chỉnh sửa thông tin ]         │  │
│  │ - Lịch sử giao dịch  │  └─────────────────────────────────┘  │
│  │                      │                                       │
│  │ MUA HÀNG             │                                       │
│  │ - Đơn hàng của tôi   │                                       │
│  │ - Thông báo          │                                       │
│  │ - Ticket của tôi     │                                       │
│  └──────────────────────┘                                       │
└─────────────────────────────────────────────────────────────────┘
```

### 3.2 Chế độ chỉnh sửa (Edit Mode)
```
┌─────────────────────────────────────────────────────────────────┐
│  ┌──────────────────────┐  ┌─────────────────────────────────┐  │
│  │ (User Card)          │  │ Cập nhật hồ sơ                  │  │
│  │                      │  │ Email, vai trò và số dư ví...   │  │
│  │ (Menu Sidebar)       │  ├─────────────────────────────────┤  │
│  │                      │  │ Họ và tên                       │  │
│  │                      │  │ [ Nguyễn Văn Khách            ] │  │
│  │                      │  │ Giới tính                       │  │
│  │                      │  │ (o) Nam   () Nữ   () Khác       │  │
│  │                      │  │ Email                           │  │
│  │                      │  │ [ customer01@gmail.com      ]X│  │
│  │                      │  │ Số điện thoại                   │  │
│  │                      │  │ [ 0987654321                  ] │  │
│  │                      │  │ Số CCCD/CMND                    │  │
│  │                      │  │ [ 001096001234                ] │  │
│  │                      │  │ Ngày sinh                       │  │
│  │                      │  │ [ 15/05/1996                ]📅 │  │
│  │                      │  │ Địa chỉ                         │  │
│  │                      │  │ [ Hà Nội                      ] │  │
│  │                      │  │                                 │  │
│  │                      │  │ [ Lưu thay đổi ] [ Hủy ]        │  │
│  └──────────────────────┘  └─────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 4. CÁC THÀNH PHẦN GIAO DIỆN CHÍNH

### 4.1 Layout Chung
* **User Card (Bên trái, trên Sidebar):** Hiển thị Avatar (chữ cái đầu), Họ tên, Email, Số dư hiện tại và nút "Nạp" tiền nhanh.
* **Menu Sidebar:** Chia làm 3 nhóm chính: TÀI KHOẢN, VÍ & GIAO DỊCH, MUA HÀNG. Đánh dấu sáng (`active`) mục "Thông tin cá nhân".

### 4.2 Chế độ xem (View Mode) — `.profile-view`
* Các thông tin nhạy cảm như Email, Số điện thoại, Số CCCD/CMND được ẩn đi một phần (ví dụ: `cus***@gmail.com`, `******4321`, `*******1234`).
* Hiển thị thêm các trường hệ thống không cho phép tự ý sửa: **Vai trò** (Customer, Seller) và **Số dư ví**.
* Nút xanh `[ Chỉnh sửa thông tin ]` sẽ chuyển trang sang trạng thái có thêm parameter `?mode=edit`.

### 4.3 Form Chỉnh Sửa Hồ Sơ — `.profile-form` (Edit Mode)
* Gồm các trường nhập liệu theo thứ tự: 
  1. Họ và tên (`#fullName`)
  2. Giới tính (`#gender` - dạng Radio Button: Nam, Nữ, Khác)
  3. Email (`#email` - nền xám xỉn, mang thuộc tính `disabled`)
  4. Số điện thoại (`#phone`)
  5. Số CCCD/CMND (`#nationalId`)
  6. Ngày sinh (`#dateOfBirth` - dạng Datepicker có icon lịch)
  7. Địa chỉ (`#address`)
* Nút `[ Lưu thay đổi ]` `.btn-update-profile` màu xanh để submit form. Nút `[ Hủy ]` màu trắng để quay lại chế độ xem (bỏ `?mode=edit`).

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
     * **Payload:** `{ "fullName": "Nguyen Van A", "phone": "0987654321", "address": "Hanoi", "gender": "Nam", "nationalId": "001090123456", "dateOfBirth": "2000-01-01" }`
   * **Thành công (HTTP 200):** Hiển thị Toast thông báo cập nhật hồ sơ thành công.
---

## 6. RESPONSIVE

* **Viewport ≥ 768px:** Layout dạng Grid chia 2 cột: Sidebar trái chiếm 250px, Content phải chiếm phần còn lại.
* **Viewport < 768px:** Sidebar chuyển thành thanh Menu cuộn ngang (Horizontal scroll) ở phía trên cùng, Content chuyển xuống dưới dạng 1 cột dọc đầy đủ.

---

## 7. ACCESSIBILITY

- Sử dụng `aria-required="true"` cho các ô nhập liệu bắt buộc như Họ tên.
- Input email mang thuộc tính `aria-disabled="true"`.

---

## 8. OUT OF SCOPE

- ❌ Tải ảnh đại diện trực tiếp trên trang (avatar upload) — Chỉ hiển thị ảnh avatar mặc định.
- ❌ Thay đổi địa chỉ email tài khoản (Email gắn cố định với tài khoản đăng ký).