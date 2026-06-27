# SPEC — Seller Inventory Management
> **Route:** `/seller/inventory` | **Template:** `templates/seller/inventory.html`
> **JS Script:** `static/js/seller-console.js` | **Prefix:** `sinv-`

---

## 1. MÔ TẢ TRANG
Cổng quản trị danh sách kho hàng dành cho Seller. Seller có thể xem danh mục sản phẩm, theo dõi số lượng tồn kho còn lại của các biến thể, chỉnh sửa nhanh thông tin giá bán và thực hiện xóa sản phẩm (soft-delete).

---

## 2. MOCKUP GIAO DIỆN (ASCII)
```
┌────────────────────────────────────────────────────────┐
│  MMO Seller Console > Kho hàng                         │
├────────────────────────────────────────────────────────┤
│  [ 🔍 Tìm kiếm sản phẩm... ]       [ ➕ Đăng Sản Phẩm ] │
│                                                        │
│  ┌──────────────────────────────────────────────────┐  │
│  │ Netflix Premium Accounts                         │  │
│  │ Giá: 50.000đ | Tồn kho: 24 sản phẩm              │  │
│  │ ┌──────────────────────────────────────────────┐ │  │
│  │ │ - 1 Month: 10 thẻ | - 3 Months: 14 thẻ       │ │  │
│  │ └──────────────────────────────────────────────┘ │  │
│  │                  [ Sửa ]   [ Xóa Sản Phẩm ]      │  │
│  └──────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────┘
```

---

## 3. STATE VÀ ĐẦU VÀO VALIDATION
```javascript
let productsList = [];
let deleteTargetId = null;
let currentPage = 0;
```

---

## 4. LUỒNG XỬ LÝ SỰ KIỆN CHI TIẾT
1. Lấy danh sách sản phẩm bằng `GET /api/seller/products?page=0`.
2. Duyệt qua danh sách, tạo HTML row động chèn vào thẻ `#inventoryTable`.
3. Bấm "Xóa Sản Phẩm":
   * Mở modal xác nhận xóa: "Bạn có chắc chắn muốn xóa sản phẩm này?".
   * Xác nhận -> Gửi request `DELETE /api/seller/products/{id}`.
   * Cập nhật trạng thái sản phẩm sang ẩn (`isDelete = 1`).