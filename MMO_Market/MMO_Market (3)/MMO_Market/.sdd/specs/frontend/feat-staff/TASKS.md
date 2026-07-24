# TASKS — Staff Console Interface (`feat-staff`)

> **Feature ID:** `feat-staff` | **Trực thuộc:** Frontend | **UC Coverage:** UC-14, UC-03, UC-09, UC-10, UC-19

---

## Phase 1: HTML Layout & Templates (Thymeleaf templates/staff/...)

- [x] **1.1** Xây dựng trang Dashboard chính `/staff/dashboard` (`dashboard.html`) hiển thị thống kê tổng số Shop và liên kết truy cập nhanh.
- [x] **1.2** Xây dựng trang Tổng quan đơn từ `/staff/documents` (`documents-dashboard.html`) hiển thị số lượng khiếu nại, yêu cầu rút tiền, giao dịch, nạp tiền cần xử lý.
- [x] **1.3** Xây dựng trang Danh sách và Chi tiết KYC (`kyc.html` & `kyc-detail.html`) chứa stepper tiến trình và thông tin chi tiết người nộp.
- [x] **1.4** Xây dựng trang Danh sách và Chi tiết Rút tiền (`withdrawals.html` & `withdrawal-detail.html`) phục vụ quy trình giải ngân thủ công.
- [x] **1.5** Xây dựng trang Quản lý Shop (`shop-registrations.html` & `shop-registration-detail.html`) cho phép Bật/Tắt (Active/Banned) shop hoặc thiết lập Tạm ngưng (Suspended) có đếm ngược.
- [x] **1.6** Xây dựng trang Quản lý nạp tiền (`topups.html` & `topup-detail.html`) tích hợp nút "Thử lại / Kích hoạt lại" cộng tiền thủ công.
- [x] **1.7** Xây dựng trang Khiếu nại và Chi tiết khiếu nại (`complaints.html` & `complaint-detail.html`) hiển thị lý do/kết quả giải quyết.
- [x] **1.8** Xây dựng trang Kênh chat phân xử khiếu nại `/staff/chat` (`chat.html`) kết nối WebSocket.
- [x] **1.9** Xây dựng trang Support Ticket (`support-tickets.html` & `support-ticket-detail.html`) để phản hồi khách hàng.
- [x] **1.10** Xây dựng trang Giao dịch toàn sàn (`transactions.html` & `transaction-detail.html`).
- [x] **1.11** Xây dựng trang Quản lý cờ cảnh báo (`flags.html` & `flag-detail.html`) hiển thị STT và mức độ vi phạm.

## Phase 2: JS Scripts & Client Operations (static/js/staff/...)

- [x] **2.1** `staff-ui.js` — quản lý phân quyền sidebar theo API `/staff/my-permissions`, bind nút xử lý KYC và mount phân trang chung (`mountStaffPagination`).
- [x] **2.2** `staff-kyc.js` & `staff-kyc-detail.js` — kết nối API KYC list/detail, hỗ trợ bộ lọc và nút "Làm mới bộ lọc".
- [x] **2.3** `staff-topups.js` — tải danh sách nạp tiền phân trang, thống kê nạp tiền, tìm kiếm mã linh hoạt (#TOPUP-1, TOPUP-1, #1, 1), và mở Modal duyệt thủ công cộng tiền (`POST /api/v1/staff/topups/{id}/retry`).
- [x] **2.4** `staff-shop-registrations.js` & `staff-shop-registration-detail.js` — gọi API quản lý shop, toggle trạng thái Active/Banned, mở modal thiết lập thời hạn Tạm ngưng (`suspendShopModal`), và hiển thị live-countdown đếm ngược tự động kích hoạt lại shop.
- [x] **2.5** `staff-complaints.js` & `staff-complaint-detail.js` — gọi API danh sách/chi tiết khiếu nại, lấy danh sách trạng thái động từ database `/api/complaints/statuses`.
- [x] **2.6** `staff-chat.js` — kết nối WebSocket giải quyết tranh chấp.
- [x] **2.7** `staff-support-tickets.js` & `staff-support-ticket-detail.js` — phản hồi support ticket cho khách hàng.
- [x] **2.8** Tích hợp Form POST truyền thống cho duyệt/từ chối rút tiền trên `withdrawals.html` và `withdrawal-detail.html` thông qua `/staff/withdrawals/update-status` (gửi ảnh biên lai `proofFile`) và `/staff/withdrawals/reject`.

## Phase 3: Testing & Verification

- [ ] **3.1** Kiểm thử tính đúng đắn của logic phân quyền link sidebar theo `myPermissions` (Staff/Admin không có quyền tương ứng phải bị vô hiệu hóa click).
- [ ] **3.2** Kiểm thử bộ đếm ngược thời gian thực (Real-time countdown timer) khi shop ở trạng thái Tạm ngưng hoạt động đúng giây và tự động reload để khôi phục trạng thái.
- [ ] **3.3** Kiểm thử tính đồng nhất số liệu đếm thống kê đỉnh trang và số dòng thực tế ở các trang: KYC, Rút tiền, Quản lý Shop, Nạp tiền, Khiếu nại.