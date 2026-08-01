-- ==============================================================================
-- CƠ SỞ DỮ LIỆU TOÀN DIỆN VÀ HOÀN CHỈNH NHẤT: MMO MARKET SYSTEM (SQL SERVER)
-- Tên tệp: MMO_System_Schema_Complete.sql
-- Tên Database: MMO_System_Schema
-- ==============================================================================

-- 1. KHỞI TẠO CƠ SỞ DỮ LIỆU
IF NOT EXISTS (SELECT * FROM sys.databases WHERE name = 'MMO_System_Schema')
BEGIN
    CREATE DATABASE MMO_System_Schema;
END
GO

USE MMO_System_Schema;
GO

-- 2. DỌN DẸP TOÀN BỘ CSDL CŨ (GỠ SẠCH KHÓA NGOẠI VÀ BẢNG CŨ BAO GỒM CẢ WISHLISTS)
-- 2.1. Xóa toàn bộ Foreign Key Constraints trước để tránh lỗi rào cản phụ thuộc (FK dependency)
DECLARE @drop_fk_sql NVARCHAR(MAX) = N'';
SELECT @drop_fk_sql += 'ALTER TABLE [' + OBJECT_SCHEMA_NAME(parent_object_id) + '].[' + OBJECT_NAME(parent_object_id) + '] DROP CONSTRAINT [' + name + '];' + CHAR(13)
FROM sys.foreign_keys;
EXEC sp_executesql @drop_fk_sql;
GO

-- 2.2. Xóa sạch 100% tất cả các bảng tồn tại trong database (Xóa bỏ Wishlists và toàn bộ bảng cũ)
DECLARE @drop_table_sql NVARCHAR(MAX) = N'';
SELECT @drop_table_sql += 'DROP TABLE [' + TABLE_SCHEMA + '].[' + TABLE_NAME + '];' + CHAR(13)
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_TYPE = 'BASE TABLE';
EXEC sp_executesql @drop_table_sql;
GO

