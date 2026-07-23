# PLAN — Staff Operations & Approvals (`feat-staff`)

## 1. Mục tiêu (Goals)

Triển khai giao diện vận hành MVC và REST API dành riêng cho nhân viên vận hành (Staff) và quản trị viên (Admin) để xử lý các yêu cầu phê duyệt trong hệ thống theo đặc tả `SPEC.md` (feat-staff). Các nghiệp vụ bao gồm:
- Xem thống kê hoạt động trên Dashboard (tổng số shop, KYC chờ duyệt, cờ cảnh báo shop).
- Lọc và kiểm duyệt yêu cầu rút tiền ngân hàng của Seller.
- Quản lý cờ cảnh báo (Flags) cho các Shop/Seller vi phạm chính sách sàn giao dịch (đánh giá nguy hiểm: Warning, Critical, Danger).
- Xem và xử lý khiếu nại (Complaints) cũng như lịch sử giao dịch toàn hệ thống.

## 2. Kiến trúc & Công nghệ

- **Backend:** Java 17, Spring Boot 3.1, Spring Data JPA, SQL Server (T-SQL).
- **Frontend:** Thymeleaf template engine kết hợp CSS & JS thuần (gọi API thông qua `authFetch` kèm JWT token).
- **Tuân thủ:** Phân chia rõ ràng giữa MVC Controller phục vụ việc render giao diện tĩnh và REST API Controller xử lý dữ liệu động. Kiểm soát quyền hạn truy cập thông qua `@PreAuthorize("hasAnyAuthority('ROLE_STAFF', 'ROLE_ADMIN')")`.

## 3. Các thành phần Backend

### 3.1. Database Migration & Entities

- **Entity `ShopFlag`** (bảng `ShopFlags`):
  - `shop` (ManyToOne -> User): Shop bị đánh cờ cảnh báo.
  - `reason` (NVARCHAR): Lý do cắm cờ.
  - `flagLevel` (VARCHAR): Mức độ nghiêm trọng (`Warning`, `Critical`, `Danger`).
  - `status` (VARCHAR): Trạng thái hoạt động của cờ (`Active`, `Removed`).
  - `createdBy` (ManyToOne -> User): Staff/Admin tạo cờ.
  - `isDelete` (BIT): Soft delete flag.

### 3.2. Repositories (Spring Data JPA)

- `ShopFlagRepository`:
  - `countByIsDeleteFalse()`: Đếm tổng số cờ đang hoạt động.
  - `searchFlags(keyword, level, status, pageable)`: Tìm kiếm phân trang cờ.
  - `findDistinctFlagLevels()`: Lấy danh sách các mức độ cờ duy nhất từ database.
  - `findDistinctStatuses()`: Lấy danh sách các trạng thái cờ duy nhất từ database.
- `SellerRegistrationRepository`:
  - `countByIsDeleteFalse()`: Đếm tổng số yêu cầu đăng ký shop.
  - `countByStatusIgnoreCaseAndIsDeleteFalse(status)`: Đếm số yêu cầu theo trạng thái không phân biệt chữ hoa/thường.
  - `searchRegistrations(status, keyword, searchId, pageable)`: Tìm kiếm phân trang kết hợp tìm theo mã ID hoặc chứa chuỗi từ khóa.
- `ComplaintRepository`, `WithdrawalRepository`, `KycRequestRepository`:
  - Cung cấp các hàm đếm số lượng hồ sơ `PENDING` phục vụ hiển thị số lượng badge trên menu sidebar của Staff.

### 3.3. DTOs

- Sử dụng các lớp DTO chung cho dữ liệu thống kê hoặc đóng gói trực tiếp bằng Map trong REST API.

### 3.4. Services (Business Logic)

- **`StaffDashboardService`**:
  - `getDashboardData()`: Trích xuất các số liệu thống kê dòng tiền, doanh số toàn sàn, tổng số người dùng, biểu đồ tăng trưởng và các đầu việc cần xử lý.
- **`ShopRegistrationService`**:
  - `getAllRegistrations(status, keyword, page, size)`: Phân trang và tìm kiếm yêu cầu mở shop. Tự bóc tách chuỗi mã `SHOP-XXXXXX` thành ID để so khớp.
  - `getRegistrationStats()`: Lấy số liệu thống kê (Total, Pending, Approved, Rejected) không phân biệt hoa thường.

### 3.5. Controllers & Security

