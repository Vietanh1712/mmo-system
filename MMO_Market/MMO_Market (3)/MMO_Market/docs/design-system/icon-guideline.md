---
title: Icon Guideline
status: active
owner: Frontend Team
last_updated: 2026-06-18
---

# Icon Guideline

## Nguồn icon

- Font Awesome 4.7 đang được dùng rộng rãi.
- Inline SVG dùng cho action icon cần nét chính xác.
- CSS mask `.ds-icon-*` dùng cho icon tái sử dụng.

Không trộn nhiều phong cách icon trong cùng một nhóm action.

## Icon dùng chung

```html
<span class="ds-icon ds-icon-sort"></span>
<span class="ds-icon ds-icon-cloud-upload"></span>
<span class="ds-icon ds-icon-flag"></span>
<span class="ds-icon ds-icon-bell"></span>
```

Icon lấy màu từ `currentColor`.

## Action Icon

- Eye: xem chi tiết.
- Pencil: chỉnh sửa.
- Trash: xóa/soft delete.
- Bell: thông báo.
- Flag: cảnh báo vi phạm.
- Cloud upload: tải file.
- Sort arrows: sắp xếp.

## Quy tắc

- Icon-only button phải có `aria-label` và `title` khi cần.
- Không dùng icon xóa cho hành động khóa hoặc đóng Shop.
- Action nguy hiểm dùng màu danger và confirmation phù hợp.
- Kích thước icon trong cùng table/menu phải đồng nhất.
- Không hardcode SVG lặp lại nếu đã có class dùng chung.

Xem [Icons Example](examples/icons-example.html) và [Buttons Catalog](examples/buttons-icons-catalog.html).
