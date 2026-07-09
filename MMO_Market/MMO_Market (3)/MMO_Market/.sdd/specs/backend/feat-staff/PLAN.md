# PLAN — Staff Operations & Approvals (`feat-staff`)

## 1. Mục tiêu (Goals)

Triển khai giao diện vận hành MVC và REST API dành riêng cho nhân viên vận hành (Staff) và quản trị viên (Admin) để xử lý các yêu cầu phê duyệt trong hệ thống theo đặc tả `SPEC.md` (feat-staff). Các nghiệp vụ bao gồm:
- Xem thống kê hoạt động trên Dashboard (khiếu nại đang xử lý, KYC chờ duyệt, yêu cầu rút tiền mới).
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
  - File: `templates/fragments/staff-sidebar.html` hiển thị danh sách các chức năng chính, bổ sung menu **Shops** liên kết đến `/staff/shop-registrations`.
- **Trang Quản lý Cờ Cảnh Báo (Shop Flags):**
  - File: `templates/staff/flags.html` & `flag-detail.html`, cùng file JS quản lý.
- **Trang Thống kê Dashboard:**
  - File: `templates/staff/dashboard.html` hiển thị biểu đồ và tổng quan hệ thống, bổ sung thẻ **Quản lý Shop** vào phần Truy cập nhanh.
- **Trang Quản lý Duyệt Yêu Cầu Shop (Shops):**
  - File: `templates/staff/shop-registrations.html` được thiết kế đồng bộ với KYC/Complaints với bảng thống kê 4 thẻ, bộ lọc tìm kiếm, bảng danh sách phân trang (có STT và Trạng thái) và Modal chi tiết duyệt shop.
  - Script: `static/js/staff/staff-shop-registrations.js` gọi các API phân trang/tìm kiếm, thống kê số liệu, tích hợp phân trang chung và điều khiển Modal hiển thị dữ liệu chi tiết kiểm duyệt.

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
