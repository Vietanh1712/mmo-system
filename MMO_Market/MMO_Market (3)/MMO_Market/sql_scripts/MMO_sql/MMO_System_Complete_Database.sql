-- ==============================================================================
-- CƠ SỞ DỮ LIỆU TOÀN DIỆN: MMO MARKET SYSTEM (SQL SERVER)
-- Tên tệp: MMO_System_Complete_Database.sql
-- Mô tả: Khởi tạo database, định nghĩa toàn bộ bảng biểu, ràng buộc,
--       triggers nghiệp vụ và nạp dữ liệu seed chuẩn (Categories, Users, Products).
--       Đồng bộ 100% với JPA Entity mapping trong mã nguồn hệ thống hiện tại.
-- ==============================================================================

-- 1. KHỞI TẠO CƠ SỞ DỮ LIỆU
IF NOT EXISTS (SELECT * FROM sys.databases WHERE name = 'MMO_System')
BEGIN
    CREATE DATABASE MMO_System;
END
GO

USE MMO_System;
GO

-- XÓA BẢNG CŨ NẾU CÓ ĐỂ TRÁNH XUNG ĐỘT (XÓA THEO THỨ TỰ CON TRƯỚC - CHA SAU)
IF OBJECT_ID('AuditLogs', 'U') IS NOT NULL DROP TABLE AuditLogs;
IF OBJECT_ID('Notifications', 'U') IS NOT NULL DROP TABLE Notifications;
IF OBJECT_ID('SystemConfigurations', 'U') IS NOT NULL DROP TABLE SystemConfigurations;
IF OBJECT_ID('Reviews', 'U') IS NOT NULL DROP TABLE Reviews;
IF OBJECT_ID('PreOrders', 'U') IS NOT NULL DROP TABLE PreOrders;
IF OBJECT_ID('Wishlists', 'U') IS NOT NULL DROP TABLE Wishlists;
IF OBJECT_ID('Chats', 'U') IS NOT NULL DROP TABLE Chats;
IF OBJECT_ID('ShopFlags', 'U') IS NOT NULL DROP TABLE ShopFlags;
IF OBJECT_ID('Complaints', 'U') IS NOT NULL DROP TABLE Complaints;
IF OBJECT_ID('WalletTransactions', 'U') IS NOT NULL DROP TABLE WalletTransactions;
IF OBJECT_ID('Withdrawals', 'U') IS NOT NULL DROP TABLE Withdrawals;
IF OBJECT_ID('DigitalAssets', 'U') IS NOT NULL DROP TABLE DigitalAssets;
IF OBJECT_ID('Transactions', 'U') IS NOT NULL DROP TABLE Transactions;
IF OBJECT_ID('TopupTransactions', 'U') IS NOT NULL DROP TABLE TopupTransactions;
IF OBJECT_ID('ProductVariants', 'U') IS NOT NULL DROP TABLE ProductVariants;
IF OBJECT_ID('Products', 'U') IS NOT NULL DROP TABLE Products;
IF OBJECT_ID('Categories', 'U') IS NOT NULL DROP TABLE Categories;
IF OBJECT_ID('SellerBankInfo', 'U') IS NOT NULL DROP TABLE SellerBankInfo;
IF OBJECT_ID('SellerRegistrations', 'U') IS NOT NULL DROP TABLE SellerRegistrations;
IF OBJECT_ID('EmailVerifications', 'U') IS NOT NULL DROP TABLE EmailVerifications;
IF OBJECT_ID('Authentications', 'U') IS NOT NULL DROP TABLE Authentications;
IF OBJECT_ID('Users', 'U') IS NOT NULL DROP TABLE Users;
GO

-- ==========================================
-- PHẦN 1: TÀI KHOẢN VÀ BẢO MẬT (MAPPED WITH JPA)
-- ==========================================

CREATE TABLE Users (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255),
    full_name NVARCHAR(255),
    gender NVARCHAR(20) NULL,
    address NVARCHAR(500) NULL,
    national_id VARCHAR(20) NULL,
    date_of_birth DATE NULL,
    role NVARCHAR(MAX) NOT NULL, -- JSON format: {"role": "Customer"}, {"role": "Seller"}, {"role": "Admin"}, {"role": "Staff"}
    phone VARCHAR(20),
    shop_status VARCHAR(20) DEFAULT 'Pending', -- Pending, Active, Banned
    balance_vnd BIGINT DEFAULT 0,
    permissions NVARCHAR(MAX) NULL,
    isVerified BIT DEFAULT 0,
    isLocked BIT DEFAULT 0,
    created_at DATETIME DEFAULT GETDATE(),
    updated_at DATETIME DEFAULT GETDATE(),
    isDelete BIT DEFAULT 0
);
GO
CREATE INDEX idx_email ON Users(email);
GO

CREATE TABLE Authentications (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    provider VARCHAR(50) NOT NULL, -- 'System' hoặc 'Google'
    third_party_token VARCHAR(255),
    refresh_token VARCHAR(512),
    refresh_token_expiry_date DATETIME,
    is_revoked BIT DEFAULT 0,
    created_at DATETIME DEFAULT GETDATE(),
    isDelete BIT DEFAULT 0,
    CONSTRAINT FK_Auth_Users FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE NO ACTION
);
GO

