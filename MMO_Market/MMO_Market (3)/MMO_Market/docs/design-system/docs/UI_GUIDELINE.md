# MMO Market Exported UI Design System

Bộ export này được tạo để copy sang project `MMO_System` dùng Spring Boot/IntelliJ. CSS độc lập, không phụ thuộc ASP.NET Core Razor, Tailwind runtime, hay Tag Helper.

## Component đã export

- Design tokens: màu, font, border, radius, shadow.
- Layout foundation: container, page shell, two-column layout, stack, cluster.
- Typography: heading, body, caption, money text, link.
- Avatar: size và tone dùng chung.
- Business badges: success, warning, danger, info, muted.
- Buttons: primary, secondary/reset filter, danger, outline, success.
- Icons/action buttons: view/edit, delete, reset/refresh.
- Forms: input, select, textarea, custom datepicker, radio, validation text.
- Filter/search/dropdown/date range bằng custom datepicker.
- Tables: header, row hover, avatar/entity cell, badge, action cell.
- Pagination.
- Simple menu: menu item, divider, danger item.
- Alerts: info, warning, danger, success.
- Tabs: active/selected tab.
- States: empty, loading, skeleton, error/success text.
- Modal: backdrop, shell, header/body/footer.
- Toggle/switch trạng thái.
- Toast notification stacked.
- Utilities: hidden, text align, width, margin-top helpers.

## Copy sang Spring Boot

Copy thư mục `exported-design-system/css` sang:

```text
src/main/resources/static/css/
```

Nếu dùng Thymeleaf, import CSS:

```html
<link rel="stylesheet" th:href="@{/css/design-system.css}">
<link rel="stylesheet" th:href="@{/css/layout.css}">
<link rel="stylesheet" th:href="@{/css/typography.css}">
<link rel="stylesheet" th:href="@{/css/avatars.css}">
<link rel="stylesheet" th:href="@{/css/badges.css}">
<link rel="stylesheet" th:href="@{/css/buttons.css}">
<link rel="stylesheet" th:href="@{/css/forms.css}">
<link rel="stylesheet" th:href="@{/css/datepicker.css}">
<link rel="stylesheet" th:href="@{/css/filters.css}">
<link rel="stylesheet" th:href="@{/css/dropdown.css}">
<link rel="stylesheet" th:href="@{/css/tables.css}">
<link rel="stylesheet" th:href="@{/css/pagination.css}">
<link rel="stylesheet" th:href="@{/css/icons.css}">
<link rel="stylesheet" th:href="@{/css/alerts.css}">
<link rel="stylesheet" th:href="@{/css/tabs.css}">
<link rel="stylesheet" th:href="@{/css/states.css}">
<link rel="stylesheet" th:href="@{/css/modal.css}">
<link rel="stylesheet" th:href="@{/css/toggle.css}">
<link rel="stylesheet" th:href="@{/css/toast.css}">
<link rel="stylesheet" th:href="@{/css/utilities.css}">
```

Nếu dùng JSP:

```html
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/design-system.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/buttons.css">
```

## Quy định sử dụng

- Dùng class prefix `.ds-` cho UI trong MMO.
- Không dùng inline style nếu đã có class export tương ứng.
- Icon dùng chung nằm trong `css/icons.css`, gọi bằng `<span class="ds-icon ds-icon-bell"></span>`.
- Dùng `.ds-container`, `.ds-page-shell`, `.ds-section`, `.ds-stack-*`, `.ds-cluster` để căn layout thay vì tự thêm margin/gap rời rạc.
- Dùng `.ds-badge-*` cho trạng thái nghiệp vụ như seller, product, order, transaction, complaint.
- Dùng `.ds-alert-*` cho thông báo nằm trong page; dùng `.ds-toast-*` cho thông báo nổi.
- Dùng `.ds-menu` cho menu đơn giản như account menu; `.ds-dropdown` trong `filters.css` chỉ dùng cho filter/search dropdown.
- Dùng duy nhất custom datepicker `.ds-datepicker` cho chọn ngày. Ngày đơn dùng 1 datepicker; date range dùng 2 datepicker `dateFrom/dateTo`.
- Modal dùng `.ds-modal-backdrop`, `.ds-modal`, `.ds-modal-header`, `.ds-modal-body`, `.ds-modal-footer`.
- Utility trong `utilities.css` chỉ dùng cho chỉnh nhỏ; nếu layout lặp lại nhiều lần thì phải đưa về component class riêng.
- Logic như phân trang, filter, dropdown search, toast stacking cần viết lại bằng JavaScript/Thymeleaf/Spring Controller của MMO. Datepicker dùng `js/datepicker.js`.
- Các file HTML trong `examples/` mở trực tiếp bằng browser để xem nhanh hình dạng component.

## Example nên xem

- `examples/ux-ui-sync-proposal-example.html`: preview toàn bộ nhóm layout, typography, avatar, badge, alert, tab, state, menu, modal.
- `examples/buttons-icons-catalog.html`: toàn bộ button và icon hiện có.
- `examples/table-example.html`: bảng có avatar, badge, icon action view/delete.
- `examples/date-picker-comparison-example.html`: chuẩn custom datepicker cho ngày đơn và khoảng ngày filter.


