USE MMO_System_Schema;
GO

IF NOT EXISTS (SELECT 1 FROM SystemConfigurations WHERE config_key = 'SHOP_OPENING_FEE_VND')
BEGIN
    INSERT INTO SystemConfigurations (config_key, config_value, description, updated_at)
    VALUES ('SHOP_OPENING_FEE_VND', '50000', N'Phí mở shop (VNĐ)', GETDATE());
    PRINT 'Inserted SHOP_OPENING_FEE_VND configuration.';
END
ELSE
BEGIN
    PRINT 'SHOP_OPENING_FEE_VND configuration already exists.';
END
GO