CREATE TABLE EmailVerifications (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    verification_code VARCHAR(6) NOT NULL,
    expiry_date DATETIME NOT NULL,
    is_used BIT DEFAULT 0,
    CONSTRAINT FK_Email_Users FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE NO ACTION
);
GO

-- ==========================================
-- PHẦN 2: THÔNG TIN NGƯỜI BÁN VÀ CỬA HÀNG (KYC)
-- ==========================================

CREATE TABLE SellerRegistrations (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    shop_name NVARCHAR(255) NOT NULL,
    description NVARCHAR(MAX),
    contract VARCHAR(255),
    signed_contract VARCHAR(255),
    status VARCHAR(20) DEFAULT 'Pending', -- Pending, Approved, Rejected
    created_at DATETIME DEFAULT GETDATE(),
    isDelete BIT DEFAULT 0,
    CONSTRAINT FK_Reg_Users FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE NO ACTION
);
GO

CREATE TABLE SellerBankInfo (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    bank_name NVARCHAR(100) NOT NULL,
    account_number VARCHAR(50) NOT NULL,
    branch NVARCHAR(100),
    created_at DATETIME DEFAULT GETDATE(),
    isDelete BIT DEFAULT 0,
    CONSTRAINT FK_Bank_Users FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE NO ACTION
);
GO

-- ==========================================
-- PHẦN 3: QUẢN LÝ SẢN PHẨM VÀ KHO SỐ (MAPPED WITH Category.java & Product.java)
-- ==========================================

CREATE TABLE Categories (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(100) NOT NULL,
    parent_id BIGINT NULL,
    description NVARCHAR(500),
    created_at DATETIME DEFAULT GETDATE(),
    updated_at DATETIME DEFAULT GETDATE(),
    is_delete BIT DEFAULT 0, -- Tên cột khớp 100% với Category.java: @Column(name = "is_delete")
    isDelete BIT DEFAULT 0,  -- Cột phụ dự phòng cho các truy vấn cũ
    CONSTRAINT FK_Category_Parent FOREIGN KEY (parent_id) REFERENCES Categories(id) ON DELETE NO ACTION
);
GO

CREATE TABLE Products (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    seller_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    name NVARCHAR(255) NOT NULL,
    description NVARCHAR(MAX),
    image VARCHAR(255),
    created_at DATETIME DEFAULT GETDATE(),
    isDelete BIT DEFAULT 0,  -- Khớp với Product.java: @Column(name = "isDelete")
    is_delete BIT DEFAULT 0, -- Cột phụ đồng bộ soft delete
    CONSTRAINT FK_Products_Seller FOREIGN KEY (seller_id) REFERENCES Users(id) ON DELETE NO ACTION,
    CONSTRAINT FK_Products_Category FOREIGN KEY (category_id) REFERENCES Categories(id) ON DELETE NO ACTION
);
GO

CREATE TABLE ProductVariants (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    product_id BIGINT NOT NULL,
    variant_name NVARCHAR(255) NOT NULL,
    price_vnd BIGINT NOT NULL,
    stock INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'Pending',
    created_at DATETIME DEFAULT GETDATE(),
    isDelete BIT DEFAULT 0, -- Khớp với ProductVariant.java: @Column(name = "isDelete")
    CONSTRAINT FK_Variants_Product FOREIGN KEY (product_id) REFERENCES Products(id) ON DELETE NO ACTION
);
GO

CREATE TABLE DigitalAssets (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    variant_id BIGINT NOT NULL,
    asset_data NVARCHAR(MAX) NOT NULL, -- Dữ liệu tài khoản/key bảo mật
    status VARCHAR(20) DEFAULT 'Available', -- Available, Sold
    transaction_id BIGINT NULL,
    created_at DATETIME DEFAULT GETDATE(),
    isDelete BIT DEFAULT 0,
    CONSTRAINT FK_Assets_Variant FOREIGN KEY (variant_id) REFERENCES ProductVariants(id) ON DELETE NO ACTION
);
GO

-- ==========================================
-- PHẦN 4: GIAO DỊCH VÀ VÍ ĐIỆN TỬ (TÀI CHÍNH)
-- ==========================================

CREATE TABLE TopupTransactions (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    amount_vnd BIGINT NOT NULL,
    sepay_code VARCHAR(255),
    status VARCHAR(20) DEFAULT 'Pending',
    created_at DATETIME DEFAULT GETDATE(),
    isDelete BIT DEFAULT 0,
    CONSTRAINT FK_Topup_Users FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE NO ACTION
);
GO

CREATE TABLE Transactions (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    variant_id BIGINT NOT NULL,
    amount_vnd BIGINT NOT NULL,
    commission_vnd BIGINT NOT NULL,
    status VARCHAR(20) DEFAULT 'Pending',
    escrow_release_date DATETIME,
    created_at DATETIME DEFAULT GETDATE(),
    isDelete BIT DEFAULT 0,
    CONSTRAINT FK_Trans_Customer FOREIGN KEY (customer_id) REFERENCES Users(id) ON DELETE NO ACTION,
    CONSTRAINT FK_Trans_Seller FOREIGN KEY (seller_id) REFERENCES Users(id) ON DELETE NO ACTION,
    CONSTRAINT FK_Trans_Product FOREIGN KEY (product_id) REFERENCES Products(id) ON DELETE NO ACTION,
    CONSTRAINT FK_Trans_Variant FOREIGN KEY (variant_id) REFERENCES ProductVariants(id) ON DELETE NO ACTION
);
GO

