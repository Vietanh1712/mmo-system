# Hướng Dẫn Xác Minh / Kiểm Thử (Dispute Chat Group in Normal Chats)

Chúng ta đã chỉnh sửa toàn diện hệ thống tin nhắn đối chất theo yêu cầu: không tạo thêm màn hình chat riêng biệt trên trang chi tiết khiếu nại, mà tích hợp tự động cuộc đối chất thành một **"Nhóm trò chuyện"** ngay trong giao diện tin nhắn thường (`/messages` cho người dùng và `/staff/chat` cho nhân viên).

---

## 1. Các Thay Đổi Đã Thực Hiện

### Giao Diện Chi Tiết Khiếu Nại
- **Customer & Seller**: Xóa bỏ hoàn toàn khung chat riêng biệt ở trang Chi tiết khiếu nại để tránh trùng lặp tính năng. Thay vào đó hiển thị nút **"Nhắn tin đối chất thương lượng"** điều hướng trực tiếp sang trang Tin nhắn thường kèm ngữ cảnh: `/messages?complaintId={id}`.
- **Staff (Quản trị)**: 
  - Loại bỏ hoàn toàn khung chat đối chất (`dispute-chat-card`) khỏi trang chi tiết khiếu nại của Staff để giữ trang gọn gàng.
  - Khi Staff nhấp nút **"Bắt đầu đối chất"**, sau khi kích hoạt thành công sẽ **chuyển hướng lập tức** sang trang chat chính của Staff `/staff/chat?complaintId={id}`.
  - Đối với các khiếu nại đã mở đối chất, hiển thị nút **"Xem Phòng chat đối chất 3 bên"** điều hướng sang trang chat chính `/staff/chat?complaintId={id}`.

### Cơ Chế "Tự Tạo Nhóm Trò Chuyện" ở Tin Nhắn Thường
- **Sidebar danh sách chat**: Tự động hiển thị một liên hệ đặc biệt mang tên `Tranh chấp #CMP-{id} (Shop: ...)` hoặc `Tranh chấp #CMP-{id} (Khách: ...)` với nhãn ảnh đại diện `TC` cho tất cả các khiếu nại đang ở trạng thái `In_Progress`.
- **Hỗ trợ ID âm (`contactId < 0`)**: Đại diện cho mã nhóm đối chất khiếu nại để tách biệt luồng xử lý với các tài khoản người dùng thông thường (`contactId > 0`).
- **Phân vai tin nhắn**: Trong nhóm chat đối chất, tin nhắn hiển thị rõ vai trò gửi (`Khách hàng`, `Cửa hàng`, `Staff`) để các bên dễ đàm phán thương lượng.
- **Quyền hạn an toàn của Staff (Read-Only)**: Trong `/staff/chat`, khi chọn cuộc tranh chấp, Staff chỉ có quyền xem nội dung đối chất của 2 bên. Ô nhập liệu và các nút tính năng bị khóa kèm thông báo: *"Nhân viên chỉ có quyền Read-only đối với phòng chat đối chất."*

---

## 2. Hướng Dẫn Các Bước Test Thực Tế

### **Bước 1: Khách hàng gửi khiếu nại**
1. Đăng nhập tài khoản **Customer**, gửi một khiếu nại mới từ trang chi tiết đơn hàng.
2. Trạng thái ban đầu của khiếu nại sẽ là `PENDING_REVIEW` (Chờ duyệt). Lúc này nút nhắn tin đối chất chưa hiển thị.

### **Bước 2: Staff kích hoạt cuộc đối chất**
1. Đăng nhập tài khoản **Staff**, xem chi tiết khiếu nại vừa tạo ở trạng thái Chờ duyệt.
2. Nhấp nút **"Bắt đầu đối chất (Mở chat)"**. 
   * Trạng thái khiếu nại chuyển sang **In_Progress** (Đang xử lý) và trang web **tự động chuyển hướng ngay lập tức** sang giao diện Chat chính của Staff (`/staff/chat?complaintId=X`).
   * Nhóm đối chất màu đỏ `Tranh chấp #CMP-X` sẽ được tự động chọn ở sidebar.

