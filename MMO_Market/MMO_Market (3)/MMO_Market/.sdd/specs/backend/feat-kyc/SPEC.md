# SPEC — KYC Verification

> **Feature ID:** `feat-kyc`
> **UC Coverage:** UC-03 (KYC Verification)

---

## 1. CONTEXT & GOAL (BỐI CẢNH & MỤC TIÊU)

### 1.1 Bối cảnh
Để đảm bảo an toàn giao dịch trên sàn giao dịch C2C và tuân thủ các quy định phòng chống lừa đảo, người dùng muốn bán hàng hoặc rút tiền số lượng lớn cần thực hiện định danh danh tính (Know Your Customer - KYC).

### 1.2 Mục tiêu
- Người dùng (Customer/Seller) tải lên thông tin giấy tờ định danh (CCCD, CMND, PASSPORT, DRIVER_LICENSE) kèm 3 ảnh chụp: mặt trước, mặt sau, chân dung.
- Cho phép nhân viên vận hành (Staff/Admin) duyệt hoặc từ chối yêu cầu KYC kèm theo lý do cụ thể.
- Chống race condition khi nhiều Staff duyệt đồng thời bằng Optimistic Locking.
- Ngăn chặn người dùng gửi nhiều hồ sơ KYC chồng chéo.

---

## 2. ACTOR (TÁC NHÂN)

| Actor | Role | Điều kiện tiền quyết |
|---|---|---|
| **Customer** | Người gửi hồ sơ | Đã đăng nhập, role = Customer, chưa có hồ sơ PENDING/APPROVED |
| **Seller** | Người gửi hồ sơ | Đã đăng nhập, role = Seller, chưa có hồ sơ PENDING/APPROVED |
| **Staff** | Người kiểm duyệt | Đã đăng nhập với role = Staff |
| **Admin** | Người kiểm duyệt | Đã đăng nhập với role = Admin |

---

## 3. FUNCTIONAL REQUIREMENTS (Cú pháp EARS)

| ID | EARS Requirement |
|---|---|
| FR-KYC-01 | WHEN a Customer or Seller submits KYC files and details, THE SYSTEM SHALL save the request with status `PENDING` and generate a unique `requestCode`. |
| FR-KYC-02 | WHEN submitting KYC, THE SYSTEM SHALL check `existsByActiveUserId`. IF an active record (PENDING or APPROVED) already exists, THE SYSTEM SHALL reject with HTTP 409. |
| FR-KYC-03 | WHEN a Staff/Admin approves the KYC request, THE SYSTEM SHALL update `KycRequest.status = APPROVED`. The `activeUserId` remains set to preserve uniqueness. |
| FR-KYC-04 | WHEN a Staff/Admin rejects the KYC request, THE SYSTEM SHALL update `status = REJECTED`, save the rejection reason, and set `activeUserId = null` to allow the user to resubmit. |
| FR-KYC-05 | WHEN a Staff queries KYC requests with search input, THE SYSTEM SHALL search across `requestCode`, `idNumber`, `user.fullName`, and `user.email`, while automatically stripping any leading `#` character from `requestCode`. |
| FR-KYC-06 | WHEN a KYC request is submitted or reviewed (Approved/Rejected), THE SYSTEM SHALL create and persist system `Notification` records for the customer and all operating Staff/Admin. |
| FR-KYC-07 | WHEN a Staff/Admin requests KYC statistics, THE SYSTEM SHALL aggregate and return total, pending, approved, and rejected counts. |
| FR-KYC-08 | WHEN a Staff/Admin or the KYC owner requests a document image, THE SYSTEM SHALL serve the file inline. IF the requester is neither Staff/Admin nor the record owner, THE SYSTEM SHALL return HTTP 403. |
| FR-KYC-09 | WHEN two Staff attempt to review the same KYC record simultaneously, THE SYSTEM SHALL detect version mismatch via Optimistic Locking and return HTTP 409 with a user-friendly message. |

---

## 4. NON-FUNCTIONAL REQUIREMENTS

| ID | Category | Requirement |
|---|---|---|
| NFR-KYC-01 | Security | Ảnh tài liệu KYC nhạy cảm phải được bảo vệ tránh rò rỉ và chỉ cho phép chủ sở hữu hoặc Staff/Admin truy cập qua endpoint bảo mật. |
| NFR-KYC-02 | Compliance | Mã số định danh `id_number` phải được chuẩn hóa, hỗ trợ các loại giấy tờ: CCCD (12 số), CMND (9 số), PASSPORT, DRIVER_LICENSE. |
| NFR-KYC-03 | Concurrency | Mọi thao tác phê duyệt phải sử dụng Optimistic Locking (`@Version`) để chống race condition. |
| NFR-KYC-04 | Storage | File ảnh được lưu trên ổ đĩa server với tên file được đổi thành định danh duy nhất (UUID-based). |

---

## 5. DATA MODEL (Mô hình dữ liệu)

