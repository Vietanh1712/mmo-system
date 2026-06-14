USE MMO_System;
GO

-- 1. Cập nhật các danh mục cha (Parent Categories)
UPDATE Categories SET name = N'Email', description = N'Các dịch vụ email và tài khoản mail' WHERE id = 1;
UPDATE Categories SET name = N'Tài khoản', description = N'Tài khoản các nền tảng xã hội & dịch vụ' WHERE id = 2;
UPDATE Categories SET name = N'Phần mềm', description = N'Các công cụ phần mềm chuyên dụng cho kinh doanh online' WHERE id = 3;
UPDATE Categories SET name = N'Tăng tương tác', description = N'Các dịch vụ tăng engagement & tương tác trên mạng xã hội' WHERE id = 4;
UPDATE Categories SET name = N'Dịch vụ phần mềm', description = N'Công cụ, plugin và dịch vụ lập trình' WHERE id = 5;
UPDATE Categories SET name = N'Blockchain', description = N'Các sản phẩm tiền ảo, NFT và blockchain' WHERE id = 6;

-- 2. Cập nhật các danh mục con - Con của "Email" (parent_id = 1)
UPDATE Categories SET name = N'Gmail', description = N'Tài khoản Gmail và G Suite' WHERE id = 7;
UPDATE Categories SET name = N'HotMail', description = N'Tài khoản Hotmail/Outlook Mail' WHERE id = 8;
UPDATE Categories SET name = N'OutlookMail', description = N'Email Outlook chính thức' WHERE id = 9;
UPDATE Categories SET name = N'RuMail', description = N'Tài khoản mail từ các trang Nga' WHERE id = 10;
UPDATE Categories SET name = N'DomainMail', description = N'Email tên miền riêng' WHERE id = 11;
UPDATE Categories SET name = N'YahooMail', description = N'Tài khoản Yahoo Mail' WHERE id = 12;
UPDATE Categories SET name = N'ProtonMail', description = N'Email bảo mật ProtonMail' WHERE id = 13;
UPDATE Categories SET name = N'Loại Mail Khác', description = N'Các loại email khác' WHERE id = 14;

-- 3. Cập nhật các danh mục con - Con của "Tài khoản" (parent_id = 2)
UPDATE Categories SET name = N'Tài khoản FB', description = N'Tài khoản Facebook cá nhân & fanpage' WHERE id = 15;
UPDATE Categories SET name = N'Tài Khoản BM', description = N'Tài khoản Business Manager Facebook' WHERE id = 16;
UPDATE Categories SET name = N'Tài Khoản Zalo', description = N'Tài khoản Zalo OA & cá nhân' WHERE id = 17;
UPDATE Categories SET name = N'Tài Khoản Twitter', description = N'Tài khoản Twitter/X với followers' WHERE id = 18;
UPDATE Categories SET name = N'Tài Khoản Telegram', description = N'Tài khoản Telegram Group & Channel' WHERE id = 19;
UPDATE Categories SET name = N'Tài Khoản Instagram', description = N'Tài khoản Instagram với followers' WHERE id = 20;
UPDATE Categories SET name = N'Tài Khoản Shopee', description = N'Tài khoản Shopee bán hàng' WHERE id = 21;
UPDATE Categories SET name = N'Tài Khoản Discord', description = N'Tài khoản Discord với server' WHERE id = 22;
UPDATE Categories SET name = N'Tài Khoản TikTok', description = N'Tài khoản TikTok với followers' WHERE id = 23;
UPDATE Categories SET name = N'Key Diệt Virus', description = N'Key phần mềm diệt virus chính hãng' WHERE id = 24;
UPDATE Categories SET name = N'Tài Khoản Capcut', description = N'Tài khoản Capcut Pro' WHERE id = 25;
UPDATE Categories SET name = N'Key Window', description = N'Key Windows & Office chính hãng' WHERE id = 26;
UPDATE Categories SET name = N'Tài Khoản Khác', description = N'Các tài khoản khác' WHERE id = 27;

-- 4. Cập nhật các danh mục con - Con của "Phần mềm" (parent_id = 3)
UPDATE Categories SET name = N'Phần Mềm FB', description = N'Tool & phần mềm quản lý Facebook' WHERE id = 28;
UPDATE Categories SET name = N'Phần Mềm Google', description = N'Công cụ Google Ads, SEO, Analytics' WHERE id = 29;
UPDATE Categories SET name = N'Phần Mềm Youtube', description = N'Tool quản lý & tối ưu hóa Youtube' WHERE id = 30;
UPDATE Categories SET name = N'Phần Mềm Tiền Ảo', description = N'Software trading & quản lý crypto' WHERE id = 31;
UPDATE Categories SET name = N'Phần Mềm PTC', description = N'Phần mềm kiếm tiền PTC tự động' WHERE id = 32;
UPDATE Categories SET name = N'Phần Mềm Captcha', description = N'Giải captcha tự động 2captcha, Anti-captcha' WHERE id = 33;
UPDATE Categories SET name = N'Phần Mềm Offer', description = N'Tool kiếm tiền từ Offer Wall' WHERE id = 34;
UPDATE Categories SET name = N'Phần Mềm PTU', description = N'Phần mềm quản lý PTU (Paid Task)' WHERE id = 35;
UPDATE Categories SET name = N'Phần Mềm Khác', description = N'Các phần mềm khác' WHERE id = 36;

