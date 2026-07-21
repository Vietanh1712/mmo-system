-- ==============================================================================
-- CƠ SỞ DỮ LIỆU TOÀN DIỆN: MMO MARKET SYSTEM (SQL SERVER)
-- Tên tệp: MMO_System_Schema.sql
-- Mô tả: Khởi tạo database, định nghĩa toàn bộ bảng biểu, ràng buộc,
--       triggers nghiệp vụ và nạp dữ liệu seed chuẩn (Categories, Users, Products,
--       ProductVariants, DigitalAssets, SystemConfigurations).
--       Đồng bộ 100% với các JPA Entity mapping trong mã nguồn hệ thống hiện tại.
-- ==============================================================================

-- 1. KHỞI TẠO CƠ SỞ DỮ LIỆU
IF NOT EXISTS (SELECT * FROM sys.databases WHERE name = 'MMO_System_Schema')
BEGIN
    CREATE DATABASE MMO_System_Schema;
END
GO

USE MMO_System_Schema;
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
IF OBJECT_ID('ShopFollowers', 'U') IS NOT NULL DROP TABLE ShopFollowers;
IF OBJECT_ID('KYCDocuments', 'U') IS NOT NULL DROP TABLE KYCDocuments;
IF OBJECT_ID('KYCRequests', 'U') IS NOT NULL DROP TABLE KYCRequests;
IF OBJECT_ID('Users', 'U') IS NOT NULL DROP TABLE Users;
GO

-- ==========================================
-- PHẦN 1: TÀI KHOẢN VÀ BẢO MẬT (MAPPED WITH User.java, Authentication.java, EmailVerification.java)
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
    shop_level INT DEFAULT 1,
    flag_3_count INT DEFAULT 0,
    withdrawal_locked BIT DEFAULT 0,
    balance_vnd BIGINT DEFAULT 0,
    deposit_vnd BIGINT DEFAULT 0,
    failed_attempts INT DEFAULT 0,
    lock_time DATETIME2 NULL,
    is_2fa_enabled BIT DEFAULT 0,
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
-- PHẦN 2: THÔNG TIN NGƯỜI BÁN VÀ CỬA HÀNG (KYC - MAPPED WITH SellerRegistration.java, SellerBankInfo.java)
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

CREATE TABLE KYCRequests (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    full_name NVARCHAR(255) NOT NULL,
    citizen_id VARCHAR(20) NOT NULL,
    date_of_birth DATE,
    front_id_image VARCHAR(255) NOT NULL,
    back_id_image VARCHAR(255) NOT NULL,
    selfie_image VARCHAR(255) NOT NULL,
    status VARCHAR(20) DEFAULT 'Pending',
    rejection_reason NVARCHAR(MAX),
    reviewed_by BIGINT NULL,
    reviewed_at DATETIME NULL,
    created_at DATETIME DEFAULT GETDATE(),
    updated_at DATETIME DEFAULT GETDATE(),
    isDelete BIT DEFAULT 0,
    CONSTRAINT FK_KYC_User FOREIGN KEY(user_id) REFERENCES Users(id),
    CONSTRAINT FK_KYC_Staff FOREIGN KEY(reviewed_by) REFERENCES Users(id)
);
GO

CREATE TABLE KYCDocuments (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    kyc_id BIGINT NOT NULL,
    document_type VARCHAR(50),
    file_url VARCHAR(255),
    created_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_KYCDoc_KYC FOREIGN KEY (kyc_id) REFERENCES KYCRequests(id)
);
GO

-- ==========================================
-- PHẦN 3: QUẢN LÝ SẢN PHẨM VÀ KHO SỐ (MAPPED WITH Category.java, Product.java, ProductVariant.java, DigitalAsset.java)
-- ==========================================

CREATE TABLE Categories (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(100) NOT NULL,
    parent_id BIGINT NULL,
    description NVARCHAR(500),
    created_at DATETIME DEFAULT GETDATE(),
    updated_at DATETIME DEFAULT GETDATE(),
    is_delete BIT DEFAULT 0, -- Đồng bộ với Category.java: @Column(name = "is_delete")
    isDelete BIT DEFAULT 0,  -- Dự phòng tương thích ngược
    CONSTRAINT FK_Category_Parent FOREIGN KEY (parent_id) REFERENCES Categories(id) ON DELETE NO ACTION
);
GO

CREATE TABLE Products (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    seller_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    name NVARCHAR(500) NOT NULL, -- Tương thích với mô tả sản phẩm dài
    description NVARCHAR(MAX),
    image VARCHAR(255),
    product_image_url NVARCHAR(500) NULL, -- Lưu trữ ảnh chi tiết sản phẩm
    product_type NVARCHAR(20) NOT NULL DEFAULT 'ACCOUNT', -- ACCOUNT | KEY | GAME_CARD
    created_at DATETIME DEFAULT GETDATE(),
    isDelete BIT DEFAULT 0,  -- Đồng bộ với Product.java: @Column(name = "isDelete")
    is_delete BIT DEFAULT 0, -- Dự phòng tương thích
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
    isDelete BIT DEFAULT 0, -- Đồng bộ với ProductVariant.java: @Column(name = "isDelete")
    CONSTRAINT FK_Variants_Product FOREIGN KEY (product_id) REFERENCES Products(id) ON DELETE NO ACTION
);
GO

CREATE TABLE DigitalAssets (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    variant_id          BIGINT NOT NULL,
    asset_type          NVARCHAR(20) NOT NULL,       -- ACCOUNT | KEY | GAME_CARD
    asset_data          NVARCHAR(MAX) NOT NULL,      -- Dữ liệu JSON dự phòng
    account_username    NVARCHAR(255) NULL,          -- Cho loại ACCOUNT
    account_password    NVARCHAR(500) NULL,          -- Cho loại ACCOUNT
    key_code            NVARCHAR(MAX) NULL,          -- Cho loại KEY
    card_code           NVARCHAR(MAX) NULL,          -- Cho loại GAME_CARD
    card_pin            NVARCHAR(255) NULL,          -- Cho loại GAME_CARD
    notes               NVARCHAR(MAX) NULL,          -- Ghi chú chung
    is_used             BIT NOT NULL DEFAULT 0,      -- 0 = còn hàng, 1 = đã bán
    is_delete           BIT NOT NULL DEFAULT 0,      -- Đồng bộ với DigitalAsset.java: @Column(name = "is_delete")
    created_at          DATETIME NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_DigitalAssets_Variant FOREIGN KEY (variant_id) REFERENCES ProductVariants(id) ON DELETE NO ACTION
);
GO

-- ==========================================
-- PHẦN 4: GIAO DỊCH VÀ VÍ ĐIỆN TỬ (TÀI CHÍNH - MAPPED WITH TopupTransaction.java, Transaction.java, Withdrawal.java)
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
    payment_method VARCHAR(50) NULL,
    quantity INT DEFAULT 1 NOT NULL,
    created_at DATETIME DEFAULT GETDATE(),
    isDelete BIT DEFAULT 0,
    CONSTRAINT FK_Trans_Customer FOREIGN KEY (customer_id) REFERENCES Users(id) ON DELETE NO ACTION,
    CONSTRAINT FK_Trans_Seller FOREIGN KEY (seller_id) REFERENCES Users(id) ON DELETE NO ACTION,
    CONSTRAINT FK_Trans_Product FOREIGN KEY (product_id) REFERENCES Products(id) ON DELETE NO ACTION,
    CONSTRAINT FK_Trans_Variant FOREIGN KEY (variant_id) REFERENCES ProductVariants(id) ON DELETE NO ACTION
);
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
    type VARCHAR(50) NOT NULL, -- TOPUP, PAYMENT, REFUND, ESCROW, WITHDRAWAL
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, SUCCESS, FAILED
    reference_id BIGINT NULL,
    reference_code VARCHAR(100) NULL,
    description NVARCHAR(MAX),
    created_at DATETIME DEFAULT GETDATE(),
    isDelete BIT DEFAULT 0,
    CONSTRAINT FK_WalletLog_Users FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE NO ACTION
);
GO

-- ==========================================
-- PHẦN 5: CHĂM SÓC KHÁCH HÀNG VÀ KIỂM DUYỆT (MAPPED WITH Complaint.java, ShopFlag.java, Chat.java)
-- ==========================================

CREATE TABLE Complaints (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    transaction_id BIGINT,
    customer_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    description NVARCHAR(MAX) NOT NULL,
    evidence NVARCHAR(MAX),
    status VARCHAR(20) DEFAULT 'Open',
    preferred_solution VARCHAR(50) NULL,
    resolution NVARCHAR(MAX),
    resolved_by BIGINT NULL,
    resolved_at DATETIME NULL,
    decision_type VARCHAR(50) NULL,
    created_at DATETIME DEFAULT GETDATE(),
    isDelete BIT DEFAULT 0,
    CONSTRAINT FK_Complaints_Trans FOREIGN KEY (transaction_id) REFERENCES Transactions(id) ON DELETE NO ACTION,
    CONSTRAINT FK_Complaints_Customer FOREIGN KEY (customer_id) REFERENCES Users(id) ON DELETE NO ACTION,
    CONSTRAINT FK_Complaints_Seller FOREIGN KEY (seller_id) REFERENCES Users(id) ON DELETE NO ACTION,
    CONSTRAINT FK_Complaints_ResolvedBy FOREIGN KEY (resolved_by) REFERENCES Users(id) ON DELETE NO ACTION
);
GO

CREATE TABLE ShopFlags (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    seller_id BIGINT NOT NULL,
    staff_id BIGINT NOT NULL,
    complaint_id BIGINT NULL,
    reason NVARCHAR(MAX) NOT NULL,
    flag_level VARCHAR(20) DEFAULT 'Warning',
    status VARCHAR(20) DEFAULT 'Pending',
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
    product_id BIGINT NULL,
    chat_type VARCHAR(20) DEFAULT 'Normal',
    message NVARCHAR(MAX) NOT NULL,
    created_at DATETIME DEFAULT GETDATE(),
    isDelete BIT DEFAULT 0,
    sender_deleted BIT DEFAULT 0,
    receiver_deleted BIT DEFAULT 0,
    isRead BIT DEFAULT 0,
    CONSTRAINT FK_Chats_Sender FOREIGN KEY (sender_id) REFERENCES Users(id) ON DELETE NO ACTION,
    CONSTRAINT FK_Chats_Receiver FOREIGN KEY (receiver_id) REFERENCES Users(id) ON DELETE NO ACTION,
    CONSTRAINT FK_Chats_Complaint FOREIGN KEY (complaint_id) REFERENCES Complaints(id) ON DELETE NO ACTION
);
GO


