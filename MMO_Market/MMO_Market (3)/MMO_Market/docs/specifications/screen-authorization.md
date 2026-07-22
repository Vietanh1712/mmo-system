# BÁO CÁO ĐẶC TẢ PHÂN QUYỀN MÀN HÌNH (SCREEN AUTHORIZATION MATRIX)
**Hệ thống**: MMO Market (Nền tảng thương mại điện tử C2C sản phẩm số)  
**Tài liệu**: SRS Document - Section 2.3 Screen Authorization  
**Phiên bản**: v1.0 (Đặc tả chi tiết màn hình)  
**Ngày cập nhật**: 2026-07-22  

---

## 2.3 Screen Authorization

Bảng ma trận dưới đây đặc tả chi tiết phân quyền truy cập chi tiết từng màn hình (Screen Name) và các tác vụ màn hình (Screen Activities) tương ứng với từng vai trò người dùng (Roles) trong hệ thống MMO Market.

### Danh sách các vai trò (Roles):
1. **Guest**: Người dùng vãng lai chưa đăng nhập.
2. **Customer**: Khách hàng đã đăng ký & xác thực tài khoản.
3. **Seller**: Người bán (Customer đã được phê duyệt hồ sơ KYC và Đăng ký Gian hàng).
4. **Staff**: Nhân viên vận hành, hỗ trợ & kiểm duyệt hệ thống.
5. **Admin**: Quản trị viên cấp cao nhất hệ thống.

---

### Bảng Ma Trận Phân Quyền Màn Hình Chi Tiết (Screen Authorization Matrix)

