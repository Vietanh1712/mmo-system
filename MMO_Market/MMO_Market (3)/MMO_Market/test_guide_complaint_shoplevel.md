# 🧪 Hướng Dẫn Test: Phân Cấp Gian Hàng & Xử Lý Khiếu Nại

> **Phạm vi test**: Credentials Card redesign, Auto Shop Level, Auto-recovery Level 2, Complaint flow

---

## 📌 BƯỚC 0 — Chuẩn bị môi trường

### 0.1 Đảm bảo server đang chạy
```powershell
# Chạy trong thư mục apps/backend
& "C:\Users\pc\Downloads\apache-maven-3.9.11-bin\apache-maven-3.9.11\bin\mvn.cmd" spring-boot:run
```

### 0.2 Tài khoản sẵn có trong DB
| Email | Mật khẩu | Vai trò |
|---|---|---|
| `gmailpro@mmo.com` | `123456` | **Seller** (GmailPro Store, Level 1) |

---

## 🗃️ BƯỚC 1 — Chèn dữ liệu mock vào SQL Server

Mở **SQL Server Management Studio** hoặc **Azure Data Studio**, chạy theo thứ tự:

### 1.1 Tạo tài khoản Buyer (Người mua) mock
```sql
-- Tạo user Buyer để test mua hàng
INSERT INTO Users (
    email, password, full_name, role, phone, isVerified, isLocked, isDelete,
    balance_vnd, deposit_vnd, created_at
)
VALUES (
    'buyer_test@gmail.com',
    '$2y$10$uRFrFYP.Ld32A.LSRCm10.LlUfbJH7vgelUd4h1dsvZCyMKz1c0Bq', -- mật khẩu: 123456
    'Nguyễn Văn Mua',
    '{"role":"Buyer"}',
    '0901234567',
    1, 0, 0,
    5000000, -- 5 triệu VNĐ
    0,
    GETDATE()
);
```

### 1.2 Tạo Seller Level 2 (để test auto-recovery)
```sql
-- Seller Level 2: tuổi >= 30 ngày, đơn >= 20, dispute rate < 2%
INSERT INTO Users (
    email, password, full_name, role, phone, isVerified, isLocked, isDelete,
    balance_vnd, deposit_vnd, shop_status, shop_level,
    withdrawal_locked, created_at
)
VALUES (
    'seller_level2@mmo.com',
    '$2y$10$uRFrFYP.Ld32A.LSRCm10.LlUfbJH7vgelUd4h1dsvZCyMKz1c0Bq', -- 123456
    'Shop Uy Tín Level 2',
    '{"role":"Seller"}',
    '0987654321',
    1, 0, 0,
    -500000,   -- âm 500k (đang NỢ)
    2000000,   -- cọc 2 triệu
    'Approved',
    2,         -- Level 2
    1,         -- withdrawal_locked = true (đang bị khóa rút tiền)
    DATEADD(DAY, -35, GETDATE()) -- shop 35 ngày tuổi
);

-- Bổ sung đăng ký shop để hiển thị tên shop
DECLARE @seller2Id BIGINT = (SELECT id FROM Users WHERE email = 'seller_level2@mmo.com');
INSERT INTO SellerRegistrations (user_id, shop_name, description, status, created_at, isDelete)
VALUES (@seller2Id, 'Shop Uy Tín Pro', 'Shop Uy Tín Pro level 2', 'Approved', DATEADD(DAY, -35, GETDATE()), 0);
```

### 1.3 Tạo Staff account để test duyệt khiếu nại
```sql
INSERT INTO Users (
    email, password, full_name, role, isVerified, isLocked, isDelete,
    balance_vnd, deposit_vnd, created_at
)
VALUES (
    'staff_test@mmo.com',
    '$2y$10$uRFrFYP.Ld32A.LSRCm10.LlUfbJH7vgelUd4h1dsvZCyMKz1c0Bq', -- 123456
    'Nhân Viên Test',
    '{"role":"Staff"}',
    1, 0, 0,
    0, 0,
    GETDATE()
);
```

