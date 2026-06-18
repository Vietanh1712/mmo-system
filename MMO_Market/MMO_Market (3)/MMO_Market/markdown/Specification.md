# SPECIFICATION.md — Tài liệu Đặc tả Yêu cầu Hệ thống MMO Market

## 1. Tổng Quan Hệ Thống (Overview)

### 1.1 Mục Tiêu Dự Án
Hệ thống MMO Market là nền tảng thương mại điện tử C2C bảo lãnh dành cho các sản phẩm và dịch vụ số. Hệ thống giải quyết bài toán lòng tin giữa người mua và người bán thông qua cơ chế giam tiền tự động, xác minh danh tính người bán và cung cấp môi trường phân xử khiếu nại minh bạch.

---

## 2. Vai Trò Người Dùng & Phân Quyền (User Roles - RBAC)

Tất cả tài khoản được lưu trữ tập trung tại bảng `Users`, hệ thống phân rã quyền hạn thông qua cột ràng buộc `role`:

| Quyền hạn | Mô tả chức năng |
|---|---|
| **Guest** | Khách vãng lai chưa xác thực. Chỉ được quyền xem danh mục sản phẩm, bài viết công khai và tiến hành đăng ký tài khoản. |
| **Customer** | Khách mua hàng. Có quyền nạp tiền VNĐ vào ví, đặt mua sản phẩm, gửi đánh giá phản hồi và tạo khiếu nại tranh chấp đơn hàng. |
| **Seller** | Người bán hàng (Được nâng cấp từ tài khoản Customer sau khi hồ sơ KYC đạt trạng thái `Approved`). Có quyền đăng bán sản phẩm, thiết lập loại hàng, quản lý kho, giải trình khiếu nại và rút tiền doanh thu về tài khoản ngân hàng. |
| **Staff** | Nhân viên kiểm duyệt (Trọng tài hệ thống). Có quyền duyệt/từ chối yêu cầu KYC, thẩm định phê duyệt lệnh rút tiền mặt, cắm cờ (`Flags`) cảnh cáo các shop vi phạm và đưa ra phán quyết đóng hồ sơ khiếu nại. |
| **Admin** | Quản trị viên tối cao. Thực hiện thiết lập tỷ lệ phần trăm hoa hồng (`Commissions`) sàn thu từ người bán và truy vấn nhật ký hệ thống (`AuditLogs`). |

---

## 3. Đặc Tả Chức Năng Cốt Lõi (Functional Specifications)

### FE01 - Xác Thực & Đăng Nhập Hệ Thống
- **Actors**: Guest, Customer, Seller, Staff, Admin.
- **Yêu cầu chức năng**:
  - Hỗ trợ đăng nhập bằng tài khoản nội bộ (Email + Mật khẩu mã hóa Bcrypt) hoặc Đăng nhập nhanh thông qua Google OAuth2.
  - Khi thực hiện đăng ký tài khoản, hệ thống bắt buộc phải sinh mã OTP gồm 6 chữ số gửi về Gmail thông qua bảng `EmailVerifications` với thời hạn hiệu lực là 5 phút.
  - Trả về mã JWT Token chứa thông tin định danh người dùng và Role tương ứng sau khi xác thực thành công.

### FE02 - Đăng Bán Sản Phẩm (Create Product Listing)
- **Actors**: Seller.
- **Yêu cầu chức năng**:
  - **WHEN** một người dùng gửi yêu cầu tạo bài đăng bán sản phẩm mới, **THE system SHALL** thực hiện kiểm tra cột `role` của người dùng đó.
  - **WHERE** trạng thái người dùng không thuộc nhóm `Seller` hoặc `Customer_Seller`, **THE system SHALL** chặn hành động và trả về lỗi 403 Forbidden.
  - Hệ thống cho phép một sản phẩm cấu hình nhiều loại hàng hóa biến thể (`ProductVariants`) với mức giá VNĐ (`price`) và số lượng tồn kho (`stock`) độc lập.

