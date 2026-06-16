-- ==============================================================================
-- SQL SEED SCRIPT: SEED MOCK DATA FOR SELLER PORTAL
-- File name: SEED_SellerMockData.sql
-- Description: Inserts sample bank info, 5 products + variants, 5 transactions,
--              5 withdrawals, 5 complaints, 5 chats, 5 reviews, and 5 flags for testing.
-- ==============================================================================

USE MMO_System;
GO

PRINT '========================================';
PRINT '🔄 STARTING SELLER MOCK DATA SEEDING...';
PRINT '========================================';

-- 1. Ensure seller01@gmail.com is Active and has a role of 'Seller'
DECLARE @SellerEmail VARCHAR(255) = 'seller01@gmail.com';
DECLARE @SellerId BIGINT;

IF EXISTS (SELECT 1 FROM Users WHERE email = @SellerEmail)
BEGIN
    UPDATE Users 
    SET shop_status = 'Active',
        role = '{"role": "Seller"}',
        balance_vnd = 25000000 -- Initial balance for withdrawals testing
    WHERE email = @SellerEmail;
    
    SET @SellerId = (SELECT id FROM Users WHERE email = @SellerEmail);
    PRINT '✓ Updated user seller01@gmail.com to Active Seller. ID: ' + CAST(@SellerId AS VARCHAR(20));
END
ELSE
BEGIN
    -- Fallback: insert seller
    DECLARE @PasswordHash VARCHAR(255) = '$2y$10$uRFrFYP.Ld32A.LSRCm10.LlUfbJH7vgelUd4h1dsvZCyMKz1c0Bq'; -- bcrypt for 123456
    INSERT INTO Users (email, password, full_name, role, phone, shop_status, balance_vnd, isVerified, isDelete)
    VALUES (@SellerEmail, @PasswordHash, N'Trần Văn Seller', '{"role": "Seller"}', '0902345678', 'Active', 25000000, 1, 0);
    SET @SellerId = SCOPE_IDENTITY();
    PRINT '✓ Created user seller01@gmail.com as Active Seller. ID: ' + CAST(@SellerId AS VARCHAR(20));
END;

-- Ensure we also have a Customer user for testing transactions
DECLARE @CustomerEmail VARCHAR(255) = 'customer01@gmail.com';
DECLARE @CustomerId BIGINT;
IF EXISTS (SELECT 1 FROM Users WHERE email = @CustomerEmail)
BEGIN
    SET @CustomerId = (SELECT id FROM Users WHERE email = @CustomerEmail);
END
ELSE
BEGIN
    DECLARE @PasswordHashCust VARCHAR(255) = '$2y$10$uRFrFYP.Ld32A.LSRCm10.LlUfbJH7vgelUd4h1dsvZCyMKz1c0Bq';
    INSERT INTO Users (email, password, full_name, role, phone, shop_status, balance_vnd, isVerified, isDelete)
    VALUES (@CustomerEmail, @PasswordHashCust, N'Nguyễn Văn Customer', '{"role": "Customer"}', '0901234567', 'Pending', 5000000, 1, 0);
    SET @CustomerId = SCOPE_IDENTITY();
END;
PRINT '✓ Customer User ID: ' + CAST(@CustomerId AS VARCHAR(20));

-- Ensure we have a Staff user for flags and chats
DECLARE @StaffEmail VARCHAR(255) = 'staff01@gmail.com';
DECLARE @StaffId BIGINT;
IF EXISTS (SELECT 1 FROM Users WHERE email = @StaffEmail)
BEGIN
    SET @StaffId = (SELECT id FROM Users WHERE email = @StaffEmail);
END
ELSE
BEGIN
    DECLARE @PasswordHashStaff VARCHAR(255) = '$2y$10$uRFrFYP.Ld32A.LSRCm10.LlUfbJH7vgelUd4h1dsvZCyMKz1c0Bq';
    INSERT INTO Users (email, password, full_name, role, phone, shop_status, balance_vnd, isVerified, isDelete)
    VALUES (@StaffEmail, @PasswordHashStaff, N'Lê Văn Staff', '{"role": "Staff"}', '0903456789', 'Approved', 0, 1, 0);
    SET @StaffId = SCOPE_IDENTITY();
END;
PRINT '✓ Staff User ID: ' + CAST(@StaffId AS VARCHAR(20));

