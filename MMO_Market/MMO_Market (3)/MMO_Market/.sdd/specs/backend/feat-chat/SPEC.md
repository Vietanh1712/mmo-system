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
    message NVARCHAR(MAX) NOT NULL,
    is_read BIT DEFAULT 0,
    created_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_Chat_Sender FOREIGN KEY(sender_id) REFERENCES Users(id),
    CONSTRAINT FK_Chat_Receiver FOREIGN KEY(receiver_id) REFERENCES Users(id)
);

CREATE TABLE ChatBlocks (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    blocked_user_id BIGINT NOT NULL,
    created_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_CB_User FOREIGN KEY(user_id) REFERENCES Users(id),
    CONSTRAINT FK_CB_Blocked FOREIGN KEY(blocked_user_id) REFERENCES Users(id)
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

### `GET /api/v1/chats`
*   **Response (200 OK):** Trả về danh sách cuộc hội thoại.