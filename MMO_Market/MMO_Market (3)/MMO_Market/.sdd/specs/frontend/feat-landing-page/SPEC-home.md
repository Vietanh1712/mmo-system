# SPEC — Trang Chủ Công Khai (Public Home Catalog)

> **Feature ID:** `feat-landing-page` | **Page:** `Home`
> **Route:** `/` | **Template:** `templates/home.html`
> **CSS Script:** `static/css/customer/style.css`
> **JS Script:** `static/js/home.js`
> **Version:** 1.0 | **Status:** Draft
> **Backend ref:** `feat-product/UC-05-product-discovery.md`

---

## 1. TỔNG QUAN TRANG

Trang chủ là điểm chạm đầu tiên của khách hàng vãng lai và người mua. Mục tiêu: **hiển thị danh mục sản phẩm số trực quan, tìm kiếm nhanh và hiển thị sản phẩm mới nhất**.

**Cấu trúc trang:**
1. **Header (Navbar):** Logo, các liên kết danh mục, thanh tìm kiếm, và khối trạng thái tài khoản (Đăng nhập/Đăng ký hoặc Số dư ví + Menu con hồ sơ).
2. **Hero Search Section:** Thanh tìm kiếm lớn nổi bật giữa nền gradient để nhập từ khóa tìm kiếm nhanh.
3. **Categories Grid:** Danh sách 4 danh mục chính (ACCOUNT, KEY, GAME_CARD, SERVICE) hiển thị dạng thẻ tròn với micro-animations khi hover.
4. **Product Grid:** Danh sách các sản phẩm mới nhất hiển thị dạng thẻ thông tin (Product Cards).
5. **Footer:** Thông tin bản quyền, điều khoản sàn và liên kết mạng xã hội.

---

## 2. DESIGN TOKENS ÁP DỤNG

```css
/* Color Palette */
--color-primary:      #0f172a; /* Slate 900 - màu chữ và navbar */
--color-brand:        #2563eb; /* Blue 600 - màu nhấn thương hiệu */
--color-brand-hover:  #1d4ed8; /* Blue 700 - màu hover nhấn */
--color-bg-gray:      #f8fafc; /* Slate 50 - nền trang chủ */
--color-border:       #e2e8f0; /* Slate 200 - đường viền */
--color-card-bg:      #ffffff; /* Nền card trắng */
--color-text-main:    #334155; /* Slate 700 - chữ chính */
--color-text-sub:     #64748b; /* Slate 500 - chữ phụ */

/* Shape & Shadows */
--radius-md:          12px;
--radius-lg:          16px;
--radius-full:        9999px;
--shadow-sm:          0 1px 3px rgba(0,0,0,0.05);
--shadow-card:        0 4px 6px -1px rgba(0,0,0,0.05), 0 2px 4px -1px rgba(0,0,0,0.03);
--shadow-hover:       0 10px 15px -3px rgba(0,0,0,0.08), 0 4px 6px -2px rgba(0,0,0,0.04);
```

---

## 3. LAYOUT TỔNG THỂ & MOCKUP

```
┌────────────────────────────────────────────────────────┐
│  [Logo] MMO Market     [Search...]      [Đăng nhập]    │ <-- Navbar (Sticky)
├────────────────────────────────────────────────────────┤
│                                                        │
│             MUA BÁN SẢN PHẨM SỐ TIN CẬY                │ <-- Hero Banner
│           [ Nhập từ khóa sản phẩm số...    ] [Tìm]      │
│                                                        │
├────────────────────────────────────────────────────────┤
│     [ACCOUNT]     [KEY]     [GAME CARD]     [SERVICE]  │ <-- Categories Grid
├────────────────────────────────────────────────────────┤
│  SẢN PHẨM MỚI NHẤT                                     │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐    │
│  │ Product Card │ │ Product Card │ │ Product Card │    │ <-- Product Grid
│  └──────────────┘ └──────────────┘ └──────────────┘    │
└────────────────────────────────────────────────────────┘
```

---

## 4. CÁC THÀNH PHẦN GIAO DIỆN CHÍNH

