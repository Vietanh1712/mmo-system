-- =============================================================================
-- Migration: Đồng bộ cấu hình hệ thống và cập nhật triggers động
-- Ngày: 2026-06-20
-- Mục đích: Đảm bảo toàn bộ các tham số cấu hình hệ thống được khởi tạo mặc định
--          và cập nhật các triggers để truy vấn giá trị động thay vì hardcode.
-- =============================================================================

USE MMO_System_Schema;
GO

-- 1. Bổ sung các cấu hình hệ thống mặc định nếu chưa tồn tại
IF EXISTS (SELECT * FROM sys.tables WHERE name = 'SystemConfigurations')
BEGIN
    -- Cấu hình chung (General Configurations)
    IF NOT EXISTS (SELECT 1 FROM SystemConfigurations WHERE config_key = 'SESSION_TIMEOUT_MINS')
        INSERT INTO SystemConfigurations (config_key, config_value, description)
        VALUES ('SESSION_TIMEOUT_MINS', '15', N'Thời gian phiên đăng nhập (phút)');

    IF NOT EXISTS (SELECT 1 FROM SystemConfigurations WHERE config_key = 'OTP_TIMEOUT_MINS')
        INSERT INTO SystemConfigurations (config_key, config_value, description)
        VALUES ('OTP_TIMEOUT_MINS', '5', N'Thời gian hiệu lực của mã OTP (phút)');

    IF NOT EXISTS (SELECT 1 FROM SystemConfigurations WHERE config_key = 'MAX_LOGIN_RETRIES')
        INSERT INTO SystemConfigurations (config_key, config_value, description)
        VALUES ('MAX_LOGIN_RETRIES', '5', N'Số lần đăng nhập sai tối đa');

    IF NOT EXISTS (SELECT 1 FROM SystemConfigurations WHERE config_key = 'LOCK_DURATION_MINS')
        INSERT INTO SystemConfigurations (config_key, config_value, description)
        VALUES ('LOCK_DURATION_MINS', '15', N'Thời gian khóa tài khoản tạm thời (phút)');

    IF NOT EXISTS (SELECT 1 FROM SystemConfigurations WHERE config_key = 'ESCROW_HOLD_HOURS')
        INSERT INTO SystemConfigurations (config_key, config_value, description)
        VALUES ('ESCROW_HOLD_HOURS', '72', N'Thời gian đóng băng tiền giao dịch bảo trợ Escrow (giờ)');

    IF NOT EXISTS (SELECT 1 FROM SystemConfigurations WHERE config_key = 'ALLOW_GOOGLE_LOGIN')
        INSERT INTO SystemConfigurations (config_key, config_value, description)
        VALUES ('ALLOW_GOOGLE_LOGIN', 'true', N'Cho phép đăng nhập bằng tài khoản Google (true/false)');

    IF NOT EXISTS (SELECT 1 FROM SystemConfigurations WHERE config_key = 'ALLOW_REGISTER')
        INSERT INTO SystemConfigurations (config_key, config_value, description)
        VALUES ('ALLOW_REGISTER', 'true', N'Cho phép đăng ký tài khoản mới (true/false)');

    IF NOT EXISTS (SELECT 1 FROM SystemConfigurations WHERE config_key = 'REQUIRE_WITHDRAW_2FA')
        INSERT INTO SystemConfigurations (config_key, config_value, description)
        VALUES ('REQUIRE_WITHDRAW_2FA', 'true', N'Bắt buộc xác thực OTP (2FA) khi rút tiền (true/false)');

    -- Cấu hình phí & hoa hồng (Commissions & Fees)
    IF NOT EXISTS (SELECT 1 FROM SystemConfigurations WHERE config_key = 'WITHDRAWAL_FEE_PERCENT')
        INSERT INTO SystemConfigurations (config_key, config_value, description)
        VALUES ('WITHDRAWAL_FEE_PERCENT', '1.5', N'Phần trăm phí rút tiền (%)');

    IF NOT EXISTS (SELECT 1 FROM SystemConfigurations WHERE config_key = 'MIN_WITHDRAW_FEE_VND')
        INSERT INTO SystemConfigurations (config_key, config_value, description)
        VALUES ('MIN_WITHDRAW_FEE_VND', '10000', N'Phí rút tiền tối thiểu (VNĐ)');

    IF NOT EXISTS (SELECT 1 FROM SystemConfigurations WHERE config_key = 'MAX_WITHDRAWAL_VND')
        INSERT INTO SystemConfigurations (config_key, config_value, description)
        VALUES ('MAX_WITHDRAWAL_VND', '50000000', N'Số tiền rút tối đa trong một giao dịch (VNĐ)');

    IF NOT EXISTS (SELECT 1 FROM SystemConfigurations WHERE config_key = 'MIN_DEPOSIT_LIMIT_VND')
        INSERT INTO SystemConfigurations (config_key, config_value, description)
        VALUES ('MIN_DEPOSIT_LIMIT_VND', '10000', N'Số tiền nạp tối thiểu (VNĐ)');

    IF NOT EXISTS (SELECT 1 FROM SystemConfigurations WHERE config_key = 'MAX_DEPOSIT_LIMIT_VND')
        INSERT INTO SystemConfigurations (config_key, config_value, description)
        VALUES ('MAX_DEPOSIT_LIMIT_VND', '50000000', N'Số tiền nạp tối đa trong một giao dịch (VNĐ)');

    IF NOT EXISTS (SELECT 1 FROM SystemConfigurations WHERE config_key = 'SELLER_UPGRADE_FEE_VND')
        INSERT INTO SystemConfigurations (config_key, config_value, description)
        VALUES ('SELLER_UPGRADE_FEE_VND', '50000', N'Phí nâng cấp tài khoản bán hàng Seller (VNĐ)');

    IF NOT EXISTS (SELECT 1 FROM SystemConfigurations WHERE config_key = 'PRODUCT_FEATURED_FEE_VND')
        INSERT INTO SystemConfigurations (config_key, config_value, description)
        VALUES ('PRODUCT_FEATURED_FEE_VND', '10000', N'Phí đẩy tin nổi bật sản phẩm (VNĐ)');

    IF NOT EXISTS (SELECT 1 FROM SystemConfigurations WHERE config_key = 'FLAT_BUYER_FEE_VND')
        INSERT INTO SystemConfigurations (config_key, config_value, description)
        VALUES ('FLAT_BUYER_FEE_VND', '1000', N'Phí cố định thu từ người mua trên mỗi đơn hàng (VNĐ)');

    PRINT 'Đã bổ sung cấu hình hệ thống mặc định đầy đủ.';