```sql
CREATE TABLE KYCRequests (
    id               BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id          BIGINT NOT NULL,
    request_code     VARCHAR(50) NOT NULL UNIQUE,      -- Mã hồ sơ độc nhất (tự sinh)
    full_name        NVARCHAR(255) NOT NULL,
    id_number        VARCHAR(20) NOT NULL,             -- Số giấy tờ định danh
    id_type          VARCHAR(30) NOT NULL,             -- CCCD | CMND | PASSPORT | DRIVER_LICENSE
    address          NVARCHAR(500) NULL,
    date_of_birth    DATE NULL,
    front_id_image   VARCHAR(255) NOT NULL,            -- Tên file ảnh mặt trước (đã đổi tên)
    back_id_image    VARCHAR(255) NOT NULL,            -- Tên file ảnh mặt sau (đã đổi tên)
    selfie_image     VARCHAR(255) NOT NULL,            -- Tên file ảnh chân dung (đã đổi tên)
    status           VARCHAR(20) DEFAULT 'PENDING',    -- PENDING | APPROVED | REJECTED
    rejection_reason NVARCHAR(MAX) NULL,
    active_user_id   BIGINT NULL UNIQUE,               -- = user_id khi PENDING/APPROVED; = NULL khi REJECTED
    reviewed_by      BIGINT NULL,
    reviewed_at      DATETIME NULL,
    version          INT DEFAULT 0,                    -- Optimistic Locking (@Version)
    created_at       DATETIME DEFAULT GETDATE(),
    updated_at       DATETIME DEFAULT GETDATE(),
    isDelete         BIT DEFAULT 0,
    CONSTRAINT FK_KYC_User  FOREIGN KEY (user_id)    REFERENCES Users(id),
    CONSTRAINT FK_KYC_Staff FOREIGN KEY (reviewed_by) REFERENCES Users(id)
);
```

> **Cơ chế `active_user_id`:** Khi user nộp hồ sơ → `active_user_id = user_id`. Khi bị REJECTED → `active_user_id = null` → user được phép nộp lại. Unique constraint trên `active_user_id` đảm bảo mỗi user chỉ có tối đa 1 hồ sơ active.

---

## 6. API SPEC (Đặc tả API)

### `POST /api/v1/kyc`
- **Auth:** `ROLE_CUSTOMER` hoặc `ROLE_SELLER`
- **Request Body (Multipart Form-Data):**

| Field | Type | Bắt buộc | Ghi chú |
|---|---|---|---|
| `fullName` | String | ✅ | |
| `dateOfBirth` | String | ✅ | Format: `yyyy-MM-dd` |
| `address` | String | ✅ | |
| `idNumber` | String | ✅ | Số giấy tờ định danh |
| `idType` | String | ✅ | `CCCD` / `CMND` / `PASSPORT` / `DRIVER_LICENSE` |
| `frontImage` | MultipartFile | ✅ | Ảnh mặt trước giấy tờ |
| `backImage` | MultipartFile | ✅ | Ảnh mặt sau giấy tờ |
| `selfieImage` | MultipartFile | ✅ | Ảnh chân dung |

- **Response 201 Created:**
  ```json
  {
    "id": 1,
    "requestCode": "#KYC-20260724-001",
    "fullName": "Nguyen Van A",
    "idNumber": "001095012345",
    "idType": "CCCD",
    "address": "Ha Noi",
    "dateOfBirth": "2000-01-01",
    "status": "PENDING",
    "version": 0,
    "createdAt": "2026-07-24T08:00:00"
  }
  ```
- **Response 409 Conflict:** `{ "success": false, "message": "Bạn đã có một hồ sơ KYC đang xử lý hoặc đã được phê duyệt." }`

---

### `GET /api/v1/kyc/me`
- **Auth:** `ROLE_CUSTOMER` hoặc `ROLE_SELLER`
- **Response 200:** Danh sách toàn bộ lịch sử KYC của user hiện tại, sắp xếp giảm dần theo `createdAt`.
  ```json
  [
    { "id": 1, "requestCode": "#KYC-...", "status": "APPROVED", "idType": "CCCD", ... },
    { "id": 2, "requestCode": "#KYC-...", "status": "REJECTED", "rejectionReason": "...", ... }
  ]
  ```

---

### `GET /api/v1/kyc/{id}/documents/{docType}`
- **Auth:** `ROLE_CUSTOMER`, `ROLE_SELLER`, `ROLE_STAFF`, `ROLE_ADMIN`
- **Path Params:** `id` (KYC request ID), `docType` (`front` / `back` / `selfie`)
- **Response 200:** File ảnh trả về dạng `inline`, Content-Type tự detect (image/jpeg, image/png...)
- **Response 403:** Requester không phải Staff/Admin và không phải chủ hồ sơ.
- **Response 404:** File không tồn tại trên server.

---

### `GET /api/v1/staff/kyc`
- **Auth:** `ROLE_STAFF` hoặc `ROLE_ADMIN`
- **Query Parameters:**

