-- 1. Xóa tài khoản cũ đã bị lỗi mật khẩu
DELETE FROM Users WHERE email = 'admin@mmo.com';
GO

-- 2. Chèn lại tài khoản Admin với mã băm mật khẩu chuẩn xác tuyệt đối
INSERT INTO Users (
    email,
    password,
    full_name,
    role,
    phone,
    shop_status,
    balance_vnd,
    permissions,
    isVerified,
    created_at,
    updated_at,
    isDelete
)
VALUES (
    'admin@mmo.com',
    -- Chuỗi mã băm BCrypt thực tế được sinh từ thư viện của dự án
    '$2a$10$NcmOXXGkICk.davDnIvgbuUcscMw31mHDhb5oei/4hHOaWZRzE.g6', 
    N'Administrator',
    '{"role": "Admin"}', 
    '0123456789',
    'Approved',
    0,
    NULL,
    1,
    GETDATE(),
    GETDATE(),
    0
);
GO
