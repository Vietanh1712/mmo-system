-- ============================================================
-- FILE: 20260611_AddDigitalAssets.sql
-- DATE: 2026-06-11
-- TITLE: Thêm bảng DigitalAssets và cột product_type cho Products
-- DESCRIPTION:
--   1. Thêm cột product_type vào bảng Products
--      Giá trị hợp lệ: ACCOUNT | KEY | GAME_CARD
--   2. Tạo bảng DigitalAssets lưu tài sản số thực tế (tài khoản, key)
--   3. Seed 6 bản ghi mẫu cho mỗi loại asset
-- ============================================================

USE MMO_System;
GO

-- ===========================================================
-- BƯỚC 1: Thêm cột product_type vào Products
-- ===========================================================
IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID('dbo.Products')
      AND name = 'product_type'
)
BEGIN
    ALTER TABLE dbo.Products
    ADD product_type NVARCHAR(20) NOT NULL DEFAULT 'ACCOUNT';
    PRINT 'Đã thêm cột product_type vào Products';
END
ELSE
BEGIN
    PRINT 'Cột product_type đã tồn tại — bỏ qua';
END
GO

-- ===========================================================
-- BƯỚC 2: Tạo hoặc nâng cấp bảng DigitalAssets
-- ===========================================================
IF EXISTS (SELECT 1 FROM sys.objects WHERE object_id = OBJECT_ID('dbo.DigitalAssets') AND type = 'U')
BEGIN
    -- Nếu bảng đã tồn tại nhưng có cột 'status' (bảng cũ từ MMO_System_Schema.sql)
    -- hoặc thiếu cột 'asset_type' thì mới drop và recreate.
    IF EXISTS (
        SELECT 1 FROM sys.columns
        WHERE object_id = OBJECT_ID('dbo.DigitalAssets')
          AND name = 'status'
    ) OR NOT EXISTS (
        SELECT 1 FROM sys.columns
        WHERE object_id = OBJECT_ID('dbo.DigitalAssets')
          AND name = 'asset_type'
    )
    BEGIN
        DROP TABLE dbo.DigitalAssets;
        PRINT 'Phát hiện bảng DigitalAssets phiên bản cũ có cột status hoặc thiếu asset_type. Đã xóa bảng cũ.';
    END
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE object_id = OBJECT_ID('dbo.DigitalAssets') AND type = 'U')
BEGIN
    CREATE TABLE dbo.DigitalAssets (
        id          BIGINT IDENTITY(1,1) PRIMARY KEY,
        variant_id  BIGINT NOT NULL,
        asset_type  NVARCHAR(20) NOT NULL,       -- ACCOUNT | KEY | GAME_CARD
        asset_data  NVARCHAR(MAX) NOT NULL,      -- JSON lưu thông tin tài sản
        is_used     BIT NOT NULL DEFAULT 0,      -- 0 = còn hàng, 1 = đã bán
        is_delete   BIT NOT NULL DEFAULT 0,
        created_at  DATETIME NOT NULL DEFAULT GETDATE(),
        CONSTRAINT FK_DigitalAssets_Variant FOREIGN KEY (variant_id)
            REFERENCES dbo.ProductVariants(id)
    );
    PRINT 'Đã tạo bảng DigitalAssets';
END
ELSE
BEGIN
    PRINT 'Bảng DigitalAssets đã đúng cấu trúc — bỏ qua tạo bảng';
END
GO

-- ===========================================================
-- BƯỚC 3: Cập nhật product_type cho sản phẩm mẫu hiện có
-- (dựa vào tên sản phẩm trong SEED_SellerMockData.sql)
-- ===========================================================

-- Sản phẩm loại tài khoản (Netflix, Spotify, Canva, ChatGPT)
UPDATE dbo.Products
SET product_type = 'ACCOUNT'
WHERE name IN ('Netflix Premium', 'Spotify Family', 'Canva Pro', 'ChatGPT Plus', 'Adobe Creative Cloud',
               'Netflix Premium 4K UHD', 'Spotify Premium Gia Đình', 'Canva Pro Giáo Dục / Cá Nhân', 'ChatGPT Plus GPT-4')
   OR name LIKE '%Netflix%'
   OR name LIKE '%Spotify%'
   OR name LIKE '%Canva%'
   OR name LIKE '%ChatGPT%'
   OR name LIKE '%Adobe%';

-- Sản phẩm loại key (Tool, Game)
UPDATE dbo.Products
SET product_type = 'KEY'
WHERE name IN ('Tool Auto Nuôi Clone Facebook Pro', 'Autocad 2024', 'Microsoft Office 365')
   OR name LIKE '%Tool Auto%'
   OR name LIKE '%Autocad%'
   OR name LIKE '%Microsoft Office%';

-- Sản phẩm loại game card (nếu có)
UPDATE dbo.Products
SET product_type = 'GAME_CARD'
WHERE name LIKE '%Garena%' OR name LIKE '%VCoin%' OR name LIKE '%Roblox%';

PRINT 'Đã cập nhật product_type cho sản phẩm mẫu';
GO

-- ===========================================================
-- BƯỚC 4: Seed dữ liệu mẫu DigitalAssets
-- Giả sử variant_id 1-3 là các biến thể của Netflix (ACCOUNT)
-- và variant_id 4-5 là các biến thể của Tool Facebook (KEY)
-- Hãy điều chỉnh variant_id cho phù hợp với dữ liệu thực tế
-- ===========================================================