### 1.4 Tạo danh mục và sản phẩm + tài sản số
```sql
-- Lấy ID seller GmailPro
DECLARE @sellerId BIGINT = (SELECT TOP 1 id FROM Users WHERE email = 'gmailpro@mmo.com');
DECLARE @catId   BIGINT  = (SELECT TOP 1 id FROM Categories WHERE parent_id IS NOT NULL);

-- Tạo sản phẩm Netflix
INSERT INTO Products (seller_id, category_id, name, description, image, product_type, isDelete)
VALUES (@sellerId, @catId,
    'Tài khoản Netflix Premium Test',
    'Tài khoản Netflix 4K dùng thử để kiểm thử hệ thống.',
    'https://via.placeholder.com/300x160/e50914/ffffff?text=Netflix',
    'ACCOUNT', 0);

DECLARE @productId BIGINT = SCOPE_IDENTITY();

-- Tạo biến thể
INSERT INTO ProductVariants (product_id, variant_name, price_vnd, stock, status, isDelete, image_url)
VALUES (@productId, '1 Tháng - 1 Màn Hình', 89000, 0, 'Active', 0,
        'https://via.placeholder.com/300x160/e50914/ffffff?text=1+Month');

DECLARE @variantId BIGINT = SCOPE_IDENTITY();

-- Chèn 3 tài khoản Netflix vào kho
INSERT INTO DigitalAssets (variant_id, asset_type, account_username, account_password, asset_data, is_used, is_delete)
VALUES
    (@variantId, 'ACCOUNT', 'nguyenthingoclinh291104@gmail.com', '1234567', '{"username":"nguyenthingoclinh291104@gmail.com","password":"1234567"}', 0, 0),
    (@variantId, 'ACCOUNT', 'tranthimai2023@gmail.com',          'pass2023', '{"username":"tranthimai2023@gmail.com","password":"pass2023"}', 0, 0),
    (@variantId, 'ACCOUNT', 'lethanhbinh_vip@gmail.com',         'binhvip99', '{"username":"lethanhbinh_vip@gmail.com","password":"binhvip99"}', 0, 0);

-- Cập nhật stock = 3
UPDATE ProductVariants SET stock = 3 WHERE id = @variantId;
```

### 1.5 Tạo đơn hàng HELD sẵn (để test credentials card & khiếu nại)
```sql
DECLARE @buyerId  BIGINT = (SELECT TOP 1 id FROM Users WHERE email = 'buyer_test@gmail.com');
DECLARE @sellerId BIGINT = (SELECT TOP 1 id FROM Users WHERE email = 'gmailpro@mmo.com');
DECLARE @varId    BIGINT = (SELECT TOP 1 id FROM ProductVariants WHERE variant_name = '1 Tháng - 1 Màn Hình');
DECLARE @assetId  BIGINT = (SELECT TOP 1 id FROM DigitalAssets WHERE variant_id = @varId AND is_used = 0);

-- Tạo transaction đã thanh toán, đang HELD (có thể xem credentials)
INSERT INTO Transactions (
    customer_id, seller_id, product_id, variant_id, amount_vnd, commission_vnd,
    status, escrow_release_date, created_at, isDelete
)
SELECT 
    @buyerId, @sellerId, pv.product_id, @varId,
    89000, 4450, -- 5% commission
    'HELD',
    DATEADD(HOUR, 72, GETDATE()),
    GETDATE(),
    0
FROM ProductVariants pv WHERE pv.id = @varId;

DECLARE @transactionId BIGINT = SCOPE_IDENTITY();

-- Đánh dấu asset đã được dùng và gán vào transaction
UPDATE DigitalAssets SET is_used = 1, transaction_id = @transactionId WHERE id = @assetId;
```

---

