-- =============================================================================
-- Migration: Add Shop Levels, Flags, and Update Escrow Trigger
-- Date: 2026-07-07
-- Purpose: Implement customer complaint and seller leveling rules
-- =============================================================================

USE MMO_System_Schema;
GO

-- 1. Add shop_level, flag_3_count, withdrawal_locked to Users table
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('Users') AND name = 'shop_level')
BEGIN
    ALTER TABLE Users ADD shop_level INT DEFAULT 1;
    PRINT 'Added shop_level to Users';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('Users') AND name = 'flag_3_count')
BEGIN
    ALTER TABLE Users ADD flag_3_count INT DEFAULT 0;
    PRINT 'Added flag_3_count to Users';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('Users') AND name = 'withdrawal_locked')
BEGIN
    ALTER TABLE Users ADD withdrawal_locked BIT DEFAULT 0;
    PRINT 'Added withdrawal_locked to Users';
END
GO

-- 2. Update trg_HoldFundsEscrow to handle dynamic escrow time based on shop level
CREATE OR ALTER TRIGGER trg_HoldFundsEscrow
ON Transactions
AFTER INSERT
AS
BEGIN
    DECLARE @EscrowHoldHours INT = 72;
    SELECT @EscrowHoldHours = TRY_CAST(config_value AS INT) 
    FROM SystemConfigurations 
    WHERE config_key = 'ESCROW_HOLD_HOURS';
    
    IF @EscrowHoldHours IS NULL SET @EscrowHoldHours = 72;

    -- Update status to Held if not already set (or Pending)
    UPDATE Transactions
    SET status = 'Held'
    FROM Transactions t
    INNER JOIN inserted i ON t.id = i.id
    WHERE t.status IS NULL OR t.status = 'Pending';

    -- Update escrow_release_date based on shop level logic
    UPDATE Transactions
    SET escrow_release_date = DATEADD(HOUR, 
        CASE 
            WHEN u.shop_level = 0 THEN 168 -- Level 0: 7 days
            WHEN u.shop_level = 1 AND (SELECT COUNT(*) FROM Transactions t2 WHERE t2.seller_id = u.id) <= 20 THEN 168 -- Level 1 first 20 orders: 7 days
            ELSE @EscrowHoldHours -- Default (Level 2 or >20 orders on Level 1): 72 hours
        END, GETDATE())
    FROM Transactions t
    INNER JOIN inserted i ON t.id = i.id
    INNER JOIN Users u ON t.seller_id = u.id
    WHERE t.escrow_release_date IS NULL;
END;
GO
PRINT 'Updated trg_HoldFundsEscrow with shop level logic.';
GO
