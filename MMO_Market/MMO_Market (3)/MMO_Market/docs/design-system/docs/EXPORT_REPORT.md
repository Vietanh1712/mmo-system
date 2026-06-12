# Export Report

## File đã quét

- `wwwroot/css/site.css`
- `wwwroot/css/theme.css`
- `wwwroot/css/teacher.css`
- `wwwroot/js/site.js`
- `Pages/Shared/_AdminLayout.cshtml`
- `Pages/Shared/_TeacherLayout.cshtml`
- `Pages/Login.cshtml`
- `Pages/AdminDashboard.cshtml`
- `Pages/AdminClasses.cshtml`
- `Pages/AdminCourses.cshtml`
- `Pages/AdminTeachers.cshtml`
- `Pages/AdminTeachers/Create.cshtml`
- `Pages/AdminTeachers/Edit.cshtml`
- `Pages/AdminStudents.cshtml`
- `Pages/AdminStudents/Create.cshtml`
- `Pages/AdminStudents/Edit.cshtml`
- `Pages/AdminStudents/ParentEdit.cshtml`
- `Pages/AdminFinance.cshtml`
- `Pages/AdminSettings.cshtml`
- `Pages/Attendance.cshtml`
- `Pages/Grades.cshtml`
- `Pages/Homework.cshtml`
- `Pages/Messages.cshtml`
- `Pages/Teacher/*.cshtml`

Không có thư mục `Views/` trong project hiện tại.

## Component tìm thấy và đã export

| Component | File export | Class gốc MMO Market | Class export |
| --- | --- | --- | --- |
| Design tokens | `css/design-system.css` | `:root` trong `site.css`, `theme.css`; Tailwind utility admin | `:root --ds-*` |
| Layout foundation | `css/layout.css` | các layout page/profile/admin lặp lại | `.ds-container`, `.ds-page-shell`, `.ds-two-column`, `.ds-section`, `.ds-stack-*`, `.ds-cluster` |
| Typography | `css/typography.css` | title/subtitle/body/money text rải rác | `.ds-heading-*`, `.ds-body`, `.ds-caption`, `.ds-money`, `.ds-link` |
| Avatar | `css/avatars.css` | table/profile/header avatar | `.ds-avatar`, `.ds-avatar-*`, `.ds-avatar-success`, `.ds-avatar-primary` |
| Business badge | `css/badges.css` | trạng thái user/seller/product/order | `.ds-badge`, `.ds-badge-*` |
| Card/panel | `css/design-system.css` | `rounded-2xl border bg-white shadow-sm`, `.dashboard-card`, `.stat-card` | `.ds-card`, `.ds-card-header` |
| Primary button | `css/buttons.css` | `.btn-primary`, `bg-blue-600 hover:bg-blue-700` | `.ds-btn .ds-btn-primary` |
| Secondary/reset button | `css/buttons.css` | `bg-slate-600 hover:bg-slate-700` | `.ds-btn .ds-btn-secondary` |
| Danger button | `css/buttons.css` | `bg-red-600 hover:bg-red-700` | `.ds-btn .ds-btn-danger` |
| Outline/cancel button | `css/buttons.css` | `border border-gray-300 bg-white` | `.ds-btn .ds-btn-outline` |
| Success button | `css/buttons.css` | `.btn-primary-green`, `.btn-save` | `.ds-btn .ds-btn-success` |
| Form controls | `css/forms.css` | `.form-control`, admin `input/select/textarea` utilities | `.ds-input`, `.ds-select`, `.ds-textarea` |
| Custom datepicker | `css/datepicker.css`, `js/datepicker.js` | generated picker + date fields | `.ds-datepicker-*`, `data-ds-datepicker` |
| Filter/search/dropdown | `css/filters.css` | `[data-search-dropdown]`, `[data-dropdown-toggle]` | `.ds-filter-*`, `.ds-dropdown-*` |
| Simple menu | `css/dropdown.css` | account/action menu pattern | `.ds-menu`, `.ds-menu-item`, `.ds-menu-divider` |
| Date range filter | `css/datepicker.css`, `js/datepicker.js` | 2 custom datepickers from/to | `.ds-datepicker-*` |
| Table | `css/tables.css` | admin tables using `w-full border-collapse`, `.attendance-table`, `.grades-table` | `.ds-table-*` |
| Pagination | `css/pagination.css` | Admin pagination block | `.ds-pagination-*` |
| Alert | `css/alerts.css` | inline success/warning/error message pattern | `.ds-alert`, `.ds-alert-*` |
| Tabs | `css/tabs.css` | profile/dashboard tab pattern | `.ds-tabs`, `.ds-tab`, `.ds-tab-active` |
| States | `css/states.css` | empty/loading/error/skeleton states | `.ds-empty-state`, `.ds-loading`, `.ds-skeleton`, `.ds-error-text`, `.ds-success-text` |
| Modal | `css/modal.css` | confirm dialog/modal shell pattern | `.ds-modal-*` |
| Toggle | `css/toggle.css` | status toggle button with track/knob | `.ds-toggle-*` |
| Toast | `css/toast.css` | `layout-toast-container`, login/admin/finance/settings toast | `.ds-toast-*` |
| Action icon buttons | `css/icons.css` | view/delete/reset icon buttons | `.ds-icon-btn-*` |
| Utilities | `css/utilities.css` | helper class nhỏ | `.ds-hidden`, `.ds-text-*`, `.ds-w-full`, `.ds-mt-*` |

## Component không tìm thấy

- Sort UI reusable: chưa thấy sort arrow hoặc sort header component rõ ràng.
- Filter icon/funnel riêng: chưa thấy icon filter riêng, chỉ có filter bằng input/dropdown.
- CSS icon font riêng: chưa thấy Font Awesome/Bootstrap Icons được dùng làm nguồn chính.

## Ghi chú copy sang MMO

Có thể copy ngay:

- `design-system.css`
- `layout.css`
- `typography.css`
- `avatars.css`
- `badges.css`
- `buttons.css`
- `forms.css`
- `datepicker.css`
- `filters.css`
- `dropdown.css`
- `tables.css`
- `pagination.css`
- `alerts.css`
- `tabs.css`
- `states.css`
- `modal.css`
- `toggle.css`
- `toast.css`
- `icons.css`
- `utilities.css`
- `datepicker.js`

Cần viết lại logic khi sang MMO:

- Submit filter/search bằng Spring Controller hoặc fetch API.
- Phân trang: backend phải nhận page/pageSize và trả total record.
- Dropdown search: cần JS riêng nếu muốn search option.
- Datepicker/date range: dùng chung `css/datepicker.css` và `js/datepicker.js`; date range dùng 2 datepicker `dateFrom/dateTo`.
- Toast: CSS đã export, JS show/hide/stacking cần viết lại theo MMO.

## Giá trị suy ra trực tiếp

- `--ds-primary` lấy từ admin button `bg-blue-600` tương đương `#2563eb`.
- `--ds-primary-hover` lấy từ `hover:bg-blue-700` tương đương `#1d4ed8`.
- `--ds-bg` lấy từ `_AdminLayout.cshtml` body background `#f8fafc`.
- `--ds-font-family` lấy từ Google font Inter trong layout và `site.css`.
- Radius/shadow lấy từ `rounded`, `rounded-xl`, `rounded-2xl`, `shadow-sm`, `shadow-xl` đang dùng trong Razor/Tailwind.


