-- ==============================================================================
-- CƠ SỞ DỮ LIỆU: MMO MARKET SYSTEM (SQL SERVER)
-- ==============================================================================

IF NOT EXISTS (SELECT * FROM sys.databases WHERE name = 'MMO_System')
BEGIN
    CREATE DATABASE MMO_System;
END
GO

USE MMO_System;
GO

-- ==========================================
-- PHẦN 1: TÀI KHOẢN VÀ BẢO MẬT
-- ==========================================

CREATE TABLE Users (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255),
    full_name NVARCHAR(255),
    role NVARCHAR(MAX) NOT NULL, -- Ví dụ: {"role": "Customer"}, {"role": "Seller"}
    phone VARCHAR(20),
    shop_status VARCHAR(20) DEFAULT 'Pending',
    balance_vnd BIGINT DEFAULT 0, -- Số dư ví tiền mặt trực tiếp
    permissions NVARCHAR(MAX),
    isVerified BIT DEFAULT 0,
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
    refresh_token VARCHAR(512), -- Lưu refresh token
    refresh_token_expiry_date DATETIME, -- Thời gian hết hạn của refresh token
    is_revoked BIT DEFAULT 0, -- Đánh dấu token đã bị thu hồi hay chưa
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
-- PHẦN 3: QUẢN LÝ SẢN PHẨM VÀ KHO SỐ
-- ==========================================

CREATE TABLE Categories (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(100) NOT NULL,
    description NVARCHAR(500),
    created_at DATETIME DEFAULT GETDATE(),
    isDelete BIT DEFAULT 0
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
    isDelete BIT DEFAULT 0,
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
    isDelete BIT DEFAULT 0,
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

-- Cập nhật khóa ngoại cho DigitalAssets sau khi đã tạo bảng Transactions
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
    complaint_id BIGINT NULL, -- Phân loại chat khiếu nại (có ID) và chat thường (NULL)
    chat_type VARCHAR(20) DEFAULT 'Normal', -- 'Normal' hoặc 'Complaint'
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
    isDelete BIT DEFAULT 0,
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
-- PHẦN 7: HỆ THỐNG VÀ KIỂM TOÁN (SYSTEM & AUDIT)
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

INSERT INTO SystemConfigurations (config_key, config_value, description)
VALUES
('DEFAULT_COMMISSION_PERCENT', '5.0', N'Phần trăm hoa hồng mặc định sàn thu của Seller'),
('MIN_WITHDRAWAL_VND', '50000', N'Số tiền rút tối thiểu'),
('MAINTENANCE_MODE', 'FALSE', N'Trạng thái bảo trì hệ thống (TRUE/FALSE)');
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

USE MMO_System;
GO

-- Password gốc cho tất cả tài khoản: 123456
-- Project đang dùng Spring Security BCryptPasswordEncoder.
-- BCrypt hash dưới đây match với chuỗi: 123456

-- CUSTOMER
DECLARE @PasswordHash VARCHAR(255) = '$2y$10$uRFrFYP.Ld32A.LSRCm10.LlUfbJH7vgelUd4h1dsvZCyMKz1c0Bq';
IF NOT EXISTS (SELECT 1 FROM Users WHERE email = 'customer01@gmail.com')
BEGIN
    INSERT INTO Users
    (email, password, full_name, role, phone, shop_status, balance_vnd, permissions, isVerified, isDelete)
    VALUES
    ('customer01@gmail.com', @PasswordHash, N'Nguyễn Văn Customer', '{"role": "Customer"}', '0901234567', 'Pending', 0, NULL, 1, 0);
END
GO

-- SELLER
DECLARE @PasswordHash VARCHAR(255) = '$2y$10$uRFrFYP.Ld32A.LSRCm10.LlUfbJH7vgelUd4h1dsvZCyMKz1c0Bq';
IF NOT EXISTS (SELECT 1 FROM Users WHERE email = 'seller01@gmail.com')
BEGIN
    INSERT INTO Users
    (email, password, full_name, role, phone, shop_status, balance_vnd, permissions, isVerified, isDelete)
    VALUES
    ('seller01@gmail.com', @PasswordHash, N'Trần Văn Seller', '{"role": "Seller"}', '0902345678', 'Active', 0, NULL, 1, 0);
END
GO

-- STAFF
DECLARE @PasswordHash VARCHAR(255) = '$2y$10$uRFrFYP.Ld32A.LSRCm10.LlUfbJH7vgelUd4h1dsvZCyMKz1c0Bq';
IF NOT EXISTS (SELECT 1 FROM Users WHERE email = 'staff01@gmail.com')
BEGIN
    INSERT INTO Users
    (email, password, full_name, role, phone, shop_status, balance_vnd, permissions, isVerified, isDelete)
    VALUES
    ('staff01@gmail.com', @PasswordHash, N'Lê Văn Staff', '{"role": "Staff"}', '0903456789', 'Approved', 0, NULL, 1, 0);
END
GO

-- ADMIN
DECLARE @PasswordHash VARCHAR(255) = '$2y$10$uRFrFYP.Ld32A.LSRCm10.LlUfbJH7vgelUd4h1dsvZCyMKz1c0Bq';
IF NOT EXISTS (SELECT 1 FROM Users WHERE email = 'admin01@gmail.com')
BEGIN
    INSERT INTO Users
    (email, password, full_name, role, phone, shop_status, balance_vnd, permissions, isVerified, isDelete)
    VALUES
    ('admin01@gmail.com', @PasswordHash, N'Admin MMO System', '{"role": "Admin"}', '0904567890', 'Active', 0, '{"all": true}', 1, 0);
END
GO

SELECT id, email, full_name, role, phone, shop_status, balance_vnd, isVerified, isDelete
FROM Users
WHERE email IN ('customer01@gmail.com', 'seller01@gmail.com', 'customerseller01@gmail.com', 'admin01@gmail.com')
ORDER BY id;
GO
