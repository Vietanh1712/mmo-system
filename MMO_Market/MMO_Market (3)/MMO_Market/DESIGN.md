# MMO Market — UI/UX Design System & Customization

Tài liệu này lưu trữ các nguyên tắc thiết kế giao diện (UI) và trải nghiệm người dùng (UX) thống nhất trên toàn bộ hệ thống MMO Market.

---

## 1. HỆ MÀU SẮC CHỦ ĐẠO (Color Palette)

* **Primary Color (Màu thương hiệu chính)**: `#ea580c` (Màu cam ấm áp, tượng trưng cho giao dịch sôi động và tin cậy).
* **Success Color (Màu thành công)**: `#10a37f` (Màu xanh lá cây hiện đại, dùng cho thông báo thành công, trạng thái khả dụng).
* **Warning Color (Màu cảnh báo)**: `#f59e0b` (Màu vàng hổ phách, dùng cho cảnh báo nhẹ, trạng thái chờ xử lý).
* **Danger/Destructive Color (Màu nguy hiểm / Đăng xuất)**: `#ef4444` (Màu đỏ tươi, dùng cho các lỗi nghiêm trọng, nút hủy, hành động xóa và đăng xuất).

---

## 2. QUY CHUẨN NỔI BẬT NÚT ĐĂNG XUẤT (Logout Button Highlight Standard)

Để nâng cao trải nghiệm người dùng và giúp hành động thoát tài khoản dễ dàng nhận diện trên mọi nền tảng (Buyer, Seller, Staff), toàn bộ các nút **Đăng xuất** phải được đồng bộ theo cấu trúc màu sắc sau:

### Màu sắc Trạng thái Bình thường (Normal State)
* **Màu chữ & Icon**: `#ef4444` (Màu đỏ nguy hiểm).
* **Font-weight**: `600` (hoặc `Bold` để tăng độ rõ nét).

### Màu sắc Trạng thái Di chuột (Hover State)
* **Màu nền (Background)**: `#fef2f2` (Màu hồng/đỏ nhạt, tạo cảm giác dịu mắt nhưng vẫn nổi bật).
* **Màu chữ & Icon**: `#dc2626` (Màu đỏ sẫm hơn).
* **Hiệu ứng chuyển đổi (Transition)**: `background-color 0.2s, color 0.2s` (Chuyển đổi mượt mà).

---

## 3. CÁC LỚP ÁP DỤNG TRONG CSS (CSS Class Implementation)

### 3.1. Dropdown Menu (Header Dropdown)
Áp dụng class `.dropdown-item--logout` trong `home.css`:
```css
.dropdown-item--logout {
    margin-top: 4px;
    color: #ef4444 !important;
    transition: background-color 0.2s, color 0.2s;
}
.dropdown-item--logout i {
    color: #ef4444 !important;
}
.dropdown-item--logout:hover {
    background-color: #fef2f2 !important;
    color: #dc2626 !important;
}
.dropdown-item--logout:hover i {
    color: #dc2626 !important;
}
```

### 3.2. Sidebar links (Account, Seller, Staff sidebars)
Áp dụng các class tương ứng `.account-sidebar__menu-link--logout`, `.seller-sidebar__menu-link--logout`, `.staff-sidebar__link--logout` trong `profile.css` và `staff.css` để đồng bộ thuộc tính `!important` chặn ghi đè:
```css
.account-sidebar__menu-link--logout,
.seller-sidebar__menu-link--logout,
.staff-sidebar__link--logout {
    color: #ef4444 !important;
    cursor: pointer !important;
}
.account-sidebar__menu-link--logout:hover,
.seller-sidebar__menu-link--logout:hover,
.staff-sidebar__link--logout:hover {
    background: #fef2f2 !important;
    color: #dc2626 !important;
}

---

## 4. HỆ THỐNG THÔNG BÁO TOAST (Toast Notification System)

Toàn bộ thông báo phản hồi nhanh (toasts) phải được đồng bộ theo thiết kế dạng thẻ (card-based toast) được định nghĩa trong [toast.js](file:///c:/Users/pc/MMO_new1/MMO_Market/MMO_Market%20(3)/MMO_Market/apps/frontend/static/js/components/toast.js). Không sử dụng dạng thanh chữ nhật đơn giản cũ.

### Quy chuẩn thiết kế (Design Specification)
* **Vị trí hiển thị**: Góc dưới bên phải màn hình (`bottom: 24px; right: 24px;`).
* **Cấu trúc thẻ**:
  * Có viền trái dày `5px` với màu tương ứng theo loại thông báo:
    * `success`: Xanh lá (`#10a37f`)
    * `error` / `danger`: Đỏ (`#ef4444`)
    * `warning`: Vàng cam (`#f59e0b`)
    * `info`: Xanh dương (`#3b82f6`)
  * Màu nền thẻ: Màu nhạt dịu mắt (ví dụ: nền xanh lá nhạt `#f0fdf4` cho thông báo thành công).
  * Chứa icon Font Awesome (`fa-check-circle`, `fa-times-circle`, `fa-exclamation-triangle`, `fa-info-circle`).
  * Có tiêu đề đậm (`Thành công`, `Lỗi`, `Cảnh báo`) và nội dung mô tả chi tiết ở dưới.
  * Có nút đóng nhanh (dấu `x`) ở góc trên bên phải.
  * Hiệu ứng chuyển động (transition) xuất hiện/mất đi mượt mà với hiệu ứng trượt nhẹ (slide).