### FE03 - Mua Hàng & Giam Tiền Bảo Lãnh (Product Buying & Escrow Hold)
- **Actors**: Customer.
- **Yêu cầu chức năng**:
  - **WHEN** Customer thực hiện gửi yêu cầu đặt mua một biến thể sản phẩm số, **THE system SHALL** thực hiện kiểm tra số dư ví tiền mặt (`wallet_balance`) của Customer với giá bán (`price`) của biến thể đó.
  - **IF** số dư ví nhỏ hơn giá sản phẩm, **THE system SHALL** từ chối xử lý và trả về mã lỗi HTTP 400 Bad Request (`INSUFFICIENT_FUNDS`).
  - **IF** số lượng tồn kho (`stock`) của mặt hàng nhỏ hơn hoặc bằng 0, **THE system SHALL** từ chối xử lý và trả về mã lỗi HTTP 400 Bad Request (`OUT_OF_STOCK`).
  - **WHEN** tất cả các điều kiện kiểm tra đều hợp lệ, **THE system SHALL** thực hiện trừ tiền mặt trong ví Customer, giảm `stock` kho hàng đi 1 đơn vị, tính phí sàn thu (`commission_amount`) dựa theo cấu hình bảng `Commissions` và tạo mới một bản ghi giao dịch trong bảng `Transactions`.
  - **THE system SHALL** đặt trạng thái giao dịch ban đầu là `Pending` và thiết lập thời hạn nhả tiền (`escrow_release_date`) bằng thời gian hiện tại cộng thêm đúng 3 ngày (72 giờ). Tiền của Seller hoàn toàn chưa được cộng ở giai đoạn này.

### FE04 - Xử Lý Tranh Chấp & Khiếu Nại (Complaint Handling)
- **Actors**: Customer, Seller, Staff.
- **Yêu cầu chức năng**:
  - **WHEN** Customer gửi yêu cầu khiếu nại đơn hàng TRƯỚC THỜI HẠN `escrow_release_date`, **THE system SHALL** đóng băng toàn bộ tiến trình của đơn hàng và tạo hồ sơ trong bảng `Complaints`.
  - **WHERE** hồ sơ khiếu nại đang ở trạng thái `Open`, **THE system SHALL** mở kênh chat bảo mật (`ComplaintChats`) cho phép Customer, Seller và Staff tham gia trao đổi thông tin, gửi bằng chứng hình ảnh lỗi.
  - **WHEN** Staff đưa ra phán quyết đóng khiếu nại với kết quả Khách hàng đúng, **THE system SHALL** thực hiện hoàn lại 100% số tiền giao dịch (`total_amount`) về lại ví tài khoản của Customer.
  - **WHEN** Staff đưa ra phán quyết đóng khiếu nại với kết quả Người bán đúng, **THE system SHALL** thực hiện giải phóng dòng tiền, thu phí hoa hồng sàn và cộng phần tiền còn lại vào ví khả dụng của Seller.

### FE05 - Yêu Cầu Rút Tiền Doanh Thu (Withdrawal Request)
- **Actors**: Seller, Staff.
- **Yêu cầu chức năng**:
  - **WHEN** Seller tạo lệnh yêu cầu rút tiền mặt về tài khoản ngân hàng cá nhân (`Withdrawals`), **THE system SHALL** kiểm tra số dư ví khả dụng.
  - **WHERE** số tiền yêu cầu rút nhỏ hơn hạn mức tối thiểu 50.000 VNĐ hoặc vượt quá `wallet_balance`, **THE system SHALL** từ chối tạo lệnh và trả về lỗi.
  - Lệnh rút tiền khởi tạo sẽ ở trạng thái `Pending`. **WHEN** Staff thực hiện chuyển khoản thực tế thành công bên ngoài ngân hàng và tải ảnh biên lai chuyển tiền lên hệ thống (`proof_file`), **THE system SHALL** cập nhật trạng thái lệnh thành `Completed` và thực hiện trừ tiền trong ví của Seller.

