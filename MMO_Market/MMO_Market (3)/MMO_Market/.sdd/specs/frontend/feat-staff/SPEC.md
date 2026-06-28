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

---

## 3. FUNCTIONAL REQUIREMENTS (EARS)
| ID | EARS Requirement |
|---|---|
| FR-STAF-01 | WHEN a Staff approves a withdrawal, THE SYSTEM SHALL release the funds from the `hold_balance` and record the transaction. |
| FR-STAF-02 | WHEN a Staff triggers a dispute resolution chat, THE SYSTEM SHALL establish a WebSocket connection. |