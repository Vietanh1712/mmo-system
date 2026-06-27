# PLAN — Direct Messaging, Block & Mute (`feat-chat`)

## 1. Mục tiêu (Goals)

Triển khai dịch vụ trò chuyện trực tiếp (Direct Messaging) giữa Người mua (Customer) và Người bán (Seller) nhằm hỗ trợ giao dịch, giải đáp thắc mắc và xử lý đơn hàng theo đặc tả `SPEC.md` (feat-chat). Các tính năng chính bao gồm:
- Gửi và nhận tin nhắn thời gian thực.
- Xem danh sách liên hệ gần đây (Chat Contacts) kèm trạng thái Online/Offline.
- Xóa lịch sử trò chuyện cục bộ (chỉ ẩn ở phía người xóa thông qua cờ xóa mềm).
- Chặn/Mở chặn tài khoản (Block/Unblock) để dừng nhận tin nhắn từ đối phương.
- Tắt/Bật thông báo (Mute/Unmute) đối với từng cuộc trò chuyện.

## 2. Kiến trúc & Công nghệ

- **Backend:** Java 17, Spring Boot 3.1, Spring Data JPA, SQL Server (T-SQL).
- **Frontend:** Thymeleaf template engine, CSS & JS thuần (gọi REST API bằng `authFetch`).
- **Tuân thủ:** Mô hình phân lớp Controller → Repository → Entity; DTO Pattern (đóng gói dữ liệu đầu ra, không trả JPA Entity trực tiếp); Quản lý trạng thái hoạt động online qua Memory Cache hoặc DB Active Time.

## 3. Các thành phần Backend

### 3.1. Database Migration & Entities

- **Entity `Chat`** (bảng `Chats`):
  - `sender` (ManyToOne -> User): Người gửi tin nhắn.
  - `receiver` (ManyToOne -> User): Người nhận tin nhắn.
  - `message` (NVARCHAR): Nội dung tin nhắn.
  - `chatType` (VARCHAR): Loại tin nhắn (mặc định `Normal`).
  - `isDelete` (BIT): Xóa mềm hệ thống.
  - `senderDeleted` & `receiverDeleted` (BIT): Đánh dấu xóa lịch sử từ phía gửi hoặc phía nhận (xóa độc lập).
- **Entity `ChatBlock`** (bảng `ChatBlocks`):
  - `blocker` (ManyToOne -> User): Người thực hiện chặn.
  - `blocked` (ManyToOne -> User): Người bị chặn.
- **Entity `ChatMute`** (bảng `ChatMutes`):
  - `user` (ManyToOne -> User): Người tắt thông báo.
  - `contact` (ManyToOne -> User): Liên hệ bị tắt thông báo.

### 3.2. Repositories (Spring Data JPA)

- `ChatRepository`:
  - Lấy toàn bộ chat liên quan đến người dùng để nhóm thành danh bạ gần đây.
  - `findActiveChatsBetweenUsers(user1, user2)`: Lấy toàn bộ tin nhắn chưa bị xóa giữa 2 người dùng.
  - `searchActiveChatsBetweenUsers(user1, user2, keyword)`: Tìm kiếm tin nhắn theo từ khóa.
- `ChatBlockRepository`:
  - `existsByBlockerAndBlocked(blocker, blocked)`: Kiểm tra trạng thái chặn.
  - `findByBlockerAndBlocked(blocker, blocked)`: Lấy bản ghi chặn để hủy chặn.
- `ChatMuteRepository`:
  - `existsByUserAndContact(user, contact)`: Kiểm tra trạng thái tắt thông báo.
  - `findByUserAndContact(user, contact)`: Lấy bản ghi mute để bật lại thông báo.

### 3.3. DTOs

- Không sử dụng DTO phức tạp, thay vào đó đóng gói dữ liệu thành các cấu trúc Map an toàn (chỉ truyền ID, họ tên, avatar chữ cái đầu, tin nhắn cuối, thời gian, trạng thái online, block, mute).

### 3.4. Services (Business Logic)

- **`UserStatusService`** (tích hợp trong Admin/Chat):
  - `updateActiveTime(userId)`: Cập nhật thời gian hoạt động cuối cùng của người dùng.
  - `isOnline(userId)`: Xác định người dùng có đang online không (thời gian hoạt động cuối cách hiện tại dưới 5 phút).

### 3.5. Controllers & Security

- **`ChatController`** (`/api/v1/chats`):
  - `GET /`: Lấy danh sách danh bạ chat gần đây.
  - `GET /{contactId}`: Lấy lịch sử nhắn tin.
  - `POST /{contactId}`: Gửi tin nhắn mới. Kiểm tra xem người gửi có chặn người nhận hoặc người nhận có chặn người gửi không. Nếu có chặn thì từ chối gửi tin nhắn.
  - `DELETE /{contactId}/history`: Xóa lịch sử chat phía mình.
  - `POST /{contactId}/block` & `POST /{contactId}/unblock`: Chặn/Mở chặn.
  - `POST /{contactId}/mute` & `POST /{contactId}/unmute`: Tắt/Bật thông báo.
  - `GET /{contactId}/search`: Tìm kiếm tin nhắn theo từ khóa.

---

## 4. Các thành phần Frontend

- **Trang tin nhắn của Người dùng (Customer/Seller):**
  - File: `templates/messages.html`.
  - Sidebar bên trái hiển thị danh bạ chat gần đây kèm chấm xanh báo trạng thái Online.
  - Cửa sổ chat bên phải hiển thị nội dung tin nhắn dạng bong bóng thoại (`in` / `out`).
  - Menu drop-down trên thanh công cụ chat cho phép: Chặn, Tắt thông báo, Xóa lịch sử trò chuyện.

---

## 5. Definition of Done

- Hệ thống bắt buộc phải kiểm tra trạng thái chặn 2 chiều trước khi cho phép lưu và gửi tin nhắn mới.
- Xóa lịch sử trò chuyện (`DELETE /{contactId}/history`) chỉ được đặt cờ `senderDeleted = true` hoặc `receiverDeleted = true`, không được xóa vật lý bản ghi trong DB để đảm bảo một bên xóa thì bên còn lại vẫn xem được đầy đủ lịch sử.
- Toàn bộ các API chat được bảo vệ chặt chẽ thông qua `@AuthenticationPrincipal Long userId`.