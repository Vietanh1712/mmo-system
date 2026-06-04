-- ==============================================================================
-- INSERT TEST PRODUCTS FOR SEARCH FUNCTIONALITY
-- Tên tệp: INSERT_TestProducts.sql
-- Mô tả: Chèn dữ liệu test sản phẩm để test tính năng tìm kiếm
-- ==============================================================================

USE MMO_System;
GO

-- BƯỚC 1: Thêm test user (người bán) nếu chưa có
IF NOT EXISTS (SELECT 1 FROM Users WHERE email = 'seller@test.com')
BEGIN
    INSERT INTO Users (email, password, full_name, role, isVerified, balance_vnd, isDelete)
    VALUES ('seller@test.com', 'hashed_password_123', N'Test Seller', 'SELLER', 1, 1000000, 0);
    PRINT N'✓ Thêm test user (seller@test.com)';
END
GO

-- BƯỚC 2: Lấy ID của seller test
DECLARE @SellerId BIGINT = (SELECT TOP 1 id FROM Users WHERE email = 'seller@test.com');
DECLARE @CategoryId1 BIGINT = (SELECT TOP 1 id FROM Categories WHERE name = N'Tài khoản Facebook' AND isDelete = 0);
DECLARE @CategoryId2 BIGINT = (SELECT TOP 1 id FROM Categories WHERE name = N'Dịch vụ Streaming' AND isDelete = 0);
DECLARE @CategoryId3 BIGINT = (SELECT TOP 1 id FROM Categories WHERE name = N'Tool Facebook' AND isDelete = 0);
DECLARE @CategoryId4 BIGINT = (SELECT TOP 1 id FROM Categories WHERE name = N'Phần Mềm Google' AND isDelete = 0);

-- Nếu không có category cụ thể, dùng category đầu tiên
IF @CategoryId1 IS NULL
    SET @CategoryId1 = (SELECT TOP 1 id FROM Categories WHERE parent_id IS NOT NULL AND isDelete = 0 ORDER BY id);

IF @CategoryId2 IS NULL
    SET @CategoryId2 = @CategoryId1;

IF @CategoryId3 IS NULL
    SET @CategoryId3 = @CategoryId1;

IF @CategoryId4 IS NULL
    SET @CategoryId4 = @CategoryId1;

PRINT N'📍 SellerId: ' + CAST(@SellerId AS NVARCHAR(20));
PRINT N'📍 CategoryId1: ' + CAST(@CategoryId1 AS NVARCHAR(20));

-- BƯỚC 3: Chèn Test Products - Netflix
PRINT N'';
PRINT N'📦 CHÈN TEST PRODUCTS CHO TÌM KIẾM...';

-- Netflix Products (5 sản phẩm)
IF NOT EXISTS (SELECT 1 FROM Products WHERE name LIKE N'Netflix%' AND seller_id = @SellerId)
BEGIN
    INSERT INTO Products (seller_id, category_id, name, description, image, isDelete)
    VALUES
        (@SellerId, @CategoryId1, N'Netflix Premium 4K UHD - 1 Tháng',
         N'Tài khoản Netflix Premium chất lượng 4K UltraHD, giao ngay 5 phút sau thanh toán. Bảo hành 100% hoàn tiền nếu không hoạt động',
         'https://via.placeholder.com/300x160/fd761a/ffffff?text=Netflix+Premium', 0),
        (@SellerId, @CategoryId1, N'Netflix Premium 6 Tháng - Giá Rẻ',
         N'Tài khoản Netflix 6 tháng với giá tốt nhất, được cập nhật mới nhất. Hỗ trợ 4K HD',
         'https://via.placeholder.com/300x160/fd761a/ffffff?text=Netflix+6Months', 0),
        (@SellerId, @CategoryId1, N'Netflix Premium 1 Năm - Bảo Hành Dài',
         N'Tài khoản Netflix gói 12 tháng, an toàn, bảo hành 12 tháng. Có chứng chỉ.',
         'https://via.placeholder.com/300x160/fd761a/ffffff?text=Netflix+1Year', 0),
        (@SellerId, @CategoryId1, N'Netflix Family Plan - 4 Hồ Sơ',
         N'Gói chia sẻ 4 hồ sơ, xem cùng lúc. Tiết kiệm chi phí gia đình.',
         'https://via.placeholder.com/300x160/fd761a/ffffff?text=Netflix+Family', 0),
        (@SellerId, @CategoryId1, N'Netflix Standard 2 Hồ Sơ - 1080p',
         N'Gói standard cho 2 hồ sơ, chất lượng 1080p Full HD. Giá rẻ.',
         'https://via.placeholder.com/300x160/fd761a/ffffff?text=Netflix+Standard', 0);

    PRINT N'✓ Đã chèn 5 sản phẩm Netflix';
