---
title: Design System Export Report
status: archived
owner: Frontend Team
last_updated: 2026-06-18
---

# Design System Export Report

Đây là báo cáo lịch sử về quá trình tổng hợp component UI vào MMO Market.

## Component đã chuẩn hóa

- Design token, layout và typography.
- Avatar, badge, button và action icon.
- Form, filter, dropdown và Custom Datepicker.
- Table, pagination và states.
- Alert, toast, modal, toggle và tab.

## Kết quả

- CSS được tách thành các file trong `src/main/resources/static/css/`.
- JavaScript dùng chung nằm trong `src/main/resources/static/js/`.
- Example trực quan nằm tại [`examples/`](examples/).
- Thymeleaf import CSS qua fragment `design-system-styles`.

## Lưu ý

Nguồn export ban đầu có component từ một dự án giao diện khác. Chỉ guideline hiện hành và CSS trong source MMO Market mới được dùng làm chuẩn.

Xem:

- [UI Guideline](ui-guideline.md)
- [UX/UI Sync Guideline](ux-ui-sync-guideline.md)
- [Icon Guideline](icon-guideline.md)