- **`StaffController`** (MVC Controller `@RequestMapping("/staff")`):
  - Phục vụ tải giao diện các trang Thymeleaf cho Staff: `/dashboard`, `/kyc`, `/withdrawals`, `/complaints`, `/flags`, `/support-tickets`, `/shop-registrations`.
  - Có `@ModelAttribute` tự động điền số lượng hồ sơ chưa xử lý vào sidebar ở mọi trang.
- **REST Controllers** (như `StaffKycController`, `StaffWithdrawalController`, `ShopRegistrationController`):
  - Phục vụ các tác vụ bất đồng bộ AJAX và API REST (phê duyệt KYC, duyệt rút tiền, tạo cờ shop, duyệt/từ chối mở shop, thống kê đăng ký shop). Bảo vệ bằng Spring Security, từ chối truy cập nếu không có role `STAFF` hoặc `ADMIN`.

---

## 4. Các thành phần Frontend

- **Hệ thống Sidebar của Staff:**
  - File: `templates/fragments/staff-sidebar.html` hiển thị danh sách các chức năng chính. Nhóm các trang Khiếu nại, Giao dịch, và Rút tiền vào menu thả xuống (Dropdown) mới có tên là "**Quản lý đơn từ**". Bổ sung menu **Shops** liên kết đến `/staff/shop-registrations`.
- **Trang Quản lý Cờ Cảnh Báo (Shop Flags):**
  - File: `templates/staff/flags.html` & `flag-detail.html`, cùng file JS quản lý.
- **Trang Thống kê Dashboard:**
  - File: `templates/staff/dashboard.html` hiển thị biểu đồ và tổng quan hệ thống, bổ sung thẻ **Quản lý Shop** vào phần Truy cập nhanh.
- **Trang Quản lý KYC (KYC Verification):**
  - Script: `static/js/staff/staff-kyc.js` bỏ tự động kích hoạt tìm kiếm khi đổi tùy chọn bộ lọc (`change` event), chỉ thực hiện tìm kiếm khi Staff bấm nút "Tìm kiếm" hoặc nhấn `Enter`.
  - Template `templates/staff/kyc-detail.html`: Đặt nhãn trạng thái (`#kycStatusBadge` hiển thị "Đã duyệt", "Chờ duyệt", "Từ chối") cố định ở góc trên bên phải của thẻ thông tin người nộp (`.ds-card-header`).
- **Trang Quản lý Khiếu nại (Complaints):**
  - File: `templates/staff/complaints.html` & Script: `static/js/staff/staff-complaints.js` hiển thị cột STT liên tục qua từng trang (`currentPage * pageSize + index + 1`).
- **Trang Quản lý Duyệt Yêu Cầu Shop (Shops):**
  - File: `templates/staff/shop-registrations.html` & Script: `static/js/staff/staff-shop-registrations.js` quản lý 5 trạng thái gian hàng với nhãn chuẩn hóa: `Active` ("Hoạt động"), `Suspended` ("Tạm ngưng"), `Locked` ("Tạm khóa"), `Withdrawn` ("Đã đóng Shop (Hoàn cọc)"), `Banned` ("Khóa vĩnh viễn"). Các thẻ thống kê hiển thị: "Shop tạm khóa", "Shop tạm ngưng", "Đã đóng Shop (Hoàn cọc)".
  - Template `templates/staff/shop-registration-detail.html`: Đặt nhãn trạng thái (`#shopStatusBadge` hiển thị "Đã duyệt", "Chờ duyệt", "Từ chối") cố định ở góc trên bên phải của thẻ thông tin Shop (`.ds-card-header`). Tích hợp Modal tạm ngưng có thời hạn (`suspendShopModal` hỗ trợ chọn Ngày, Giờ, Phút `suspendedUntil`), hiển thị khung đếm ngược thời gian thực (Real-time countdown timer) ở cả thẻ thông tin Shop và thẻ Cập nhật trạng thái (`#cardSuspendedAlert`). Cơ chế tự động khôi phục về trạng thái Hoạt động (`Active`) khi hết thời hạn tạm ngưng via `@Scheduled` job hoặc lazy evaluation khi xem thông tin.
- **Trang Quản lý Phiếu Hỗ Trợ (Support Tickets - UC-17):**
  - File: `templates/staff/support-tickets.html` & `support-ticket-detail.html` cùng Script tương ứng tiếp nhận phiếu hỗ trợ, phản hồi giải pháp và chuyển đổi trạng thái (`OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`).

---

## 5. Definition of Done

