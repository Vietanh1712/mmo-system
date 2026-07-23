# PLAN — Complaint Management (`feat-complaint`)

## 1. Mục tiêu (Goals)

Triển khai quy trình khiếu nại đơn hàng (Complaints / Dispute) theo đặc tả `SPEC.md` (feat-complaint). Hệ thống cho phép:
- Người mua (Customer) gửi khiếu nại đối với đơn hàng số gặp lỗi trong thời hạn bảo lãnh của Escrow. Khi đó, hệ thống sẽ tạm hoãn việc giải ngân cho Người bán (Seller) và đóng băng số tiền giao dịch (status = `Disputed`).
- Người bán (Seller) và Người mua (Customer) trao đổi bằng chứng, thông tin đối chất.
- Nhân viên vận hành (Staff/Admin) phân xử khiếu nại, đưa ra giải pháp (Resolution), thực hiện hoàn tiền pro-rata theo tỷ lệ sử dụng hoặc giải ngân toàn bộ cho Người bán, gắn cờ cảnh cáo nếu Shop sai phạm và cập nhật trạng thái kết thúc cuộc đối chất.

---

## 2. Kiến trúc & Công nghệ

- **Backend:** Java 17, Spring Boot 3.1, Spring Data JPA, SQL Server (T-SQL).
- **Frontend:** Giao diện Thymeleaf, gọi REST API bất đồng bộ qua `fetch` đính kèm Token JWT.
- **Tuân thủ:** Mô hình phân lớp Controller → Service → Repository → Entity; DTO Pattern cho Request/Response; Kiểm tra quyền hạn nghiêm ngặt (chỉ có bên mua mới được khiếu nại, Staff/Admin mới được quyền phân xử).

---

## 3. Các thành phần Backend

### 3.1. Database Model & Entity (`Complaint`)

- **Bảng `Complaints`** lưu thông tin:
  - `transaction` (ManyToOne -> Transaction): Đơn hàng/Giao dịch bị khiếu nại.
  - `customer` (ManyToOne -> User): Người mua tạo khiếu nại.
  - `seller` (ManyToOne -> User): Người bán bị khiếu nại.
  - `description` (NVARCHAR): Chi tiết mô tả lỗi sản phẩm.
  - `evidence` (NVARCHAR): Đường dẫn bằng chứng (Bắt buộc).
  - `status` (VARCHAR): Trạng thái khiếu nại (`Open`, `PENDING_REVIEW`, `In_Progress`, `Resolved`, `Rejected`, `Completed`).
  - `preferredSolution` (VARCHAR): Giải pháp mong muốn (`REPLACEMENT`, `REFUND`).
  - `resolution` (NVARCHAR): Kết luận và hướng phân xử của Staff.
  - `resolvedBy` (ManyToOne -> User): Nhân viên xử lý khiếu nại.
  - `resolvedAt` (LocalDateTime): Thời điểm phân xử.
  - `isDelete` (BIT): Soft delete.

### 3.2. Repositories (Spring Data JPA)

- **`ComplaintRepository`**:
  - `findByCustomerAndIsDeleteFalseOrderByCreatedAtDesc(customer)`: Danh sách khiếu nại của người mua.
  - `findBySellerAndIsDeleteFalseOrderByCreatedAtDesc(seller)`: Danh sách khiếu nại chống lại Shop.
  - `findFirstByTransactionIdAndIsDeleteFalseOrderByIdDesc(transactionId)`: Tìm khiếu nại của đơn hàng.
  - `findAllByIsDeleteFalseOrderByCreatedAtDesc()`: Lấy toàn bộ khiếu nại hệ thống phục vụ Staff.

### 3.3. DTOs

- Request: Đóng gói các trường `transactionId`, `description`, `evidence`, `preferredSolution` (gửi lên qua JSON).
- Response: Map hoặc DTO chứa thông tin chi tiết khiếu nại (`ComplaintDTO`), thông tin giao dịch liên quan và thông tin hai bên.

### 3.4. Services (Business Logic)