---

## 4. Yêu Cầu Phi Chức Năng (Non-functional Requirements)

- **Hiệu năng hệ thống (Performance)**: Thời gian phản hồi cho các API kiểm tra kho và trừ tiền ví bắt buộc phải nhỏ hơn 500ms dưới điều kiện tải thông thường.
- **Tính toàn vẹn tài chính (ACID)**: Tất cả luồng logic xử lý biến động số dư tài khoản bắt buộc phải được bọc trong một Database Transaction duy nhất (`@Transactional`). Bất kỳ lỗi Runtime Exception nào xảy ra trong quá trình ghi dữ liệu đều phải kích hoạt cơ chế Rollback toàn cục dữ liệu về trạng thái ban đầu để tránh lỗi sai lệch tài chính.
- **Bảo mật**: Áp dụng cơ chế Khóa bi quan (Pessimistic Locking - `@Lock(LockModeType.PESSIMISTIC_WRITE)`) tại tầng Database JPA khi thực hiện truy vấn kiểm tra số dư ví và kho hàng để chặn đứng hoàn toàn lỗi spam click đồng thời (Race Condition). Mọi dữ liệu mật khẩu người dùng bắt buộc phải được băm bằng thuật toán Bcrypt trước khi lưu trữ.

---

## 5. Cấu Trúc Thực Thể Cơ Sở Dữ Liệu Gợi Ý (Suggested Entities)

Hệ thống được vận hành dựa trên 19 bảng thực thể cốt lõi sau:
1. `Users`: Lưu trữ tập trung tài khoản người dùng và số dư ví tiền mặt VNĐ (`wallet_balance`).
2. `Authentications`: Lưu trữ thông tin liên kết đăng nhập bên thứ 3 (Google OAuth2).
3. `EmailVerifications`: Quản lý mã OTP xác thực tài khoản gửi về Gmail.
4. `Commissions`: Cấu hình tỷ lệ phí sàn do Admin thiết lập cho Seller.
5. `AuditLogs`: Nhật ký hệ thống ghi vết toàn bộ hành động của người dùng nội bộ để Admin thanh tra.
6. `KYC_Requests`: Hồ sơ đăng ký thông tin định danh CCCD của Seller chờ Staff kiểm duyệt.
7. `Flags`: Nhật ký cắm cờ cảnh cáo các shop vi phạm do Staff thực hiện.
8. `Categories`: Danh mục phân loại sản phẩm số.
9. `Products`: Thông tin tổng quan của sản phẩm do Seller đăng bán.
10. `ProductVariants`: Các biến thể phân loại của sản phẩm số (chứa giá tiền VNĐ và số lượng tồn kho).
11. `Wishlists`: Danh sách sản phẩm yêu thích được Customer lưu trữ.
12. `Deposits`: Nhật ký lịch sử nạp tiền VNĐ vào ví của người dùng thông qua cổng thanh toán tự động Sepay.
13. `Transactions`: Lưu trữ thông tin mua bán sản phẩm số và thời hạn giam tiền bảo lãnh (`escrow_release_date`).
14. `SellerBankInfo`: Thông tin tài khoản ngân hàng nhận tiền của Seller dùng cho luồng rút tiền.
15. `Withdrawals`: Lệnh rút tiền mặt VNĐ của người bán chờ Staff giải ngân và tải hóa đơn biên lai ngân hàng.
16. `Complaints`: Hồ sơ tranh chấp đơn hàng chờ Staff đứng ra làm trọng tài phân xử.
17. `ComplaintChats`: Kênh chat trao đổi bằng chứng 3 bên trong luồng xử lý tranh chấp khiếu nại.
18. `Reviews`: Hệ thống chấm điểm sao và để lại bình luận phản hồi sản phẩm của Customer.
19. `Notifications`: Hệ thống thông báo đẩy hiển thị qua tài khoản của người dùng.
# FE06 - Homepage & Product Discovery

