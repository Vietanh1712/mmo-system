ALTER TABLE Transactions
ADD payment_method NVARCHAR(50);


UPDATE Transactions
SET payment_method = 'Wallet'
WHERE id IN (1,2,3);

UPDATE Transactions
SET payment_method = 'Bank Transfer'
WHERE id IN (4,5,6);

UPDATE Transactions
SET payment_method = 'VietQR'
WHERE id IN (7,8);

UPDATE Transactions
SET payment_method = 'Credit Card'
WHERE id IN (9,10);