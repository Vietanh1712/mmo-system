-- ==============================================================================
-- QUICK REFERENCE: CATEGORIES HIERARCHICAL STRUCTURE
-- ==============================================================================

-- Danh mục Cha (6 cái)
-- ID: 1-6, parent_id = NULL

1. Email                  (8 con)
   ├─ Gmail
   ├─ HotMail
   ├─ OutlookMail
   ├─ RuMail
   ├─ DomainMail
   ├─ YahooMail
   ├─ ProtonMail
   └─ Loại Mail Khác

2. Tài khoản             (13 con)
   ├─ Tài khoản FB
   ├─ Tài Khoản BM
   ├─ Tài Khoản Zalo
   ├─ Tài Khoản Twitter
   ├─ Tài Khoản Telegram
   ├─ Tài Khoản Instagram
   ├─ Tài Khoản Shopee
   ├─ Tài Khoản Discord
   ├─ Tài Khoản TikTok
   ├─ Key Diệt Virus
   ├─ Tài Khoản Capcut
   ├─ Key Window
   └─ Tài Khoản Khác

3. Phần mềm              (9 con)
   ├─ Phần Mềm FB
   ├─ Phần Mềm Google
   ├─ Phần Mềm Youtube
   ├─ Phần Mềm Tiền Ảo
   ├─ Phần Mềm PTC
   ├─ Phần Mềm Captcha
   ├─ Phần Mềm Offer
   ├─ Phần Mềm PTU
   └─ Phần Mềm Khác

4. Tăng tương tác        (11 con)
   ├─ Dịch vụ Facebook
   ├─ Dịch vụ Tiktok
   ├─ Dịch vụ Google
   ├─ Dịch vụ Telegram
   ├─ Dịch vụ Shopee
   ├─ Dịch vụ Discord
   ├─ Dịch vụ Twitter
   ├─ Dịch vụ Youtube
   ├─ Dịch vụ Zalo
   ├─ Dịch vụ Instagram
   └─ Tương tác khác

5. Dịch vụ phần mềm      (11 con)
   ├─ Tool MMO
   ├─ Tool Facebook
   ├─ Tool Google
   ├─ Tool Youtube
   ├─ Tool TikTok
   ├─ Tool Instagram
   ├─ Đồ họa - Design
   ├─ Video Editor
   ├─ Plugin & Extension
   ├─ Script & Bot
   └─ Phần mềm khác

6. Blockchain            (7 con)
   ├─ Tiền ảo - Crypto
   ├─ NFT
   ├─ Coinlist
   ├─ Airdrop
   ├─ Ví điện tử
   ├─ Tài khoản sàn
   └─ Blockchain khác

-- ==============================================================================
-- HỮU DỤNG SQL QUERIES
-- ==============================================================================

-- 1. Xem tất cả danh mục theo cây phân cấp
WITH CTE_Categories AS (
    SELECT id, name, parent_id, 0 as [Level]
    FROM Categories
    WHERE parent_id IS NULL AND isDelete = 0

    UNION ALL

    SELECT c.id, c.name, c.parent_id, ct.[Level] + 1
    FROM Categories c
    INNER JOIN CTE_Categories ct ON c.parent_id = ct.id
    WHERE c.isDelete = 0
)
SELECT
    REPLICATE('  ', [Level]) + name as [Danh mục],
    id,
    parent_id,
    [Level]
FROM CTE_Categories
ORDER BY parent_id, id;

-- 2. Lấy tất cả danh mục con của một danh mục cha
SELECT id, name, parent_id
FROM Categories
WHERE parent_id = 1 AND isDelete = 0;

-- 3. Lấy danh mục cha của một danh mục
SELECT TOP 1 id, name, parent_id
FROM Categories
WHERE id = (SELECT parent_id FROM Categories WHERE id = 9);

-- 4. Đếm danh mục cha và con
SELECT
    COUNT(CASE WHEN parent_id IS NULL THEN 1 END) as [Danh mục Cha],
    COUNT(CASE WHEN parent_id IS NOT NULL THEN 1 END) as [Danh mục Con],
    COUNT(*) as [Tổng cộng]
FROM Categories
WHERE isDelete = 0;

-- 5. Tìm sản phẩm theo danh mục (bao gồm con của con)
SELECT
    p.id, p.name, c.name as [Category],
    pv.price_vnd, pv.stock
FROM Products p
INNER JOIN Categories c ON p.category_id = c.id
WHERE c.parent_id = 2 OR c.id = 2  -- Tài khoản + tất cả con của nó
    AND p.isDelete = 0
    AND c.isDelete = 0;

-- 6. Cập nhật danh mục cha (có sẵn parent_id = NULL)
UPDATE Categories
SET name = 'Email Marketing'
WHERE id = 1 AND parent_id IS NULL;

-- 7. Xóa mềm (Soft Delete) một danh mục
UPDATE Categories
SET isDelete = 1
WHERE id = 15;

-- 8. Khôi phục (Undo Soft Delete) một danh mục
UPDATE Categories
SET isDelete = 0
WHERE id = 15;

-- ==============================================================================
-- FRONTEND INTEGRATION HINTS
-- ==============================================================================

-- Để lấy danh mục cha cho dropdown filter:
SELECT id, name
FROM Categories
WHERE parent_id IS NULL AND isDelete = 0
ORDER BY id;

-- Để lấy danh mục con khi user chọn một cha:
SELECT id, name, parent_id
FROM Categories
WHERE parent_id = @parentId AND isDelete = 0
ORDER BY id;

-- ==============================================================================
-- NOTES
-- ==============================================================================

-- • Tổng: 65 danh mục (6 cha + 59 con)
-- • parent_id = NULL => Danh mục Cha
-- • parent_id > 0 => Danh mục Con
-- • isDelete = 0 => Hiện thị
-- • isDelete = 1 => Ẩn (Soft Delete)
-- • Không bao giờ dùng DELETE cứng trên Categories
-- • Khi tạo sản phẩm, category_id phải là danh mục Con (parent_id IS NOT NULL)

-- ==============================================================================

