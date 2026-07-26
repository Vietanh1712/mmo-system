# SPEC — Đăng Ký Mở Shop (Seller Registration)

> **Feature ID:** `feat-customer` | **Page:** `Register Shop`
> **Route:** `/account/register-shop` | **Template:** `templates/account/register-shop.html`
> **CSS Script:** `static/css/customer/style.css`
> **JS Script:** `static/js/customer/account-register-shop.js`

---

## 1. TỔNG QUAN TRANG

Trang cho phép người dùng thông thường nâng cấp tài khoản thành Người Bán (Seller) để mở gian hàng trên MMO Market. Quy trình này yêu cầu người dùng phải đáp ứng đủ 3 điều kiện (Xác thực Email, Bổ sung đủ Profile, và Duyệt KYC). Đăng ký sẽ được duyệt tự động và thu một khoản phí mở Shop (do Admin thiết lập trong cấu hình hệ thống).

**Cấu trúc trang:**
1. **Sidebar điều hướng:** Menu bên trái.
2. **Khu vực Đăng ký (Main):** Trình bày các điều kiện cần thiết và form nhập liệu thông tin Shop.

---

## 2. LAYOUT TỔNG THỂ & MOCKUP

```text
┌─────────────────────────────────────────────────────────────────┐
│ [Logo] MMO Market                         [Search]      [User]  │
├─────────────────────────────────────────────────────────────────┤
│  ┌──────────────────────┐  ┌─────────────────────────────────┐  │
│  │ (User Card)          │  │ Nâng cấp tài khoản              │  │
│  │                      │  │ Đăng ký mở Shop  [Chưa đăng ký] │  │
│  │ (Menu Sidebar)       │  │ Gửi yêu cầu trở thành Seller... │  │
│  │ - Thông tin cá nhân  │  ├─────────────────────────────────┤  │
│  │ - Định danh KYC      │  │ [i] Hồ sơ cần được KYC trước... │  │
│  │ - Đăng ký mở Shop    │  ├─────────────────────────────────┤  │
│  │                      │  │ Điều kiện mở Shop [2/3 hoàn tất]│  │
│  │                      │  │ [v] Xác thực email              │  │
│  │                      │  │ [v] Thông tin cá nhân           │  │
│  │                      │  │ [!] Định danh KYC   [Check KYC] │  │
│  │                      │  ├─────────────────────────────────┤  │
│  │                      │  │ Thông tin đăng ký               │  │
│  │                      │  │ Tên Shop         Danh mục       │  │
│  │                      │  │ [ Tên gian hàng] [ Chọn DM  V ] │  │
│  │                      │  │ Mô tả Shop                      │  │
│  │                      │  │ [                           ]   │  │
│  │                      │  │ Email hỗ trợ     SĐT hỗ trợ     │  │
│  │                      │  │ [ admin@mmo.net] [ 0987654..]   │  │
│  │                      │  │                                 │  │
│  │                      │  │ [x] Tôi cam kết tuân thủ...     │  │
│  │                      │  │ [ Gửi yêu cầu mở Shop ] [Hủy]   │  │
│  └──────────────────────┘  └─────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 3. CÁC THÀNH PHẦN GIAO DIỆN CHÍNH

### 3.1 Khối Điều Kiện Mở Shop (Conditions Checklist)
* Hiển thị danh sách 3 điều kiện:
  1. **Xác thực Email:** Nếu đã xác thực (hiển thị Tick xanh).
  2. **Thông tin cá nhân:** Đã cập nhật đủ số điện thoại và thông tin cơ bản (Tick xanh).
  3. **Định danh KYC:** Trạng thái KYC của user (`APPROVED` -> Tick xanh; ngược lại -> Icon đồng hồ màu cam và nút "Kiểm tra KYC" điều hướng sang trang KYC).
* **Tiến trình:** Hiển thị text ví dụ `2/3 hoàn tất` dựa trên số điều kiện đạt được.

### 3.2 Form Thông Tin Đăng Ký (Shop Form)
* Bị **vô hiệu hóa (disabled)** nút "Gửi yêu cầu" nếu chưa hoàn thành 3/3 điều kiện mở Shop (đặc biệt là KYC).
* **Fields:** 
  * `Tên Shop`: Text input (bắt buộc).
  * `Danh mục kinh doanh chính`: Dropdown Select.
  * `Mô tả Shop`: Textarea.
  * `Email hỗ trợ`: Text input (Gợi ý điền sẵn email của user).
  * `SĐT hỗ trợ`: Text input (Gợi ý điền sẵn SĐT của user).
* **Checkbox Cam kết:** Bắt buộc phải được đánh dấu (Checked) mới kích hoạt được nút Submit.
* Nút `[ Gửi yêu cầu mở Shop ]` sẽ gọi API đăng ký.

---

## 4. LUỒNG XỬ LÝ JS & AJAX

1. **Gửi yêu cầu Mở Shop:**
   * Bắt sự kiện click nút "Gửi yêu cầu mở Shop". Lấy dữ liệu từ các ô input.
   * Hiển thị hộp thoại Xác nhận (Confirm Dialog): Cảnh báo hệ thống sẽ trừ một khoản **Phí mở Shop** (ví dụ 50.000 VNĐ) trong ví của tài khoản. Số tiền phí này được Admin cấu hình linh động trên hệ thống thông qua biến cấu hình `SHOP_OPENING_FEE_VND`.
   * Nếu User đồng ý, gọi API:
     * **Endpoint:** `POST /api/v1/profile/register-shop`
     * **Headers:** `Authorization: Bearer <token>`
     * **Payload:** `{ shopName, description, category, supportEmail, supportPhone }`
2. **Xử lý phản hồi:**
   * **Thành công (HTTP 200):** Thông báo đăng ký thành công. Hệ thống tự động nâng cấp user thành SELLER. Reload lại trang để thay đổi giao diện (có thể ẩn form đăng ký và hiện nút "Đến kênh người bán").
   * **Thất bại (HTTP 400):** 
     * Nếu lỗi do số dư ví (Balance < 50k), thông báo "Số dư không đủ" và hướng dẫn đi Nạp tiền.
     * Nếu lỗi do KYC chưa hoàn tất hoặc tên shop trùng lặp, hiện lỗi tương ứng ở Toast Message.
