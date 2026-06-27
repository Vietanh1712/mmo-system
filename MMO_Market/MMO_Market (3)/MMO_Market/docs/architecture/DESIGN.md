---
version: alpha
name: MMO-Market-Design-System
description: MMO Market uses a professional dark-slate and peach design system ("Nexus Market") to project credibility, clarity, and safety for C2C digital commerce. Anchored by Roboto typography, it features 8px-rounded UI elements, soft card elevations, and distinct visual treatments for roles (Buyer, Seller, Staff, Admin) to optimize transaction security and escrow management.

colors:
  primary: "#0f172a"          # Brand Dark Slate (Chính)
  primary-hover: "#1e293b"    # Hover variant
  primary-light: "#fff7f3"    # Light Peach hover background
  cta-orange: "#fbceb5"       # Brand Peach highlight (Nút mua/nạp tiền)
  cta-orange-hover: "#f0b99a" # Brand Peach hover highlight
  canvas: "#ffffff"           # Canvas background
  surface: "#f8fafc"          # Slate light background (Nền phụ)
  hairline: "#e2e8f0"         # Slate border gray
  hairline-strong: "#cbd5e1"  # Input border slate
  ink-deep: "#090d16"         # Near black
  ink: "#0f172a"              # Primary text
  charcoal: "#334155"         # Warm charcoal body
  slate: "#475569"            # Secondary slate text
  steel: "#64748b"            # Tertiary steel
  muted: "#94a3b8"            # Placeholders, disabled
  on-dark: "#ffffff"          # White text
  semantic-success: "#10b981" # Safe Emerald Green (Xác nhận, đã nhận)
  semantic-warning: "#f59e0b" # Alert Amber Orange (Đang tạm giữ Escrow)
  semantic-error: "#ef4444"   # Danger Rose Red (Khiếu nại, hủy đơn)

typography:
  hero-display:
    fontFamily: Roboto
    fontSize: 48px
    fontWeight: 700
    lineHeight: 1.15
    letterSpacing: -1px
  display-lg:
    fontFamily: Roboto
    fontSize: 36px
    fontWeight: 700
    lineHeight: 1.20
  heading-1:
    fontFamily: Roboto
    fontSize: 30px
    fontWeight: 700
    lineHeight: 1.25
  heading-2:
    fontFamily: Roboto
    fontSize: 24px
    fontWeight: 600
    lineHeight: 1.30
  heading-3:
    fontFamily: Roboto
    fontSize: 20px
    fontWeight: 600
    lineHeight: 1.35
  body-md:
    fontFamily: Roboto
    fontSize: 16px
    fontWeight: 400
    lineHeight: 1.50
  body-sm:
    fontFamily: Roboto
    fontSize: 14px
    fontWeight: 400
    lineHeight: 1.45
  caption:
    fontFamily: Roboto
    fontSize: 12px
    fontWeight: 500
    lineHeight: 1.40
  money:
    fontFamily: Roboto
    fontSize: 16px
    fontWeight: 700
    lineHeight: 1.40

rounded:
  xs: 4px
  sm: 6px
  md: 8px
  lg: 12px
  xl: 16px
  full: 9999px

spacing:
  xxs: 4px
  xs: 8px
  sm: 12px
  md: 16px
  lg: 20px
  xl: 24px
  xxl: 32px
  xxxl: 40px
  section-sm: 48px
  section: 64px

components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.on-dark}"
    rounded: "{rounded.md}"
    padding: "10px 18px"
  button-cta:
    backgroundColor: "{colors.cta-orange}"
    textColor: "{colors.primary}"
    rounded: "{rounded.md}"
    padding: "10px 18px"
  card-base:
    backgroundColor: "{colors.canvas}"
    rounded: "{rounded.lg}"
    padding: "{spacing.xl}"
    border: "1px solid {colors.hairline}"
  text-input:
    backgroundColor: "{colors.canvas}"
    textColor: "{colors.ink}"
    rounded: "{rounded.md}"
    padding: "{spacing.sm} {spacing.md}"
    border: "1px solid {colors.hairline-strong}"
    height: 44px
---

## Overview

MMO Market xây dựng ngôn ngữ thiết kế **Nexus Market** tập trung vào độ tin cậy và sự minh bạch của các giao dịch sản phẩm số C2C. Hệ thống sử dụng gam màu chủ đạo là xanh đá phiến đậm ({colors.primary}) để biểu trưng cho cấu trúc vận hành ổn định, kết hợp với sắc cam đào ({colors.cta-orange}) làm điểm nhấn năng động cho các hành động mua sắm, nạp tiền và kêu gọi hành động (Call to Action).

