# SPEC — Staff Console Dashboard & Approvals
> **Feature ID:** `feat-staff`

---

## 1. CONTEXT & GOAL (BỐI CẢNH & MỤC TIÊU)
Nhân viên (Staff) kiểm duyệt hồ sơ KYC, xử lý các lệnh rút tiền mặt đang chờ duyệt, và giải quyết khiếu nại tranh chấp đơn hàng thông qua phòng chat.

---

## 2. DANH SÁCH FILE THỰC TẾ & MAPPING
Các tệp tin thực tế trong dự án (nằm dưới thư mục `apps/frontend/`):

- **Dashboard chính (`/staff/dashboard`)**:
  * View: `templates/staff/dashboard.html`
  * Script: `static/js/staff/staff-ui.js`
- **Duyệt KYC (`/staff/kyc`)**:
  * View: `templates/staff/kyc.html`
  * Script: `static/js/staff/staff-kyc.js`
- **Chi tiết KYC (`/staff/kyc/detail`)**:
  * View: `templates/staff/kyc-detail.html`
  * Script: `static/js/staff/staff-kyc-detail.js`
- **Yêu cầu rút tiền (`/staff/withdrawals`)**:
  * View: `templates/staff/withdrawals.html`
  * Script: `static/js/staff/staff-ui.js`
- **Chi tiết rút tiền (`/staff/withdrawals/detail`)**:
  * View: `templates/staff/withdrawal-detail.html`
  * Script: `static/js/staff/staff-ui.js`
- **Khiếu nại tranh chấp (`/staff/complaints`)**:
  * View: `templates/staff/complaints.html`
  * Script: `static/js/staff/staff-complaints.js`
- **Chi tiết khiếu nại (`/staff/complaints/detail`)**:
  * View: `templates/staff/complaint-detail.html`
  * Script: `static/js/staff/staff-complaint-detail.js`
- **Kênh chat phân xử (`/staff/chat`)**:
  * View: `templates/staff/chat.html`
  * Script: `static/js/staff/staff-chat.js`
- **Support Ticket (`/staff/support-tickets`)**:
  * View: `templates/staff/support-tickets.html`
  * Script: `static/js/staff/staff-support-tickets.js`
- **Chi tiết Ticket (`/staff/support-tickets/detail`)**:
  * View: `templates/staff/support-ticket-detail.html`
  * Script: `static/js/staff/staff-support-ticket-detail.js`
- **Giao dịch toàn sàn (`/staff/transactions`)**:
  * View: `templates/staff/transactions.html`
  * Script: `static/js/staff/staff-ui.js`
- **Cảnh báo vi phạm Shop (`/staff/flags`)**:
  * View: `templates/staff/flags.html`
  * Script: `static/js/staff/staff-ui.js`
- **Quản lý Shop (`/staff/shop-registrations`)**:
  * View: `templates/staff/shop-registrations.html`
  * Script: `static/js/staff/staff-shop-registrations.js`
- **Chi tiết đăng ký Shop (`/staff/shop-registrations/detail`)**:
  * View: `templates/staff/shop-registration-detail.html`
  * Script: `static/js/staff/staff-shop-registration-detail.js`
- **Cập nhật trạng thái Shop (`/staff/shop-registrations/update-status`)**:
  * View: `templates/staff/shop-registration-update-status.html`
  * Script: `static/js/staff/staff-shop-registration-update-status.js`
- **Tổng quan đơn từ (`/staff/documents`)**:
  * View: `templates/staff/documents-dashboard.html`
  * Script: `static/js/staff/staff-ui.js`

---

