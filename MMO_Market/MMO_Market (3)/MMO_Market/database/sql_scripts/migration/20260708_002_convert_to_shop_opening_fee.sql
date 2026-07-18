-- Migration: 20260708_002_convert_to_shop_opening_fee
-- Description: Convert SELLER_UPGRADE_FEE_VND to SHOP_OPENING_FEE_VND and remove PRODUCT_FEATURED_FEE_VND

USE MMO_System_Schema;
GO

IF EXISTS (SELECT 1 FROM SystemConfigurations WHERE config_key = 'SELLER_UPGRADE_FEE_VND')
BEGIN
    UPDATE SystemConfigurations
    SET config_key = 'SHOP_OPENING_FEE_VND',
        description = N'Phí mở shop (VNĐ)'
    WHERE config_key = 'SELLER_UPGRADE_FEE_VND';
    PRINT 'Renamed SELLER_UPGRADE_FEE_VND to SHOP_OPENING_FEE_VND in SystemConfigurations.';
END
GO

IF EXISTS (SELECT 1 FROM SystemConfigurations WHERE config_key = 'PRODUCT_FEATURED_FEE_VND')
BEGIN
    DELETE FROM SystemConfigurations
    WHERE config_key = 'PRODUCT_FEATURED_FEE_VND';
    PRINT 'Deleted PRODUCT_FEATURED_FEE_VND from SystemConfigurations.';
END
GO