CREATE TABLE ChatMutes (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    contact_id BIGINT NOT NULL,
    created_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_ChatMutes_User FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE NO ACTION,
    CONSTRAINT FK_ChatMutes_Contact FOREIGN KEY (contact_id) REFERENCES Users(id) ON DELETE NO ACTION,
    CONSTRAINT UQ_ChatMutes UNIQUE (user_id, contact_id)
);
GO

-- ==========================================
-- PHẦN 6: TÍNH NĂNG MỞ RỘNG (WISH_LIST, PRE_ORDER, REVIEW, SHOP_FOLLOWERS)
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
    isDelete BIT DEFAULT 0,
    CONSTRAINT FK_PreOrder_Customer FOREIGN KEY (customer_id) REFERENCES Users(id) ON DELETE NO ACTION,
    CONSTRAINT FK_PreOrder_Product FOREIGN KEY (product_id) REFERENCES Products(id) ON DELETE NO ACTION
);
GO

CREATE TABLE Reviews (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    product_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    transaction_id BIGINT NULL,
    rating INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment NVARCHAR(MAX),
    media_url NVARCHAR(MAX),
    created_at DATETIME DEFAULT GETDATE(),
    isDelete BIT DEFAULT 0,
    CONSTRAINT FK_Reviews_Product FOREIGN KEY (product_id) REFERENCES Products(id) ON DELETE NO ACTION,
    CONSTRAINT FK_Reviews_User FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE NO ACTION,
    CONSTRAINT FK_Reviews_Transaction FOREIGN KEY (transaction_id) REFERENCES Transactions(id) ON DELETE NO ACTION
);
GO

CREATE TABLE ShopFollowers (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    follower_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    created_at DATETIME DEFAULT GETDATE(),
    isDelete BIT DEFAULT 0,
    CONSTRAINT FK_Followers_Follower FOREIGN KEY (follower_id) REFERENCES Users(id) ON DELETE NO ACTION,
    CONSTRAINT FK_Followers_Seller FOREIGN KEY (seller_id) REFERENCES Users(id) ON DELETE NO ACTION,
    CONSTRAINT UQ_Follower_Seller UNIQUE (follower_id, seller_id)
);
GO
CREATE INDEX idx_follower ON ShopFollowers(follower_id);
GO
CREATE INDEX idx_seller ON ShopFollowers(seller_id);
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
    user_id BIGINT NULL,
    title NVARCHAR(255) NOT NULL,
    content NVARCHAR(MAX),
    type VARCHAR(50) NOT NULL DEFAULT 'info',
    isRead BIT DEFAULT 0,
    severity VARCHAR(50) DEFAULT 'INFO',
    target_url VARCHAR(500) NULL,
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
-- PHẦN 8: TRIGGERS NGHIỆP VỤ (CHẠY TRÊN SQL SERVER - SET-BASED)
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

-- 2. Khởi tạo danh mục hàng hóa (Categories) - Cấu trúc cây phân cấp
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

-- 12 Sellers cho danh sách sản phẩm mẫu
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

-- Các tài khoản phân quyền đặc thù phục vụ hệ thống
INSERT INTO Users (id, email, password, full_name, gender, address, national_id, date_of_birth, role, phone, shop_status, balance_vnd, isVerified, isLocked, isDelete)
VALUES
(13, 'customer01@gmail.com', @PasswordHash, N'Nguyễn Văn Khách', N'Nam', N'123 Đường Nguyễn Trãi, Hà Nội', '001096001234', '1996-05-15', '{"role": "Customer"}', '0987654321', 'Pending', 500000, 1, 0, 0),
(14, 'staff01@gmail.com', @PasswordHash, N'Trần Thị Nhân Viên', N'Nữ', N'456 Cầu Giấy, Hà Nội', '001098005678', '1998-08-20', '{"role": "Staff"}', '0912345678', 'Approved', 0, 1, 0, 0),
(15, 'admin01@gmail.com', @PasswordHash, N'Admin MMO System', N'Nam', N'Hệ thống MMO Market', '001090009999', '1990-01-01', '{"role": "Admin"}', '0900000000', 'Approved', 0, 1, 0, 0),
-- Tài khoản admin cấu hình hệ thống (admin@mmo.com / mật khẩu 123456)
(16, 'admin@mmo.com', '$2a$10$NcmOXXGkICk.davDnIvgbuUcscMw31mHDhb5oei/4hHOaWZRzE.g6', N'Administrator', N'Nam', N'Hệ thống MMO Market', '001090000000', '1990-01-01', '{"role": "Admin"}', '0123456789', 'Approved', 0, 1, 0, 0);

SET IDENTITY_INSERT Users OFF;
GO

-- 4. Khởi tạo sản phẩm mẫu (Products)
SET IDENTITY_INSERT Products ON;

INSERT INTO Products (id, seller_id, category_id, name, description, image, product_image_url, product_type, isDelete, is_delete)
VALUES 
(1, 1, 27, N'Tài khoản Netflix Premium 4K UHD 1 Tháng (Xem riêng 1 thiết bị, bảo hành 1 đổi 1)', N'Xem phim chất lượng Ultra HD 4K trên mọi thiết bị. Giao tài khoản tự động lập tức sau khi thanh toán. Bảo hành 1 đổi 1 suốt thời gian sử dụng.', 'https://via.placeholder.com/300x160/fd761a/ffffff?text=Netflix+Premium', 'https://via.placeholder.com/300x160/fd761a/ffffff?text=Netflix+Premium', 'ACCOUNT', 0, 0),
(2, 2, 27, N'Tài khoản Netflix Premium 4K UHD Gói 1 Năm (Chính chủ gia hạn ổn định)', N'Gói cước Netflix Premium 12 tháng xem ổn định không lo bị khóa hay đăng xuất. Hỗ trợ xem trên SmartTV, điện thoại, máy tính.', 'https://via.placeholder.com/300x160/fd761a/ffffff?text=Netflix+1Year', 'https://via.placeholder.com/300x160/fd761a/ffffff?text=Netflix+1Year', 'ACCOUNT', 0, 0),
(3, 3, 27, N'Tài khoản ChatGPT Plus (OpenAI GPT-4o) Chính Chủ Sẵn 20$ Hạn 1 Tháng', N'Tài khoản OpenAI nâng cấp sẵn gói Plus trị giá 20$. Sử dụng GPT-4o không giới hạn tốc độ và tính năng mới nhất.', 'https://via.placeholder.com/300x160/fd761a/ffffff?text=ChatGPT+Plus', 'https://via.placeholder.com/300x160/fd761a/ffffff?text=ChatGPT+Plus', 'ACCOUNT', 0, 0),
(4, 4, 27, N'Spotify Premium 1 Năm Giá Siêu Rẻ (Nâng cấp Family email của bạn)', N'Nghe nhạc chất lượng cao không quảng cáo trên Spotify. Nâng cấp trực tiếp trên email cá nhân của bạn thông qua liên kết Family.', 'https://via.placeholder.com/300x160/fd761a/ffffff?text=Spotify+Premium', 'https://via.placeholder.com/300x160/fd761a/ffffff?text=Spotify+Premium', 'ACCOUNT', 0, 0),
(5, 5, 26, N'Key Windows 11 Pro Bản Quyền Vĩnh Viễn (Kèm hướng dẫn active chi tiết)', N'Kích hoạt bản quyền Windows 11 Professional vĩnh viễn theo máy. Hỗ trợ cập nhật đầy đủ, cài đặt lại Win vẫn giữ bản quyền.', 'https://via.placeholder.com/300x160/fd761a/ffffff?text=Windows+11+Key', 'https://via.placeholder.com/300x160/fd761a/ffffff?text=Windows+11+Key', 'KEY', 0, 0),
(6, 6, 27, N'Youtube Premium Không Quảng Cáo 6 Tháng (Add Family bao chạy mượt)', N'Xem video Youtube không quảng cáo, hỗ trợ phát nhạc trong nền và tải xuống offline. Nâng cấp tài khoản chính chủ qua Family group.', 'https://via.placeholder.com/300x160/fd761a/ffffff?text=Youtube+Premium', 'https://via.placeholder.com/300x160/fd761a/ffffff?text=Youtube+Premium', 'ACCOUNT', 0, 0),
(7, 7, 36, N'Gói Tài Khoản Canva Pro Thiết Kế 1 Năm Trọn Gói', N'Mở khóa toàn bộ tính năng Canva Pro: hàng triệu ảnh, video, font chữ cao cấp và công cụ xóa nền thông minh 1-click.', 'https://via.placeholder.com/300x160/fd761a/ffffff?text=Canva+Pro', 'https://via.placeholder.com/300x160/fd761a/ffffff?text=Canva+Pro', 'ACCOUNT', 0, 0),
(8, 8, 7, N'Combo 10 Gmail Việt Cổ 2018 - 2020 Cực Sạch Có Sẵn Kênh Youtube', N'Tài khoản Gmail Việt Nam đăng ký từ năm 2018-2020 cực kỳ uy tín, độ trust cao, phù hợp chạy quảng cáo hoặc làm kênh MMO.', 'https://via.placeholder.com/300x160/fd761a/ffffff?text=Gmail+Co', 'https://via.placeholder.com/300x160/fd761a/ffffff?text=Gmail+Co', 'ACCOUNT', 0, 0),
(9, 9, 49, N'Tool Nuôi Nick Facebook Auto Like Post Share Độc Quyền', N'Phần mềm tự động tương tác nick Facebook, nuôi tài khoản số lượng lớn, tự động đi seeding, share bài viết hàng loạt an toàn.', 'https://via.placeholder.com/300x160/fd761a/ffffff?text=FB+Nuoi+Nick+Tool', 'https://via.placeholder.com/300x160/fd761a/ffffff?text=FB+Nuoi+Nick+Tool', 'KEY', 0, 0),
(10, 10, 27, N'Tài khoản NordVPN Premium 1 Năm Bảo Mật Mã Hóa Cao', N'Dịch vụ mạng riêng ảo (VPN) bảo mật hàng đầu thế giới. Mã hóa dữ liệu duyệt web, truy cập website bị chặn với tốc độ cao.', 'https://via.placeholder.com/300x160/fd761a/ffffff?text=NordVPN+1Year', 'https://via.placeholder.com/300x160/fd761a/ffffff?text=NordVPN+1Year', 'ACCOUNT', 0, 0),
(11, 11, 38, N'Tăng 1000 Follower Thật TikTok Việt Tốc Độ Nhanh Tự Nhiên', N'Dịch vụ tăng 1000 lượt theo dõi thật cho tài khoản TikTok Việt Nam. Đảm bảo an toàn 100% cho tài khoản, hỗ trợ bật livestream.', 'https://via.placeholder.com/300x160/fd761a/ffffff?text=TikTok+Followers', 'https://via.placeholder.com/300x160/fd761a/ffffff?text=TikTok+Followers', 'ACCOUNT', 0, 0),
(12, 2, 63, N'Ví Điện Tử Trust Wallet Hạn Cổ Có Sẵn Cụm Từ Bảo Mật', N'Ví tiền điện tử Trust Wallet được tạo từ lâu, đi kèm 12 ký tự bảo mật (seed phrase) sạch sẽ, thích hợp chứa tài sản số hoặc giao dịch.', 'https://via.placeholder.com/300x160/fd761a/ffffff?text=Trust+Wallet', 'https://via.placeholder.com/300x160/fd761a/ffffff?text=Trust+Wallet', 'KEY', 0, 0),
(13, 12, 54, N'Gói Dịch Vụ Thiết Kế Logo & Banner Chuyên Nghiệp (Không thuộc danh mục chính)', N'Thiết kế bộ nhận diện thương hiệu cơ bản bao gồm 1 logo và 1 banner facebook/website chuyên nghiệp theo đúng yêu cầu.', 'https://via.placeholder.com/300x160/fd761a/ffffff?text=Logo+Design', 'https://via.placeholder.com/300x160/fd761a/ffffff?text=Logo+Design', 'KEY', 0, 0);

