# Walkthrough: Escrow Cycle, 3-way Dispute Chat & Financial Deduction Formula

Chúng ta đã hoàn thiện hệ thống xử lý khiếu nại và giam tiền (escrow) theo yêu cầu.

## Các Thay Đổi Đã Thực Hiện

### 1. Sửa Lỗi JS Nghiêm Trọng tại Trang Chi Tiết Khiếu Nại (Staff)
- **Tập tin**: `staff-complaint-detail.js`
- **Chi tiết**: Loại bỏ khai báo trùng lặp `statusVal`, `badge`, `badgeClass`, `statusText` làm vỡ JS trang Staff.
- **Tính năng mới**: Thiết lập cơ chế tự động tải lại (polling) tin nhắn đối chất mỗi 4 giây khi trạng thái khiếu nại đang là `In_Progress`.

### 2. Thêm Nút "Xem Phòng Chat Đối Chất" & Nâng Cấp Giao Diện Staff
- **Tập tin**: `staff/complaint-detail.html`
- **Chi tiết**: Thêm nút bấm trực quan để cuộn nhanh đến phòng chat đối chất 3 bên từ phần quyết định xử lý khiếu nại.
- **Hiển thị**: Phân biệt rõ vai trò gửi tin nhắn trong phòng chat:
  - Khách hàng (Customer) - nền xanh nhạt
  - Người bán (Seller) - nền cam nhạt
  - Hệ thống / Staff - nền xám nhạt

### 3. Hỗ Trợ Phòng Chat 3 Bên Từ Phía Người Bán (Seller)
- **Tập tin**: `seller/complaint-detail.html`, `seller-console.js`
- **Chi tiết**:
  - Nâng cấp giao diện chi tiết khiếu nại của Seller thành cấu trúc 2 cột.
  - Tích hợp khung chat đối chất giống như Customer.
  - Tự động tải lại tin nhắn đàm phán mỗi 4 giây.
  - Khóa form gửi tin nhắn và hiển thị thông báo đóng phòng chat một khi Staff đã ra phán quyết (`Resolved` hoặc `Rejected`).

---

## Hướng Dẫn Xác Minh / Kiểm Thử (Manual Verification)

1. **Khởi động ứng dụng**:
   - Chạy Spring Boot backend.
2. **Kích hoạt phòng chat 3 bên**:
   - Đăng nhập tài khoản **Staff**.
   - Vào mục **Khiếu nại**, chọn một khiếu nại ở trạng thái `PENDING_REVIEW` (Chờ duyệt).
   - Nhấp nút **"Bắt đầu đối chất (Mở chat)"** để kích hoạt trạng thái `In_Progress`.
3. **Đàm phán đối chất**:
   - Đăng nhập tài khoản **Customer**, vào trang chi tiết khiếu nại → Nhập tin nhắn gửi đi.
   - Đăng nhập tài khoản **Seller**, vào trang chi tiết khiếu nại → Xem tin nhắn đối chất và nhập tin nhắn phản hồi.
   - Kiểm tra xem cả hai bên có nhận được tin nhắn của nhau theo thời gian thực (realtime polling 4s) hay không.
4. **Phán quyết của Staff & Khấu trừ tài chính**:
   - Đăng nhập tài khoản **Staff**, xem cuộc hội thoại đối chất của 2 bên trực tiếp từ chi tiết khiếu nại (chế độ Read-only).
   - Staff đưa ra phán quyết:
     - Chọn **Giải quyết** (Resolved) → Hệ thống tự động hoàn tiền cho khách hàng theo công thức tỷ lệ số ngày chưa sử dụng và trừ tiền trực tiếp từ ví của Seller.
     - Chọn **Từ chối** (Rejected) → Giải ngân 100% tiền đơn hàng cho Seller.
