-- ==============================================================================
-- KHÔI PHỤC BẢNG CATEGORIES VỚI SCHEMA CHÍNH XÁC
-- Tên tệp: RESTORE_Categories_Fresh.sql
-- Mô tả: Xóa bảng cũ, tạo mới với parent_id, insert dữ liệu phân cấp
-- ==============================================================================

USE MMO_System;
GO

-- 1. Xóa ràng buộc khóa ngoại từ các bảng liên quan (nếu có)
BEGIN TRY
    -- Xóa từ Products nếu tồn tại FK
    IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE CONSTRAINT_NAME = 'FK_Products_Category')
    BEGIN
        ALTER TABLE Products DROP CONSTRAINT FK_Products_Category;
    END

    -- Xóa từ Categories nếu tồn tại self-reference
    IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE CONSTRAINT_NAME = 'FK_Category_Parent')
    BEGIN
        ALTER TABLE Categories DROP CONSTRAINT FK_Category_Parent;
    END
END TRY
BEGIN CATCH
    PRINT N'⚠ Không tìm thấy FK cũ (bỏ qua)';
END CATCH
GO

-- 2. Xóa bảng Categories nếu tồn tại
IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'Categories')
BEGIN
    DROP TABLE Categories;
    PRINT N'✓ Đã xóa bảng Categories cũ';
END
GO

-- 3. Tạo bảng Categories MỚI với schema đúng
CREATE TABLE Categories (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(100) NOT NULL,
    parent_id BIGINT NULL,
    description NVARCHAR(500),
    created_at DATETIME DEFAULT GETDATE(),
    updated_at DATETIME DEFAULT GETDATE(),
    isDelete BIT DEFAULT 0
);
GO

PRINT N'✓ Bảng Categories mới đã được tạo';
GO

-- 4. Thêm Foreign Key cho self-reference (parent_id)
ALTER TABLE Categories
ADD CONSTRAINT FK_Category_Parent FOREIGN KEY (parent_id) REFERENCES Categories(id) ON DELETE NO ACTION;
GO

PRINT N'✓ Foreign Key FK_Category_Parent đã được thêm';
GO

-- 5. Tạo Index cho parent_id để cải thiện hiệu suất
CREATE INDEX IDX_Categories_ParentId ON Categories(parent_id);
GO

PRINT N'✓ Index IDX_Categories_ParentId đã được tạo';
GO

-- 6. INSERT DANH MỤC PHA (6 categories)
-- Lưu ý: Tất cả giá trị isDelete PHẢI là 0
PRINT N'';
PRINT N'▶ Inserting parent categories...';

INSERT INTO Categories (name, parent_id, description, isDelete)
VALUES
    (N'Email', NULL, N'Các dịch vụ email và tài khoản mail', 0),
    (N'Tài khoản', NULL, N'Tài khoản các nền tảng xã hội & dịch vụ', 0),
    (N'Phần mềm', NULL, N'Các công cụ phần mềm chuyên dụng cho kinh doanh online', 0),
    (N'Tăng tương tác', NULL, N'Các dịch vụ tăng engagement & tương tác trên mạng xã hội', 0),
    (N'Dịch vụ phần mềm', NULL, N'Công cụ, plugin và dịch vụ lập trình', 0),
    (N'Blockchain', NULL, N'Các sản phẩm tiền ảo, NFT và blockchain', 0);

PRINT N'✓ Đã insert 6 danh mục cha';
GO

-- 7. INSERT DANH MỤC CON - Con của "Email" (parent_id = 1)
INSERT INTO Categories (name, parent_id, description, isDelete)
VALUES
    (N'Gmail', 1, N'Tài khoản Gmail và G Suite', 0),
    (N'HotMail', 1, N'Tài khoản Hotmail/Outlook Mail', 0),
    (N'OutlookMail', 1, N'Email Outlook chính thức', 0),
    (N'RuMail', 1, N'Tài khoản mail từ các trang Nga', 0),
    (N'DomainMail', 1, N'Email tên miền riêng', 0),
    (N'YahooMail', 1, N'Tài khoản Yahoo Mail', 0),
    (N'ProtonMail', 1, N'Email bảo mật ProtonMail', 0),
    (N'Loại Mail Khác', 1, N'Các loại email khác', 0);

