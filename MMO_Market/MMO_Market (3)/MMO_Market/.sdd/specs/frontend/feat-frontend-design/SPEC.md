# SPEC — UI/UX Design System
> **Feature ID:** `feat-frontend-design`
> **Version:** 1.0 | **Status:** Implemented
> **Author:** Team | **Last Updated:** 2026-06-27

---

## 1. TỔNG QUAN HỆ THỐNG THIẾT KẾ (DESIGN SYSTEM)
Toàn bộ phong cách thiết kế UI/UX của MMO Market được quy định tập trung bằng các biến CSS variables toàn cục, đảm bảo trải nghiệm thống nhất giữa các trang.

---

## 2. DANH SÁCH FILE CSS THỰC TẾ
```
apps/frontend/static/css/
├── variables.css           ← Định nghĩa mã màu Slate/Indigo, font chữ Outfit
├── layout.css              ← Cấu trúc grid, flexbox, container chính
├── components/
│   ├── buttons.css         ← Các nút bấm premium, hiệu ứng hover
│   ├── modals.css          ← Hộp thoại gương mờ (Glassmorphism)
│   ├── tables.css          ← Bảng danh sách tối ưu khoảng cách
│   └── toasts.css          ← Khung hiển thị thông báo góc màn hình
├── seller-console.css      ← CSS chuyên dụng cho Seller Console
└── admin-console.css       ← CSS chuyên dụng cho Admin Console
```