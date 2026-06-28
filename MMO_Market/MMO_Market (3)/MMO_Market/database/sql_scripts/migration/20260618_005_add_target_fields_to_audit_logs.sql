-- =============================================================================
-- Migration: Bổ sung các cột Target vào AuditLogs
-- Ngày: 2026-06-18
-- Mục đích: Hỗ trợ log chi tiết đối tượng chịu tác động từ hành động của Admin/Staff
-- =============================================================================

USE MMO_Market_Schema;
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
