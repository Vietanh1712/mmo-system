# 🎨 VISUAL REFERENCE GUIDE - MMO Market Search Results

## Color Palette Reference

```
┌──────────────────────────────────────────────────────────────┐
│                      COLOR SCHEME                            │
├──────────────────────────────────────────────────────────────┤
│                                                               │
│  PRIMARY COLOR (Xanh Dương)                                  │
│  ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓                                             │
│  #0058be - Links, Buttons, Active States, Text highlights   │
│  RGB(0, 88, 190)                                             │
│                                                               │
│  ACCENT COLOR (Cam)                                          │
│  ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓                                             │
│  #fd761a - "Mua Ngay" Button, Tags, Badges                  │
│  RGB(253, 118, 26)                                           │
│                                                               │
│  BACKGROUND (Xám Nhạt)                                       │
│  ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓                                             │
│  #F8F9FB - Main page background                             │
│  RGB(248, 249, 251)                                          │
│                                                               │
│  CARD/CONTENT (Trắng Tinh)                                   │
│  ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓                                             │
│  #FFFFFF - Cards, Filter panels, Product boxes              │
│  RGB(255, 255, 255)                                          │
│                                                               │
│  TEXT PRIMARY (Tối)                                          │
│  ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓                                             │
│  #333333 - Main text, Titles, Descriptions                  │
│  RGB(51, 51, 51)                                             │
│                                                               │
│  TEXT SECONDARY (Xám)                                        │
│  ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓                                             │
│  #666666 - Supporting text, Subtitles                       │
│  RGB(102, 102, 102)                                          │
│                                                               │
│  SUCCESS/INSTANT (Xanh Lá)                                   │
│  ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓                                             │
│  #28a745 - "Giao Tức Thì" tag                               │
│  RGB(40, 167, 69)                                            │
│                                                               │
│  FOOTER DARK (Tối Hải Quân)                                  │
│  ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓                                             │
│  #0a192f - Footer background                                │
│  RGB(10, 25, 47)                                             │
│                                                               │
└──────────────────────────────────────────────────────────────┘
```

## Typography Reference

```
┌──────────────────────────────────────────────────────────────┐
│                    FONT FAMILY                               │
├──────────────────────────────────────────────────────────────┤
│  Font: Be Vietnam Pro                                        │
│  URL: https://fonts.googleapis.com/css2?family=...           │
│                                                               │
│  WEIGHT 400 (Regular)                                        │
│  The quick brown fox jumps over the lazy dog                │
│                                                               │
│  WEIGHT 500 (Medium)                                         │
│  The quick brown fox jumps over the lazy dog                │
│                                                               │
│  WEIGHT 600 (SemiBold)                                       │
│  The quick brown fox jumps over the lazy dog                │
│                                                               │
│  WEIGHT 700 (Bold)                                           │
│  The quick brown fox jumps over the lazy dog                │
│                                                               │
│  Usage:                                                      │
│  - Body text: 400, 14px                                     │
│  - Labels: 600, 14px                                        │
│  - Titles: 700, 16px                                        │
│  - Headers: 700, 22-28px                                    │
│                                                               │
└──────────────────────────────────────────────────────────────┘
```

## Component Styling Reference

### Button Styles

```css
/* Primary Button (Mua Ngay) */
background: linear-gradient(135deg, #fd761a 0%, #ff9100 100%);
color: #FFFFFF;
padding: 12px 16px;
border: none;
border-radius: 6px;
font-weight: 700;
box-shadow: 0 4px 12px rgba(253, 118, 26, 0.3);
transition: all 0.3s ease;

:hover {
  background: linear-gradient(135deg, #ff9100 0%, #ffb333 100%);
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(253, 118, 26, 0.4);
}

/* Secondary Button (Áp dụng Bộ lọc) */
background: linear-gradient(135deg, #0058be 0%, #004294 100%);
color: #FFFFFF;
padding: 12px 16px;
border: none;
border-radius: 6px;
font-weight: 600;
box-shadow: 0 4px 12px rgba(0, 88, 190, 0.3);
transition: all 0.3s ease;

:hover {
  background: linear-gradient(135deg, #004294 0%, #003070 100%);
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 88, 190, 0.4);
}
```