ALTER TABLE DigitalAssets ADD CONSTRAINT FK_Assets_Trans FOREIGN KEY (transaction_id) REFERENCES Transactions(id) ON DELETE NO ACTION;
GO

CREATE TABLE Withdrawals (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    seller_id BIGINT NOT NULL,
    bank_info_id BIGINT NOT NULL,
    amount_vnd BIGINT NOT NULL,
    status VARCHAR(20) DEFAULT 'Pending',
    proof_file VARCHAR(255),
    created_at DATETIME DEFAULT GETDATE(),
    isDelete BIT DEFAULT 0,
    CONSTRAINT FK_Withdraw_Seller FOREIGN KEY (seller_id) REFERENCES Users(id) ON DELETE NO ACTION,
    CONSTRAINT FK_Withdraw_Bank FOREIGN KEY (bank_info_id) REFERENCES SellerBankInfo(id) ON DELETE NO ACTION
);
GO

CREATE TABLE WalletTransactions (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    amount_vnd BIGINT NOT NULL,
    balance_after BIGINT NOT NULL,
    transaction_type VARCHAR(50) NOT NULL, -- DEPOSIT, WITHDRAW, PURCHASE, SALE, REFUND
    reference_id BIGINT NULL,
    description NVARCHAR(MAX),
    created_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_WalletLog_Users FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE NO ACTION
);
GO

-- ==========================================
-- PHẦN 5: CHĂM SÓC KHÁCH HÀNG VÀ KIỂM DUYỆT
-- ==========================================

CREATE TABLE Complaints (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    transaction_id BIGINT,
    customer_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    description NVARCHAR(MAX) NOT NULL,
    evidence NVARCHAR(MAX),
    status VARCHAR(20) DEFAULT 'Open',
    resolution NVARCHAR(MAX),
    created_at DATETIME DEFAULT GETDATE(),
    isDelete BIT DEFAULT 0,
    CONSTRAINT FK_Complaints_Trans FOREIGN KEY (transaction_id) REFERENCES Transactions(id) ON DELETE NO ACTION,
    CONSTRAINT FK_Complaints_Customer FOREIGN KEY (customer_id) REFERENCES Users(id) ON DELETE NO ACTION,
    CONSTRAINT FK_Complaints_Seller FOREIGN KEY (seller_id) REFERENCES Users(id) ON DELETE NO ACTION
);
GO

CREATE TABLE ShopFlags (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    seller_id BIGINT NOT NULL,
    staff_id BIGINT NOT NULL,
    complaint_id BIGINT NULL,
    reason NVARCHAR(MAX) NOT NULL,
    flag_level VARCHAR(20) DEFAULT 'Warning',
    created_at DATETIME DEFAULT GETDATE(),
    isDelete BIT DEFAULT 0,
    CONSTRAINT FK_Flags_Seller FOREIGN KEY (seller_id) REFERENCES Users(id) ON DELETE NO ACTION,
    CONSTRAINT FK_Flags_Staff FOREIGN KEY (staff_id) REFERENCES Users(id) ON DELETE NO ACTION,
    CONSTRAINT FK_Flags_Complaint FOREIGN KEY (complaint_id) REFERENCES Complaints(id) ON DELETE NO ACTION
);
GO

CREATE TABLE Chats (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    sender_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    complaint_id BIGINT NULL,
    chat_type VARCHAR(20) DEFAULT 'Normal',
    message NVARCHAR(MAX) NOT NULL,
    created_at DATETIME DEFAULT GETDATE(),
    isDelete BIT DEFAULT 0,
    CONSTRAINT FK_Chats_Sender FOREIGN KEY (sender_id) REFERENCES Users(id) ON DELETE NO ACTION,
    CONSTRAINT FK_Chats_Receiver FOREIGN KEY (receiver_id) REFERENCES Users(id) ON DELETE NO ACTION,
    CONSTRAINT FK_Chats_Complaint FOREIGN KEY (complaint_id) REFERENCES Complaints(id) ON DELETE NO ACTION
);
GO

-- ==========================================
-- PHẦN 6: TÍNH NĂNG MỞ RỘNG (WISH_LIST, PRE_ORDER, REVIEW)
-- ==========================================

CREATE TABLE Wishlists (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    created_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_Wishlist_Customer FOREIGN KEY (customer_id) REFERENCES Users(id) ON DELETE NO ACTION,
    CONSTRAINT FK_Wishlist_Product FOREIGN KEY (product_id) REFERENCES Products(id) ON DELETE NO ACTION,
    CONSTRAINT UQ_Customer_Product UNIQUE (customer_id, product_id)
);
GO

