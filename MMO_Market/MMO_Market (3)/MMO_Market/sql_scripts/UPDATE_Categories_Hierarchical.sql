-- ==============================================================================
-- CẬP NHẬT BẢNG CATEGORIES VỚI CẤU TRÚC PHÂN CẤP (HIERARCHICAL)
-- Tên tệp: UPDATE_Categories_Hierarchical.sql
-- Ngày: 2026-05-28
-- Mô tả: Thêm cột parent_id, xóa dữ liệu cũ, insert danh mục cha-con
-- ==============================================================================

USE MMO_System;
GO

-- 1. Thêm cột parent_id vào bảng Categories
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('Categories') AND name = 'parent_id')
BEGIN
    ALTER TABLE Categories ADD parent_id BIGINT NULL;
    ALTER TABLE Categories ADD CONSTRAINT FK_Category_Parent FOREIGN KEY (parent_id) REFERENCES Categories(id) ON DELETE NO ACTION;
    PRINT N'✓ Cột parent_id và foreign key đã được thêm vào bảng Categories';
END
ELSE
BEGIN
    PRINT N'⚠ Cột parent_id đã tồn tại trong bảng Categories';
END
GO

-- 2. Xóa dữ liệu cũ (nếu có) để insert bộ mới chuẩn chỉ
-- Bước này xóa mềm (Soft Delete) hoặc hard delete tùy yêu cầu
BEGIN
    DELETE FROM Categories WHERE isDelete = 0 OR isDelete = 1;
    DBCC CHECKIDENT ('Categories', RESEED, 0);
    PRINT N'✓ Dữ liệu cũ đã được xóa, Identity reset về 0';
END
GO

-- 3. Insert Danh mục cha (Parent Categories) - parent_id = NULL
-- Tổng 6 danh mục cha chính
DECLARE @StartIdentity INT = 1;

INSERT INTO Categories (name, parent_id, description, isDelete)
VALUES
    ('Email', NULL, N'Các dịch vụ email và tài khoản mail', 0),                          -- ID: 1
    ('Tài khoản', NULL, N'Tài khoản các nền tảng xã hội & dịch vụ', 0),                 -- ID: 2
    ('Phần mềm', NULL, N'Các công cụ phần mềm chuyên dụng cho kinh doanh online', 0),   -- ID: 3
    ('Tăng tương tác', NULL, N'Các dịch vụ tăng engagement & tương tác trên mạng xã hội', 0),  -- ID: 4
    ('Dịch vụ phần mềm', NULL, N'Công cụ, plugin và dịch vụ lập trình', 0),             -- ID: 5
    ('Blockchain', NULL, N'Các sản phẩm tiền ảo, NFT và blockchain', 0);                -- ID: 6

PRINT N'✓ Đã insert 6 danh mục cha';
GO

-- 4. Insert Danh mục con (Child Categories) - gắn với parent_id

-- Con của "Email" (parent_id = 1)
INSERT INTO Categories (name, parent_id, description, isDelete)
VALUES
    ('Gmail', 1, N'Tài khoản Gmail và G Suite', 0),
    ('HotMail', 1, N'Tài khoản Hotmail/Outlook Mail', 0),
    ('OutlookMail', 1, N'Email Outlook chính thức', 0),
    ('RuMail', 1, N'Tài khoản mail từ các trang Nga', 0),
    ('DomainMail', 1, N'Email tên miền riêng', 0),
    ('YahooMail', 1, N'Tài khoản Yahoo Mail', 0),
    ('ProtonMail', 1, N'Email bảo mật ProtonMail', 0),
    ('Loại Mail Khác', 1, N'Các loại email khác', 0);

PRINT N'✓ Đã insert 8 danh mục con của "Email"';
GO

-- Con của "Tài khoản" (parent_id = 2)
INSERT INTO Categories (name, parent_id, description, isDelete)
VALUES
    ('Tài khoản FB', 2, N'Tài khoản Facebook cá nhân & fanpage', 0),
    ('Tài Khoản BM', 2, N'Tài khoản Business Manager Facebook', 0),
    ('Tài Khoản Zalo', 2, N'Tài khoản Zalo OA & cá nhân', 0),
    ('Tài Khoản Twitter', 2, N'Tài khoản Twitter/X với followers', 0),
    ('Tài Khoản Telegram', 2, N'Tài khoản Telegram Group & Channel', 0),
    ('Tài Khoản Instagram', 2, N'Tài khoản Instagram với followers', 0),
    ('Tài Khoản Shopee', 2, N'Tài khoản Shopee bán hàng', 0),
    ('Tài Khoản Discord', 2, N'Tài khoản Discord với server', 0),
    ('Tài Khoản TikTok', 2, N'Tài khoản TikTok với followers', 0),
    ('Key Diệt Virus', 2, N'Key phần mềm diệt virus chính hãng', 0),
    ('Tài Khoản Capcut', 2, N'Tài khoản Capcut Pro', 0),
    ('Key Window', 2, N'Key Windows & Office chính hãng', 0),
    ('Tài Khoản Khác', 2, N'Các tài khoản khác', 0);

PRINT N'✓ Đã insert 13 danh mục con của "Tài khoản"';
GO

