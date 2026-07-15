USE MMO_System_Schema;
GO

-- =============================================================================
-- Migration: Bổ sung các cột duyệt yêu cầu rút tiền vào Withdrawals
-- Ngày: 2026-06-18
-- Mục đích: Lưu trữ lịch sử duyệt rút tiền của Staff/Admin cùng lý do từ chối nếu có
-- =============================================================================

USE MMO_Market_Schema;
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

