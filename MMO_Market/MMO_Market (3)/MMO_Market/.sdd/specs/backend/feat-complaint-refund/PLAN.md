# PLAN — Complaint Refund Notification (`feat-complaint-refund`)

## 1. Mục tiêu (Goals)

Triển khai tính năng **ghi lại hoàn tiền từ khiếu nại vào lịch sử giao dịch và gửi thông báo hoàn tiền/giải ngân** cho người dùng. 
Đồng thời khắc phục các lỗi kỹ thuật phát sinh:
- Ghi nhận giao dịch hoàn tiền theo tỷ lệ sử dụng (Pro-rata) dựa trên số ngày bảo lãnh thực tế của gói sản phẩm.
- Sửa lỗi gọi phương thức `recordTransaction` trong `ComplaintServiceImpl` với đầy đủ tham số (bao gồm số dư sau giao dịch `balanceAfter` và ID tham chiếu `referenceId` là ID của khiếu nại).
- Gửi thông báo (Notification) tự động cho Buyer và Seller khi khiếu nại được giải quyết (Resolved/Completed - Hoàn tiền tỷ lệ / Rejected - Giải ngân toàn bộ).
- Cập nhật thống kê "Tổng nạp" trong trang Lịch sử giao dịch (Backend & Frontend) để tính cả các giao dịch `REFUND` thành công.

---

## 2. Kiến trúc & Công nghệ

- **Backend:** Java 17, Spring Boot 3.1, Spring Data JPA, SQL Server (T-SQL).
- **Frontend:** HTML/CSS/JS (Thymeleaf thuần, vanilla JS).
- **Tuân thủ:** Mô hình phân lớp Controller → Service → Repository → Entity; `@Transactional` cho atomicity; Logging via SLF4J.

---

## 3. Các thành phần Backend

### 3.1. Database Migration & Entities

- **Entity `WalletTransaction`** (bảng `WalletTransactions`):
  - Sử dụng các cột đã định nghĩa: `balance_after`, `transaction_type`, `reference_id` (lưu ID khiếu nại).
- **Entity `Notification`** (bảng `Notifications`):
  - Sử dụng bảng `Notifications` hiện tại để tạo thông báo cá nhân cho người dùng khi hoàn tiền/giải ngân khiếu nại.

### 3.2. Services (Business Logic)

- **`ComplaintServiceImpl`** (Xử lý nghiệp vụ):
  - Inject `NotificationRepository` via `@Autowired`.
  - Gọi phương thức `walletService.recordTransaction` để ghi sổ cái:
    - Khi trạng thái khiếu nại đổi thành `Resolved` hoặc `Completed` (Đồng ý hoàn tiền tỷ lệ cho Buyer):
      - Tính toán số ngày sử dụng từ lúc mua tới lúc gửi khiếu nại. Phân bổ tiền hoàn `refundAmount` cho Buyer và phần còn lại giải ngân cho Seller.
      - Ghi giao dịch `REFUND` cho Buyer với tham chiếu `REFUND-CMP-{complaintId}-TX-{transactionId}`.
      - Ghi giao dịch `PAYMENT` cho Seller với tham chiếu `PAYOUT-CMP-RESOLVED-{complaintId}-TX-{transactionId}`.
      - Tạo và lưu `Notification` loại `WALLET`, severity `SUCCESS` gửi cho Buyer.
      - Tạo và lưu `Notification` loại `WALLET`, severity `SUCCESS` gửi cho Seller.
    - Khi trạng thái khiếu nại đổi thành `Rejected` (Từ chối khiếu nại, giải ngân toàn bộ cho Seller):
      - Ghi giao dịch `PAYMENT` (Payout) cho Seller với tham chiếu `PAYOUT-CMP-REJECTED-{complaintId}-TX-{transactionId}`.
      - Tạo và lưu `Notification` loại `WALLET`, severity `SUCCESS` gửi cho Seller.
      - Tạo và lưu `Notification` loại `WALLET`, severity `DANGER` gửi cho Buyer.

- **`WalletService`** (Thống kê số dư):
  - Trong phương thức `getWalletStats(...)`: Cộng thêm các giao dịch `REFUND` thành công vào `totalTopup` để thống kê khớp với số dư ví thực tế.

---

## 4. Các thành phần Frontend

- **`account-transactions.js`** (Modify):
  - Trong hàm `renderSummary(transactions)`: Cập nhật logic tính `summary.topup` bằng cách cộng thêm số tiền từ các giao dịch có loại là `REFUND` và trạng thái `SUCCESS`.

---

## 5. File Change Summary

| # | File | Type | Change | Description |
|---|------|------|--------|-------------|
| 1 | `ComplaintServiceImpl.java` | **[MODIFY]** | Tính hoàn tiền pro-rata, ghi giao dịch ví cho cả hai bên, và gửi Notification khi kết thúc khiếu nại. | Phân xử tài chính và tạo thông báo hệ thống |
| 2 | `WalletService.java` | **[MODIFY]** | Cập nhật logic tính `totalTopup` trong `getWalletStats` gồm cả `REFUND`. | Thống kê số dư chính xác ở Backend |
| 3 | `account-transactions.js` | **[MODIFY]** | Cập nhật logic tính `summary.topup` gồm cả `REFUND`. | Thống kê số dư chính xác ở Frontend |

---

## 6. Definition of Done

- Toàn bộ giao dịch hoàn tiền/từ chối phải được ghi nhận trong `WalletTransactions` (atomicity).
- Thông báo (Notification) phải được tạo và gửi đúng đối tượng.
- Thống kê "Tổng nạp" trên giao diện Lịch sử giao dịch phải bao gồm cả giao dịch hoàn tiền.
- Không còn lỗi biên dịch Maven (`mvn clean compile` thành công).
- Logging via SLF4J (không sử dụng `System.out.println` hay `printStackTrace`).
