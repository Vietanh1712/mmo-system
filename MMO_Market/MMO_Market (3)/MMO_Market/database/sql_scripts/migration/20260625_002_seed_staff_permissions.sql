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

    PRINT 'Đã gán toàn bộ 5 permissions cho Staff (id = 14)';
END
GO