## 🧪 TEST CASE 1 — Credentials Card mới (UI)

**Mục tiêu**: Xác nhận giao diện redesign hiển thị đúng

### Bước:
1. Đăng nhập bằng `buyer_test@gmail.com` / `123456`
2. Vào `http://localhost:8080/account/orders/MMO-ORD-TEST001`
3. **Kiểm tra visual**:

| Điểm kiểm tra | Kết quả mong đợi |
|---|---|
| Header card "Sản phẩm đã mua" | Có gradient xanh + icon 🔑 + badge "Bảo mật" |
| Label tài khoản | `TÀI KHOẢN (EMAIL/USERNAME)` — chữ hoa nhỏ |
| Giá trị email | Monospace font, nền xám nhạt |
| Nút Copy | Border, hover lift, transition mượt |
| **Click Copy tài khoản** | Nút đổi → ✅ xanh "Đã copy!" trong 2 giây, toast xuất hiện |
| **Click Copy mật khẩu** | Tương tự |
| Vùng cảnh báo | Gradient kem vàng, icon ⚠️ |

---

## 🧪 TEST CASE 2 — Trạng thái đơn hàng khác nhau

Chạy SQL để kiểm tra từng trạng thái:

```sql
-- Lấy ID giao dịch của buyer_test vừa tạo ở bước trên
DECLARE @buyerId BIGINT = (SELECT id FROM Users WHERE email = 'buyer_test@gmail.com');
DECLARE @txId BIGINT = (SELECT TOP 1 id FROM Transactions WHERE customer_id = @buyerId ORDER BY id DESC);

-- Test PENDING (chưa có credentials)
UPDATE Transactions SET status = 'PENDING' WHERE id = @txId;
-- Mong đợi: Hiện thông báo màu xanh dương "Đơn hàng đang chờ xử lý..."

-- Test DISPUTED
UPDATE Transactions SET status = 'DISPUTED' WHERE id = @txId;
-- Mong đợi: Thông báo màu cam "Thông tin nhận hàng đang được giữ..."

-- Test CANCELLED
UPDATE Transactions SET status = 'CANCELLED' WHERE id = @txId;
-- Mong đợi: Thông báo màu đỏ "Đơn hàng đã hủy..."

-- Khôi phục về HELD để test các case khác
UPDATE Transactions SET status = 'HELD' WHERE id = @txId;
```

**Mỗi lần đổi**: F5 trang và kiểm tra màu sắc + icon của `cred-status-msg`.

---

## 🧪 TEST CASE 3 — Gửi khiếu nại (Complaint Flow)

**Chuẩn bị**: Đơn hàng phải ở trạng thái `HELD` hoặc `PAID`

### Bước:
1. Đăng nhập `buyer_test@gmail.com`
2. Vào chi tiết đơn `MMO-ORD-TEST001`
3. Click **"Khiếu nại"**
4. Điền: *"Tài khoản đã bị thay mật khẩu, không đăng nhập được"*
5. Chọn phương án: **Đổi tài khoản mới**
6. Upload ảnh bằng chứng
7. Submit

**Kiểm tra DB sau submit**:
```sql
DECLARE @buyerId BIGINT = (SELECT id FROM Users WHERE email = 'buyer_test@gmail.com');

SELECT c.id, c.status, c.description, c.preferred_solution, t.status AS tx_status
FROM Complaints c
JOIN Transactions t ON t.id = c.transaction_id
WHERE t.customer_id = @buyerId;
-- Mong đợi: complaint.status = 'Open', transaction.status = 'DISPUTED'
```

---

## 🧪 TEST CASE 4 — Staff duyệt khiếu nại (Resolved)

### 4A — Seller Level 1 bị trừ tiền, shop bị khóa

```sql
-- Setup: Seller GmailPro chỉ có 50k (ít hơn tiền hoàn 89k)
UPDATE Users SET balance_vnd = 50000, deposit_vnd = 0
WHERE email = 'gmailpro@mmo.com';
```

