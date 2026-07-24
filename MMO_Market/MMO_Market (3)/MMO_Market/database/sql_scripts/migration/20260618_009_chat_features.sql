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