-- 2. Insert Seller Bank Info if not exists
DECLARE @BankInfoId BIGINT;
IF NOT EXISTS (SELECT 1 FROM SellerBankInfo WHERE user_id = @SellerId AND isDelete = 0)
BEGIN
    INSERT INTO SellerBankInfo (user_id, bank_name, account_number, branch, created_at, isDelete)
    VALUES (@SellerId, N'Vietcombank', '0123456789', N'Chi nhánh Hà Nội', GETDATE(), 0);
    SET @BankInfoId = SCOPE_IDENTITY();
    PRINT '✓ Inserted Bank Info for Seller';
END
ELSE
BEGIN
    SET @BankInfoId = (SELECT TOP 1 id FROM SellerBankInfo WHERE user_id = @SellerId AND isDelete = 0);
END;

-- 3. Retrieve some sub-categories for seeding products
DECLARE @CatStreaming BIGINT = (SELECT TOP 1 id FROM Categories WHERE name LIKE N'%Streaming%' OR name LIKE N'%Khác%' AND isDelete = 0);
DECLARE @CatSocial BIGINT = (SELECT TOP 1 id FROM Categories WHERE name LIKE N'%Facebook%' OR name LIKE N'%MXH%' AND isDelete = 0);
DECLARE @CatSoftware BIGINT = (SELECT TOP 1 id FROM Categories WHERE name LIKE N'%Phần mềm%' OR name LIKE N'%Google%' AND isDelete = 0);

IF @CatStreaming IS NULL SET @CatStreaming = 1;
IF @CatSocial IS NULL SET @CatSocial = 1;
IF @CatSoftware IS NULL SET @CatSoftware = 1;

-- 4. Clean up old test data in proper dependency order to avoid foreign key conflicts
DELETE FROM Chats WHERE complaint_id IN (SELECT id FROM Complaints WHERE seller_id = @SellerId) OR sender_id = @SellerId OR receiver_id = @SellerId;
DELETE FROM ShopFlags WHERE seller_id = @SellerId;
DELETE FROM Complaints WHERE seller_id = @SellerId;
DELETE FROM Reviews WHERE product_id IN (SELECT id FROM Products WHERE seller_id = @SellerId);
DELETE FROM WalletTransactions WHERE user_id = @SellerId OR reference_id IN (SELECT id FROM Transactions WHERE seller_id = @SellerId);
DELETE FROM Transactions WHERE seller_id = @SellerId;
DELETE FROM Withdrawals WHERE seller_id = @SellerId;
DELETE FROM ProductVariants WHERE product_id IN (SELECT id FROM Products WHERE seller_id = @SellerId);
DELETE FROM Products WHERE seller_id = @SellerId;
PRINT '✓ Cleaned up all old test data for this seller';


-- 5. Insert 5 Products & Variants
-- Product 1: Netflix Premium
DECLARE @P1 BIGINT;
INSERT INTO Products (seller_id, category_id, name, description, image, created_at, isDelete)
VALUES (@SellerId, @CatStreaming, N'Netflix Premium 4K UHD', N'Tài khoản Netflix Premium hỗ trợ 4K UltraHD, bảo hành trọn đời gói. Xem đồng thời 4 màn hình.', 'https://via.placeholder.com/300x160/fd761a/ffffff?text=Netflix+Premium', GETDATE(), 0);
SET @P1 = SCOPE_IDENTITY();

INSERT INTO ProductVariants (product_id, variant_name, price_vnd, stock, status, created_at, isDelete)
VALUES 
(@P1, N'Gói 1 tháng (1 Profile)', 89000, 50, 'Active', GETDATE(), 0),
(@P1, N'Gói 3 tháng (1 Profile)', 249000, 30, 'Active', GETDATE(), 0),
(@P1, N'Gói 12 tháng (Trọn gói)', 890000, 10, 'Active', GETDATE(), 0);

-- Product 2: Spotify Family
DECLARE @P2 BIGINT;
INSERT INTO Products (seller_id, category_id, name, description, image, created_at, isDelete)
VALUES (@SellerId, @CatStreaming, N'Spotify Premium Gia Đình', N'Tài khoản gia hạn Spotify Premium Family, chính chủ email cá nhân của khách hàng.', 'https://via.placeholder.com/300x160/1db954/ffffff?text=Spotify+Family', GETDATE(), 0);
SET @P2 = SCOPE_IDENTITY();

INSERT INTO ProductVariants (product_id, variant_name, price_vnd, stock, status, created_at, isDelete)
VALUES 
(@P2, N'Gói gia hạn 6 tháng', 120000, 100, 'Active', GETDATE(), 0),
(@P2, N'Gói gia hạn 12 tháng', 220000, 40, 'Active', GETDATE(), 0);

