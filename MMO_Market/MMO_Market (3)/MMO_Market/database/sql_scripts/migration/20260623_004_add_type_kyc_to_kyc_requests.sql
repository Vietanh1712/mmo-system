
USE MMO_System_Schema;
GO

ALTER TABLE KYCRequests
ADD type_kyc NVARCHAR(50);
GO



EXEC sp_executesql N'
UPDATE KYCRequests
SET type_kyc = ''CCCD''
WHERE id IN (1,2,3, 10);

UPDATE KYCRequests
SET type_kyc = ''Birth certificate''
WHERE id IN (4,5,6);

UPDATE KYCRequests
SET type_kyc = ''Passport''
WHERE id IN (7,8, 9);
';