CREATE TABLE PreOrders (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    expected_price_vnd BIGINT NOT NULL,
    quantity INT DEFAULT 1,
    status VARCHAR(20) DEFAULT 'Pending',
    notes NVARCHAR(MAX),
    created_at DATETIME DEFAULT GETDATE(),
    isDelete BIT DEFAULT 0, -- Khớp với PreOrder.java: @Column(name = "isDelete")
    CONSTRAINT FK_PreOrder_Customer FOREIGN KEY (customer_id) REFERENCES Users(id) ON DELETE NO ACTION,
    CONSTRAINT FK_PreOrder_Product FOREIGN KEY (product_id) REFERENCES Products(id) ON DELETE NO ACTION
);
GO

CREATE TABLE Reviews (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    product_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    rating INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment NVARCHAR(MAX),
    created_at DATETIME DEFAULT GETDATE(),
    isDelete BIT DEFAULT 0,
    CONSTRAINT FK_Reviews_Product FOREIGN KEY (product_id) REFERENCES Products(id) ON DELETE NO ACTION,
    CONSTRAINT FK_Reviews_User FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE NO ACTION
);
GO

-- ==========================================
-- PHẦN 7: HỆ THỐNG VÀ KIỂM TOÁN (SYSTEM & AUDIT - MAPPED WITH AuditLog.java)
-- ==========================================

CREATE TABLE SystemConfigurations (
    id INT IDENTITY(1,1) PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL UNIQUE,
    config_value NVARCHAR(MAX) NOT NULL,
    description NVARCHAR(500),
    updated_by BIGINT NULL,
    updated_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_Config_Admin FOREIGN KEY (updated_by) REFERENCES Users(id) ON DELETE NO ACTION
);
GO

CREATE TABLE Notifications (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title NVARCHAR(255) NOT NULL,
    content NVARCHAR(MAX),
    created_at DATETIME DEFAULT GETDATE(),
    isDelete BIT DEFAULT 0,
    CONSTRAINT FK_Notif_Users FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE NO ACTION
);
GO

CREATE TABLE AuditLogs (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    action VARCHAR(255) NOT NULL,
    details NVARCHAR(MAX),
    created_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_Audit_Users FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE NO ACTION
);
GO

-- ==========================================
-- PHẦN 8: TRIGGERS NGHIỆP VỤ (CHẠY TRÊN SQL SERVER)
-- ==========================================

-- 1. Trigger kiểm tra số tiền rút tối thiểu 50,000 VND
CREATE OR ALTER TRIGGER trg_CheckWithdrawalMin
ON Withdrawals
AFTER INSERT
AS
BEGIN
    IF EXISTS (SELECT 1 FROM inserted WHERE amount_vnd < 50000)
    BEGIN
        RAISERROR (N'Lỗi: Số tiền rút tối thiểu phải là 50,000 VNĐ theo chính sách sàn.', 16, 1);
        ROLLBACK TRANSACTION;
    END
END;
GO

-- 2. Trigger thiết lập trạng thái Giữ Tiền (Escrow 3 ngày) khi khởi tạo giao dịch
CREATE OR ALTER TRIGGER trg_HoldFundsEscrow
ON Transactions
AFTER INSERT
AS
BEGIN
    UPDATE Transactions
    SET status = 'Held',
        escrow_release_date = DATEADD(DAY, 3, GETDATE())
    FROM Transactions t
    INNER JOIN inserted i ON t.id = i.id;
END;
GO

-- 3. Trigger cập nhật file minh chứng rút tiền khi Status chuyển sang Completed
CREATE OR ALTER TRIGGER trg_UpdateWithdrawalProof
ON Withdrawals
AFTER UPDATE
AS
BEGIN
    IF UPDATE(status)
    BEGIN
        UPDATE Withdrawals
        SET proof_file = 'proof_bank_' + CAST(i.id AS VARCHAR(20)) + '.jpg'
        FROM Withdrawals w
        INNER JOIN inserted i ON w.id = i.id
        INNER JOIN deleted d ON w.id = d.id
        WHERE i.status = 'Completed' AND d.status != 'Completed';
    END
END;
GO

-- 4. Trigger tự động cấp quyền Customer_Seller khi cửa hàng được Approved
CREATE OR ALTER TRIGGER trg_UpdateShopStatus
ON SellerRegistrations
AFTER UPDATE
AS
BEGIN
    IF UPDATE(status)
    BEGIN
        UPDATE Users
        SET shop_status = i.status,
            role = CASE
                WHEN i.status = 'Approved' AND JSON_VALUE(Users.role, '$.role') = 'Customer' THEN '{"role": "Customer_Seller"}'
                WHEN i.status = 'Rejected' AND JSON_VALUE(Users.role, '$.role') = 'Customer_Seller' THEN '{"role": "Customer"}'
                ELSE Users.role
            END
        FROM Users
        INNER JOIN inserted i ON Users.id = i.user_id
        INNER JOIN deleted d ON i.id = d.id
        WHERE i.status IN ('Approved', 'Rejected') AND i.status != d.status;
    END
END;
GO

-- ==========================================
-- PHẦN 9: NẠP DỮ LIỆU SEED (DATA SEEDING)
-- ==========================================