### Bước:
1. Đăng nhập `staff_test@mmo.com` / `123456`
2. Vào trang quản lý khiếu nại → tìm khiếu nại của `MMO-ORD-TEST001`
3. Duyệt: **Resolved** (Seller có lỗi, hoàn tiền cho buyer)

**Kiểm tra DB sau duyệt**:
```sql
SELECT
    u.email,
    u.balance_vnd,
    u.deposit_vnd,
    u.shop_status,
    u.shop_level,
    u.withdrawal_locked
FROM Users u WHERE u.email = 'gmailpro@mmo.com';
```

| Trường | Giá trị mong đợi |
|---|---|
| `balance_vnd` | ≤ 0 (đã trừ) |
| `shop_status` | `Locked` (Level 1 bị ví âm) |
| `shop_level` | Có thể là 0 nếu dispute rate ≥ 2% |

---

### 4B — Seller Level 2: Shop vẫn Active, chỉ khóa rút tiền

```sql
-- Setup Seller Level 2 có tiền 100k nhưng phải hoàn 89k
DECLARE @sel2Id BIGINT = (SELECT TOP 1 id FROM Users WHERE email = 'seller_level2@mmo.com');
DECLARE @buyerId BIGINT = (SELECT TOP 1 id FROM Users WHERE email = 'buyer_test@gmail.com');
DECLARE @varId BIGINT = (SELECT TOP 1 id FROM ProductVariants);

-- Tạo đơn hàng mới cho Seller Level 2
INSERT INTO Transactions (
    customer_id, seller_id, product_id, variant_id, amount_vnd, commission_vnd,
    status, escrow_release_date, created_at, isDelete
)
SELECT 
    @buyerId, @sel2Id, pv.product_id, pv.id,
    89000, 4450, 'HELD',
    DATEADD(HOUR, 72, GETDATE()), GETDATE(), 0
FROM ProductVariants pv WHERE pv.id = @varId;

DECLARE @lvl2TxId BIGINT = SCOPE_IDENTITY();

-- Tạo khiếu nại cho đơn này
INSERT INTO Complaints (transaction_id, customer_id, seller_id, status, description, preferred_solution, created_at, isDelete)
VALUES (
    @lvl2TxId, @buyerId, @sel2Id, 'Open',
    'Test Level 2 complaint', 'REFUND', GETDATE(), 0
);
```

1. Staff duyệt khiếu nại `MMO-ORD-LVL2-001` → **Resolved**
2. Kiểm tra:

```sql
SELECT email, balance_vnd, shop_status, shop_level, withdrawal_locked
FROM Users WHERE email = 'seller_level2@mmo.com';
-- Mong đợi:
-- shop_status = 'Approved' (KHÔNG bị khóa - Level 2)
-- withdrawal_locked = 1 (khóa rút tiền nếu ví âm)
-- balance_vnd < 0 (đang nợ)
```

---

## 🧪 TEST CASE 5 — Auto-recovery: Seller Level 2 tự trả nợ qua escrow

**Chuẩn bị**: Seller Level 2 phải đang NỢ (`balance_vnd < 0`), `withdrawal_locked = 1`

```sql
-- Tạo đơn hàng HELD mới cho Level 2 Seller (đơn hàng để giải ngân thu nợ)
DECLARE @sel2Id BIGINT = (SELECT TOP 1 id FROM Users WHERE email = 'seller_level2@mmo.com');
DECLARE @buyerId BIGINT = (SELECT TOP 1 id FROM Users WHERE email = 'buyer_test@gmail.com');
DECLARE @varId BIGINT = (SELECT TOP 1 id FROM ProductVariants);

INSERT INTO Transactions (
    customer_id, seller_id, product_id, variant_id, amount_vnd, commission_vnd,
    status, escrow_release_date, created_at, isDelete
)
SELECT 
    @buyerId, @sel2Id, pv.product_id, pv.id,
    600000, 30000,  -- 600k giải ngân → đủ bù nợ 500k
    'HELD',
    DATEADD(HOUR, -1, GETDATE()), -- escrow đã hết hạn
    DATEADD(HOUR, -73, GETDATE()),
    0
FROM ProductVariants pv WHERE pv.id = @varId;
```

