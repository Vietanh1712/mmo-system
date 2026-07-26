# SPEC — Direct Messaging, Block & Mute
> **Feature ID:** `feat-chat`
> **UC Coverage:** UC-12 (Direct Chat)
> **Version:** 1.0 | **Status:** Active
> **Author:** Team | **Last Updated:** 2026-06-27

---

## 1. CONTEXT & GOAL (BỐI CẢNH & MỤC TIÊU)
Cung cấp khả năng trao đổi thông tin trực tiếp giữa người mua và người bán để giải quyết giao dịch, hỗ trợ tính năng chặn/tắt thông báo.

---

## 5. DATA MODEL (Mô hình dữ liệu)

```sql
CREATE TABLE Chats (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    sender_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    complaint_id BIGINT NULL,
    product_id BIGINT NULL,
    chat_type VARCHAR(50) DEFAULT 'Normal', -- Normal, Complaint
    message NVARCHAR(MAX) NOT NULL,
    is_read BIT DEFAULT 0,
    sender_deleted BIT DEFAULT 0,
    receiver_deleted BIT DEFAULT 0,
    created_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_Chat_Sender FOREIGN KEY(sender_id) REFERENCES Users(id),
    CONSTRAINT FK_Chat_Receiver FOREIGN KEY(receiver_id) REFERENCES Users(id),
    CONSTRAINT FK_Chat_Complaint FOREIGN KEY(complaint_id) REFERENCES Complaints(id)
);



CREATE TABLE ChatMutes (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    muted_user_id BIGINT NOT NULL,
    created_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_CM_User FOREIGN KEY(user_id) REFERENCES Users(id),
    CONSTRAINT FK_CM_Muted FOREIGN KEY(muted_user_id) REFERENCES Users(id)
);
```

---

## 6. API SPEC (Đặc tả API)

### 6.1 API dành cho Người Dùng (Customer & Seller - `/api/v1/chats`)

*   **`GET /api/v1/chats`**: Lấy danh bạ chat gần đây của người dùng (tự động gộp nhóm và lồng ghép phòng chat đối chất đang hoạt động nếu có).
*   **`GET /api/v1/chats/contact/{contactId}/info`**: Lấy thông tin liên hệ cụ thể (hỗ trợ cả liên hệ dạng tranh chấp nếu `contactId < 0`).
*   **`GET /api/v1/chats/{contactId}`**: Lấy lịch sử nhắn tin. Đánh dấu đã đọc các tin nhắn nhận được.
*   **`POST /api/v1/chats/{contactId}`**: Gửi tin nhắn mới.
    *   *Ràng buộc:* Không thể tự gửi cho chính mình. Không thể gửi vào phòng đối chất (`contactId < 0`) nếu khiếu nại đã đóng.
*   **`DELETE /api/v1/chats/{contactId}/history`**: Xóa lịch sử chat phía mình (đặt cờ `senderDeleted` hoặc `receiverDeleted`).
*   **`POST /api/v1/chats/{contactId}/mute`** / **`POST /api/v1/chats/{contactId}/unmute`**: Tắt hoặc bật thông báo tin nhắn.
*   **`GET /api/v1/chats/{contactId}/search`**: Tìm kiếm tin nhắn theo từ khóa trong cuộc trò chuyện.

### 6.2 API dành cho Nhân viên (Staff & Admin - `/api/v1/staff/chat`)

*   **`GET /api/v1/staff/chat/conversations`**: Lấy danh sách hội thoại của Staff (gồm hội thoại thông thường và phòng chat đối chất).
*   **`GET /api/v1/staff/chat/search`**: Tìm kiếm người dùng hệ thống để chủ động chat hỗ trợ.
*   **`GET /api/v1/staff/chat/{targetUserId}`**: Lấy lịch sử chat.
*   **`POST /api/v1/staff/chat/{targetUserId}`**: Gửi tin nhắn đến người dùng.
    *   *Ràng buộc:* Staff có quyền Read-only đối với phòng chat đối chất (`targetUserId < 0`), không được phép gửi tin nhắn vào phòng này.