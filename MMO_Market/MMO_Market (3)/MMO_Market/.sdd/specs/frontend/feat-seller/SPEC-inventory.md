# SPEC — Quản Lý Kho Hàng Người Bán (Seller Inventory Management)

> **Feature ID:** `feat-seller` | **Page:** `SellerInventory`
> **Route:** `/seller/inventory` | **Template:** `templates/seller/inventory.html`
> **CSS Script:** `static/css/customer/style.css`
> **JS Script:** `static/js/seller-inventory.js`
> **Version:** 1.0 | **Status:** Draft
> **Backend ref:** `feat-product/UC-05-product-discovery.md`

---

## 1. TỔNG QUAN TRANG

Trang Quản lý kho hàng cho phép người bán (Seller) xem danh sách toàn bộ các sản phẩm đã đăng, theo dõi số lượng tồn kho thực tế của từng phân loại biến thể, điều chỉnh giá bán và thực hiện xóa mềm (Soft delete) sản phẩm vi phạm hoặc dừng kinh doanh.

**Cấu trúc trang:**
1. **Sidebar điều hướng Kênh Người Bán:** Dashboard, Quản lý kho hàng (Inventory), Lịch sử đơn hàng bán, Yêu cầu rút tiền (Withdrawals).
2. **Khối bộ lọc tìm kiếm (Search & Filters):** Tìm kiếm theo tên sản phẩm, bộ lọc danh mục sản phẩm và nút "Đăng sản phẩm mới".
3. **Bảng danh sách sản phẩm (Inventory Table):**
   * Hiển thị thông tin tổng quát sản phẩm, giá bán, tổng tồn kho và trạng thái.
   * Tích hợp menu thả xuống hiển thị chi tiết các phân loại biến thể (Variants) và tồn kho tương ứng của mỗi loại.

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
--ds-warning:         #f59e0b;

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
│  │  Sidebar  │  │  QUẢN LÝ KHO HÀNG                  │ │
│  │  - Dashbd │  │  [ Tìm kiếm...]  [➕ Đăng Sản Phẩm] │ │
│  │  - Kho    │  │                                    │ │
│  │  - Orders │  │  ┌──────────────────────────────┐  │ │
│  │  - Rúttiền│  │  │ Netflix Premium Accounts     │  │ │
│  └───────────┘  │  │ Tồn kho: 24 | Giá: 50K VNĐ   │  │ │
│                 │  │ [Biến thể ▼]   [Sửa]  [Xóa]  │  │ │
│                 │  └──────────────────────────────┘  │ │
│                 └────────────────────────────────────┘ │
└────────────────────────────────────────────────────────┘
```

---

## 4. CÁC THÀNH PHẦN GIAO DIỆN CHÍNH

### 4.1 Danh Sách Thẻ Kho Hàng — `.sinv-product-row`
* Thiết kế dạng bảng hoặc dòng thẻ ngang.
* Hiển thị ảnh thumbnail sản phẩm, tiêu đề sản phẩm, khoảng giá của các biến thể, tổng tồn kho (tổng số lượng tài sản số chưa bán thuộc biến thể sản phẩm đó).
* **Nút bấm thao tác:**
  * *Nút Sửa (`.btn-edit`):* Click chuyển hướng sang màn hình `/seller/products/edit/{productId}`.
  * *Nút Xóa (`.btn-delete`):* Click kích hoạt modal xác nhận xóa mềm sản phẩm (`isDelete = 1`).

### 4.2 Bảng Chi Tiết Biến Thể Thả Xuống — `.sinv-variant-dropdown`
* Khi click vào nút "Biến thể ▼", mở rộng panel nằm ngay phía dưới dòng sản phẩm hiển thị bảng chi tiết:
  * Tên phân loại (ví dụ: "1 Tháng", "3 Tháng").
  * Đơn giá cụ thể của phân loại đó.
  * Số lượng tồn kho cụ thể của phân loại đó.

---

## 5. LUỒNG XỬ LÝ JS & AJAX

1. **Tải danh sách kho hàng:**
   * Gọi API:
     * **Endpoint:** `GET /api/v1/seller/products?page=0&size=10`
     * **Headers:** `Authorization: Bearer <token>`
   * **Thành công (HTTP 200):** Đổ dữ liệu chèn vào bảng sản phẩm và các biến thể con.
2. **Xử lý xóa mềm sản phẩm:**
   * Người bán click nút "Xóa" trên một sản phẩm.
   * Hiển thị Modal cảnh báo: "Bạn có chắc chắn muốn xóa sản phẩm này? Thao tác này không thể hoàn tác nhưng không ảnh hưởng tới các đơn hàng đã bán."
   * Người bán click xác nhận đồng ý -> Gửi API:
     * **Endpoint:** `DELETE /api/v1/seller/products/{productId}`
     * **Headers:** `Authorization: Bearer <token>`
   * **Thành công (HTTP 200):** Cập nhật ẩn dòng sản phẩm đó khỏi giao diện ngay lập tức bằng hiệu ứng Fadeout nhẹ, hiển thị Toast báo xóa thành công.

---

## 6. RESPONSIVE

* **Viewport ≥ 768px:** Layout bảng hiển thị đầy đủ thông tin chi tiết trên một dòng ngang.
* **Viewport < 768px:** Bảng chuyển đổi thành dạng danh sách thẻ dọc độc lập. Mỗi sản phẩm là một thẻ card riêng biệt, nút "Sửa" và "Xóa" hiển thị to dễ chạm bấm ở góc dưới thẻ.

---

## 7. ACCESSIBILITY

- Modal xác nhận xóa áp dụng focus mặc định vào nút "Hủy bỏ" để tránh người dùng nhấn nhanh phím Space/Enter gây xóa nhầm.
- Ô tìm kiếm sản phẩm có thuộc tính `aria-controls` trỏ vào ID của bảng danh sách kho hàng.

---

## 8. OUT OF SCOPE

- ❌ Nhập kho hàng loạt (Batch importing) từ file Excel trực tiếp tại trang danh sách (Phải thực hiện trong form chi tiết sản phẩm).