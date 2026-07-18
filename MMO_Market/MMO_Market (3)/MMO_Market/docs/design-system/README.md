---
title: Design System
status: active
owner: Frontend Team
last_updated: 2026-06-18
---

# Design System

Design System là nguồn chuẩn bắt buộc cho UI dùng chung của MMO Market. Mọi UI
mới và mọi phần UI hiện có được chỉnh sửa phải tái sử dụng Design System trước
khi tạo pattern riêng.

## Tài liệu

- [UI Guideline](ui-guideline.md)
- [UX/UI Sync Guideline](ux-ui-sync-guideline.md)
- [Icon Guideline](icon-guideline.md)
- [Export Report](export-report.md)

## Examples

Các example HTML nằm trong [`examples/`](examples/):

- Button và icon
- Form
- Filter/search/sort
- Custom Datepicker
- Pagination
- Table
- Toast
- Toggle

## Quy tắc

- Font mặc định: Roboto.
- Bắt buộc kiểm tra và tái sử dụng class `ds-*`, token, fragment và JavaScript
  component dùng chung trước khi tạo class/component mới.
- Thymeleaf page có UI phải import fragment `design-system-styles`; CSS riêng
  của page chỉ bổ sung phần đặc thù và được import sau Design System.
- Không hardcode giá trị trình bày nếu token hoặc component tương ứng đã có.
- Dùng Custom Datepicker chung cho ngày đơn và date range.
- Search chỉ thực thi khi Enter hoặc bấm nút, trừ trường hợp autocomplete.
- Không nhúng style lớn trực tiếp trong template nếu có thể tái sử dụng.
- Trạng thái hover, focus, disabled, loading, empty và error phải nhất quán.
- UI phải đáp ứng responsive, keyboard navigation và accessible name phù hợp.
- Trường hợp cần lệch Design System phải ghi rõ lý do trong mô tả thay đổi và
  cập nhật Design System nếu pattern mới có khả năng tái sử dụng.
