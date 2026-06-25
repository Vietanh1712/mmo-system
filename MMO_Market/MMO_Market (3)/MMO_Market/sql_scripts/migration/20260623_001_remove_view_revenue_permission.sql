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