SET IDENTITY_INSERT Products OFF;
GO

-- 5. Khởi tạo biến thể sản phẩm (ProductVariants)
INSERT INTO ProductVariants (product_id, variant_name, price_vnd, stock, status, isDelete)
VALUES 
(1, N'1 Tháng (Shared)', 65000, 156, 'Active', 0),
(2, N'1 Năm (Chính Chủ)', 650000, 42, 'Active', 0),
(3, N'1 Tháng', 150000, 89, 'Active', 0),
(4, N'1 Năm', 250000, 45, 'Active', 0),
(5, N'Windows 11 Pro Key', 99000, 999, 'Active', 0),
(6, N'6 Tháng', 120000, 230, 'Active', 0),
(7, N'1 Năm', 180000, 120, 'Active', 0),
(8, N'Combo 10 Gmail', 35000, 500, 'Active', 0),
(9, N'Tool Nuôi Nick FB Vĩnh Viễn', 850000, 75, 'Active', 0),
(10, N'1 Năm', 350000, 15, 'Active', 0),
(11, N'TikTok 1000 Follower', 95000, 9999, 'Active', 0),
(12, N'Trust Wallet Cổ', 150000, 0, 'Active', 0),
(13, N'Gói Thiết Kế Banner Logo', 299000, 50, 'Active', 0);
GO

-- 6. Khởi tạo kho tài sản số mẫu (DigitalAssets) khớp cấu trúc cột chi tiết mới
-- variant_id 1 (Netflix Shared - ACCOUNT)
INSERT INTO DigitalAssets (variant_id, asset_type, asset_data, account_username, account_password, key_code, notes, is_used, is_delete)
VALUES
(1, 'ACCOUNT', N'{"username":"netflix_user_01@gmail.com","password":"NetflixSecure@2026","note":"Tài khoản mới 100%"}', 'netflix_user_01@gmail.com', 'NetflixSecure@2026', NULL, N'Tài khoản mới 100%', 0, 0),
(1, 'ACCOUNT', N'{"username":"netflix_user_02@gmail.com","password":"NetflixPass2026!","note":"Gói Premium 4K"}', 'netflix_user_02@gmail.com', 'NetflixPass2026!', NULL, N'Gói Premium 4K', 0, 0),
(1, 'ACCOUNT', N'{"username":"netflix_user_03@gmail.com","password":"Nf2026@Premium","note":"Màn hình riêng"}', 'netflix_user_03@gmail.com', 'Nf2026@Premium', NULL, N'Màn hình riêng', 0, 0),
(1, 'ACCOUNT', N'{"username":"netflix_user_04@gmail.com","password":"Nf@Account2026","note":"Còn 11 tháng"}', 'netflix_user_04@gmail.com', 'Nf@Account2026', NULL, N'Còn 11 tháng', 0, 0),
(1, 'ACCOUNT', N'{"username":"netflix_user_05@gmail.com","password":"NetflixPro2026","note":"Đổi mk sau nhận"}', 'netflix_user_05@gmail.com', 'NetflixPro2026', NULL, N'Đổi mk sau nhận', 0, 0);

-- variant_id 4 (Spotify Premium - ACCOUNT)
INSERT INTO DigitalAssets (variant_id, asset_type, asset_data, account_username, account_password, key_code, notes, is_used, is_delete)
VALUES
(4, 'ACCOUNT', N'{"username":"spotify_family01@gmail.com","password":"Sp0tify@2026","note":"Gói gia đình 6 slot"}', 'spotify_family01@gmail.com', 'Sp0tify@2026', NULL, N'Gói gia đình 6 slot', 0, 0),
(4, 'ACCOUNT', N'{"username":"spotify_family02@gmail.com","password":"Sp0tify!2026","note":"Còn 5 slot trống"}', 'spotify_family02@gmail.com', 'Sp0tify!2026', NULL, N'Còn 5 slot trống', 0, 0),
(4, 'ACCOUNT', N'{"username":"spotify_family03@gmail.com","password":"SpFamily2026","note":"Còn 4 slot trống"}', 'spotify_family03@gmail.com', 'SpFamily2026', NULL, N'Còn 4 slot trống', 0, 0);

-- variant_id 5 (Windows 11 Key - KEY)
INSERT INTO DigitalAssets (variant_id, asset_type, asset_data, account_username, account_password, key_code, notes, is_used, is_delete)
VALUES
(5, 'KEY', N'{"key":"WIN11-PRO-A1B2-C3D4-E5F6","note":"Key bản quyền OEM kích hoạt Online"}', NULL, NULL, 'WIN11-PRO-A1B2-C3D4-E5F6', N'Key bản quyền OEM kích hoạt Online', 0, 0),
(5, 'KEY', N'{"key":"WIN11-PRO-G7H8-I9J0-K1L2","note":"Key bản quyền OEM"}', NULL, NULL, 'WIN11-PRO-G7H8-I9J0-K1L2', N'Key bản quyền OEM', 0, 0),
(5, 'KEY', N'{"key":"WIN11-PRO-M3N4-O5P6-Q7R8","note":"Key kích hoạt vĩnh viễn"}', NULL, NULL, 'WIN11-PRO-M3N4-O5P6-Q7R8', N'Key kích hoạt vĩnh viễn', 0, 0);

-- variant_id 9 (Tool Facebook - KEY)
INSERT INTO DigitalAssets (variant_id, asset_type, asset_data, account_username, account_password, key_code, notes, is_used, is_delete)
VALUES
(9, 'KEY', N'{"key":"TOOL-FB-NUOI NICK-A1B2-C3D4","note":"Tool Nuôi nick FB vĩnh viễn"}', NULL, NULL, 'TOOL-FB-NUOI NICK-A1B2-C3D4', N'Tool Nuôi nick FB vĩnh viễn', 0, 0),
(9, 'KEY', N'{"key":"TOOL-FB-NUOI NICK-G7H8-I9J0","note":"Tool Nuôi nick FB"}', NULL, NULL, 'TOOL-FB-NUOI NICK-G7H8-I9J0', N'Tool Nuôi nick FB', 0, 0);
GO

-- ==========================================
-- PHẦN 10: NẠP DỮ LIỆU MOCK SELLER PORTAL (TỪ SEED_SellerMockData.sql)
-- ==========================================
PRINT N'🔄 Đang nạp dữ liệu Mock cho Seller Portal...';

-- Cập nhật số dư ví và trạng thái của Seller 01 (id = 2)
UPDATE Users 
SET shop_status = 'Active',
    balance_vnd = 25000000
WHERE id = 2;

-- Seed Seller Bank Info
INSERT INTO SellerBankInfo (user_id, bank_name, account_number, branch, created_at, isDelete)
VALUES (2, N'Vietcombank', '0123456789', N'Chi nhánh Hà Nội', GETDATE(), 0);

-- Dọn dẹp các lịch sử giao dịch, khiếu nại và đánh giá để chuẩn bị cho môi trường chạy thực tế sạch 100%
-- Không chèn sẵn Transactions, Withdrawals, Complaints, Chats, Reviews, ShopFlags mẫu.

PRINT N'✓ Nạp dữ liệu Mock thành công.';
GO

-- ==============================================================================
-- HOÀN TẤT SETUP DATABASE
-- ==============================================================================
PRINT N'';
PRINT N'======================================================';
PRINT N'✓ CƠ SỞ DỮ LIỆU MMO_SYSTEM ĐÃ ĐƯỢC KHỞI TẠO HOÀN CHỈNH VÀ ĐỒNG BỘ 100%!';
PRINT N'======================================================';
GO



-- ==============================================================================
-- MERGED MIGRATION: 20260618_001_create_permissions_and_user_permissions.sql
-- ==============================================================================
-- =============================================================================
-- Migration: Tạo bảng Permissions và UserPermissions
-- Ngày: 2026-06-18
-- Mục đích: Hỗ trợ phân quyền người dùng (RBAC) chi tiết cho Staff và Admin
-- =============================================================================

USE MMO_System_Schema;
GO

IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Permissions')
BEGIN
    CREATE TABLE Permissions (
        id INT IDENTITY(1,1) PRIMARY KEY,
        name VARCHAR(100) NOT NULL UNIQUE,
        group_name NVARCHAR(100) NOT NULL,
        description NVARCHAR(500) NULL,
        created_at DATETIME DEFAULT GETDATE()
    );
    PRINT 'Đã tạo bảng Permissions';
