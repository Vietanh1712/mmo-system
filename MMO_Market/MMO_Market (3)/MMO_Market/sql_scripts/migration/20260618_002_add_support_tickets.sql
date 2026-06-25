-- ==============================================================================
-- MIGRATION: ADD SUPPORT TICKETS TABLE (SQL SERVER)
-- Tên tệp: 20260618_001_add_support_tickets.sql
-- Mô tả: Khởi tạo bảng SupportTickets phân tách với bảng Complaints
-- ==============================================================================
USE MMO_System_Schema;
GO

IF OBJECT_ID('SupportTickets', 'U') IS NOT NULL DROP TABLE SupportTickets;
GO

CREATE TABLE SupportTickets (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    category NVARCHAR(100) NOT NULL, -- Lỗi nạp tiền, Lỗi tài khoản, Góp ý, Khác
    title NVARCHAR(255) NOT NULL,
    description NVARCHAR(MAX) NOT NULL,
    status VARCHAR(20) DEFAULT 'Open', -- Open, Processing, Resolved, Closed
    resolution NVARCHAR(MAX) NULL,
    created_at DATETIME DEFAULT GETDATE(),
    isDelete BIT DEFAULT 0,
    CONSTRAINT FK_SupportTickets_Users FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE NO ACTION
);
GO

CREATE INDEX idx_support_tickets_user ON SupportTickets(user_id);
GO