-- 1. Cấu hình hệ thống mặc định
INSERT INTO SystemConfigurations (config_key, config_value, description)
VALUES
('DEFAULT_COMMISSION_PERCENT', '5.0', N'Phần trăm hoa hồng mặc định sàn thu của Seller'),
('MIN_WITHDRAWAL_VND', '50000', N'Số tiền rút tối thiểu'),
('MAINTENANCE_MODE', 'FALSE', N'Trạng thái bảo trì hệ thống (TRUE/FALSE)');
GO

-- 2. Khởi tạo danh mục hàng hóa (Categories) - Cấu trúc cây
SET IDENTITY_INSERT Categories ON;

-- Danh mục Cha
INSERT INTO Categories (id, name, parent_id, description, is_delete, isDelete)
VALUES
(1, N'Email', NULL, N'Các dịch vụ email và tài khoản mail', 0, 0),
(2, N'Tài khoản', NULL, N'Tài khoản các nền tảng xã hội & dịch vụ', 0, 0),
(3, N'Phần mềm', NULL, N'Các công cụ phần mềm chuyên dụng cho kinh doanh online', 0, 0),
(4, N'Tăng tương tác', NULL, N'Các dịch vụ tăng engagement & tương tác trên mạng xã hội', 0, 0),
(5, N'Dịch vụ phần mềm', NULL, N'Công cụ, plugin và dịch vụ lập trình', 0, 0),
(6, N'Blockchain', NULL, N'Các sản phẩm tiền ảo, NFT và blockchain', 0, 0);

-- Danh mục Con
INSERT INTO Categories (id, name, parent_id, description, is_delete, isDelete)
VALUES
(7, N'Gmail', 1, N'Tài khoản Gmail và G Suite', 0, 0),
(8, N'HotMail', 1, N'Tài khoản Hotmail/Outlook Mail', 0, 0),
(9, N'OutlookMail', 1, N'Email Outlook chính thức', 0, 0),
(10, N'RuMail', 1, N'Tài khoản mail từ các trang Nga', 0, 0),
(11, N'DomainMail', 1, N'Email tên miền riêng', 0, 0),
(12, N'YahooMail', 1, N'Tài khoản Yahoo Mail', 0, 0),
(13, N'ProtonMail', 1, N'Email bảo mật ProtonMail', 0, 0),
(14, N'Loại Mail Khác', 1, N'Các loại email khác', 0, 0),

(15, N'Tài khoản FB', 2, N'Tài khoản Facebook cá nhân & fanpage', 0, 0),
(16, N'Tài Khoản BM', 2, N'Tài khoản Business Manager Facebook', 0, 0),
(17, N'Tài Khoản Zalo', 2, N'Tài khoản Zalo OA & cá nhân', 0, 0),
(18, N'Tài Khoản Twitter', 2, N'Tài khoản Twitter/X với followers', 0, 0),
(19, N'Tài Khoản Telegram', 2, N'Tài khoản Telegram Group & Channel', 0, 0),
(20, N'Tài Khoản Instagram', 2, N'Tài khoản Instagram với followers', 0, 0),
(21, N'Tài Khoản Shopee', 2, N'Tài khoản Shopee bán hàng', 0, 0),
(22, N'Tài Khoản Discord', 2, N'Tài khoản Discord với server', 0, 0),
(23, N'Tài Khoản TikTok', 2, N'Tài khoản TikTok với followers', 0, 0),
(24, N'Key Diệt Virus', 2, N'Key phần mềm diệt virus chính hãng', 0, 0),
(25, N'Tài Khoản Capcut', 2, N'Tài khoản Capcut Pro', 0, 0),
(26, N'Key Window', 2, N'Key Windows & Office chính hãng', 0, 0),
(27, N'Tài Khoản Khác', 2, N'Các tài khoản khác', 0, 0),

(28, N'Phần Mềm FB', 3, N'Tool & phần mềm quản lý Facebook', 0, 0),
(29, N'Phần Mềm Google', 3, N'Công cụ Google Ads, SEO, Analytics', 0, 0),
(30, N'Phần Mềm Youtube', 3, N'Tool quản lý & tối ưu hóa Youtube', 0, 0),
(31, N'Phần Mềm Tiền Ảo', 3, N'Software trading & quản lý crypto', 0, 0),
(32, N'Phần Mềm PTC', 3, N'Phần mềm kiếm tiền PTC tự động', 0, 0),
(33, N'Phần Mềm Captcha', 3, N'Giải captcha tự động 2captcha, Anti-captcha', 0, 0),
(34, N'Phần Mềm Offer', 3, N'Tool kiếm tiền từ Offer Wall', 0, 0),
(35, N'Phần Mềm PTU', 3, N'Phần mềm quản lý PTU (Paid Task)', 0, 0),
(36, N'Phần Mềm Khác', 3, N'Các phần mềm khác', 0, 0),

