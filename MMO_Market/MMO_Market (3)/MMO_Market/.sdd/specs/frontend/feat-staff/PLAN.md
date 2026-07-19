# PLAN — Staff Console Interface (`feat-staff`)

## Templates
- `apps/frontend/templates/staff/kyc.html` — Danh sách yêu cầu KYC
- `apps/frontend/templates/staff/kyc-detail.html` — Chi tiết KYC (v1.1: bổ sung full_name, email, date_of_birth, address, rejection_reason)
- `apps/frontend/templates/staff/withdrawals.html`
- `apps/frontend/templates/staff/complaints.html`
- `apps/frontend/templates/staff/transactions.html` — Giao dịch toàn sàn (v1.1: bộ lọc ngày rút gọn còn 1 trường `fromDate`; v1.3: load động trạng thái từ database và Việt hóa nhãn hiển thị)
- `apps/frontend/templates/staff/shop-registrations.html` — Quản lý yêu cầu mở Shop (v2.0: đồng bộ hóa giao diện gồm thống kê, bộ lọc từ khóa/trạng thái, phân trang; v2.3: loại bỏ Modal review)
- `apps/frontend/templates/staff/shop-registration-detail.html` [NEW] — Chi tiết đăng ký Shop (v2.3: trang chi tiết riêng biệt thay thế Modal review, tương tự như KYC detail)

## JS Scripts
- `apps/frontend/static/js/staff/staff-kyc.js`
- `apps/frontend/static/js/staff/staff-kyc-detail.js` — (v1.1: bind fullName, email, dateOfBirth, address, rejectionReason từ API)
- `apps/frontend/static/js/staff/staff-ui.js`
- `apps/frontend/static/js/staff/staff-chat.js`
- `apps/frontend/static/js/staff/staff-shop-registrations.js` — (v2.0: gọi API thống kê & tìm kiếm phân trang mới; v2.3: đổi nút xem chi tiết thành link điều hướng, loại bỏ modal review, hiển thị mô tả shop dưới tên shop)
- `apps/frontend/static/js/staff/staff-shop-registration-detail.js` [NEW] — (v2.3: load chi tiết và gửi kết quả review duyệt/từ chối lên API)

