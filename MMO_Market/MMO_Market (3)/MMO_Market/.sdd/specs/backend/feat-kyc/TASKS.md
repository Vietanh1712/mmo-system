# TASKS — KYC Verification (`feat-kyc`)

> **Feature ID:** `feat-kyc` | **UC Coverage:** UC-03 (KYC Verification)
> **Phiên bản:** 1.0 | **Cập nhật:** 2026-06-27

---

## Phase 1: Database & Entities

- [x] **1.1** Database script khởi tạo bảng `KYCRequests` liên kết khóa ngoại với bảng `Users` và lưu thông tin căn cước công dân `citizen_id` và các tệp hình ảnh.
- [x] **1.2** JPA Entity `KycRequest` — map chính xác các trường `citizenId`, `frontIdImage`, `backIdImage`, `selfieImage`, `status` và `rejectionReason`.

## Phase 2: Repositories

- [x] **2.1** `KycRequestRepository` — cung cấp truy vấn kiểm tra hồ sơ theo user `findByUserIdAndIsDeleteFalse` và tìm kiếm theo trạng thái.

## Phase 3: DTOs & Validation

- [x] **3.1** Ràng buộc validate định dạng tệp ảnh (jpg, png) tại tầng Controller trước khi chuyển xử lý.
- [x] **3.2** `KycResponseDto` — trả về trạng thái định danh rõ ràng.

## Phase 4: Business Logic (Services)

- [x] **4.1** `KycService.submitKyc()` — kiểm tra điều kiện tài khoản chưa được định danh, lưu vết hồ sơ định danh dưới dạng trạng thái `Pending`.
- [x] **4.2** `KycStorageService` — xử lý lưu trữ tệp hình ảnh tài liệu KYC an toàn dưới định dạng tên ngẫu nhiên tránh lộ thông tin.

## Phase 5: Controllers & Security

- [x] **5.1** `KycController` — tiếp nhận hồ sơ qua `POST /api/v1/kyc` dưới dạng multipart form-data.
- [x] **5.2** Chỉ cho phép người dùng đăng nhập truy cập các tài nguyên KYC cá nhân qua phân quyền Security.

## Phase 6: Testing

- [x] **6.1** Unit test kiểm tra luồng chặn không cho gửi thêm KYC khi có hồ sơ đang xử lý.