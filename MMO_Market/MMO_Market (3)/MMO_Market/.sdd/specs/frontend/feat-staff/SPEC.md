# SPEC — Staff Console Dashboard & Approvals
> **Feature ID:** `feat-staff`
> **Version:** 2.0 | **Status:** Active
> **Author:** Team | **Last Updated:** 2026-06-27

---

## 1. CONTEXT & GOAL (BỐI CẢNH & MỤC TIÊU)
Nhân viên (Staff) kiểm duyệt hồ sơ KYC, xử lý các lệnh rút tiền mặt đang chờ duyệt, và giải quyết khiếu nại tranh chấp đơn hàng thông qua phòng chat.

---

## 2. DANH SÁCH FILE THỰC TẾ & MAPPING
Các tệp tin thực tế trong dự án:

- **Dashboard chính (`/staff/dashboard`)**:
  * View: `templates/staff/dashboard.html`
  * Script: `static/js/staff-ui.js`
- **Duyệt KYC (`/staff/kyc`)**:
  * View: `templates/staff/kyc.html`
  * Script: `static/js/staff-kyc.js`
- **Chi tiết KYC (`/staff/kyc/detail`)**:
  * View: `templates/staff/kyc-detail.html`
  * Script: `static/js/staff-kyc-detail.js`
- **Yêu cầu rút tiền (`/staff/withdrawals`)**:
  * View: `templates/staff/withdrawals.html`
  * Script: `static/js/staff-ui.js`
- **Chi tiết rút tiền (`/staff/withdrawals/detail`)**:
  * View: `templates/staff/withdrawal-detail.html`
  * Script: `static/js/staff-ui.js`
- **Khiếu nại tranh chấp (`/staff/complaints`)**:
  * View: `templates/staff/complaints.html`
  * Script: `static/js/staff-complaints.js`
- **Chi tiết khiếu nại (`/staff/complaints/detail`)**:
  * View: `templates/staff/complaint-detail.html`
  * Script: `static/js/staff-complaint-detail.js`
- **Kênh chat phân xử (`/staff/chat`)**:
  * View: `templates/staff/chat.html`
  * Script: `static/js/staff-chat.js`
- **Support Ticket (`/staff/support-tickets`)**:
  * View: `templates/staff/support-tickets.html`
  * Script: `static/js/staff-support-tickets.js`
- **Chi tiết Ticket (`/staff/support-tickets/detail`)**:
  * View: `templates/staff/support-ticket-detail.html`
  * Script: `static/js/staff-support-ticket-detail.js`
- **Giao dịch toàn sàn (`/staff/transactions`)**:
  * View: `templates/staff/transactions.html`
  * Script: `static/js/staff-ui.js`
- **Cảnh báo vi phạm Shop (`/staff/flags`)**:
  * View: `templates/staff/flags.html`
  * Script: `static/js/staff-ui.js`
- **Quản lý Shop (`/staff/shop-registrations`)**:
  * View: `templates/staff/shop-registrations.html`
  * Script: `static/js/staff/staff-shop-registrations.js`

---

