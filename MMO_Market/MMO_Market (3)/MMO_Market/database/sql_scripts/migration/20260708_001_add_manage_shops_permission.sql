-- Migration: 20260708_001_add_manage_shops_permission
-- Description: Add MANAGE_SHOPS permission to Permissions table and assign to default staff user

USE MMO_System_Schema;
GO

IF EXISTS (SELECT * FROM sys.tables WHERE name = 'Permissions')
BEGIN
    IF NOT EXISTS (SELECT 1 FROM Permissions WHERE name = 'MANAGE_SHOPS')
    BEGIN
        INSERT INTO Permissions (name, group_name, description) 
        VALUES ('MANAGE_SHOPS', N'Vận hành', N'Cho phép xem, phê duyệt yêu cầu mở gian hàng, khóa hoặc mở khóa hoạt động của các Shop.');
        PRINT 'Added MANAGE_SHOPS permission to Permissions table.';
    END
    ELSE
    BEGIN
        PRINT 'MANAGE_SHOPS permission already exists in Permissions table.';
    END
END
GO

-- Assign MANAGE_SHOPS permission to default Staff user (user_id = 14)
IF EXISTS (SELECT * FROM sys.tables WHERE name = 'UserPermissions') AND EXISTS (SELECT 1 FROM Users WHERE id = 14)
BEGIN
    IF EXISTS (SELECT 1 FROM Permissions WHERE name = 'MANAGE_SHOPS')
    BEGIN
        DECLARE @ShopsPermId INT = (SELECT id FROM Permissions WHERE name = 'MANAGE_SHOPS');
        IF NOT EXISTS (SELECT 1 FROM UserPermissions WHERE user_id = 14 AND permission_id = @ShopsPermId)
        BEGIN
            INSERT INTO UserPermissions (user_id, permission_id) VALUES (14, @ShopsPermId);
            PRINT 'Assigned MANAGE_SHOPS permission to default Staff user (id = 14).';
        END
        ELSE
        BEGIN
            PRINT 'MANAGE_SHOPS permission already assigned to default Staff user (id = 14).';
        END
    END
END
GO