### Bước test:
1. Đăng nhập Staff → gọi API release escrow:

```bash
curl -X POST http://localhost:8080/api/transactions/{transactionId}/release-escrow \
     -H "Authorization: Bearer {staff_token}"
```

Hoặc kiểm tra qua SQL (nếu có scheduler):
```sql
-- Xem số dư trước
SELECT balance_vnd, withdrawal_locked FROM Users WHERE email = 'seller_level2@mmo.com';
```

2. Sau khi release:
```sql
SELECT balance_vnd, withdrawal_locked, shop_status
FROM Users WHERE email = 'seller_level2@mmo.com';
```

| Trường | Trước | Sau |
|---|---|---|
| `balance_vnd` | -500,000 | +70,000 (600k - 500k nợ - 30k commission) |
| `withdrawal_locked` | `1` | **`0`** (tự động mở khóa!) |

---

## 🧪 TEST CASE 6 — Credentials hiển thị đúng loại sản phẩm

### 6A — Mã Key (KEY)
```sql
-- Tạo sản phẩm key và đơn hàng để test
DECLARE @sellerId BIGINT = (SELECT TOP 1 id FROM Users WHERE email = 'gmailpro@mmo.com');
DECLARE @catId   BIGINT  = (SELECT TOP 1 id FROM categories WHERE parent_id IS NOT NULL);

INSERT INTO products (seller_id, category_id, name, description, image, product_type, is_delete)
VALUES (@sellerId, @catId, 'Windows 11 Pro Key', 'Key bản quyền Win 11', 'https://via.placeholder.com/300', 'KEY', 0);

INSERT INTO product_variants (product_id, variant_name, price_vnd, stock, status, is_delete, image_url)
VALUES (SCOPE_IDENTITY(), 'Key Full', 150000, 1, 'Active', 0, 'https://via.placeholder.com/300');

INSERT INTO digital_assets (variant_id, asset_type, key_code, is_used, is_delete)
VALUES (SCOPE_IDENTITY(), 'KEY', 'WIN11-PRO-ABCDE-FGHIJ-KLMNO', 0, 0);

UPDATE product_variants SET stock = 1 WHERE variant_name = 'Key Full';
```

Tạo đơn hàng HELD cho key này, vào trang chi tiết:
- **Mong đợi**: Label hiển thị `MÃ KÍCH HOẠT (KEY):` thay vì `TÀI KHOẢN`
- Không có dòng mật khẩu

---

## 📋 Checklist Tổng Hợp

| # | Test Case | Pass/Fail | Ghi chú |
|---|---|---|---|
| 1 | Credentials card có gradient header xanh | ☐ | |
| 2 | Nút Copy — hover effect (lift) | ☐ | |
| 3 | Click Copy → badge xanh "Đã copy!" 2 giây | ☐ | |
| 4 | Toast xuất hiện sau copy | ☐ | |
| 5 | PENDING → thông báo xanh dương | ☐ | |
| 6 | DISPUTED → thông báo cam | ☐ | |
| 7 | CANCELLED → thông báo đỏ | ☐ | |
| 8 | KEY product: không có dòng mật khẩu | ☐ | |
| 9 | Complaint submit → status = DISPUTED | ☐ | |
| 10 | Staff Resolved → Seller Level 1 shop Locked | ☐ | |
| 11 | Staff Resolved → Seller Level 2 shop vẫn Active | ☐ | |
| 12 | Auto-recovery: balance dương → withdrawalLocked = false | ☐ | |
| 13 | shopLevel tự động tính lại sau Resolved | ☐ | |