-- 5. Cập nhật các danh mục con - Con của "Tăng tương tác" (parent_id = 4)
UPDATE Categories SET name = N'Dịch vụ Facebook', description = N'Tăng like, follow, bình luận Facebook' WHERE id = 37;
UPDATE Categories SET name = N'Dịch vụ Tiktok', description = N'Tăng view, like, follow TikTok' WHERE id = 38;
UPDATE Categories SET name = N'Dịch vụ Google', description = N'Dịch vụ SEO & tối ưu Google' WHERE id = 39;
UPDATE Categories SET name = N'Dịch vụ Telegram', description = N'Tăng member Telegram Channel/Group' WHERE id = 40;
UPDATE Categories SET name = N'Dịch vụ Shopee', description = N'Tăng view, mua hàng Shopee' WHERE id = 41;
UPDATE Categories SET name = N'Dịch vụ Discord', description = N'Tăng member Discord server' WHERE id = 42;
UPDATE Categories SET name = N'Dịch vụ Twitter', description = N'Tăng follower, retweet Twitter' WHERE id = 43;
UPDATE Categories SET name = N'Dịch vụ Youtube', description = N'Tăng view, subcriber Youtube' WHERE id = 44;
UPDATE Categories SET name = N'Dịch vụ Zalo', description = N'Tăng member Zalo OA & tương tác' WHERE id = 45;
UPDATE Categories SET name = N'Dịch vụ Instagram', description = N'Tăng follow, like Instagram' WHERE id = 46;
UPDATE Categories SET name = N'Tương tác khác', description = N'Các dịch vụ tương tác khác' WHERE id = 47;

-- 6. Cập nhật các danh mục con - Con của "Dịch vụ phần mềm" (parent_id = 5)
UPDATE Categories SET name = N'Tool MMO', description = N'Công cụ MMO marketing tự động' WHERE id = 48;
UPDATE Categories SET name = N'Tool Facebook', description = N'Tool chuyên biệt Facebook' WHERE id = 49;
UPDATE Categories SET name = N'Tool Google', description = N'Tool kiếm tiền Google' WHERE id = 50;
UPDATE Categories SET name = N'Tool Youtube', description = N'Bot & công cụ Youtube' WHERE id = 51;
UPDATE Categories SET name = N'Tool TikTok', description = N'Công cụ TikTok automation' WHERE id = 52;
UPDATE Categories SET name = N'Tool Instagram', description = N'Bot Instagram & follow automation' WHERE id = 53;
UPDATE Categories SET name = N'Đồ họa - Design', description = N'Công cụ thiết kế đồ họa' WHERE id = 54;
UPDATE Categories SET name = N'Video Editor', description = N'Phần mềm chỉnh sửa video' WHERE id = 55;
UPDATE Categories SET name = N'Plugin & Extension', description = N'Plugin browser & extension hữu ích' WHERE id = 56;
UPDATE Categories SET name = N'Script & Bot', description = N'Script tự động hóa & bot công việc' WHERE id = 57;
UPDATE Categories SET name = N'Phần mềm khác', description = N'Các phần mềm khác' WHERE id = 58;

-- 7. Cập nhật các danh mục con - Con của "Blockchain" (parent_id = 6)
UPDATE Categories SET name = N'Tiền ảo - Crypto', description = N'Bitcoin, Ethereum, Altcoin khác' WHERE id = 59;
UPDATE Categories SET name = N'NFT', description = N'Token NFT và digital art' WHERE id = 60;
UPDATE Categories SET name = N'Coinlist', description = N'Coinlist & các IDO token mới' WHERE id = 61;
UPDATE Categories SET name = N'Airdrop', description = N'Airdrop token & chiến dịch phát free' WHERE id = 62;
UPDATE Categories SET name = N'Ví điện tử', description = N'Ví tiền điện tử & wallet' WHERE id = 63;
UPDATE Categories SET name = N'Tài khoản sàn', description = N'Tài khoản Binance, Bybit, OKX, v.v' WHERE id = 64;
UPDATE Categories SET name = N'Blockchain khác', description = N'Các sản phẩm blockchain khác' WHERE id = 65;

PRINT N'✓ Đã cập nhật sửa lỗi phông chữ tiếng Việt cho 65 danh mục thành công!';
GO