-- Con của "Phần mềm" (parent_id = 3)
INSERT INTO Categories (name, parent_id, description, isDelete)
VALUES
    ('Phần Mềm FB', 3, N'Tool & phần mềm quản lý Facebook', 0),
    ('Phần Mềm Google', 3, N'Công cụ Google Ads, SEO, Analytics', 0),
    ('Phần Mềm Youtube', 3, N'Tool quản lý & tối ưu hóa Youtube', 0),
    ('Phần Mềm Tiền Ảo', 3, N'Software trading & quản lý crypto', 0),
    ('Phần Mềm PTC', 3, N'Phần mềm kiếm tiền PTC tự động', 0),
    ('Phần Mềm Captcha', 3, N'Giải captcha tự động 2captcha, Anti-captcha', 0),
    ('Phần Mềm Offer', 3, N'Tool kiếm tiền từ Offer Wall', 0),
    ('Phần Mềm PTU', 3, N'Phần mềm quản lý PTU (Paid Task)', 0),
    ('Phần Mềm Khác', 3, N'Các phần mềm khác', 0);

PRINT N'✓ Đã insert 9 danh mục con của "Phần mềm"';
GO

-- Con của "Tăng tương tác" (parent_id = 4)
INSERT INTO Categories (name, parent_id, description, isDelete)
VALUES
    ('Dịch vụ Facebook', 4, N'Tăng like, follow, bình luận Facebook', 0),
    ('Dịch vụ Tiktok', 4, N'Tăng view, like, follow TikTok', 0),
    ('Dịch vụ Google', 4, N'Dịch vụ SEO & tối ưu Google', 0),
    ('Dịch vụ Telegram', 4, N'Tăng member Telegram Channel/Group', 0),
    ('Dịch vụ Shopee', 4, N'Tăng view, mua hàng Shopee', 0),
    ('Dịch vụ Discord', 4, N'Tăng member Discord server', 0),
    ('Dịch vụ Twitter', 4, N'Tăng follower, retweet Twitter', 0),
    ('Dịch vụ Youtube', 4, N'Tăng view, subcriber Youtube', 0),
    ('Dịch vụ Zalo', 4, N'Tăng member Zalo OA & tương tác', 0),
    ('Dịch vụ Instagram', 4, N'Tăng follow, like Instagram', 0),
    ('Tương tác khác', 4, N'Các dịch vụ tương tác khác', 0);

PRINT N'✓ Đã insert 11 danh mục con của "Tăng tương tác"';
GO

-- Con của "Dịch vụ phần mềm" (parent_id = 5)
INSERT INTO Categories (name, parent_id, description, isDelete)
VALUES
    ('Tool MMO', 5, N'Công cụ MMO marketing tự động', 0),
    ('Tool Facebook', 5, N'Tool chuyên biệt Facebook', 0),
    ('Tool Google', 5, N'Tool kiếm tiền Google', 0),
    ('Tool Youtube', 5, N'Bot & công cụ Youtube', 0),
    ('Tool TikTok', 5, N'Công cụ TikTok automation', 0),
    ('Tool Instagram', 5, N'Bot Instagram & follow automation', 0),
    ('Đồ họa - Design', 5, N'Công cụ thiết kế đồ họa', 0),
    ('Video Editor', 5, N'Phần mềm chỉnh sửa video', 0),
    ('Plugin & Extension', 5, N'Plugin browser & extension hữu ích', 0),
    ('Script & Bot', 5, N'Script tự động hóa & bot công việc', 0),
    ('Phần mềm khác', 5, N'Các phần mềm khác', 0);

PRINT N'✓ Đã insert 11 danh mục con của "Dịch vụ phần mềm"';
GO

-- Con của "Blockchain" (parent_id = 6)
INSERT INTO Categories (name, parent_id, description, isDelete)
VALUES
    ('Tiền ảo - Crypto', 6, N'Bitcoin, Ethereum, Altcoin khác', 0),
    ('NFT', 6, N'Token NFT và digital art', 0),
    ('Coinlist', 6, N'Coinlist & các IDO token mới', 0),
    ('Airdrop', 6, N'Airdrop token & chiến dịch phát free', 0),
    ('Ví điện tử', 6, N'Ví tiền điện tử & wallet', 0),
    ('Tài khoản sàn', 6, N'Tài khoản Binance, Bybit, OKX, v.v', 0),
    ('Blockchain khác', 6, N'Các sản phẩm blockchain khác', 0);

PRINT N'✓ Đã insert 7 danh mục con của "Blockchain"';
GO

-- 5. Kiểm tra kết quả insert
PRINT N'========================================';
PRINT N'✓ CẬP NHẬT CATEGORIES HOÀN THİNH SAY!';
PRINT N'========================================';

SELECT
    id,
    name,
    parent_id,
    CASE WHEN parent_id IS NULL THEN N'Danh mục Cha' ELSE N'Danh mục Con' END AS [Loại],
    created_at
FROM Categories
WHERE isDelete = 0
ORDER BY parent_id, id;

-- Thống kê
DECLARE @TotalParent INT = (SELECT COUNT(*) FROM Categories WHERE parent_id IS NULL AND isDelete = 0);
DECLARE @TotalChild INT = (SELECT COUNT(*) FROM Categories WHERE parent_id IS NOT NULL AND isDelete = 0);
DECLARE @GrandTotal INT = (SELECT COUNT(*) FROM Categories WHERE isDelete = 0);

PRINT N'';
PRINT N'📊 THỐNG KÊ:';
PRINT N'  - Danh mục cha: ' + CAST(@TotalParent AS VARCHAR(5));
PRINT N'  - Danh mục con: ' + CAST(@TotalChild AS VARCHAR(5));
PRINT N'  - Tổng cộng: ' + CAST(@GrandTotal AS VARCHAR(5));
GO

