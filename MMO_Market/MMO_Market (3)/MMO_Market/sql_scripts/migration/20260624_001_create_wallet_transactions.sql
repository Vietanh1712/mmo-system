-- Migration: 20260624_001_create_wallet_transactions
-- Description: Create WalletTransactions table for storing all wallet balance history (ledger).

IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[WalletTransactions]') AND type in (N'U'))
BEGIN
    CREATE TABLE [dbo].[WalletTransactions] (
        [id] BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        [user_id] BIGINT NOT NULL,
        [type] VARCHAR(50) NOT NULL, -- TOPUP, PAYMENT, REFUND, ESCROW, WITHDRAWAL
        [amount_vnd] BIGINT NOT NULL,
        [status] VARCHAR(20) NOT NULL, -- PENDING, SUCCESS, FAILED
        [description] NVARCHAR(255) NULL,
        [reference_code] VARCHAR(100) NULL, -- E.g. MMO-ORD-1234, SEPAY-1234
        [created_at] DATETIME DEFAULT GETDATE(),
        [isDelete] BIT DEFAULT 0,
        CONSTRAINT [FK_WalletTransactions_Users] FOREIGN KEY ([user_id]) REFERENCES [dbo].[Users]([id])
    );
    
    CREATE INDEX [IX_WalletTransactions_UserId] ON [dbo].[WalletTransactions]([user_id], [created_at] DESC);
    PRINT 'Created WalletTransactions table successfully.';
END
ELSE
BEGIN
    PRINT 'WalletTransactions table already exists.';
END
GO
