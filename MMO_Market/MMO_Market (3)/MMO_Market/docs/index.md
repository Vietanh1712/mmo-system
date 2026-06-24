---
title: MMO Market Documentation
status: active
owner: Project Team
last_updated: 2026-06-18
---

# Documentation Index

## Bắt đầu

- [README](../README.md): tổng quan và cách chạy nhanh.
- [AGENTS](../AGENTS.md): quy tắc bắt buộc khi thay đổi dự án.
- [Local Development](runbooks/local-development.md): cấu hình, build và run.

## Tài liệu nguồn

- [Business Specification](specifications/business-specification.md): hành vi nghiệp vụ mong muốn.
- [Architecture Overview](architecture/overview.md): kiến trúc và trạng thái hệ thống.
- [Database Reference](database/schema-reference.md): snapshot schema và quy tắc database.

## API

- [API Guideline](api/README.md): quy tắc thiết kế, sử dụng và tài liệu hóa API.
- [API Reference](api/api-reference.md): danh mục REST endpoint hiện có.

## Module

- [Search](modules/search.md)

Các module chưa có tài liệu riêng phải được bổ sung tại `docs/modules/` khi bắt đầu thay đổi đáng kể.

## Design System

- [Design System Index](design-system/README.md)
- [UI Guideline](design-system/ui-guideline.md)
- [UX/UI Sync Guideline](design-system/ux-ui-sync-guideline.md)
- [Icon Guideline](design-system/icon-guideline.md)
- [Examples](design-system/examples/)

## Changes

- [Implementation History](changes/implementation-history.md)
- [Admin profile fields](changes/admin/2026-06-04-user-profile-fields.md)
- [Changelog](../CHANGELOG.md)

## Archive

`docs/archive/` chứa báo cáo triển khai và đặc tả cũ. Nội dung archive không phải source of truth và có thể không còn khớp code hiện tại.

## Quy ước

- File Markdown dùng lowercase kebab-case.
- Mỗi tài liệu sống có metadata `title`, `status`, `owner`, `last_updated`.
- `status`: `draft`, `active`, `deprecated`, `archived`.
- Link nội bộ dùng đường dẫn tương đối.
- Không ghi credential hoặc dữ liệu nhạy cảm vào tài liệu.