## Description
Giao diện trang chủ (Homepage) của nền tảng TapHoaMMO (MMO Market). Nơi hiển thị thông điệp cốt lõi của sàn, công cụ tìm kiếm toàn cục, và danh sách các danh mục/sản phẩm nổi bật để thu hút người mua.

## Actors
- Guest (Khách vãng lai)
- Customer
- Seller
- Staff
- Admin

## Functional Requirements
- **Header & Navigation**:
  - **WHEN** người dùng truy cập trang chủ, **THE system SHALL** hiển thị thanh điều hướng gồm: Logo, Thanh tìm kiếm (Search bar), và các menu (Trang chủ, Nạp tiền, Cấp bậc, Tin tức, Cầu hỏi thường gặp, Liên hệ).
  - **IF** người dùng chưa đăng nhập (Guest), **THE system SHALL** hiển thị nút "Đăng nhập" và "Đăng ký".
  - **IF** người dùng đã đăng nhập, **THE system SHALL** hiển thị Avatar, Tên người dùng và Số dư ví khả dụng (VNĐ).
- **Hero Banner & Lời kêu gọi hành động (CTA)**:
  - Hiển thị Banner chính với thông điệp "MUA BÁN VỚI TAPHOAMMO - An toàn - Nhanh chóng - Tiện lợi".
  - Hiển thị 2 nút CTA điều hướng nhanh: "Nạp tiền ngay" và "Đăng ký ngay".
- **Section Tính năng nổi bật (Core Values)**:
  - Hiển thị tĩnh 4 cam kết của sàn: "Bảo mật tài khoản", "Nạp tiền tự động", "Giao dịch an toàn" (Escrow), và "Hỗ trợ 24/7".
- **Section Danh mục & Sản phẩm (Product Showcase)**:
  - **THE system SHALL** truy vấn và hiển thị các danh mục chính (Ví dụ: Thuê Code & Dịch Vụ, Mua Bán Phần Mềm, Tài Khoản Premium) từ bảng `Categories`.
  - Bên dưới mỗi danh mục, **THE system SHALL** hiển thị lưới các sản phẩm (`Products`) thuộc danh mục đó kèm theo Hình ảnh, Tên sản phẩm, Mô tả ngắn và nút "Xem thêm" / "Mua ngay".
- **Global Search (Tìm kiếm toàn cục)**:
  - **WHEN** người dùng nhập từ khóa vào Search bar và nhấn Enter, **THE system SHALL** truy vấn tìm kiếm gần đúng (LIKE) trên các trường `name` và `description` của bảng `Products` và chuyển hướng sang trang Kết quả tìm kiếm.
- **Footer & Sticky Contact**:
  - Hiển thị thông tin liên hệ tĩnh, chính sách, và các logo cổng thanh toán hỗ trợ (MoMo, VNPay, Visa, v.v.).
  - Hiển thị thanh công cụ liên hệ thả nổi (Sticky Sidebar) chứa các nút gọi nhanh ra ứng dụng ngoài: Zalo, Facebook Messenger, Telegram, và Hotline.

## Business Rules
- **Quy tắc hiển thị sản phẩm**: Hệ thống CHỈ được phép hiển thị các danh mục và sản phẩm có cờ `isDelete = 0`. Các sản phẩm bị ẩn hoặc shop bị khóa (Banned) tuyệt đối không được xuất hiện trên Homepage.
- **Giới hạn truy vấn (Pagination/Limit)**: Để đảm bảo thời gian phản hồi (Response time) < 2 giây, mỗi danh mục trên trang chủ chỉ hiển thị tối đa 8-12 sản phẩm mới nhất hoặc bán chạy nhất. Cần bấm "Xem tất cả" để load thêm.
- **Quy tắc điều hướng (Authentication Redirect)**: Nếu Guest (chưa đăng nhập) bấm vào các menu yêu cầu quyền hạn như "Nạp tiền" hoặc nút "Nạp tiền ngay", hệ thống bắt buộc phải điều hướng (Redirect) về trang Đăng nhập (`/login`) trước khi cho phép đi tiếp.

