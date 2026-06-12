-- ==============================================================================
-- KHỞI TẠO DỮ LIỆU SẢN PHẨM MẪU CHUẨN (UTF-8)
-- Tên tệp: SEED_Products_Fresh.sql
-- Mô tả: Xóa sản phẩm cũ, cập nhật thông tin người bán (IDs 1-12), chèn sản phẩm chuẩn
-- ==============================================================================

USE MMO_System;
GO

-- BƯỚC 1: Xóa sản phẩm và biến thể cũ để tránh xung đột
PRINT N'🧹 Đang xóa sản phẩm và biến thể cũ...';
DELETE FROM ProductVariants;
DELETE FROM Products;
GO

-- BƯỚC 2: Cập nhật / Đồng bộ thông tin Users (Người bán) cho IDs 1-12
PRINT N'👤 Đang cập nhật thông tin người bán (IDs 1-12)...';

-- Tạo mật khẩu băm mặc định (match với '123456' của Spring Security)
DECLARE @PasswordHash VARCHAR(255) = '$2y$10$uRFrFYP.Ld32A.LSRCm10.LlUfbJH7vgelUd4h1dsvZCyMKz1c0Bq';

UPDATE Users
SET email = 'netflixvn@mmo.com', password = @PasswordHash, full_name = N'NetflixVN Store', role = '{"role": "Seller"}', shop_status = 'Approved', isVerified = 1, isDelete = 0
WHERE id = 1;

UPDATE Users
SET email = 'netflixvip@mmo.com', password = @PasswordHash, full_name = N'Netflix_Vip_Store', role = '{"role": "Seller"}', shop_status = 'Approved', isVerified = 1, isDelete = 0
WHERE id = 2;

UPDATE Users
SET email = 'aihelper@mmo.com', password = @PasswordHash, full_name = N'AI_Helper Store', role = '{"role": "Seller"}', shop_status = 'Approved', isVerified = 1, isDelete = 0
WHERE id = 3;

UPDATE Users
SET email = 'musiclovers@mmo.com', password = @PasswordHash, full_name = N'MusicLovers Store', role = '{"role": "Seller"}', shop_status = 'Approved', isVerified = 1, isDelete = 0
WHERE id = 4;

UPDATE Users
SET email = 'microsoft@mmo.com', password = @PasswordHash, full_name = N'Microsoft_Reseller Store', role = '{"role": "Seller"}', shop_status = 'Approved', isVerified = 1, isDelete = 0
WHERE id = 5;

UPDATE Users
SET email = 'redpremium@mmo.com', password = @PasswordHash, full_name = N'RedPremium Store', role = '{"role": "Seller"}', shop_status = 'Approved', isVerified = 1, isDelete = 0
WHERE id = 6;

UPDATE Users
SET email = 'canvapro@mmo.com', password = @PasswordHash, full_name = N'CanvaPro Store', role = '{"role": "Seller"}', shop_status = 'Approved', isVerified = 1, isDelete = 0
WHERE id = 7;

UPDATE Users
SET email = 'gmailpro@mmo.com', password = @PasswordHash, full_name = N'GmailPro Store', role = '{"role": "Seller"}', shop_status = 'Approved', isVerified = 1, isDelete = 0
WHERE id = 8;

UPDATE Users
SET email = 'mmocoder@mmo.com', password = @PasswordHash, full_name = N'MMO_Coder Store', role = '{"role": "Seller"}', shop_status = 'Approved', isVerified = 1, isDelete = 0
WHERE id = 9;

UPDATE Users
SET email = 'securenet@mmo.com', password = @PasswordHash, full_name = N'SecureNet Store', role = '{"role": "Seller"}', shop_status = 'Approved', isVerified = 1, isDelete = 0
WHERE id = 10;

UPDATE Users
SET email = 'socialmediaup@mmo.com', password = @PasswordHash, full_name = N'SocialMediaUp Store', role = '{"role": "Seller"}', shop_status = 'Approved', isVerified = 1, isDelete = 0
WHERE id = 11;

UPDATE Users
SET email = 'bannerdesign@mmo.com', password = @PasswordHash, full_name = N'BannerDesign Store', role = '{"role": "Seller"}', shop_status = 'Approved', isVerified = 1, isDelete = 0
WHERE id = 12;

