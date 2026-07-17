# PLAN — Direct Messaging, Block & Mute (`feat-chat`)

## 1. Mục tiêu (Goals)

Triển khai dịch vụ trò chuyện trực tiếp (Direct Messaging) giữa Người mua (Customer) và Người bán (Seller) nhằm hỗ trợ giao dịch, giải đáp thắc mắc và xử lý đơn hàng theo đặc tả `SPEC.md` (feat-chat). Các tính năng chính bao gồm:
- Gửi và nhận tin nhắn thời gian thực.
- Xem danh sách liên hệ gần đây (Chat Contacts) kèm trạng thái Online/Offline.
- Xóa lịch sử trò chuyện cục bộ (chỉ ẩn ở phía người xóa thông qua cờ xóa mềm).
- Tắt/Bật thông báo (Mute/Unmute) đối với từng cuộc trò chuyện.
- **Tích hợp Phòng đối chất Tranh chấp (Dispute / Complaint Chat Rooms):**
  - Tự động mở phòng chat đối chất 3 bên khi Staff duyệt chuyển khiếu nại sang trạng thái đối chất (`InProgress`).
  - Cho phép người mua và người bán trực tiếp gửi bằng chứng, đối thoại trong phòng chat này (được mã hóa dạng `contactId < 0`).
  - Cho phép Staff/Admin vào đọc toàn bộ lịch sử đối thoại (quyền Read-only) để đưa ra phán quyết khiếu nại.
- **Dịch vụ Chat Hỗ trợ của Nhân viên (Staff Chat Support):**
  - Cho phép Staff tìm kiếm người dùng và nhắn tin trực tiếp để giải quyết thắc mắc.

## 2. Kiến trúc & Công nghệ

- **Backend:** Java 17, Spring Boot 3.1, Spring Data JPA, SQL Server (T-SQL).
- **Frontend:** Thymeleaf template engine, CSS & JS thuần (gọi REST API bằng `authFetch`).
- **Tuân thủ:** Mô hình phân lớp Controller → Repository → Entity; DTO Pattern (đóng gói dữ liệu đầu ra, không trả JPA Entity trực tiếp); Quản lý trạng thái hoạt động online qua Memory Cache hoặc DB Active Time.

## 3. Các thành phần Backend

### 3.1. Database Migration & Entities

- **Entity `Chat`** (bảng `Chats`):
  - `sender` (ManyToOne -> User): Người gửi tin nhắn.
  - `receiver` (ManyToOne -> User): Người nhận tin nhắn.
  - `complaint` (ManyToOne -> Complaint): Tranh chấp liên kết (nếu thuộc phòng chat đối chất).
  - `productId` (Long): Mã sản phẩm ngữ cảnh (nếu người mua hỏi về sản phẩm cụ thể).
  - `chatType` (VARCHAR): Loại tin nhắn (`Normal` hoặc `Complaint`).
  - `isDelete` (BIT): Xóa mềm hệ thống.
  - `senderDeleted` & `receiverDeleted` (BIT): Đánh dấu xóa lịch sử từ phía gửi hoặc phía nhận (xóa độc lập).
- **Entity `ChatMute`** (bảng `ChatMutes`):
  - `user` (ManyToOne -> User): Người tắt thông báo.
  - `contact` (ManyToOne -> User): Liên hệ bị tắt thông báo.

### 3.2. Repositories (Spring Data JPA)

- `ChatRepository`:
  - Lấy toàn bộ chat liên quan đến người dùng để nhóm thành danh bạ gần đây.
  - `findActiveChatsBetweenUsers(user1, user2)`: Lấy toàn bộ tin nhắn chưa bị xóa giữa 2 người dùng.
  - `searchActiveChatsBetweenUsers(user1, user2, keyword)`: Tìm kiếm tin nhắn theo từ khóa.
- `ChatMuteRepository`:
  - `existsByUserAndContact(user, contact)`: Kiểm tra trạng thái tắt thông báo.
  - `findByUserAndContact(user, contact)`: Lấy bản ghi mute để bật lại thông báo.

