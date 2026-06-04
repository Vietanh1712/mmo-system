# MMO Market Icon Guideline

Nguồn icon đang thấy trong MMO Market:

- Inline SVG trực tiếp trong Razor: phần lớn icon ở sidebar, button, action table, search, calendar, logout.
- Kiểu stroke/icon giống Lucide, nhưng project không import thư viện icon riêng cho admin UI.
- Không thấy CSS icon font riêng như Font Awesome hoặc Bootstrap Icons trong UI admin hiện tại.

Icon/action đã thấy:

| Action | Đã thấy | Nguồn/cách dùng |
| --- | --- | --- |
| Search | Có | Inline SVG kính lúp trong filter/search button |
| Trash can | Có | Inline SVG trong action xóa |
| Plus | Có | Inline SVG trong nút thêm mới |
| Angle Left | Có | Ký tự `&lsaquo;`, `&laquo;` ở pagination |
| Angle Right | Có | Ký tự `&rsaquo;`, `&raquo;` ở pagination |
| Edit / View | Có | Icon mắt inline SVG cho xem/sửa |
| Toggle | Có | CSS track/knob, không phải icon SVG |
| Pagination | Có | Ký tự điều hướng |
| Toast Notification | Có | Inline SVG/check/close trong layout toast |
| Calendar | Có | Inline SVG lịch ở datepicker |
| Refresh/reset | Có | Inline SVG cấp lại mật khẩu nhanh |
| Filter | Chưa thấy icon filter riêng | MMO Market dùng ô filter/dropdown, chưa thấy icon funnel |
| Sort arrows | Có | `.ds-icon ds-icon-sort` trong `css/icons.css` |
| Cloud Upload | Có | `.ds-icon ds-icon-cloud-upload` trong `css/icons.css` |
| Flag | Có | `.ds-icon ds-icon-flag` trong `css/icons.css` |
| Notification Bell | Có | `.ds-icon ds-icon-bell` trong `css/icons.css` |

## Icon CSS dùng chung

Các icon mới được dựng bằng CSS mask từ inline SVG data URI. Icon lấy màu từ `currentColor`, vì vậy mặc định có thể hiển thị màu đen và vẫn đổi màu linh hoạt bằng thuộc tính `color` ở class cha.

```html
<span class="ds-icon ds-icon-sort"></span>
<span class="ds-icon ds-icon-cloud-upload"></span>
<span class="ds-icon ds-icon-flag"></span>
<span class="ds-icon ds-icon-bell"></span>
```

Ví dụ đổi kích thước và màu:

```html
<span class="ds-icon ds-icon-bell" style="width:24px;height:24px;color:#000"></span>
```

Quy tắc dùng:

- Luôn dùng chung base class `.ds-icon`.
- Không hardcode màu trong HTML nếu có thể dùng class CSS cha.
- Dùng `currentColor` để icon đi theo màu chữ/button.
- Khi đặt icon trong button, dùng `aria-label` hoặc text ẩn nếu button chỉ có icon.
- Không trộn nhiều style icon khác nhau trên cùng một màn nếu không cần.

## Khi nào dùng từng icon

| Icon | Class | Dùng cho |
| --- | --- | --- |
| Sort arrows | `.ds-icon-sort` | Sắp xếp cột bảng, sort ASC/DESC, đổi thứ tự danh sách |
| Cloud Upload | `.ds-icon-cloud-upload` | Upload file, kéo thả file, import dữ liệu |
| Flag | `.ds-icon-flag` | Báo cáo vi phạm, đánh dấu khiếu nại, cảnh báo nội dung |
| Notification Bell | `.ds-icon-bell` | Thông báo hệ thống, notification menu, trạng thái có tin mới |

## Khuyến nghị khi mang sang MMO_System

- Copy SVG inline từ ví dụ hoặc dùng cùng style stroke `fill="none" stroke="currentColor" stroke-width="2"`.
- Nếu MMO dùng thư viện icon, chọn Lucide tương đương để đồng bộ nét icon.
- Với icon dùng lại nhiều lần, ưu tiên `.ds-icon-*` trong `icons.css` thay vì copy inline SVG lặp lại.


