-- =============================================================================
-- Migration: Bổ sung cột hồ sơ người dùng cho màn Quản trị (Thêm/Sửa/Chi tiết TK)
-- Ngày: 2026-06-04
-- Mục đích: Lưu địa chỉ, CCCD, ngày sinh, giới tính khi Admin tạo/cập nhật nhân viên
--           và hiển thị trên màn chi tiết tài khoản.
-- Cách chạy: Thực thi trên SQL Server (T-SQL) sau khi backup DB.
-- Lưu ý: Hibernate có thể tự ALTER nếu ddl-auto=update; file này dùng cho môi trường
--         chỉnh schema thủ công theo AGENTS.md (soft delete isDelete, không DELETE cứng).
-- =============================================================================

IF COL_LENGTH('Users', 'gender') IS NULL
BEGIN
    ALTER TABLE Users ADD gender NVARCHAR(20) NULL;
END
GO

IF COL_LENGTH('Users', 'address') IS NULL
BEGIN
    ALTER TABLE Users ADD address NVARCHAR(500) NULL;
END
GO

IF COL_LENGTH('Users', 'national_id') IS NULL
BEGIN
    ALTER TABLE Users ADD national_id VARCHAR(20) NULL;
END
GO

IF COL_LENGTH('Users', 'date_of_birth') IS NULL
BEGIN
    ALTER TABLE Users ADD date_of_birth DATE NULL;
END
GO

-- isLocked đã có trong entity; nếu DB cũ chưa có, chạy add_isLocked_to_users.sql trước.