### 3.3. DTOs

- Không sử dụng DTO phức tạp, thay vào đó đóng gói dữ liệu thành các cấu trúc Map an toàn (chỉ truyền ID, họ tên, avatar chữ cái đầu, tin nhắn cuối, thời gian, trạng thái online, mute).

### 3.4. Services (Business Logic)

- **`UserStatusService`** (tích hợp trong Admin/Chat):
  - `updateActiveTime(userId)`: Cập nhật thời gian hoạt động cuối cùng của người dùng.
  - `isOnline(userId)`: Xác định người dùng có đang online không (thời gian hoạt động cuối cách hiện tại dưới 5 phút).

### 3.5. Controllers & Security

- **`ChatController`** (`/api/v1/chats`):
  - `GET /`: Lấy danh sách danh bạ chat gần đây (gồm cả các phòng chat tranh chấp active dạng ID âm).
  - `GET /contact/{contactId}/info`: Lấy thông tin chi tiết liên hệ (họ tên, online/offline, avatar).
  - `GET /{contactId}`: Lấy lịch sử nhắn tin. Nếu `contactId < 0`, tự động tìm phòng đối chất tranh chấp tương ứng và đánh dấu đã đọc.
  - `POST /{contactId}`: Gửi tin nhắn mới.
    - Đối với chat đối chất (`contactId < 0`): Chỉ cho phép gửi khi trạng thái khiếu nại là `In_Progress` / `InProgress`, kiểm tra quyền tham gia của Customer và Seller.
  - `DELETE /{contactId}/history`: Xóa lịch sử chat phía mình.
  - `POST /{contactId}/mute` & `POST /{contactId}/unmute`: Tắt/Bật thông báo.
  - `GET /{contactId}/search`: Tìm kiếm tin nhắn theo từ khóa.

- **`StaffChatRestController`** (`/api/v1/staff/chat`):
  - `GET /conversations`: Lấy danh sách hội thoại của Staff (gồm cả phòng chat tranh chấp).
  - `GET /search`: Staff tìm kiếm người dùng để nhắn tin.
  - `GET /{targetUserId}`: Lấy lịch sử nhắn tin (hỗ trợ đọc phòng đối chất tranh chấp nếu targetUserId < 0).
  - `POST /{targetUserId}`: Staff nhắn tin cho người dùng.
    - *Ràng buộc:* Cấm Staff gửi tin nhắn vào phòng đối chất (`targetUserId < 0`), trả về lỗi 403 (Read-only).

---

## 4. Các thành phần Frontend

- **Trang tin nhắn của Người dùng (Customer/Seller):**
  - File: `templates/messages.html` và JS `static/js/chat/chat.js` (hoặc tương đương).
  - Sidebar bên trái hiển thị danh bạ chat gần đây kèm chấm xanh báo trạng thái Online.
  - Cửa sổ chat bên phải hiển thị nội dung tin nhắn dạng bong bóng thoại (`in` / `out`).
  - Menu drop-down trên thanh công cụ chat cho phép: Tắt thông báo, Xóa lịch sử trò chuyện.
- **Trang tin nhắn của Staff/Admin:**
  - File: `templates/staff/chat.html` và JS tương ứng. Cho phép quản lý hội thoại của nhân viên hỗ trợ.

## 5. Definition of Done

- Xóa lịch sử trò chuyện (`DELETE /{contactId}/history`) chỉ được đặt cờ `senderDeleted = true` hoặc `receiverDeleted = true`, không được xóa vật lý bản ghi trong DB để đảm bảo một bên xóa thì bên còn lại vẫn xem được đầy đủ lịch sử.
- Phòng chat tranh chấp (ID âm) chỉ khả dụng khi khiếu nại đang ở trạng thái `InProgress` / `In_Progress`.
- Nhân viên vận hành (Staff) chỉ được phép xem lịch sử cuộc đối chất (Read-only), cấm gửi tin nhắn vào phòng đối chất qua API của Staff.
- Toàn bộ các API chat được bảo vệ chặt chẽ thông qua `@AuthenticationPrincipal Long userId`.