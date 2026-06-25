

CREATE TABLE KYCRequests (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,

    user_id BIGINT NOT NULL,

    full_name NVARCHAR(255) NOT NULL,

    citizen_id VARCHAR(20) NOT NULL,

    date_of_birth DATE,

    front_id_image VARCHAR(255) NOT NULL,

    back_id_image VARCHAR(255) NOT NULL,

    selfie_image VARCHAR(255) NOT NULL,

    status VARCHAR(20) DEFAULT 'Pending',

    rejection_reason NVARCHAR(MAX),

    reviewed_by BIGINT NULL,

    reviewed_at DATETIME NULL,

    created_at DATETIME DEFAULT GETDATE(),

    updated_at DATETIME DEFAULT GETDATE(),

    isDelete BIT DEFAULT 0,

    CONSTRAINT FK_KYC_User
        FOREIGN KEY(user_id)
        REFERENCES Users(id),

    CONSTRAINT FK_KYC_Staff
        FOREIGN KEY(reviewed_by)
        REFERENCES Users(id)
);
GO

CREATE TABLE KYCDocuments (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,

    kyc_id BIGINT NOT NULL,

    document_type VARCHAR(50),

    file_url VARCHAR(255),

    created_at DATETIME DEFAULT GETDATE(),

    CONSTRAINT FK_KYCDoc_KYC
    FOREIGN KEY (kyc_id)
    REFERENCES KYCRequests(id)
);