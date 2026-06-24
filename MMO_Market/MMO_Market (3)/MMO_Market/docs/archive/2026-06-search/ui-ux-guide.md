# UI_UX_GUIDE.md

## Mục đích

Tài liệu này là nguồn tham chiếu giao diện (UI) và trải nghiệm người dùng (UX) cho MMO Market.

Nguồn nội dung:

* VISUAL_REFERENCE_GUIDE.md

Lưu ý:

* Không chứa nghiệp vụ.
* Không chứa schema database.
* Không chứa logic backend.
* Chỉ dùng làm chuẩn thiết kế giao diện.

---

# DESIGN PRINCIPLES

## Mục tiêu giao diện

* Hiện đại
* Sạch sẽ
* Dễ đọc
* Tối ưu chuyển đổi
* Responsive
* Thân thiện với thiết bị di động

---

# COLOR SYSTEM

## Primary Color

Tên:

```text
Primary Blue
```

Mã màu:

```text
#0058be
```

Sử dụng cho:

* Link
* Button chính
* Active State
* Search Highlight
* Pagination Active

---

## Accent Color

Tên:

```text
Accent Orange
```

Mã màu:

```text
#fd761a
```

Sử dụng cho:

* Buy Button
* CTA
* Badge
* Highlight

---

## Background Color

Tên:

```text
Light Gray
```

Mã màu:

```text
#F8F9FB
```

Sử dụng cho:

* Nền trang

---

## Card Background

Tên:

```text
White
```

Mã màu:

```text
#FFFFFF
```

Sử dụng cho:

* Product Card
* Sidebar
* Modal
* Filter Panel

---

## Text Primary

Mã màu:

```text
#333333
```

---

## Text Secondary

Mã màu:

```text
#666666
```

---

## Success

Mã màu:

```text
#28a745
```

Sử dụng cho:

* Success Badge
* Instant Delivery Tag

---

## Footer Background

Mã màu:

```text
#0a192f
```

---

# TYPOGRAPHY

## Font Family

```text
Be Vietnam Pro
```

---

## Font Weight

Regular

```text
400
```

---

Medium

```text
500
```

---

Semi Bold

```text
600
```

---

Bold

```text
700
```

---

## Suggested Usage

Body Text

```text
14px
400
```

---

Label

```text
14px
600
```

---

Title

```text
16px
700
```

---

Page Header

```text
22px - 28px
700
```

---

# BUTTON DESIGN

## Primary Button

Ví dụ:

```text
MUA NGAY
```

Style:

```css
background:
linear-gradient(
135deg,
#fd761a,
#ff9100
);
```

Đặc điểm:

* Chữ trắng
* Border Radius 6px
* Hover nâng lên nhẹ

---

## Secondary Button

Ví dụ:

```text
ÁP DỤNG BỘ LỌC
```

Style:

```css
background:
linear-gradient(
135deg,
#0058be,
#004294
);
```

---

# CARD DESIGN

## Product Card

Style:

```css
background: #FFFFFF;
border-radius: 8px;
```

---

Bao gồm:

* Product Image
* Category
* Rating
* Product Name
* Seller
* Price
* Stock
* Buy Button

---

## Filter Card

Style:

```css
background: #FFFFFF;
border-radius: 8px;
position: sticky;
```

---

# BADGE SYSTEM

## Instant Delivery

Text:

```text
GIAO TỨC THÌ
```

Màu:

```text
#28a745
```

---

## Bestseller

Text:

```text
BÁN CHẠY
```

Màu:

```text
#fd761a
```

---

## Category Badge

Màu:

```text
#f0f0f0
```

---

# HEADER DESIGN

Bao gồm:

* Logo
* Search Box
* Cart
* User Profile

Đặc điểm:

```css
position: sticky;
top: 0;
```

---

# SEARCH BAR

Bao gồm:

* Input Search
* Search Button

Ví dụ:

```text
Netflix
ChatGPT
Facebook
```

---

# SIDEBAR FILTER

Bao gồm:

## Category

Ví dụ:

```text
Email
Tài khoản
Phần mềm
Blockchain
```

---

## Price Range

Ví dụ:

```text
0
100.000
500.000
1.000.000
```

---

## Stock

```text
Còn hàng
Hết hàng
```

---

## Rating

```text
5★
4★+
3★+
```

---

## Delivery

```text
Instant
24h
7 ngày
```

---

# PRODUCT GRID

## Desktop

```text
3 Columns
```

---

## Tablet

```text
2 Columns
```

---

## Mobile

```text
1 Column
```

---

# PRODUCT CARD LAYOUT

```text
+----------------------+
| Image                |
| Badge                |
+----------------------+
| Category | Rating    |
| Product Name         |
| Seller               |
| Price                |
| Stock                |
| Buy Button           |
+----------------------+
```

---

# PAGINATION

Bao gồm:

```text
<
1
2
3
4
5
>
```

---

Active Page:

```text
#0058be
```

---

# FOOTER

Bao gồm:

## About

Thông tin MMO Market

---

## Support

Hỗ trợ khách hàng

---

## Community

Kênh cộng đồng

---

Footer Color:

```text
#0a192f
```

---

# RESPONSIVE DESIGN

## Desktop

```text
>= 1024px
```

Layout:

```text
Sidebar 25%
Content 75%
```

---

## Tablet

```text
768px - 1023px
```

Layout:

```text
Sidebar Top
Grid 2 Columns
```

---

## Mobile

```text
< 768px
```

Layout:

```text
Single Column
```

---

# SPACING SYSTEM

## Border Radius

Card

```text
8px
```

Button

```text
6px
```

Input

```text
4px
```

---

## Grid Gap

Desktop

```text
20px
```

Tablet

```text
15px
```

Mobile

```text
12px
```

---

# ICON SYSTEM

Khuyến nghị sử dụng:

```text
FontAwesome
```

hoặc

```text
Lucide
```

Các icon phổ biến:

* Search
* Shopping Cart
* User
* Store
* Star
* Check Circle
* Chevron Left
* Chevron Right
* Filter

---

# ACCESSIBILITY

## Contrast

Primary:

```text
#0058be
```

phải đảm bảo đọc rõ trên nền trắng.

---

## Focus State

Tất cả:

* Button
* Link
* Input

phải có trạng thái Focus.

---

## Text Size

Body tối thiểu:

```text
14px
```

---

# ANIMATION

## Hover

Button:

```css
transform: translateY(-2px);
```

---

Card:

```css
transform: translateY(-4px);
```

---

## Transition

```css
transition: all .3s ease;
```

---

# BROWSER SUPPORT

Chrome

```text
90+
```

Firefox

```text
88+
```

Safari

```text
14+
```

Edge

```text
90+
```

---

# FUTURE UI MODULES

Các module sẽ sử dụng cùng Design System:

* Homepage
* Search
* Product Detail
* Cart
* Checkout
* Wallet
* KYC
* Complaint
* Seller Dashboard
* Admin Dashboard

---

# RELATED FILES

* AGENTS.md
* DATABASE.md
* SPECIFICATION.md
* SEARCH_MODULE.md
* IMPLEMENTATION_HISTORY.md

---

# IMPORTANT NOTES

1. Mọi giao diện phải tuân thủ Color System.

2. Mọi giao diện phải Responsive.

3. Không sử dụng màu sắc ngoài Design System nếu chưa được phê duyệt.

4. Không thay đổi Typography nếu chưa cập nhật tài liệu này.

5. Đây là tài liệu UI chuẩn cho MMO Market.
