# TASKS — Direct Messaging, Block & Mute (`feat-chat`)

> **Feature ID:** `feat-chat` | **UC Coverage:** UC-12 (Direct Chat)
> **Phiên bản:** 1.0 | **Cập nhật:** 2026-06-27

---

## Phase 1: Database & Entities

- [x] **1.1** Tạo lập các bảng `Chats` và `ChatMutes`.

## Phase 2: Repositories

- [x] **2.1** `ChatRepository` — lấy lịch sử nhắn tin phân trang giữa hai người dùng.

## Phase 4: Business Logic (Services)

- [x] **4.1** `UserStatusService` — quản lý trạng thái online/offline của người dùng.

## Phase 5: Controllers & Security

- [x] **5.1** `ChatController` — các REST API endpoints `/api/v1/chats` để gửi, nhận và lấy lịch sử tin nhắn của Người dùng qua cơ chế Polling.
- [x] **5.2** `ChatController` — tích hợp kiểm soát logic tắt thông báo (mute) và xóa lịch sử mềm phía client.
- [x] **5.3** `ChatController` — xử lý logic tạo/tham gia phòng chat đối chất tranh chấp cho khách hàng & người bán khi khiếu nại đang đối chất (`contactId < 0`).
- [x] **5.4** `StaffChatRestController` — các REST API endpoints `/api/v1/staff/chat` dành riêng cho Staff quản lý hội thoại và đọc (Read-only) phòng đối chất.