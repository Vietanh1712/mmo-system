# TASKS — KYC Verification (`feat-kyc`)

> **Feature ID:** `feat-kyc` | **UC Coverage:** UC-03 (KYC Verification)

---

## Phase 1: Database & Entities

- [x] **1.1** Database script khởi tạo bảng `KYCRequests` liên kết khóa ngoại với bảng `Users`, lưu thông tin `id_number`, `id_type`, `request_code`, `active_user_id` và các tệp hình ảnh.
- [x] **1.2** JPA Entity `KycRequest` (`KycRequest.java`) — mapping chính xác các trường `idNumber`, `idType` (enum `IdType`), `requestCode`, `activeUserId`, `version` (Optimistic Locking), `frontIdImage`, `backIdImage`, `selfieImage`, `status` (enum `KycStatus`), `reviewedBy`, `reviewedAt`, `rejectionReason`, `isDelete`.

## Phase 2: Repositories

- [x] **2.1** `KycRequestRepository` — cung cấp các truy vấn:
  - `findByIdAndIsDeleteFalse(id)`: Lấy chi tiết hồ sơ.
  - `findByUser_IdAndIsDeleteFalseOrderByCreatedAtDesc(userId)`: Lấy lịch sử KYC của user.
  - `findAllByIsDeleteFalse(pageable)`: Lấy toàn bộ danh sách.
  - `findByStatusAndIsDeleteFalse(status, pageable)`: Lọc theo trạng thái.
  - `searchKycRequests(status, cleanCode, idType, pageable)`: Tìm kiếm đa trường động.
  - `existsByActiveUserId(activeUserId)`: Kiểm tra hồ sơ KYC hoạt động để chặn gửi trùng lặp.

## Phase 3: DTOs & Validation

- [x] **3.1** Request DTO: `KycReviewRequest` (chứa `status`, `rejectionReason`, `version`).
- [x] **3.2** Response DTO: `KycResponseDto` (chứa đầy đủ thông tin hồ sơ KYC và thông tin cá nhân của user để trả về Client).
- [x] **3.3** Ràng buộc validate định dạng và dung lượng ảnh tại tầng Controller/Service trước khi lưu trữ.

## Phase 4: Business Logic (Services)

- [x] **4.1** `KycStorageService` — xử lý lưu trữ tệp hình ảnh an toàn, sinh tên file ngẫu nhiên sử dụng UUID để tránh trùng lặp và rò rỉ thông tin.
- [x] **4.2** `KycService.submitKyc()` — cập nhật thông tin cá nhân (`fullName`, `address`, `dateOfBirth`) vào `User`, kiểm tra `existsByActiveUserId` (chặn nộp trùng), lưu ảnh, sinh `requestCode` duy nhất, lưu `KycRequest` trạng thái `PENDING` và tạo thông báo hệ thống gửi cho Customer & Staff.
- [x] **4.3** `KycService.getMyKycHistory()` — lấy lịch sử định danh của user đang đăng nhập.
- [x] **4.4** `KycService.getKycRequestById()` — lấy chi tiết hồ sơ KYC phục vụ kiểm duyệt.
- [x] **4.5** `KycService.getKycDocument()` — tải tệp ảnh an toàn, kiểm tra quyền truy cập (chỉ cho phép Staff/Admin hoặc chính chủ sở hữu hồ sơ).
- [x] **4.6** `KycService.reviewKycRequest()` — phê duyệt hoặc từ chối hồ sơ. Kiểm tra khóa lạc quan `@Version` để chống race condition. Nếu từ chối, giải phóng `activeUserId = null` để cho phép nộp lại và lưu lý do từ chối. Tạo thông báo kết quả gửi cho Customer.
- [x] **4.7** `KycService.getKycStatistics()` — lấy số liệu thống kê tổng số, chờ duyệt, đã duyệt, từ chối.

## Phase 5: Controllers & Security

- [x] **5.1** `KycController` (REST API `/api/v1/kyc`):
  - `POST /` — nộp hồ sơ KYC (`ROLE_CUSTOMER`, `ROLE_SELLER`). Trả về HTTP 201 hoặc 409 khi bị trùng.
  - `GET /me` — xem lịch sử KYC (`ROLE_CUSTOMER`, `ROLE_SELLER`).
  - `GET /{id}/documents/{docType}` — tải ảnh KYC (`ROLE_CUSTOMER`, `ROLE_SELLER`, `ROLE_STAFF`, `ROLE_ADMIN`).
- [x] **5.2** `StaffKycController` (REST API `/api/v1/staff/kyc`):
  - `GET /` — lấy danh sách phân trang, hỗ trợ bộ lọc và search động, sắp xếp giảm dần theo `createdAt` (`ROLE_STAFF`, `ROLE_ADMIN`).
  - `GET /{id}` — lấy chi tiết một hồ sơ (`ROLE_STAFF`, `ROLE_ADMIN`).
  - `POST /{id}/review` — duyệt/từ chối, xử lý exception Optimistic Lock trả HTTP 409 (`ROLE_STAFF`, `ROLE_ADMIN`).
  - `GET /stats` — xem thống kê KYC (`ROLE_STAFF`, `ROLE_ADMIN`).
- [x] **5.3** `DemoKycController` (REST API `/api/demo`) — cung cấp các endpoint stub duyệt nhanh phục vụ test nhanh/demo.
- [x] **5.4** Tích hợp phân quyền Spring Security và filter JWT (`JwtAuthenticationFilter`) bảo vệ toàn bộ API KYC nhạy cảm.

## Phase 6: Testing

- [ ] **6.1** Unit test kiểm tra luồng chặn không cho gửi thêm KYC khi có hồ sơ đang xử lý (chưa có trong source code test).
- [ ] **6.2** Unit test kiểm tra luồng Optimistic Locking hoạt động đúng khi có nhiều Staff duyệt đồng thời (chưa có trong source code test).

## Phase 7: Final Review

- [x] **7.1** Đảm bảo ảnh KYC không thể truy cập công khai nếu không đính kèm Bearer Token hợp lệ.
- [x] **7.2** Check tính nhất quán của trạng thái `activeUserId` trên DB khi hồ sơ chuyển từ PENDING sang APPROVED hoặc REJECTED.