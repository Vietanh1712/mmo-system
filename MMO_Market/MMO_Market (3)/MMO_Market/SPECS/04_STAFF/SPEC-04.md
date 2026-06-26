# SPEC-04: Staff Operations

## 1. Context and Goal
**Goal:** Đặc tả quy trình vận hành và kiểm soát rủi ro dành cho Staff.
**Context:** Staff là vai trò điều phối hệ thống. Họ có nhiệm vụ kiểm duyệt KYC của người dùng muốn thành Seller, phê duyệt các khoản rút tiền, giải quyết tranh chấp (Complaint) giữa Customer và Seller, và cắm cờ (Flag) những shop có dấu hiệu gian lận.

## 2. Actors
- **Primary:** Staff
- **Secondary:** Seller, Customer (Bị ảnh hưởng bởi quyết định của Staff)

## 3. Functional Requirements (EARS)
- **FR-01 (Approve KYC):** KHI có một yêu cầu KYC mới (Pending), Staff PHẢI xem xét ảnh CMND/CCCD và đưa ra quyết định Duyệt (Approved) hoặc Từ chối (Rejected).
- **FR-02 (Promote Role):** KHI Staff duyệt KYC thành công, hệ thống PHẢI tự động nâng cấp Role của User từ Customer lên Seller.
- **FR-03 (Resolve Complaint - Refund):** TRONG KHI phân xử khiếu nại, NẾU Staff quyết định Khách hàng đúng (Refund), hệ thống PHẢI hoàn tiền từ khoản Escrow trả về ví Customer, và trạng thái đơn hàng thành `REFUNDED`.
- **FR-04 (Resolve Complaint - Reject):** TRONG KHI phân xử khiếu nại, NẾU Staff quyết định Seller đúng (Reject Complaint), hệ thống PHẢI giải phóng khoản Escrow và cộng vào ví Seller.
- **FR-05 (Approve Withdrawal):** KHI duyệt lệnh rút tiền, Staff PHẢI chuyển khoản thực tế, sau đó lên hệ thống Upload biên lai và chuyển trạng thái lệnh sang `COMPLETED`, hệ thống PHẢI trừ vĩnh viễn số tiền khỏi `Hold Balance`.
- **FR-06 (Flag Shop):** NẾU Staff phát hiện Shop có hành vi gian lận, hệ thống PHẢI cho phép Staff cắm cờ (Flag) gian lận. Khi cắm cờ, toàn bộ sản phẩm của Shop đó sẽ bị ẩn khỏi Homepage.

## 4. Non-Functional Requirements
- **Auditability:** Mọi hành động của Staff (Duyệt KYC, Hoàn tiền, Rút tiền) phải được ghi log (Audit Log) để Admin có thể truy vết.
- **Reliability:** Các luồng tiền tệ (Hoàn tiền, Giải phóng Escrow) bắt buộc bọc trong Database Transaction.

## 5. Data Model
- **Table `Flags`:**
  - `flag_id` (PK, BIGINT)
  - `target_seller_id` (FK to Users)
  - `staff_id` (FK to Users)
  - `reason` (NVARCHAR(500))
  - `created_at` (DATETIME)
- **Bảng `Users`, `Withdrawals`, `Complaints`:** Cập nhật trạng thái tương ứng dựa trên hành động của Staff.

## 6. API Specification
- **POST `/api/v1/staff/kyc/{kyc_id}/approve`**
  - **Body:** `{ action: "APPROVE" | "REJECT", reason_if_reject: "..." }`
  - **Response:** 200 OK
- **POST `/api/v1/staff/withdrawals/{withdrawal_id}/complete`**
  - **Body:** `{ receipt_image_url }`
  - **Response:** 200 OK
- **POST `/api/v1/staff/complaints/{complaint_id}/resolve`**
  - **Body:** `{ decision: "REFUND_CUSTOMER" | "PAY_SELLER", note: "..." }`
  - **Response:** 200 OK

## 7. Error Handling
- `400 Bad Request`: Thao tác duyệt khi trạng thái không phải là Pending.
- `403 Forbidden`: Người thao tác không có Role Staff hoặc Admin.

## 8. Acceptance Criteria
- **AC-01:** GIVEN Lệnh khiếu nại đang OPEN, WHEN Staff gọi API `REFUND_CUSTOMER`, THEN khoản tiền giam (Escrow) của đơn hàng đó bị xóa bỏ, ví Customer tăng lên số tiền tương ứng đơn hàng, và ví Seller không đổi.
- **AC-02:** GIVEN User đang là Customer, WHEN Staff gọi API duyệt KYC thành công, THEN Role của User đó chuyển ngay thành Seller và họ có quyền đăng sản phẩm.

## 9. Out of Scope
- Tự động quét và phát hiện hình ảnh CCCD giả mạo bằng AI (Staff dùng mắt thường kiểm tra).
