-- Migration: 20260629_001_enhance_wallet_transactions_for_complaints
-- Description: Add missing columns to WalletTransactions table to support refund logging from complaint resolution
-- Purpose: When complaints are resolved/rejected, we need to record the transaction in wallet history with balance_after and reference_id

USE MMO_System_Schema;
GO

-- Add missing columns if they don't exist
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('WalletTransactions') AND name = 'balance_after')
BEGIN
    ALTER TABLE [dbo].[WalletTransactions] ADD [balance_after] BIGINT NOT NULL DEFAULT 0;
    PRINT 'Added balance_after column to WalletTransactions.';
END
ELSE
BEGIN
    PRINT 'balance_after column already exists in WalletTransactions.';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('WalletTransactions') AND name = 'transaction_type')
BEGIN
    ALTER TABLE [dbo].[WalletTransactions] ADD [transaction_type] VARCHAR(50) NOT NULL DEFAULT 'IN';
    PRINT 'Added transaction_type column to WalletTransactions.';
END
ELSE
BEGIN
    PRINT 'transaction_type column already exists in WalletTransactions.';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('WalletTransactions') AND name = 'reference_id')
BEGIN
    ALTER TABLE [dbo].[WalletTransactions] ADD [reference_id] BIGINT NULL;
    PRINT 'Added reference_id column to WalletTransactions.';
END
ELSE
BEGIN
    PRINT 'reference_id column already exists in WalletTransactions.';
END
GO

-- Create index for better query performance when looking up transactions by reference_id
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE object_id = OBJECT_ID('WalletTransactions') AND name = 'IX_WalletTransactions_ReferenceId')
BEGIN
    CREATE INDEX [IX_WalletTransactions_ReferenceId] ON [dbo].[WalletTransactions]([reference_id]);
    PRINT 'Created index IX_WalletTransactions_ReferenceId.';
END
GO

-- Create a new index to improve query performance for complaint refund lookup
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE object_id = OBJECT_ID('WalletTransactions') AND name = 'IX_WalletTransactions_Type_Status')
BEGIN
    CREATE INDEX [IX_WalletTransactions_Type_Status] ON [dbo].[WalletTransactions]([type], [status]);
    PRINT 'Created index IX_WalletTransactions_Type_Status.';
END
GO

PRINT 'Migration 20260629_001_enhance_wallet_transactions_for_complaints completed successfully.';
GO