## 3. FUNCTIONAL REQUIREMENTS (EARS)
| ID | EARS Requirement |
|---|---|
| FR-STAF-01 | WHEN a Staff approves a withdrawal, THE SYSTEM SHALL release the funds from the `hold_balance` and record the transaction. |
| FR-STAF-02 | WHEN a Staff triggers a dispute resolution chat, THE SYSTEM SHALL establish a WebSocket connection. |
| FR-STAF-03 | WHEN Staff views the `/staff/transactions` page, THE SYSTEM SHALL provide a single date-picker filter (`fromDate`) allowing Staff to search transactions created on a specific day (00:00:00 – 23:59:59). The former two-field range filter (fromDate + toDate) has been consolidated into one field. |
| FR-STAF-04 | WHEN Staff opens a KYC detail page, THE SYSTEM SHALL display the following submitter information from the `Users` table alongside KYC document data: `full_name`, `email`, `date_of_birth`, `address`, and `rejection_reason` from `KycRequest`. |
| FR-STAF-05 | WHEN a KYC request has `rejection_reason IS NULL`, THE SYSTEM SHALL still render the "Lý do từ chối" row and display `-` as placeholder. |
| FR-STAF-06 | WHEN Staff views the `/staff/flags` page, THE SYSTEM SHALL query the database for distinct levels and statuses to populate the filter dropdown comboboxes dynamically, rather than using hardcoded values. |
| FR-STAF-07 | WHEN Staff views a Shop Flag detail page (`/staff/flags/detail`), THE SYSTEM SHALL populate the level and status select dropdowns dynamically from the database using distinct values. |
| FR-STAF-08 | WHEN Staff views a Withdrawal detail page (`/staff/withdrawals/detail`), THE SYSTEM SHALL populate the status select dropdown dynamically from the database using distinct values. |
| FR-STAF-09 | WHEN Staff views the `/staff/transactions` page, THE SYSTEM SHALL populate the status select filter dynamically from the database using distinct values, and display user-friendly Vietnamese labels for both status and type filter options. |
| FR-STAF-10 | WHEN Staff views `/staff/flags` or `/staff/flags/detail`, THE SYSTEM SHALL translate and display the flag level and status values in Vietnamese (Warning -> Cảnh cáo, Danger -> Nguy hiểm, Critical -> Phê bình, Suspension -> Đình chỉ, Ban -> Cấm; Effect -> Hiệu lực, Removed/Remove -> Đã gỡ) case-insensitively, while preserving English values in option values for backend requests. |
| FR-STAF-11 | WHEN Staff views `/staff/flags` table, THE SYSTEM SHALL display a pagination-aware sequence number (STT) column computed as `currentPage * pageSize + index`. |
| FR-STAF-12 | WHEN Staff clicks the "Khiếu nại" button on `/staff/flags/detail`, THE SYSTEM SHALL navigate to `/staff/complaints/detail?id={complaint_id}` and update the `sessionStorage` `selectedComplaintId` dynamically based on the URL parameter. If `flag.complaint` is null, THE SYSTEM SHALL hide the "Khiếu nại" button. |
| FR-STAF-13 | WHEN Staff views `/staff/withdrawals` or `/staff/withdrawals/detail`, THE SYSTEM SHALL translate and display the status values in Vietnamese (Pending -> Chờ duyệt, Approved -> Hoàn tất, Rejected -> Từ chối) case-insensitively, and style status badges dynamically (green for Approved, red for Rejected, yellow for Pending). |
| FR-STAF-14 | WHEN Staff views `/staff/transactions` or `/staff/transactions/detail`, THE SYSTEM SHALL translate and display the status values in Vietnamese (Success/Completed -> Thành công, Pending/Processing -> Đang xử lý, Hold/Holding/Escrow/Held -> Tạm giữ, Refunded/Refund -> Hoàn tiền, Disputed/Complaint -> Khiếu nại, Rejected/Failed/Cancelled -> Từ chối) case-insensitively, and style badges dynamically. |
| FR-STAF-15 | WHEN Staff views `/staff/transactions`, THE SYSTEM SHALL calculate and display transaction statistic numbers (completed, pending, failed) by querying English statuses from the database in a case-insensitive, null-safe way. |
| FR-STAF-16 | WHEN Staff clicks the "Làm mới bộ lọc" button on `/staff/kyc`, THE SYSTEM SHALL reset all search input values and dropdown selections (Mã hồ sơ, Loại giấy tờ, Trạng thái) to empty, and trigger a fresh reload of both the KYC list and statistics. |
| FR-STAF-17 | WHEN Staff views `/staff/complaints`, THE SYSTEM SHALL query and display complaint statistics (total, inProgress, resolved, rejected) by summing all corresponding status casing and terminology variants (e.g. Open + InProgress for in-progress stats) to ensure accuracy. |
| FR-STAF-18 | WHEN Staff filters complaints by status on `/staff/complaints`, THE SYSTEM SHALL map the UI selected status value (New, InProgress, Resolved, Rejected) to a collection of equivalent database status strings to fetch all matching records. |
| FR-STAF-19 | WHEN Staff searches or counts complaints on `/staff/complaints`, THE SYSTEM SHALL query records using `(c.isDelete = false OR c.isDelete IS NULL)` to support null values in the soft delete flag in the database. |
| FR-STAF-20 | WHEN Staff requests all complaints without any status filter on `/staff/complaints`, THE SYSTEM SHALL bind a dummy status to the SQL IN parameter to prevent Hibernate from throwing an empty collection exception. |
| FR-STAF-21 | THE SYSTEM SHALL display and treat both New/Open and InProgress/Processing statuses as "Đang xử lý" (yellow badge) on complaints, and remove "Mới" option from the filters and options. |
| FR-STAF-22 | THE SYSTEM SHALL remove the "+ Cắm cờ mới" button from the `/staff/flags` header area. |
| FR-STAF-23 | WHEN Staff views the complaint detail page (`/staff/complaints/detail`), THE SYSTEM SHALL query and display the `resolution` value under a row labeled "Lý do / Kết quả" inside the information card. |
| FR-STAF-24 | THE SYSTEM SHALL NOT support searching or filtering complaints by category/type. The complaints table shall remove the "Loại" (Type) column from the display. |
| FR-STAF-25 | THE SYSTEM SHALL fetch distinct complaint statuses dynamically from the database using the GET `/api/complaints/statuses` endpoint to populate the status filter dropdown, rather than using hardcoded values. |
| FR-STAF-26 | WHEN Staff views lists of transactions, withdrawals, shop flags, KYC requests, or complaints, THE SYSTEM SHALL display the data sorted by creation time (`createdAt`) in descending order (newest first). |
| FR-STAF-27 | WHEN Customer views their pre-orders history list, THE SYSTEM SHALL retrieve and display pre-orders sorted by creation time (`createdAt`) in descending order (newest first). |
| FR-STAF-28 | WHEN searching or listing products, THE SYSTEM SHALL default to sorting the products by creation time (`createdAt`) in descending order (newest first) unless otherwise specified by the user. |
| FR-STAF-29 | WHEN Staff views `/staff/withdrawals`, `/staff/transactions`, or `/staff/kyc` tables, THE SYSTEM SHALL display a pagination-aware sequence number (STT) column computed as `currentPage * pageSize + index + 1`. |
| FR-STAF-30 | WHEN Staff views the `/staff/shop-registrations` page, THE SYSTEM SHALL query the database and display shop statistics: Tổng số Shop (total registered shops), Shop đang hoạt động (active/approved shops), Shop bị khóa (banned/locked shops), and Tổng tiền cọc (sum of shop deposits formatted as VNĐ). |
| FR-STAF-31 | WHEN Staff searches or filters shop registrations on `/staff/shop-registrations`, THE SYSTEM SHALL query the database using the keyword and shop account status (`shopStatus`). The request status (`status`) filter has been removed. |
| FR-STAF-32 | WHEN Staff clicks on the "Quản lý Shop" quick link on the dashboard, THE SYSTEM SHALL navigate to `/staff/shop-registrations` and display the shop registrations interface. |
| FR-STAF-33 | THE SYSTEM SHALL NOT display the business category column in the Shops registrations table, and instead display three columns: Trạng thái Shop (`shop_status`), Tiền cọc (`deposit_vnd` formatted as VNĐ), and Số dư (`balance_vnd` formatted as VNĐ). The Shop Name column SHALL display the Shop Name, Support Email, and Support Phone (plain text). |
| FR-STAF-34 | WHEN displaying the shop account status (`shopStatus`) badge on the `/staff/shop-registrations` page, THE SYSTEM SHALL translate database values to standard Vietnamese (Pending -> Chờ kích hoạt, Approved/Active -> Hoạt động, Banned/Locked -> Đang bị khóa, Rejected -> Bị từ chối). |
| FR-STAF-35 | THE SYSTEM SHALL load the `staff-ui.js` script dependency on `/staff/shop-registrations` to enable the `mountStaffPagination` function for table pagination. |
| FR-STAF-36 | THE SYSTEM SHALL dynamically load the shop status filter dropdown from the `GET /api/v1/shop-registrations/shop-statuses` endpoint to match database records exactly (e.g. Approved, Active, Pending, Banned) and prevent search filter mismatch. |
| FR-STAF-37 | THE SYSTEM SHALL NOT display or load the registration request status filter, as it has been removed from the user interface. |
| FR-STAF-38 | THE SYSTEM SHALL NOT display the detail eye button in the Actions column of the Shop registrations table, and instead display a switch toggle to control the active status of the shop account. |
| FR-STAF-39 | WHEN Staff changes the switch toggle state, THE SYSTEM SHALL send a PUT request to `/api/v1/shop-registrations/{id}/toggle-status?active={true/false}` to change the shop status to Active (checked) or Banned (unchecked) in the database. |