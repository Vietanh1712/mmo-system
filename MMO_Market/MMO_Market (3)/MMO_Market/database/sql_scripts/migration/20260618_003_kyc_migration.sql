USE MMO_Market_Schema;
GO
-- =============================================================================
-- MIGRATION: Chuẩn hóa KYC V2 (Strict Mode)
-- =============================================================================

BEGIN TRY
    BEGIN TRANSACTION;
    PRINT '--- BẮT ĐẦU MIGRATION KYC ---';

    -- 1. KIỂM TRA BASELINE
    IF OBJECT_ID('dbo.KYCRequests', 'U') IS NULL
        RAISERROR('LỖI BASELINE: Bảng dbo.KYCRequests không tồn tại.', 16, 1);

    IF OBJECT_ID('dbo.KYCDocuments', 'U') IS NULL
        RAISERROR('LỖI BASELINE: Bảng dbo.KYCDocuments không tồn tại.', 16, 1);

    -- Pre-check citizen_id (VARCHAR(20) NOT NULL), status (VARCHAR(20)), isDelete (BIT NULL)
    IF NOT EXISTS (
        SELECT 1 FROM sys.columns c JOIN sys.types t ON c.user_type_id = t.user_type_id
        WHERE c.object_id = OBJECT_ID('dbo.KYCRequests') AND c.name = 'citizen_id' AND t.name = 'varchar' AND c.max_length = 20 AND c.is_nullable = 0
    ) OR NOT EXISTS (
        SELECT 1 FROM sys.columns c JOIN sys.types t ON c.user_type_id = t.user_type_id
        WHERE c.object_id = OBJECT_ID('dbo.KYCRequests') AND c.name = 'status' AND t.name = 'varchar' AND c.max_length = 20
    ) OR NOT EXISTS (
        SELECT 1 FROM sys.columns c JOIN sys.types t ON c.user_type_id = t.user_type_id
        WHERE c.object_id = OBJECT_ID('dbo.KYCRequests') AND c.name = 'isDelete' AND t.name = 'bit' AND c.is_nullable = 1
    )
    BEGIN
        RAISERROR('LỖI BASELINE: Cấu hình citizen_id, status hoặc isDelete không đúng kiểu dữ liệu khởi điểm.', 16, 1);
    END

    IF EXISTS (SELECT 1 FROM sys.columns WHERE Name IN ('id_number', 'id_type', 'request_code', 'version', 'active_user_id') AND Object_ID = OBJECT_ID('dbo.KYCRequests'))
        RAISERROR('LỖI BASELINE: Tồn tại cột rác từ version khác. Abort.', 16, 1);

    -- Pre-check rỗng dữ liệu
    IF EXISTS (SELECT 1 FROM dbo.KYCDocuments) OR EXISTS (SELECT 1 FROM dbo.KYCRequests)
        RAISERROR('LỖI DỮ LIỆU: Bảng KYCRequests hoặc KYCDocuments đang có dữ liệu rác. Yêu cầu truncate DB Test trước.', 16, 1);

    -- 2. SCHEMA CHANGES
    DROP TABLE dbo.KYCDocuments;

    EXEC sp_rename 'dbo.KYCRequests.citizen_id', 'id_number', 'COLUMN';
    ALTER TABLE dbo.KYCRequests ALTER COLUMN id_number VARCHAR(50) NOT NULL;

    -- Xử lý Default Status Cũ (Drop 'Pending' cũ, thêm 'PENDING' mới)
    DECLARE @statusConstraintName NVARCHAR(200);
    SELECT @statusConstraintName = d.name 
    FROM sys.default_constraints d JOIN sys.columns c ON d.parent_object_id = c.object_id AND d.parent_column_id = c.column_id 
    WHERE c.name = 'status' AND c.object_id = OBJECT_ID('dbo.KYCRequests');

    IF @statusConstraintName IS NOT NULL
        EXEC('ALTER TABLE dbo.KYCRequests DROP CONSTRAINT ' + @statusConstraintName);
        
    ALTER TABLE dbo.KYCRequests ADD CONSTRAINT DF_KYC_Status DEFAULT 'PENDING' FOR status;

    -- Thêm id_type, request_code, version
    ALTER TABLE dbo.KYCRequests ADD id_type VARCHAR(50) NOT NULL CONSTRAINT DF_KYC_IdType DEFAULT 'CCCD';
    ALTER TABLE dbo.KYCRequests ADD request_code VARCHAR(32) NOT NULL;
    ALTER TABLE dbo.KYCRequests ADD CONSTRAINT UQ_KYC_RequestCode UNIQUE (request_code);
    ALTER TABLE dbo.KYCRequests ADD version INT NOT NULL CONSTRAINT DF_KYC_Version DEFAULT 0;

    -- Xử lý isDelete Default Cũ -> Set BIT NOT NULL
    DECLARE @isDeleteConstraintName NVARCHAR(200);
    SELECT @isDeleteConstraintName = d.name 
    FROM sys.default_constraints d JOIN sys.columns c ON d.parent_object_id = c.object_id AND d.parent_column_id = c.column_id 
    WHERE c.name = 'isDelete' AND c.object_id = OBJECT_ID('dbo.KYCRequests');

    IF @isDeleteConstraintName IS NOT NULL
        EXEC('ALTER TABLE dbo.KYCRequests DROP CONSTRAINT ' + @isDeleteConstraintName);

    ALTER TABLE dbo.KYCRequests ALTER COLUMN isDelete BIT NOT NULL;
    ALTER TABLE dbo.KYCRequests ADD CONSTRAINT DF_KYC_IsDelete DEFAULT 0 FOR isDelete;

    -- Thêm active_user_id
    ALTER TABLE dbo.KYCRequests ADD active_user_id BIGINT NULL;

    -- 3. CONSTRAINTS & INDEXES
    EXEC('CREATE UNIQUE INDEX UQ_KYC_Active_Per_User ON dbo.KYCRequests(active_user_id) WHERE active_user_id IS NOT NULL;');

    EXEC('ALTER TABLE dbo.KYCRequests ADD CONSTRAINT CHK_KYC_Status CHECK (status IN (''PENDING'', ''APPROVED'', ''REJECTED''));');
    EXEC('ALTER TABLE dbo.KYCRequests ADD CONSTRAINT CHK_KYC_IdType CHECK (id_type IN (''CCCD'', ''CMND'', ''PASSPORT'', ''DRIVER_LICENSE''));');

    EXEC('ALTER TABLE dbo.KYCRequests ADD CONSTRAINT CHK_KYC_ActiveState CHECK (
        (isDelete = 1 AND active_user_id IS NULL) OR
        (isDelete = 0 AND status IN (''PENDING'', ''APPROVED'') AND active_user_id = user_id) OR
        (isDelete = 0 AND status = ''REJECTED'' AND active_user_id IS NULL)
    );');

    EXEC('ALTER TABLE dbo.KYCRequests ADD CONSTRAINT CHK_KYC_ReviewState CHECK (
        (status = ''PENDING'' AND reviewed_by IS NULL AND reviewed_at IS NULL AND rejection_reason IS NULL) OR
        (status = ''APPROVED'' AND reviewed_by IS NOT NULL AND reviewed_at IS NOT NULL AND rejection_reason IS NULL) OR
        (status = ''REJECTED'' AND reviewed_by IS NOT NULL AND reviewed_at IS NOT NULL AND rejection_reason IS NOT NULL AND LTRIM(RTRIM(rejection_reason)) != '''')
    );');

    EXEC('CREATE INDEX IDX_KYC_User_Created ON dbo.KYCRequests(user_id, created_at DESC);');
    EXEC('CREATE INDEX IDX_KYC_Status_Created ON dbo.KYCRequests(status, created_at DESC);');
    EXEC('CREATE INDEX IDX_KYC_Reviewer_Date ON dbo.KYCRequests(reviewed_by, reviewed_at DESC);');

    PRINT '--- MIGRATION HOÀN TẤT THÀNH CÔNG ---';
    COMMIT TRANSACTION;

END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    DECLARE @E NVARCHAR(4000) = ERROR_MESSAGE();
    RAISERROR(@E, 16, 1);
END CATCH;
GO


USE MMO_Market_Schema;
GO
-- 1. Check Constraint
SELECT name, definition FROM sys.check_constraints WHERE parent_object_id = OBJECT_ID('dbo.KYCRequests');

-- 2. Check Index definition
SELECT i.name, i.filter_definition, c.name as column_name 
FROM sys.indexes i JOIN sys.index_columns ic ON i.object_id = ic.object_id AND i.index_id = ic.index_id
JOIN sys.columns c ON ic.object_id = c.object_id AND ic.column_id = c.column_id
WHERE i.object_id = OBJECT_ID('dbo.KYCRequests');

-- 3. Check nullability/default
SELECT c.name, t.name as type, c.max_length, c.is_nullable, d.definition as default_val
FROM sys.columns c JOIN sys.types t ON c.user_type_id = t.user_type_id
LEFT JOIN sys.default_constraints d ON c.default_object_id = d.object_id
WHERE c.object_id = OBJECT_ID('dbo.KYCRequests') AND c.name IN ('status', 'isDelete', 'id_number', 'request_code');
