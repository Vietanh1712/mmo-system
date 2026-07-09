USE MMO_System_Schema;
GO

-- Migration: Add product_id context column to Chats table
-- Date: 2026-07-05
-- Purpose: Store product context when customer initiates chat from product page

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('Chats') AND name = 'product_id')
BEGIN
    ALTER TABLE Chats ADD product_id BIGINT NULL;
    PRINT 'Column product_id added to Chats table.';
END
ELSE
BEGIN
    PRINT 'Column product_id already exists in Chats table.';
END
GO

