# SPEC — Form Đăng & Sửa Sản Phẩm (Seller Product Add & Edit Form)

> **Feature ID:** `feat-seller` | **Pages:** `ProductAdd`, `ProductEdit`
> **Routes:** `/seller/products/new`, `/seller/products/edit/{productId}`
> **Templates:** `templates/seller/product-add.html`, `templates/seller/product-edit.html`
> **CSS Script:** `static/css/customer/style.css`
> **JS Script:** `static/js/seller-product-form.js`
> **Version:** 1.0 | **Status:** Draft
> **Backend ref:** `feat-product/UC-06-shop-product-management.md`

---

## 1. TỔNG QUAN TRANG

Trang biểu mẫu sản phẩm cung cấp giao diện cho phép người bán (Seller) thực hiện thêm mới sản phẩm số hoặc chỉnh sửa thông tin sản phẩm hiện tại, cấu hình các phân loại biến thể linh hoạt và tải lên danh sách mã code tài sản số bàn giao tự động cho khách hàng.

**Cấu trúc trang:**
1. **Sidebar điều hướng Kênh Người Bán:** Menu quản lý tài khoản bên trái.
2. **Form thông tin cơ bản (Basic Info Card):** Tên sản phẩm, mô tả chi tiết, danh mục ngành hàng (ACCOUNT, KEY, GAME_CARD, SERVICE).
3. **Form quản lý biến thể (Variants Form Builder):** Cho phép bấm nút "Thêm phân loại" để tạo động các biến thể (ví dụ: Phân loại: "Thời hạn", các lựa chọn: "1 Tháng" - giá 50k, "3 Tháng" - giá 140k).
4. **Vùng nhập mã thẻ bàn giao (Asset Input Zone):** Ô nhập văn bản lớn (Textarea) để dán danh sách mã code tài sản (mỗi dòng một mã) bàn giao tự động cho từng biến thể tương ứng.

---

## 2. DESIGN TOKENS ÁP DỤNG

```css
/* Color Palette */
--ds-primary:         #2563eb;
--ds-primary-hover:   #1d4ed8;
--ds-bg:              #f8fafc;
--ds-card:            #ffffff;
--ds-border:          #cbd5e1;
--ds-error:           #ef4444;
--ds-success:         #10b981;

/* Shape & Spacing */
--ds-radius-md:       8px;
--ds-radius-lg:       12px;
--ds-shadow:          0 1px 3px 0 rgba(0,0,0,0.1);
```

---

## 3. LAYOUT TỔNG THỂ & MOCKUP

```
┌────────────────────────────────────────────────────────┐
│ [Logo] MMO Market Kênh Người Bán               [Shop]  │
├────────────────────────────────────────────────────────┤
│  ┌───────────┐  ┌────────────────────────────────────┐ │
│  │  Sidebar  │  │  ĐĂNG SẢN PHẨM MỚI                 │ │
│  │  - Dashbd │  │  Tên sản phẩm: [ Netflix Premium]  │ │
│  │  - Kho    │  │  Danh mục:     [ ACCOUNT       ] ▼ │ │
│  │  - Orders │  ├────────────────────────────────────┤ │
│  │  - Rúttiền│  │  BIẾN THỂ VÀ KHO HÀNG              │ │
│  └───────────┘  │  Phân loại 1: [ 1 Tháng ] Giá:[50K]│ │
│                 │  Nhập mã code bàn giao (dòng/mã):  │ │
│                 │  ┌──────────────────────────────┐  │ │
│                 │  │ acc1@gmail.com|pass1         │  │ │
│                 │  └──────────────────────────────┘  │ │
│                 │  [ ĐĂNG SẢN PHẨM ]                 │ │
│                 └────────────────────────────────────┘ │
└────────────────────────────────────────────────────────┘
```

---

## 4. CÁC THÀNH PHẦN GIAO DIỆN CHÍNH

### 4.1 Bộ Thiết Lập Biến Thể Động — `.sform-variant-builder`
* Cho phép người bán định cấu hình tối đa 2 nhóm thuộc tính phân loại (ví dụ: Dung lượng, Thời hạn).
* Người dùng nhấp chọn "Thêm phân loại" -> Chèn thêm động dòng thông tin gồm: Tên phân loại, Giá bán (đơn vị VNĐ), và vùng dán mã tài sản số tương ứng. Có nút icon hình thùng rác màu đỏ để xóa phân loại đó.

### 4.2 Ô Nhập Mã Code Kỹ Thuật Số — `.sform-asset-textarea`
* Khung nhập văn bản lớn: `font-family: monospace`, `height: 150px`.
* Hướng dẫn trực quan: "Nhập mỗi dòng một mã thẻ. Số lượng tồn kho sẽ tự động tính bằng tổng số dòng mã bạn dán vào."
* *Bảo mật:* Danh sách mã này được gửi thô qua giao thức HTTPS bảo mật lên backend, tại đây hệ thống thực hiện mã hóa AES-256 trước khi ghi vào database để chống rò rỉ.

---

## 5. LUỒNG XỬ LÝ JS & AJAX

1. **Khởi tạo chế độ Edit (Chỉ áp dụng với Route Edit):**
   * Đọc `productId` từ URL.
   * Gửi API lấy thông tin cũ của sản phẩm:
     * **Endpoint:** `GET /api/v1/seller/products/{productId}`
     * **Headers:** `Authorization: Bearer <token>`
   * **Thành công (HTTP 200):** Kết xuất dữ liệu cũ vào các ô input và tạo sẵn các dòng biến thể cũ.
2. **Gửi form Đăng/Sửa sản phẩm:**
   * Validate dữ liệu đầu vào: Tiêu đề không để trống, Giá bán phải là số nguyên lớn `> 0`.
   * Gom dữ liệu từ các dòng biến thể động thành cấu trúc JSON:
     ```json
     {
       "name": "Netflix Premium Accounts",
       "description": "Premium 4K UltraHD",
       "category": "ACCOUNT",
       "variants": [
         {
           "name": "1 Tháng",
           "price": 50000,
           "rawAssets": ["acc1|pass1", "acc2|pass2"]
         }
       ]
     }
     ```
   * Gửi API:
     * **Mới:** `POST /api/v1/seller/products`
     * **Sửa:** `PUT /api/v1/seller/products/{productId}`
     * **Headers:** `Content-Type: application/json`, `Authorization: Bearer <token>`
   * **Thành công (HTTP 200):**
     * Hiển thị Toast thông báo đăng/sửa thành công.
     * Chuyển hướng người dùng quay lại trang quản lý kho hàng `/seller/inventory` sau 1.5 giây.

---

## 6. RESPONSIVE

* Form thiết kế dạng cột dọc đơn trên màn hình di động, mở rộng padding và khoảng cách dòng để tránh việc nhập nhầm dữ liệu trên màn hình cảm ứng bé.

---

## 7. ACCESSIBILITY

- Sử dụng nhãn ẩn `aria-label` cho các nút xóa dòng biến thể động.
- Group thông tin biến thể được bọc trong thẻ `<fieldset>` có `<legend>` để tăng cấu trúc ngữ nghĩa cho trình đọc màn hình.

---

## 8. OUT OF SCOPE

- ❌ Tải file Excel chứa hàng nghìn mã code trực tiếp (Chỉ hỗ trợ copy-paste text vào khung nhập liệu).