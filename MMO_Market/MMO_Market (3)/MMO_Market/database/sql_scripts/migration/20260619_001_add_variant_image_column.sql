-- ==============================================================================
-- ADD IMAGE URL TO PRODUCT VARIANTS
-- File: 20260619_AddVariantImageColumn.sql
-- Description: Add image_url column to ProductVariants table.
-- ==============================================================================

USE MMO_Market_Schema;
GO

IF NOT EXISTS (
    SELECT 1 
    FROM sys.columns 
    WHERE object_id = OBJECT_ID('dbo.ProductVariants') 
      AND name = 'image_url'
)
BEGIN
    ALTER TABLE dbo.ProductVariants
    ADD image_url NVARCHAR(500) NULL;
    PRINT N'✓ Added column image_url to ProductVariants table.';
END
ELSE
BEGIN
    PRINT N'✓ Column image_url already exists in ProductVariants table.';
END
GO
