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
    IF NOT EXISTS (SELECT 1 FROM Permissions WHERE name = 'VIEW_REVENUE')
        INSERT INTO Permissions (name, group_name, description) VALUES ('VIEW_REVENUE', N'Báo cáo', N'Xem doanh thu, dòng tiền và cấu hình phí hệ thống');
    IF NOT EXISTS (SELECT 1 FROM Permissions WHERE name = 'MANAGE_SYSTEM_CONFIG')
        INSERT INTO Permissions (name, group_name, description) VALUES ('MANAGE_SYSTEM_CONFIG', N'Hệ thống', N'Quản lý các cấu hình tham số hệ thống');
    
    PRINT 'Đã seed 8 permissions hệ thống';
END
GO