PRINT N'✓ Đã insert 8 danh mục con của "Email"';
GO

-- 8. Con của "Tài khoản" (parent_id = 2)
INSERT INTO Categories (name, parent_id, description, isDelete)
VALUES
    (N'Tài khoản FB', 2, N'Tài khoản Facebook cá nhân & fanpage', 0),
    (N'Tài Khoản BM', 2, N'Tài khoản Business Manager Facebook', 0),
    (N'Tài Khoản Zalo', 2, N'Tài khoản Zalo OA & cá nhân', 0),
    (N'Tài Khoản Twitter', 2, N'Tài khoản Twitter/X với followers', 0),
    (N'Tài Khoản Telegram', 2, N'Tài khoản Telegram Group & Channel', 0),
    (N'Tài Khoản Instagram', 2, N'Tài khoản Instagram với followers', 0),
    (N'Tài Khoản Shopee', 2, N'Tài khoản Shopee bán hàng', 0),
    (N'Tài Khoản Discord', 2, N'Tài khoản Discord với server', 0),
    (N'Tài Khoản TikTok', 2, N'Tài khoản TikTok với followers', 0),
    (N'Key Diệt Virus', 2, N'Key phần mềm diệt virus chính hãng', 0),
    (N'Tài Khoản Capcut', 2, N'Tài khoản Capcut Pro', 0),
    (N'Key Window', 2, N'Key Windows & Office chính hãng', 0),
    (N'Tài Khoản Khác', 2, N'Các tài khoản khác', 0);

PRINT N'✓ Đã insert 13 danh mục con của "Tài khoản"';
GO

-- 9. Con của "Phần mềm" (parent_id = 3)
INSERT INTO Categories (name, parent_id, description, isDelete)
VALUES
    (N'Phần Mềm FB', 3, N'Tool & phần mềm quản lý Facebook', 0),
    (N'Phần Mềm Google', 3, N'Công cụ Google Ads, SEO, Analytics', 0),
    (N'Phần Mềm Youtube', 3, N'Tool quản lý & tối ưu hóa Youtube', 0),
    (N'Phần Mềm Tiền Ảo', 3, N'Software trading & quản lý crypto', 0),
    (N'Phần Mềm PTC', 3, N'Phần mềm kiếm tiền PTC tự động', 0),
    (N'Phần Mềm Captcha', 3, N'Giải captcha tự động 2captcha, Anti-captcha', 0),
    (N'Phần Mềm Offer', 3, N'Tool kiếm tiền từ Offer Wall', 0),
    (N'Phần Mềm PTU', 3, N'Phần mềm quản lý PTU (Paid Task)', 0),
    (N'Phần Mềm Khác', 3, N'Các phần mềm khác', 0);

PRINT N'✓ Đã insert 9 danh mục con của "Phần mềm"';
GO

-- 10. Con của "Tăng tương tác" (parent_id = 4)
INSERT INTO Categories (name, parent_id, description, isDelete)
VALUES
    (N'Dịch vụ Facebook', 4, N'Tăng like, follow, bình luận Facebook', 0),
    (N'Dịch vụ Tiktok', 4, N'Tăng view, like, follow TikTok', 0),
    (N'Dịch vụ Google', 4, N'Dịch vụ SEO & tối ưu Google', 0),
    (N'Dịch vụ Telegram', 4, N'Tăng member Telegram Channel/Group', 0),
    (N'Dịch vụ Shopee', 4, N'Tăng view, mua hàng Shopee', 0),
    (N'Dịch vụ Discord', 4, N'Tăng member Discord server', 0),
    (N'Dịch vụ Twitter', 4, N'Tăng follower, retweet Twitter', 0),
    (N'Dịch vụ Youtube', 4, N'Tăng view, subcriber Youtube', 0),
    (N'Dịch vụ Zalo', 4, N'Tăng member Zalo OA & tương tác', 0),
    (N'Dịch vụ Instagram', 4, N'Tăng follow, like Instagram', 0),
    (N'Tương tác khác', 4, N'Các dịch vụ tương tác khác', 0);

