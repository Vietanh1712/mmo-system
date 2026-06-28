-- =========================================================================
-- Migration: Add transaction_id to DigitalAssets
-- Date: 2026-06-25
-- Description: Liên kết tài sản số đã bán với Transaction tương ứng.
-- =========================================================================
USE MMO_System_Schema;
GO

IF NOT EXISTS (
    SELECT 1 
    FROM sys.columns 
    WHERE object_id = OBJECT_ID('DigitalAssets') AND name = 'transaction_id'
)
BEGIN
    ALTER TABLE DigitalAssets
    ADD transaction_id BIGINT NULL;

    -- Thêm foreign key liên kết với Transactions
    ALTER TABLE DigitalAssets
    ADD CONSTRAINT FK_DigitalAssets_Transactions
    FOREIGN KEY (transaction_id) REFERENCES Transactions(id);
END
GO
