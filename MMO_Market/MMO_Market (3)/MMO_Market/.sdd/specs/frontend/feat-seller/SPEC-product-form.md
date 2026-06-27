# SPEC — Seller Product Add & Edit Form
> **Routes:** `/seller/products/new`, `/seller/products/edit`
> **Templates:** `templates/seller/product-add.html`, `templates/seller/product-edit.html`
> **JS Script:** `static/js/seller-console.js`

---

## 1. MÔ TẢ TRANG
Biểu mẫu cho phép Seller tạo mới sản phẩm, thêm các biến thể khác nhau (như thời hạn 1 tháng, 3 tháng) và dán danh sách mã code tài sản số.

---

## 2. MOCKUP GIAO DIỆN (ASCII)
```
┌────────────────────────────────────────────────────────┐
│  MMO Seller Console > Đăng sản phẩm mới                │
├────────────────────────────────────────────────────────┤
│  Tên sản phẩm *:  [ Netflix Premium Tài Khoản ]         │
│  Danh mục *:      [ Tài khoản xem phim       ] (dropdown)│
│  Giá bán (VNĐ) *: [ 50000                     ]        │
│                                                        │
│  Nhập mã code số (Mỗi dòng một code):                  │
│  ┌──────────────────────────────────────────────────┐  │
│  │ acc1@gmail.com|pass123                           │  │
│  │ acc2@gmail.com|pass456                           │  │
│  └──────────────────────────────────────────────────┘  │
│                                                        │
│                   [ Đăng Sản Phẩm ]                    │
└────────────────────────────────────────────────────────┘
```

---

## 3. LOGIC MÃ HÓA TÀI SẢN SỐ CLIENT-SIDE
1. Khi click "Đăng Sản Phẩm", JS đọc nội dung ô nhập mã code.
2. Kiểm tra tính hợp lệ (không để trống).
3. Sử dụng thư viện mã hóa đối xứng AES-256 mã hóa danh sách code trước khi đóng gói JSON:
   ```javascript
   const encryptedCode = CryptoJS.AES.encrypt(rawCodes, secretKey).toString();
   ```
4. Gửi payload lên `POST /api/seller/products`.