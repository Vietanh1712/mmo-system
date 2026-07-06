# Hướng Dẫn Xác Minh / Kiểm Thử (Dispute Chat Group in Normal Chats)

Chúng ta đã chỉnh sửa toàn diện hệ thống tin nhắn đối chất theo yêu cầu: không tạo thêm màn hình chat riêng biệt trên trang chi tiết khiếu nại, mà tích hợp tự động cuộc đối chất thành một **"Nhóm trò chuyện"** ngay trong giao diện tin nhắn thường (`/messages`).

---

## 1. Các Thay Đổi Đã Thực Hiện

### Giao Diện Chi Tiết Khiếu Nại
- **Customer & Seller**: Xóa bỏ hoàn toàn khung chat riêng biệt ở trang Chi tiết khiếu nại để tránh trùng lặp tính năng. Thay vào đó hiển thị nút **"Nhắn tin đối chất thương lượng"** điều hướng trực tiếp sang trang Tin nhắn thường kèm ngữ cảnh: `/messages?complaintId={id}`.
- **Staff**: Giữ nguyên khung theo dõi đối chất Read-only tại trang quản lý khiếu nại của Staff để phục vụ công tác giám sát và ra phán quyết.

### Cơ Chế "Tự Tạo Nhóm Trò Chuyện" ở Tin Nhắn Thường
- **Sidebar danh sách chat**: Tự động hiển thị một liên hệ đặc biệt mang tên `Tranh chấp #CMP-{id} (Shop: ...)` hoặc `Tranh chấp #CMP-{id} (Khách: ...)` với nhãn ảnh đại diện `TC` cho tất cả các khiếu nại đang ở trạng thái `In_Progress`.
- **Hỗ trợ ID âm (`contactId < 0`)**: Đại diện cho mã nhóm đối chất khiếu nại để tách biệt luồng xử lý với các tài khoản người dùng thông thường (`contactId > 0`).
- **Phân vai tin nhắn**: Trong nhóm chat đối chất, tin nhắn hiển thị rõ vai trò gửi (`Khách hàng`, `Cửa hàng`, `Staff`) để các bên dễ đàm phán thương lượng.
- **Ràng buộc an toàn**: Khi chat nhóm đối chất, vô hiệu hóa các nút Chặn, Tắt thông báo, và Đính kèm tệp để đảm bảo tính minh bạch của quá trình tranh chấp.

---

## 2. Hướng Dẫn Các Bước Test Thực Tế

### **Bước 1: Khách hàng gửi khiếu nại**
1. Đăng nhập tài khoản **Customer**, gửi một khiếu nại mới từ trang chi tiết đơn hàng.
2. Trạng thái ban đầu của khiếu nại sẽ là `PENDING_REVIEW` (Chờ duyệt). Lúc này nút nhắn tin đối chất chưa hiển thị.

### **Bước 2: Staff kích hoạt cuộc đối chất**
1. Đăng nhập tài khoản **Staff**, xem chi tiết khiếu nại vừa tạo và nhấp nút **"Bắt đầu đối chất (Mở chat)"**. 
2. Trạng thái khiếu nại chuyển sang `In_Progress`.

### **Bước 3: Nhắn tin đối chất tại trang Tin nhắn thường**
1. **Customer**: Vào trang chi tiết khiếu nại -> nhấp nút **"Nhắn tin đối chất thương lượng"**. Trang web sẽ tự động chuyển hướng sang `/messages?complaintId={id}` và chọn nhóm đối chất `Tranh chấp #CMP-{id}` ở sidebar.
2. **Seller**: Vào Kênh người bán -> chi tiết khiếu nại -> nhấp nút **"Nhắn tin đối chất với Khách hàng"** để chuyển hướng tương tự.
3. Thử nhắn tin qua lại giữa hai bên trực tiếp trên giao diện chat bình thường. Tin nhắn sẽ tự động gửi và lưu kèm ngữ cảnh khiếu nại.
4. **Staff**: F5 lại trang chi tiết khiếu nại của nhân viên để kiểm tra lịch sử đối chất của 2 bên hiển thị đầy đủ ở chế độ Read-only.
