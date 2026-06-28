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
- **Các phân vùng view phụ (được render động bằng JS)**:
  * `dashboard`: Thống kê tài chính.
  * `audit-logs`: Vết lịch sử thao tác nhạy cảm.
  * `revenue`: Phân bổ doanh thu.
  * `add-staff`: Thêm nhân sự Staff.
  * `system-config` / `commissions`: Thiết lập biểu phí.
  * `permissions`: Phân quyền RBAC cho Staff.
  * `notifications`: Gửi thông báo hệ thống.

---

## 3. FUNCTIONAL REQUIREMENTS (EARS)
| ID | EARS Requirement |
|---|---|
| FR-ADM-01 | WHEN an Admin modifies commission settings, THE SYSTEM SHALL save parameters inside the `SystemConfigurations` table. |
| FR-ADM-02 | WHEN an Admin toggles account lock, THE SYSTEM SHALL update `isLocked = 1` or `0` on the `Users` table. |