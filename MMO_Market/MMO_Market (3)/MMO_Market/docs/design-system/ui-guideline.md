---
title: UI Guideline
status: active
owner: Frontend Team
last_updated: 2026-06-18
---

# UI Guideline

## Nền tảng

- Font: Roboto.
- CSS dùng chung nằm trong `src/main/resources/static/css/`.
- Component dùng prefix `.ds-`.
- Thymeleaf page có UI phải import fragment `fragments/design-system-styles`.

## Component

- Layout: `.ds-container`, `.ds-page-shell`, `.ds-two-column`, `.ds-stack-*`, `.ds-cluster-*`.
- Typography: `.ds-heading-*`, `.ds-body`, `.ds-caption`, `.ds-money`.
- Button: `.ds-btn` kết hợp primary, secondary, outline, danger hoặc success.
- Form: `.ds-input`, `.ds-select`, `.ds-textarea`, validation text.
- Date: chỉ dùng Custom Datepicker và `js/datepicker.js`.
- Datepicker và dropdown dùng overlay portal chung; không đặt panel thủ công trong
  card/table hoặc hạ `z-index`, vì component đã tự căn viewport và tránh clipping.
- Table: `.ds-table`, entity cell, badge và action icon.
- Pagination: `.ds-pagination`, `.ds-page-link`.
- Feedback: alert cho trạng thái cố định, toast cho phản hồi tạm thời.
- State: loading, skeleton, empty, error và disabled.
- Modal: backdrop, header, body và footer dùng class chung.

## Quy tắc

- Kiểm tra catalog/example và dùng CSS/component chung trước khi tạo class mới.
- Chỉ tạo page-specific class cho layout hoặc hành vi thực sự đặc thù.
- Không hardcode màu, spacing, font, radius hoặc shadow đã có token tương ứng.
- Không dùng inline style cho pattern lặp lại.
- Không tạo nhiều biến thể giống nhau chỉ khác tên.
- Search/filter chỉ chạy khi Enter hoặc bấm nút, trừ autocomplete.
- Form phải có label, focus, validation và disabled state.
- Icon-only button phải có `aria-label`.
- Màu trạng thái phải giữ cùng ngữ nghĩa giữa các màn.
- Pattern mới có thể tái sử dụng phải được đưa về Design System thay vì sao
  chép giữa nhiều page.

## Examples

- [Buttons and icons](examples/buttons-icons-catalog.html)
- [Forms](examples/forms-example.html)
- [Filter, search and sort](examples/filter-search-sort-example.html)
- [Custom Datepicker](examples/date-picker-comparison-example.html)
- [Table](examples/table-example.html)
- [Pagination](examples/pagination-example.html)
- [Toast](examples/toast-example.html)
- [Toggle](examples/toggle-example.html)
- [Full proposal](examples/ux-ui-sync-proposal-example.html)