## Changelog
| Version | Date | Mô tả |
|---|---|---|
| 1.0 | 2026-06-27 | Khởi tạo: KYC list/detail, withdrawals, complaints |
| 1.1 | 2026-07-01 | Bổ sung thông tin user vào KYC detail; rút gọn bộ lọc ngày ở transactions.html |
| 1.2 | 2026-07-01 | Load dữ liệu động từ database cho 2 bộ lọc Mức độ và Trạng thái tại flags.html |
| 1.3 | 2026-07-02 | Load dữ liệu động từ database cho bộ lọc Trạng thái tại transactions.html và Việt hóa bộ lọc Loại giao dịch |
| 1.4 | 2026-07-02 | Việt hóa giao diện cờ cảnh báo (cột trạng thái/mức độ, filter 'Tất cả mức độ/trạng thái'); thêm cột STT tự động theo phân trang; sửa link Khiếu nại kết nối chính xác và an toàn trong flag-detail |
| 1.5 | 2026-07-02 | Việt hóa trạng thái rút tiền (Pending -> Chờ duyệt, Approved -> Hoàn tất, Rejected -> Từ chối) và style động cho badge trạng thái trong withdrawals và withdrawal-detail |
| 1.6 | 2026-07-02 | Tối ưu hóa hàm đếm rút tiền tại StaffController và WithdrawalRepository sử dụng countByStatusesAndNotDeleted để tránh lỗi kiểu dữ liệu và null-safe đối với isDelete |
| 1.7 | 2026-07-02 | Việt hóa hiển thị trạng thái giao dịch (Success/Completed -> Thành công, Pending/Processing -> Đang xử lý, Hold/Holding/Escrow/Held -> Tạm giữ, Refunded/Refund -> Hoàn tiền, Disputed/Complaint -> Khiếu nại, Rejected/Failed/Cancelled -> Từ chối) và style màu sắc cho badge trạng thái động trong danh sách giao dịch và chi tiết giao dịch |
| 1.8 | 2026-07-02 | Sửa logic thống kê giao dịch tại StaffController và TransactionRepository sử dụng countByStatusesAndNotDeleted với các trạng thái tiếng Anh thực tế trong DB để hiển thị chính xác số lượng thành công, đang xử lý, thất bại |
| 1.9 | 2026-07-02 | Thêm nút 'Làm mới bộ lọc' vào trang quản lý định danh KYC (kyc.html) và xử lý sự kiện trong staff-kyc.js để đưa tất cả các trường lọc (mã hồ sơ, loại giấy tờ, trạng thái) về trống và tải lại danh sách cùng các con số thống kê |
| 1.10 | 2026-07-02 | Cập nhật logic tính toán thống kê Khiếu nại tại ComplaintServiceImpl để gộp đếm cả các trạng thái tương đương (Open + InProgress cho đang xử lý, và các biến thể chữ hoa/thường cho resolved và rejected) |
| 1.11 | 2026-07-02 | Sửa đổi searchComplaintsForStaff ở ComplaintServiceImpl và ComplaintRepository để map trạng thái lọc từ UI thành tập hợp trạng thái tương đương trong DB khi truy vấn danh sách khiếu nại của Staff |
| 1.12 | 2026-07-02 | Thêm hỗ trợ kiểm tra null-safe đối với trường soft-delete isDelete (w.isDelete = false OR w.isDelete IS NULL) trong tất cả các câu truy vấn và thống kê khiếu nại của Staff để tránh mất dữ liệu |
| 1.13 | 2026-07-02 | Khắc phục lỗi bind trống danh sách queryStatuses bằng phần tử DUMMY khi hasStatus = false để tránh lỗi validate Hibernate query syntax (tránh API 500 fallback sang mock data) |
| 1.14 | 2026-07-02 | Gộp trạng thái Mới (New/Open) thành Đang xử lý (InProgress) ở cả hiển thị cột trạng thái, badge chi tiết, form dropdown và mapping điều kiện lọc dữ liệu ở Backend |
| 1.15 | 2026-07-02 | Xóa nút '+ Cắm cờ mới' khỏi tiêu đề trang quản lý cờ cảnh báo (flags.html) để tránh việc Staff tự ý tạo cờ không đúng quy trình |
| 1.16 | 2026-07-02 | Bổ sung trường thông tin 'Lý do / Kết quả' lấy từ thuộc tính resolution của bảng Complaints vào thông tin chi tiết của khiếu nại (complaint-detail.html) |
| 1.17 | 2026-07-04 | Loại bỏ tính năng tìm kiếm theo loại khiếu nại ở trang quản lý khiếu nại, loại bỏ cột 'Loại' trong bảng danh sách |
| 1.18 | 2026-07-04 | Triển khai API lấy danh sách trạng thái khiếu nại động từ Database và tải động danh sách trạng thái lên bộ lọc giao diện thay vì hardcode |
| 2.0 | 2026-07-07 | Tích hợp Quản lý Shop vào Trung tâm Staff (Sidebar, Dashboard Quick Access), nâng cấp giao diện đồng bộ (thống kê, bộ lọc trạng thái yêu cầu & trạng thái shop, phân trang); thay thế cột danh mục bằng 3 cột: Trạng thái Shop, Tiền cọc, Số dư. Sửa lỗi thiếu script staff-ui.js hiển thị phân trang, Việt hóa hoàn chỉnh trạng thái; tải động danh sách trạng thái shop và trạng thái yêu cầu từ database lên bộ lọc; thay thế nút xem chi tiết ở cột Thao tác bằng nút gạt Bật/Tắt (Active/Banned) kiểm soát trực tiếp trạng thái hoạt động của Shop. |
| 2.1 | 2026-07-09 | Loại bỏ bộ lọc và cột Trạng thái yêu cầu khỏi giao diện Quản lý Shop; đổi tên trang thành Quản lý Shop; thay đổi các thẻ thống kê thành Tổng số Shop, Shop đang hoạt động (màu xanh lá), Shop bị khóa (màu vàng/cam), Tổng tiền cọc lấy dữ liệu động từ Database; Việt hóa và thay đổi màu sắc badge trạng thái của Shop thành màu xanh (hoạt động) và màu cam (đang bị khóa) |
| 2.2 | 2026-07-10 | Loại bỏ cột Ngày gửi khỏi giao diện danh sách Shop, giữ bảng tinh gọn với 7 cột chính; khôi phục biểu tượng con mắt trong cột Thao tác để mở modal xem chi tiết và kiểm duyệt. |
| 2.3 | 2026-07-11 | Thay thế popup Modal xem chi tiết/kiểm duyệt yêu cầu mở Shop bằng trang xem chi tiết riêng biệt `/staff/shop-registrations/detail?id={id}` tương tự như KYC. Cấu hình cột "Tên Shop" trong bảng danh sách chỉ hiển thị tên shop duy nhất (loại bỏ mô tả, email/sđt liên hệ khỏi ô). Sửa đổi tiêu đề trang chi tiết thành "Xem chi tiết Shop", hiển thị mã shop làm subtitle, loại bỏ trường danh mục kinh doanh và trường ngày nộp yêu cầu, chuyển đổi nguồn thông tin email và số điện thoại liên hệ từ bảng `Users`, và tích hợp thông tin tài khoản ngân hàng của Seller (Số tài khoản, ngân hàng, chi nhánh) vào phần thông tin tài chính. |
| 2.4 | 2026-07-11 | Thêm Menu thả xuống (Dropdown) collapsible "Quản lý đơn từ" trong Sidebar của Staff, nhóm 3 mục: Khiếu nại, Giao dịch và Rút tiền để tối ưu hóa không gian hiển thị của thanh bên. |
| 2.5 | 2026-07-11 | Triển khai trang tổng quan `/staff/documents` (documents-dashboard.html) cho phân hệ Quản lý đơn từ, hiển thị các số liệu thống kê (Khiếu nại đang mở, Yêu cầu rút tiền chờ duyệt, Giao dịch chờ duyệt) và thẻ truy cập nhanh. Tách tiêu đề "Quản lý đơn từ" trong Sidebar thành liên kết trực tiếp trỏ đến trang tổng quan này. Trên trang Dashboard tổng quan chính `/staff/dashboard`, loại bỏ 2 thẻ "Quản lý khiếu nại" và "Quản lý rút tiền" riêng lẻ và thay thế bằng thẻ truy cập nhanh đến "Quản lý đơn từ" tương ứng. |
| 2.6 | 2026-07-11 | Cập nhật các chỉ số trên trang Dashboard tổng quan chính `/staff/dashboard`: Thay thế thẻ thống kê "Khiếu nại đang mở" bằng "Tổng số Shop" (được đếm tự động từ database) và xóa bỏ hoàn toàn thẻ thống kê "Yêu cầu rút tiền". |
| 2.7 | 2026-07-17 | Xóa cột "Lý do" khỏi bảng danh sách cờ cảnh báo (flags.html); Đồng thời tích hợp mục "Phiếu Hỗ Trợ" (Support Tickets) vào trong nhóm menu thả xuống và trang tổng quan "Quản lý đơn từ" (documents-dashboard.html). |