(37, N'Dịch vụ Facebook', 4, N'Tăng like, follow, bình luận Facebook', 0, 0),
(38, N'Dịch vụ Tiktok', 4, N'Tăng view, like, follow TikTok', 0, 0),
(39, N'Dịch vụ Google', 4, N'Dịch vụ SEO & tối ưu Google', 0, 0),
(40, N'Dịch vụ Telegram', 4, N'Tăng member Telegram Channel/Group', 0, 0),
(41, N'Dịch vụ Shopee', 4, N'Tăng view, mua hàng Shopee', 0, 0),
(42, N'Dịch vụ Discord', 4, N'Tăng member Discord server', 0, 0),
(43, N'Dịch vụ Twitter', 4, N'Tăng follower, retweet Twitter', 0, 0),
(44, N'Dịch vụ Youtube', 4, N'Tăng view, subcriber Youtube', 0, 0),
(45, N'Dịch vụ Zalo', 4, N'Tăng member Zalo OA & tương tác', 0, 0),
(46, N'Dịch vụ Instagram', 4, N'Tăng follow, like Instagram', 0, 0),
(47, N'Tương tác khác', 4, N'Các dịch vụ tương tác khác', 0, 0),

(48, N'Tool MMO', 5, N'Công cụ MMO marketing tự động', 0, 0),
(49, N'Tool Facebook', 5, N'Tool chuyên biệt Facebook', 0, 0),
(50, N'Tool Google', 5, N'Tool kiếm tiền Google', 0, 0),
(51, N'Tool Youtube', 5, N'Bot & công cụ Youtube', 0, 0),
(52, N'Tool TikTok', 5, N'Công cụ TikTok automation', 0, 0),
(53, N'Tool Instagram', 5, N'Bot Instagram & follow automation', 0, 0),
(54, N'Đồ họa - Design', 5, N'Công cụ thiết kế đồ họa', 0, 0),
(55, N'Video Editor', 5, N'Phần mềm chỉnh sửa video', 0, 0),
(56, N'Plugin & Extension', 5, N'Plugin browser & extension hữu ích', 0, 0),
(57, N'Script & Bot', 5, N'Script tự động hóa & bot công việc', 0, 0),
(58, N'Phần mềm khác', 5, N'Các phần mềm khác', 0, 0),

(59, N'Tiền ảo - Crypto', 6, N'Bitcoin, Ethereum, Altcoin khác', 0, 0),
(60, N'NFT', 6, N'Token NFT và digital art', 0, 0),
(61, N'Coinlist', 6, N'Coinlist & các IDO token mới', 0, 0),
(62, N'Airdrop', 6, N'Airdrop token & chiến dịch phát free', 0, 0),
(63, N'Ví điện tử', 6, N'Ví tiền điện tử & wallet', 0, 0),
(64, N'Tài khoản sàn', 6, N'Tài khoản Binance, Bybit, OKX, v.v', 0, 0),
(65, N'Blockchain khác', 6, N'Các sản phẩm blockchain khác', 0, 0);

SET IDENTITY_INSERT Categories OFF;
GO

-- 3. Khởi tạo tài khoản mẫu (Users)
-- Mật khẩu mặc định của Sellers/Customers: '123456' băm theo chuẩn BCrypt
DECLARE @PasswordHash VARCHAR(255) = '$2y$10$uRFrFYP.Ld32A.LSRCm10.LlUfbJH7vgelUd4h1dsvZCyMKz1c0Bq';

SET IDENTITY_INSERT Users ON;

-- 12 Sellers cho danh sách sản phẩm
INSERT INTO Users (id, email, password, full_name, gender, address, national_id, date_of_birth, role, phone, shop_status, balance_vnd, isVerified, isLocked, isDelete)
VALUES
(1, 'netflixvn@mmo.com', @PasswordHash, N'NetflixVN Store', NULL, NULL, NULL, NULL, '{"role": "Seller"}', '0901111111', 'Approved', 1500000, 1, 0, 0),
(2, 'netflixvip@mmo.com', @PasswordHash, N'Netflix_Vip_Store', NULL, NULL, NULL, NULL, '{"role": "Seller"}', '0902222222', 'Approved', 4500000, 1, 0, 0),
(3, 'aihelper@mmo.com', @PasswordHash, N'AI_Helper Store', NULL, NULL, NULL, NULL, '{"role": "Seller"}', '0903333333', 'Approved', 1200000, 1, 0, 0),
(4, 'musiclovers@mmo.com', @PasswordHash, N'MusicLovers Store', NULL, NULL, NULL, NULL, '{"role": "Seller"}', '0904444444', 'Approved', 800000, 1, 0, 0),
(5, 'microsoft@mmo.com', @PasswordHash, N'Microsoft_Reseller Store', NULL, NULL, NULL, NULL, '{"role": "Seller"}', '0905555555', 'Approved', 6200000, 1, 0, 0),
(6, 'redpremium@mmo.com', @PasswordHash, N'RedPremium Store', NULL, NULL, NULL, NULL, '{"role": "Seller"}', '0906666666', 'Approved', 950000, 1, 0, 0),
(7, 'canvapro@mmo.com', @PasswordHash, N'CanvaPro Store', NULL, NULL, NULL, NULL, '{"role": "Seller"}', '0907777777', 'Approved', 3200000, 1, 0, 0),
(8, 'gmailpro@mmo.com', @PasswordHash, N'GmailPro Store', NULL, NULL, NULL, NULL, '{"role": "Seller"}', '0908888888', 'Approved', 110000, 1, 0, 0),
(9, 'mmocoder@mmo.com', @PasswordHash, N'MMO_Coder Store', NULL, NULL, NULL, NULL, '{"role": "Seller"}', '0909999999', 'Approved', 8500000, 1, 0, 0),
(10, 'securenet@mmo.com', @PasswordHash, N'SecureNet Store', NULL, NULL, NULL, NULL, '{"role": "Seller"}', '0901234567', 'Approved', 720000, 1, 0, 0),
(11, 'socialmediaup@mmo.com', @PasswordHash, N'SocialMediaUp Store', NULL, NULL, NULL, NULL, '{"role": "Seller"}', '0902345678', 'Approved', 210000, 1, 0, 0),
(12, 'bannerdesign@mmo.com', @PasswordHash, N'BannerDesign Store', NULL, NULL, NULL, NULL, '{"role": "Seller"}', '0903456789', 'Approved', 0, 1, 0, 0);

