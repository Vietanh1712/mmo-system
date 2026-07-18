# PLAN — Complaint Refund Notification (`feat-complaint-refund`)

## 1. Mục tiêu (Goals)

Triển khai tính năng **ghi lại hoàn tiền từ khiếu nại vào lịch sử giao dịch và gửi thông báo hoàn tiền/giải ngân** cho người dùng. 
Đồng thời khắc phục các lỗi kỹ thuật phát sinh:
- Sửa lỗi biên dịch do gọi sai tên phương thức `recordTransactionWithReference` thành `recordTransaction` trong `ComplaintServiceImpl`.
- Gửi thông báo (Notification) tự động cho Buyer và Seller khi khiếu nại được giải quyết (Resolved - Hoàn tiền / Rejected - Giải ngân).
- Cập nhật thống kê "Tổng nạp" trong trang Lịch sử giao dịch (Backend & Frontend) để tính cả các giao dịch `REFUND` thành công.

## 2. Kiến trúc & Công nghệ

- **Backend:** Java 17, Spring Boot 3.1, Spring Data JPA, SQL Server (T-SQL).
- **Frontend:** HTML/CSS/JS (Thymeleaf thuần, vanilla JS).
- **Tuân thủ:** Mô hình phân lớp Controller → Service → Repository → Entity; `@Transactional` cho atomicity; Logging via SLF4J.

## 3. Các thành phần Backend

### 3.1. Database Migration & Entities

- **Entity `WalletTransaction`** (bảng `WalletTransactions`):
  - Sử dụng các cột đã migrate: `balance_after`, `transaction_type`, `reference_id`.
- **Entity `Notification`** (bảng `Notifications`):
  - Sử dụng bảng `Notifications` hiện tại để tạo thông báo cá nhân cho người dùng khi hoàn tiền/giải ngân khiếu nại.

### 3.2. Services (Business Logic)

- **`ComplaintServiceImpl`** (Modify):
  - Inject `NotificationRepository` via `@Autowired`.
  - Sửa lỗi gọi sai phương thức: Đổi từ `walletService.recordTransactionWithReference` thành `walletService.recordTransaction`.
  - Trong phương thức `updateComplaintStatus(...)`:
    - Khi trạng thái khiếu nại đổi thành `Resolved` (Đồng ý hoàn tiền cho Buyer):
      - Ghi giao dịch `REFUND` cho Buyer bằng `walletService.recordTransaction(...)`.
      - Tạo và lưu `Notification` loại `WALLET`, severity `SUCCESS` gửi cho Buyer thông báo được hoàn tiền.
      - Tạo và lưu `Notification` loại `WALLET`, severity `WARNING` gửi cho Seller thông báo đơn hàng bị hoàn tiền do khiếu nại.
    - Khi trạng thái khiếu nại đổi thành `Rejected` (Từ chối khiếu nại của Buyer, giải ngân cho Seller):
      - Ghi giao dịch `PAYMENT` (Payout) cho Seller bằng `walletService.recordTransaction(...)`.
      - Tạo và lưu `Notification` loại `WALLET`, severity `SUCCESS` gửi cho Seller thông báo tiền giải ngân đã vào ví.
      - Tạo và lưu `Notification` loại `WALLET`, severity `DANGER` gửi cho Buyer thông báo khiếu nại bị từ chối kèm lý do.

- **`WalletService`** (Modify):
  - Trong phương thức `getWalletStats(...)`: Cộng thêm các giao dịch `REFUND` thành công vào `totalTopup` để thống kê khớp với biến động số dư.

---

## 4. Các thành phần Frontend

- **`account-transactions.js`** (Modify):
  - Trong hàm `renderSummary(transactions)`: Cập nhật logic tính `summary.topup` bằng cách cộng thêm số tiền từ các giao dịch có loại là `REFUND` và trạng thái `SUCCESS`.

---

## 5. File Change Summary

| # | File | Type | Change | Description |
|---|------|------|--------|-------------|
| 1 | `ComplaintServiceImpl.java` | **[MODIFY]** | Inject `NotificationRepository`, sửa gọi tên hàm `recordTransaction`, tạo và lưu `Notification` khi Resolve/Reject khiếu nại. | Sửa lỗi biên dịch và tạo thông báo hệ thống |
| 2 | `WalletService.java` | **[MODIFY]** | Cập nhật logic tính `totalTopup` trong `getWalletStats` gồm cả `REFUND`. | Thống kê số dư chính xác ở Backend |
| 3 | `account-transactions.js` | **[MODIFY]** | Cập nhật logic tính `summary.topup` gồm cả `REFUND`. | Thống kê số dư chính xác ở Frontend |

---

## 6. Definition of Done

- Toàn bộ giao dịch hoàn tiền/từ chối phải được ghi nhận trong `WalletTransactions` (atomicity).
- Thông báo (Notification) phải được tạo và gửi đúng đối tượng (Buyer nhận được thông báo hoàn tiền, Seller nhận được thông báo giải ngân/bị hoàn tiền).
- Thống kê "Tổng nạp" trên giao diện Lịch sử giao dịch phải bao gồm cả giao dịch hoàn tiền.
- Không còn lỗi biên dịch Maven (`mvn clean compile` thành công).
- Logging via SLF4J (không sử dụng `System.out.println` hay `printStackTrace`).