PRINT N'✓ Đã cập nhật xong thông tin 12 người bán.';
GO

-- BƯỚC 3: Chèn sản phẩm với ID cố định (1-13) để khớp với PDP Frontend
PRINT N'📦 Đang chèn sản phẩm mẫu...';
SET IDENTITY_INSERT Products ON;

-- Product 1 (NetflixVN Store - ID 1, Category 27: Tài khoản Khác)
INSERT INTO Products (id, seller_id, category_id, name, description, image, isDelete, is_delete)
VALUES (1, 1, 27, N'Tài khoản Netflix Premium 4K UHD 1 Tháng (Xem riêng 1 thiết bị, bảo hành 1 đổi 1)', 
        N'Xem phim chất lượng Ultra HD 4K trên mọi thiết bị. Giao tài khoản tự động lập tức sau khi thanh toán. Bảo hành 1 đổi 1 suốt thời gian sử dụng.', 
        'https://via.placeholder.com/300x160/fd761a/ffffff?text=Netflix+Premium', 0, 0);

-- Product 2 (Netflix_Vip_Store - ID 2, Category 27: Tài khoản Khác)
INSERT INTO Products (id, seller_id, category_id, name, description, image, isDelete, is_delete)
VALUES (2, 2, 27, N'Tài khoản Netflix Premium 4K UHD Gói 1 Năm (Chính chủ gia hạn ổn định)', 
        N'Gói cước Netflix Premium 12 tháng xem ổn định không lo bị khóa hay đăng xuất. Hỗ trợ xem trên SmartTV, điện thoại, máy tính.', 
        'https://via.placeholder.com/300x160/fd761a/ffffff?text=Netflix+1Year', 0, 0);

-- Product 3 (AI_Helper Store - ID 3, Category 27: Tài khoản Khác)
INSERT INTO Products (id, seller_id, category_id, name, description, image, isDelete, is_delete)
VALUES (3, 3, 27, N'Tài khoản ChatGPT Plus (OpenAI GPT-4o) Chính Chủ Sẵn 20$ Hạn 1 Tháng', 
        N'Tài khoản OpenAI nâng cấp sẵn gói Plus trị giá 20$. Sử dụng GPT-4o không giới hạn tốc độ và tính năng mới nhất.', 
        'https://via.placeholder.com/300x160/fd761a/ffffff?text=ChatGPT+Plus', 0, 0);

-- Product 4 (MusicLovers Store - ID 4, Category 27: Tài khoản Khác)
INSERT INTO Products (id, seller_id, category_id, name, description, image, isDelete, is_delete)
VALUES (4, 4, 27, N'Spotify Premium 1 Năm Giá Siêu Rẻ (Nâng cấp Family email của bạn)', 
        N'Nghe nhạc chất lượng cao không quảng cáo trên Spotify. Nâng cấp trực tiếp trên email cá nhân của bạn thông qua liên kết Family.', 
        'https://via.placeholder.com/300x160/fd761a/ffffff?text=Spotify+Premium', 0, 0);

-- Product 5 (Microsoft_Reseller Store - ID 5, Category 26: Key Window)
INSERT INTO Products (id, seller_id, category_id, name, description, image, isDelete, is_delete)
VALUES (5, 5, 26, N'Key Windows 11 Pro Bản Quyền Vĩnh Viễn (Kèm hướng dẫn active chi tiết)', 
        N'Kích hoạt bản quyền Windows 11 Professional vĩnh viễn theo máy. Hỗ trợ cập nhật đầy đủ, cài đặt lại Win vẫn giữ bản quyền.', 
        'https://via.placeholder.com/300x160/fd761a/ffffff?text=Windows+11+Key', 0, 0);

-- Product 6 (RedPremium Store - ID 6, Category 27: Tài khoản Khác)
INSERT INTO Products (id, seller_id, category_id, name, description, image, isDelete, is_delete)
VALUES (6, 6, 27, N'Youtube Premium Không Quảng Cáo 6 Tháng (Add Family bao chạy mượt)', 
        N'Xem video Youtube không quảng cáo, hỗ trợ phát nhạc trong nền và tải xuống offline. Nâng cấp tài khoản chính chủ qua Family group.', 
        'https://via.placeholder.com/300x160/fd761a/ffffff?text=Youtube+Premium', 0, 0);

