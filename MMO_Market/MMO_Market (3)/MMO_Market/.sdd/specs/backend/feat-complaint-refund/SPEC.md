# SPEC — Complaint Refund & Notification in Wallet History
> **Feature ID:** `feat-complaint-refund`
> **UC Coverage:** UC-14 (Complaint Management), UC-09 (Notifications)
> **Version:** 1.1 | **Status:** Active
> **Author:** Team | **Last Updated:** 2026-06-29

---

## 1. CONTEXT & GOAL (BỐI CẢNH & MỤC TIÊU)

### 1.1 Bối cảnh
Khi Staff xử lý khiếu nại (complaint) bằng cách hoàn tiền hoặc từ chối, hệ thống phải **tự động ghi lại giao dịch** vào lịch sử ví (WalletTransactions) và **gửi thông báo hệ thống (Notification)** cho người dùng để cung cấp tính minh bạch, cho phép người dùng theo dõi tất cả hoàn tiền/giải ngân và nhận thông báo ngay lập tức.

### 1.2 Mục tiêu
- Tự động ghi WalletTransaction khi complaint được resolved (hoàn tiền) hoặc rejected (từ chối).
- Tự động gửi thông báo hệ thống (Notification) cho các bên liên quan (Buyer và Seller) khi trạng thái khiếu nại thay đổi.
- Cập nhật số liệu thống kê ví (Tổng nạp) để bao gồm cả tiền hoàn (`REFUND`) trên cả backend và frontend.
- Cung cấp tính traceable thông qua reference codes liên kết complaint → transaction.

---

## 2. ACTOR (TÁC NHÂN)
| Actor | Role | Điều kiện tiền quyết |
|---|---|---|
| **Staff** | Nhân viên vận hành | Tài khoản có vai trò `Staff` |
| **Customer** | Người mua (Buyer) | Bất kỳ tài khoản nào đã tạo khiếu nại |
| **Seller** | Người bán (Seller) | Tài khoản Seller có khiếu nại được xử lý |

---

## 3. FUNCTIONAL REQUIREMENTS (Cú pháp EARS)

### 3.1 Ghi REFUND transaction & gửi Notification khi Complaint Resolved
| ID | EARS Requirement |
|:---|:---|
| FR-COMPLAINT-REFUND-01 | WHEN a Staff resolves a complaint (status = "Resolved"), THE SYSTEM SHALL create a WalletTransaction with type="REFUND" recording the refund amount for the Customer (Buyer). |
| FR-COMPLAINT-REFUND-02 | THE SYSTEM SHALL update Customer.balanceVnd by adding transaction.amountVnd. |
| FR-COMPLAINT-REFUND-03 | THE SYSTEM SHALL record reference_code in format "REFUND-CMP-{complaintId}-TX-{transactionId}" for audit traceability. |
| FR-COMPLAINT-REFUND-04 | WHEN a complaint is resolved, THE SYSTEM SHALL create a Notification with type="WALLET" for the Customer notifying them about the refund success. |
| FR-COMPLAINT-REFUND-05 | WHEN a complaint is resolved, THE SYSTEM SHALL create a Notification with type="WALLET" for the Seller notifying them that the order has been refunded to the buyer. |

### 3.2 Ghi PAYMENT transaction & gửi Notification khi Complaint Rejected
| ID | EARS Requirement |
|:---|:---|
| FR-COMPLAINT-REFUND-06 | WHEN a Staff rejects a complaint (status = "Rejected"), THE SYSTEM SHALL create a WalletTransaction with type="PAYMENT" recording the payout (amount - commission) for the Seller. |
| FR-COMPLAINT-REFUND-07 | THE SYSTEM SHALL update Seller.balanceVnd by adding (transaction.amountVnd - commission). |
| FR-COMPLAINT-REFUND-08 | THE SYSTEM SHALL record reference_code in format "PAYOUT-CMP-REJECTED-{complaintId}-TX-{transactionId}". |
| FR-COMPLAINT-REFUND-09 | WHEN a complaint is rejected, THE SYSTEM SHALL create a Notification with type="WALLET" for the Seller notifying them that the payout has been successfully credited to their wallet. |
| FR-COMPLAINT-REFUND-10 | WHEN a complaint is rejected, THE SYSTEM SHALL create a Notification with type="WALLET" for the Customer (Buyer) notifying them that the complaint has been rejected along with the resolution reason. |

### 3.3 Hiển thị & Thống kê trong Wallet History
| ID | EARS Requirement |
|:---|:---|
| FR-COMPLAINT-REFUND-11 | WHEN a Customer or Seller views wallet transaction history, THE SYSTEM SHALL include REFUND (hoàn tiền) or PAYMENT (giải ngân) transactions. |
| FR-COMPLAINT-REFUND-12 | THE SYSTEM SHALL calculate and display REFUND transactions under the "Tổng nạp" (Total Topup) statistic on both backend and frontend. |

---

## 4. NON-FUNCTIONAL REQUIREMENTS
| ID | Category | Requirement |
|:---|:---|:---|
| NFR-COMPLAINT-REFUND-01 | Atomicity | Complaint resolution must be atomic: complaint status + transaction creation + notification creation + balance update all succeed or all rollback (use @Transactional). |
| NFR-COMPLAINT-REFUND-02 | Logging | All refund/payout operations must be logged via SLF4J with details: user ID, amount, balance_after, reference_code. |
| NFR-COMPLAINT-REFUND-03 | Performance | WalletTransaction & Notification creation must complete < 1 second per complaint resolution. |

---

## 5. DATA MODEL (Mô hình dữ liệu)

### 5.1 WalletTransactions & Notifications Tables
Cả hai bảng `WalletTransactions` và `Notifications` đều được giữ nguyên cấu trúc database hiện tại để lưu thông tin giao dịch hoàn tiền và thông báo cá nhân.

---

##  acceptance criteria (Tiêu chí nghiệm thu)
| ID | Scenario | Given (Bối cảnh) | When (Hành động) | Then (Kết quả) |
|---|---|---|---|---|
| AC-COMPLAINT-REFUND-01 | Hoàn tiền thành công | Staff ở trang khiếu nại detail | Bấm "Giải quyết" (Resolved) với lý do | Complaint.status = "Resolved", Customer.balanceVnd ↑, WalletTransaction (type="REFUND") được tạo, Notification được gửi cho cả Buyer và Seller, log ghi nhận |
| AC-COMPLAINT-REFUND-02 | Từ chối khiếu nại thành công | Staff ở trang khiếu nại detail | Bấm "Từ chối" (Rejected) với lý do | Complaint.status = "Rejected", Seller.balanceVnd ↑, WalletTransaction (type="PAYMENT") được tạo, Notification được gửi cho cả Buyer và Seller, log ghi nhận |
| AC-COMPLAINT-REFUND-03 | Thống kê số dư chính xác | Người dùng xem lịch sử giao dịch | Có giao dịch REFUND thành công | Thống kê "Tổng nạp" tăng thêm bằng số tiền hoàn của giao dịch đó |