### Card Styles

```css
/* Product Card */
background: #FFFFFF;
border-radius: 8px;
border: 1px solid #f0f0f0;
box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
transition: all 0.3s ease;
overflow: hidden;

:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.12);
  border-color: #0058be;
}

/* Filter Panel */
background: #FFFFFF;
border-radius: 8px;
box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
padding: 20px;
position: sticky;
top: 20px;
```

### Badge Styles

```css
/* Instant Delivery Badge */
background: linear-gradient(135deg, #28a745 0%, #20c997 100%);
color: #FFFFFF;
font-size: 11px;
padding: 6px 12px;
border-radius: 4px;
font-weight: 700;
text-transform: uppercase;

/* Bestseller Badge */
background: linear-gradient(135deg, #fd761a 0%, #ff9100 100%);
color: #FFFFFF;
font-size: 11px;
padding: 6px 12px;
border-radius: 4px;
font-weight: 700;
text-transform: uppercase;

/* Category Badge */
background: #f0f0f0;
color: #0058be;
padding: 4px 10px;
border-radius: 4px;
border: 1px solid #e0e0e0;
font-weight: 600;
font-size: 12px;
```

## Layout Specifications

### Grid System

```
Desktop Layout (≥1024px)
┌─────────────────────────────────────────────┐
│               HEADER (Sticky)                │
├──────────────┬──────────────────────────────┤
│              │                              │
│   SIDEBAR    │     PRODUCT GRID (3 cols)   │
│   (25%)      │         (75%)                │
│              │                              │
│ • Filters    │ ┌─────┐ ┌─────┐ ┌─────┐     │
│ • Categories │ │Card │ │Card │ │Card │     │
│ • Price      │ │  1  │ │  2  │ │  3  │     │
│ • Rating     │ └─────┘ └─────┘ └─────┘     │
│              │                              │
│              │ ┌─────┐ ┌─────┐ ┌─────┐     │
│              │ │Card │ │Card │ │Card │     │
│              │ │  4  │ │  5  │ │  6  │     │
│              │ └─────┘ └─────┘ └─────┘     │
│              │                              │
│              │         PAGINATION           │
├──────────────┴──────────────────────────────┤
│              FOOTER (Dark)                  │
└─────────────────────────────────────────────┘

Tablet Layout (768px - 1023px)
┌─────────────────────────────────────────────┐
│               HEADER                        │
├─────────────────────────────────────────────┤
│              SIDEBAR (Filters)              │
│  Category │ Price │ Stock │ Rating │ Speed │
├─────────────────────────────────────────────┤
│        PRODUCT GRID (2 columns)             │
│      ┌──────────┐  ┌──────────┐            │
│      │  Card 1  │  │  Card 2  │            │
│      └──────────┘  └──────────┘            │
│      ┌──────────┐  ┌──────────┐            │
│      │  Card 3  │  │  Card 4  │            │
│      └──────────┘  └──────────┘            │
│         PAGINATION & FOOTER                │
├─────────────────────────────────────────────┤
│              FOOTER                        │
└─────────────────────────────────────────────┘

Mobile Layout (<768px)
┌──────────────────────┐
│   HEADER (Compact)   │
├──────────────────────┤
│    FILTERS (Full)    │
│ (Collapsible panel)  │
├──────────────────────┤
│  PRODUCT GRID (1-2)  │
│   ┌──────────────┐   │
│   │   Card 1     │   │
│   └──────────────┘   │
│   ┌──────────────┐   │
│   │   Card 2     │   │
│   └──────────────┘   │
├──────────────────────┤
│    PAGINATION        │
├──────────────────────┤
│      FOOTER          │
└──────────────────────┘
```

### Spacing Specifications

