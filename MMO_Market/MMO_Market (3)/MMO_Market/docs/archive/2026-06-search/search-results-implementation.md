# 🎯 HƯỚNG DẪN TRIỂN KHAI - SEARCH RESULTS PAGE & CATEGORIES HIERARCHICAL

**Ngày Cập nhật**: 2026-05-28  
**Phiên bản**: 1.0  
**Dự án**: MMO Market System  

---

## 📋 TỔNG QUAN CẬP NHẬT

### 1️⃣ **CẬP NHẬT DATABASE** ✅
- **File SQL**: `sql_scripts/UPDATE_Categories_Hierarchical.sql`
- **Công việc thực hiện**:
  - ✅ Thêm cột `parent_id` vào bảng `Categories`
  - ✅ Tạo Foreign Key constraint `FK_Category_Parent`
  - ✅ Insert 6 danh mục cha (Parent Categories)
  - ✅ Insert 59 danh mục con (Child Categories)
  - ✅ Tạo cấu trúc phân cấp hoàn chỉnh

**Danh mục cha (6 cái):**
1. **Email** (8 con) - Gmail, HotMail, OutlookMail, RuMail, DomainMail, YahooMail, ProtonMail, Loại Mail Khác
2. **Tài khoản** (13 con) - FB, BM, Zalo, Twitter, Telegram, Instagram, Shopee, Discord, TikTok, Key Diệt Virus, Capcut, Key Window, Tài khoản Khác
3. **Phần mềm** (9 con) - Phần Mềm FB, Google, Youtube, Tiền Ảo, PTC, Captcha, Offer, PTU, Khác
4. **Tăng tương tác** (11 con) - Dịch vụ Facebook, Tiktok, Google, Telegram, Shopee, Discord, Twitter, Youtube, Zalo, Instagram, Khác
5. **Dịch vụ phần mềm** (11 con) - Tool MMO, FB, Google, Youtube, TikTok, Instagram, Design, Video Editor, Plugin, Script, Khác
6. **Blockchain** (7 con) - Tiền ảo, NFT, Coinlist, Airdrop, Ví điện tử, Tài khoản sàn, Khác

**Cách chạy SQL:**
```sql
-- Mở SQL Server Management Studio (SSMS)
-- Mở file: UPDATE_Categories_Hierarchical.sql
-- Click Execute hoặc Ctrl + E
-- Kiểm tra kết quả: SELECT * FROM Categories WHERE isDelete = 0;
```

---

### 2️⃣ **FRONTEND - SEARCH RESULTS PAGE** ✅

#### **File cập nhật:**
1. `src/main/resources/templates/search-results.html` - HTML
2. `src/main/resources/static/css/search-results.css` - CSS

#### **Các thành phần giao diện:**