- Sidebar luôn phải phản ánh chính xác số lượng hồ sơ chưa xử lý ở thời gian thực (real-time badge count).
- Mọi thay đổi trạng thái nhạy cảm (như duyệt rút tiền, gỡ cờ shop, giải quyết khiếu nại) chỉ được thực hiện bởi nhân viên có tài khoản hợp lệ với role `STAFF` hoặc `ADMIN` (xác thực chặt chẽ ở cả controller MVC và API REST).

---

## 6. Sắp xếp hiển thị dữ liệu theo thời gian mới nhất

Cập nhật tất cả các màn hình hiển thị danh sách của Staff, Customer và Product để mặc định sắp xếp theo thời gian tạo mới nhất (`createdAt` descending / DESC).

### 6.1. Chi tiết thay đổi Backend:
- **`StaffController.java`**:
  - Cấu hình lại `Pageable` của `/transactions`, `/withdrawals`, và `/flags` thành `Sort.by("createdAt").descending()`.
- **`StaffKycController.java`**:
  - Đổi sorting của `pageRequest` trong `/api/v1/staff/kyc` thành `Sort.by(Sort.Direction.DESC, "createdAt")`.
- **`ComplaintServiceImpl.java`**:
  - Đổi sorting trong `searchComplaintsForStaff` thành `Sort.by("createdAt").descending()`.
  - Thay đổi `getAllComplaintsForStaff` để gọi `complaintRepository.findAllByIsDeleteFalseOrderByCreatedAtDesc()`.
- **`PreOrderRepository.java` & `PreOrderService.java`**:
  - Thêm phương thức `findByCustomerAndIsDeleteFalseOrderByCreatedAtDesc(User customer)` vào repository.
  - Cập nhật service `getPreOrdersByCustomer` sử dụng phương thức sắp xếp mới để đảm bảo lịch sử đặt hàng trước hiển thị mới nhất trước.
- **`ProductSearchController.java`**:
  - Cấu hình `@PageableDefault` của `/products` có hướng sắp xếp mặc định là `Sort.Direction.DESC` theo `createdAt`.

---

## 7. Thêm cột Số thứ tự (STT) cho các bảng danh sách của Staff

Bổ sung cột STT (Sequence Number) được tính toán theo phân trang (`currentPage * pageSize + index + 1`) vào các danh sách quản lý của Staff.

### 7.1. Chi tiết thay đổi Frontend:
- **`withdrawals.html`**:
  - Bổ sung `<th class="ds-table-center" style="width: 60px;">STT</th>` vào header của bảng.
  - Sử dụng biến Thymeleaf `iterStat` để render `<td class="ds-table-center" th:text="${currentPage * pageSize + iterStat.count}">1</td>`.
- **`transactions.html`**:
  - Bổ sung `<th class="ds-table-center" style="width: 60px;">STT</th>` vào header của bảng.
  - Sử dụng biến Thymeleaf `iterStat` để render `<td class="ds-table-center" th:text="${currentPage * pageSize + iterStat.count}">1</td>`.
- **`kyc.html` & `staff-kyc.js`**:
  - Bổ sung `<th class="ds-table-center" style="width: 60px;">STT</th>` và cập nhật `colspan` từ 6 thành 7 trong `kyc.html`.
  - Cập nhật hàm `loadKycList` và `renderTable` trong `staff-kyc.js` để truyền `page` và `size`, tính toán `stt = page * size + index + 1` và hiển thị trên cột đầu tiên.

---

## 8. Tích hợp Quản lý Shop (phiên bản 2.0)
- **`User.java`**: Thêm trường `depositVnd` để quản lý tiền cọc ký quỹ của shop.
- **`ShopRegistrationResponseDto.java`**: Thêm các trường `shopStatus`, `depositVnd`, `balanceVnd` và `ownerName` để trả về thông tin shop và chủ shop đi kèm.
- **`UserRepository.java` & `SellerRegistrationRepository.java`**:
  - `UserRepository` hỗ trợ lấy danh sách các trạng thái shop duy nhất (`findDistinctShopStatuses()`).
  - `SellerRegistrationRepository` hỗ trợ lấy danh sách các trạng thái yêu cầu duy nhất (`findDistinctStatuses()`) và cập nhật câu truy vấn để hỗ trợ lọc theo trạng thái shop (`shopStatus`) và từ khóa keyword.