-- Các tài khoản phân quyền đặc quyền
INSERT INTO Users (id, email, password, full_name, gender, address, national_id, date_of_birth, role, phone, shop_status, balance_vnd, isVerified, isLocked, isDelete)
VALUES
(13, 'customer01@gmail.com', @PasswordHash, N'Nguyễn Văn Khách', N'Nam', N'123 Đường Nguyễn Trãi, Hà Nội', '001096001234', '1996-05-15', '{"role": "Customer"}', '0987654321', 'Pending', 500000, 1, 0, 0),
(14, 'staff01@gmail.com', @PasswordHash, N'Trần Thị Nhân Viên', N'Nữ', N'456 Cầu Giấy, Hà Nội', '001098005678', '1998-08-20', '{"role": "Staff"}', '0912345678', 'Approved', 0, 1, 0, 0),
(15, 'admin01@gmail.com', @PasswordHash, N'Admin MMO System', N'Nam', N'Hệ thống MMO Market', '001090009999', '1990-01-01', '{"role": "Admin"}', '0900000000', 'Approved', 0, 1, 0, 0),
-- Tài khoản admin chuẩn cấu hình hệ thống (admin@mmo.com / 123456)
(16, 'admin@mmo.com', '$2a$10$NcmOXXGkICk.davDnIvgbuUcscMw31mHDhb5oei/4hHOaWZRzE.g6', N'Administrator', N'Nam', N'Hệ thống MMO Market', '001090000000', '1990-01-01', '{"role": "Admin"}', '0123456789', 'Approved', 0, 1, 0, 0);

SET IDENTITY_INSERT Users OFF;
GO

-- 4. Khởi tạo sản phẩm mẫu (Products)
SET IDENTITY_INSERT Products ON;

