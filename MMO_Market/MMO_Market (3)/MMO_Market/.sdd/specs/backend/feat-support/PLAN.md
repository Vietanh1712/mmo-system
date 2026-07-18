# PLAN — Support Ticket Management (`feat-support`)

## 1. Mục tiêu (Goals)

Triển khai luồng hỗ trợ kỹ thuật và giải quyết khiếu nại giao dịch thông qua các thẻ yêu cầu trợ giúp (Support Tickets) theo đặc tả `SPEC.md` (feat-support). Hệ thống cho phép:
- Người dùng (Customer/Seller) gửi các yêu cầu trợ giúp phân loại theo danh mục, theo dõi danh sách lịch sử và chi tiết phản hồi của hệ thống.
- Nhân viên vận hành (Staff/Admin) xem toàn bộ danh sách ticket hệ thống, phản hồi hướng xử lý (Resolution) và đóng/mở ticket.

## 2. Kiến trúc & Công nghệ

- **Backend:** Java 17, Spring Boot 3.1, Spring Data JPA, SQL Server (T-SQL).
- **Frontend:** Thymeleaf template engine, JS thuần gọi REST API (`authFetch` truyền Authorization JWT header).
- **Tuân thủ:** Mô hình phân lớp Controller → Service → Repository → Entity; DTO Pattern (đóng gói Request/Response dưới dạng Map hoặc DTO sạch); kiểm soát phân quyền chặt chẽ theo vai trò (Customer/Seller được tạo, Staff/Admin được duyệt).

## 3. Các thành phần Backend

### 3.1. Database Migration & Entities

- **Entity `SupportTicket`** (bảng `SupportTickets`):
  - `user` (ManyToOne -> User): Người tạo yêu cầu.
  - `category` (VARCHAR): Danh mục hỗ trợ (`TECHNICAL`, `TRANSACTION`, `ACCOUNT`, `OTHER`).
  - `title` (NVARCHAR): Tiêu đề ngắn gọn của vấn đề.
  - `description` (NVARCHAR): Chi tiết mô tả vấn đề gặp phải.
  - `status` (VARCHAR): Trạng thái ticket (`OPEN`, `RESOLVED`, `CLOSED`).
  - `resolution` (NVARCHAR): Nội dung phản hồi, xử lý từ Staff.
  - `isDelete` (BIT): Hỗ trợ soft delete.

### 3.2. Repositories (Spring Data JPA)

- **`SupportTicketRepository`**:
  - `findByUser_IdAndIsDeleteFalseOrderByCreatedAtDesc(userId)`: Lấy các ticket cá nhân của người dùng.
  - `findAllByIsDeleteFalseOrderByCreatedAtDesc()`: Lấy toàn bộ ticket hệ thống phục vụ Staff/Admin.
  - `findByIdAndIsDeleteFalse(id)`: Lấy chi tiết ticket.

### 3.3. DTOs

- Request: Map hoặc JSON object thô (chứa `category`, `title`, `description` khi tạo; `status`, `resolution` khi update).
- Response: Map chứa thông tin chi tiết ticket được định dạng phù hợp trước khi gửi ra ngoài (không lộ Entity).

### 3.4. Services (Business Logic)

- **`SupportTicketService`**:
  - `createTicket(userId, category, title, description)`:
    - Tìm kiếm User, kiểm tra sự tồn tại.
    - Lưu ticket mới với trạng thái ban đầu là `OPEN`.
  - `getUserTickets(userId)`: Lấy lịch sử ticket của người dùng.
  - `getAllTickets()`: Lấy toàn bộ ticket hệ thống (chỉ cho phép Staff/Admin).
  - `getTicketById(id)`: Lấy chi tiết ticket.
  - `updateTicketStatus(id, status, resolution)`:
    - Xác thực trạng thái mới hợp lệ (`OPEN`, `RESOLVED`, `CLOSED`).
    - Lưu nội dung xử lý của Staff và cập nhật trạng thái.

### 3.5. Controllers & Security

- **`SupportTicketController`** (`/api/support-tickets`):
  - `POST /`: Người dùng tạo ticket mới (chỉ role `CUSTOMER`, `SELLER`).
  - `GET /`: Người dùng xem lịch sử ticket (chỉ role `CUSTOMER`, `SELLER`).
  - `GET /all`: Staff/Admin xem danh sách ticket toàn hệ thống (`ROLE_STAFF`, `ROLE_ADMIN`).
  - `GET /{id}`: Xem chi tiết ticket (Yêu cầu kiểm tra quyền sở hữu: là chủ nhân ticket hoặc có role `STAFF`, `ADMIN`).
  - `PUT /{id}/status`: Cập nhật trạng thái và phản hồi giải pháp (`ROLE_STAFF`, `ROLE_ADMIN`).

---

## 4. Các thành phần Frontend

- **Màn hình Gửi Hỗ trợ & Lịch sử của Người dùng:**
  - File: `templates/account/tickets.html` và JS `static/js/customer/account-tickets.js`.
  - Hiển thị danh sách ticket cá nhân. Form tạo mới bằng Modal popup hoặc form riêng.
- **Màn hình Danh sách & Phê duyệt của Staff:**
  - File: `templates/staff/support-tickets.html` và JS `static/js/staff/staff-support-tickets.js`.
  - Màn hình chi tiết duyệt: `templates/staff/support-ticket-detail.html` và JS `static/js/staff/staff-support-ticket-detail.js` (Form nhập Resolution và các nút Resolved/Closed).

---

## 5. Definition of Done

- API xem chi tiết `/api/support-tickets/{id}` bắt buộc phải kiểm tra quyền sở hữu (Ownership Validation) để tránh lỗ hổng IDOR (không cho phép user A đọc ticket của user B).
- Chỉ những tài khoản có vai trò `STAFF` hoặc `ADMIN` mới có quyền truy cập endpoint `/all` và cập nhật status.

---

## 6. Cải tiến và Bản địa hóa Thuật ngữ (Phiên bản 1.1)

Thay đổi toàn bộ thuật ngữ hiển thị trên giao diện người dùng (Thymeleaf templates) và mã xử lý (JavaScript) từ "Ticket" / "Ticket hỗ trợ" sang "Phiếu hỗ trợ" để thống nhất ngôn ngữ tiếng Việt:
- **Sidebar & Menu:** Đổi "Ticket Hỗ Trợ" thành "Phiếu Hỗ Trợ" trên sidebar của Staff, và "Ticket của tôi" thành "Phiếu hỗ trợ của tôi" trên sidebar của Customer.
- **Giao diện Customer (`tickets.html`, `support.html`):** Thay đổi toàn bộ nhãn hiển thị, tiêu đề bảng, thông điệp phản hồi toast từ "Ticket" sang "Phiếu hỗ trợ".
- **Giao diện Staff (`support-tickets.html`, `support-ticket-detail.html`):** Việt hóa tiêu đề, breadcrumbs và các nhãn mã ticket thành "Phiếu hỗ trợ" và "Mã Phiếu".
- **API Backend (`SupportTicketController.java`, `SupportTicketService.java`):** Việt hóa các thông báo lỗi trả về từ API và các Exception message (như "Không tìm thấy phiếu hỗ trợ với ID").