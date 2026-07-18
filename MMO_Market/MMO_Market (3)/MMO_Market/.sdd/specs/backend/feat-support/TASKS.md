# TASKS — Support Ticket Management (Phiếu hỗ trợ) (`feat-support`)

> **Feature ID:** `feat-support`
> **Phiên bản:** 1.0 | **Cập nhật:** 2026-06-27

---

## Phase 1: Database & Entities

- [x] **1.1** Khởi tạo bảng `SupportTickets` lưu trữ yêu cầu hỗ trợ.

## Phase 2: Repositories

- [x] **2.1** `SupportTicketRepository` — truy vấn danh sách yêu cầu hỗ trợ.

## Phase 4: Business Logic (Services)

- [x] **4.1** `SupportTicketService.createTicket()` — ghi nhận yêu cầu hỗ trợ kỹ thuật ở trạng thái `Open`.

## Phase 5: Controllers & Security

- [x] **5.1** `SupportTicketController` — API gửi phiếu hỗ trợ `/api/support-tickets`.