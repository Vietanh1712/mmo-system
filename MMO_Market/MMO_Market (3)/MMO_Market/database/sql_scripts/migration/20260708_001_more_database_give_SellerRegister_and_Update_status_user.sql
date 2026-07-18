

IF NOT EXISTS (
    SELECT *
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_NAME = 'Users'
      AND COLUMN_NAME = 'deposit_vnd'
)
BEGIN
    ALTER TABLE Users
    ADD deposit_vnd BIGINT NOT NULL DEFAULT 0;

    PRINT 'Đã thêm cột deposit_vnd vào bảng Users.';
END
ELSE
BEGIN
    PRINT 'Cột deposit_vnd đã tồn tại.';
END
GO

UPDATE Users
SET deposit_vnd = 500000
WHERE id BETWEEN 1 AND 16;
GO

INSERT INTO SellerRegistrations
(user_id, shop_name, description, contract, signed_contract, status, created_at, isDelete)
VALUES
(1, N'NetflixVN Store', N'Cửa hàng Netflix', 'contract1.pdf', 'signed1.pdf', 'Approved', GETDATE(), 0),
(2, N'Netflix_Vip_Store', N'Cửa hàng Netflix VIP', 'contract2.pdf', 'signed2.pdf', 'Approved', GETDATE(), 0),
(3, N'AI_Helper Store', N'Cửa hàng AI', 'contract3.pdf', 'signed3.pdf', 'Approved', GETDATE(), 0),
(4, N'MusicLovers Store', N'Cửa hàng nhạc', 'contract4.pdf', 'signed4.pdf', 'Approved', GETDATE(), 0),
(5, N'Microsoft_Reseller Store', N'Cửa hàng Microsoft', 'contract5.pdf', 'signed5.pdf', 'Approved', GETDATE(), 0),
(6, N'RedPremium Store', N'Cửa hàng Premium', 'contract6.pdf', 'signed6.pdf', 'Approved', GETDATE(), 0),
(7, N'CanvaPro Store', N'Cửa hàng Canva', 'contract7.pdf', 'signed7.pdf', 'Approved', GETDATE(), 0),
(8, N'GmailPro Store', N'Cửa hàng Gmail', 'contract8.pdf', 'signed8.pdf', 'Approved', GETDATE(), 0),

(9, N'MMO_Coder Store', N'Cửa hàng MMO', 'contract9.pdf', 'signed9.pdf', 'Pending', GETDATE(), 0),
(10, N'SecureNet Store', N'Cửa hàng VPN', 'contract10.pdf', 'signed10.pdf', 'Pending', GETDATE(), 0),
(11, N'SocialMediaUp Store', N'Cửa hàng Social', 'contract11.pdf', 'signed11.pdf', 'Pending', GETDATE(), 0),
(12, N'BannerDesign Store', N'Cửa hàng Design', 'contract12.pdf', 'signed12.pdf', 'Pending', GETDATE(), 0),

(13, N'NguyenVanKhach Shop', N'Shop khách hàng', 'contract13.pdf', 'signed13.pdf', 'Rejected', GETDATE(), 0),
(14, N'Staff Demo Shop', N'Shop Staff', 'contract14.pdf', 'signed14.pdf', 'Rejected', GETDATE(), 0),
(15, N'Admin Demo Shop', N'Shop Admin', 'contract15.pdf', 'signed15.pdf', 'Rejected', GETDATE(), 0),
(16, N'Administrator Shop', N'Shop Administrator', 'contract16.pdf', 'signed16.pdf', 'Rejected', GETDATE(), 0);

select * from SellerRegistrations

select * from Users

Update Users set 
shop_status = 'Approved'
where id in (2,6,7)

Update Users set 
shop_status = 'Locked'
where id in (3,4,11,12,8,6,7,13,14,16)