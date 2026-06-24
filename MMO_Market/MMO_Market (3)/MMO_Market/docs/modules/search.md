---
title: Search Module
status: active
owner: Customer Shopping Team
last_updated: 2026-06-18
---

# Search Module

## Mục tiêu

Cho phép Guest và người dùng đã đăng nhập tìm, lọc và mở chi tiết sản phẩm số.

## Routes

### MVC

- `GET /search`: trang kết quả tìm kiếm.
- `GET /products`: danh mục sản phẩm.
- `GET /products/{productId}`: chi tiết sản phẩm.
- `GET /shop/{sellerId}`: Shop công khai.

### API

- `GET /api/search/products`
- `GET /api/search/products/featured`
- `GET /api/search/products/{productId}`
- `GET /api/search/products/{productId}/reviews`
- `GET /api/search/categories`
- `GET /api/search/seller/{sellerId}`
- `POST /api/search/seller/{sellerId}/follow`

Tham số filter phải được xác nhận từ `ProductSearchController` và `ProductSearchService` trước khi thay đổi frontend.

## Flow

1. Người dùng nhập keyword trên header.
2. Search chỉ chạy khi Enter hoặc bấm nút tìm.
3. Browser điều hướng tới `/search` với query parameters.
4. Frontend gọi `/api/search/products`.
5. API áp dụng filter, sort và pagination.
6. Card dẫn tới Product Detail.

Autocomplete là hành vi riêng và có thể cập nhật khi người dùng gõ.

## Data Rules

- Chỉ trả sản phẩm `isDelete = 0`.
- Không hiển thị Shop không được phép hoạt động.
- Giá lấy từ ProductVariant còn hiệu lực.
- Search không làm thay đổi stock hoặc số dư.
- Featured products dùng dữ liệu bán hàng thực tế khi có.

## UI

- Header giữ search ở giữa và cart/avatar cùng hàng.
- Filter dùng Design System.
- Search, reset, sort và pagination phải đồng bộ example.
- Trạng thái loading, empty và error phải rõ ràng.
- Font dùng Roboto.

## Security

- Search và Product Detail có thể public.
- Cart, checkout, follow hoặc hành động cá nhân phải kiểm tra đăng nhập.
- Không tin seller/product ID từ frontend khi thực hiện nghiệp vụ.

## Test Checklist

- Keyword rỗng.
- Keyword có dấu/không dấu và ký tự đặc biệt.
- Không có kết quả.
- Category/price/stock/rating filter.
- Sort và pagination.
- Product bị soft delete.
- Seller bị khóa.
- API lỗi hoặc timeout.
- Responsive desktop/tablet/mobile.

## Historical Documents

Các báo cáo triển khai Search tháng 6/2026 nằm trong [`../archive/2026-06-search/`](../archive/2026-06-search/). Chúng chỉ dùng tra cứu lịch sử.