END
GO

IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'UserPermissions')
BEGIN
    CREATE TABLE UserPermissions (
        user_id BIGINT NOT NULL,
        permission_id INT NOT NULL,
        assigned_at DATETIME DEFAULT GETDATE(),
        PRIMARY KEY (user_id, permission_id),
        CONSTRAINT FK_UserPermissions_User FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE,
        CONSTRAINT FK_UserPermissions_Permission FOREIGN KEY (permission_id) REFERENCES Permissions(id) ON DELETE CASCADE
    );
    PRINT 'Đã tạo bảng UserPermissions';
END
GO

-- Seed 8 permissions hệ thống vào bảng Permissions
IF EXISTS (SELECT * FROM sys.tables WHERE name = 'Permissions')
BEGIN
    IF NOT EXISTS (SELECT 1 FROM Permissions WHERE name = 'MANAGE_USERS')
        INSERT INTO Permissions (name, group_name, description) VALUES ('MANAGE_USERS', N'Người dùng', N'Quản lý thông tin và trạng thái người dùng');
    IF NOT EXISTS (SELECT 1 FROM Permissions WHERE name = 'MANAGE_PRODUCTS')
        INSERT INTO Permissions (name, group_name, description) VALUES ('MANAGE_PRODUCTS', N'Sản phẩm', N'Kiểm duyệt và quản lý sản phẩm, biến thể');
    IF NOT EXISTS (SELECT 1 FROM Permissions WHERE name = 'MANAGE_CATEGORIES')
        INSERT INTO Permissions (name, group_name, description) VALUES ('MANAGE_CATEGORIES', N'Danh mục', N'Quản lý danh mục sản phẩm');
    IF NOT EXISTS (SELECT 1 FROM Permissions WHERE name = 'APPROVE_KYC')
        INSERT INTO Permissions (name, group_name, description) VALUES ('APPROVE_KYC', N'KYC', N'Xem và duyệt các yêu cầu xác minh danh tính');
    IF NOT EXISTS (SELECT 1 FROM Permissions WHERE name = 'APPROVE_WITHDRAWALS')
        INSERT INTO Permissions (name, group_name, description) VALUES ('APPROVE_WITHDRAWALS', N'Tài chính', N'Xem và duyệt các yêu cầu rút tiền');
    IF NOT EXISTS (SELECT 1 FROM Permissions WHERE name = 'RESOLVE_COMPLAINTS')
        INSERT INTO Permissions (name, group_name, description) VALUES ('RESOLVE_COMPLAINTS', N'Tranh chấp', N'Giải quyết các khiếu nại và tranh chấp đơn hàng');
    IF NOT EXISTS (SELECT 1 FROM Permissions WHERE name = 'MANAGE_SYSTEM_CONFIG')
        INSERT INTO Permissions (name, group_name, description) VALUES ('MANAGE_SYSTEM_CONFIG', N'Hệ thống', N'Quản lý các cấu hình tham số hệ thống');
    
    PRINT 'Đã seed 7 permissions hệ thống';
END
GO


-- ==============================================================================
-- MERGED MIGRATION: 20260618_002_add_support_tickets.sql
-- ==============================================================================
-- ==============================================================================
-- MIGRATION: ADD SUPPORT TICKETS TABLE (SQL SERVER)
-- Tên tệp: 20260618_001_add_support_tickets.sql
-- Mô tả: Khởi tạo bảng SupportTickets phân tách với bảng Complaints
-- ==============================================================================
USE MMO_System_Schema;
GO

IF OBJECT_ID('SupportTickets', 'U') IS NOT NULL DROP TABLE SupportTickets;
GO

CREATE TABLE SupportTickets (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    category NVARCHAR(100) NOT NULL, -- Lỗi nạp tiền, Lỗi tài khoản, Góp ý, Khác
    title NVARCHAR(255) NOT NULL,
    description NVARCHAR(MAX) NOT NULL,
    status VARCHAR(20) DEFAULT 'Open', -- Open, Processing, Resolved, Closed
    resolution NVARCHAR(MAX) NULL,
    created_at DATETIME DEFAULT GETDATE(),
    isDelete BIT DEFAULT 0,
    CONSTRAINT FK_SupportTickets_Users FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE NO ACTION
);
GO

CREATE INDEX idx_support_tickets_user ON SupportTickets(user_id);
GO


-- ==============================================================================
-- MERGED MIGRATION: 20260618_003_kyc_migration.sql
-- ==============================================================================
USE MMO_System_Schema;
GO
-- =============================================================================
-- MIGRATION: Chuẩn hóa KYC V2 (Strict Mode)
-- =============================================================================

