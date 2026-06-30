---
title: UX/UI Sync Guideline
status: active
owner: Frontend Team
last_updated: 2026-06-18
---

# UX/UI Sync Guideline

## Import

Thymeleaf page có UI bắt buộc dùng:

```html
<th:block th:replace="~{fragments/design-system-styles :: designSystemStyles}"></th:block>
```

Page-specific CSS được import sau Design System.

## Layout

- Dùng container và page shell chung.
- Sidebar dùng fragment theo phân hệ.
- Card cùng cấp phải có spacing, radius và shadow nhất quán.
- Table rộng được ưu tiên mở rộng content area trước khi thêm horizontal scroll.

## Navigation

- Header giữ cấu trúc thống nhất trên toàn hệ thống.
- Menu chỉ hiển thị theo role để hỗ trợ UX; backend vẫn phải kiểm tra RBAC.
- Mỗi màn hình có đường vào và đường quay lại rõ ràng.
- Không tạo hai màn hình có cùng mục đích nếu có thể hợp nhất bằng tab/state.

## Form And Filter

- Label luôn gắn với input.
- Search chạy khi Enter hoặc bấm “Tìm kiếm”.
- “Làm mới” đưa filter về mặc định.
- Date range dùng hai Custom Datepicker.
- Action filter đặt cuối hàng và căn phải.
- Success message tự ẩn; error cố định cho đến khi người dùng sửa hoặc retry.

## Table

- Có STT khi bảng cần đối chiếu nhanh.
- Header, badge trạng thái và action icon dùng class chung.
- Có loading, empty và error state.
- Pagination chỉ hiện khi có dữ liệu.

## Responsive

- Desktop: giữ đầy đủ thông tin chính.
- Tablet: giảm số cột hoặc chuyển metadata xuống dòng.
- Mobile: ưu tiên card/list; action vẫn phải truy cập được.

## Accessibility

- Focus state rõ ràng.
- Không chỉ dùng màu để truyền đạt trạng thái.
- Button icon có accessible name.
- Modal quản lý focus và hỗ trợ Escape khi có JavaScript tương ứng.

## Review Checklist

- [ ] Đã kiểm tra component, token, fragment và example hiện có trước khi tạo mới.
- [ ] Page import `design-system-styles` trước CSS riêng.
- [ ] Dùng Roboto và token chung.
- [ ] Không trùng component đã có.
- [ ] Không hardcode pattern trình bày đã có trong Design System.
- [ ] Hover/focus/disabled/loading đầy đủ.
- [ ] Navigation đến và đi hợp lý.
- [ ] Responsive.
- [ ] Keyboard navigation và accessible name phù hợp.
- [ ] Không dùng mock mà không ghi chú rõ.
