-- ============================================================================
-- Migration: Add status column to Notifications table
-- Date: 2026-07-23
-- Description: Adds status column to Notifications table (DRAFT vs PUBLISHED)
-- ============================================================================

IF NOT EXISTS (
    SELECT 1 
    FROM sys.columns 
    WHERE object_id = OBJECT_ID(N'[dbo].[Notifications]') 
      AND name = N'status'
)
BEGIN
    ALTER TABLE [dbo].[Notifications] 
    ADD [status] NVARCHAR(20) NOT NULL CONSTRAINT [DF_Notifications_status] DEFAULT N'PUBLISHED';

    -- Update existing records to PUBLISHED
    UPDATE [dbo].[Notifications] 
    SET [status] = N'PUBLISHED' 
    WHERE [status] IS NULL;
END
GO

-- ============================================================================
-- Rollback Script:
-- ALTER TABLE [dbo].[Notifications] DROP CONSTRAINT [DF_Notifications_status];
-- ALTER TABLE [dbo].[Notifications] DROP COLUMN [status];
-- ============================================================================