BEGIN TRY
    BEGIN TRANSACTION;
    PRINT '--- BẮT ĐẦU MIGRATION KYC ---';

    -- 1. KIỂM TRA BASELINE (Chỉ chạy migration nếu chưa ở V2)
    IF NOT EXISTS (
        SELECT 1 FROM sys.columns 
        WHERE object_id = OBJECT_ID('dbo.KYCRequests') AND name = 'citizen_id'
    )
    BEGIN
        PRINT '--- Cột citizen_id không tồn tại (đã ở dạng V2 hoặc đã được migrate trước đó). Bỏ qua. ---';
        IF OBJECT_ID('dbo.KYCDocuments', 'U') IS NOT NULL
            DROP TABLE dbo.KYCDocuments;
    END
    ELSE
    BEGIN
        IF OBJECT_ID('dbo.KYCRequests', 'U') IS NULL
            RAISERROR('LỖI BASELINE: Bảng dbo.KYCRequests không tồn tại.', 16, 1);

        IF OBJECT_ID('dbo.KYCDocuments', 'U') IS NULL
            RAISERROR('LỖI BASELINE: Bảng dbo.KYCDocuments không tồn tại.', 16, 1);

        -- Pre-check citizen_id (VARCHAR(20) NOT NULL), status (VARCHAR(20)), isDelete (BIT NULL)
        IF NOT EXISTS (
            SELECT 1 FROM sys.columns c JOIN sys.types t ON c.user_type_id = t.user_type_id
            WHERE c.object_id = OBJECT_ID('dbo.KYCRequests') AND c.name = 'citizen_id' AND t.name = 'varchar' AND c.max_length = 20 AND c.is_nullable = 0
        ) OR NOT EXISTS (
            SELECT 1 FROM sys.columns c JOIN sys.types t ON c.user_type_id = t.user_type_id
            WHERE c.object_id = OBJECT_ID('dbo.KYCRequests') AND c.name = 'status' AND t.name = 'varchar' AND c.max_length = 20
        ) OR NOT EXISTS (
            SELECT 1 FROM sys.columns c JOIN sys.types t ON c.user_type_id = t.user_type_id
            WHERE c.object_id = OBJECT_ID('dbo.KYCRequests') AND c.name = 'isDelete' AND t.name = 'bit' AND c.is_nullable = 1
        )
        BEGIN
            RAISERROR('LỖI BASELINE: Cấu hình citizen_id, status hoặc isDelete không đúng kiểu dữ liệu khởi điểm.', 16, 1);
        END

        IF EXISTS (SELECT 1 FROM sys.columns WHERE Name IN ('id_number', 'id_type', 'request_code', 'version', 'active_user_id') AND Object_ID = OBJECT_ID('dbo.KYCRequests'))
            RAISERROR('LỖI BASELINE: Tồn tại cột rác từ version khác. Abort.', 16, 1);

        -- Pre-check rỗng dữ liệu
        IF EXISTS (SELECT 1 FROM dbo.KYCDocuments) OR EXISTS (SELECT 1 FROM dbo.KYCRequests)
            RAISERROR('LỖI DỮ LIỆU: Bảng KYCRequests hoặc KYCDocuments đang có dữ liệu rác. Yêu cầu truncate DB Test trước.', 16, 1);

        -- 2. SCHEMA CHANGES
        DROP TABLE dbo.KYCDocuments;

        EXEC sp_rename 'dbo.KYCRequests.citizen_id', 'id_number', 'COLUMN';
        ALTER TABLE dbo.KYCRequests ALTER COLUMN id_number VARCHAR(50) NOT NULL;

        -- Xử lý Default Status Cũ (Drop 'Pending' cũ, thêm 'PENDING' mới)
        DECLARE @statusConstraintName NVARCHAR(200);
        SELECT @statusConstraintName = d.name 
        FROM sys.default_constraints d JOIN sys.columns c ON d.parent_object_id = c.object_id AND d.parent_column_id = c.column_id 
        WHERE c.name = 'status' AND c.object_id = OBJECT_ID('dbo.KYCRequests');

        IF @statusConstraintName IS NOT NULL
            EXEC('ALTER TABLE dbo.KYCRequests DROP CONSTRAINT ' + @statusConstraintName);
            
        ALTER TABLE dbo.KYCRequests ADD CONSTRAINT DF_KYC_Status DEFAULT 'PENDING' FOR status;

        -- Thêm id_type, request_code, version
        ALTER TABLE dbo.KYCRequests ADD id_type VARCHAR(50) NOT NULL CONSTRAINT DF_KYC_IdType DEFAULT 'CCCD';
        ALTER TABLE dbo.KYCRequests ADD request_code VARCHAR(32) NOT NULL;
        ALTER TABLE dbo.KYCRequests ADD CONSTRAINT UQ_KYC_RequestCode UNIQUE (request_code);
        ALTER TABLE dbo.KYCRequests ADD version INT NOT NULL CONSTRAINT DF_KYC_Version DEFAULT 0;

        -- Xử lý isDelete Default Cũ -> Set BIT NOT NULL
        DECLARE @isDeleteConstraintName NVARCHAR(200);
        SELECT @isDeleteConstraintName = d.name 
        FROM sys.default_constraints d JOIN sys.columns c ON d.parent_object_id = c.object_id AND d.parent_column_id = c.column_id 
        WHERE c.name = 'isDelete' AND c.object_id = OBJECT_ID('dbo.KYCRequests');

        IF @isDeleteConstraintName IS NOT NULL
            EXEC('ALTER TABLE dbo.KYCRequests DROP CONSTRAINT ' + @isDeleteConstraintName);

        ALTER TABLE dbo.KYCRequests ALTER COLUMN isDelete BIT NOT NULL;
        ALTER TABLE dbo.KYCRequests ADD CONSTRAINT DF_KYC_IsDelete DEFAULT 0 FOR isDelete;

        -- Thêm active_user_id
        ALTER TABLE dbo.KYCRequests ADD active_user_id BIGINT NULL;

        -- 3. CONSTRAINTS & INDEXES
        EXEC('CREATE UNIQUE INDEX UQ_KYC_Active_Per_User ON dbo.KYCRequests(active_user_id) WHERE active_user_id IS NOT NULL;');

        EXEC('ALTER TABLE dbo.KYCRequests ADD CONSTRAINT CHK_KYC_Status CHECK (status IN (''PENDING'', ''APPROVED'', ''REJECTED''));');
        EXEC('ALTER TABLE dbo.KYCRequests ADD CONSTRAINT CHK_KYC_IdType CHECK (id_type IN (''CCCD'', ''CMND'', ''PASSPORT'', ''DRIVER_LICENSE''));');

        EXEC('ALTER TABLE dbo.KYCRequests ADD CONSTRAINT CHK_KYC_ActiveState CHECK (
            (isDelete = 1 AND active_user_id IS NULL) OR
            (isDelete = 0 AND status IN (''PENDING'', ''APPROVED'') AND active_user_id = user_id) OR
            (isDelete = 0 AND status = ''REJECTED'' AND active_user_id IS NULL)
        );');

        EXEC('ALTER TABLE dbo.KYCRequests ADD CONSTRAINT CHK_KYC_ReviewState CHECK (
            (status = ''PENDING'' AND reviewed_by IS NULL AND reviewed_at IS NULL AND rejection_reason IS NULL) OR
            (status = ''APPROVED'' AND reviewed_by IS NOT NULL AND reviewed_at IS NOT NULL AND rejection_reason IS NULL) OR
            (status = ''REJECTED'' AND reviewed_by IS NOT NULL AND reviewed_at IS NOT NULL AND rejection_reason IS NOT NULL AND LTRIM(RTRIM(rejection_reason)) != '''')
        );');

        EXEC('CREATE INDEX IDX_KYC_User_Created ON dbo.KYCRequests(user_id, created_at DESC);');
        EXEC('CREATE INDEX IDX_KYC_Status_Created ON dbo.KYCRequests(status, created_at DESC);');
        EXEC('CREATE INDEX IDX_KYC_Reviewer_Date ON dbo.KYCRequests(reviewed_by, reviewed_at DESC);');
    END

    PRINT '--- MIGRATION HOÀN TẤT THÀNH CÔNG ---';
    COMMIT TRANSACTION;

END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    DECLARE @E NVARCHAR(4000) = ERROR_MESSAGE();
    RAISERROR(@E, 16, 1);
END CATCH;
GO


USE MMO_System_Schema;
GO
-- 1. Check Constraint
SELECT name, definition FROM sys.check_constraints WHERE parent_object_id = OBJECT_ID('dbo.KYCRequests');

-- 2. Check Index definition
SELECT i.name, i.filter_definition, c.name as column_name 
FROM sys.indexes i JOIN sys.index_columns ic ON i.object_id = ic.object_id AND i.index_id = ic.index_id
JOIN sys.columns c ON ic.object_id = c.object_id AND ic.column_id = c.column_id
WHERE i.object_id = OBJECT_ID('dbo.KYCRequests');

-- 3. Check nullability/default
SELECT c.name, t.name as type, c.max_length, c.is_nullable, d.definition as default_val
FROM sys.columns c JOIN sys.types t ON c.user_type_id = t.user_type_id
LEFT JOIN sys.default_constraints d ON c.default_object_id = d.object_id
WHERE c.object_id = OBJECT_ID('dbo.KYCRequests') AND c.name IN ('status', 'isDelete', 'id_number', 'request_code');


-- ==============================================================================
-- MERGED MIGRATION: 20260618_004_add_media_url_to_reviews.sql
-- ==============================================================================
USE MMO_System_Schema;
GO

IF COL_LENGTH('Reviews', 'media_url') IS NULL
BEGIN
    ALTER TABLE Reviews ADD media_url NVARCHAR(MAX) NULL;
END
GO


-- ==============================================================================
-- MERGED MIGRATION: 20260618_005_add_target_fields_to_audit_logs.sql
-- ==============================================================================
-- =============================================================================
-- Migration: Bổ sung các cột Target vào AuditLogs
-- Ngày: 2026-06-18
-- Mục đích: Hỗ trợ log chi tiết đối tượng chịu tác động từ hành động của Admin/Staff
-- =============================================================================

USE MMO_System_Schema;
GO

-- Thêm cột target_user_id vào AuditLogs
IF NOT EXISTS (
    SELECT 1 FROM sys.columns 
    WHERE object_id = OBJECT_ID('AuditLogs') 
      AND name = 'target_user_id'
)
BEGIN
    ALTER TABLE AuditLogs
    ADD target_user_id BIGINT NULL;
    PRINT 'Đã thêm cột target_user_id vào AuditLogs';
END
GO

-- Thêm cột target_id vào AuditLogs
IF NOT EXISTS (
    SELECT 1 FROM sys.columns 
    WHERE object_id = OBJECT_ID('AuditLogs') 
      AND name = 'target_id'
)
BEGIN
    ALTER TABLE AuditLogs
    ADD target_id BIGINT NULL;
    PRINT 'Đã thêm cột target_id vào AuditLogs';
END
GO

-- Thêm cột target_type vào AuditLogs
IF NOT EXISTS (
    SELECT 1 FROM sys.columns 
    WHERE object_id = OBJECT_ID('AuditLogs') 
      AND name = 'target_type'
)
BEGIN
    ALTER TABLE AuditLogs
    ADD target_type VARCHAR(100) NULL;
    PRINT 'Đã thêm cột target_type vào AuditLogs';
END
GO


-- ==============================================================================
-- MERGED MIGRATION: 20260618_006_add_transaction_id_to_reviews.sql
-- ==============================================================================
-- Migration: Add transaction_id to Reviews table
-- Purpose: Link each review to a specific transaction so users can review
--          the same product multiple times if they have different purchase orders.
-- Date: 2026-06-18

USE MMO_System_Schema;
GO

IF NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_NAME = 'Reviews' AND COLUMN_NAME = 'transaction_id'
)
BEGIN
    ALTER TABLE Reviews ADD transaction_id BIGINT NULL;
    ALTER TABLE Reviews ADD CONSTRAINT FK_Reviews_Transaction
        FOREIGN KEY (transaction_id) REFERENCES Transactions(id) ON DELETE NO ACTION;
    PRINT 'Added transaction_id column and FK to Reviews table.';
END
ELSE
BEGIN
    PRINT 'Column transaction_id already exists in Reviews table. Skipping.';
END
GO


-- ==============================================================================
-- MERGED MIGRATION: 20260618_007_drop_redundant_kyc_columns.sql
-- ==============================================================================
-- Migration: Drop redundant KYC columns
-- Description: Drop full_name and date_of_birth from KYCRequests since they are now managed in the Users table via KycRequest -> User entity relationship.
-- Author: Antigravity Agent
-- Date: 2026-06-18

USE MMO_System_Schema;
GO

-- 1. Pre-check and drop full_name
IF EXISTS (
    SELECT 1 
    FROM sys.columns 
    WHERE Name = N'full_name' AND Object_ID = Object_ID(N'dbo.KYCRequests')
)
BEGIN
    ALTER TABLE dbo.KYCRequests DROP COLUMN full_name;
    PRINT 'Dropped column full_name from KYCRequests.';
END
ELSE
BEGIN
    PRINT 'Column full_name does not exist in KYCRequests.';
END
GO

-- 2. Pre-check and drop date_of_birth
IF EXISTS (
    SELECT 1 
    FROM sys.columns 
    WHERE Name = N'date_of_birth' AND Object_ID = Object_ID(N'dbo.KYCRequests')
)
BEGIN
    ALTER TABLE dbo.KYCRequests DROP COLUMN date_of_birth;
    PRINT 'Dropped column date_of_birth from KYCRequests.';
END
ELSE
BEGIN
    PRINT 'Column date_of_birth does not exist in KYCRequests.';
END
GO

-- 3. Verification
PRINT 'Verification: Current columns in KYCRequests';
SELECT column_name, data_type, is_nullable
FROM INFORMATION_SCHEMA.COLUMNS
WHERE table_name = 'KYCRequests';
GO


-- ==============================================================================
-- MERGED MIGRATION: 20260618_008_add_reviewer_and_rejection_reason_to_withdrawals.sql
-- ==============================================================================
-- =============================================================================
-- Migration: Bổ sung các cột duyệt yêu cầu rút tiền vào Withdrawals
-- Ngày: 2026-06-18
-- Mục đích: Lưu trữ lịch sử duyệt rút tiền của Staff/Admin cùng lý do từ chối nếu có
-- =============================================================================

USE MMO_System_Schema;
GO

-- Thêm cột reviewed_by vào Withdrawals
IF NOT EXISTS (
    SELECT 1 FROM sys.columns 
    WHERE object_id = OBJECT_ID('Withdrawals') 
      AND name = 'reviewed_by'
)
BEGIN
    ALTER TABLE Withdrawals
    ADD reviewed_by BIGINT NULL;
    
    -- Thêm khoá ngoại trỏ tới Users
    ALTER TABLE Withdrawals
    ADD CONSTRAINT FK_Withdrawals_ReviewedBy FOREIGN KEY (reviewed_by) REFERENCES Users(id) ON DELETE NO ACTION;
    
    PRINT 'Đã thêm cột reviewed_by và FK tương ứng vào Withdrawals';
END
GO

-- Thêm cột reviewed_at vào Withdrawals
IF NOT EXISTS (
    SELECT 1 FROM sys.columns 
    WHERE object_id = OBJECT_ID('Withdrawals') 
      AND name = 'reviewed_at'
)
BEGIN
    ALTER TABLE Withdrawals
    ADD reviewed_at DATETIME NULL;
    PRINT 'Đã thêm cột reviewed_at vào Withdrawals';
END
GO

-- Thêm cột rejection_reason vào Withdrawals
IF NOT EXISTS (
    SELECT 1 FROM sys.columns 
    WHERE object_id = OBJECT_ID('Withdrawals') 
      AND name = 'rejection_reason'
)
BEGIN
    ALTER TABLE Withdrawals
    ADD rejection_reason NVARCHAR(MAX) NULL;
    PRINT 'Đã thêm cột rejection_reason vào Withdrawals';
END
GO

-- Thêm cột fee_vnd vào Withdrawals
IF NOT EXISTS (
    SELECT 1 FROM sys.columns 
    WHERE object_id = OBJECT_ID('Withdrawals') 
      AND name = 'fee_vnd'
)
BEGIN
    ALTER TABLE Withdrawals
    ADD fee_vnd BIGINT NULL DEFAULT 0;
    PRINT 'Đã thêm cột fee_vnd vào Withdrawals';
END
GO


-- ==============================================================================
-- MERGED MIGRATION: 20260618_009_chat_features.sql
-- ==============================================================================
-- Migration: Add chat delete/block/mute features
-- Date: 2026-06-18

USE MMO_System_Schema;
GO


IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('Chats') AND name = 'sender_deleted')
BEGIN
    ALTER TABLE Chats ADD sender_deleted BIT DEFAULT 0;
END
GO

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('Chats') AND name = 'receiver_deleted')
BEGIN
    ALTER TABLE Chats ADD receiver_deleted BIT DEFAULT 0;
END
GO


-- Create ChatMutes table
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID('ChatMutes') AND type = 'U')
BEGIN
    CREATE TABLE ChatMutes (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        user_id BIGINT NOT NULL,
        contact_id BIGINT NOT NULL,
        created_at DATETIME DEFAULT GETDATE(),
        CONSTRAINT FK_ChatMutes_User FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE NO ACTION,
        CONSTRAINT FK_ChatMutes_Contact FOREIGN KEY (contact_id) REFERENCES Users(id) ON DELETE NO ACTION,
        CONSTRAINT UQ_ChatMutes UNIQUE (user_id, contact_id)
    );
END
GO


-- ==============================================================================
-- MERGED MIGRATION: 20260618_010_add_resolution_fields_to_complaints.sql
-- ==============================================================================
-- =============================================================================
-- Migration: Bổ sung các cột phân xử tranh chấp vào Complaints
-- Ngày: 2026-06-18
-- Mục đích: Lưu trữ lịch sử giải quyết khiếu nại của Staff/Admin cùng loại quyết định
-- =============================================================================

USE MMO_System_Schema;
GO

-- Thêm cột resolved_by vào Complaints
IF NOT EXISTS (
    SELECT 1 FROM sys.columns 
    WHERE object_id = OBJECT_ID('Complaints') 
      AND name = 'resolved_by'
)
BEGIN
    ALTER TABLE Complaints
    ADD resolved_by BIGINT NULL;
    
    -- Thêm khoá ngoại trỏ tới Users
    ALTER TABLE Complaints
    ADD CONSTRAINT FK_Complaints_ResolvedBy FOREIGN KEY (resolved_by) REFERENCES Users(id) ON DELETE NO ACTION;
    
    PRINT 'Đã thêm cột resolved_by và FK tương ứng vào Complaints';
END
GO

-- Thêm cột resolved_at vào Complaints
IF NOT EXISTS (
    SELECT 1 FROM sys.columns 
    WHERE object_id = OBJECT_ID('Complaints') 
      AND name = 'resolved_at'
)
BEGIN
    ALTER TABLE Complaints
    ADD resolved_at DATETIME NULL;
    PRINT 'Đã thêm cột resolved_at vào Complaints';
END
GO

-- Thêm cột decision_type vào Complaints
IF NOT EXISTS (
    SELECT 1 FROM sys.columns 
    WHERE object_id = OBJECT_ID('Complaints') 
      AND name = 'decision_type'
)
BEGIN
    ALTER TABLE Complaints
    ADD decision_type VARCHAR(50) NULL; -- VD: REFUND_CUSTOMER, PAY_SELLER, DISMISS
    PRINT 'Đã thêm cột decision_type vào Complaints';
END
GO


-- ==============================================================================
-- MERGED MIGRATION: 20260618_011_add_is_global_to_notifications.sql
-- ==============================================================================
-- =============================================================================
-- Migration: Cập nhật bảng Notifications để hỗ trợ thông báo toàn hệ thống (Broadcast)
-- Ngày: 2026-06-18
-- Mục đích: Sửa user_id thành Nullable và bổ sung cột is_global
-- =============================================================================

USE MMO_System_Schema;
GO

-- Sửa cột user_id thành Nullable trong bảng Notifications
IF EXISTS (
    SELECT 1 FROM sys.columns 
    WHERE object_id = OBJECT_ID('Notifications') 
      AND name = 'user_id' 
      AND is_nullable = 0
)
BEGIN
    ALTER TABLE Notifications
    ALTER COLUMN user_id BIGINT NULL;
    PRINT 'Đã sửa cột user_id thành Nullable trong Notifications';
END
GO

-- Thêm cột is_global vào Notifications
IF NOT EXISTS (
    SELECT 1 FROM sys.columns 
    WHERE object_id = OBJECT_ID('Notifications') 
      AND name = 'is_global'
)
BEGIN
    ALTER TABLE Notifications
    ADD is_global BIT NOT NULL DEFAULT 0;
    PRINT 'Đã thêm cột is_global vào Notifications';
END
GO


-- ==============================================================================
-- MERGED MIGRATION: 20260619_001_add_variant_image_column.sql
-- ==============================================================================
-- ==============================================================================
-- ADD IMAGE URL TO PRODUCT VARIANTS
-- File: 20260619_AddVariantImageColumn.sql
-- Description: Add image_url column to ProductVariants table.
-- ==============================================================================

USE MMO_System_Schema;
GO

IF NOT EXISTS (
    SELECT 1 
    FROM sys.columns 
    WHERE object_id = OBJECT_ID('dbo.ProductVariants') 
      AND name = 'image_url'
)
BEGIN
    ALTER TABLE dbo.ProductVariants
    ADD image_url NVARCHAR(500) NULL;
    PRINT N'✓ Added column image_url to ProductVariants table.';
END
ELSE
BEGIN
    PRINT N'✓ Column image_url already exists in ProductVariants table.';
END
GO


-- ==============================================================================
-- MERGED MIGRATION: 20260619_002_fix_categories_is_delete.sql
-- ==============================================================================
-- ==============================================================================
-- FIX CATEGORY DELETION COLUMNS
-- File: 20260619_FixCategoriesIsDelete.sql
-- Description: Reset column values to 0 where they are NULL.
-- ==============================================================================

USE MMO_System_Schema;
GO

-- 1. Ensure any null is_delete or isDelete columns are active (0)
UPDATE Categories
SET is_delete = 0
WHERE is_delete IS NULL;

UPDATE Categories
SET isDelete = 0
WHERE isDelete IS NULL;
GO

PRINT N'✓ Categories deletion status fixed successfully.';
GO


-- ==============================================================================
-- MERGED MIGRATION: 20260620_001_sync_system_configurations_and_triggers.sql
-- ==============================================================================
-- =============================================================================
-- Migration: Đồng bộ cấu hình hệ thống và cập nhật triggers động
-- Ngày: 2026-06-20
-- Mục đích: Đảm bảo toàn bộ các tham số cấu hình hệ thống được khởi tạo mặc định
--          và cập nhật các triggers để truy vấn giá trị động thay vì hardcode.
-- =============================================================================

USE MMO_System_Schema;
GO

-- 1. Bổ sung các cấu hình hệ thống mặc định nếu chưa tồn tại
IF EXISTS (SELECT * FROM sys.tables WHERE name = 'SystemConfigurations')
BEGIN
    -- Cấu hình chung (General Configurations)
    IF NOT EXISTS (SELECT 1 FROM SystemConfigurations WHERE config_key = 'SESSION_TIMEOUT_MINS')
        INSERT INTO SystemConfigurations (config_key, config_value, description)
        VALUES ('SESSION_TIMEOUT_MINS', '15', N'Thời gian phiên đăng nhập (phút)');

    IF NOT EXISTS (SELECT 1 FROM SystemConfigurations WHERE config_key = 'OTP_TIMEOUT_MINS')
        INSERT INTO SystemConfigurations (config_key, config_value, description)
        VALUES ('OTP_TIMEOUT_MINS', '5', N'Thời gian hiệu lực của mã OTP (phút)');

    IF NOT EXISTS (SELECT 1 FROM SystemConfigurations WHERE config_key = 'MAX_LOGIN_RETRIES')
        INSERT INTO SystemConfigurations (config_key, config_value, description)
        VALUES ('MAX_LOGIN_RETRIES', '5', N'Số lần đăng nhập sai tối đa');

    IF NOT EXISTS (SELECT 1 FROM SystemConfigurations WHERE config_key = 'LOCK_DURATION_MINS')
        INSERT INTO SystemConfigurations (config_key, config_value, description)
        VALUES ('LOCK_DURATION_MINS', '15', N'Thời gian khóa tài khoản tạm thời (phút)');

    IF NOT EXISTS (SELECT 1 FROM SystemConfigurations WHERE config_key = 'ESCROW_HOLD_HOURS')
        INSERT INTO SystemConfigurations (config_key, config_value, description)
        VALUES ('ESCROW_HOLD_HOURS', '72', N'Thời gian đóng băng tiền giao dịch bảo trợ Escrow (giờ)');

    IF NOT EXISTS (SELECT 1 FROM SystemConfigurations WHERE config_key = 'ALLOW_GOOGLE_LOGIN')
        INSERT INTO SystemConfigurations (config_key, config_value, description)
        VALUES ('ALLOW_GOOGLE_LOGIN', 'true', N'Cho phép đăng nhập bằng tài khoản Google (true/false)');

    IF NOT EXISTS (SELECT 1 FROM SystemConfigurations WHERE config_key = 'ALLOW_REGISTER')
        INSERT INTO SystemConfigurations (config_key, config_value, description)
        VALUES ('ALLOW_REGISTER', 'true', N'Cho phép đăng ký tài khoản mới (true/false)');

    IF NOT EXISTS (SELECT 1 FROM SystemConfigurations WHERE config_key = 'REQUIRE_WITHDRAW_2FA')
        INSERT INTO SystemConfigurations (config_key, config_value, description)
        VALUES ('REQUIRE_WITHDRAW_2FA', 'true', N'Bắt buộc xác thực OTP (2FA) khi rút tiền (true/false)');

    -- Cấu hình phí & hoa hồng (Commissions & Fees)
    IF NOT EXISTS (SELECT 1 FROM SystemConfigurations WHERE config_key = 'WITHDRAWAL_FEE_PERCENT')
        INSERT INTO SystemConfigurations (config_key, config_value, description)
        VALUES ('WITHDRAWAL_FEE_PERCENT', '1.5', N'Phần trăm phí rút tiền (%)');

    IF NOT EXISTS (SELECT 1 FROM SystemConfigurations WHERE config_key = 'MIN_WITHDRAW_FEE_VND')
        INSERT INTO SystemConfigurations (config_key, config_value, description)
        VALUES ('MIN_WITHDRAW_FEE_VND', '10000', N'Phí rút tiền tối thiểu (VNĐ)');

    IF NOT EXISTS (SELECT 1 FROM SystemConfigurations WHERE config_key = 'MAX_WITHDRAWAL_VND')
        INSERT INTO SystemConfigurations (config_key, config_value, description)
        VALUES ('MAX_WITHDRAWAL_VND', '50000000', N'Số tiền rút tối đa trong một giao dịch (VNĐ)');

    IF NOT EXISTS (SELECT 1 FROM SystemConfigurations WHERE config_key = 'MIN_DEPOSIT_LIMIT_VND')
        INSERT INTO SystemConfigurations (config_key, config_value, description)
        VALUES ('MIN_DEPOSIT_LIMIT_VND', '10000', N'Số tiền nạp tối thiểu (VNĐ)');

    IF NOT EXISTS (SELECT 1 FROM SystemConfigurations WHERE config_key = 'MAX_DEPOSIT_LIMIT_VND')
        INSERT INTO SystemConfigurations (config_key, config_value, description)
        VALUES ('MAX_DEPOSIT_LIMIT_VND', '50000000', N'Số tiền nạp tối đa trong một giao dịch (VNĐ)');

    IF NOT EXISTS (SELECT 1 FROM SystemConfigurations WHERE config_key = 'SELLER_UPGRADE_FEE_VND')
        INSERT INTO SystemConfigurations (config_key, config_value, description)
        VALUES ('SELLER_UPGRADE_FEE_VND', '50000', N'Phí nâng cấp tài khoản bán hàng Seller (VNĐ)');

    IF NOT EXISTS (SELECT 1 FROM SystemConfigurations WHERE config_key = 'PRODUCT_FEATURED_FEE_VND')
        INSERT INTO SystemConfigurations (config_key, config_value, description)
        VALUES ('PRODUCT_FEATURED_FEE_VND', '10000', N'Phí đẩy tin nổi bật sản phẩm (VNĐ)');

    IF NOT EXISTS (SELECT 1 FROM SystemConfigurations WHERE config_key = 'FLAT_BUYER_FEE_VND')
        INSERT INTO SystemConfigurations (config_key, config_value, description)
        VALUES ('FLAT_BUYER_FEE_VND', '1000', N'Phí cố định thu từ người mua trên mỗi đơn hàng (VNĐ)');

    PRINT 'Đã bổ sung cấu hình hệ thống mặc định đầy đủ.';
END
GO

-- 2. Cập nhật trigger trg_CheckWithdrawalMin kiểm tra số tiền rút tối thiểu động từ cấu hình
CREATE OR ALTER TRIGGER trg_CheckWithdrawalMin
ON Withdrawals
AFTER INSERT
AS
BEGIN
    DECLARE @MinWithdrawal BIGINT = 50000;
    SELECT @MinWithdrawal = TRY_CAST(config_value AS BIGINT) 
    FROM SystemConfigurations 
    WHERE config_key = 'MIN_WITHDRAWAL_VND';
    
    IF @MinWithdrawal IS NULL SET @MinWithdrawal = 50000;

    IF EXISTS (SELECT 1 FROM inserted WHERE amount_vnd < @MinWithdrawal)
    BEGIN
        DECLARE @ErrMsg NVARCHAR(255) = N'Lỗi: Số tiền rút tối thiểu phải là ' + FORMAT(@MinWithdrawal, 'N0') + N' VNĐ theo chính sách sàn.';
        RAISERROR (@ErrMsg, 16, 1);
        ROLLBACK TRANSACTION;
    END
END;
GO
PRINT 'Đã cập nhật trigger trg_CheckWithdrawalMin động.';
GO

-- 3. Cập nhật trigger trg_HoldFundsEscrow thiết lập trạng thái Giữ Tiền và thời gian giam tiền Escrow động từ cấu hình
CREATE OR ALTER TRIGGER trg_HoldFundsEscrow
ON Transactions
AFTER INSERT
AS
BEGIN
    DECLARE @EscrowHoldHours INT = 72;
    SELECT @EscrowHoldHours = TRY_CAST(config_value AS INT) 
    FROM SystemConfigurations 
    WHERE config_key = 'ESCROW_HOLD_HOURS';
    
    IF @EscrowHoldHours IS NULL SET @EscrowHoldHours = 72;

    -- Update statuses to 'Held'
    UPDATE Transactions
    SET status = 'Held'
    FROM Transactions t
    INNER JOIN inserted i ON t.id = i.id
    WHERE t.status IS NULL OR t.status = 'Pending';

    -- Update Escrow release dates dynamically based on shop level and completed order count
    UPDATE t
    SET t.escrow_release_date = DATEADD(HOUR, 
        CASE 
            WHEN u.shop_level = 0 THEN 168 -- 7 days for warned shops (Level 0)
            WHEN u.shop_level = 1 AND (
                SELECT COUNT(*) FROM Transactions tx 
                INNER JOIN Products p2 ON tx.product_id = p2.id 
                WHERE p2.seller_id = p.seller_id AND tx.status IN ('Completed', 'Delivered', 'Paid')
            ) < 20 THEN 168 -- 7 days for new shops (Level 1) under 20 orders
            ELSE @EscrowHoldHours -- 3 days normally
        END, 
        GETDATE())
    FROM Transactions t
    INNER JOIN inserted i ON t.id = i.id
    INNER JOIN Products p ON i.product_id = p.id
    INNER JOIN Users u ON p.seller_id = u.id
    WHERE t.escrow_release_date IS NULL;
END;
GO
PRINT 'Đã cập nhật trigger trg_HoldFundsEscrow động.';
GO


-- ==============================================================================
-- MERGED MIGRATION: 20260623_001_add_shop_registration_columns.sql
-- ==============================================================================
-- Migration: Add category, support_email, support_phone to SellerRegistrations
-- Purpose: Support full shop registration form data
-- Author: Antigravity
-- Date: 2026-06-23

USE MMO_System_Schema;
GO

-- Pre-check
IF EXISTS (SELECT * FROM information_schema.tables WHERE table_name = 'SellerRegistrations')
BEGIN
    PRINT 'Table SellerRegistrations exists. Proceeding with adding columns...';
    
    -- Check and add category
    IF NOT EXISTS (SELECT * FROM information_schema.columns WHERE table_name = 'SellerRegistrations' AND column_name = 'category')
    BEGIN
        ALTER TABLE SellerRegistrations ADD category NVARCHAR(100) NULL;
        PRINT 'Added column category.';
    END
    ELSE
    BEGIN
        PRINT 'Column category already exists.';
    END

    -- Check and add support_email
    IF NOT EXISTS (SELECT * FROM information_schema.columns WHERE table_name = 'SellerRegistrations' AND column_name = 'support_email')
    BEGIN
        ALTER TABLE SellerRegistrations ADD support_email VARCHAR(255) NULL;
        PRINT 'Added column support_email.';
    END
    ELSE
    BEGIN
        PRINT 'Column support_email already exists.';
    END

    -- Check and add support_phone
    IF NOT EXISTS (SELECT * FROM information_schema.columns WHERE table_name = 'SellerRegistrations' AND column_name = 'support_phone')
    BEGIN
        ALTER TABLE SellerRegistrations ADD support_phone VARCHAR(20) NULL;
        PRINT 'Added column support_phone.';
    END
    ELSE
    BEGIN
        PRINT 'Column support_phone already exists.';
    END
END
ELSE
BEGIN
    PRINT 'Error: Table SellerRegistrations does not exist.';
END
GO

-- Verification
SELECT column_name, data_type, character_maximum_length 
FROM information_schema.columns 
WHERE table_name = 'SellerRegistrations' 
  AND column_name IN ('category', 'support_email', 'support_phone');
GO


-- =========================================================================================
-- MIGRATION: Thêm cột rejection_reason cho SellerRegistrations
-- AUTHOR: Agent
-- DATE: 2026-06-23
-- =========================================================================================

-- 1. Pre-check: Nếu chưa có cột rejection_reason thì thêm vào
IF NOT EXISTS (
    SELECT * FROM sys.columns 
    WHERE object_id = OBJECT_ID(N'[dbo].[SellerRegistrations]') 
    AND name = 'rejection_reason'
)
BEGIN
    ALTER TABLE [dbo].[SellerRegistrations]
    ADD [rejection_reason] NVARCHAR(MAX) NULL;
    
    PRINT 'Da them cot rejection_reason thanh cong.';
END
ELSE
BEGIN
    PRINT 'Cot rejection_reason da ton tai.';
END
GO


-- ==============================================================================
-- MERGED MIGRATION: 20260623_002_remove_view_revenue_permission.sql
-- ==============================================================================
-- =============================================================================
-- Migration: Xóa quyền VIEW_REVENUE (Xem báo cáo doanh thu sàn)
-- Ngày: 2026-06-23
-- Mục đích: Loại bỏ quyền xem báo cáo doanh thu sàn khỏi hệ thống
-- =============================================================================

USE MMO_System_Schema;
GO

IF EXISTS (SELECT 1 FROM Permissions WHERE name = 'VIEW_REVENUE')
BEGIN
    DELETE FROM Permissions WHERE name = 'VIEW_REVENUE';
    PRINT 'Đã xóa quyền VIEW_REVENUE khỏi bảng Permissions';
END
ELSE
BEGIN
    PRINT 'Quyền VIEW_REVENUE không tồn tại hoặc đã được xóa trước đó';
END
GO


-- ==============================================================================
-- MERGED MIGRATION: 20260623_003_add_payment_method_to_transactions.sql
-- ==============================================================================

USE MMO_System_Schema;
GO

ALTER TABLE Transactions
ADD payment_method NVARCHAR(50);
GO


EXEC sp_executesql N'
UPDATE Transactions
SET payment_method = ''Wallet''
WHERE id IN (1,2,3);

UPDATE Transactions
SET payment_method = ''Bank Transfer''
WHERE id IN (4,5,6);

UPDATE Transactions
SET payment_method = ''VietQR''
WHERE id IN (7,8);

UPDATE Transactions
SET payment_method = ''Credit Card''
WHERE id IN (9,10);
';

-- ==============================================================================
-- MERGED MIGRATION: 20260623_004_add_type_kyc_to_kyc_requests.sql
-- ==============================================================================

USE MMO_System_Schema;
GO

ALTER TABLE KYCRequests
ADD type_kyc NVARCHAR(50);
GO



EXEC sp_executesql N'
UPDATE KYCRequests
SET type_kyc = ''CCCD''
WHERE id IN (1,2,3, 10);

UPDATE KYCRequests
SET type_kyc = ''Birth certificate''
WHERE id IN (4,5,6);

UPDATE KYCRequests
SET type_kyc = ''Passport''
WHERE id IN (7,8, 9);
';



-- ==============================================================================
-- MERGED MIGRATION: 20260624_001_create_wallet_transactions.sql
-- ==============================================================================
-- Migration: 20260624_001_create_wallet_transactions
-- Description: Create WalletTransactions table for storing all wallet balance history (ledger).

USE MMO_System_Schema;
GO

IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[WalletTransactions]') AND type in (N'U'))
BEGIN
    CREATE TABLE [dbo].[WalletTransactions] (
        [id] BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        [user_id] BIGINT NOT NULL,
        [type] VARCHAR(50) NOT NULL, -- TOPUP, PAYMENT, REFUND, ESCROW, WITHDRAWAL
        [amount_vnd] BIGINT NOT NULL,
        [status] VARCHAR(20) NOT NULL, -- PENDING, SUCCESS, FAILED
        [description] NVARCHAR(255) NULL,
        [reference_code] VARCHAR(100) NULL, -- E.g. MMO-ORD-1234, SEPAY-1234
        [created_at] DATETIME DEFAULT GETDATE(),
        [isDelete] BIT DEFAULT 0,
        CONSTRAINT [FK_WalletTransactions_Users] FOREIGN KEY ([user_id]) REFERENCES [dbo].[Users]([id])
    );
    
    CREATE INDEX [IX_WalletTransactions_UserId] ON [dbo].[WalletTransactions]([user_id], [created_at] DESC);
    PRINT 'Created WalletTransactions table successfully.';
END
ELSE
BEGIN
    PRINT 'WalletTransactions table already exists.';
END
GO


-- ==============================================================================
-- MERGED MIGRATION: 20260625_001_add_transaction_id_to_digital_assets.sql
-- ==============================================================================
-- =========================================================================
-- Migration: Add transaction_id to DigitalAssets
-- Date: 2026-06-25
-- Description: Liên kết tài sản số đã bán với Transaction tương ứng.
-- =========================================================================
USE MMO_System_Schema;
GO

IF NOT EXISTS (
    SELECT 1 
    FROM sys.columns 
    WHERE object_id = OBJECT_ID('DigitalAssets') AND name = 'transaction_id'
)
BEGIN
    ALTER TABLE DigitalAssets
    ADD transaction_id BIGINT NULL;

    -- Thêm foreign key liên kết với Transactions
    ALTER TABLE DigitalAssets
    ADD CONSTRAINT FK_DigitalAssets_Transactions
    FOREIGN KEY (transaction_id) REFERENCES Transactions(id);
END
GO


-- ==============================================================================
-- MERGED MIGRATION: 20260625_002_seed_staff_permissions.sql
-- ==============================================================================
-- =============================================================================
-- Migration: Seed permissions for default Staff user
-- Ngày: 2026-06-25
-- Mục đích: Gán quyền mặc định cho staff01@gmail.com (id = 14)
-- =============================================================================

USE MMO_System_Schema;
GO

-- Kiểm tra và chèn các permission bổ sung nếu chưa có (để đảm bảo đồng bộ với DatabaseSeeder)
IF EXISTS (SELECT * FROM sys.tables WHERE name = 'Permissions')
BEGIN
    IF NOT EXISTS (SELECT 1 FROM Permissions WHERE name = 'FLAG_SELLER')
        INSERT INTO Permissions (name, group_name, description) VALUES ('FLAG_SELLER', N'Kiểm duyệt', N'Cho phép gắn cờ vi phạm (gạch phạt) đối với người bán vi phạm chính sách.');
        
    IF NOT EXISTS (SELECT 1 FROM Permissions WHERE name = 'HANDLE_DISPUTES')
        INSERT INTO Permissions (name, group_name, description) VALUES ('HANDLE_DISPUTES', N'Vận hành', N'Cho phép làm trung gian giải quyết khiếu nại giữa người mua và người bán, hoàn trả hoặc giải ngân tiền Escrow.');
        
    IF NOT EXISTS (SELECT 1 FROM Permissions WHERE name = 'MANAGE_SUPPORT')
        INSERT INTO Permissions (name, group_name, description) VALUES ('MANAGE_SUPPORT', N'Vận hành', N'Cho phép tiếp nhận, phản hồi và hỗ trợ giải đáp các thắc mắc (ticketing/live chat) của khách hàng.');

    IF NOT EXISTS (SELECT 1 FROM Permissions WHERE name = 'MANAGE_SHOPS')
        INSERT INTO Permissions (name, group_name, description) VALUES ('MANAGE_SHOPS', N'Vận hành', N'Cho phép xem, phê duyệt yêu cầu mở gian hàng, khóa hoặc mở khóa hoạt động của các Shop.');
END
GO

-- Gán toàn bộ quyền cho Staff mẫu (user_id = 14, staff01@gmail.com)
IF EXISTS (SELECT * FROM sys.tables WHERE name = 'UserPermissions') AND EXISTS (SELECT 1 FROM Users WHERE id = 14)
BEGIN
    -- APPROVE_KYC
    IF EXISTS (SELECT 1 FROM Permissions WHERE name = 'APPROVE_KYC')
    BEGIN
        DECLARE @KycPermId INT = (SELECT id FROM Permissions WHERE name = 'APPROVE_KYC');
        IF NOT EXISTS (SELECT 1 FROM UserPermissions WHERE user_id = 14 AND permission_id = @KycPermId)
            INSERT INTO UserPermissions (user_id, permission_id) VALUES (14, @KycPermId);
    END

    -- FLAG_SELLER
    IF EXISTS (SELECT 1 FROM Permissions WHERE name = 'FLAG_SELLER')
    BEGIN
        DECLARE @FlagPermId INT = (SELECT id FROM Permissions WHERE name = 'FLAG_SELLER');
        IF NOT EXISTS (SELECT 1 FROM UserPermissions WHERE user_id = 14 AND permission_id = @FlagPermId)
            INSERT INTO UserPermissions (user_id, permission_id) VALUES (14, @FlagPermId);
    END

    -- APPROVE_WITHDRAWALS
    IF EXISTS (SELECT 1 FROM Permissions WHERE name = 'APPROVE_WITHDRAWALS')
    BEGIN
        DECLARE @WithdrawPermId INT = (SELECT id FROM Permissions WHERE name = 'APPROVE_WITHDRAWALS');
        IF NOT EXISTS (SELECT 1 FROM UserPermissions WHERE user_id = 14 AND permission_id = @WithdrawPermId)
            INSERT INTO UserPermissions (user_id, permission_id) VALUES (14, @WithdrawPermId);
    END

    -- HANDLE_DISPUTES
    IF EXISTS (SELECT 1 FROM Permissions WHERE name = 'HANDLE_DISPUTES')
    BEGIN
        DECLARE @DisputePermId INT = (SELECT id FROM Permissions WHERE name = 'HANDLE_DISPUTES');
        IF NOT EXISTS (SELECT 1 FROM UserPermissions WHERE user_id = 14 AND permission_id = @DisputePermId)
            INSERT INTO UserPermissions (user_id, permission_id) VALUES (14, @DisputePermId);
    END

    -- MANAGE_SUPPORT
    IF EXISTS (SELECT 1 FROM Permissions WHERE name = 'MANAGE_SUPPORT')
    BEGIN
        DECLARE @SupportPermId INT = (SELECT id FROM Permissions WHERE name = 'MANAGE_SUPPORT');
        IF NOT EXISTS (SELECT 1 FROM UserPermissions WHERE user_id = 14 AND permission_id = @SupportPermId)
            INSERT INTO UserPermissions (user_id, permission_id) VALUES (14, @SupportPermId);
    END

    -- MANAGE_SHOPS
    IF EXISTS (SELECT 1 FROM Permissions WHERE name = 'MANAGE_SHOPS')
    BEGIN
        DECLARE @ShopsPermId INT = (SELECT id FROM Permissions WHERE name = 'MANAGE_SHOPS');
        IF NOT EXISTS (SELECT 1 FROM UserPermissions WHERE user_id = 14 AND permission_id = @ShopsPermId)
            INSERT INTO UserPermissions (user_id, permission_id) VALUES (14, @ShopsPermId);
    END

    PRINT 'Đã gán toàn bộ 6 permissions cho Staff (id = 14)';
END
GO


-- ==============================================================================
-- MERGED MIGRATION: 20260625_003_add_user_guide_to_products.sql
-- ==============================================================================
-- Migration: Thêm cột user_guide vào bảng Products
-- Tạo bởi Antigravity AI
-- Ngày: 2026-06-25

IF NOT EXISTS (
    SELECT * FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_NAME = 'Products' AND COLUMN_NAME = 'user_guide'
)
BEGIN
    ALTER TABLE Products ADD user_guide NVARCHAR(MAX) NULL;
    PRINT 'Da them cot user_guide vao bang Products thanh cong.';
END
ELSE
BEGIN
    PRINT 'Cot user_guide da ton tai trong bang Products.';
END
GO
