# PLAN — Seller Console Dashboard (`feat-seller`)

## 1. Context & Objectives
Triển khai giao diện Seller Console cho người bán bao gồm quản lý kho hàng, sản phẩm, biến thể, giao dịch, khiếu nại và cấu hình thông tin gian hàng.

## 2. Dynamic Component & Templates Mapping
- **Cấu hình Shop (`/seller/shop-info`)**:
  * View: `templates/seller/shop-info.html`
  * Script: `static/js/seller/seller-shop-settings.js`
  * Hiển thị trạng thái hoạt động của Shop (Huy hiệu màu sắc, mô tả).
  * Hiển thị khung cảnh báo và đồng hồ đếm ngược thời gian thực (`sellerSuspendedAlert`) khi Shop ở trạng thái `Tạm ngưng` (`Suspended`).
  * Tự động tải lại trang và khôi phục trạng thái hoạt động khi hết hạn tạm ngưng.
  * Ẩn nút thủ công "Tạm đóng cửa hàng" trên giao diện Seller.
  * Việt hóa 100% nhãn hiển thị trạng thái Shop (Hoạt động, Tạm ngưng, Tạm khóa, Khóa vĩnh viễn, Đã đóng Shop, Chờ duyệt) ở Sidebar và trang Thông tin cá nhân (`/profile`).

## 3. Lịch sử thay đổi (Change Log)
- **Khóa đăng bán sản phẩm khi Shop không hoạt động**:
  - Tại trang Đăng bán sản phẩm (`/seller/products/new`), tích hợp kiểm tra trạng thái hoạt động của Shop thông qua API `/api/seller/shop-info`.
  - Nếu trạng thái là `Suspended`, `Locked`, `Banned` hoặc `Withdrawn`, hiển thị thông báo lỗi màu đỏ ở đầu trang và vô hiệu hóa (`disabled`) toàn bộ các trường nhập liệu, nút thêm biến thể cũng như nút đăng sản phẩm.

## 4. Verification Plan
- Truy cập `/seller/shop-info` và xác nhận trạng thái được lấy chính xác từ API `/api/v1/seller/shop-info`.
- Khi Staff tạm ngưng Shop kèm mốc thời gian, xác nhận thẻ đếm ngược màu vàng hiển thị thời gian chạy thực tế theo từng giây.
- Đổi trạng thái Shop sang `Suspended` rồi truy cập trang Đăng bán sản phẩm, xác nhận có thông báo shop bị tạm ngưng và không thể điền các trường thông tin hay bấm đăng bán.