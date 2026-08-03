-- Compensation script to add transaction records to WalletTransactions for all historical approved shop registrations
-- that do not have a corresponding transaction record yet.

INSERT INTO WalletTransactions (user_id, type, amount_vnd, status, description, reference_code, balance_after, transaction_type, created_at, isDelete)
SELECT 
    sr.user_id,
    'PAYMENT',
    -50000, -- Default SHOP_OPENING_FEE_VND is 50,000 VND
    'SUCCESS',
    N'Trừ phí đăng ký mở Shop: ' + sr.shop_name,
    'SHOP-REG-' + CAST(sr.user_id AS VARCHAR) + '-' + CONVERT(VARCHAR, sr.created_at, 112),
    u.balance_vnd,
    'OUT',
    sr.created_at,
    0
FROM SellerRegistrations sr
JOIN Users u ON sr.user_id = u.id
WHERE sr.status IN ('Approved', 'APPROVED')
  AND sr.user_id NOT IN (
      SELECT DISTINCT wt.user_id 
      FROM WalletTransactions wt 
      WHERE wt.description LIKE N'%shop%' OR wt.description LIKE N'%phí mở%' OR wt.type = 'SHOP_REGISTRATION'
  );
GO
