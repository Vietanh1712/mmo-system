USE MMO_System_Schema;
GO

USE MMO_System;
GO

IF COL_LENGTH('Reviews', 'media_url') IS NULL
BEGIN
    ALTER TABLE Reviews ADD media_url NVARCHAR(MAX) NULL;
END
GO

