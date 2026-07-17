# PLAN — Admin Console Interface (`feat-admin`)

## Templates
- `apps/frontend/templates/admin/users.html` — Màn hình quản trị cấp cao duy nhất (Single Page Console) của Admin.

## JS Scripts
- `apps/frontend/static/js/admin-console.js` — Kịch bản điều khiển giao diện chính của Admin Console, quản lý chuyển đổi view panel và liên kết AJAX với các REST API.

## Changelog
| Version | Date | Mô tả |
|---|---|---|
| 1.0 | 2026-06-27 | Khởi tạo giao diện Admin Console cơ bản, tích hợp danh sách tài khoản và nút khóa/mở khóa tài khoản. |
| 1.1 | 2026-06-28 | Thiết kế form thêm mới nhân viên Staff và tích hợp kiểm tra dữ liệu đầu vào phía client. |
| 1.2 | 2026-06-29 | Dựng phân vùng Doanh thu & Dòng tiền, kết nối API thống kê tổng quan doanh thu từ đơn hàng. |
| 1.3 | 2026-06-30 | Bổ sung phân vùng Audit Logs hiển thị lịch sử thao tác của Admin/Staff phục vụ kiểm toán bảo mật. |
| 1.4 | 2026-07-01 | Tích hợp tính năng xuất báo cáo Excel/CSV cho cả hai phân vùng Doanh thu và Audit Logs. |
| 1.5 | 2026-07-02 | Việt hóa các nhãn hiển thị vai trò tài khoản (Admin, Staff, Seller, Customer) và trạng thái hoạt động. |
| 1.6 | 2026-07-03 | Tối ưu hóa component phân trang chung, sửa lỗi nhảy trang và đồng bộ hiển thị tổng số bản ghi cho các bảng. |
| 1.7 | 2026-07-04 | Cải tiến bộ lọc tìm kiếm tài khoản theo email, số điện thoại, họ tên và lọc theo vai trò người dùng. |
| 1.8 | 2026-07-05 | Phát triển custom multiselect dropdown tìm kiếm nhanh cho việc chọn nhiều quyền và chọn nhân viên Staff trong phân vùng Permissions. |
| 1.9 | 2026-07-06 | Thêm khung hiển thị mô tả chi tiết quyền hạn `#selectedPermDescBox` hỗ trợ Admin khi thực hiện phân quyền Staff. |
| 1.10 | 2026-07-08 | Tích hợp vẽ đồ họa biểu đồ doanh thu tuần tự động bằng thẻ SVG dựa trên số liệu thực tế từ REST API. |
| 1.11 | 2026-07-10 | Phát triển Tooltip động `#chartTooltip` hiển thị số tiền định dạng VNĐ khi hover chuột qua các chấm tròn trên biểu đồ SVG. |
| 1.12 | 2026-07-12 | Thiết kế phân vùng Cấu hình hệ thống chung (Session timeout, OTP timeout, Max login retries, Google login toggle switch). |
| 1.13 | 2026-07-14 | Thiết kế phân vùng Phí & Hoa hồng (Tỷ lệ hoa hồng C2C, phí rút tiền %, phí mở shop và các hạn mức giao dịch ví). |
| 1.14 | 2026-07-15 | Cập nhật logic validate biểu phí và hoa hồng, giới hạn tỷ lệ phần trăm từ 0% đến 100% trước khi cho phép gửi API. |
| 2.0 | 2026-07-16 | Phân tách giao diện Quản lý thông báo và bảo trì toàn sàn thành phân vùng riêng. Bổ sung banner cảnh báo màu đỏ hiển thị trạng thái bảo trì hệ thống và nút tắt nhanh. Hoàn thiện 7 file đặc tả frontend chi tiết cho từng phân vùng. |