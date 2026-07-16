# SPEC — Complaint Refund & Notification in Wallet History
> **Feature ID:** `feat-complaint-refund`
> **UC Coverage:** UC-14 (Complaint Management), UC-09 (Notifications)
> **Version:** 1.2 | **Status:** Active
> **Author:** Team | **Last Updated:** 2026-07-16

---

## 1. CONTEXT & GOAL (BỐI CẢNH & MỤC TIÊU)

### 1.1 Bối cảnh
Khi Staff xử lý phán quyết khiếu nại (Complaints) bằng cách chấp nhận hoàn tiền (`Resolved`/`Completed`) hoặc từ chối (`Rejected`), hệ thống phải **tự động phân bổ lại dòng tài chính**, ghi nhận lịch sử giao dịch ví (`WalletTransactions`) và **gửi thông báo đẩy (`Notification`)** cho cả hai bên (Buyer và Seller). 

### 1.2 Mục tiêu
- Thực hiện hoàn tiền theo tỷ lệ thời gian sử dụng thực tế (Pro-rata) khi khiếu nại được chấp nhận.
- Tự động ghi nhận `WalletTransaction` với loại tương ứng (`REFUND` cho Buyer và `PAYMENT` cho Seller) ngay khi Staff phê duyệt hoặc từ chối khiếu nại.
- Gửi thông báo hệ thống để báo cáo số dư ví biến động và lý do phán quyết của Staff.
- Tích hợp số tiền hoàn `REFUND` vào thống kê "Tổng nạp" (Total Topup) trên giao diện quản lý ví của khách hàng.

---

## 2. ACTOR (TÁC NHÂN)
| Actor | Role | Điều kiện tiền quyết |
|---|---|---|
| **Staff** | Nhân viên vận hành | Cập nhật phán quyết trạng thái khiếu nại |
| **Customer** | Người mua (Buyer) | Nhận tiền hoàn tỷ lệ khi khiếu nại thành công |
| **Seller** | Người bán (Seller) | Nhận giải ngân (toàn bộ hoặc một phần) tùy thuộc kết quả phân xử |

---

## 3. FUNCTIONAL REQUIREMENTS (Cú pháp EARS)

### 3.1 Chấp nhận khiếu nại (Status = "Resolved" hoặc "Completed") — Hoàn tiền tỷ lệ (Pro-rata)
| ID | EARS Requirement |
|:---|:---|
| **FR-REFUND-01** | WHEN a Staff resolves a complaint as "Resolved" or "Completed", THE SYSTEM SHALL parse the subscription duration days ($D_{total}$) from the variant name (e.g. "1 year" $\rightarrow$ 365, "1 month" $\rightarrow$ 30). |
| **FR-REFUND-02** | THE SYSTEM SHALL compute the days used ($D_{used}$) and days remaining ($D_{remaining} = D_{total} - D_{used}$) from transaction creation to complaint creation. |
| **FR-REFUND-03** | THE SYSTEM SHALL calculate refund amount for Buyer: $A_{refund} = \lceil A_{total} / D_{total} \times D_{remaining} \rceil$. |
| **FR-REFUND-04** | THE SYSTEM SHALL compute payout amount for Seller: $A_{payout} = A_{total} - A_{refund}$, and Seller net payout: $A_{net\_payout} = A_{payout} - Commission_{actual}$. |
| **FR-REFUND-05** | THE SYSTEM SHALL add $A_{refund}$ to Customer's `balanceVnd` and create a `WalletTransaction` of type `REFUND` with reference `REFUND-CMP-{complaintId}-TX-{transactionId}`. |
| **FR-REFUND-06** | THE SYSTEM SHALL add $A_{net\_payout}$ to Seller's `balanceVnd` and create a `WalletTransaction` of type `PAYMENT` with reference `PAYOUT-CMP-RESOLVED-{complaintId}-TX-{transactionId}`. |
| **FR-REFUND-07** | THE SYSTEM SHALL create success Notifications (type = `WALLET`) notifying both Customer and Seller of their respective refund and payout amounts. |

### 3.2 Từ chối khiếu nại (Status = "Rejected") — Giải ngân toàn bộ cho Seller
| ID | EARS Requirement |
|:---|:---|
| **FR-REFUND-08** | WHEN a Staff rejects a complaint as "Rejected", THE SYSTEM SHALL calculate full Seller payout: $A_{net\_payout} = A_{total} - Commission_{original}$. |
| **FR-REFUND-09** | THE SYSTEM SHALL update Seller's `balanceVnd` by adding $A_{net\_payout}$ and create a `WalletTransaction` of type `PAYMENT` with reference `PAYOUT-CMP-REJECTED-{complaintId}-TX-{transactionId}`. |
| **FR-REFUND-10** | THE SYSTEM SHALL create success Notifications (type = `WALLET`) notifying Seller of full payout, and Customer of complaint rejection with the resolution reason. |

### 3.3 Thống kê trong Wallet History
| ID | EARS Requirement |
|:---|:---|
| **FR-REFUND-11** | WHEN calculating user wallet stats, THE SYSTEM SHALL include both `TOPUP` and `REFUND` transaction amounts in the "Tổng nạp" (Total Topup) statistic. |

---

## 4. NON-FUNCTIONAL REQUIREMENTS
| ID | Category | Requirement |
|:---|:---|:---|
| **NFR-REFUND-01** | Atomicity | Quy trình hoàn tiền và giải ngân bắt buộc phải nằm trong `@Transactional` (rollback toàn bộ nếu có bất kỳ lỗi nào xảy ra). |
| **NFR-REFUND-02** | Traceability | Mọi mã tham chiếu giao dịch ví (`referenceCode`) phải tuân thủ đúng định dạng quy định để dễ dàng tra cứu kiểm toán. |

---

## 5. Tiêu Chí Nghiệm Thu (Acceptance Criteria)

### AC-REFUND-01 — Tính toán hoàn tiền tỷ lệ
- **Cho trước:** Đơn hàng trị giá 300,000 VNĐ cho gói biến thể "Netflix 6 tháng" (180 ngày). Khách hàng tạo khiếu nại sau 60 ngày sử dụng.
- **Khi:** Staff phán quyết "Resolved" cho khiếu nại đó.
- **Thì:**
  - Số ngày còn lại là 120 ngày.
  - Số tiền hoàn Buyer nhận: $300,000 / 180 \times 120 = 200,000$ VNĐ.
  - Số tiền giải ngân cho Seller (trước hoa hồng): $100,000$ VNĐ.
  - Hệ thống ghi nhận các giao dịch `REFUND` cho Buyer và `PAYMENT` cho Seller với mã tham chiếu tương ứng.