-- Product 3: Canva Pro
DECLARE @P3 BIGINT;
INSERT INTO Products (seller_id, category_id, name, description, image, created_at, isDelete)
VALUES (@SellerId, @CatSoftware, N'Canva Pro Giáo Dục / Cá Nhân', N'Nâng cấp tài khoản Canva Pro chính chủ. Thiết kế không giới hạn, truy cập hàng triệu tài nguyên.', 'https://via.placeholder.com/300x160/00c4cc/ffffff?text=Canva+Pro', GETDATE(), 0);
SET @P3 = SCOPE_IDENTITY();

INSERT INTO ProductVariants (product_id, variant_name, price_vnd, stock, status, created_at, isDelete)
VALUES 
(@P3, N'Nâng cấp 1 năm', 150000, 150, 'Active', GETDATE(), 0),
(@P3, N'Nâng cấp trọn đời', 350000, 0, 'Pending', GETDATE(), 0); -- Out of stock / pending

-- Product 4: ChatGPT Plus
DECLARE @P4 BIGINT;
INSERT INTO Products (seller_id, category_id, name, description, image, created_at, isDelete)
VALUES (@SellerId, @CatSoftware, N'ChatGPT Plus GPT-4', N'Tài khoản ChatGPT Plus dùng chung hoặc riêng tư, kích hoạt sẵn gói Plus 20 USD/tháng.', 'https://via.placeholder.com/300x160/10a37f/ffffff?text=ChatGPT+Plus', GETDATE(), 0);
SET @P4 = SCOPE_IDENTITY();

INSERT INTO ProductVariants (product_id, variant_name, price_vnd, stock, status, created_at, isDelete)
VALUES 
(@P4, N'Dùng chung (Shared 4 người)', 99000, 20, 'Active', GETDATE(), 0),
(@P4, N'Dùng riêng (Private chính chủ)', 499000, 15, 'Active', GETDATE(), 0);

-- Product 5: Tool Auto Nuôi Clone Facebook
DECLARE @P5 BIGINT;
INSERT INTO Products (seller_id, category_id, name, description, image, created_at, isDelete)
VALUES (@SellerId, @CatSocial, N'Tool Auto Nuôi Clone Facebook Pro', N'Phần mềm tự động hóa tương tác Facebook: Like, Share, Seeding group, gửi tin nhắn tự động.', 'https://via.placeholder.com/300x160/1877f2/ffffff?text=FB+Nuoi+Clone', GETDATE(), 0);
SET @P5 = SCOPE_IDENTITY();

INSERT INTO ProductVariants (product_id, variant_name, price_vnd, stock, status, created_at, isDelete)
VALUES 
(@P5, N'Key sử dụng 1 tháng (1 Máy)', 199000, 80, 'Active', GETDATE(), 0),
(@P5, N'Key sử dụng 1 năm (1 Máy)', 999000, 25, 'Active', GETDATE(), 0);

PRINT '✓ Seeded 5 Products and ProductVariants';

-- Retrieve variant IDs for transactions
DECLARE @V1 BIGINT = (SELECT TOP 1 id FROM ProductVariants WHERE product_id = @P1);
DECLARE @V2 BIGINT = (SELECT TOP 1 id FROM ProductVariants WHERE product_id = @P2);
DECLARE @V3 BIGINT = (SELECT TOP 1 id FROM ProductVariants WHERE product_id = @P3);
DECLARE @V4 BIGINT = (SELECT TOP 1 id FROM ProductVariants WHERE product_id = @P4);
DECLARE @V5 BIGINT = (SELECT TOP 1 id FROM ProductVariants WHERE product_id = @P5);

-- 6. Seed 5 Transactions (Sales History)
DECLARE @T1 BIGINT, @T2 BIGINT, @T3 BIGINT, @T4 BIGINT, @T5 BIGINT;

-- Transaction 1: Completed
INSERT INTO Transactions (customer_id, seller_id, product_id, variant_id, amount_vnd, commission_vnd, status, escrow_release_date, created_at, isDelete)
VALUES (@CustomerId, @SellerId, @P1, @V1, 89000, 4450, 'Completed', DATEADD(DAY, -4, GETDATE()), DATEADD(DAY, -7, GETDATE()), 0);
SET @T1 = SCOPE_IDENTITY();

-- Transaction 2: Held (In Escrow)
INSERT INTO Transactions (customer_id, seller_id, product_id, variant_id, amount_vnd, commission_vnd, status, escrow_release_date, created_at, isDelete)
VALUES (@CustomerId, @SellerId, @P2, @V2, 120000, 6000, 'Held', DATEADD(DAY, 1, GETDATE()), DATEADD(DAY, -2, GETDATE()), 0);
SET @T2 = SCOPE_IDENTITY();