-- ==============================================================================
-- PHẦN 1: TÀI KHOẢN VÀ BẢO MẬT (MAPPED WITH User.java, Authentication.java, EmailVerification.java)
-- ==============================================================================
CREATE TABLE Users (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NULL,
    full_name NVARCHAR(255) NULL,
    gender NVARCHAR(20) NULL,
    address NVARCHAR(500) NULL,
    avatar NVARCHAR(255) NULL,
    national_id VARCHAR(20) NULL,
    date_of_birth DATE NULL,
    role NVARCHAR(MAX) NOT NULL, -- Định dạng JSON: {"role": "Customer"} hoặc {"role": "Seller"}
    phone VARCHAR(20) NULL,
    shop_status VARCHAR(20) DEFAULT 'Pending', -- Pending, Active, Banned
    shop_level INT NOT NULL DEFAULT 1, -- Level 1 = Shop Mới, Level 2 = Uy tín, Level 0 = Cảnh cáo
    flag_3_count INT NOT NULL DEFAULT 0,
    withdrawal_locked BIT NOT NULL DEFAULT 0,
    balance_vnd BIGINT DEFAULT 0,
    deposit_vnd BIGINT NOT NULL DEFAULT 0,
    permissions NVARCHAR(MAX) NULL,
    isVerified BIT DEFAULT 0,
    isLocked BIT DEFAULT 0,
    is_2fa_enabled BIT NOT NULL DEFAULT 0,
    failed_attempts INT NOT NULL DEFAULT 0,
    lock_time DATETIME2(6) NULL,
    suspended_until DATETIME2(6) NULL,
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
    provider VARCHAR(50) NOT NULL, -- GOOGLE, PASSWORD
    third_party_token VARCHAR(255) NULL,
    refresh_token VARCHAR(512) NULL,
    refresh_token_expiry_date DATETIME NULL,
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

-- ==============================================================================
-- PHẦN 2: THÔNG TIN CỬA HÀNG VÀ XÁC MINH DANH TÍNH (KYC)
-- ==============================================================================
CREATE TABLE SellerRegistrations (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    shop_name NVARCHAR(255) NOT NULL,
    description NVARCHAR(MAX) NULL,
    category NVARCHAR(100) NULL,
    support_email VARCHAR(255) NULL,
    support_phone VARCHAR(20) NULL,
    contract VARCHAR(255) NULL,
    signed_contract VARCHAR(255) NULL,
    status VARCHAR(20) DEFAULT 'Pending', -- Pending, Approved, Rejected
    rejection_reason NVARCHAR(MAX) NULL,
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
    branch NVARCHAR(100) NULL,
    created_at DATETIME DEFAULT GETDATE(),
    isDelete BIT DEFAULT 0,
    CONSTRAINT FK_Bank_Users FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE NO ACTION
);
GO

CREATE TABLE KYCRequests (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    active_user_id BIGINT NULL,
    full_name NVARCHAR(255) NULL,
    citizen_id VARCHAR(20) NULL,
    id_number VARCHAR(50) NULL,
    id_type VARCHAR(50) NULL,
    request_code VARCHAR(32) NULL,
    date_of_birth DATE NULL,
    front_id_image VARCHAR(255) NULL,
    back_id_image VARCHAR(255) NULL,
    selfie_image VARCHAR(255) NULL,
    status VARCHAR(20) DEFAULT 'Pending', -- Pending, Approved, Rejected
    rejection_reason NVARCHAR(MAX) NULL,
    reviewed_by BIGINT NULL,
    reviewed_at DATETIME NULL,
    version INT NOT NULL DEFAULT 0,
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
    document_type VARCHAR(50) NULL,
    file_url VARCHAR(255) NULL,
    created_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_KYCDoc_KYC FOREIGN KEY (kyc_id) REFERENCES KYCRequests(id)
);
GO

-- ==============================================================================
-- PHẦN 3: QUẢN LÝ DANH MỤC VÀ SẢN PHẨM SỐ
-- ==============================================================================
CREATE TABLE Categories (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(100) NOT NULL,
    parent_id BIGINT NULL,
    description NVARCHAR(500) NULL,
    created_at DATETIME DEFAULT GETDATE(),
    updated_at DATETIME DEFAULT GETDATE(),
    is_delete BIT DEFAULT 0, -- Legacy soft-delete flag
    isDelete BIT DEFAULT 0,  -- Standard soft-delete flag
    CONSTRAINT FK_Category_Parent FOREIGN KEY (parent_id) REFERENCES Categories(id) ON DELETE NO ACTION
);
GO

CREATE TABLE Products (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    seller_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    name NVARCHAR(500) NOT NULL,
    description NVARCHAR(MAX) NULL,
    image VARCHAR(255) NULL,
    product_image_url NVARCHAR(500) NULL,
    product_type NVARCHAR(20) NOT NULL DEFAULT 'ACCOUNT', -- ACCOUNT | KEY | GAME_CARD | SERVICE
    user_guide NVARCHAR(MAX) NULL,
    created_at DATETIME DEFAULT GETDATE(),
    is_delete BIT DEFAULT 0, -- Legacy soft-delete flag
    isDelete BIT DEFAULT 0,  -- Standard soft-delete flag
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
    status VARCHAR(20) DEFAULT 'Pending', -- Pending, Active, Inactive
    image_url NVARCHAR(500) NULL,
    created_at DATETIME DEFAULT GETDATE(),
    isDelete BIT DEFAULT 0,
    CONSTRAINT FK_Variants_Product FOREIGN KEY (product_id) REFERENCES Products(id) ON DELETE NO ACTION
);
GO

CREATE TABLE DigitalAssets (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    variant_id BIGINT NOT NULL,
    transaction_id BIGINT NULL,
    asset_type NVARCHAR(20) NOT NULL, -- ACCOUNT | KEY | GAME_CARD
    asset_data NVARCHAR(MAX) NOT NULL,
    account_username NVARCHAR(255) NULL,
    account_password NVARCHAR(500) NULL,
    key_code NVARCHAR(MAX) NULL,
    card_code NVARCHAR(MAX) NULL,
    card_pin NVARCHAR(255) NULL,
    notes NVARCHAR(MAX) NULL,
    is_used BIT NOT NULL DEFAULT 0,
    is_delete BIT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_DigitalAssets_Variant FOREIGN KEY (variant_id) REFERENCES ProductVariants(id) ON DELETE NO ACTION
);
GO

-- ==============================================================================
-- PHẦN 4: GIAO DỊCH, NẠP/RÚT VÀ VÍ ĐIỆN TỬ
-- ==============================================================================
CREATE TABLE TopupTransactions (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    amount_vnd BIGINT NOT NULL,
    sepay_code VARCHAR(255) NULL,
    status VARCHAR(20) DEFAULT 'Pending', -- Pending, Success, Failed
    balance_before BIGINT NULL,
    balance_after BIGINT NULL,
    transfer_content VARCHAR(500) NULL,
    failure_reason VARCHAR(500) NULL,
    staff_note VARCHAR(500) NULL,
    processed_by_staff_id BIGINT NULL,
    created_at DATETIME DEFAULT GETDATE(),
    isDelete BIT DEFAULT 0,
    CONSTRAINT FK_Topup_Users FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE NO ACTION,
    CONSTRAINT FK_TopupTransactions_Staff FOREIGN KEY (processed_by_staff_id) REFERENCES Users(id) ON DELETE NO ACTION
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
    status VARCHAR(20) DEFAULT 'Pending', -- Pending, Held, Completed, Refunded
    payment_method VARCHAR(255) NULL,
    quantity INT NOT NULL DEFAULT 1,
    escrow_release_date DATETIME NULL,
    created_at DATETIME DEFAULT GETDATE(),
    isDelete BIT DEFAULT 0,
    CONSTRAINT FK_Trans_Customer FOREIGN KEY (customer_id) REFERENCES Users(id) ON DELETE NO ACTION,
    CONSTRAINT FK_Trans_Seller FOREIGN KEY (seller_id) REFERENCES Users(id) ON DELETE NO ACTION,
    CONSTRAINT FK_Trans_Product FOREIGN KEY (product_id) REFERENCES Products(id) ON DELETE NO ACTION,
    CONSTRAINT FK_Trans_Variant FOREIGN KEY (variant_id) REFERENCES ProductVariants(id) ON DELETE NO ACTION
);
GO

-- Liên kết tài sản với hoá đơn
ALTER TABLE DigitalAssets
    ADD CONSTRAINT FK_DigitalAssets_Transaction
    FOREIGN KEY (transaction_id) REFERENCES Transactions(id) ON DELETE NO ACTION;
GO
CREATE INDEX IX_DigitalAssets_Available ON DigitalAssets(variant_id, is_used, is_delete, id);
GO

CREATE TABLE Withdrawals (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    seller_id BIGINT NOT NULL,
    bank_info_id BIGINT NOT NULL,
    amount_vnd BIGINT NOT NULL,
    fee_vnd BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(20) DEFAULT 'Pending', -- Pending, Completed, Rejected
    proof_file VARCHAR(255) NULL,
    rejection_reason NVARCHAR(MAX) NULL,
    reviewed_by BIGINT NULL,
    reviewed_at DATETIME2(6) NULL,
    created_at DATETIME DEFAULT GETDATE(),
    isDelete BIT DEFAULT 0,
    CONSTRAINT FK_Withdraw_Seller FOREIGN KEY (seller_id) REFERENCES Users(id) ON DELETE NO ACTION,
    CONSTRAINT FK_Withdraw_Bank FOREIGN KEY (bank_info_id) REFERENCES SellerBankInfo(id) ON DELETE NO ACTION,
    CONSTRAINT FK_Withdrawals_ReviewedBy FOREIGN KEY (reviewed_by) REFERENCES Users(id) ON DELETE NO ACTION
);
GO

CREATE TABLE WalletTransactions (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    amount_vnd BIGINT NOT NULL,
    balance_after BIGINT NOT NULL,
    transaction_type VARCHAR(50) NOT NULL, -- PAYMENT, TOPUP, WITHDRAW, REFUND, ESCROW_RELEASE
    type VARCHAR(50) NULL, -- legacy field mapping
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reference_id BIGINT NULL,
    reference_code VARCHAR(100) NULL,
    description NVARCHAR(MAX) NULL,
    isDelete BIT NOT NULL DEFAULT 0,
    created_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_WalletLog_Users FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE NO ACTION
);
GO

-- ==============================================================================
-- PHẦN 5: CHĂM SÓC KHÁCH HÀNG, TRANH CHẤP VÀ KỶ LUẬT (ESCROW)
-- ==============================================================================
CREATE TABLE Complaints (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    transaction_id BIGINT NULL,
    customer_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    description NVARCHAR(MAX) NOT NULL,
    evidence NVARCHAR(MAX) NULL,
    status VARCHAR(20) DEFAULT 'Open', -- Open, In_Progress, Resolved, Dismissed
    resolution NVARCHAR(MAX) NULL,
    preferred_solution VARCHAR(50) NULL,
    resolved_by BIGINT NULL,
    resolved_at DATETIME2(6) NULL,
    decision_type VARCHAR(50) NULL, -- REFUND_BUYER | PAY_SELLER
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
    flag_level VARCHAR(20) DEFAULT 'Warning', -- Warning, Suspended, Banned
    status VARCHAR(20) NOT NULL DEFAULT 'Effect', -- Effect, Expired
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
    chat_type VARCHAR(20) DEFAULT 'Normal', -- Normal | Dispute
    message NVARCHAR(MAX) NOT NULL,
    isRead BIT NOT NULL DEFAULT 0,
    created_at DATETIME DEFAULT GETDATE(),
    isDelete BIT DEFAULT 0,
    sender_deleted BIT DEFAULT 0,
    receiver_deleted BIT DEFAULT 0,
    CONSTRAINT FK_Chats_Sender FOREIGN KEY (sender_id) REFERENCES Users(id) ON DELETE NO ACTION,
    CONSTRAINT FK_Chats_Receiver FOREIGN KEY (receiver_id) REFERENCES Users(id) ON DELETE NO ACTION,
    CONSTRAINT FK_Chats_Complaint FOREIGN KEY (complaint_id) REFERENCES Complaints(id) ON DELETE NO ACTION,
    CONSTRAINT FK_Chats_Product FOREIGN KEY (product_id) REFERENCES Products(id) ON DELETE NO ACTION
);
GO

-- ==============================================================================
-- PHẦN 6: TÍNH NĂNG ĐẶT TRƯỚC, ĐÁNH GIÁ VÀ THEO DÕI SHOP
-- ==============================================================================
CREATE TABLE PreOrders (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    variant_id BIGINT NULL,
    expected_price_vnd BIGINT NOT NULL,
    quantity INT DEFAULT 1,
    status VARCHAR(20) DEFAULT 'Pending', -- Pending, Approved, Cancelled, Completed
    notes NVARCHAR(MAX) NULL,
    deliveryData NVARCHAR(MAX) NULL,
    created_at DATETIME DEFAULT GETDATE(),
    isDelete BIT DEFAULT 0,
    CONSTRAINT FK_PreOrder_Customer FOREIGN KEY (customer_id) REFERENCES Users(id) ON DELETE NO ACTION,
    CONSTRAINT FK_PreOrder_Product FOREIGN KEY (product_id) REFERENCES Products(id) ON DELETE NO ACTION,
    CONSTRAINT FK_PreOrder_Variant FOREIGN KEY (variant_id) REFERENCES ProductVariants(id) ON DELETE NO ACTION
);
GO
CREATE INDEX IX_PreOrders_VariantQueue ON PreOrders(variant_id, status, isDelete, created_at, id) INCLUDE (quantity, customer_id, product_id);
GO

CREATE TABLE Reviews (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    product_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    transaction_id BIGINT NULL,
    rating INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment NVARCHAR(MAX) NULL,
    media_url NVARCHAR(MAX) NULL,
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

-- ==============================================================================
-- PHẦN 7: CẤU HÌNH HỆ THỐNG VÀ THÔNG BÁO / NHẬT KÝ KIỂM TOÁN
-- ==============================================================================
CREATE TABLE SystemConfigurations (
    id INT IDENTITY(1,1) PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL UNIQUE,
    config_value NVARCHAR(MAX) NOT NULL,
    description NVARCHAR(500) NULL,
    updated_by BIGINT NULL,
    updated_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_Config_Admin FOREIGN KEY (updated_by) REFERENCES Users(id) ON DELETE NO ACTION
);
GO

CREATE TABLE Notifications (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title NVARCHAR(255) NOT NULL,
    content NVARCHAR(MAX) NULL,
    type VARCHAR(50) NOT NULL DEFAULT 'info', -- info, success, warning, danger
    isRead BIT NOT NULL DEFAULT 0,
    severity VARCHAR(50) NOT NULL DEFAULT 'INFO',
    target_url VARCHAR(500) NULL,
    status NVARCHAR(20) NOT NULL DEFAULT N'PUBLISHED',
    created_at DATETIME DEFAULT GETDATE(),
    isDelete BIT DEFAULT 0,
    CONSTRAINT FK_Notif_Users FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE NO ACTION
);
GO

CREATE TABLE AuditLogs (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    target_user_id BIGINT NULL,
    target_id BIGINT NULL,
    target_type VARCHAR(100) NULL,
    action VARCHAR(255) NOT NULL,
    details NVARCHAR(MAX) NULL,
    created_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_Audit_Users FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE NO ACTION
);
GO

-- ==============================================================================
-- PHẦN BỔ SUNG: PHÂN QUYỀN RBAC (PERMISSIONS) VÀ TICKET HỖ TRỢ
-- ==============================================================================
CREATE TABLE Permissions (
    id INT IDENTITY(1,1) PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    group_name NVARCHAR(100) NOT NULL,
    description NVARCHAR(500) NULL,
    created_at DATETIME2(6) NOT NULL DEFAULT SYSDATETIME()
);
GO

CREATE TABLE UserPermissions (
    user_id BIGINT NOT NULL,
    permission_id INT NOT NULL,
    CONSTRAINT PK_UserPermissions PRIMARY KEY (user_id, permission_id),
    CONSTRAINT FK_UserPermissions_User FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE,
    CONSTRAINT FK_UserPermissions_Permission FOREIGN KEY (permission_id) REFERENCES Permissions(id) ON DELETE CASCADE
);
GO

CREATE TABLE SupportTickets (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    category NVARCHAR(100) NOT NULL,
    title NVARCHAR(255) NOT NULL,
    description NVARCHAR(MAX) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'Open', -- Open, In_Progress, Resolved, Closed
    resolution NVARCHAR(MAX) NULL,
    created_at DATETIME2(6) NOT NULL DEFAULT SYSDATETIME(),
    isDelete BIT NOT NULL DEFAULT 0,
    CONSTRAINT FK_SupportTickets_User FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE NO ACTION
);
GO

CREATE TABLE ChatMessages (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    sender_id BIGINT NOT NULL,
    recipient_id BIGINT NOT NULL,
    content NVARCHAR(1000) NOT NULL,
    attachment_url VARCHAR(500) NULL,
    is_read BIT NOT NULL DEFAULT 0,
    created_at DATETIME2(6) NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_ChatMessages_Sender FOREIGN KEY (sender_id) REFERENCES Users(id) ON DELETE NO ACTION,
    CONSTRAINT FK_ChatMessages_Recipient FOREIGN KEY (recipient_id) REFERENCES Users(id) ON DELETE NO ACTION
);
GO

-- ==============================================================================
-- PHẦN 8: TRIGGERS ĐẢM BẢO LUẬT NGHIỆP VỤ (BUSINESS INVARIANTS)
-- ==============================================================================

-- Trigger 1: Bắt buộc rút tối thiểu 50,000 VNĐ
CREATE OR ALTER TRIGGER trg_CheckWithdrawalMin
ON Withdrawals
AFTER INSERT
AS
BEGIN
    IF EXISTS (SELECT 1 FROM inserted WHERE amount_vnd < 50000)
    BEGIN
        RAISERROR (N'Lỗi nghiệp vụ: Số tiền yêu cầu rút tối thiểu phải là 50,000 VNĐ.', 16, 1);
        ROLLBACK TRANSACTION;
    END
END;
GO

-- Trigger 2: Tự động giam tiền Escrow 72 giờ mặc định khi có giao dịch
CREATE OR ALTER TRIGGER trg_HoldFundsEscrow
ON Transactions
AFTER INSERT
AS
BEGIN
    UPDATE Transactions
    SET status = 'Held',
        escrow_release_date = DATEADD(HOUR, 72, GETDATE())
    FROM Transactions t
    INNER JOIN inserted i ON t.id = i.id;
END;
GO

-- Trigger 3: Cập nhật minh chứng rút tiền khi hoàn thành
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

-- Trigger 4: Đồng bộ trạng thái cửa hàng lên người dùng khi duyệt đơn đăng ký shop
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

-- ==============================================================================
-- PHẦN 9: NẠP DỮ LIỆU SEED (DATA SEEDING)
-- ==============================================================================

-- 9.1. System Configurations
INSERT INTO SystemConfigurations (config_key, config_value, description)
VALUES
('DEFAULT_COMMISSION_PERCENT', '5.0', N'Phần trăm hoa hồng mặc định sàn thu của Seller'),
('MIN_WITHDRAWAL_VND', '50000', N'Số tiền rút tối thiểu cho mỗi lần thực hiện'),
('FLAT_BUYER_FEE_VND', '0', N'Phí cố định áp dụng cho mỗi đơn hàng người mua'),
('ESCROW_HOLD_HOURS', '72', N'Thời gian mặc định đóng băng tiền đơn hàng (72 giờ)'),
('MAINTENANCE_MODE', 'FALSE', N'Trạng thái bảo trì hệ thống (TRUE/FALSE)');
GO

-- 9.2. Permissions
INSERT INTO Permissions (name, group_name, description) VALUES
('MANAGE_USERS', N'Người dùng', N'Quản lý thông tin và trạng thái người dùng'),
('MANAGE_PRODUCTS', N'Sản phẩm', N'Kiểm duyệt và quản lý sản phẩm, biến thể'),
('MANAGE_CATEGORIES', N'Danh mục', N'Quản lý danh mục sản phẩm'),
('APPROVE_KYC', N'KYC', N'Xem và duyệt các yêu cầu xác minh danh tính'),
('APPROVE_WITHDRAWALS', N'Tài chính', N'Xem và duyệt các yêu cầu rút tiền'),
('RESOLVE_COMPLAINTS', N'Tranh chấp', N'Giải quyết các khiếu nại và tranh chấp đơn hàng'),
('MANAGE_SYSTEM_CONFIG', N'Hệ thống', N'Quản lý các cấu hình tham số hệ thống'),
('FLAG_SELLER', N'Kỷ luật', N'Cảnh cáo shop hoặc gắn cờ vi phạm seller');
GO

-- 9.3. Categories
SET IDENTITY_INSERT Categories ON;
INSERT INTO Categories (id, name, parent_id, description, is_delete, isDelete)
VALUES
(1, N'Email', NULL, N'Các dịch vụ email và tài khoản mail', 0, 0),
(2, N'Tài khoản', NULL, N'Tài khoản các nền tảng xã hội & dịch vụ', 0, 0),
(3, N'Phần mềm', NULL, N'Các công cụ phần mềm chuyên dụng cho kinh doanh online', 0, 0),
(4, N'Tăng tương tác', NULL, N'Các dịch vụ tăng engagement & tương tác trên mạng xã hội', 0, 0),
(5, N'Dịch vụ phần mềm', NULL, N'Công cụ, plugin và dịch vụ lập trình', 0, 0),
(6, N'Blockchain', NULL, N'Các sản phẩm tiền ảo, NFT và blockchain', 0, 0),
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
(44, N'Dịch vụ Youtube', 4, N'Tăng view, subscriber Youtube', 0, 0),
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

-- 9.4. Users Seed Data (Mật khẩu mặc định: '123' mã hoá BCrypt)
DECLARE @PasswordHash VARCHAR(255) = '$2y$10$uRFrFYP.Ld32A.LSRCm10.LlUfbJH7vgelUd4h1dsvZCyMKz1c0Bq';
SET IDENTITY_INSERT Users ON;
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
(12, 'bannerdesign@mmo.com', @PasswordHash, N'BannerDesign Store', NULL, NULL, NULL, NULL, '{"role": "Seller"}', '0903456789', 'Approved', 0, 1, 0, 0),
(13, 'customer01@gmail.com', @PasswordHash, N'Nguyễn Văn Khách', N'Nam', N'123 Đường Nguyễn Trãi, Hà Nội', '001096001234', '1996-05-15', '{"role": "Customer"}', '0987654321', 'Pending', 50000000, 1, 0, 0),
(14, 'staff01@gmail.com', @PasswordHash, N'Trần Thị Nhân Viên', N'Nữ', N'456 Cầu Giấy, Hà Nội', '001098005678', '1998-08-20', '{"role": "Staff"}', '0912345678', 'Approved', 0, 1, 0, 0),
(15, 'admin01@gmail.com', @PasswordHash, N'Admin MMO System', N'Nam', N'Hệ thống MMO Market', '001090009999', '1990-01-01', '{"role": "Admin"}', '0900000000', 'Approved', 0, 1, 0, 0),
(16, 'admin@mmo.com', '$2a$10$NcmOXXGkICk.davDnIvgbuUcscMw31mHDhb5oei/4hHOaWZRzE.g6', N'Administrator', N'Nam', N'Hệ thống MMO Market', '001090000000', '1990-01-01', '{"role": "Admin"}', '0123456789', 'Approved', 0, 1, 0, 0);
SET IDENTITY_INSERT Users OFF;
GO

-- 9.5. Seller Bank Info
INSERT INTO SellerBankInfo (user_id, bank_name, account_number, branch, created_at, isDelete)
VALUES 
(1, N'Vietcombank', '0123456789', N'Chi nhánh Hà Nội', GETDATE(), 0),
(2, N'Techcombank', '9876543210', N'Chi nhánh Cầu Giấy', GETDATE(), 0);
GO

-- 9.6. Products
SET IDENTITY_INSERT Products ON;
INSERT INTO Products (id, seller_id, category_id, name, description, image, product_image_url, product_type, isDelete, is_delete)
VALUES 
(1, 1, 27, N'Tài khoản Netflix Premium 4K UHD 1 Tháng (Xem riêng 1 thiết bị, bảo hành 1 đổi 1)', N'Xem phim chất lượng Ultra HD 4K trên mọi thiết bị...', 'https://via.placeholder.com/300x160/fd761a/ffffff?text=Netflix+Premium', 'https://via.placeholder.com/300x160/fd761a/ffffff?text=Netflix+Premium', 'ACCOUNT', 0, 0),
(2, 2, 27, N'Tài khoản Netflix Premium 4K UHD Gói 1 Năm (Chính chủ gia hạn ổn định)', N'Gói cước Netflix Premium 12 tháng xem ổn định...', 'https://via.placeholder.com/300x160/fd761a/ffffff?text=Netflix+1Year', 'https://via.placeholder.com/300x160/fd761a/ffffff?text=Netflix+1Year', 'ACCOUNT', 0, 0),
(3, 3, 27, N'Tài khoản ChatGPT Plus (OpenAI GPT-4o) Chính Chủ Sẵn 20$ Hạn 1 Tháng', N'Tài khoản OpenAI nâng cấp sẵn gói Plus trị giá 20$...', 'https://via.placeholder.com/300x160/fd761a/ffffff?text=ChatGPT+Plus', 'https://via.placeholder.com/300x160/fd761a/ffffff?text=ChatGPT+Plus', 'ACCOUNT', 0, 0),
(4, 4, 27, N'Spotify Premium 1 Năm Giá Siêu Rẻ (Nâng cấp Family email của bạn)', N'Nghe nhạc không quảng cáo trên Spotify...', 'https://via.placeholder.com/300x160/fd761a/ffffff?text=Spotify+Premium', 'https://via.placeholder.com/300x160/fd761a/ffffff?text=Spotify+Premium', 'ACCOUNT', 0, 0),
(5, 5, 26, N'Key Windows 11 Pro Bản Quyền Vĩnh Viễn (Kèm hướng dẫn active chi tiết)', N'Kích hoạt bản quyền Windows 11 Professional vĩnh viễn...', 'https://via.placeholder.com/300x160/fd761a/ffffff?text=Windows+11+Key', 'https://via.placeholder.com/300x160/fd761a/ffffff?text=Windows+11+Key', 'KEY', 0, 0),
(6, 6, 27, N'Youtube Premium Không Quảng Cáo 6 Tháng (Add Family bao chạy mượt)', N'Xem video Youtube không quảng cáo...', 'https://via.placeholder.com/300x160/fd761a/ffffff?text=Youtube+Premium', 'https://via.placeholder.com/300x160/fd761a/ffffff?text=Youtube+Premium', 'ACCOUNT', 0, 0),
(7, 7, 36, N'Gói Tài Khoản Canva Pro Thiết Kế 1 Năm Trọn Gói', N'Mở khóa toàn bộ tính năng Canva Pro...', 'https://via.placeholder.com/300x160/fd761a/ffffff?text=Canva+Pro', 'https://via.placeholder.com/300x160/fd761a/ffffff?text=Canva+Pro', 'ACCOUNT', 0, 0),
(8, 8, 7, N'Combo 10 Gmail Việt Cổ 2018 - 2020 Cực Sạch Có Sẵn Kênh Youtube', N'Tài khoản Gmail Việt Nam đăng ký từ năm 2018-2020...', 'https://via.placeholder.com/300x160/fd761a/ffffff?text=Gmail+Co', 'https://via.placeholder.com/300x160/fd761a/ffffff?text=Gmail+Co', 'ACCOUNT', 0, 0),
(9, 9, 49, N'Tool Nuôi Nick Facebook Auto Like Post Share Độc Quyền', N'Phần mềm tự động tương tác nick Facebook...', 'https://via.placeholder.com/300x160/fd761a/ffffff?text=FB+Nuoi+Nick+Tool', 'https://via.placeholder.com/300x160/fd761a/ffffff?text=FB+Nuoi+Nick+Tool', 'KEY', 0, 0),
(10, 10, 27, N'Tài khoản NordVPN Premium 1 Năm Bảo Mật Mã Hóa Cao', N'Dịch vụ mạng riêng ảo (VPN) bảo mật hàng đầu thế giới...', 'https://via.placeholder.com/300x160/fd761a/ffffff?text=NordVPN+1Year', 'https://via.placeholder.com/300x160/fd761a/ffffff?text=NordVPN+1Year', 'ACCOUNT', 0, 0),
(11, 11, 38, N'Tăng 1000 Follower Thật TikTok Việt Tốc Độ Nhanh Tự Nhiên', N'Dịch vụ tăng 1000 lượt theo dõi thật...', 'https://via.placeholder.com/300x160/fd761a/ffffff?text=TikTok+Followers', 'https://via.placeholder.com/300x160/fd761a/ffffff?text=TikTok+Followers', 'ACCOUNT', 0, 0),
(12, 2, 63, N'Ví Điện Tử Trust Wallet Hạn Cổ Có Sẵn Cụm Từ Bảo Mật', N'Ví tiền điện tử Trust Wallet được tạo từ lâu...', 'https://via.placeholder.com/300x160/fd761a/ffffff?text=Trust+Wallet', 'https://via.placeholder.com/300x160/fd761a/ffffff?text=Trust+Wallet', 'KEY', 0, 0),
(13, 12, 54, N'Gói Dịch Vụ Thiết Kế Logo & Banner Chuyên Nghiệp (Không thuộc danh mục chính)', N'Thiết kế bộ nhận diện thương hiệu cơ bản...', 'https://via.placeholder.com/300x160/fd761a/ffffff?text=Logo+Design', 'https://via.placeholder.com/300x160/fd761a/ffffff?text=Logo+Design', 'KEY', 0, 0);
SET IDENTITY_INSERT Products OFF;
GO

-- 9.7. Product Variants
SET IDENTITY_INSERT ProductVariants ON;
INSERT INTO ProductVariants (id, product_id, variant_name, price_vnd, stock, status, isDelete)
VALUES 
(1, 1, N'1 Tháng (Shared)', 65000, 156, 'Active', 0),
(2, 2, N'1 Năm (Chính Chủ)', 650000, 42, 'Active', 0),
(3, 3, N'1 Tháng', 150000, 89, 'Active', 0),
(4, 4, N'1 Năm', 250000, 45, 'Active', 0),
(5, 5, N'Windows 11 Pro Key', 99000, 999, 'Active', 0),
(6, 6, N'6 Tháng', 120000, 230, 'Active', 0),
(7, 7, N'1 Năm', 180000, 120, 'Active', 0),
(8, 8, N'Combo 10 Gmail', 35000, 500, 'Active', 0),
(9, 9, N'Tool Nuôi Nick FB Vĩnh Viễn', 850000, 75, 'Active', 0),
(10, 10, N'1 Năm', 350000, 15, 'Active', 0),
(11, 11, N'TikTok 1000 Follower', 95000, 9999, 'Active', 0),
(12, 12, N'Trust Wallet Cổ', 150000, 0, 'Active', 0),
(13, 13, N'Gói Thiết Kế Banner Logo', 299000, 50, 'Active', 0);
SET IDENTITY_INSERT ProductVariants OFF;
GO

-- 9.8. Digital Assets
INSERT INTO DigitalAssets (variant_id, asset_type, asset_data, account_username, account_password, key_code, notes, is_used, is_delete)
VALUES
(1, 'ACCOUNT', N'{"username":"netflix_user_01@gmail.com","password":"NetflixSecure@2026"}', 'netflix_user_01@gmail.com', 'NetflixSecure@2026', NULL, N'Tài khoản mới 100%', 0, 0),
(1, 'ACCOUNT', N'{"username":"netflix_user_02@gmail.com","password":"NetflixPass2026!"}', 'netflix_user_02@gmail.com', 'NetflixPass2026!', NULL, N'Gói Premium 4K', 0, 0),
(2, 'ACCOUNT', N'{"username":"nfx1year_001@gmail.com","password":"VipNetflix@001"}', N'nfx1year_001@gmail.com', N'VipNetflix@001', NULL, N'Gói 1 Năm Chính Chủ', 0, 0),
(2, 'ACCOUNT', N'{"username":"nfx1year_002@gmail.com","password":"VipNetflix@002"}', N'nfx1year_002@gmail.com', N'VipNetflix@002', NULL, N'Gói 1 Năm Chính Chủ', 0, 0),
(2, 'ACCOUNT', N'{"username":"nfx1year_003@gmail.com","password":"VipNetflix@003"}', N'nfx1year_003@gmail.com', N'VipNetflix@003', NULL, N'Gói 1 Năm Chính Chủ', 0, 0),
(4, 'ACCOUNT', N'{"username":"spotify_family01@gmail.com","password":"Sp0tify@2026"}', 'spotify_family01@gmail.com', 'Sp0tify@2026', NULL, N'Gói gia đình 6 slot', 0, 0),
(5, 'KEY', N'{"key":"WIN11-PRO-A1B2-C3D4-E5F6"}', NULL, NULL, 'WIN11-PRO-A1B2-C3D4-E5F6', N'Key bản quyền OEM', 0, 0),
(9, 'KEY', N'{"key":"TOOL-FB-NUOI NICK-A1B2-C3D4"}', NULL, NULL, 'TOOL-FB-NUOI NICK-A1B2-C3D4', N'Tool Nuôi nick FB vĩnh viễn', 0, 0);
GO

-- 9.9. User Permissions Mapping (Cho Staff và Admin)
INSERT INTO UserPermissions (user_id, permission_id)
SELECT 14, id FROM Permissions WHERE name IN ('APPROVE_KYC', 'APPROVE_WITHDRAWALS', 'RESOLVE_COMPLAINTS');
GO
INSERT INTO UserPermissions (user_id, permission_id)
SELECT 15, id FROM Permissions;
GO
INSERT INTO UserPermissions (user_id, permission_id)
SELECT 16, id FROM Permissions;
GO

PRINT N'Hoàn tất thiết lập Cơ sở dữ liệu MMO Market System đồng bộ và hoàn chỉnh nhất!';
GO
