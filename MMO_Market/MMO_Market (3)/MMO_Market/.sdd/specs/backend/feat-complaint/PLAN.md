# PLAN — Complaint Management (`feat-complaint`)

## 1. Mục tiêu (Goals)

Triển khai quy trình khiếu nại đơn hàng (Complaints / Dispute) theo đặc tả `SPEC.md` (feat-complaint). Hệ thống cho phép:
- Người mua (Customer) gửi khiếu nại đối với đơn hàng số gặp lỗi trong thời gian bảo lãnh Escrow 72 giờ. Khi đó, hệ thống sẽ tạm hoãn việc giải ngân cho Người bán (Seller) và đóng băng số tiền giao dịch.
- Người bán (Seller) và Người mua (Customer) trao đổi bằng chứng, thông tin chi tiết.
- Nhân viên vận hành (Staff/Admin) phân xử khiếu nại, đưa ra giải pháp (Resolution), thực hiện hoàn tiền lại cho Người mua hoặc giải ngân cho Người bán và đóng ticket khiếu nại.

## 2. Kiến trúc & Công nghệ

- **Backend:** Java 17, Spring Boot 3.1, Spring Data JPA, SQL Server (T-SQL).
- **Frontend:** Giao diện Thymeleaf, gọi REST API bất đồng bộ qua `authFetch` đính kèm Token JWT.
- **Tuân thủ:** Mô hình phân lớp Controller → Service → Repository → Entity; DTO Pattern cho Request/Response; Kiểm tra quyền hạn nghiêm ngặt (chỉ có bên mua mới được khiếu nại, Staff/Admin mới được quyền phân xử).

## 3. Các thành phần Backend

### 3.1. Database Migration & Entities

- **Entity `Complaint`** (bảng `Complaints`):
  - `transaction` (ManyToOne -> Transaction): Đơn hàng/Giao dịch bị khiếu nại.
  - `customer` (ManyToOne -> User): Người mua tạo khiếu nại.
  - `seller` (ManyToOne -> User): Người bán bị khiếu nại.
  - `description` (NVARCHAR): Chi tiết mô tả lỗi sản phẩm.
  - `evidence` (NVARCHAR): Đường dẫn hình ảnh/video bằng chứng (chứng minh lỗi).
  - `status` (VARCHAR): Trạng thái khiếu nại (`OPEN`, `RESOLVED`, `CLOSED`).
  - `resolution` (NVARCHAR): Kết luận và hướng phân xử của Staff.
  - `isDelete` (BIT): Soft delete flag.

### 3.2. Repositories (Spring Data JPA)

- **`ComplaintRepository`**:
  - `findByCustomer_IdAndIsDeleteFalseOrderByCreatedAtDesc(customerId)`: Danh sách khiếu nại của người mua.
  - `findByTransaction_IdAndIsDeleteFalse(transactionId)`: Tìm khiếu nại theo đơn hàng.
  - `findAllByIsDeleteFalseOrderByCreatedAtDesc()`: Lấy toàn bộ khiếu nại hệ thống phục vụ Staff.

### 3.3. DTOs

- Request: Đóng gói các trường `transactionId`, `description`, `evidence`.
- Response: Map hoặc DTO chứa thông tin chi tiết khiếu nại, thông tin đơn hàng và thông tin liên hệ của hai bên.

### 3.4. Services (Business Logic)

- **`ComplaintService`**:
  - `createComplaint(customerId, transactionId, description, evidence)`:
    - Tìm và kiểm tra Transaction có tồn tại và thuộc sở hữu của Customer không.
    - Kiểm tra xem giao dịch có nằm trong thời gian 72h Escrow bảo lãnh không (nếu đã quá 72h và đã giải ngân thì không cho phép tạo khiếu nại).
    - Cập nhật trạng thái Transaction sang `Disputed` (Đóng băng dòng tiền không cho tự động giải ngân).
    - Lưu bản ghi `Complaint` với trạng thái `OPEN`.
  - `updateComplaintStatus(id, status, resolution, flagLevel, flagReason)`:
    - Chỉ cho phép Staff/Admin thực hiện.
    - Cập nhật nội dung phân xử `resolution`.
    - Nếu phân xử hoàn tiền (Refund): Trả lại tiền từ Escrow về `availableBalance` của Customer.
    - Nếu phân xử từ chối khiếu nại: Thực hiện giải ngân số tiền đơn hàng từ Escrow sang `availableBalance` của Seller.
    - Nếu Staff/Admin chọn gắn cờ người bán: tạo và lưu bản ghi `ShopFlag` với `flagLevel` và `flagReason` tương ứng.
    - Đổi trạng thái khiếu nại sang `RESOLVED` hoặc `CLOSED`.

### 3.5. Controllers & Security

- **`ComplaintController`** (`/api/complaints`):
  - `POST /`: Người mua tạo khiếu nại (`@PreAuthorize("hasRole('CUSTOMER')")`).
  - `GET /`: Người mua xem lịch sử khiếu nại cá nhân (`@PreAuthorize("hasRole('CUSTOMER')")`).
  - `GET /all`: Staff/Admin xem danh sách khiếu nại toàn sàn (`@PreAuthorize("hasAnyAuthority('ROLE_STAFF', 'ROLE_ADMIN')")`).
  - `GET /{id}`: Xem chi tiết (Yêu cầu ownership hoặc có quyền Staff/Admin).
  - `PUT /{id}/status`: Staff cập nhật trạng thái phân xử kèm theo thông tin gắn cờ shop (`@PreAuthorize("hasAnyAuthority('ROLE_STAFF', 'ROLE_ADMIN')")`).
  - `GET /{id}/chats`: Trả về tin nhắn chat khiếu nại (Quyền: Buyer, Seller, Staff/Admin).
  - `POST /{id}/chats`: Cho phép gửi tin nhắn chat khiếu nại (Quyền: Buyer, Seller. Chặn Staff/Admin gửi).

---

## 4. Các thành phần Frontend

- **Màn hình khiếu nại của Người mua:**
  - File: `templates/account/order-detail.html`. Hiển thị thêm hộp thoại Chat Tranh Chấp cho phép nhắn tin thương lượng trực tiếp với Seller khi trạng thái đơn là `DISPUTED`.
- **Màn hình phân xử của Staff:**
  - File: `templates/staff/complaint-detail.html` và JS `static/js/staff/staff-complaint-detail.js`.
  - Staff xem mô tả lỗi, xem bằng chứng và **xem lịch sử cuộc trò chuyện thương lượng giữa Buyer và Seller** (chế độ Read-only).
  - Staff chọn phương án xử lý (Hoàn tiền / Giải ngân) và tick chọn gắn cờ cảnh cáo shop người bán (`ShopFlag`).

---

## 5. Definition of Done

- API chi tiết khiếu nại `/api/complaints/{id}` bắt buộc phải kiểm tra quyền hạn (phải là bên mua, bên bán hoặc Staff) để tránh IDOR.
- Việc đổi trạng thái sang `RESOLVED` / `CLOSED` phải đi kèm với hành động hoàn tiền hoặc giải ngân ví tương ứng sử dụng `Pessimistic Lock` để bảo toàn số dư.
- Chỉ cho phép tạo khiếu nại trong thời hạn bảo lãnh 72 giờ của Escrow.