-- Product 7 (CanvaPro Store - ID 7, Category 36: Phần Mềm Khác)
INSERT INTO Products (id, seller_id, category_id, name, description, image, isDelete, is_delete)
VALUES (7, 7, 36, N'Gói Tài Khoản Canva Pro Thiết Kế 1 Năm Trọn Gói', 
        N'Mở khóa toàn bộ tính năng Canva Pro: hàng triệu ảnh, video, font chữ cao cấp và công cụ xóa nền thông minh 1-click.', 
        'https://via.placeholder.com/300x160/fd761a/ffffff?text=Canva+Pro', 0, 0);

-- Product 8 (GmailPro Store - ID 8, Category 7: Gmail)
INSERT INTO Products (id, seller_id, category_id, name, description, image, isDelete, is_delete)
VALUES (8, 8, 7, N'Combo 10 Gmail Việt Cổ 2018 - 2020 Cực Sạch Có Sẵn Kênh Youtube', 
        N'Tài khoản Gmail Việt Nam đăng ký từ năm 2018-2020 cực kỳ uy tín, độ trust cao, phù hợp chạy quảng cáo hoặc làm kênh MMO.', 
        'https://via.placeholder.com/300x160/fd761a/ffffff?text=Gmail+Co', 0, 0);

-- Product 9 (MMO_Coder Store - ID 9, Category 49: Tool Facebook)
INSERT INTO Products (id, seller_id, category_id, name, description, image, isDelete, is_delete)
VALUES (9, 9, 49, N'Tool Nuôi Nick Facebook Auto Like Post Share Độc Quyền', 
        N'Phần mềm tự động tương tác nick Facebook, nuôi tài khoản số lượng lớn, tự động đi seeding, share bài viết hàng loạt an toàn.', 
        'https://via.placeholder.com/300x160/fd761a/ffffff?text=FB+Nuoi+Nick+Tool', 0, 0);

-- Product 10 (SecureNet Store - ID 10, Category 27: Tài khoản Khác)
INSERT INTO Products (id, seller_id, category_id, name, description, image, isDelete, is_delete)
VALUES (10, 10, 27, N'Tài khoản NordVPN Premium 1 Năm Bảo Mật Mã Hóa Cao', 
        N'Dịch vụ mạng riêng ảo (VPN) bảo mật hàng đầu thế giới. Mã hóa dữ liệu duyệt web, truy cập website bị chặn với tốc độ cao.', 
        'https://via.placeholder.com/300x160/fd761a/ffffff?text=NordVPN+1Year', 0, 0);

-- Product 11 (SocialMediaUp Store - ID 11, Category 38: Dịch vụ Tiktok)
INSERT INTO Products (id, seller_id, category_id, name, description, image, isDelete, is_delete)
VALUES (11, 11, 38, N'Tăng 1000 Follower Thật TikTok Việt Tốc Độ Nhanh Tự Nhiên', 
        N'Dịch vụ tăng 1000 lượt theo dõi thật cho tài khoản TikTok Việt Nam. Đảm bảo an toàn 100% cho tài khoản, hỗ trợ bật livestream.', 
        'https://via.placeholder.com/300x160/fd761a/ffffff?text=TikTok+Followers', 0, 0);

-- Product 12 (Netflix_Vip_Store - ID 12, Category 63: Ví diên tử)
INSERT INTO Products (id, seller_id, category_id, name, description, image, isDelete, is_delete)
VALUES (12, 2, 63, N'Ví Điện Tử Trust Wallet Hạn Cổ Có Sẵn Cụm Từ Bảo Mật', 
        N'Ví tiền điện tử Trust Wallet được tạo từ lâu, đi kèm 12 ký tự bảo mật (seed phrase) sạch sẽ, thích hợp chứa tài sản số hoặc giao dịch.', 
        'https://via.placeholder.com/300x160/fd761a/ffffff?text=Trust+Wallet', 0, 0);

