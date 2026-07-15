-- Migration: Add shop levels, flags, and update escrow trigger
-- Purpose: Support seller leveling system and new shop 7-day escrow

USE MMO_System_Schema;
GO

-- 1. Add shop_level and flag_3_count to Users table if they do not exist
IF NOT EXISTS (SELECT * FROM information_schema.columns WHERE table_name = 'Users' AND column_name = 'shop_level')
BEGIN
    ALTER TABLE Users ADD shop_level INT DEFAULT 1;
    PRINT 'Added shop_level to Users.';
END
GO

IF NOT EXISTS (SELECT * FROM information_schema.columns WHERE table_name = 'Users' AND column_name = 'flag_3_count')
BEGIN
    ALTER TABLE Users ADD flag_3_count INT DEFAULT 0;
    PRINT 'Added flag_3_count to Users.';
END
GO

-- 2. Update trg_HoldFundsEscrow to handle Level 0 and Level 1 escrow time
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

    -- Update statuses to 'Held'
    UPDATE Transactions
    SET status = 'Held'
    FROM Transactions t
    INNER JOIN inserted i ON t.id = i.id
    WHERE t.status IS NULL OR t.status = 'Pending';

    -- Update Escrow release dates dynamically based on shop level and completed order count
    UPDATE t
    SET t.escrow_release_date = DATEADD(HOUR, 
        CASE 
            WHEN u.shop_level = 0 THEN 168 -- 7 days for warned shops (Level 0)
            WHEN u.shop_level = 1 AND (
                SELECT COUNT(*) FROM Transactions tx 
                INNER JOIN Products p2 ON tx.product_id = p2.id 
                WHERE p2.seller_id = p.seller_id AND tx.status IN ('Completed', 'Delivered', 'Paid')
            ) < 20 THEN 168 -- 7 days for new shops (Level 1) under 20 orders
            ELSE @EscrowHoldHours -- 3 days normally
        END, 
        GETDATE())
    FROM Transactions t
    INNER JOIN inserted i ON t.id = i.id
    INNER JOIN Products p ON i.product_id = p.id
    INNER JOIN Users u ON p.seller_id = u.id
    WHERE t.escrow_release_date IS NULL;
END;
GO
PRINT 'Updated trg_HoldFundsEscrow for new shop logic.';
GO