-- Lấy ID biến thể đầu tiên của từng sản phẩm để seed
DECLARE @netflixVariantId BIGINT;
DECLARE @toolVariantId BIGINT;
DECLARE @spotifyVariantId BIGINT;

SELECT TOP 1 @netflixVariantId = pv.id
FROM dbo.ProductVariants pv
JOIN dbo.Products p ON pv.product_id = p.id
WHERE (p.name = 'Netflix Premium' OR p.name = 'Netflix Premium 4K UHD' OR p.name LIKE 'Netflix Premium%') AND pv.isDelete = 0
ORDER BY pv.id;

SELECT TOP 1 @toolVariantId = pv.id
FROM dbo.ProductVariants pv
JOIN dbo.Products p ON pv.product_id = p.id
WHERE (p.name = 'Tool Auto Nuôi Clone Facebook Pro' OR p.name LIKE 'Tool Auto Nuôi Clone Facebook%') AND pv.isDelete = 0
ORDER BY pv.id;

SELECT TOP 1 @spotifyVariantId = pv.id
FROM dbo.ProductVariants pv
JOIN dbo.Products p ON pv.product_id = p.id
WHERE (p.name = 'Spotify Family' OR p.name = 'Spotify Premium Gia Đình' OR p.name LIKE 'Spotify%') AND pv.isDelete = 0
ORDER BY pv.id;

-- Xóa seed cũ nếu chạy lại
DELETE FROM dbo.DigitalAssets WHERE is_delete = 0 AND created_at < GETDATE();

-- Seed ACCOUNT assets (Netflix)
IF @netflixVariantId IS NOT NULL
BEGIN
    INSERT INTO dbo.DigitalAssets (variant_id, asset_type, asset_data, is_used)
    VALUES
        (@netflixVariantId, 'ACCOUNT', N'{"username":"netflix_user_01@gmail.com","password":"NetflixSecure@2026","note":"Tài khoản mới 100%"}', 0),
        (@netflixVariantId, 'ACCOUNT', N'{"username":"netflix_user_02@gmail.com","password":"NetflixPass2026!","note":"Gói Premium 4K"}',  0),
        (@netflixVariantId, 'ACCOUNT', N'{"username":"netflix_user_03@gmail.com","password":"Nf2026@Premium","note":"Màn hình riêng"}',   0),
        (@netflixVariantId, 'ACCOUNT', N'{"username":"netflix_user_04@gmail.com","password":"Nf@Account2026","note":"Còn 11 tháng"}',     0),
        (@netflixVariantId, 'ACCOUNT', N'{"username":"netflix_user_05@gmail.com","password":"NetflixPro2026","note":"Đổi mk sau nhận"}',   0);
    PRINT 'Đã seed 5 ACCOUNT assets cho Netflix';
END

-- Seed KEY assets (Tool Facebook)
IF @toolVariantId IS NOT NULL
BEGIN
    INSERT INTO dbo.DigitalAssets (variant_id, asset_type, asset_data, is_used)
    VALUES
        (@toolVariantId, 'KEY', N'{"key":"TOOL-A1B2-C3D4-E5F6","note":"Key 1 tháng — chưa kích hoạt"}',  0),
        (@toolVariantId, 'KEY', N'{"key":"TOOL-G7H8-I9J0-K1L2","note":"Key 1 tháng — chưa kích hoạt"}',  0),
        (@toolVariantId, 'KEY', N'{"key":"TOOL-M3N4-O5P6-Q7R8","note":"Key 1 tháng — còn mới"}',         0),
        (@toolVariantId, 'KEY', N'{"key":"TOOL-S9T0-U1V2-W3X4","note":"Key 1 tháng — còn mới"}',         0),
        (@toolVariantId, 'KEY', N'{"key":"TOOL-Y5Z6-A7B8-C9D0","note":"Key 1 tháng — đặc biệt"}',        0);
    PRINT 'Đã seed 5 KEY assets cho Tool Facebook';
END

-- Seed ACCOUNT assets (Spotify)
IF @spotifyVariantId IS NOT NULL
BEGIN
    INSERT INTO dbo.DigitalAssets (variant_id, asset_type, asset_data, is_used)
    VALUES
        (@spotifyVariantId, 'ACCOUNT', N'{"username":"spotify_family01@gmail.com","password":"Sp0tify@2026","note":"Gói gia đình 6 slot"}', 0),
        (@spotifyVariantId, 'ACCOUNT', N'{"username":"spotify_family02@gmail.com","password":"Sp0tify!2026","note":"Còn 5 slot trống"}',    0),
        (@spotifyVariantId, 'ACCOUNT', N'{"username":"spotify_family03@gmail.com","password":"SpFamily2026","note":"Còn 4 slot trống"}',     0),
        (@spotifyVariantId, 'ACCOUNT', N'{"username":"spotify_family04@gmail.com","password":"SpFam@2026","note":"Đổi mk sau nhận"}',        0),
        (@spotifyVariantId, 'ACCOUNT', N'{"username":"spotify_family05@gmail.com","password":"Family!2026","note":"Tài khoản mới"}',         0);
    PRINT 'Đã seed 5 ACCOUNT assets cho Spotify';
END

GO

PRINT '===================================================';
PRINT 'Hoàn thành: 20260611_AddDigitalAssets.sql';
PRINT 'Bước tiếp theo: Chạy Maven spring-boot:run để áp dụng model mới';
PRINT '===================================================';
