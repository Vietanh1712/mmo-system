# TASKS — Admin Console Interface (`feat-admin`)

> **Feature ID:** `feat-admin`
> **Phiên bản:** 2.0 | **Cập nhật:** 2026-07-16

---

## Phase 1: HTML Layout & Sidebar Structure

- [x] **1.1** Xây dựng trang HTML quản trị duy nhất `users.html` kế thừa header và footer hệ thống.
- [x] **1.2** Dựng cấu trúc Sidebar điều hướng phân vùng động với 3 nhóm chính: Tổng quan, Quản lý tài khoản, Cấu hình hệ thống.
- [x] **1.3** Triển khai phân chia 8 view panel ẩn/hiện động sử dụng Javascript thông qua ID tương ứng (`dashboardView`, `audit-logsView`, `revenueView`, `accountFormView`, `system-configView`, `commissionsView`, `notificationsView`, `accountsView`, `permissionsView`).

## Phase 2: CSS & Styling System

- [x] **2.1** Tích hợp thiết kế hệ thống (`design-system-styles`) cho các thẻ thống kê (`.stat-card-modern`), bộ lọc panel (`.ds-filter-panel`), bảng dữ liệu (`.ds-table`), và modal thông báo (`.ds-modal-backdrop`).
- [x] **2.2** Định dạng màu sắc trực quan cho các badge vai trò (Admin/Staff/Seller/Customer) và badge hoạt động (Active/Locked).

## Phase 3: JavaScript Views Management & AJAX Bindings

- [x] **3.1** Viết kịch bản điều hướng `switchAdminView(viewId)` lưu trạng thái tab đang xem.
- [x] **3.2** **Bảng điều khiển (Dashboard panel):** Nạp số liệu thống kê tài khoản, vẽ đường line và area biểu đồ SVG doanh thu tuần tự động, hiển thị tooltip tiền tệ khi di chuột.
- [x] **3.3** **Nhật ký hệ thống (Audit Logs panel):** Nạp dữ liệu log phân trang, lọc theo loại hành động, click mở modal xem chi tiết JSON payload và xuất file CSV.
- [x] **3.4** **Doanh thu & Dòng tiền (Revenue panel):** Hiển thị số liệu 4 nguồn doanh thu ròng, tìm kiếm giao dịch dòng tiền và xuất file CSV.
- [x] **3.5** **Quản lý tài khoản (Accounts & Staff CRUD panel):** Nạp danh sách thành viên phân trang, nút toggle lock tài khoản hoạt động động, và form CRUD thông tin nhân viên Staff.
- [x] **3.6** **Phân quyền nhân viên (Permissions panel):** Dựng custom dropdown multiselect lọc theo nhóm quyền, tìm kiếm quyền, tìm kiếm nhân viên, gán quyền hàng loạt và thu hồi nhanh tag quyền.
- [x] **3.7** **Cấu hình & Biểu phí (Config panels):** Điền dữ liệu cấu hình mặc định lên form, trượt toggle switch thay đổi trạng thái, validate dữ liệu đầu vào và gửi PUT cập nhật cấu hình.
- [x] **3.8** **Quản trị thông báo & Bảo trì (Notifications panel):** Mở modal soạn thảo thông báo hệ thống, toggle nhanh trạng thái bảo trì, hiển thị banner đỏ cảnh báo bảo trì đầu trang và soft delete thông báo đã phát.

## Phase 4: Verification & Specification Docs

- [x] **4.1** Kiểm thử tính an toàn bảo mật: Đảm bảo các REST API đều được chặn quyền quản trị chính xác.
- [x] **4.2** Viết 7 tệp tin đặc tả frontend chi tiết mô tả đầy đủ mockup ASCII, design tokens, các khối DOM chính và luồng xử lý AJAX tương ứng với từng view panel phụ.