END
GO

-- 2. Cập nhật trigger trg_CheckWithdrawalMin kiểm tra số tiền rút tối thiểu động từ cấu hình
CREATE OR ALTER TRIGGER trg_CheckWithdrawalMin
ON Withdrawals
AFTER INSERT
AS
BEGIN
    DECLARE @MinWithdrawal BIGINT = 50000;
    SELECT @MinWithdrawal = TRY_CAST(config_value AS BIGINT) 
    FROM SystemConfigurations 
    WHERE config_key = 'MIN_WITHDRAWAL_VND';
    
    IF @MinWithdrawal IS NULL SET @MinWithdrawal = 50000;

    IF EXISTS (SELECT 1 FROM inserted WHERE amount_vnd < @MinWithdrawal)
    BEGIN
        DECLARE @ErrMsg NVARCHAR(255) = N'Lỗi: Số tiền rút tối thiểu phải là ' + FORMAT(@MinWithdrawal, 'N0') + N' VNĐ theo chính sách sàn.';
        RAISERROR (@ErrMsg, 16, 1);
        ROLLBACK TRANSACTION;
    END
END;
GO
PRINT 'Đã cập nhật trigger trg_CheckWithdrawalMin động.';
GO

-- 3. Cập nhật trigger trg_HoldFundsEscrow thiết lập trạng thái Giữ Tiền và thời gian giam tiền Escrow động từ cấu hình
CREATE OR ALTER TRIGGER trg_HoldFundsEscrow
ON Transactions
AFTER INSERT
AS
BEGIN
    DECLARE @EscrowHoldHours INT = 72;
    SELECT @EscrowHoldHours = TRY_CAST(config_value AS INT) 
    FROM SystemConfigurations 
    WHERE config_key = 'ESCROW_HOLD_HOURS';
    
    IF @EscrowHoldHours IS NULL SET @EscrowHoldHours = 72;

    UPDATE Transactions
    SET status = 'Held'
    FROM Transactions t
    INNER JOIN inserted i ON t.id = i.id
    WHERE t.status IS NULL OR t.status = 'Pending';

    UPDATE Transactions
    SET escrow_release_date = DATEADD(HOUR, @EscrowHoldHours, GETDATE())
    FROM Transactions t
    INNER JOIN inserted i ON t.id = i.id
    WHERE t.escrow_release_date IS NULL;
END;
GO
PRINT 'Đã cập nhật trigger trg_HoldFundsEscrow động.';
GO
