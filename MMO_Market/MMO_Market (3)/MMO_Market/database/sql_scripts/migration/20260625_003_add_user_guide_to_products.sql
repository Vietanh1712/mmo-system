-- Migration: Thêm cột user_guide vào bảng Products
-- Tạo bởi Antigravity AI
-- Ngày: 2026-06-25

IF NOT EXISTS (
    SELECT * FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_NAME = 'Products' AND COLUMN_NAME = 'user_guide'
)
BEGIN
    ALTER TABLE Products ADD user_guide NVARCHAR(MAX) NULL;
    PRINT 'Da them cot user_guide vao bang Products thanh cong.';
END
ELSE
BEGIN
    PRINT 'Cot user_guide da ton tai trong bang Products.';
END
GO
