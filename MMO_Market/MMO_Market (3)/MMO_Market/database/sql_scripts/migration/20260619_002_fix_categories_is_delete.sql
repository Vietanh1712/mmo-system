-- ==============================================================================
-- FIX CATEGORY DELETION COLUMNS
-- File: 20260619_FixCategoriesIsDelete.sql
-- Description: Reset column values to 0 where they are NULL.
-- ==============================================================================

USE MMO_System;
GO

-- 1. Ensure any null is_delete or isDelete columns are active (0)
UPDATE Categories
SET is_delete = 0
WHERE is_delete IS NULL;

UPDATE Categories
SET isDelete = 0
WHERE isDelete IS NULL;
GO

PRINT N'✓ Categories deletion status fixed successfully.';
GO
