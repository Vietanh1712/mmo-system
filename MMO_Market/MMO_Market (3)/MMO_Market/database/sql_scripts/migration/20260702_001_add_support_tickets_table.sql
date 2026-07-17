-- Migration: 20260702_001_add_support_tickets_table
-- Description: Create SupportTickets table if it does not exist to support customer and seller help requests
-- ==============================================================================
USE MMO_System_Schema;
GO

IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[SupportTickets]') AND type in (N'U'))
BEGIN
    CREATE TABLE [dbo].[SupportTickets] (
        [id] BIGINT IDENTITY(1,1) PRIMARY KEY,
        [user_id] BIGINT NOT NULL,
        [category] NVARCHAR(100) NOT NULL, -- Lỗi nạp tiền, Lỗi tài khoản, Góp ý, Khác
        [title] NVARCHAR(255) NOT NULL,
        [description] NVARCHAR(MAX) NOT NULL,
        [status] VARCHAR(20) DEFAULT 'Open', -- Open, Processing, Resolved, Closed
        [resolution] NVARCHAR(MAX) NULL,
        [created_at] DATETIME DEFAULT GETDATE(),
        [isDelete] BIT DEFAULT 0,
        CONSTRAINT [FK_SupportTickets_Users] FOREIGN KEY ([user_id]) REFERENCES [dbo].[Users]([id]) ON DELETE NO ACTION
    );
    
    CREATE INDEX [idx_support_tickets_user] ON [dbo].[SupportTickets]([user_id]);
    PRINT 'Created SupportTickets table and its index successfully.';
END
ELSE
BEGIN
    PRINT 'SupportTickets table already exists in database.';
END
GO