**Vai trò:** Bạn là một Expert Frontend Developer.

**Công nghệ sử dụng:** ReactJS (hoặc HTML/JS thuần), CSS thuần (Plain CSS) và thư viện icon (Lucide-react hoặc FontAwesome). Tuyệt đối KHÔNG sử dụng Tailwind CSS.

**Yêu cầu:** Hãy tạo cho tôi một component `AdvancedSearchBar` mô phỏng thanh tìm kiếm nâng cao của các sàn thương mại điện tử.

Component này bao gồm 2 file: một file giao diện (`AdvancedSearchBar.jsx`) và một file style riêng biệt (`AdvancedSearchBar.css`).

Dưới đây là đặc tả chi tiết về cấu trúc UI/UX:

### 1. Bố cục chính (Main Container)
- Tạo một thẻ `div` hoặc `form` bọc ngoài cùng (class: `.search-container`).
- Yêu cầu CSS: `display: flex`, `align-items: center`, background màu trắng, viền mỏng màu xám, bo góc (`border-radius`), chiều cao khoảng `45px`.
- Khi focus vào ô input bên trong, `.search-container` đổi màu viền sang màu primary của dự án.

### 2. Phần bên trái: Dropdown chọn danh mục
- Tạo một khối (class: `.search-category-dropdown`).
- CSS mặc định: Nền xám nhạt, padding vừa phải, có viền phải (`border-right`) để ngăn cách với input. Hiển thị chữ "All" và icon mũi tên. `cursor: pointer`.
- Khi click vào, hiển thị một panel con (class: `.dropdown-panel`) có `position: absolute`, nằm ngay dưới thanh search.
- Trong `.dropdown-panel` có:
  - Một ô input nhỏ để tìm danh mục.
  - Một danh sách có scroll (`max-height: 250px`, `overflow-y: auto`) chứa các tên danh mục (ví dụ: Tất cả, Phần mềm, Tài khoản). Hover vào từng item thì đổi màu nền.

### 3. Phần trung tâm: Ô nhập từ khóa (Main Input)
- Tạo một thẻ `<input type="text">` (class: `.search-input`).
- CSS: `flex: 1` (chiếm toàn bộ không gian còn lại), xóa bỏ `border` và `outline` mặc định, padding hợp lý.
- Placeholder: "Từ khóa, Tài khoản, Mã nguồn,..."

### 4. Phần bên phải: Nút tìm kiếm (Search Button)
- Tạo một nút `<button>` (class: `.search-button`).
- CSS: Background màu primary (ví dụ: `#0f172a`), icon màu trắng. Không có text, chỉ có icon kính lúp.
- Hover: Đổi màu nền tối hơn một chút.
- Phải bo góc bên phải sao cho khớp hoàn hảo với góc bo của `.search-container`.

### 5. Phần bên dưới: Từ khóa gợi ý (Quick Links)
- Tạo một khối nằm ngay dưới thanh search (class: `.search-quick-links`).
- CSS: `display: flex`, `gap: 15px`, `margin-top: 10px`.
- Chứa các thẻ `<a>` (class: `.quick-link-item`). Font chữ nhỏ, màu xám nhạt. Hover vào thì có gạch chân (`text-decoration: underline`) và đổi màu sang primary.

**Yêu cầu cho AI:** - Viết đầy đủ logic React (useState) để xử lý việc đóng/mở dropdown.
- Code CSS phải sạch sẽ, đặt tên class theo chuẩn BEM (ví dụ: `search-bar__input`) để không bị xung đột với các trang khác.