INSERT INTO Products (id, seller_id, category_id, name, description, image, isDelete, is_delete)
VALUES 
(1, 1, 27, N'Tài khoản Netflix Premium 4K UHD 1 Tháng (Xem riêng 1 thiết bị, bảo hành 1 đổi 1)', N'Xem phim chất lượng Ultra HD 4K trên mọi thiết bị. Giao tài khoản tự động lập tức sau khi thanh toán. Bảo hành 1 đổi 1 suốt thời gian sử dụng.', 'https://via.placeholder.com/300x160/fd761a/ffffff?text=Netflix+Premium', 0, 0),
(2, 2, 27, N'Tài khoản Netflix Premium 4K UHD Gói 1 Năm (Chính chủ gia hạn ổn định)', N'Gói cước Netflix Premium 12 tháng xem ổn định không lo bị khóa hay đăng xuất. Hỗ trợ xem trên SmartTV, điện thoại, máy tính.', 'https://via.placeholder.com/300x160/fd761a/ffffff?text=Netflix+1Year', 0, 0),
(3, 3, 27, N'Tài khoản ChatGPT Plus (OpenAI GPT-4o) Chính Chủ Sẵn 20$ Hạn 1 Tháng', N'Tài khoản OpenAI nâng cấp sẵn gói Plus trị giá 20$. Sử dụng GPT-4o không giới hạn tốc độ và tính năng mới nhất.', 'https://via.placeholder.com/300x160/fd761a/ffffff?text=ChatGPT+Plus', 0, 0),
(4, 4, 27, N'Spotify Premium 1 Năm Giá Siêu Rẻ (Nâng cấp Family email của bạn)', N'Nghe nhạc chất lượng cao không quảng cáo trên Spotify. Nâng cấp trực tiếp trên email cá nhân của bạn thông qua liên kết Family.', 'https://via.placeholder.com/300x160/fd761a/ffffff?text=Spotify+Premium', 0, 0),
(5, 5, 26, N'Key Windows 11 Pro Bản Quyền Vĩnh Viễn (Kèm hướng dẫn active chi tiết)', N'Kích hoạt bản quyền Windows 11 Professional vĩnh viễn theo máy. Hỗ trợ cập nhật đầy đủ, cài đặt lại Win vẫn giữ bản quyền.', 'https://via.placeholder.com/300x160/fd761a/ffffff?text=Windows+11+Key', 0, 0),
(6, 6, 27, N'Youtube Premium Không Quảng Cáo 6 Tháng (Add Family bao chạy mượt)', N'Xem video Youtube không quảng cáo, hỗ trợ phát nhạc trong nền và tải xuống offline. Nâng cấp tài khoản chính chủ qua Family group.', 'https://via.placeholder.com/300x160/fd761a/ffffff?text=Youtube+Premium', 0, 0),
(7, 7, 36, N'Gói Tài Khoản Canva Pro Thiết Kế 1 Năm Trọn Gói', N'Mở khóa toàn bộ tính năng Canva Pro: hàng triệu ảnh, video, font chữ cao cấp và công cụ xóa nền thông minh 1-click.', 'https://via.placeholder.com/300x160/fd761a/ffffff?text=Canva+Pro', 0, 0),
(8, 8, 7, N'Combo 10 Gmail Việt Cổ 2018 - 2020 Cực Sạch Có Sẵn Kênh Youtube', N'Tài khoản Gmail Việt Nam đăng ký từ năm 2018-2020 cực kỳ uy tín, độ trust cao, phù hợp chạy quảng cáo hoặc làm kênh MMO.', 'https://via.placeholder.com/300x160/fd761a/ffffff?text=Gmail+Co', 0, 0),
(9, 9, 49, N'Tool Nuôi Nick Facebook Auto Like Post Share Độc Quyền', N'Phần mềm tự động tương tác nick Facebook, nuôi tài khoản số lượng lớn, tự động đi seeding, share bài viết hàng loạt an toàn.', 'https://via.placeholder.com/300x160/fd761a/ffffff?text=FB+Nuoi+Nick+Tool', 0, 0),
(10, 10, 27, N'Tài khoản NordVPN Premium 1 Năm Bảo Mật Mã Hóa Cao', N'Dịch vụ mạng riêng ảo (VPN) bảo mật hàng đầu thế giới. Mã hóa dữ liệu duyệt web, truy cập website bị chặn với tốc độ cao.', 'https://via.placeholder.com/300x160/fd761a/ffffff?text=NordVPN+1Year', 0, 0),
(11, 11, 38, N'Tăng 1000 Follower Thật TikTok Việt Tốc Độ Nhanh Tự Nhiên', N'Dịch vụ tăng 1000 lượt theo dõi thật cho tài khoản TikTok Việt Nam. Đảm bảo an toàn 100% cho tài khoản, hỗ trợ bật livestream.', 'https://via.placeholder.com/300x160/fd761a/ffffff?text=TikTok+Followers', 0, 0),
(12, 2, 63, N'Ví Điện Tử Trust Wallet Hạn Cổ Có Sẵn Cụm Từ Bảo Mật', N'Ví tiền điện tử Trust Wallet được tạo từ lâu, đi kèm 12 ký tự bảo mật (seed phrase) sạch sẽ, thích hợp chứa tài sản số hoặc giao dịch.', 'https://via.placeholder.com/300x160/fd761a/ffffff?text=Trust+Wallet', 0, 0),
(13, 12, 54, N'Gói Dịch Vụ Thiết Kế Logo & Banner Chuyên Nghiệp (Không thuộc danh mục chính)', N'Thiết kế bộ nhận diện thương hiệu cơ bản bao gồm 1 logo và 1 banner facebook/website chuyên nghiệp theo đúng yêu cầu.', 'https://via.placeholder.com/300x160/fd761a/ffffff?text=Logo+Design', 0, 0);

SET IDENTITY_INSERT Products OFF;
GO

-- 5. Khởi tạo biến thể sản phẩm (ProductVariants)
INSERT INTO ProductVariants (product_id, variant_name, price_vnd, stock, status, isDelete)
VALUES 
(1, N'Netflix Premium 1 Tháng (Shared)', 65000, 156, 'Active', 0),
(2, N'Netflix Premium 1 Năm (Chính Chủ)', 650000, 42, 'Active', 0),
(3, N'ChatGPT Plus 1 Tháng', 150000, 89, 'Active', 0),
(4, N'Spotify Premium 1 Năm', 250000, 45, 'Active', 0),
(5, N'Windows 11 Pro Key', 99000, 999, 'Active', 0),
(6, N'Youtube Premium 6 Tháng', 120000, 230, 'Active', 0),
(7, N'Canva Pro 1 Năm', 180000, 120, 'Active', 0),
(8, N'Combo 10 Gmail', 35000, 500, 'Active', 0),
(9, N'Tool Nuôi Nick FB Vĩnh Viễn', 850000, 75, 'Active', 0),
(10, N'NordVPN Premium 1 Năm', 350000, 15, 'Active', 0),
(11, N'TikTok 1000 Follower', 95000, 9999, 'Active', 0),
(12, N'Trust Wallet Cổ', 150000, 0, 'Active', 0),
(13, N'Gói Thiết Kế Banner Logo', 299000, 50, 'Active', 0);
GO

-- ==============================================================================
-- HOÀN TẤT SETUP DATABASE
-- ==============================================================================
PRINT N'';
PRINT N'======================================================';
PRINT N'✓ CƠ SỞ DỮ LIỆU MMO_SYSTEM ĐÃ ĐƯỢC KHỞI TẠO HOÀN CHỈNH!';
PRINT N'======================================================';
GO