**A. Header (Light Mode)**
- ✅ Logo MMO Market với icon shopping-bag màu cam (#fd761a)
- ✅ Thanh tìm kiếm nhỏ để nhập lại từ khóa
- ✅ Icon giỏ hàng & profile
- ✅ Sticky header (dính khi scroll)

**B. Breadcrumb**
- ✅ Dâu mối: Trang chủ > Kết quả Tìm kiếm
- ✅ Màu xanh dương (#0058be) cho link

**C. Title Section**
- ✅ Hiển thị: **"Kết quả cho "Netflix Premium""**
- ✅ Từ khóa in nghiêng & màu xanh dương
- ✅ Dòng text phụ: "Chúng tôi tìm thấy 128 tài sản số chất lượng cao..."

**D. Layout 2 Cột (25%-75%)**

**Cột Trái - Filters Sidebar (25%)**
- ✅ Nền tus, bo góc 8px, bóng nhẹ
- ✅ 5 nhóm bộ lọc:
  * Danh mục (Dropdown)
  * Khoảng giá (Range Slider với VNĐ)
  * Tình trạng kho (Checkbox)
  * Đánh giá (Checkbox 5☆, 4☆+, 3☆+)
  * Tốc độ giao hàng (Checkbox: Tức thì, 24h, 7 ngày)
- ✅ Nút "Áp dụng Bộ lọc" màu xanh dương gradient
- ✅ Sticky position (dính khi scroll)

**Cột Phải - Product Grid (75%)**
- ✅ Product Header với:
  * Text: "Hiển thị 12 sản phẩm trên 128"
  * Dropdown "Sắp xếp" (Mới nhất, Giá Thấp-Cao, Cao-Thấp, Đánh giá cao, Bán chạy)
- ✅ Grid 3 cột sản phẩm
- ✅ 12 Product Cards (có thể tăng/giảm)

**E. Product Card**
```
┌─────────────────────────┐
│                         │
│   [Image + Tag Badge]   │  Tag: "Giao Tức Thì" (xanh) / "Bán Chạy" (cam)
│                         │
├─────────────────────────┤
│ Category | Rating ⭐    │  VD: "Streaming | ⭐ 4.9"
│ Product Title Bold      │  VD: "Netflix Premium 4K UHD..."
│ 🏪 StoreMaster ✓        │  Seller badge với tick xanh
│ Price | Stock           │  VD: "450.000 VNĐ | 15 sản phẩm"
│ [Mua Ngay Button]       │  Nút cam, chữ trắng, full width
└─────────────────────────┘
```

**F. Pagination**
- ✅ Nút Previous (<)
- ✅ Số trang (1, 2, 3, 4, 5, ..., 11)
- ✅ Nút Next (>)
- ✅ Active page có nền xanh dương gradient

**G. Footer**
- ✅ Nền icon tối (#0a192f)
- ✅ 3 cột: Về MMO Market, Hỗ trợ, Cộng đồng
- ✅ Border-top cam (#fd761a)
- ✅ Copyright & tagline

---

## 🎨 **COLOR PALETTE & STYLING GUIDE**

| Loại | Màu | Mã Hex | Cách Dùng |
|------|-----|--------|----------|
| **Primary (Xanh)** | Xanh dương | #0058be | Link, Button Apply, Active states |
| **Accent (Cam)** | Cam | #fd761a | "Mua Ngay" button, "Giao Tức Thì", Tags |
| **Background** | Xám nhạt | #F8F9FB | Nền trang chính |
| **Card** | Trắng | #FFFFFF | Các khối card/filter |
| **Text** | Tối | #333, #555 | Chữ thông thường |
| **Secondary** | Xám | #666, #999 | Chữ phụ, placeholder |
| **Success** | Xanh lá | #28a745 | "Giao Tức Thì" tag |
| **Dark Footer** | Tối | #0a192f | Footer background |

**Font**: Be Vietnam Pro (400, 500, 600, 700 weights)

---

## 🔧 **CÁCH CHẠY & KIỂM TRA**

### **Step 1: Cập nhật Database**
```bash
# Mở SSMS
# Chạy file: sql_scripts/UPDATE_Categories_Hierarchical.sql
# Kiểm tra:
SELECT COUNT(*) as 'Tổng Categories' FROM Categories WHERE isDelete = 0;
SELECT COUNT(*) as 'Danh mục Cha' FROM Categories WHERE parent_id IS NULL AND isDelete = 0;
SELECT COUNT(*) as 'Danh mục Con' FROM Categories WHERE parent_id IS NOT NULL AND isDelete = 0;
```

### **Step 2: Rebuild Project**
```bash
# Cách 1: Maven
mvn clean package

# Cách 2: IntelliJ IDEA
Build > Rebuild Project (Ctrl + F9)
```

### **Step 3: Run Application**
```bash
# Spring Boot sẽ start trên port 8080
# URL: http://localhost:8080/

# Kiểm tra Search Results Page:
# 1. Vào Trang chủ
# 2. Nhập "Netflix Premium" vào thanh tìm kiếm
# 3. Bấm "Tìm"
# 4. Chuyển hướng sang: /search?q=Netflix+Premium hoặc tương tự
```

### **Step 4: Test Features**
- ✅ Breadcrumb click: Quay lại Home
- ✅ Header search: Tìm kiếm lại từ khóa khác
- ✅ Filter sidebar: Áp dụng các bộ lọc
- ✅ Price range slider: Kéo thay đổi giá
- ✅ Product cards: Hover hiệu ứng, click "Mua Ngay"
- ✅ Pagination: Chuyển trang
- ✅ Responsive: Kiểm tra trên mobile (DevTools F12 > Toggle device toolbar)

---

## 📱 **RESPONSIVE DESIGN**

| Breakpoint | Thay đổi |
|------------|---------|
| **Desktop (1024px+)** | Layout 2 cột, grid 3 sản phẩm |
| **Tablet (768px-1023px)** | Grid 2 sản phẩm, sidebar trên |
| **Mobile (480px-767px)** | Grid 1 sản phẩm, side layout |
| **Small (< 480px)** | Card layout ngang, full responsive |

---

## 🔌 **TÍCH HỢP BACKEND (HẬU TỬI)**

### **Endpoints cần tạo:**

**1. Search API**
```java
@GetMapping("/api/v1/search")
public ModelAndView search(@RequestParam String q, 
                           @RequestParam(defaultValue = "0") int page) {
    // Tìm kiếm Products theo keyword
    // Sắp xếp theo mới nhất
    // Return ModelAndView("search-results", productList)
}
```

**2. Filter API** (Optional - có thể làm sau)
```java
@PostMapping("/api/v1/products/filter")
public ResponseEntity<?> filterProducts(@RequestBody FilterRequest filters) {
    // Lọc theo category, price, rating, stock
    // Return JSON list sản phẩm
}
```

**3. Thymeleaf Controller**
```java
@GetMapping("/search")
public String searchPage(
    @RequestParam(required = false) String q,
    Model model) {
    
    model.addAttribute("searchQuery", q);
    // Lấy products từ DB
    model.addAttribute("products", products);
    model.addAttribute("totalCount", products.size());
    
    return "search-results";
}
```

### **Database Query (T-SQL)**
```sql
-- Tìm sản phẩm theo từ khóa (LIKE) với category
SELECT 
    p.id, p.name, p.image, c.name as category,
    pv.price_vnd, pv.stock,
    (SELECT AVG(r.rating) FROM Reviews r WHERE r.product_id = p.id) as avg_rating,
    u.full_name as seller_name, u.shop_status
FROM Products p
INNER JOIN Categories c ON p.category_id = c.id
INNER JOIN ProductVariants pv ON p.id = pv.product_id
INNER JOIN Users u ON p.seller_id = u.id
WHERE p.name LIKE '%' + @keyword + '%' 
    AND p.isDelete = 0 
    AND c.isDelete = 0
    AND u.shop_status = 'Active'
ORDER BY p.created_at DESC
OFFSET @page * 12 ROWS FETCH NEXT 12 ROWS ONLY;
```

---

## ⚠️ **CHÚ Ý QUAN TRỌNG**

### **1. Cấu trúc phân cấp Categories**
- ✅ `parent_id = NULL` = Danh mục cha
- ✅ `parent_id = 1,2,3...` = Danh mục con
- ✅ Khi hiển thị filter, chỉ show danh mục cha (sau này có sub-menu)

### **2. Currency (VNĐ)**
- ✅ Tất cả giá đều là **VNĐ** (BIGINT trong DB)
- ✅ Hiển thị: "45.000 VNĐ", "1.500.000 VNĐ"
- ✅ Format: `.toLocaleString('vi-VN')`

### **3. Soft Delete**
- ✅ **KHÔNG** xóa trực tiếp bằng DELETE
- ✅ Luôn thêm điều kiện `WHERE isDelete = 0`
- ✅ Cập nhật: `UPDATE Categories SET isDelete = 1 WHERE id = X;`

### **4. Transactions (Giao dịch tài chính)**
- ✅ "Mua Ngay" button chỉ là demo, sẽ link đến checkout
- ✅ Escrow sẽ tự động locking tiền 72 giờ (trigger DB)
- ✅ Xem chi tiết: `AGENTS.md` - Phần "Cơ chế Giam tiền (Escrow)"

### **5. Phân quyền (RBAC)**
- ✅ Trang Search Results công khai (không cần login)
- ✅ Button "Mua Ngay" sẽ check xem user đã login chưa
- ✅ Tuân theo AGENTS.md -> phần 6 "TIÊU CHÍ HOÀN THÀNH"

---

## 📝 **CHECKLIST HOÀN THÀNH**

- [x] ✅ Cập nhật SQL - Thêm parent_id vào Categories
- [x] ✅ Insert 6 danh mục cha + 59 danh mục con
- [x] ✅ Tạo search-results.html (Vietnamese, VNĐ currency)
- [x] ✅ Tạo search-results.css (Responsive, gradient buttons)
- [x] ✅ Header Light Mode với sticky position
- [x] ✅ Filter Sidebar với 5 nhóm lọc
- [x] ✅ Product Grid 3 cột + 12 sample cards
- [x] ✅ Pagination controls
- [x] ✅ Dark Footer với 3 sections
- [x] ✅ JavaScript functionality (search, filters, pagination)
- [x] ✅ Responsive design (Desktop, Tablet, Mobile)
- [x] ✅ Color scheme: Xanh #0058be, Cam #fd761a, Xám #F8F9FB

---

## 📚 **TÀI LIỆU THAM KHẢO**

- **AGENTS.md** - Ngữ cảnh dự án & architecture principles
- **DATABASE.md** - Schema chi tiết
- **Specification.md** - Feature specification
- **UPDATE_Categories_Hierarchical.sql** - SQL script

---

## ✉️ **CẦN HỖ TRỢ?**

Nếu có lỗi:
1. Kiểm tra console browser (F12)
2. Kiểm tra server logs (Terminal)
3. Xác nhận SQL đã chạy thành công
4. Clear browser cache (Ctrl + Shift + Delete)

---

**Cập nhật lần cuối**: 2026-05-28  
**Người tạo**: AI Copilot (GitHub Copilot)  
**Trạng thái**: ✅ HOÀN THÀNH

