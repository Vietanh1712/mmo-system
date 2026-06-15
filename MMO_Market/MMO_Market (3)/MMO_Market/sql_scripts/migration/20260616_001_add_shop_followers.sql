-- =============================================================================
-- Migration: Tạo bảng theo dõi cửa hàng (Shop Followers)
-- Ngày: 2026-06-16
-- Mục đích: Lưu thông tin người mua theo dõi cửa hàng của người bán
-- =============================================================================

IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'ShopFollowers')
BEGIN
    CREATE TABLE ShopFollowers (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        follower_id BIGINT NOT NULL,
        seller_id BIGINT NOT NULL,
        created_at DATETIME DEFAULT GETDATE(),
        isDelete BIT DEFAULT 0,
        CONSTRAINT FK_Followers_Follower FOREIGN KEY (follower_id) REFERENCES Users(id) ON DELETE NO ACTION,
        CONSTRAINT FK_Followers_Seller FOREIGN KEY (seller_id) REFERENCES Users(id) ON DELETE NO ACTION,
        CONSTRAINT UQ_Follower_Seller UNIQUE (follower_id, seller_id)
    );

    CREATE INDEX idx_follower ON ShopFollowers(follower_id);
    CREATE INDEX idx_seller ON ShopFollowers(seller_id);
END
GO
