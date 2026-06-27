# SPEC — Customer Profile Settings
> **Route:** `/profile` | **Guard:** Private (sessionToken check)
> **Template:** `templates/profile/index.html` | **JS Script:** `static/js/profile.js`
> **Prefix:** `cprf-`

---

## 1. MÔ TẢ TRANG
Giao diện quản lý thông tin cá nhân của khách hàng, cho phép sửa họ tên, số điện thoại, địa chỉ và gửi yêu cầu thay đổi mật khẩu.

---

## 2. MOCKUP GIAO DIỆN
```
┌────────────────────────────────────────────────────────┐
│  MMO Market > Hồ sơ cá nhân                            │
├────────────────────────────────────────────────────────┤
│  Họ và tên:     [ Nguyễn Văn A                     ]   │
│  Email:         customer@gmail.com (Read-only)         │
│  Số điện thoại: [ 0912345678                       ]   │
│  Địa chỉ:       [ 123 Đường Láng, Hà Nội           ]   │
│                                                        │
│             [ Cập Nhật Hồ Sơ ]                         │
│                                                        │
│  Đổi mật khẩu:                                         │
│  Mật khẩu cũ:   [ ********* ]                          │
│  Mật khẩu mới:  [ ********* ]                          │
│  [ Đổi Mật Khẩu ]                                      │
└────────────────────────────────────────────────────────┘
```

---

## 3. STATE VÀ ĐẦU VÀO VALIDATION
- **Họ và tên**: Không được để trống.
- **Số điện thoại**: Định dạng regex `^0[0-9]{9}$`.
- **Mật khẩu mới**: Độ dài tối thiểu 8 ký tự.

---

## 4. LUỒNG XỬ LÝ & GỌI API
1. Khi load trang, gọi `GET /api/v1/profile` qua `api-client.js`.
2. Trả về JSON -> Điền dữ liệu vào input `#fullName`, `#phone`, `#address`.
3. Khi click "Cập Nhật Hồ Sơ":
   * Chạy validate local -> Gửi `PUT /api/v1/profile` với token đính kèm.
   * Hiển thị Toast thông báo thành công.