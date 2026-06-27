# TASKS — Direct Messaging, Block & Mute (`feat-chat`)

> **Feature ID:** `feat-chat` | **UC Coverage:** UC-12 (Direct Chat)
> **Phiên bản:** 1.0 | **Cập nhật:** 2026-06-27

---

## Phase 1: Database & Entities

- [x] **1.1** Tạo lập các bảng `Chats`, `ChatBlocks` và `ChatMutes`.

## Phase 2: Repositories

- [x] **2.1** `ChatRepository` — lấy lịch sử nhắn tin phân trang giữa hai người dùng.

## Phase 4: Business Logic (Services)

- [x] **4.1** Kiểm tra trạng thái block giữa người gửi và người nhận trước khi lưu tin nhắn.

## Phase 5: Controllers & Security

- [x] **5.1** `ChatController` — API nhắn tin và cấu hình block/mute `/api/v1/chats/**`.