- **`ComplaintService`**:
  - `createComplaint(customerId, transactionId, description, evidence, preferredSolution)`:
    - Tìm và kiểm tra Transaction có tồn tại và thuộc sở hữu của Customer không.
    - Kiểm tra xem giao dịch đã ở trạng thái `Disputed`, `Cancelled` hay `Refunded` chưa.
    - Cập nhật trạng thái Transaction sang `Disputed` để đóng băng dòng tiền.
    - Lưu bản ghi `Complaint` với trạng thái `PENDING_REVIEW` và gửi thông báo cho Buyer, Seller, Staff.
  - `startDispute(complaintId, staffId)`:
    - Chuyển trạng thái khiếu nại sang `In_Progress` và tự động gửi tin nhắn hệ thống kích hoạt phòng chat.
  - `updateComplaintStatus(id, status, resolution, flagLevel, flagReason, staffId)`:
    - Cập nhật nội dung phân xử `resolution`, nhân viên xử lý và `resolvedAt`.
    - Tạo `ShopFlag` gắn cờ cảnh cáo nếu `flagLevel` khác "None".
    - Nếu phân xử hoàn tiền (status là `Resolved` hoặc `Completed`): Tính toán hoàn tiền pro-rata theo tỷ lệ số ngày chưa sử dụng. Hoàn tiền cho Buyer (loại giao dịch `REFUND`) và giải ngân phần còn lại cho Seller (loại giao dịch `PAYMENT`).
    - Nếu phân xử từ chối khiếu nại (status là `Rejected`): Thực hiện giải ngân toàn bộ số tiền (sau khi trừ hoa hồng) cho ví Seller (loại giao dịch `PAYMENT`).
    - Gọi `ShopLevelService.evaluateSellerLevel(sellerId)` để tự động đánh giá lại cấp độ shop.

### 3.5. Controllers & Security

- **`ComplaintController`** (`/api/complaints`):
  - `POST /`: Người mua tạo khiếu nại.
  - `GET /`: Người mua xem lịch sử khiếu nại cá nhân.
  - `GET /all`: Staff/Admin xem danh sách khiếu nại toàn sàn (phân trang & từ khóa).
  - `GET /{id}`: Xem chi tiết (Yêu cầu ownership hoặc quyền Staff/Admin).
  - `POST /{id}/start-dispute`: Staff/Admin mở phòng đối chất.
  - `PUT /{id}/status`: Staff cập nhật trạng thái phân xử kèm theo thông tin gắn cờ shop.
  - `GET /{id}/chats`: Trả về tin nhắn chat đối chất.
  - `POST /{id}/chats`: Gửi tin nhắn chat đối chất (chỉ cho phép Buyer và Seller gửi tin nhắn, Staff ở quyền Read-only).

---

## 4. Các thành phần Frontend

- **Màn hình khiếu nại của Người mua:**
  - File: `templates/account/order-detail.html`. Hiển thị thêm hộp thoại Chat Tranh Chấp cho phép nhắn tin thương lượng trực tiếp với Seller khi trạng thái đơn là `DISPUTED`.
- **Màn hình phân xử của Staff:**
  - File: `templates/staff/complaints.html` & `complaint-detail.html` và JS `static/js/staff/staff-complaints.js`, `staff-complaint-detail.js`.
  - Staff xem mô tả lỗi, xem bằng chứng và xem lịch sử cuộc trò chuyện thương lượng giữa Buyer và Seller (chế độ Read-only).
  - Staff chọn phương án xử lý phân quyết khiếu nại (InProgress - Đang xử lý, Resolved - Chấp nhận/Hoàn tiền, Rejected - Từ chối/Giải ngân), nhập ghi chú giải quyết (`resolution`), và có thể tích chọn gắn cờ phạt shop vi phạm (`ShopFlag`).

---

## 5. Definition of Done

- API chi tiết khiếu nại `/api/complaints/{id}` bắt buộc phải kiểm tra quyền hạn (phải là bên mua, bên bán hoặc Staff) để tránh IDOR.
- Việc đổi trạng thái sang `Resolved` / `Completed` / `Rejected` phải đi kèm với hành động hoàn tiền hoặc giải ngân ví tương ứng sử dụng `@Transactional` để bảo toàn số dư.
- Chỉ cho phép tạo khiếu nại trong thời hạn bảo lãnh của Escrow.