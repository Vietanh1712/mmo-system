-- Migration: Add category, support_email, support_phone to SellerRegistrations
-- Purpose: Support full shop registration form data
-- Author: Antigravity
-- Date: 2026-06-23

USE MMO_System_Schema;
GO

-- Pre-check
IF EXISTS (SELECT * FROM information_schema.tables WHERE table_name = 'SellerRegistrations')
BEGIN
    PRINT 'Table SellerRegistrations exists. Proceeding with adding columns...';
    
    -- Check and add category
    IF NOT EXISTS (SELECT * FROM information_schema.columns WHERE table_name = 'SellerRegistrations' AND column_name = 'category')
    BEGIN
        ALTER TABLE SellerRegistrations ADD category NVARCHAR(100) NULL;
        PRINT 'Added column category.';
    END
    ELSE
    BEGIN
        PRINT 'Column category already exists.';
    END

    -- Check and add support_email
    IF NOT EXISTS (SELECT * FROM information_schema.columns WHERE table_name = 'SellerRegistrations' AND column_name = 'support_email')
    BEGIN
        ALTER TABLE SellerRegistrations ADD support_email VARCHAR(255) NULL;
        PRINT 'Added column support_email.';
    END
    ELSE
    BEGIN
        PRINT 'Column support_email already exists.';
    END

    -- Check and add support_phone
    IF NOT EXISTS (SELECT * FROM information_schema.columns WHERE table_name = 'SellerRegistrations' AND column_name = 'support_phone')
    BEGIN
        ALTER TABLE SellerRegistrations ADD support_phone VARCHAR(20) NULL;
        PRINT 'Added column support_phone.';
    END
    ELSE
    BEGIN
        PRINT 'Column support_phone already exists.';
    END
END
ELSE
BEGIN
    PRINT 'Error: Table SellerRegistrations does not exist.';
END
GO

-- Verification
SELECT column_name, data_type, character_maximum_length 
FROM information_schema.columns 
WHERE table_name = 'SellerRegistrations' 
  AND column_name IN ('category', 'support_email', 'support_phone');
GO


-- =========================================================================================
-- MIGRATION: Thêm cột rejection_reason cho SellerRegistrations
-- AUTHOR: Agent
-- DATE: 2026-06-23
-- =========================================================================================

-- 1. Pre-check: Nếu chưa có cột rejection_reason thì thêm vào
IF NOT EXISTS (
    SELECT * FROM sys.columns 
    WHERE object_id = OBJECT_ID(N'[dbo].[SellerRegistrations]') 
    AND name = 'rejection_reason'
)
BEGIN
    ALTER TABLE [dbo].[SellerRegistrations]
    ADD [rejection_reason] NVARCHAR(MAX) NULL;
    
    PRINT 'Da them cot rejection_reason thanh cong.';
END
ELSE
BEGIN
    PRINT 'Cot rejection_reason da ton tai.';
END
GO