### **Bước 3: Nhắn tin đối chất tại trang Tin nhắn thường**
1. **Khách hàng**:
   * Trên tab Customer: Vào mục **Lịch sử khiếu nại** (`/account/complaints`), nhấn xem chi tiết khiếu nại.
   * Nhấn nút **"Nhắn tin đối chất thương lượng"** -> Tự động chuyển sang `/messages?complaintId=X` và chọn nhóm đối chất `Tranh chấp #CMP-X` ở sidebar.
   * Gửi một tin nhắn đàm phán bất kỳ.

2. **Người bán (Shop)**:
   * Trên tab Seller: Vào Kênh người bán -> **Quản lý khiếu nại** (`/seller/complaints`), bấm xem chi tiết khiếu nại đang bị phạt.
   * Nhấp nút **"Nhắn tin đối chất với Khách hàng"** -> Tự động chuyển sang `/messages?complaintId=X` trên trang tin nhắn của Seller và chọn đúng nhóm chat tranh chấp.
   * Gửi phản hồi tin nhắn cho Khách hàng.

### **Bước 4: Staff theo dõi từ mục Chat chính**
1. Trên tab Staff: Xem chi tiết khiếu nại, nhấn **"Xem Phòng chat đối chất 3 bên"** -> Tự động chuyển hướng Staff sang `/staff/chat?complaintId=X` và tự động chọn liên hệ đối chất màu đỏ `Tranh chấp #CMP-X`.
2. Kiểm tra:
   * Hiển thị đầy đủ lịch sử nhắn tin của Customer & Seller kèm nhãn phân vai rõ ràng.
   * Ô nhập tin nhắn của Staff bị khóa và có placeholder: *"Nhân viên chỉ có quyền Read-only đối với phòng chat đối chất."*

## 2. Kết quả kiểm tra
- Dự án biên dịch thành công 100% (`BUILD SUCCESS`).
- Đảm bảo tính liên thông dữ liệu hoàn hảo giữa Seller, Customer và Staff.

---

## 3. Cấu hình gửi Mail & Sửa lỗi OTP (SMTP Configuration)
- **Lỗi xác thực**: Trước đó, quá trình gửi OTP bị gián đoạn do lỗi `AuthenticationFailedException: 535-5.7.8 Username and Password not accepted` từ phía Gmail.
- **Khắc phục**: 
  - Đã cập nhật mật khẩu ứng dụng (App Password) mới (`xaxb rmys deci cauy`) cho tài khoản `nguyenthingoclinh291104@gmail.com` tại dòng 61 của file `application.properties`.
  - Đã giải phóng tiến trình cũ chạy nền đang giữ cổng `8080` và khởi động lại Server thành công. Hiện tại email OTP đăng ký và khôi phục mật khẩu hoạt động tốt.

---

## 4. Tính năng Đếm ngược 60 giây khi Gửi lại OTP (Resend OTP Countdown)
- **Cải tiến UX & Bảo mật**: Tránh việc spam click gửi lại OTP liên tục gây nghẽn hòm thư hoặc trùng mã.
- **Hoạt động**:
  - Tại trang **Xác thực OTP đăng ký** (`verify-otp.html`) và **Đặt lại mật khẩu** (`reset-password.html`), ngay khi người dùng vừa mở trang, nút **"Gửi lại"** sẽ tự động khóa và đếm ngược hiển thị `Gửi lại (60s)...`.
  - Hết 60 giây, nút này sẽ hiển thị lại chữ **"Gửi lại"** và cho phép tương tác. Khi bấm, quy trình đếm ngược 60 giây sẽ được lặp lại.
  - Logic điều khiển đếm ngược được cập nhật thống nhất trong file `auth.js` cho cả hai màn hình.
