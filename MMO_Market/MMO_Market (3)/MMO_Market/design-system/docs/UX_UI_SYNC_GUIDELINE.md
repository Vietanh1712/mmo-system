# UX/UI Sync Guideline

Tài liệu này mô tả các CSS mới dùng để đồng bộ UX/UI trong MMO Market.

## Thứ tự import khuyến nghị

```html
<link rel="stylesheet" th:href="@{/css/design-system.css}">
<link rel="stylesheet" th:href="@{/css/layout.css}">
<link rel="stylesheet" th:href="@{/css/typography.css}">
<link rel="stylesheet" th:href="@{/css/avatars.css}">
<link rel="stylesheet" th:href="@{/css/badges.css}">
<link rel="stylesheet" th:href="@{/css/buttons.css}">
<link rel="stylesheet" th:href="@{/css/forms.css}">
<link rel="stylesheet" th:href="@{/css/filters.css}">
<link rel="stylesheet" th:href="@{/css/dropdown.css}">
<link rel="stylesheet" th:href="@{/css/tables.css}">
<link rel="stylesheet" th:href="@{/css/pagination.css}">
<link rel="stylesheet" th:href="@{/css/icons.css}">
<link rel="stylesheet" th:href="@{/css/alerts.css}">
<link rel="stylesheet" th:href="@{/css/tabs.css}">
<link rel="stylesheet" th:href="@{/css/states.css}">
<link rel="stylesheet" th:href="@{/css/modal.css}">
<link rel="stylesheet" th:href="@{/css/toast.css}">
<link rel="stylesheet" th:href="@{/css/toggle.css}">
<link rel="stylesheet" th:href="@{/css/utilities.css}">
```

Trong Thymeleaf nên dùng fragment:

```html
<th:block th:replace="~{fragments/design-system-styles :: designSystemStyles}"></th:block>
```

## Layout

- `.ds-container`: container page chuẩn, giới hạn chiều rộng.
- `.ds-container-fluid`: container full width.
- `.ds-page-shell`: layout page dạng grid có gap chuẩn.
- `.ds-two-column`: layout sidebar + content.
- `.ds-section`: nhóm nội dung theo chiều dọc.
- `.ds-stack-xs`, `.ds-stack-sm`, `.ds-stack-md`, `.ds-stack-lg`: spacing dọc.
- `.ds-cluster`, `.ds-cluster-between`: hàng ngang có wrap.

## Typography

- `.ds-heading-xl`, `.ds-heading-lg`, `.ds-heading-md`: tiêu đề.
- `.ds-body`: đoạn nội dung chính.
- `.ds-caption`: mô tả phụ, helper text, meta.
- `.ds-money`: tiền VNĐ hoặc số dư ví.
- `.ds-link`: link text đồng bộ màu primary.

## Avatar

- Base: `.ds-avatar`.
- Size: `.ds-avatar-sm`, `.ds-avatar-md`, `.ds-avatar-lg`.
- Tone: `.ds-avatar-success`, `.ds-avatar-primary`.

Ví dụ:

```html
<span class="ds-avatar ds-avatar-sm ds-avatar-success">TE</span>
```

## Badge

Dùng cho trạng thái nghiệp vụ.

- `.ds-badge-success`: hoạt động, hoàn tất.
- `.ds-badge-warning`: chờ duyệt, đang giữ tiền.
- `.ds-badge-danger`: khóa, thất bại, bị từ chối.
- `.ds-badge-info`: đang xử lý.
- `.ds-badge-muted`: bản nháp, chưa kích hoạt.

## Alert và Toast

- `.ds-alert-*`: thông báo nằm trong page.
- `.ds-toast-*`: thông báo nổi theo hành động JavaScript.

Không dùng toast để thay thế trạng thái cố định trên màn hình.

## Menu và Filter Dropdown

- `.ds-menu`: menu đơn giản như account menu hoặc action menu.
- `.ds-dropdown`: chỉ dùng cho filter/search dropdown trong `filters.css`.

Không trộn hai nhóm này để tránh conflict layout.

## Modal

Dùng các class:

- `.ds-modal-backdrop`
- `.ds-modal`
- `.ds-modal-header`
- `.ds-modal-body`
- `.ds-modal-footer`

Modal chỉ là shell UI. Logic đóng/mở, focus trap, submit vẫn cần JavaScript riêng.

## States

- `.ds-empty-state`: danh sách rỗng.
- `.ds-loading`: khóa tương tác khi đang gọi API.
- `.ds-skeleton`, `.ds-skeleton-line`: loading placeholder.
- `.ds-error-text`, `.ds-success-text`: text trạng thái nhỏ.

## Utilities

`utilities.css` chỉ chứa helper nhỏ:

- `.ds-hidden`
- `.ds-text-left`, `.ds-text-center`, `.ds-text-right`
- `.ds-w-full`
- `.ds-mt-sm`, `.ds-mt-md`, `.ds-mt-lg`

Nếu một pattern lặp lại nhiều lần, không thêm utility mới tùy tiện. Hãy tạo component class rõ nghĩa.

## Example

Mở file sau để xem trực quan:

```text
docs/design-system/examples/ux-ui-sync-proposal-example.html
```
