# PLAN — Staff Console Interface (`feat-staff`)

## Templates
- `apps/frontend/templates/staff/kyc.html` — Danh sách yêu cầu KYC
- `apps/frontend/templates/staff/kyc-detail.html` — Chi tiết KYC (bổ sung full_name, email, date_of_birth, address, rejection_reason)
- `apps/frontend/templates/staff/withdrawals.html`
- `apps/frontend/templates/staff/complaints.html`
- `apps/frontend/templates/staff/transactions.html` — Giao dịch toàn sàn (bộ lọc ngày rút gọn còn 1 trường `fromDate`; load động trạng thái từ database và Việt hóa nhãn hiển thị)
- `apps/frontend/templates/staff/shop-registrations.html` — Quản lý yêu cầu mở Shop (đồng bộ hóa giao diện gồm thống kê, bộ lọc từ khóa/trạng thái, phân trang; loại bỏ Modal review)
- `apps/frontend/templates/staff/shop-registration-detail.html` — Chi tiết đăng ký Shop (trang chi tiết riêng biệt thay thế Modal review, tương tự như KYC detail)

## JS Scripts
- `apps/frontend/static/js/staff/staff-kyc.js`
- `apps/frontend/static/js/staff/staff-kyc-detail.js` — (bind fullName, email, dateOfBirth, address, rejectionReason từ API)
- `apps/frontend/static/js/staff/staff-ui.js`
- `apps/frontend/static/js/staff/staff-chat.js`
- `apps/frontend/static/js/staff/staff-shop-registrations.js` — (gọi API thống kê & tìm kiếm phân trang mới; đổi nút xem chi tiết thành link điều hướng, loại bỏ modal review, hiển thị mô tả shop dưới tên shop)
- `apps/frontend/static/js/staff/staff-shop-registration-detail.js` — (load chi tiết và gửi kết quả review duyệt/từ chối lên API)

## Lịch sử thay đổi

