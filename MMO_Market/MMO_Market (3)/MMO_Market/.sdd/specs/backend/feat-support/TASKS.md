# TASKS — Support Ticket Management (`feat-support`)

> **Feature ID:** `feat-support`
> **Phiên bản:** 1.0 | **Cập nhật:** 2026-06-27

---

## Phase 1: Database & Entities

- [x] **1.1** Khởi tạo bảng `SupportTickets` lưu trữ yêu cầu hỗ trợ.

## Phase 2: Repositories

- [x] **2.1** `SupportTicketRepository` — truy vấn danh sách yêu cầu hỗ trợ.

## Phase 4: Business Logic (Services)

- [x] **4.1** `SupportTicketService.createTicket()` — ghi nhận yêu cầu hỗ trợ kỹ thuật ở trạng thái `Open`.
- [x] **4.2** `SupportTicketService` — tích hợp gửi thông báo hệ thống tự động cho người dùng và các tài khoản Staff/Admin.
- [x] **4.3** `SupportTicketService.updateTicketStatus()` — cập nhật trạng thái (`Open`, `Processing`, `Resolved`) và giải pháp phản hồi.

## Phase 5: Controllers & Security

- [x] **5.1** `SupportTicketController` — API gửi ticket hỗ trợ `/api/support-tickets` và API lấy danh sách cá nhân.
- [x] **5.2** `SupportTicketController.getAllTickets()` — API lấy danh sách tất cả các ticket cho Staff/Admin tại `/api/support-tickets/all`.
- [x] **5.3** `SupportTicketController.getTicketById()` — kiểm tra phân quyền sở hữu tránh lỗ hổng bảo mật IDOR.
- [x] **5.4** `SupportTicketController.updateTicketStatus()` — API cập nhật trạng thái tại `/api/support-tickets/{id}/status` dành riêng cho Staff/Admin.