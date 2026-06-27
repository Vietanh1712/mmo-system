-- Migration: Add transaction_id to Reviews table
-- Purpose: Link each review to a specific transaction so users can review
--          the same product multiple times if they have different purchase orders.
-- Date: 2026-06-18

USE MMO_Market_Schema;
GO

IF NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_NAME = 'Reviews' AND COLUMN_NAME = 'transaction_id'
)
BEGIN
    ALTER TABLE Reviews ADD transaction_id BIGINT NULL;
    ALTER TABLE Reviews ADD CONSTRAINT FK_Reviews_Transaction
        FOREIGN KEY (transaction_id) REFERENCES Transactions(id) ON DELETE NO ACTION;
    PRINT 'Added transaction_id column and FK to Reviews table.';
END
ELSE
BEGIN
    PRINT 'Column transaction_id already exists in Reviews table. Skipping.';
END
GO