PRINT N'✓ Đã insert 11 danh mục con của "Tăng tương tác"';
GO

-- 11. Con của "Dịch vụ phần mềm" (parent_id = 5)
INSERT INTO Categories (name, parent_id, description, isDelete)
VALUES
    (N'Tool MMO', 5, N'Công cụ MMO marketing tự động', 0),
    (N'Tool Facebook', 5, N'Tool chuyên biệt Facebook', 0),
    (N'Tool Google', 5, N'Tool kiếm tiền Google', 0),
    (N'Tool Youtube', 5, N'Bot & công cụ Youtube', 0),
    (N'Tool TikTok', 5, N'Công cụ TikTok automation', 0),
    (N'Tool Instagram', 5, N'Bot Instagram & follow automation', 0),
    (N'Đồ họa - Design', 5, N'Công cụ thiết kế đồ họa', 0),
    (N'Video Editor', 5, N'Phần mềm chỉnh sửa video', 0),
    (N'Plugin & Extension', 5, N'Plugin browser & extension hữu ích', 0),
    (N'Script & Bot', 5, N'Script tự động hóa & bot công việc', 0),
    (N'Phần mềm khác', 5, N'Các phần mềm khác', 0);

PRINT N'✓ Đã insert 11 danh mục con của "Dịch vụ phần mềm"';
GO

-- 12. Con của "Blockchain" (parent_id = 6)
INSERT INTO Categories (name, parent_id, description, isDelete)
VALUES
    (N'Tiền ảo - Crypto', 6, N'Bitcoin, Ethereum, Altcoin khác', 0),
    (N'NFT', 6, N'Token NFT và digital art', 0),
    (N'Coinlist', 6, N'Coinlist & các IDO token mới', 0),
    (N'Airdrop', 6, N'Airdrop token & chiến dịch phát free', 0),
    (N'Ví điện tử', 6, N'Ví tiền điện tử & wallet', 0),
    (N'Tài khoản sàn', 6, N'Tài khoản Binance, Bybit, OKX, v.v', 0),
    (N'Blockchain khác', 6, N'Các sản phẩm blockchain khác', 0);

PRINT N'✓ Đã insert 7 danh mục con của "Blockchain"';
GO

-- 13. Kiểm tra kết quả
PRINT N'';
PRINT N'========================================';
PRINT N'✓ CẬP NHẬT CATEGORIES HOÀN THÀNH!';
PRINT N'========================================';
PRINT N'';

-- Hiển thị các danh mục cha
PRINT N'📋 DANH MỤC CHA (Parent Categories):';
SELECT
    id,
    name AS [Tên danh mục],
    description AS [Mô tả]
FROM Categories
WHERE parent_id IS NULL AND isDelete = 0
ORDER BY id;

PRINT N'';
PRINT N'📊 THỐNG KÊ SỐ LIỆU:';

DECLARE @TotalParent INT = (SELECT COUNT(*) FROM Categories WHERE parent_id IS NULL AND isDelete = 0);
DECLARE @TotalChild INT = (SELECT COUNT(*) FROM Categories WHERE parent_id IS NOT NULL AND isDelete = 0);
DECLARE @GrandTotal INT = (SELECT COUNT(*) FROM Categories WHERE isDelete = 0);

PRINT N'  ✓ Danh mục cha: ' + CAST(@TotalParent AS VARCHAR(5));
PRINT N'  ✓ Danh mục con: ' + CAST(@TotalChild AS VARCHAR(5));
PRINT N'  ✓ Tổng cộng: ' + CAST(@GrandTotal AS VARCHAR(5));

PRINT N'';
PRINT N'✅ Tất cả dữ liệu đã được insert thành công!';
PRINT N'';

-- Kiểm tra chi tiết full tree
PRINT N'📂 FULL DANH MỤC TREE:';
SELECT
    id,
    name AS [Tên],
    parent_id AS [Parent ID],
    CASE
        WHEN parent_id IS NULL THEN '├─ [DANH MỤC CHA]'
        ELSE '  └─ [Con]'
    END AS [Loại],
    created_at AS [Ngày tạo]
FROM Categories
WHERE isDelete = 0
ORDER BY parent_id, id;
GO