-- Transaction 3: Pending
INSERT INTO Transactions (customer_id, seller_id, product_id, variant_id, amount_vnd, commission_vnd, status, escrow_release_date, created_at, isDelete)
VALUES (@CustomerId, @SellerId, @P3, @V3, 150000, 7500, 'Pending', DATEADD(DAY, 2, GETDATE()), DATEADD(HOUR, -6, GETDATE()), 0);
SET @T3 = SCOPE_IDENTITY();

-- Transaction 4: Completed
INSERT INTO Transactions (customer_id, seller_id, product_id, variant_id, amount_vnd, commission_vnd, status, escrow_release_date, created_at, isDelete)
VALUES (@CustomerId, @SellerId, @P4, @V4, 499000, 24950, 'Completed', DATEADD(DAY, -1, GETDATE()), DATEADD(DAY, -4, GETDATE()), 0);
SET @T4 = SCOPE_IDENTITY();

-- Transaction 5: Completed (With Dispute/Complaint later)
INSERT INTO Transactions (customer_id, seller_id, product_id, variant_id, amount_vnd, commission_vnd, status, escrow_release_date, created_at, isDelete)
VALUES (@CustomerId, @SellerId, @P5, @V5, 199000, 9950, 'Completed', DATEADD(DAY, -2, GETDATE()), DATEADD(DAY, -5, GETDATE()), 0);
SET @T5 = SCOPE_IDENTITY();

PRINT '✓ Seeded 5 Sales Transactions';

-- 7. Seed 5 Withdrawals

INSERT INTO Withdrawals (seller_id, bank_info_id, amount_vnd, status, proof_file, created_at, isDelete)
VALUES 
(@SellerId, @BankInfoId, 150000, 'Completed', 'proof_bank_1.jpg', DATEADD(DAY, -15, GETDATE()), 0),
(@SellerId, @BankInfoId, 250000, 'Completed', 'proof_bank_2.jpg', DATEADD(DAY, -10, GETDATE()), 0),
(@SellerId, @BankInfoId, 500000, 'Completed', 'proof_bank_3.jpg', DATEADD(DAY, -5, GETDATE()), 0),
(@SellerId, @BankInfoId, 1000000, 'Pending', NULL, DATEADD(DAY, -1, GETDATE()), 0),
(@SellerId, @BankInfoId, 2000000, 'Pending', NULL, DATEADD(HOUR, -2, GETDATE()), 0);

PRINT '✓ Seeded 5 Withdrawals';

-- 8. Seed 5 Complaints & Chat Messages

DECLARE @C1 BIGINT, @C2 BIGINT, @C3 BIGINT, @C4 BIGINT, @C5 BIGINT;

INSERT INTO Complaints (transaction_id, customer_id, seller_id, description, evidence, status, resolution, created_at, isDelete)
VALUES (@T5, @CustomerId, @SellerId, N'Tài khoản Netflix bị đổi mật khẩu không vào được.', N'Ảnh chụp màn hình lỗi mật khẩu sai', 'Open', NULL, DATEADD(DAY, -2, GETDATE()), 0);
SET @C1 = SCOPE_IDENTITY();

INSERT INTO Complaints (transaction_id, customer_id, seller_id, description, evidence, status, resolution, created_at, isDelete)
VALUES (@T2, @CustomerId, @SellerId, N'Không nâng cấp được tài khoản Spotify Premium.', NULL, 'In_Progress', NULL, DATEADD(DAY, -1, GETDATE()), 0);
SET @C2 = SCOPE_IDENTITY();

INSERT INTO Complaints (transaction_id, customer_id, seller_id, description, evidence, status, resolution, created_at, isDelete)
VALUES (@T4, @CustomerId, @SellerId, N'Tài khoản ChatGPT Plus bị khóa API.', N'Lỗi API Quota Exceeded', 'Resolved', N'Đã hoàn lại 100% tiền mặt cho người mua', DATEADD(DAY, -3, GETDATE()), 0);
SET @C3 = SCOPE_IDENTITY();

INSERT INTO Complaints (transaction_id, customer_id, seller_id, description, evidence, status, resolution, created_at, isDelete)
VALUES (@T1, @CustomerId, @SellerId, N'Canva Pro không có tính năng tạo nhóm.', NULL, 'Closed', N'Lỗi do cấu hình người dùng, đã được hỗ trợ xong', DATEADD(DAY, -5, GETDATE()), 0);
SET @C4 = SCOPE_IDENTITY();

