---
title: Implementation History
status: active
owner: Engineering Team
last_updated: 2026-06-18
---

# Implementation History

Tài liệu này ghi nhận lịch sử kỹ thuật. Đây không phải business specification hoặc database source of truth.

## 2026-06-18 - Documentation Standardization

- Tạo `README.md`, `AGENTS.md`, `CHANGELOG.md`.
- Chuẩn hóa tài liệu sống vào `docs/`.
- Chuyển báo cáo Search và sửa lỗi cũ vào archive.
- Xóa bản Markdown trùng lặp.
- Chuẩn hóa link và tên file.

## 2026-06 - Account And Wallet Frontend

- Đồng bộ Account Sidebar.
- Hoàn thiện giao diện Profile, KYC, Security, Wallet, Top-up, Transaction History, Orders và Notifications.
- Dùng Design System, Custom Datepicker, table và pagination dùng chung.
- Thêm Register Shop và Close Shop frontend.
- Hạn chế Register Shop theo role ở frontend.

## 2026-06 - Seller And Staff Merge

- Thêm Seller API/prototype cho dashboard, product, variant, transaction, withdrawal, complaint và digital asset.
- Thêm Staff Transaction list/detail dạng frontend prototype.
- Giữ header search/cart/notification khi resolve merge.
- Sửa native query sản phẩm bán chạy tương thích SQL Server.

## 2026-06 - Notification Frontend

- Thêm Admin notification management.
- Thêm notification bell và Notification Center.
- Broadcast hiện dùng browser storage, chưa có API/database production.

## 2026-06-04 - Search And Java Fixes

- Sửa import Stream và DTO null-safe.
- Thêm Search Results, filter và category hierarchy prototype.
- Chi tiết lịch sử nằm trong:
  - [Compilation fix](../archive/2026-06-fixes/compilation-error-fixed.md)
  - [Java fix final](../archive/2026-06-fixes/java-error-fixed-final.md)
  - [Search archive](../archive/2026-06-search/)

## Known Technical Debt

- RBAC MVC chưa hoàn chỉnh.
- Một số module chưa có Service layer đúng chuẩn.
- Secret/config nhạy cảm cần chuyển sang environment variables và rotate.
- `ddl-auto=update` cần thay bằng migration + `validate`.
- Notification, Staff Transaction và một số Admin/Seller flow vẫn là frontend mock.
- Build artifact `target/classes` không nên được theo dõi lâu dài.
