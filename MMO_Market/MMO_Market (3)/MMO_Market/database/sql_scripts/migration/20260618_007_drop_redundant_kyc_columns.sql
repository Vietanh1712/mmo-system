USE MMO_System_Schema;
GO

-- Migration: Drop redundant KYC columns
-- Description: Drop full_name and date_of_birth from KYCRequests since they are now managed in the Users table via KycRequest -> User entity relationship.
-- Author: Antigravity Agent
-- Date: 2026-06-18

USE MMO_Market_Schema;
GO

-- 1. Pre-check and drop full_name
IF EXISTS (
    SELECT 1 
    FROM sys.columns 
    WHERE Name = N'full_name' AND Object_ID = Object_ID(N'dbo.KYCRequests')
)
BEGIN
    ALTER TABLE dbo.KYCRequests DROP COLUMN full_name;
    PRINT 'Dropped column full_name from KYCRequests.';
END
ELSE
BEGIN
    PRINT 'Column full_name does not exist in KYCRequests.';
END
GO

-- 2. Pre-check and drop date_of_birth
IF EXISTS (
    SELECT 1 
    FROM sys.columns 
    WHERE Name = N'date_of_birth' AND Object_ID = Object_ID(N'dbo.KYCRequests')
)
BEGIN
    ALTER TABLE dbo.KYCRequests DROP COLUMN date_of_birth;
    PRINT 'Dropped column date_of_birth from KYCRequests.';
END
ELSE
BEGIN
    PRINT 'Column date_of_birth does not exist in KYCRequests.';
END
GO

-- 3. Verification
PRINT 'Verification: Current columns in KYCRequests';
SELECT column_name, data_type, is_nullable
FROM INFORMATION_SCHEMA.COLUMNS
WHERE table_name = 'KYCRequests';
GO