## 3. FUNCTIONAL REQUIREMENTS (EARS)
| ID | EARS Requirement |
|---|---|
| FR-STAF-01 | WHEN a Staff approves a withdrawal, THE SYSTEM SHALL release the funds from the `hold_balance` and record the transaction. |
| FR-STAF-02 | WHEN a Staff triggers a dispute resolution chat, THE SYSTEM SHALL establish a WebSocket connection. |
| FR-STAF-03 | WHEN Staff views the `/staff/transactions` page, THE SYSTEM SHALL provide a single date-picker filter (`fromDate`) allowing Staff to search transactions created on a specific day (00:00:00 – 23:59:59). The former two-field range filter (fromDate + toDate) has been consolidated into one field. |
| FR-STAF-04 | WHEN Staff opens a KYC detail page, THE SYSTEM SHALL display the following submitter information from the `Users` table alongside KYC document data: `full_name`, `email`, `date_of_birth`, `address`, and `rejection_reason` from `KycRequest`. |
| FR-STAF-05 | WHEN a KYC request has `rejection_reason IS NULL`, THE SYSTEM SHALL still render the "Lý do từ chối" row and display `-` as placeholder. |
| FR-STAF-06 | WHEN Staff views the `/staff/flags` page, THE SYSTEM SHALL populate the level and status filter select dropdowns with fixed predefined options (matching Admin filtering style). |
| FR-STAF-07 | WHEN Staff views a Shop Flag detail page (`/staff/flags/detail`), THE SYSTEM SHALL populate the level and status select dropdowns with fixed predefined options. |
| FR-STAF-08 | WHEN Staff views `/staff/withdrawals`, THE SYSTEM SHALL populate the status select filter with fixed predefined options (Pending, Approved, Rejected). |
| FR-STAF-09 | WHEN Staff views the `/staff/transactions` page, THE SYSTEM SHALL populate the status select filter with fixed predefined options (Success, Pending, Holding, Refunded, Disputed, Failed) with user-friendly Vietnamese labels, and map each selected status option to a collection of equivalent casing and terminology variants (e.g. Success -> Success, SUCCESS, Completed, COMPLETED, Approved; Failed -> Failed, FAILED, Fail, FAIL, Rejected, REJECTED, Cancelled, CANCELLED) to ensure 100% accurate search results. |
| FR-STAF-10 | WHEN Staff views `/staff/flags` or `/staff/flags/detail`, THE SYSTEM SHALL translate and display the flag level and status values in Vietnamese (Warning -> Cảnh cáo, Danger -> Nguy hiểm, Critical -> Phê bình, Suspension -> Đình chỉ, Ban -> Cấm; Effect -> Hiệu lực, Removed/Remove -> Đã gỡ) case-insensitively, while preserving English values in option values for backend requests. |
| FR-STAF-11 | WHEN Staff views `/staff/flags` table, THE SYSTEM SHALL display a pagination-aware sequence number (STT) column computed as `currentPage * pageSize + index`. |
| FR-STAF-12 | WHEN Staff clicks the "Khiếu nại" button on `/staff/flags/detail`, THE SYSTEM SHALL navigate to `/staff/complaints/detail?id={complaint_id}` and update the `sessionStorage` `selectedComplaintId` dynamically based on the URL parameter. If `flag.complaint` is null, THE SYSTEM SHALL hide the "Khiếu nại" button. |
| FR-STAF-13 | WHEN Staff views `/staff/withdrawals` or `/staff/withdrawals/detail`, THE SYSTEM SHALL translate and display the status values in Vietnamese (Pending -> Chờ duyệt, Approved -> Hoàn tất, Rejected -> Từ chối) case-insensitively, and style status badges dynamically (green for Approved, red for Rejected, yellow for Pending). |
| FR-STAF-14 | WHEN Staff views `/staff/transactions` or `/staff/transactions/detail`, THE SYSTEM SHALL translate and display the status values in Vietnamese (Success/Completed -> Thành công, Pending/Processing -> Đang xử lý, Hold/Holding/Escrow/Held -> Tạm giữ, Refunded/Refund -> Hoàn tiền, Disputed/Complaint -> Khiếu nại, Rejected/Failed/Cancelled -> Từ chối) case-insensitively, and style badges dynamically. |
| FR-STAF-15 | WHEN Staff views `/staff/transactions`, THE SYSTEM SHALL calculate and display overall transaction statistics (total, completed, pending, failed) from the database as fixed system-wide counts, so that top stat cards do not shrink when filtering by status, while displaying the filtered record count (`totalFilteredTransactions`) in the table pagination footer. |
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
| FR-STAF-27 | WHEN Staff views `/staff/shop-registrations`, THE SYSTEM SHALL calculate and display shop statistics (total, active, suspended, locked, banned, withdrawn, deposit sum) directly from the registered shop list by matching all status terminology variants (including TEMPORARILY_CLOSED and CLOSED) to guarantee 100% mathematical consistency with table rows. |
| FR-STAF-27 | WHEN Customer views their pre-orders history list, THE SYSTEM SHALL retrieve and display pre-orders sorted by creation time (`createdAt`) in descending order (newest first). |
| FR-STAF-28 | WHEN searching or listing products, THE SYSTEM SHALL default to sorting the products by creation time (`createdAt`) in descending order (newest first) unless otherwise specified by the user. |
| FR-STAF-29 | WHEN Staff views `/staff/withdrawals`, `/staff/transactions`, `/staff/kyc`, `/staff/support-tickets`, or `/staff/shop-registrations` tables, THE SYSTEM SHALL display a pagination-aware sequence number (STT) column computed as `currentPage * pageSize + index + 1`. |
| FR-STAF-30 | WHEN Staff views the `/staff/shop-registrations` page, THE SYSTEM SHALL query the database and display shop statistics: Tổng số Shop (total registered shops), Shop đang hoạt động (active/approved shops), Shop bị khóa (banned/locked shops), and Tổng tiền cọc (sum of shop deposits formatted as VNĐ). |
| FR-STAF-31 | WHEN Staff searches or filters shop registrations on `/staff/shop-registrations`, THE SYSTEM SHALL query the database using the keyword and shop account status (`shopStatus`). The request status (`status`) filter has been removed. |
| FR-STAF-32 | WHEN Staff clicks on the "Quản lý Shop" quick link on the dashboard, THE SYSTEM SHALL navigate to `/staff/shop-registrations` and display the shop registrations interface. |
| FR-STAF-33 | THE SYSTEM SHALL NOT display the business category column, the submission date ("Ngày gửi") column, and the description ("Mô tả") column in the Shops registrations table, and instead display: Trạng thái Shop (`shop_status`), Tiền cọc (`deposit_vnd` formatted as VNĐ), and Số dư (`balance_vnd` formatted as VNĐ). The Shop Name column SHALL display only the Shop Name. |
| FR-STAF-34 | WHEN displaying the shop account status (`shopStatus`) badge on `/staff/shop-registrations`, THE SYSTEM SHALL translate database values to standardized Vietnamese: Active ("Hoạt động"), Suspended ("Tạm ngưng"), Locked ("Tạm khóa"), Banned ("Khóa vĩnh viễn"), and Withdrawn ("Đã đóng Shop (Hoàn cọc)"). Stat cards SHALL display: "Shop tạm khóa", "Shop tạm ngưng", and "Đã đóng Shop (Hoàn cọc)". |
| FR-STAF-35 | THE SYSTEM SHALL load the `staff-ui.js` script dependency on `/staff/shop-registrations` to enable the `mountStaffPagination` function for table pagination. |
| FR-STAF-36 | WHEN Staff views `/staff/topups`, THE SYSTEM SHALL display a list of all topup transactions (including SePay code, user info, amount, raw transfer content, status, and creation time) and 4 top statistics cards: Tổng giao dịch nạp (total topups), Thành công (success), Thất bại (failed), and Chờ xử lý (pending). |
| FR-STAF-37 | WHEN Staff clicks "Xem chi tiết" on a top-up transaction, THE SYSTEM SHALL navigate to `/staff/topups/detail?id={id}` displaying full transaction details, bank reference, user before/after balance, raw content, and staff intervention notes. |
| FR-STAF-38 | WHEN Staff clicks "Thử lại / Kích hoạt lại" on a Failed top-up transaction, THE SYSTEM SHALL display a popup modal allowing Staff to verify/enter target User ID, skip minimum deposit check, and submit a manual top-up activation that credits user balance, logs wallet ledger, updates status to Success, and notifies the user. |
| FR-STAF-36 | THE SYSTEM SHALL dynamically load the shop status filter dropdown from the `GET /api/v1/shop-registrations/shop-statuses` endpoint to match database records exactly (e.g. Approved, Active, Pending, Banned) and prevent search filter mismatch. |
| FR-STAF-37 | THE SYSTEM SHALL NOT display or load the registration request status filter, as it has been removed from the user interface. |
| FR-STAF-38 | THE SYSTEM SHALL display a single eye icon button (`ds-icon-btn ds-icon-btn-view`) in the Actions column of the Shop registrations table to navigate directly to `/staff/shop-registrations/detail?id={id}`. |
| FR-STAF-39 | WHEN Staff changes the switch toggle state, THE SYSTEM SHALL send a PUT request to `/api/v1/shop-registrations/{id}/toggle-status?active={true/false}` to change the shop status to Active (checked) or Banned (unchecked) in the database. |
| FR-STAF-40 | WHEN Staff views the Shop registration detail page (`/staff/shop-registrations/detail`), THE SYSTEM SHALL display the shop profile, owner name, financial info (deposit, balance), owner contact info, bank info, PENDING approval panel (if pending), AND the consolidated "Cập nhật trạng thái hoạt động" status management section with 5 status options. |
| FR-STAF-41 | WHEN Staff reviews a Shop registration request, THE SYSTEM SHALL display the decision panel (Approve / Reject buttons with a rejection reason text area) only if the request status is "PENDING". If the request is rejected, the system SHALL display the rejection reason. |
| FR-STAF-42 | THE SYSTEM SHALL display the shop code in the format '#SHOP-{id}' (e.g. #SHOP-1 instead of #SHOP-000001) where the ID is the raw shop registration ID retrieved from the database. |
| FR-STAF-43 | WHEN Staff clicks on the "Quản lý đơn từ" menu link, THE SYSTEM SHALL navigate to `/staff/documents` and display the documents overview page containing stats and quick access cards for complaints, transactions, withdrawals, and support tickets. |
| FR-STAF-44 | THE SYSTEM SHALL display "Tổng số Shop" (Total Shops count from the database) as the first stats card and exclude "Yêu cầu rút tiền" (Withdrawal requests) stats card on the main staff dashboard page (`/staff/dashboard`). |
| FR-STAF-45 | THE SYSTEM SHALL query and display the count of "Giao dịch chờ duyệt" (Pending transactions count from the database) alongside open complaints, pending withdrawals, and pending support tickets on the documents overview page (`/staff/documents`). |
| FR-STAF-46 | THE SYSTEM SHALL group complaints, transactions, withdrawals, and support tickets under a collapsible "Quản lý đơn từ" dropdown in the sidebar where clicking the header title navigates to `/staff/documents` and clicking the arrow toggle collapses or opens the menu. |
| FR-STAF-47 | WHEN Staff views the `/staff/support-tickets` page, THE SYSTEM SHALL query and display all user support tickets sorted by `createdAt` in descending order, showing their category, customer name, title, and current status. |
| FR-STAF-48 | WHEN Staff views `/staff/support-tickets/detail`, THE SYSTEM SHALL query and render the ticket details, customer information, message timeline, and enable Staff to post replies to the customer. |
| FR-STAF-49 | THE SYSTEM SHALL consolidate shop detail viewing and operating status management into a single page `/staff/shop-registrations/detail`, eliminating the standalone `/staff/shop-registrations/update-status` page. |
| FR-STAF-50 | WHEN Staff accesses `/staff/documents`, THE SYSTEM SHALL render quick-link cards allowing immediate navigation to complaints list, transactions list, and withdrawals list. |
| FR-STAF-51 | THE SYSTEM SHALL NOT display the "Lý do" (Reason) column in the Shop Flags table on `/staff/flags`. |
| FR-STAF-52 | WHEN Staff views the `/staff/flags` page, THE SYSTEM SHALL display the statistic cards (`staff-stat-grid`) in a 3-column grid layout per row (`staff-stat-grid--3cols`) on desktop screens, responsive to 2 columns on tablet and 1 column on mobile screens. |
| FR-STAF-53 | WHEN a Shop is in Suspended status, THE SYSTEM SHALL display a live real-time countdown timer ("Tự động mở lại sau") showing remaining Days, Hours, Minutes, and Seconds until automatic reinstatement to Active status. |
| FR-STAF-54 | THE SYSTEM SHALL standardize the layout and CSS structure of `/staff/topups/detail` using the unified Staff Design System standards (`staff-breadcrumb`, `staff-page-header`, `staff-detail-grid`, `ds-card`, `staff-info-list`, and `ds-money`) to guarantee 100% UI layout synchronization across all Staff detail pages. |
| FR-STAF-54 | WHEN Staff views the `/staff/shop-registrations` page, THE SYSTEM SHALL display the statistic cards (`staff-stat-grid`) in a 4-column grid layout per row (`staff-stat-grid--4cols`) on desktop screens, responsive to 2 columns on tablet and 1 column on mobile screens. |
| FR-STAF-55 | WHEN Staff selects "Tạm khóa" (Locked) or "Tạm ngưng" (Suspended) status for a shop, THE SYSTEM SHALL launch a modal dialog (`suspendShopModal`) or container (`#lockDurationContainer`) requiring Staff to specify the expiration Date, Hour, and Minute (`suspendedUntil`) before submitting. |
| FR-STAF-56 | WHEN a shop is in Suspended or Locked status, THE SYSTEM SHALL display a highlighted alert card (`cardSuspendedAlert`) inside the "Cập nhật trạng thái hoạt động" side card containing the exact deadline date and a real-time countdown timer ("Tự động mở lại sau"). |
| FR-STAF-57 | WHEN Staff searches on `/staff/topups`, THE SYSTEM SHALL parse keyword formats including `#TOPUP-{id}`, `TOPUP-{id}`, `#{id}`, or `{id}` to match top-up transaction IDs or user IDs, and trigger search automatically on Enter key press or status dropdown change. |
| FR-STAF-58 | WHEN Staff searches shop registrations on `/staff/shop-registrations`, THE SYSTEM SHALL match the input keyword against shop code (e.g., `SHOP-1` or `1`), shop name, support email, support phone, category, seller full name, and seller email. |
| FR-STAF-59 | WHEN Staff filters complaints on `/staff/complaints`, THE SYSTEM SHALL provide a date-picker input (`#staff-date-input`) allowing Staff to filter complaints created on a specific day. |
| FR-STAF-60 | WHEN a Seller whose shop is in "Tạm khóa" (Locked) or "Tạm ngưng" (Suspended) status attempts to open any Seller Console page, THE SYSTEM SHALL render a full-screen blurred modal overlay (`showShopLockedOverlay`) displaying the unlock deadline date & time and blocking access. |