END

-- ChatGPT Plus Products (3 sản phẩm)
IF NOT EXISTS (SELECT 1 FROM Products WHERE name LIKE N'ChatGPT%' AND seller_id = @SellerId)
BEGIN
    INSERT INTO Products (seller_id, category_id, name, description, image, isDelete)
    VALUES
        (@SellerId, @CategoryId2, N'ChatGPT Plus 1 Tháng - Truy Cập Không Giới Hạn',
         N'Tài khoản ChatGPT Plus với quyền truy cập GPT-4, Plugin, Code Interpreter. Giao tức thì.',
         'https://via.placeholder.com/300x160/fd761a/ffffff?text=ChatGPT+Plus', 0),
        (@SellerId, @CategoryId2, N'ChatGPT Plus 3 Tháng - Giá Ưu Đãi',
         N'Gói 3 tháng ChatGPT Plus giá rẻ nhất hiện nay. Kích hoạt ngay lập tức.',
         'https://via.placeholder.com/300x160/fd761a/ffffff?text=ChatGPT+3Month', 0),
        (@SellerId, @CategoryId2, N'ChatGPT Team Plan - Nhóm Làm Việc',
         N'Gói team ChatGPT Plus cho nhóm làm việc, chia sẻ workspace. Giải pháp doanh nghiệp',
         'https://via.placeholder.com/300x160/fd761a/ffffff?text=ChatGPT+Team', 0);

    PRINT N'✓ Đã chèn 3 sản phẩm ChatGPT Plus';
END

-- Facebook Tool Products (4 sản phẩm)
IF NOT EXISTS (SELECT 1 FROM Products WHERE name LIKE N'Tool Facebook%' AND seller_id = @SellerId)
BEGIN
    INSERT INTO Products (seller_id, category_id, name, description, image, isDelete)
    VALUES
        (@SellerId, @CategoryId3, N'Tool Facebook Auto Like v2.5 - Tối Ưu Nhất',
         N'Tool tăng like Facebook tự động, an toàn, không bị khóa. Hỗ trợ Fanpage, Bài viết cá nhân',
         'https://via.placeholder.com/300x160/fd761a/ffffff?text=Tool+FB+Auto', 0),
        (@SellerId, @CategoryId3, N'Tool Facebook Comment Tự Động - Pro Version',
         N'Bot comment Facebook chuyên dụng, nhanh, hiệu quả. Có hỗ trợ comment tiếng Việt tự nhiên',
         'https://via.placeholder.com/300x160/fd761a/ffffff?text=FB+Comment+Bot', 0),
        (@SellerId, @CategoryId3, N'Tool Clone Facebook Account - Bảo Mật 99%',
         N'Công cụ clone tài khoản Facebook an toàn, không sợ khóa. Hỗ trợ 2FA.',
         'https://via.placeholder.com/300x160/fd761a/ffffff?text=FB+Clone+Tool', 0),
        (@SellerId, @CategoryId3, N'Facebook Marketing Suite - Toàn Bộ Công Cụ',
         N'Bộ công cụ marketing Facebook đầy đủ: Like, Comment, Follow, Message. Tiết kiệm 70%',
         'https://via.placeholder.com/300x160/fd761a/ffffff?text=FB+Marketing', 0);

    PRINT N'✓ Đã chèn 4 sản phẩm Tool Facebook';
END

-- Google Ads / SEO Products (3 sản phẩm)
IF NOT EXISTS (SELECT 1 FROM Products WHERE name LIKE N'Google%' AND seller_id = @SellerId AND name NOT LIKE N'Google Ads Code%')
BEGIN
    INSERT INTO Products (seller_id, category_id, name, description, image, isDelete)
    VALUES
        (@SellerId, @CategoryId4, N'Google SEO Tool - Rank Higher on Google',
         N'Công cụ SEO chuyên dụng cho Google Search, tối ưu từ khóa, tăng rank nhanh',
         'https://via.placeholder.com/300x160/fd761a/ffffff?text=Google+SEO', 0),
        (@SellerId, @CategoryId4, N'Google Ads Manager - Quản Lý Quảng Cáo',
         N'Tool quản lý Google Ads tích hợp, tự động tối ưu bidding, giảm chi phí 40%',
         'https://via.placeholder.com/300x160/fd761a/ffffff?text=Google+Ads', 0),
        (@SellerId, @CategoryId4, N'Google Analytics Pro Plugin',
         N'Plugin analytics cao cấp, thống kê chi tiết dữ liệu truy cập, conversion tracking',
         'https://via.placeholder.com/300x160/fd761a/ffffff?text=GA+Plugin', 0);

    PRINT N'✓ Đã chèn 3 sản phẩm Google Tool';
END

-- BƯỚC 4: Thêm ProductVariants cho các sản phẩm
PRINT N'';
PRINT N'📝 CHÈN PRODUCT VARIANTS...';