- **`ShopRegistrationController.java` & `ShopRegistrationService.java`**:
  - Cung cấp REST API endpoint `GET /api/v1/shop-registrations/shop-statuses` và `GET /api/v1/shop-registrations/statuses` để tải động danh sách bộ lọc.
  - Cung cấp API `PUT /api/v1/shop-registrations/{id}/toggle-status?active={true/false}` để Bật/Tắt trạng thái hoạt động của Shop (cập nhật User's `shopStatus` thành `Active` hoặc `Banned`).
- **`shop-registrations.html` & `staff-shop-registrations.js`**:
  - Loại bỏ cột "Danh mục".
  - Thêm 3 cột: "Trạng thái Shop" (hiển thị badge), "Tiền cọc (VNĐ)" và "Số dư (VNĐ)" (định dạng tiền tệ VNĐ).
  - Tích hợp bộ lọc tìm kiếm theo Trạng thái Shop và bộ phân trang chung.
  - Khai báo bổ sung tệp `staff-ui.js` để tránh lỗi `mountStaffPagination is not defined`.
  - Thực hiện dịch hoàn toàn các giá trị trạng thái shop (Pending, Approved, Active, Banned, Locked, Rejected) sang tiếng Việt trong hàm hiển thị badge JS.
  - Tải động danh sách các trạng thái tài khoản shop từ database lên thanh bộ lọc để giải quyết triệt để lỗi không tìm kiếm được do lệch giá trị cứng (ví dụ database lưu "Approved" nhưng UI gửi "ACTIVE").
  - Loại bỏ nút hình con mắt trong cột Thao tác và thay thế bằng nút gạt Bật/Tắt trạng thái shop; loại bỏ hoàn toàn các liên kết/modal xem chi tiết.

## 9. Cải tiến Quản lý Shop (phiên bản 2.1 & 2.2 & 2.3)
- **`ShopRegistrationController.java` & `ShopRegistrationService.java`**:
  - Triển khai API `GET /api/v1/shop-registrations/{id}` lấy thông tin chi tiết một đăng ký Shop theo ID.
- **`StaffController.java`**:
  - Thêm route MVC `/staff/shop-registrations/detail` để trả về giao diện xem chi tiết.
- **`shop-registrations.html` & `staff-shop-registrations.js`**:
  - Loại bỏ hoàn toàn modal review cũ.
  - Chuyển đổi nút xem chi tiết (biểu tượng con mắt) thành liên kết điều hướng sang trang chi tiết mới.
  - Cấu hình cột "Tên Shop" trong bảng danh sách chỉ hiển thị duy nhất Tên Shop (loại bỏ phần mô tả, email và số điện thoại).
- **`shop-registration-detail.html` & `staff-shop-registration-detail.js`**:
  - Tạo trang chi tiết hiển thị đầy đủ thông tin về shop (loại bỏ trường danh mục và ngày nộp yêu cầu), chủ sở hữu (thông tin liên hệ email và số điện thoại được lấy trực tiếp từ bảng `Users` thay vì request), thông tin tài chính (tích hợp thêm thông tin tài khoản ngân hàng của Seller gồm Số tài khoản, Ngân hàng, Chi nhánh lấy từ bảng `SellerBankInfo`) và tích hợp panel duyệt/từ chối yêu cầu (kèm lý do từ chối) tương tự như KYC.

## 10. Tái cấu trúc Menu Sidebar (phiên bản 2.4)
- **`staff-sidebar.html`**:
  - Nhóm các tính năng Khiếu nại, Giao dịch và Rút tiền vào một Menu thả xuống (Dropdown) collapsible duy nhất có tên là "Quản lý đơn từ" để giao diện thanh bên của Staff được gọn gàng và khoa học hơn.
  - Tự động giữ trạng thái mở (`is-open`) nếu trang hiện tại thuộc một trong ba chức năng trong nhóm.
- **`staff.css` & `staff-ui.js`**:
  - Thêm CSS styling cho dropdown container, arrow rotate animation và slide down menu.
  - Thêm JS click handler để cho phép toggle thu/phóng dropdown thủ công.

## 11. Bổ sung Trang tổng quan Quản lý đơn từ (phiên bản 2.5)
- **`StaffController.java` & `documents-dashboard.html`**:
  - Triển khai endpoint `/staff/documents` hiển thị trang tổng quan riêng biệt cho phân hệ "Quản lý đơn từ", kết nối dữ liệu thống kê từ database (`dashboard.openComplaints`, `dashboard.pendingWithdrawals`, `dashboard.pendingTransactions`).
  - Thêm các thẻ truy cập nhanh đến 3 chức năng con: Khiếu nại, Giao dịch, Rút tiền.
- **Tái cấu trúc Sidebar Dropdown & Main Dashboard (`dashboard.html`)**:
  - Tách tiêu đề "Quản lý đơn từ" thành liên kết trực tiếp trỏ đến `/staff/documents`, giữ nút mũi tên làm công cụ đóng mở (toggle) danh sách menu con bên dưới.
  - Trên trang `/staff/dashboard` (dashboard.html), loại bỏ 2 liên kết truy cập nhanh "Quản lý khiếu nại" và "Quản lý rút tiền" riêng rẽ, thay thế bằng 1 thẻ duy nhất là "**Quản lý đơn từ**" trỏ đến `/staff/documents`.

## 12. Thay đổi chỉ số thống kê trên Dashboard chính (phiên bản 2.6)
- **`StaffDashboardDTO.java` & `StaffDashboardServiceImpl.java`**:
  - Triển khai thuộc tính `totalShops` lấy số lượng Shop đăng ký từ database bằng `sellerRegistrationRepository.countByIsDeleteFalse()`.
- **`dashboard.html`**:
  - Thay thế thẻ thống kê "**Khiếu nại đang mở**" bằng "**Tổng số Shop**" (màu xanh lá) để làm nổi bật quy mô hoạt động hệ thống.
  - Xóa bỏ hoàn toàn thẻ thống kê "**Yêu cầu rút tiền**" khỏi màn hình Dashboard chính (do chỉ số này đã chuyển sang trang tổng quan đơn từ).

## 13. Cập nhật giao diện Xử lý khiếu nại & Xem chi tiết giao dịch (phiên bản 2.7)
- **Cải tiến giao diện khiếu nại (`complaint-detail.html`)**:
  - Đổi nhãn trạng thái `"Từ chối khiếu nại"` thành `"Từ chối"`.
  - Thay thế hai nút bấm cứng `"Giải quyết"` và `"Từ chối"` bằng một nút bấm `"Cập nhật"` duy nhất kết nối trực tiếp với bộ chọn trạng thái xử lý (`complaintStatus`).
  - Loại bỏ hộp cảnh báo giả lập `ds-alert` `"Chế độ giả lập Staff — Cập nhật sẽ đồng bộ tức thì sang người dùng."`.
- **Cải tiến logic xử lý khiếu nại (`staff-complaint-detail.js`)**:
  - Triển khai hàm `submitComplaintStatus()` để đọc giá trị trạng thái từ bộ chọn và gửi đi qua hàm `handleStaffAction()`.
  - Sửa lại câu thông báo thành công (toast) hiển thị đúng theo trạng thái được chọn (`Đang xử lý` / `Đã giải quyết` / `Từ chối`).
  - Hỗ trợ fallback lý do giải quyết mặc định phù hợp cho trạng thái `InProgress`.
- **Cải tiến giao diện chi tiết giao dịch (`transaction-detail.html`)**:
  - Loại bỏ hộp cảnh báo `ds-alert` `"Giao dịch đã hoàn tất, tiền đã được giữ bảo lãnh theo quy trình C2C."` để làm gọn và trực quan hóa giao diện tóm tắt giao dịch.

## 14. Bổ sung 4 trạng thái Shop mới và nâng cấp 6 thẻ thống kê Shop (`staff-shop-registrations.js`) (phiên bản 2.8)
- **Ánh xạ trạng thái hiển thị (Badge & Filter dropdown)**:
  - `WITHDRAWN` hoặc `DELETED` -> "Đã xóa (rút tiền cọc)" (Badge đỏ / danh sách chọn lọc).
  - `SUSPENDED` hoặc `TEMP_LOCKED` -> "Khóa xóa có thời hạn" (Badge vàng / danh sách chọn lọc).
  - `LOCKED` || `INDEFINITE_LOCKED` -> "Khóa xóa vô thời hạn" (Badge vàng / danh sách chọn lọc).
  - `BANNED` hoặc `PERMANENT_BANNED` -> "Khóa Shop vĩnh viễn" (Badge đỏ / danh sách chọn lọc).
- **Thống kê 6 thẻ chỉ số mới**:
  - `shop-registrations.html` hiển thị 6 thẻ tương ứng với 6 chỉ số: Tổng số Shop, Tổng tiền cọc, Shop khóa vĩnh viễn, Shop khóa vô thời hạn, Shop khóa có thời hạn, Xóa Shop.
  - Tích hợp các hàm đếm chuyên biệt trong `UserRepository` (`countTotalShops`, `countPermanentBannedShops`, `countIndefiniteLockedShops`, `countTemporarySuspendedShops`, `countWithdrawnShops`, `sumTotalDeposit`) và cập nhật hàm trả về DTO ở Service.
  - Script Frontend `staff-shop-registrations.js` chịu trách nhiệm gọi API thống kê và gán giá trị tương ứng lên UI.

## 15. Thay thế nút Bật/Tắt hoạt động bằng nút sang trang Cập nhật trạng thái Shop riêng biệt (phiên bản 2.9)
- **Cải tiến danh sách Shop (`staff-shop-registrations.js`)**:
  - Gỡ bỏ điều khiển chuyển mạch toggle `ds-switch` khỏi danh sách hàng của bảng Shop.
  - Thay thế bằng liên kết nút bấm dạng `ds-btn ds-btn-sm ds-btn-outline` với nhãn `"Cập nhật trạng thái"`. Khi Staff ấn nút này sẽ điều hướng sang trang thay đổi trạng thái Shop riêng biệt `/staff/shop-registrations/update-status?id=...`.
- **Tạo trang Cập nhật trạng thái Shop riêng biệt (`shop-registration-update-status.html` & `staff-shop-registration-update-status.js`)**:
  - Tạo view MVC mới và tệp Javascript xử lý nghiệp vụ riêng biệt.
  - Hiển thị thông tin cơ bản của shop: Mã shop, Tên shop, Chủ sở hữu, Trạng thái hoạt động hiện tại.
  - Cung cấp một select dropdown chứa 5 tùy chọn trạng thái: Hoạt động (Active), Đã xóa (rút tiền cọc) (Withdrawn), Khóa xóa có thời hạn (Suspended), Khóa xóa vô thời hạn (Locked), Khóa Shop vĩnh viễn (Banned) cùng nút bấm `"Lưu cập nhật"`.
- **Triển khai REST API cập nhật trạng thái (`ShopRegistrationController.java` & `ShopRegistrationService.java`)**:
  - Cung cấp REST API endpoint `PUT /api/v1/shop-registrations/{id}/update-status?status=...` để lưu giá trị trạng thái hoạt động mới được chọn vào cột `shop_status` trong bảng `Users`.
  - Triển khai logic AJAX đồng bộ trong `staff-shop-registration-update-status.js` để gửi yêu cầu lưu và điều hướng quay lại danh sách.

## 16. Loại bỏ cột Lý do khỏi bảng danh sách cờ cảnh báo (phiên bản 3.0)
- **`flags.html`**:
  - Gỡ bỏ cột "Lý do" (cột tiêu đề `<th>Lý do</th>` và ô dữ liệu `<td th:text="${flag.reason}"></td>`) khỏi bảng hiển thị danh sách cờ cảnh báo để giao diện hiển thị tinh gọn hơn, tránh việc lý do dài gây vỡ khung bảng. Lý do chi tiết vẫn được hiển thị tại trang chi tiết cờ cảnh báo (`flag-detail.html`) và có thể chỉnh sửa tại đây.

## 17. Tích hợp Phiếu hỗ trợ vào Quản lý đơn từ (phiên bản 3.1)
- **Staff Sidebar & Documents Dashboard (`staff-sidebar.html`, `documents-dashboard.html`):**
  - Di chuyển menu liên kết "Phiếu Hỗ Trợ" từ cấp cao nhất vào bên trong menu thả xuống (Dropdown) "Quản lý đơn từ".
  - Cập nhật điều kiện hiển thị trạng thái mở (`is-open`) của Dropdown để tự động kích hoạt khi đang xem trang Phiếu hỗ trợ (`support-tickets`).
  - Bổ sung thẻ thống kê số lượng phiếu hỗ trợ chờ xử lý (`dashboard.pendingTickets`) và thẻ truy cập nhanh cho "Phiếu hỗ trợ" trong bảng chức năng của Documents Dashboard.
- **Staff Dashboard Service (`StaffDashboardServiceImpl.java` & `StaffDashboardDTO.java`):**
  - Bổ sung logic đếm số phiếu hỗ trợ có trạng thái "Open" hoặc "Processing" chưa bị xóa (`isDelete = false`) thông qua `SupportTicketRepository` để truyền giá trị vào DTO hiển thị.