INSERT INTO Complaints (transaction_id, customer_id, seller_id, description, evidence, status, resolution, created_at, isDelete)
VALUES (@T3, @CustomerId, @SellerId, N'Key bản quyền Tool Facebook bị lỗi kích hoạt.', NULL, 'Open', NULL, DATEADD(HOUR, -4, GETDATE()), 0);
SET @C5 = SCOPE_IDENTITY();

PRINT '✓ Seeded 5 Complaints';

-- Seed Chat Messages for Complaint @C1 (Netflix Password issue)
INSERT INTO Chats (sender_id, receiver_id, complaint_id, chat_type, message, created_at, isDelete)
VALUES 
(@CustomerId, @SellerId, @C1, 'Complaint', N'Chào shop, tài khoản mình mua báo sai mật khẩu ạ!', DATEADD(DAY, -2, GETDATE()), 0),
(@SellerId, @CustomerId, @C1, 'Complaint', N'Chào bạn, để mình kiểm tra lại và gửi profile thay thế nhé.', DATEADD(DAY, -1, GETDATE()), 0),
(@CustomerId, @SellerId, @C1, 'Complaint', N'Dạ nhanh giùm mình với nha shop.', DATEADD(HOUR, -12, GETDATE()), 0);

-- Seed Chat Messages for Complaint @C2 (Spotify upgrade)
INSERT INTO Chats (sender_id, receiver_id, complaint_id, chat_type, message, created_at, isDelete)
VALUES 
(@CustomerId, @SellerId, @C2, 'Complaint', N'Shop ơi gói Spotify gia đình bị lỗi quốc gia không add được.', DATEADD(DAY, -1, GETDATE()), 0),
(@StaffId, @SellerId, @C2, 'Complaint', N'Nhân viên hỗ trợ: Yêu cầu shop seller01 kiểm tra và thay đổi link gia đình trong 24h.', DATEADD(HOUR, -6, GETDATE()), 0);

PRINT '✓ Seeded Complaint Chats';

-- 9. Seed 5 Reviews for products

INSERT INTO Reviews (product_id, user_id, rating, comment, created_at, isDelete)
VALUES 
(@P1, @CustomerId, 5, N'Tài khoản xem mượt, hỗ trợ 4K cực nét. Rất uy tín!', DATEADD(DAY, -6, GETDATE()), 0),
(@P2, @CustomerId, 4, N'Spotify nghe tốt nhưng lúc đầu hơi khó kích hoạt. Chủ shop nhiệt tình.', DATEADD(DAY, -4, GETDATE()), 0),
(@P3, @CustomerId, 5, N'Canva nâng cấp rất nhanh chỉ mất 2 phút. Cảm ơn shop.', DATEADD(DAY, -2, GETDATE()), 0),
(@P4, @CustomerId, 1, N'ChatGPT Plus bị khóa khá nhanh. Cần xem lại bảo hành.', DATEADD(DAY, -1, GETDATE()), 0),
(@P5, @CustomerId, 5, N'Tool Facebook hoạt động cực tốt, nuôi clone rất hiệu quả.', DATEADD(HOUR, -3, GETDATE()), 0);

PRINT '✓ Seeded 5 Reviews';

-- 10. Seed 5 Shop Flags (Warnings)

INSERT INTO ShopFlags (seller_id, staff_id, complaint_id, reason, flag_level, created_at, isDelete)
VALUES 
(@SellerId, @StaffId, @C1, N'Shop phản hồi khiếu nại của khách hàng chậm hơn 24 giờ.', 'Warning', DATEADD(DAY, -10, GETDATE()), 0),
(@SellerId, @StaffId, @C2, N'Bán sản phẩm lỗi quốc gia nhiều lần trên Spotify.', 'Warning', DATEADD(DAY, -5, GETDATE()), 0),
(@SellerId, @StaffId, NULL, N'Đăng mô tả sản phẩm chứa từ khóa spam quảng cáo ngoài sàn.', 'Warning', DATEADD(DAY, -2, GETDATE()), 0),
(@SellerId, @StaffId, @C3, N'Khách khiếu nại lừa đảo key bản quyền, đang trong diện theo dõi.', 'Suspension', DATEADD(DAY, -1, GETDATE()), 0),
(@SellerId, @StaffId, NULL, N'Cảnh báo: Vi phạm đăng ký nhiều tài khoản bán chéo.', 'Warning', DATEADD(HOUR, -1, GETDATE()), 0);

PRINT '✓ Seeded 5 Shop Flags';

PRINT '========================================';
PRINT '✅ SELLER MOCK DATA SEEDING COMPLETE!';
PRINT '========================================';
GO