DECLARE @ProductId1 BIGINT, @ProductId2 BIGINT, @ProductId3 BIGINT;

-- Variant cho Netflix Premium
IF NOT EXISTS (SELECT 1 FROM ProductVariants WHERE product_id IN (SELECT id FROM Products WHERE name = N'Netflix Premium 4K UHD - 1 Tháng'))
BEGIN
    SET @ProductId1 = (SELECT TOP 1 id FROM Products WHERE name = N'Netflix Premium 4K UHD - 1 Tháng' AND seller_id = @SellerId AND isDelete = 0);

    IF @ProductId1 IS NOT NULL
    BEGIN
        INSERT INTO ProductVariants (product_id, variant_name, price_vnd, stock, status, isDelete)
        VALUES
            (@ProductId1, N'Netflix Premium 4K - 1 Tháng', 45000, 15, 'Active', 0),
            (@ProductId1, N'Netflix Premium 4K - 1 Tháng (Shared)', 25000, 20, 'Active', 0);

        PRINT N'✓ Đã thêm variant cho sản phẩm Netflix Premium';
    END
END

-- Variant cho ChatGPT Plus
IF NOT EXISTS (SELECT 1 FROM ProductVariants WHERE product_id IN (SELECT id FROM Products WHERE name = N'ChatGPT Plus 1 Tháng - Truy Cập Không Giới Hạn'))
BEGIN
    SET @ProductId2 = (SELECT TOP 1 id FROM Products WHERE name = N'ChatGPT Plus 1 Tháng - Truy Cập Không Giới Hạn' AND seller_id = @SellerId AND isDelete = 0);

    IF @ProductId2 IS NOT NULL
    BEGIN
        INSERT INTO ProductVariants (product_id, variant_name, price_vnd, stock, status, isDelete)
        VALUES
            (@ProductId2, N'ChatGPT Plus 1 Tháng', 199000, 10, 'Active', 0),
            (@ProductId2, N'ChatGPT Plus 3 Tháng', 579000, 5, 'Active', 0);

        PRINT N'✓ Đã thêm variant cho sản phẩm ChatGPT Plus';
    END
END

-- Variant cho Facebook Tool
IF NOT EXISTS (SELECT 1 FROM ProductVariants WHERE product_id IN (SELECT id FROM Products WHERE name = N'Tool Facebook Auto Like v2.5 - Tối Ưu Nhất'))
BEGIN
    SET @ProductId3 = (SELECT TOP 1 id FROM Products WHERE name = N'Tool Facebook Auto Like v2.5 - Tối Ưu Nhất' AND seller_id = @SellerId AND isDelete = 0);

    IF @ProductId3 IS NOT NULL
    BEGIN
        INSERT INTO ProductVariants (product_id, variant_name, price_vnd, stock, status, isDelete)
        VALUES
            (@ProductId3, N'Facebook Tool - 1 Tháng', 199000, 8, 'Active', 0),
            (@ProductId3, N'Facebook Tool - 3 Tháng', 499000, 12, 'Active', 0);

        PRINT N'✓ Đã thêm variant cho sản phẩm Facebook Tool';
    END
END

-- BƯỚC 5: Kiểm tra kết quả
PRINT N'';
PRINT N'========================================';
PRINT N'✅ CHÈN DỮ LIỆU TEST HOÀN THÀNH!';
PRINT N'========================================';

SELECT
    COUNT(*) AS [Tổng sản phẩm test],
    SUM(CASE WHEN name LIKE N'Netflix%' THEN 1 ELSE 0 END) AS [Netflix],
    SUM(CASE WHEN name LIKE N'ChatGPT%' THEN 1 ELSE 0 END) AS [ChatGPT],
    SUM(CASE WHEN name LIKE N'Tool Facebook%' OR name LIKE N'Facebook%' THEN 1 ELSE 0 END) AS [Facebook Tools],
    SUM(CASE WHEN name LIKE N'Google%' THEN 1 ELSE 0 END) AS [Google Tools]
FROM Products
WHERE seller_id = @SellerId AND isDelete = 0;

PRINT N'';
PRINT N'📋 DANH SÁCH SẢN PHẨM TEST:';
SELECT
    id,
    name AS [Tên sản phẩm],
    (SELECT COUNT(*) FROM ProductVariants WHERE product_id = Products.id) AS [Số variant],
    (SELECT SUM(stock) FROM ProductVariants WHERE product_id = Products.id) AS [Tổng kho]
FROM Products
WHERE seller_id = @SellerId AND isDelete = 0
ORDER BY id;

PRINT N'';
PRINT N'✨ Bây giờ hãy tìm kiếm: "Netflix", "ChatGPT", "Facebook", "Google" trên trang chủ!';
GO

