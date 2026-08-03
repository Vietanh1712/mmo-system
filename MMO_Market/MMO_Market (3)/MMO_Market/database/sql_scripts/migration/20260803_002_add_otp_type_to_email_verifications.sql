-- Alter table EmailVerifications to add otp_type column
IF NOT EXISTS (
    SELECT 1 
    FROM sys.columns 
    WHERE object_id = OBJECT_ID('EmailVerifications') 
      AND name = 'otp_type'
)
BEGIN
    ALTER TABLE EmailVerifications ADD otp_type VARCHAR(50) NULL;
END
GO

-- Update existing records to have a default type 'REGISTRATION'
UPDATE EmailVerifications
SET otp_type = 'REGISTRATION'
WHERE otp_type IS NULL;
GO
