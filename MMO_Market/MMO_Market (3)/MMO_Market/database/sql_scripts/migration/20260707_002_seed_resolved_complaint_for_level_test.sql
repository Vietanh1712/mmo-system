-- Migration: 20260707_002_seed_resolved_complaint_for_level_test
-- Description: Seed 1 transaction and 1 resolved complaint for seller 1 (netflixvn@mmo.com) to test Level 0 warnings

IF NOT EXISTS (SELECT 1 FROM Transactions WHERE customer_id = 13 AND seller_id = 1 AND product_id = 1 AND variant_id = 1 AND amount_vnd = 100000)
BEGIN
    INSERT INTO Transactions (customer_id, seller_id, product_id, variant_id, amount_vnd, commission_vnd, status, isDelete, created_at)
    VALUES (13, 1, 1, 1, 100000, 5000, 'Completed', 0, GETDATE());

    INSERT INTO Complaints (transaction_id, customer_id, seller_id, description, evidence, status, isDelete, created_at)
    VALUES (@@IDENTITY, 13, 1, N'Lỗi tài khoản: Không đăng nhập được', N'img_test.png', 'Resolved', 0, GETDATE());
    
    PRINT 'Seeded test transaction and complaint for Level 0 test successfully.';
END
ELSE
BEGIN
    PRINT 'Test transaction and complaint already seeded.';
END
GO
