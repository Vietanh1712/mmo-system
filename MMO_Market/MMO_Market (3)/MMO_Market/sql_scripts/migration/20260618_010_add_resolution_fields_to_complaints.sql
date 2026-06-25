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