-- Product 13 (BannerDesign Store - ID 13, Category 54: Đồ họa - Design)
INSERT INTO Products (id, seller_id, category_id, name, description, image, isDelete, is_delete)
VALUES (13, 12, 54, N'Gói Dịch Vụ Thiết Kế Logo & Banner Chuyên Nghiệp (Không thuộc danh mục chính)', 
        N'Thiết kế bộ nhận diện thương hiệu cơ bản bao gồm 1 logo và 1 banner facebook/website chuyên nghiệp theo đúng yêu cầu.', 
        'https://via.placeholder.com/300x160/fd761a/ffffff?text=Logo+Design', 0, 0);

SET IDENTITY_INSERT Products OFF;
PRINT N'✓ Đã chèn xong 13 sản phẩm mẫu.';
GO

-- BƯỚC 4: Chèn Biến thể sản phẩm (Product Variants)
PRINT N'📝 Đang chèn biến thể sản phẩm...';

-- Product 1
INSERT INTO ProductVariants (product_id, variant_name, price_vnd, stock, status, isDelete)
VALUES (1, N'Netflix Premium 1 Tháng (Shared)', 65000, 156, 'Active', 0);

-- Product 2
INSERT INTO ProductVariants (product_id, variant_name, price_vnd, stock, status, isDelete)
VALUES (2, N'Netflix Premium 1 Năm (Chính Chủ)', 650000, 42, 'Active', 0);

-- Product 3
INSERT INTO ProductVariants (product_id, variant_name, price_vnd, stock, status, isDelete)
VALUES (3, N'ChatGPT Plus 1 Tháng', 150000, 89, 'Active', 0);

-- Product 4
INSERT INTO ProductVariants (product_id, variant_name, price_vnd, stock, status, isDelete)
VALUES (4, N'Spotify Premium 1 Năm', 250000, 45, 'Active', 0);

-- Product 5
INSERT INTO ProductVariants (product_id, variant_name, price_vnd, stock, status, isDelete)
VALUES (5, N'Windows 11 Pro Key', 99000, 999, 'Active', 0);

-- Product 6
INSERT INTO ProductVariants (product_id, variant_name, price_vnd, stock, status, isDelete)
VALUES (6, N'Youtube Premium 6 Tháng', 120000, 230, 'Active', 0);

-- Product 7
INSERT INTO ProductVariants (product_id, variant_name, price_vnd, stock, status, isDelete)
VALUES (7, N'Canva Pro 1 Năm', 180000, 120, 'Active', 0);

-- Product 8
INSERT INTO ProductVariants (product_id, variant_name, price_vnd, stock, status, isDelete)
VALUES (8, N'Combo 10 Gmail', 35000, 500, 'Active', 0);

-- Product 9
INSERT INTO ProductVariants (product_id, variant_name, price_vnd, stock, status, isDelete)
VALUES (9, N'Tool Nuôi Nick FB Vĩnh Viễn', 850000, 75, 'Active', 0);

-- Product 10
INSERT INTO ProductVariants (product_id, variant_name, price_vnd, stock, status, isDelete)
VALUES (10, N'NordVPN Premium 1 Năm', 350000, 15, 'Active', 0);

-- Product 11
INSERT INTO ProductVariants (product_id, variant_name, price_vnd, stock, status, isDelete)
VALUES (11, N'TikTok 1000 Follower', 95000, 9999, 'Active', 0);

-- Product 12 (Hết hàng - stock = 0)
INSERT INTO ProductVariants (product_id, variant_name, price_vnd, stock, status, isDelete)
VALUES (12, N'Trust Wallet Cổ', 150000, 0, 'Active', 0);

-- Product 13
INSERT INTO ProductVariants (product_id, variant_name, price_vnd, stock, status, isDelete)
VALUES (13, N'Gói Thiết Kế Banner Logo', 299000, 50, 'Active', 0);

PRINT N'✓ Đã chèn xong biến thể.';
GO

-- BƯỚC 5: Kiểm tra kết quả
PRINT N'';
PRINT N'========================================';
PRINT N'✓ KHỞI TẠO DỮ LIỆU SẢN PHẨM HOÀN THÀNH!';
PRINT N'========================================';
SELECT id, name, seller_id, category_id FROM Products ORDER BY id;
GO
