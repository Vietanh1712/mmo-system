# PLAN — Xác thực định danh KYC (`feat-kyc`)

## 1. Mục tiêu (Goals)

Triển khai luồng xác thực định danh KYC (Know Your Customer) phía người dùng (Customer) và luồng phê duyệt phía nhân viên (Staff) theo `SPEC.md` (UC-03). Hệ thống cho phép người dùng gửi hồ sơ gồm thông tin cá nhân và 3 loại ảnh chụp (mặt trước, mặt sau, chân dung), đồng thời cho phép Staff xem danh sách phân trang, chi tiết hồ sơ, duyệt hoặc từ chối kèm lý do — tuân thủ hiến pháp dự án `AGENTS.md` và các quy tắc nghiệp vụ ví tài chính.

## 2. Kiến trúc & Công nghệ

- **Backend:** Java 17, Spring Boot 3.1, Spring Data JPA, SQL Server (T-SQL).
- **Frontend:** Thymeleaf template engine, CSS thuần, Javascript thuần (`authFetch` giao tiếp qua REST API có JWT, với base path `/api`).
- **Tuân thủ:** Controller → Service → Repository → Entity; DTO Pattern (không trả JPA Entity trực tiếp ra API); business logic hoàn toàn xử lý ở backend; Soft Delete với `isDelete = 0`.

## 3. Các thành phần Backend

### 3.1. Database Migration & Entities

- Bản ghi được lưu ở bảng `KYCRequests` liên kết với bảng `Users` qua khóa ngoại `user_id`.
- Entity `KycRequest` (`@Entity` mapping bảng `KYCRequests`):
  - Chứa thông tin `idNumber`, `idType` (CCCD, CMND, PASSPORT, DRIVER_LICENSE), `requestCode` độc nhất.
  - Lưu tên file ảnh an toàn: `frontIdImage`, `backIdImage`, `selfieImage`.
  - Khóa lạc quan `@Version` trường `version` kiểu `Integer` để chống race condition khi phê duyệt đồng thời.
  - `activeUserId` dùng để duy trì index độc nhất: chỉ có tối đa 1 hồ sơ hoạt động (`PENDING` hoặc `APPROVED`) cho mỗi user.
  - Soft delete qua trường `isDelete` (kiểu `Boolean`).

### 3.2. Repositories (Spring Data JPA)

- `KycRequestRepository`:
  - `findByIdAndIsDeleteFalse(id)`: Lấy chi tiết hồ sơ.
  - `findByUser_IdAndIsDeleteFalseOrderByCreatedAtDesc(userId)`: Lấy lịch sử KYC của user.
  - `findAllByIsDeleteFalse(pageable)`: Lấy toàn bộ danh sách KYC phân trang phục vụ Staff.
  - `findByStatusAndIsDeleteFalse(status, pageable)`: Lọc danh sách KYC theo trạng thái phân trang.
  - `searchKycRequests(status, requestCode, idType, pageable)`: Tìm kiếm và lọc động yêu cầu KYC theo status, requestCode, và idType phục vụ Staff.
  - `existsByActiveUserId(activeUserId)`: Kiểm tra xem user có yêu cầu KYC nào đang xử lý/hoàn thành không.

### 3.3. DTOs

- Request: `KycReviewRequest` (chứa `status`, `rejectionReason`, `version` của Staff gửi lên).
- Response: `KycResponseDto` (chứa `id`, `idNumber`, `idType`, `requestCode`, `status`, `rejectionReason`, `version`, `createdAt`, `updatedAt`). Mapping tại Service.

### 3.4. Services (Business Logic)

- **`KycService`**:
  - `submitKyc(userId, fullName, dateOfBirth, address, idNumber, idType, front, back, selfie)`:
    - Tìm và cập nhật trực tiếp thông tin cá nhân `fullName`, `address`, `dateOfBirth` sang bảng `Users`.
    - Kiểm tra nếu tồn tại `existsByActiveUserId(userId)` thì ném lỗi `IllegalStateException` chặn gửi trùng lặp.
    - Lưu file qua `KycStorageService`.
    - Tạo và lưu `KycRequest` với `status = KycStatus.PENDING` và gán `activeUserId = userId`.
  - `reviewKycRequest(id, reviewerId, reviewRequest)`:
    - Kiểm tra `@Version` khớp giữa DTO và DB, nếu lệch ném `ObjectOptimisticLockingFailureException`.
    - Chỉ cho phép duyệt khi trạng thái hiện tại là `PENDING`.
    - Nếu `REJECTED`: Gán `activeUserId = null` để giải phóng trạng thái active, cho phép user gửi lại yêu cầu mới. Lưu lý do từ chối.
  - `getKycDocument(kycId, docType, userId, isStaff)`: Lấy file ảnh tương ứng. Kiểm tra bảo mật nếu không phải Staff/Admin và không phải chủ nhân hồ sơ thì chặn truy cập.