| Screen | Guest | Customer | Seller | Staff | Admin |
| :--- | :---: | :---: | :---: | :---: | :---: |
| **<< Màn hình Đăng nhập / Đăng ký >>** | | | | | |
| &nbsp;&nbsp;&nbsp;&nbsp;Query All Data (Truy cập giao diện Đăng nhập / Đăng ký) | X | | | | |
| &nbsp;&nbsp;&nbsp;&nbsp;Add New Data (Thực hiện Đăng ký tài khoản / Đăng nhập / OAuth2) | X | | | | |
| **<< Màn hình Khôi phục & Đổi Mật khẩu >>** | | | | | |
| &nbsp;&nbsp;&nbsp;&nbsp;Query Own Data (Truy cập giao diện Khôi phục / Đổi mật khẩu) | | X | X | X | X |
| &nbsp;&nbsp;&nbsp;&nbsp;Update Own Data (Gửi yêu cầu reset / Đổi mật khẩu / Cấu hình 2FA) | | X | X | X | X |
| **<< Màn hình Hồ sơ Cá nhân (`/profile`) >>** | | | | | |
| &nbsp;&nbsp;&nbsp;&nbsp;Query Own Data (Xem thông tin tài khoản cá nhân, sdt, email, số dư) | | X | X | X | X |
| &nbsp;&nbsp;&nbsp;&nbsp;Update Own Data (Cập nhật họ tên, số điện thoại, avatar) | | X | X | X | X |
| **<< Màn hình Định danh Cá nhân KYC (`/account/kyc`) >>** | | | | | |
| &nbsp;&nbsp;&nbsp;&nbsp;Query Own Data (Xem trạng thái hồ sơ KYC cá nhân) | | X | X | | |
| &nbsp;&nbsp;&nbsp;&nbsp;Add New Data (Đăng tải tài liệu CMND/CCCD, ảnh xác thực) | | X | | | |
| **<< Màn hình Danh sách KYC - Staff (`/staff/kyc`) >>** | | | | | |
| &nbsp;&nbsp;&nbsp;&nbsp;Query Managed Data (Xem danh sách các hồ sơ KYC chờ kiểm duyệt) | | | | X | X |
| **<< Màn hình Chi tiết KYC - Staff (`/staff/kyc/detail`) >>** | | | | | |
| &nbsp;&nbsp;&nbsp;&nbsp;Query Managed Data (Xem thông tin chi tiết & ảnh tài liệu KYC) | | | | X | X |
| &nbsp;&nbsp;&nbsp;&nbsp;Update Managed Data (Phê duyệt hoặc từ chối hồ sơ KYC) | | | | X | X |
| **<< Màn hình Đăng ký Mở Shop (`/account/register-shop`) >>** | | | | | |
| &nbsp;&nbsp;&nbsp;&nbsp;Query Own Data (Xem trạng thái đơn đăng ký gian hàng cá nhân) | | X | X | | |
| &nbsp;&nbsp;&nbsp;&nbsp;Add New Data (Gửi đơn đăng ký tên Shop, mô tả & thông tin gian hàng) | | X | | | |
| **<< Màn hình Quản lý Đăng ký Shop - Staff (`/staff/shop-registrations`) >>** | | | | | |
| &nbsp;&nbsp;&nbsp;&nbsp;Query Managed Data (Xem danh sách đơn đăng ký mở Shop) | | | | X | X |
| &nbsp;&nbsp;&nbsp;&nbsp;Update Managed Data (Phê duyệt hoặc từ chối đơn mở gian hàng) | | | | X | X |
| **<< Màn hình Trang chủ (`/`) >>** | | | | | |
| &nbsp;&nbsp;&nbsp;&nbsp;Query All Data (Xem banner, danh mục sản phẩm, sản phẩm nổi bật) | X | X | X | X | X |
| **<< Màn hình Tìm kiếm & Danh mục Sản phẩm (`/search`, `/products`) >>** | | | | | |
| &nbsp;&nbsp;&nbsp;&nbsp;Query All Data (Tìm kiếm từ khóa, lọc theo danh mục, khoảng giá, rating) | X | X | X | X | X |
| **<< Màn hình Chi tiết Sản phẩm (`/products/{id}`) >>** | | | | | |
| &nbsp;&nbsp;&nbsp;&nbsp;Query All Data (Xem thông tin sản phẩm, Variant, mô tả, đánh giá) | X | X | X | X | X |
| **<< Màn hình Chi tiết Gian hàng (`/shop/{id}`) >>** | | | | | |
| &nbsp;&nbsp;&nbsp;&nbsp;Query All Data (Xem thông tin Shop, danh sách sản phẩm, điểm rating) | X | X | X | X | X |
| &nbsp;&nbsp;&nbsp;&nbsp;Update Own Data (Theo dõi / Bỏ theo dõi gian hàng - Follow Shop) | | X | X | | |
| &nbsp;&nbsp;&nbsp;&nbsp;Add New Data (Gửi cờ báo cáo gian hàng vi phạm - Flag Shop) | | X | X | | |
| **<< Màn hình Giỏ hàng (`/cart`) >>** | | | | | |
| &nbsp;&nbsp;&nbsp;&nbsp;Query Own Data (Xem danh sách sản phẩm trong giỏ hàng) | | X | X | | |
| &nbsp;&nbsp;&nbsp;&nbsp;Update Own Data (Thêm, bớt, chỉnh sửa số lượng món hàng) | | X | X | | |
| **<< Màn hình Thanh toán Escrow (`/checkout`) >>** | | | | | |
| &nbsp;&nbsp;&nbsp;&nbsp;Query Own Data (Xem tổng tiền đơn hàng, số dư ví khả dụng) | | X | X | | |
| &nbsp;&nbsp;&nbsp;&nbsp;Add New Data (Xác nhận đặt mua & thực hiện thanh toán Escrow) | | X | X | | |
| **<< Màn hình Danh sách Đặt trước Pre-Order (`/preorder`) >>** | | | | | |
| &nbsp;&nbsp;&nbsp;&nbsp;Query All Data (Xem danh sách các sản phẩm cho phép đặt mua trước) | X | X | X | X | X |
| &nbsp;&nbsp;&nbsp;&nbsp;Add New Data (Tạo đơn đặt trước sản phẩm Pre-order) | | X | X | | |
| **<< Màn hình Đặt trước của tôi (`/account/preorders`) >>** | | | | | |
| &nbsp;&nbsp;&nbsp;&nbsp;Query Own Data (Xem danh sách & trạng thái đơn đặt trước cá nhân) | | X | X | | |
| &nbsp;&nbsp;&nbsp;&nbsp;Update Own Data (Hủy đơn đặt trước cá nhân) | | X | X | | |
| **<< Màn hình Quản lý Đặt trước - Seller (`/seller/preorders`) >>** | | | | | |
| &nbsp;&nbsp;&nbsp;&nbsp;Query Own Data (Xem các đơn đặt trước sản phẩm thuộc Shop mình) | | | X | | |
| &nbsp;&nbsp;&nbsp;&nbsp;Update Own Data (Xác nhận đơn Pre-order / Cập nhật ngày giao hàng) | | | X | | |
| **<< Màn hình Quản lý Đơn mua (`/account/orders`) >>** | | | | | |
| &nbsp;&nbsp;&nbsp;&nbsp;Query Own Data (Xem danh sách toàn bộ đơn hàng đã mua) | | X | X | | |
| **<< Màn hình Chi tiết Đơn mua (`/account/orders/{code}`) >>** | | | | | |
| &nbsp;&nbsp;&nbsp;&nbsp;Query Own Data (Xem chi tiết đơn hàng & thông tin giao dịch) | | X | X | | |
| &nbsp;&nbsp;&nbsp;&nbsp;Query Own Data (Nhận & tải tài sản số: mã Key, tài khoản, giftcode) | | X | X | | |
| **<< Màn hình Đánh giá Sản phẩm (`/account/orders/{code}/feedback`) >>** | | | | | |
| &nbsp;&nbsp;&nbsp;&nbsp;Add New Data (Gửi đánh giá số sao 1-5 và nhận xét sản phẩm) | | X | X | | |
| **<< Màn hình Tạo Khiếu nại (`/account/complaints/new`) >>** | | | | | |
| &nbsp;&nbsp;&nbsp;&nbsp;Add New Data (Gửi đơn khiếu nại đối với đơn hàng gặp sự cố) | | X | X | | |
| **<< Màn hình Danh sách Khiếu nại cá nhân (`/account/complaints`) >>** | | | | | |
| &nbsp;&nbsp;&nbsp;&nbsp;Query Own Data (Xem danh sách khiếu nại do cá nhân tạo) | | X | X | | |
| **<< Màn hình Chi tiết Khiếu nại cá nhân (`/account/complaints/detail`) >>** | | | | | |
| &nbsp;&nbsp;&nbsp;&nbsp;Query Own Data (Xem chi tiết tiến trình khiếu nại & tin nhắn trao đổi) | | X | X | | |
| &nbsp;&nbsp;&nbsp;&nbsp;Update Own Data (Cung cấp bổ sung bằng chứng, hình ảnh/video lỗi) | | X | X | | |
| **<< Màn hình Ví cá nhân (`/wallet`) >>** | | | | | |
| &nbsp;&nbsp;&nbsp;&nbsp;Query Own Data (Xem số dư ví khả dụng & số dư đóng băng hold balance) | | X | X | | |
| **<< Màn hình Nạp tiền Ví (`/wallet/topup`) >>** | | | | | |
| &nbsp;&nbsp;&nbsp;&nbsp;Add New Data (Tạo yêu cầu nạp tiền, hiển thị VietQR / SePay) | | X | X | | |
| **<< Màn hình Lịch sử Giao dịch Ví (`/wallet/transactions`) >>** | | | | | |
| &nbsp;&nbsp;&nbsp;&nbsp;Query Own Data (Xem lịch sử biến động số dư & giao dịch nạp/trừ tiền) | | X | X | | |
| **<< Màn hình Dashboard Seller (`/seller/dashboard`) >>** | | | | | |
| &nbsp;&nbsp;&nbsp;&nbsp;Query Own Data (Xem thống kê doanh thu, đơn bán, sản phẩm bán chạy) | | | X | | |
| **<< Màn hình Thông tin Gian hàng Seller (`/seller/shop-info`) >>** | | | | | |
| &nbsp;&nbsp;&nbsp;&nbsp;Query Own Data (Xem thông tin gian hàng cá nhân) | | | X | | |
| &nbsp;&nbsp;&nbsp;&nbsp;Update Own Data (Cập nhật tên Shop, Logo, Banner, Mô tả) | | | X | | |
| &nbsp;&nbsp;&nbsp;&nbsp;Update Own Data (Bật/tắt trạng thái tạm đóng cửa gian hàng) | | | X | | |
| **<< Màn hình Quản lý Sản phẩm Seller (`/seller/inventory`) >>** | | | | | |
| &nbsp;&nbsp;&nbsp;&nbsp;Query Own Data (Xem danh sách sản phẩm & tồn kho của Shop) | | | X | | |
| &nbsp;&nbsp;&nbsp;&nbsp;Delete Own Data (Ẩn / Xóa mềm sản phẩm khỏi sàn) | | | X | | |
| **<< Màn hình Thêm/Sửa Sản phẩm Seller (`/seller/products/new`, `/seller/products/edit`) >>** | | | | | |
| &nbsp;&nbsp;&nbsp;&nbsp;Add New Data (Thêm sản phẩm mới, phân loại Variant, nạp kho tài sản số) | | | X | | |
| &nbsp;&nbsp;&nbsp;&nbsp;Update Own Data (Cập nhật giá bán, tên, hình ảnh, mô tả sản phẩm/variant) | | | X | | |
| **<< Màn hình Quản lý Đơn bán Seller (`/seller/transactions`) >>** | | | | | |
| &nbsp;&nbsp;&nbsp;&nbsp;Query Own Data (Xem danh sách đơn bán & trạng thái giải ngân Escrow) | | | X | | |
| **<< Màn hình Yêu cầu Rút tiền Seller (`/seller/withdrawals`) >>** | | | | | |
| &nbsp;&nbsp;&nbsp;&nbsp;Query Own Data (Xem số dư khả dụng & lịch sử rút tiền) | | | X | | |
| &nbsp;&nbsp;&nbsp;&nbsp;Add New Data (Tạo lệnh rút tiền về tài khoản ngân hàng) | | | X | | |
| **<< Màn hình Quản lý Khiếu nại Seller (`/seller/complaints`, `/seller/complaints/detail`) >>** | | | | | |
| &nbsp;&nbsp;&nbsp;&nbsp;Query Own Data (Xem danh sách đơn bán bị người mua khiếu nại) | | | X | | |
| &nbsp;&nbsp;&nbsp;&nbsp;Update Own Data (Gửi phản hồi, bằng chứng đối chứng với Buyer & Staff) | | | X | | |
| **<< Màn hình Dashboard Staff (`/staff/dashboard`) >>** | | | | | |
| &nbsp;&nbsp;&nbsp;&nbsp;Query Managed Data (Xem tổng quan chỉ số vận hành: KYC, khiếu nại, rút tiền) | | | | X | X |
| **<< Màn hình Xử lý Khiếu nại Staff (`/staff/complaints`, `/staff/complaints/detail`) >>** | | | | | |
| &nbsp;&nbsp;&nbsp;&nbsp;Query Managed Data (Xem toàn bộ danh sách khiếu nại trên sàn) | | | | X | X |
| &nbsp;&nbsp;&nbsp;&nbsp;Update Managed Data (Đưa ra phán quyết khiếu nại: Hoàn tiền Buyer / Giải ngân Seller) | | | | X | X |
| **<< Màn hình Duyệt Rút tiền Staff (`/staff/withdrawals`, `/staff/withdrawals/detail`) >>** | | | | | |
| &nbsp;&nbsp;&nbsp;&nbsp;Query Managed Data (Xem danh sách các yêu cầu rút tiền từ Seller) | | | | X | X |
| &nbsp;&nbsp;&nbsp;&nbsp;Update Managed Data (Duyệt/Từ chối lệnh rút tiền & upload ảnh bill chuyển khoản) | | | | X | X |
| **<< Màn hình Tra cứu Giao dịch Staff (`/staff/transactions`, `/staff/transactions/detail`) >>** | | | | | |
| &nbsp;&nbsp;&nbsp;&nbsp;Query Managed Data (Tra cứu chi tiết toàn bộ giao dịch mua bán & Escrow trên sàn) | | | | X | X |
| **<< Màn hình Xử lý Cờ Cảnh báo Staff (`/staff/flags`, `/staff/flags/detail`) >>** | | | | | |
| &nbsp;&nbsp;&nbsp;&nbsp;Query Managed Data (Xem danh sách cờ báo cáo gian hàng vi phạm) | | | | X | X |
| &nbsp;&nbsp;&nbsp;&nbsp;Update Managed Data (Gán cấp độ cảnh báo gian hàng: Warning, Danger, Critical) | | | | X | X |
| &nbsp;&nbsp;&nbsp;&nbsp;Delete Data (Gỡ cờ cảnh báo gian hàng) | | | | X | X |
| **<< Màn hình Xử lý Phiếu Hỗ trợ Staff (`/staff/support-tickets`) >>** | | | | | |
| &nbsp;&nbsp;&nbsp;&nbsp;Query Managed Data (Xem danh sách yêu cầu hỗ trợ từ người dùng) | | | | X | X |
| &nbsp;&nbsp;&nbsp;&nbsp;Update Managed Data (Phản hồi & đóng phiếu hỗ trợ) | | | | X | X |
| **<< Màn hình Quyền hạn cá nhân Staff (`/staff/my-permissions`) >>** | | | | | |
| &nbsp;&nbsp;&nbsp;&nbsp;Query Own Data (Xem danh sách các quyền hạn thao tác được Admin cấp) | | | | X | X |
| **<< Màn hình Quản lý Người dùng Admin (`/admin/users`) >>** | | | | | |
| &nbsp;&nbsp;&nbsp;&nbsp;Query All Data (Xem danh sách toàn bộ tài khoản người dùng trên hệ thống) | | | | | X |
| &nbsp;&nbsp;&nbsp;&nbsp;Update All Data (Khóa / Mở khóa tài khoản, thay đổi Vai trò Role) | | | | | X |
| **<< Màn hình Phân quyền Staff Admin (`/admin/staff-permissions`) >>** | | | | | |
| &nbsp;&nbsp;&nbsp;&nbsp;Query All Data (Xem bảng danh sách Staff và các quyền hạn được gán) | | | | | X |
| &nbsp;&nbsp;&nbsp;&nbsp;Update All Data (Gán hoặc rút bớt quyền hạn tác vụ của từng Staff) | | | | | X |
| **<< Màn hình Cấu hình Hệ thống Admin (`/admin/system-config`) >>** | | | | | |
| &nbsp;&nbsp;&nbsp;&nbsp;Query All Data (Xem các thông số cấu hình hệ thống, tỷ lệ phí, thời gian Escrow) | | | | | X |
| &nbsp;&nbsp;&nbsp;&nbsp;Update All Data (Thay đổi thời gian Escrow 72h, mức phí sàn, hạn mức nạp/rút) | | | | | X |
| **<< Màn hình Báo cáo Doanh thu Admin (`/admin/revenue`) >>** | | | | | |
| &nbsp;&nbsp;&nbsp;&nbsp;Query All Data (Xem báo cáo thống kê tổng doanh thu phí sàn & lịch sử Audit Logs) | | | | | X |
| **<< Màn hình Trò chuyện Chat (`/messages`, `/staff/chat`) >>** | | | | | |
| &nbsp;&nbsp;&nbsp;&nbsp;Add New Data (Gửi tin nhắn trực tiếp giữa Customer, Seller & Staff) | | X | X | X | X |
| &nbsp;&nbsp;&nbsp;&nbsp;Query Own Data (Xem lịch sử cuộc trò chuyện cá nhân) | | X | X | X | X |
| **<< Màn hình Thông báo (`/notifications`, `/account/notifications`) >>** | | | | | |
| &nbsp;&nbsp;&nbsp;&nbsp;Query Own Data (Xem danh sách thông báo cá nhân) | | X | X | X | X |
| &nbsp;&nbsp;&nbsp;&nbsp;Update Own Data (Đánh dấu đã đọc thông báo) | | X | X | X | X |

---

### Ghi Chú Kỹ Thuật Nghiệp Vụ (Business & Technical Notes):
1. **Phân loại tác vụ chuẩn SRS**:
   - `Query All Data`: Quyền xem toàn bộ dữ liệu công khai hoặc dữ liệu quản trị toàn sàn.
   - `Query Own Data`: Quyền xem dữ liệu cá nhân thuộc sở hữu chính người dùng đó.
   - `Query Managed Data`: Quyền xem dữ liệu trong phạm vi quản lý vận hành của Nhân viên / Admin.
   - `Add New Data`: Quyền tạo mới bản ghi dữ liệu.
   - `Update Own Data`: Quyền chỉnh sửa bản ghi thuộc sở hữu cá nhân.
   - `Update Managed Data`: Quyền cập nhật/phê duyệt dữ liệu trong luồng xử lý vận hành.
   - `Update All Data`: Quyền thay đổi dữ liệu cấu hình/tài khoản cấp hệ thống.
   - `Delete Data / Delete Own Data`: Quyền gỡ bỏ hoặc ẩn mềm (Soft Delete `isDelete = 1`).