| Param | Type | Ghi chú |
|---|---|---|
| `status` | String | `PENDING` / `APPROVED` / `REJECTED` (optional) |
| `requestCode` | String | Tìm theo mã hồ sơ (có thể nhập `#`), idNumber, fullName, email (optional) |
| `idType` | String | `CCCD` / `CMND` / `PASSPORT` / `DRIVER_LICENSE` (optional) |
| `page` | int | default: 0 |
| `size` | int | default: 10 |

- **Response 200:** `Page<KycResponseDto>` — sắp xếp **giảm dần theo `createdAt`**.

---

### `GET /api/v1/staff/kyc/{id}`
- **Auth:** `ROLE_STAFF` hoặc `ROLE_ADMIN`
- **Response 200:** `KycResponseDto` chi tiết hồ sơ (kèm `version` để dùng cho review).
- **Response 404:** `{ "message": "Không tìm thấy hồ sơ KYC" }`

---

### `POST /api/v1/staff/kyc/{id}/review`
- **Auth:** `ROLE_STAFF` hoặc `ROLE_ADMIN`
- **Request Body:**
  ```json
  { "status": "APPROVED", "rejectionReason": null, "version": 0 }
  ```
- **Response 200:** `KycResponseDto` sau khi cập nhật.
- **Response 409 Conflict:** `{ "success": false, "message": "Dữ liệu đã bị thay đổi bởi một người dùng khác. Vui lòng tải lại trang và thử lại." }` (Optimistic Lock)
- **Response 400:** `{ "success": false, "message": "..." }` (trạng thái không hợp lệ hoặc thiếu lý do từ chối)

---

### `GET /api/v1/staff/kyc/stats`
- **Auth:** `ROLE_STAFF` hoặc `ROLE_ADMIN`
- **Response 200:**
  ```json
  { "total": 10, "pending": 3, "approved": 5, "rejected": 2 }
  ```

---

## 7. ERROR HANDLING (Xử lý lỗi)

| HTTP Code | Tình huống | Message |
|---|---|---|
| 409 | Gửi KYC mới khi đã có hồ sơ PENDING/APPROVED | "Bạn đã có một hồ sơ KYC đang xử lý hoặc đã được phê duyệt." |
| 409 | Hai Staff duyệt cùng lúc (Optimistic Lock) | "Dữ liệu đã bị thay đổi bởi một người dùng khác. Vui lòng tải lại trang và thử lại." |
| 400 | Duyệt hồ sơ không ở trạng thái PENDING | "Chỉ có thể duyệt hồ sơ đang ở trạng thái PENDING" |
| 400 | Từ chối mà không có lý do | "Lý do từ chối là bắt buộc" |
| 403 | Truy cập ảnh KYC không có quyền | (HTTP 403, không body) |
| 404 | Hồ sơ KYC không tồn tại | `{ "message": "Không tìm thấy hồ sơ KYC" }` |
| 500 | Lỗi hệ thống khi nộp / duyệt KYC | `{ "success": false, "message": "Lỗi hệ thống khi nộp KYC." }` |

---

## 8. ACCEPTANCE CRITERIA (Tiêu chí nghiệm thu)

| ID | Scenario | Given | When | Then |
|---|---|---|---|---|
| AC-KYC-01 | Nộp KYC thành công | Customer/Seller chưa có hồ sơ active | Upload đủ 3 ảnh + form hợp lệ | HTTP 201, hồ sơ lưu status `PENDING`, `requestCode` được tạo |
| AC-KYC-02 | Chặn nộp trùng lặp | Customer đã có hồ sơ `PENDING` | Gửi lại `POST /api/v1/kyc` | HTTP 409, hồ sơ cũ không bị ảnh hưởng |
| AC-KYC-03 | Staff duyệt thành công | Hồ sơ ở trạng thái `PENDING` | `POST /review` với status `APPROVED` | HTTP 200, hồ sơ chuyển `APPROVED`, notification được tạo |
| AC-KYC-04 | Staff từ chối kèm lý do | Hồ sơ ở trạng thái `PENDING` | `POST /review` với status `REJECTED` + reason | HTTP 200, `active_user_id = null`, user có thể nộp lại |
| AC-KYC-05 | Optimistic Lock khi duyệt đồng thời | Hai Staff cùng mở hồ sơ `version=0` | Cả hai submit review cùng lúc | Staff thứ hai nhận HTTP 409 "Dữ liệu đã bị thay đổi..." |
| AC-KYC-06 | Xem ảnh với quyền hợp lệ | Staff đang đăng nhập | `GET /{id}/documents/front` | HTTP 200, file ảnh trả về dạng inline |
| AC-KYC-07 | Chặn xem ảnh không có quyền | Customer không phải chủ hồ sơ | `GET /{id}/documents/front` của hồ sơ người khác | HTTP 403 |
| AC-KYC-08 | Tìm kiếm bằng mã `#` | Staff nhập `#KYC-001` vào search | `GET /staff/kyc?requestCode=%23KYC-001` | `#` được strip, tìm đúng hồ sơ |