- **`KycStorageService`**:
  - `storeFile(file)`: Đổi tên file định danh duy nhất và lưu vào ổ đĩa.
  - `getFile(fileName)`: Trả về đối tượng `File` bảo mật trên ổ đĩa server.

### 3.5. Controllers & Security

- **`KycController`** (`/api/v1/kyc`):
  - `POST /`: Customer nộp hồ sơ KYC (`@PreAuthorize("hasAuthority('ROLE_CUSTOMER')")`).
  - `GET /me`: Customer xem lịch sử KYC (`@PreAuthorize("hasAnyAuthority('ROLE_CUSTOMER', 'ROLE_SELLER')")`).
  - `GET /{id}/documents/{docType}`: Tải ảnh hồ sơ định dạng nhạy cảm (`@PreAuthorize` phân quyền chi tiết).
- **`StaffKycController`** (`/api/v1/staff/kyc`):
  - `GET /`: Lấy danh sách hồ sơ (`@PreAuthorize("hasAnyAuthority('ROLE_STAFF', 'ROLE_ADMIN')")`).
  - `GET /{id}`: Xem chi tiết hồ sơ (`@PreAuthorize("hasAnyAuthority('ROLE_STAFF', 'ROLE_ADMIN')")`).
  - `POST /{id}/review`: Phê duyệt/từ chối hồ sơ (`@PreAuthorize("hasAnyAuthority('ROLE_STAFF', 'ROLE_ADMIN')")`).

---

## 4. Các thành phần Frontend

Sử dụng Thymeleaf kết hợp với JS thuần gọi API qua `authFetch` (tiền tố `/api` tự động được ghép bởi `API_BASE`):

- **Trang KYC của Học viên/Khách hàng:**
  - File: `templates/account/kyc.html` và [account-kyc.js](file:///c:/Users/pc/MMO_new1/MMO_Market/MMO_Market%20(3)/MMO_Market/apps/frontend/static/js/customer/account-kyc.js).
  - Tải thông tin hiện tại qua `/v1/profile` và trạng thái hồ sơ qua `/v1/kyc/me`.
  - Hiển thị UI đa trạng thái: Chưa định danh, Đang chờ duyệt, Đã xác minh, Bị từ chối (kèm nút Gửi lại).
  - Form submit chứa thông tin cá nhân (hỗ trợ 4 loại giấy tờ định danh: CCCD, CMND, PASSPORT, DRIVER_LICENSE) và 3 ô tải ảnh. Gọi API `POST /api/v1/kyc`.

- **Trang danh sách duyệt KYC của Staff:**
  - File: `templates/staff/kyc.html` và [staff-kyc.js](file:///c:/Users/pc/MMO_new1/MMO_Market/MMO_Market%20(3)/MMO_Market/apps/frontend/static/js/staff/staff-kyc.js).
  - Gọi API lấy danh sách thông qua `/v1/staff/kyc` (tránh lặp tiền tố `/api`).
  - Hỗ trợ lọc theo mã hồ sơ (text search), loại giấy tờ (dropdown select), và trạng thái hồ sơ.
  - Phân trang kết quả kiểm duyệt đồng bộ thiết kế hệ thống và sắp xếp theo ID tăng dần.

- **Trang chi tiết và xử lý duyệt của Staff:**
  - File: `templates/staff/kyc-detail.html` và [staff-kyc-detail.js](file:///c:/Users/pc/MMO_new1/MMO_Market/MMO_Market%20(3)/MMO_Market/apps/frontend/static/js/staff/staff-kyc-detail.js).
  - Gọi API `/v1/staff/kyc/{id}` lấy chi tiết hồ sơ.
  - Tải ảnh trực tiếp có đính kèm Access Token bảo mật.
  - Form phê duyệt: Nút Duyệt / Từ chối (bắt buộc nhập lý do).

---

## 5. Definition of Done

- Toàn bộ thay đổi phải tuân thủ việc gọi API qua `authFetch` chuẩn xác không lặp tiền tố `/api` (không dùng `/api/v1` mà dùng `/v1` trong tham số).
- Unit test Service đạt độ bao phủ cần thiết.
- Phê duyệt đồng thời phải kích hoạt Optimistic Locking thành công và hiển thị thông báo lỗi thân thiện thay vì crash.
- Mọi API endpoint cho Staff phải được kiểm soát chặt chẽ bằng `@PreAuthorize("hasAnyAuthority('ROLE_STAFF', 'ROLE_ADMIN')")`.
- Soft delete và lọc trạng thái `isDelete = 0` được áp dụng trên mọi query của `KycRequestRepository`.