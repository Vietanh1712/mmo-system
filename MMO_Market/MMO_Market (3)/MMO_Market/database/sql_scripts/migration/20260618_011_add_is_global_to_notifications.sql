-- =============================================================================
-- Migration: Cập nhật bảng Notifications để hỗ trợ thông báo toàn hệ thống (Broadcast)
-- Ngày: 2026-06-18
-- Mục đích: Sửa user_id thành Nullable và bổ sung cột is_global
-- =============================================================================

USE MMO_Market_Schema;
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