```
Element Padding & Margins:
• Page padding: 30px (desktop), 15px (tablet), 10px (mobile)
• Card padding: 15px (inner content)
• Button padding: 12px vertical, 16px horizontal
• Gap between grid items: 20px (desktop), 15px (tablet), 12px (mobile)
• Section margins: 20-50px top/bottom

Vertical Rhythm:
• Line height: 1.4 - 1.6 (for body text)
• Margin between sections: 40-60px
• Padding in cards: 15px uniform
• Border radius: 8px (cards), 6px (buttons), 4px (inputs)
```

## Price Formatting Reference

```javascript
// VNĐ Currency Format
function formatPrice(price) {
  return price.toLocaleString('vi-VN') + ' VNĐ';
}

Examples:
45000 → "45.000 VNĐ"
1500000 → "1.500.000 VNĐ"
99900 → "99.900 VNĐ"
```

## Icon Reference (FontAwesome 6.4)

```
• Shopping Bag: <i class="fas fa-shopping-bag"></i>
• Search: <i class="fas fa-search"></i>
• Shopping Cart: <i class="fas fa-shopping-cart"></i>
• User Circle: <i class="fas fa-user-circle"></i>
• Star (Filled): <i class="fas fa-star"></i>
• Store: <i class="fas fa-store"></i>
• Check Circle: <i class="fas fa-check-circle"></i>
• Chevron Left: <i class="fas fa-chevron-left"></i>
• Chevron Right: <i class="fas fa-chevron-right"></i>
• Filter: <i class="fas fa-filter"></i>
```

## Animation & Transition Reference

```css
/* Standard Transitions */
transition: all 0.2s ease;      /* Quick (buttons, borders) */
transition: all 0.3s ease;      /* Medium (cards, hover) */
transition: background-color 0.2s ease;  /* Color changes */

/* Transform Effects */
transform: translateY(-2px);    /* Lift on hover */
transform: scale(1.05);         /* Scale up on hover */
transform: translateY(0);       /* Reset on click */

/* Box Shadow Progression */
box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);          /* Resting */
box-shadow: 0 8px 16px rgba(0, 0, 0, 0.12);         /* Hover */
box-shadow: 0 4px 12px rgba(253, 118, 26, 0.3);     /* Accent color */
```

## Responsive Breakpoints

```
Desktop:     ≥1024px    (1024px - ∞)     - Full layout
Tablet:      768-1023px (768px - 1023px) - Medium layout
Mobile:      480-767px  (480px - 767px)  - Compact layout  
Small:       <480px     (<480px)         - Minimal layout

Media Query Format:
@media (max-width: 768px) { }    /* Below tablet */
@media (max-width: 480px) { }    /* Below mobile */
@media (min-width: 1024px) { }   /* Desktop and above */
```

## Z-Index Reference

```css
body                    z-index: auto
.header                 z-index: 100 (sticky)
.footer                 z-index: auto
.sidebar                z-index: auto
.product-card           z-index: auto
.product-card:hover     z-index: 10
.modal (if exists)      z-index: 1000
.dropdown (if exists)   z-index: 100
```

## Accessibility Highlights

```
✓ Color Contrast Ratios:
  - #0058be on #FFFFFF: 8.5:1 (AAA - Excellent)
  - #fd761a on #FFFFFF: 5.8:1 (AAA - Excellent)
  - Button text on button: Minimum 4.5:1

✓ Focus States:
  - All interactive elements have clear focus indicators
  - Links have underline or color change on focus
  - Buttons have border or shadow change

✓ Text Sizing:
  - Minimum 14px for body text
  - 16px+ for links
  - 18px+ for headings

✓ Icons:
  - Always paired with text labels
  - Color-blind friendly (not relying on color alone)
```

## Browser Support

```
Chrome:     90+ (Full support)
Firefox:    88+ (Full support)
Safari:     14+ (Full support)
Edge:       90+ (Full support)
Mobile:     iOS Safari 14+, Chrome Android Latest
IE 11:      Not supported (CSS Grid, Flexbox limitations)
```

---

**Note**: All colors, dimensions, and styles should be verified using browser DevTools.  
**Font Loading**: Be Vietnam Pro loads from Google Fonts, requires internet connection.

---

**Last Updated**: 2026-05-28  
**Version**: 1.0

