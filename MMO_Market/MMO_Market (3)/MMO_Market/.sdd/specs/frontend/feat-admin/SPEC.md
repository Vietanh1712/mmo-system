# SPEC — Admin Console Dashboard & RBAC
> **Feature ID:** `feat-admin`
> **UC Coverage:** UC-15 (System Administration)
> **Version:** 2.0 | **Status:** Active
> **Author:** Team | **Last Updated:** 2026-06-27

---

## 1. CONTEXT & GOAL (BỐI CẢNH & MỤC TIÊU)
Quản trị viên tối cao (Admin) kiểm soát hệ thống, thay đổi biểu phí phần trăm, khóa/mở khóa tài khoản, phân quyền Staff và kiểm tra vết lịch sử audit logs.

---

## 2. DANH SÁCH FILE THỰC TẾ & MAPPING
Các tệp tin thực tế trong dự án:

- **Trang quản trị duy nhất (`/admin/users`)**:
  * View: `templates/admin/users.html`
  * Script: `static/js/admin-console.js`
- **Các phân vùng view phụ & tài liệu chi tiết (được render động bằng JS)**:
  * `dashboard` (Bảng điều khiển): Thống kê tài chính & SVG chart ([SPEC-dashboard.md](file:///c:/SU26/SWP/mmo-market1/MMO_Market/MMO_Market%20%283%29/MMO_Market/.sdd/specs/frontend/feat-admin/SPEC-dashboard.md))
  * `audit-logs` (Nhật ký hệ thống): Kiểm toán hoạt động ([SPEC-audit-logs.md](file:///c:/SU26/SWP/mmo-market1/MMO_Market/MMO_Market%20%283%29/MMO_Market/.sdd/specs/frontend/feat-admin/SPEC-audit-logs.md))
  * `revenue` (Doanh thu & Dòng tiền): Dòng tiền giao dịch ([SPEC-revenue.md](file:///c:/SU26/SWP/mmo-market1/MMO_Market/MMO_Market%20%283%29/MMO_Market/.sdd/specs/frontend/feat-admin/SPEC-revenue.md))
  * `accounts` / `add-staff` (Quản lý tài khoản): Khóa/mở khóa & CRUD Staff ([SPEC-users.md](file:///c:/SU26/SWP/mmo-market1/MMO_Market/MMO_Market%20%283%29/MMO_Market/.sdd/specs/frontend/feat-admin/SPEC-users.md))
  * `system-config` / `commissions` (Cấu hình hệ thống & Phí): Thiết lập thông số và biểu phí ([SPEC-config.md](file:///c:/SU26/SWP/mmo-market1/MMO_Market/MMO_Market%20%283%29/MMO_Market/.sdd/specs/frontend/feat-admin/SPEC-config.md))
  * `permissions` (Phân quyền nhân viên): Gán/thu hồi quyền Staff ([SPEC-permissions.md](file:///c:/SU26/SWP/mmo-market1/MMO_Market%20%283%29/MMO_Market/.sdd/specs/frontend/feat-admin/SPEC-permissions.md))
  * `notifications` (Quản lý thông báo & Bảo trì): Soạn thông báo & bảo trì hệ thống ([SPEC-notifications.md](file:///c:/SU26/SWP/mmo-market1/MMO_Market%20%283%29/MMO_Market/.sdd/specs/frontend/feat-admin/SPEC-notifications.md))

---

## 3. FUNCTIONAL REQUIREMENTS (EARS)
| ID | EARS Requirement |
|---|---|
| FR-ADM-01 | WHEN an Admin modifies commission settings, THE SYSTEM SHALL save parameters inside the `SystemConfigurations` table. |
| FR-ADM-02 | WHEN an Admin toggles account lock, THE SYSTEM SHALL update `isLocked = 1` or `0` on the `Users` table. |