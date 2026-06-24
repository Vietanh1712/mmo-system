# Changelog

Tài liệu ghi nhận thay đổi đáng chú ý theo hướng người dùng và kiến trúc. Chi tiết triển khai cũ nằm tại [Implementation History](docs/changes/implementation-history.md).

## Unreleased

### Documentation

- Chuẩn hóa tài liệu về `README.md`, `AGENTS.md`, `CHANGELOG.md` và `docs/`.
- Tách tài liệu sống khỏi báo cáo lịch sử trong `docs/archive/`.
- Chuẩn hóa tên file Markdown về lowercase kebab-case.

### Frontend

- Đồng bộ Account Sidebar và các màn Account & Wallet.
- Thêm luồng đăng ký/đóng Shop dạng frontend.
- Bổ sung Notification Center và các màn Seller/Staff.

### Backend

- Bổ sung các API Seller và mô hình dữ liệu liên quan.
- Bổ sung tìm kiếm sản phẩm, nạp tiền SePay và quản lý tài khoản Admin.

## 2026-06-04

- Triển khai và sửa lỗi module Search.
- Sửa lỗi import Stream và các xử lý DTO null-safe.
- Bổ sung thay đổi hồ sơ người dùng trong Admin.

## Archive

- [Search implementation archive](docs/archive/2026-06-search/)
- [Compilation fix archive](docs/archive/2026-06-fixes/)