### 4.1 Thanh Điều Hướng (Navbar) — `.main-navbar`
* **Layout:** `flex`, `justify-content: space-between`, `align-items: center`, `height: 72px`, `position: sticky`, `top: 0`, `z-index: 100`, `background: var(--color-card-bg)`, `border-bottom: 1px solid var(--color-border)`.
* **Khối Tài Khoản (User Area):**
  * *Chưa đăng nhập:* Hiển thị hai nút: "Đăng nhập" (nền viền) và "Đăng ký" (nền xanh thương hiệu).
  * *Đã đăng nhập:* Hiển thị số dư ví (`available_balance` định dạng VNĐ) và biểu tượng avatar người dùng. Hover vào avatar sẽ kích hoạt trình thả xuống (Dropdown menu) chứa liên kết: Kênh người bán, Ví tiền, Lịch sử mua hàng, Hồ sơ cá nhân và nút Đăng xuất.

### 4.2 Khu Vực Tìm Kiếm (Hero Search) — `.hero-section`
* **Layout:** Chiều cao `280px`, nền gradient từ xanh đậm đến xanh nhạt. Căn giữa toàn bộ nội dung.
* **Thanh tìm kiếm `.hero-search-box`:**
  * Thân hộp tìm kiếm: Rộng tối đa `640px`, border-radius `var(--radius-full)`, nền trắng, shadow nổi bật.
  * Ô input: Không viền mặc định, font-size `16px`.
  * Nút tìm kiếm: Tích hợp icon kính lúp, nền màu xanh thương hiệu, bo tròn pill shape.

### 4.3 Thẻ Sản Phẩm — `.product-card`
* **Layout:** Grid 4 cột trên desktop, bo tròn `var(--radius-md)`, shadow nhẹ.
* **Hiệu ứng hover:** Dịch chuyển nhẹ lên phía trên (`transform: translateY(-4px)`) và shadow đậm hơn (`var(--shadow-hover)`).
* **Nội dung thẻ:**
  * Ảnh đại diện sản phẩm (`.card-img`).
  * Tên sản phẩm, nhãn danh mục dạng badge nhỏ.
  * Tên Shop người bán (bấm để xem shop chi tiết).
  * Giá tiền (đỏ nổi bật dạng số nguyên VNĐ) và badge số lượng tồn kho.

---

## 5. LUỒNG XỬ LÝ JS & AJAX

1. **Khởi tạo trang chủ:**
   * Trực tiếp kết xuất thông tin sản phẩm và danh mục thông qua cơ chế Server-side Rendering của Thymeleaf để tăng tốc độ SEO.
2. **Xử lý sự kiện tìm kiếm nhanh:**
   * Khi người dùng gõ từ khóa vào ô tìm kiếm và bấm Enter hoặc click nút "Tìm kiếm":
     * JS ngăn cản hành vi mặc định và điều hướng sang trang kết quả tìm kiếm: `/search?query={encodeURIComponent(keyword)}`.
3. **Menu thả xuống ví tiền & Hồ sơ:**
   * JS lắng nghe sự kiện click trên biểu tượng Avatar người dùng để hiển thị và ẩn `.user-dropdown` bằng cách toggle class `.active`.

---

## 6. RESPONSIVE

| Breakpoint | Mô tả thay đổi bố cục |
|:---|:---|
| `≥ 1024px` | Product Grid hiển thị 4 cột. |
| `< 1024px` | Product Grid hiển thị 3 cột. Ẩn bớt các link phụ trên Navbar chuyển vào Hamburger Menu. |
| `< 768px`  | Product Grid hiển thị 2 cột. Hero Search giảm chiều cao còn `200px` và font-size tiêu đề nhỏ đi. |
| `< 480px`  | Product Grid hiển thị 1 cột đơn. Navbar chỉ giữ logo và biểu tượng Hamburger Menu di động. |

---

## 7. ACCESSIBILITY

- Sử dụng `aria-expanded` để thông báo trạng thái đóng mở của Dropdown Menu người dùng.
- Ô input tìm kiếm chính có nhãn ẩn `aria-label="Tìm kiếm sản phẩm số"`.
- Tab order chạy tự nhiên: Logo -> Ô nhập từ khóa -> Nút tìm kiếm -> Các thẻ danh mục -> Các thẻ sản phẩm.

---

## 8. OUT OF SCOPE

- ❌ Tự động tải thêm sản phẩm khi cuộn trang (Infinite scroll) — Sử dụng phân trang số truyền thống.
- ❌ Chatbot AI trực tiếp tại góc màn hình trang chủ.