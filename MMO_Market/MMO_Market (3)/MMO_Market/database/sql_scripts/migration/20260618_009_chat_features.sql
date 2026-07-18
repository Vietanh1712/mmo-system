USE MMO_System_Schema;
GO

-- Migration: Add chat delete/block/mute features
-- Date: 2026-06-18

USE MMO_Market_Schema;
GO


IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('Chats') AND name = 'sender_deleted')
BEGIN
    ALTER TABLE Chats ADD sender_deleted BIT DEFAULT 0;
END
GO

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('Chats') AND name = 'receiver_deleted')
BEGIN
    ALTER TABLE Chats ADD receiver_deleted BIT DEFAULT 0;
END
GO

-- Create ChatBlocks table
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID('ChatBlocks') AND type = 'U')
BEGIN
    CREATE TABLE ChatBlocks (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        blocker_id BIGINT NOT NULL,
        blocked_id BIGINT NOT NULL,
        created_at DATETIME DEFAULT GETDATE(),
        CONSTRAINT FK_ChatBlocks_Blocker FOREIGN KEY (blocker_id) REFERENCES Users(id) ON DELETE NO ACTION,
        CONSTRAINT FK_ChatBlocks_Blocked FOREIGN KEY (blocked_id) REFERENCES Users(id) ON DELETE NO ACTION,
        CONSTRAINT UQ_ChatBlocks UNIQUE (blocker_id, blocked_id)
    );
END
GO

-- Create ChatMutes table
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID('ChatMutes') AND type = 'U')
BEGIN
    CREATE TABLE ChatMutes (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        user_id BIGINT NOT NULL,
        contact_id BIGINT NOT NULL,
        created_at DATETIME DEFAULT GETDATE(),
        CONSTRAINT FK_ChatMutes_User FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE NO ACTION,
        CONSTRAINT FK_ChatMutes_Contact FOREIGN KEY (contact_id) REFERENCES Users(id) ON DELETE NO ACTION,
        CONSTRAINT UQ_ChatMutes UNIQUE (user_id, contact_id)
    );
END
GO