- Khởi tạo: KYC list/detail, withdrawals, complaints
- Bổ sung thông tin user vào KYC detail; rút gọn bộ lọc ngày ở transactions.html
- Load dữ liệu động từ database cho 2 bộ lọc Mức độ và Trạng thái tại flags.html
- Load dữ liệu động từ database cho bộ lọc Trạng thái tại transactions.html và Việt hóa bộ lọc Loại giao dịch
- Việt hóa giao diện cờ cảnh báo (cột trạng thái/mức độ, filter 'Tất cả mức độ/trạng thái'); thêm cột STT tự động theo phân trang; sửa link Khiếu nại kết nối chính xác và an toàn trong flag-detail
- Việt hóa trạng thái rút tiền (Pending -> Chờ duyệt, Approved -> Hoàn tất, Rejected -> Từ chối) và style động cho badge trạng thái trong withdrawals và withdrawal-detail
- Tối ưu hóa hàm đếm rút tiền tại StaffController và WithdrawalRepository sử dụng countByStatusesAndNotDeleted để tránh lỗi kiểu dữ liệu và null-safe đối với isDelete
- Việt hóa hiển thị trạng thái giao dịch (Success/Completed -> Thành công, Pending/Processing -> Đang xử lý, Hold/Holding/Escrow/Held -> Tạm giữ, Refunded/Refund -> Hoàn tiền, Disputed/Complaint -> Khiếu nại, Rejected/Failed/Cancelled -> Từ chối) và style màu sắc cho badge trạng thái động trong danh sách giao dịch và chi tiết giao dịch
- Sửa logic thống kê giao dịch tại StaffController và TransactionRepository sử dụng countByStatusesAndNotDeleted với các trạng thái tiếng Anh thực tế trong DB để hiển thị chính xác số lượng thành công, đang xử lý, thất bại
- Thêm nút 'Làm mới bộ lọc' vào trang quản lý định danh KYC (kyc.html) và xử lý sự kiện trong staff-kyc.js để đưa tất cả các trường lọc (mã hồ sơ, loại giấy tờ, trạng thái) về trống và tải lại danh sách cùng các con số thống kê
- Cập nhật logic tính toán thống kê Khiếu nại tại ComplaintServiceImpl để gộp đếm cả các trạng thái tương đương (Open + InProgress cho đang xử lý, và các biến thể chữ hoa/thường cho resolved và rejected)
- Sửa đổi searchComplaintsForStaff ở ComplaintServiceImpl và ComplaintRepository để map trạng thái lọc từ UI thành tập hợp trạng thái tương đương trong DB khi truy vấn danh sách khiếu nại của Staff
- Thêm hỗ trợ kiểm tra null-safe đối với trường soft-delete isDelete (w.isDelete = false OR w.isDelete IS NULL) trong tất cả các câu truy vấn và thống kê khiếu nại của Staff để tránh mất dữ liệu
- Khắc phục lỗi bind trống danh sách queryStatuses bằng phần tử DUMMY khi hasStatus = false để tránh lỗi validate Hibernate query syntax (tránh API 500 fallback sang mock data)
- Gộp trạng thái Mới (New/Open) thành Đang xử lý (InProgress) ở cả hiển thị cột trạng thái, badge chi tiết, form dropdown và mapping điều kiện lọc dữ liệu ở Backend
- Xóa nút '+ Cắm cờ mới' khỏi tiêu đề trang quản lý cờ cảnh báo (flags.html) để tránh việc Staff tự ý tạo cờ không đúng quy trình
- Bổ sung trường thông tin 'Lý do / Kết quả' lấy từ thuộc tính resolution của bảng Complaints vào thông tin chi tiết của khiếu nại (complaint-detail.html)
- Loại bỏ tính năng tìm kiếm theo loại khiếu nại ở trang quản lý khiếu nại, loại bỏ cột 'Loại' trong bảng danh sách
- Triển khai API lấy danh sách trạng thái khiếu nại động từ Database và tải động danh sách trạng thái lên bộ lọc giao diện thay vì hardcode
- Tích hợp Quản lý Shop vào Trung tâm Staff (Sidebar, Dashboard Quick Access), nâng cấp giao diện đồng bộ (thống kê, bộ lọc trạng thái yêu cầu & trạng thái shop, phân trang); thay thế cột danh mục bằng 3 cột: Trạng thái Shop, Tiền cọc, Số dư. Sửa lỗi thiếu script staff-ui.js hiển thị phân trang, Việt hóa hoàn chỉnh trạng thái; tải động danh sách trạng thái shop và trạng thái yêu cầu từ database lên bộ lọc; thay thế nút xem chi tiết ở cột Thao tác bằng nút gạt Bật/Tắt (Active/Banned) kiểm soát trực tiếp trạng thái hoạt động của Shop.
- Loại bỏ bộ lọc và cột Trạng thái yêu cầu khỏi giao diện Quản lý Shop; đổi tên trang thành Quản lý Shop; thay đổi các thẻ thống kê thành Tổng số Shop, Shop đang hoạt động (màu xanh lá), Shop bị khóa (màu vàng/cam), Tổng tiền cọc lấy dữ liệu động từ Database; Việt hóa và thay đổi màu sắc badge trạng thái của Shop thành màu xanh (hoạt động) và màu cam (đang bị khóa)
- Loại bỏ cột Ngày gửi khỏi giao diện danh sách Shop, giữ bảng tinh gọn với 7 cột chính; khôi phục biểu tượng con mắt trong cột Thao tác để mở modal xem chi tiết và kiểm duyệt.
- Thay thế popup Modal xem chi tiết/kiểm duyệt yêu cầu mở Shop bằng trang xem chi tiết riêng biệt `/staff/shop-registrations/detail?id={id}` tương tự như KYC. Cấu hình cột "Tên Shop" trong bảng danh sách chỉ hiển thị tên shop duy nhất (loại bỏ mô tả, email/sđt liên hệ khỏi ô). Sửa đổi tiêu đề trang chi tiết thành "Xem chi tiết Shop", hiển thị mã shop làm subtitle, loại bỏ trường danh mục kinh doanh và trường ngày nộp yêu cầu, chuyển đổi nguồn thông tin email và số điện thoại liên hệ từ bảng `Users`, và tích hợp thông tin tài khoản ngân hàng của Seller (Số tài khoản, ngân hàng, chi nhánh) vào phần thông tin tài chính.
- Thêm Menu thả xuống (Dropdown) collapsible "Quản lý đơn từ" trong Sidebar của Staff, nhóm 3 mục: Khiếu nại, Giao dịch và Rút tiền để tối ưu hóa không gian hiển thị của thanh bên.
- Triển khai trang tổng quan `/staff/documents` (documents-dashboard.html) cho phân hệ Quản lý đơn từ, hiển thị các số liệu thống kê (Khiếu nại đang mở, Yêu cầu rút tiền chờ duyệt, Giao dịch chờ duyệt) và thẻ truy cập nhanh. Tách tiêu đề "Quản lý đơn từ" trong Sidebar thành liên kết trực tiếp trỏ đến trang tổng quan này. Trên trang Dashboard tổng quan chính `/staff/dashboard`, loại bỏ 2 thẻ "Quản lý khiếu nại" và "Quản lý rút tiền" riêng lẻ và thay thế bằng thẻ truy cập nhanh đến "Quản lý đơn từ" tương ứng.
- Cập nhật các chỉ số trên trang Dashboard tổng quan chính `/staff/dashboard`: Thay thế thẻ thống kê "Khiếu nại đang mở" bằng "Tổng số Shop" (được đếm tự động từ database) và xóa bỏ hoàn toàn thẻ thống kê "Yêu cầu rút tiền".
- Xóa cột "Lý do" khỏi bảng danh sách cờ cảnh báo (flags.html); Đồng thời tích hợp mục "Phiếu Hỗ Trợ" (Support Tickets) vào trong nhóm menu thả xuống và trang tổng quan "Quản lý đơn từ" (documents-dashboard.html).
- Cập nhật giao diện cờ cảnh báo (flags.html & staff.css): Thay đổi bố cục các thẻ thống kê thành 3 ô trên 1 hàng (staff-stat-grid--3cols) trên desktop và tự động phản hồi theo kích thước màn hình.
- Cập nhật giao diện Quản lý Shop (shop-registrations.html & staff.css): Thay đổi bố cục thẻ thống kê thành 4 ô trên 1 hàng (staff-stat-grid--4cols) trên desktop.
- Chuẩn hóa 5 nhãn trạng thái tài khoản Shop tiếng Việt (Hoạt động, Tạm ngưng, Tạm khóa, Khóa vĩnh viễn, Đã đóng Shop (Hoàn cọc)); Cập nhật các thẻ thống kê danh sách Shop; Hợp nhất chức năng xem chi tiết và cập nhật trạng thái hoạt động vào 1 trang duy nhất `/staff/shop-registrations/detail`; Rút gọn cột thao tác bảng danh sách Shop thành 1 nút icon con mắt (`ds-icon-btn-view`) duy nhất; Tích hợp Modal chọn Ngày/Giờ/Phút tạm ngưng (`suspendShopModal`); Hiển thị khung đếm ngược thời gian thực (Real-time countdown timer) ở thẻ thông tin Shop và thẻ Cập nhật trạng thái; Tự động khôi phục về trạng thái 'Hoạt động' khi đếm ngược kết thúc.
- Đồng bộ hóa 100% tất cả bộ lọc trạng thái (Shop, Giao dịch, Rút tiền, Cờ cảnh báo, Khiếu nại) trên phân hệ Staff sang kiểu Lọc Cố Định (Predefined / Fixed Options) như Admin; loại bỏ hoàn toàn việc gọi API quét trạng thái động `DISTINCT` từ DB để giữ bộ lọc hiển thị đầy đủ và ổn định.
- Nâng cấp truy vấn tìm kiếm lọc giao dịch ở `StaffController` and `TransactionRepository`: Tự động quy đổi trạng thái được chọn trên UI (Thành công, Đang xử lý, Tạm giữ, Hoàn tiền, Khiếu nại, Thất bại/Từ chối) thành danh sách toàn bộ các từ đồng nghĩa và biến thể chữ hoa/thường tương ứng trong DB (`t.status IN (:statuses)`) giúp kết quả lọc chính xác 100%.
- Nâng cấp hàm đếm thống kê Quản lý Shop `getRegistrationStats()` trong `ShopRegistrationService` và `UserRepository`: Duyệt đếm trực tiếp trên danh sách `SellerRegistration` và bổ sung đầy đủ tất cả từ khóa biến thể (`TEMPORARILY_CLOSED`, `CLOSED`, `TEMP_SUSPENDED`, `INDEFINITE_LOCKED`, `PERMANENT_BANNED`) đảm bảo các ô thống kê đỉnh trang khớp 100% với dữ liệu dòng ở bảng bên dưới.
- Triển khai phân hệ Quản lý nạp tiền (`/staff/topups` & `/staff/topups/detail`): Hỗ trợ ghi vết giao dịch thất bại từ webhook SePay, xem chi tiết luồng tiền/mã SePay/số dư trước sau, và nút 'Thử lại / Kích hoạt lại' mở Modal popup cho phép Staff can thiệp cộng tiền thủ công, ghi nhật ký Wallet Ledger, gửi thông báo và chuyển trạng thái đơn sang Success.
- Đồng bộ hóa 100% bố cục trang xem chi tiết nạp tiền (`/staff/topups/detail`) theo chuẩn Design System chung của Staff (`staff-breadcrumb`, `staff-page-header`, `staff-detail-grid`, `ds-card`, `staff-info-list`, `ds-money`) khớp với trang Rút tiền, Giao dịch, KYC và Khiếu nại.
- Nâng cấp bộ trích xuất từ khóa tìm kiếm nạp tiền theo mã ID (bóc tách các dạng #TOPUP-1, TOPUP-1, #1, 1), tích hợp sự kiện tìm kiếm tự động khi gõ Enter hoặc đổi dropdown trạng thái, và chuyển đổi thẻ thống kê số 4 thành 'Chờ xử lý' (Pending count).
- Bổ sung thuộc tính pendingTopups vào StaffDashboardDTO & StaffDashboardServiceImpl, và thêm thẻ thống kê 'Nạp tiền cần xử lý' vào trang Tổng quan quản lý đơn từ (/staff/documents).
- Khóa chức năng thay đổi trạng thái khi Shop đã ở trạng thái "Đã đóng Shop (Hoàn phí)" (Withdrawn) hoặc "Đã xóa" (Deleted): Ở màn hình cập nhật trạng thái (`shop-registration-update-status.html`) và màn hình chi tiết (`shop-registration-detail.html`), tự động ẩn biểu mẫu/nút lưu cập nhật trạng thái và hiển thị thông báo cảnh báo màu đỏ tương ứng.
- Đồng bộ hóa 3 mức độ cờ vi phạm (Mức độ 1: Cảnh cáo, Mức độ 2: Nguy hiểm, Mức độ 3: Cấm) trên toàn bộ phân hệ quản lý cờ cảnh báo của Staff. Mức độ 2 (Danger / Suspension) tương ứng với Tạm khóa shop, Mức độ 3 (Ban) tương ứng với Khóa vĩnh viễn shop.
- Triển khai quy tắc tự động khóa shop vĩnh viễn (Banned) và xóa thời hạn tạm khóa khi Seller bị cắm từ 3 cờ vi phạm trở lên đang có hiệu lực.
- Cập nhật giao diện danh sách cờ cảnh báo (flags.html): 3 ô thống kê cờ tương ứng với 3 mức độ; cột "Staff" chuyển đổi thành cột "Seller" hiển thị địa chỉ email của người bán bị phạt để tăng khả năng theo dõi.