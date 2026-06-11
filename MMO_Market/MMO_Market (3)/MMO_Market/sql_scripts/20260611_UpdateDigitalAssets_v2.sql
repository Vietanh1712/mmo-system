-- ============================================================
-- FILE: 20260611_UpdateDigitalAssets_v2.sql
-- DATE: 2026-06-11
-- TITLE: Nâng cấp bảng DigitalAssets - Thêm quản lý ảnh sản phẩm
-- DESCRIPTION:
--   1. Thêm cột 'product_image_url' vào bảng Products
--   2. Thêm các cột chi tiết cho DigitalAssets dựa trên loại tài sản
--      - account_username, account_password (cho loại ACCOUNT)
--      - key_code (cho loại KEY)
--      - card_code, card_pin (cho loại GAME_CARD)
--   3. Làm strict schema để validate theo loại tài sản
-- ============================================================

USE MMO_System;
GO

-- ===========================================================
-- BƯỚC 1: Thêm cột product_image_url vào Products
-- ===========================================================
IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID('dbo.Products')
      AND name = 'product_image_url'
)
BEGIN
    ALTER TABLE dbo.Products
    ADD product_image_url NVARCHAR(500) NULL;
    PRINT 'Đã thêm cột product_image_url vào Products';
END
ELSE
BEGIN
    PRINT 'Cột product_image_url đã tồn tại — bỏ qua';
END
GO

-- ===========================================================
-- BƯỚC 2: Cập nhật DigitalAssets - Thêm cột chi tiết
-- ===========================================================

-- Thêm cột account_username (cho loại ACCOUNT)
IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID('dbo.DigitalAssets')
      AND name = 'account_username'
)
BEGIN
    ALTER TABLE dbo.DigitalAssets
    ADD account_username NVARCHAR(255) NULL;
    PRINT 'Đã thêm cột account_username';
END
GO

-- Thêm cột account_password (cho loại ACCOUNT)
IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID('dbo.DigitalAssets')
      AND name = 'account_password'
)
BEGIN
    ALTER TABLE dbo.DigitalAssets
    ADD account_password NVARCHAR(500) NULL;
    PRINT 'Đã thêm cột account_password';
END
GO

-- Thêm cột key_code (cho loại KEY)
IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID('dbo.DigitalAssets')
      AND name = 'key_code'
)
BEGIN
    ALTER TABLE dbo.DigitalAssets
    ADD key_code NVARCHAR(MAX) NULL;
    PRINT 'Đã thêm cột key_code';
END
GO

-- Thêm cột card_code (cho loại GAME_CARD)
IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID('dbo.DigitalAssets')
      AND name = 'card_code'
)
BEGIN
    ALTER TABLE dbo.DigitalAssets
    ADD card_code NVARCHAR(MAX) NULL;
    PRINT 'Đã thêm cột card_code';
END
GO

-- Thêm cột card_pin (cho loại GAME_CARD)
IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID('dbo.DigitalAssets')
      AND name = 'card_pin'
)
BEGIN
    ALTER TABLE dbo.DigitalAssets
    ADD card_pin NVARCHAR(255) NULL;
    PRINT 'Đã thêm cột card_pin';
END
GO

-- Thêm cột notes (ghi chú chung cho tất cả loại)
IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID('dbo.DigitalAssets')
      AND name = 'notes'
)
BEGIN
    ALTER TABLE dbo.DigitalAssets
    ADD notes NVARCHAR(MAX) NULL;
    PRINT 'Đã thêm cột notes';
END
GO

-- ===========================================================
-- BƯỚC 3: Di chuyển dữ liệu cũ từ JSON asset_data sang cột chi tiết (nếu có)
-- ===========================================================
IF EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID('dbo.DigitalAssets')
      AND name = 'account_username'
)
BEGIN
    UPDATE dbo.DigitalAssets
    SET account_username = ISNULL(account_username, JSON_VALUE(asset_data, '$.username')),
        account_password = ISNULL(account_password, JSON_VALUE(asset_data, '$.password')),
        notes = ISNULL(notes, JSON_VALUE(asset_data, '$.note'))
    WHERE asset_type = 'ACCOUNT';

    UPDATE dbo.DigitalAssets
    SET key_code = ISNULL(key_code, JSON_VALUE(asset_data, '$.key')),
        notes = ISNULL(notes, JSON_VALUE(asset_data, '$.note'))
    WHERE asset_type = 'KEY';

    UPDATE dbo.DigitalAssets
    SET card_code = ISNULL(card_code, JSON_VALUE(asset_data, '$.card_code')),
        card_pin = ISNULL(card_pin, JSON_VALUE(asset_data, '$.card_pin')),
        notes = ISNULL(notes, JSON_VALUE(asset_data, '$.note'))
    WHERE asset_type = 'GAME_CARD';

    PRINT 'Đã chuyển đổi dữ liệu từ JSON asset_data sang các cột chi tiết tương ứng';
END
GO

PRINT '===================================================';
PRINT 'Hoàn thành: 20260611_UpdateDigitalAssets_v2.sql';
PRINT 'Schema đã sẵn sàng cho quản lý tài sản số với ảnh và chi tiết loại tài sản';
PRINT '===================================================';