Thiết kế loại bỏ các hiệu ứng bóng đổ phức tạp hoặc màu sắc lòe loẹt, hướng tới giao diện phẳng sạch sẽ với bán kính bo góc `{rounded.md}` (8px) cho các phần tử nhập liệu, nút bấm và `{rounded.lg}` (12px) cho các tấm thẻ thông tin.

---

## Colors

### Brand & Primary

*   **Dark Slate** ({colors.primary}): Màu thương hiệu chính, sử dụng cho thanh tiêu đề (Header), tiêu đề chính và các nút điều hướng cốt lõi.
*   **Slate Hover** ({colors.primary-hover}): Biến thể đậm hơn khi rê chuột.
*   **Peach Highlight** ({colors.cta-orange}): Màu cam đào nhấn mạnh, dùng làm nút mua nhanh sản phẩm số, xác nhận nạp tiền tự động hoặc nạp ví.

### Neutral & Surface

*   **Canvas White** ({colors.canvas}): Nền chính của trang web và các thẻ nội dung.
*   **Surface Light** ({colors.surface}): Màu xám nhạt nhẹ nhàng làm nền cho phần nội dung hoặc danh sách bộ lọc.
*   **Hairline Slate** ({colors.hairline}): Đường kẻ chia vùng hoặc viền mỏng của bảng dữ liệu.

### Semantic & Status

*   **Success Emerald** ({colors.semantic-success}): Trạng thái đơn hàng hoàn thành, giao dịch nạp tiền thành công, hoặc tài khoản đã duyệt KYC.
*   **Warning Amber** ({colors.semantic-warning}): Trạng thái quỹ đang tạm giữ trong Escrow, lệnh rút tiền đang chờ xử lý.
*   **Error Rose** ({colors.semantic-error}): Trạng thái khiếu nại tranh chấp, từ chối KYC hoặc hủy giao dịch.

---

## Typography

### Font Family

Roboto là font chữ chính thức được áp dụng cho toàn bộ ứng dụng nhằm tối ưu hóa tính dễ đọc và gọn gàng trên cả thiết bị di động lẫn máy tính để bàn.

### Hierarchy

| Token | Size | Weight | Line Height | Use |
|---|---|---|---|---|
| `{typography.hero-display}` | 48px | 700 | 1.15 | Tiêu đề lớn trang chủ chào mừng |
| `{typography.heading-1}` | 30px | 700 | 1.25 | Tiêu đề các trang quản lý chính |
| `{typography.heading-2}` | 24px | 600 | 1.30 | Tiêu đề các mục con, nhóm tính năng |
| `{typography.body-md}` | 16px | 400 | 1.50 | Nội dung văn bản mô tả, thông tin chi tiết |
| `{typography.body-sm}` | 14px | 400 | 1.45 | Bảng dữ liệu, văn bản phụ, chú thích chân trang |
| `{typography.money}` | 16px | 700 | 1.40 | Hiển thị số tiền VNĐ lớn đậm nét |

---

## Elevation & Depth

*   **Level 0 (Flat)**: Không bóng đổ, dùng viền mỏng `{colors.hairline}`. Sử dụng cho hầu hết thẻ danh mục sản phẩm.
*   **Level 1 (Subtle Shadow)**: Dùng cho các thẻ khi hover nhẹ.
*   **Level 2 (Popups & Modals)**: Bóng đổ lan rộng để phân lớp giao diện khi mở hộp thoại xác nhận rút tiền hoặc xem mã code.

---

## Do's and Don'ts

### Do
- Sử dụng sắc cam đào ({colors.cta-orange}) có mục đích cho các nút giao dịch ví và thanh toán đơn hàng.
- Bo góc các ô nhập và nút bấm đúng 8px (`{rounded.md}`).
- Luôn hiển thị trạng thái số dư phân tách giữa khả dụng và tạm giữ Escrow.

### Don't
- Không tự ý thêm các dải màu gradient sặc sỡ phá vỡ tông màu tối giản của Nexus Market.
- Không dùng nút bấm dạng viên thuốc bo tròn 100% (Pill), hãy dùng hình chữ nhật bo góc 8px.
- Không hiển thị thô mã code sản phẩm số chưa giải mã lên màn hình danh sách.

---

## Responsive Behavior

- Giao diện tự động co giãn từ 1280px ở máy tính xuống 1 cột duy nhất dưới 768px (Mobile).
- Thanh menu chính thu gọn thành menu hamburger trên điện thoại.
- Các bảng lịch sử đơn hàng tự động cuộn ngang (overflow-x: auto) để tránh tràn layout.
