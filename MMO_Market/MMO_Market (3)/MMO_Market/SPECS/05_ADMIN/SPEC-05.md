# SPEC-05: System Administration

## 1. Context and Goal
**Goal:** Đặc tả phân hệ Quản trị hệ thống (Admin).
**Context:** Admin là người có quyền cao nhất, không tham gia trực tiếp vào việc duyệt đơn hàng hay giải quyết khiếu nại (đó là việc của Staff), mà tập trung vào thiết lập vĩ mô: Cấu hình phí giao dịch, phần trăm hoa hồng, bật/tắt chế độ bảo trì, và quản lý nhân sự (cấp quyền Staff).

## 2. Actors
- **Primary:** Admin
- **Secondary:** Toàn bộ hệ thống (bị ảnh hưởng bởi cấu hình của Admin)

## 3. Functional Requirements (EARS)
- **FR-01 (Manage Configuration):** MẶC ĐỊNH, hệ thống PHẢI cho phép Admin thay đổi cấu hình toàn cục (System Configuration), bao gồm: Phí giao dịch cố định, Phần trăm hoa hồng (Commission Rate) tính trên mỗi đơn hàng.
- **FR-02 (Maintenance Mode):** KHI Admin bật chế độ Maintenance, hệ thống PHẢI chặn tất cả các Request mới từ Customer và Seller (hiển thị trang bảo trì), ngoại trừ các Request từ IP hoặc Role của Admin.
- **FR-03 (Role Assignment):** KHI Admin muốn cấp quyền, hệ thống PHẢI cho phép Admin thay đổi Role của một người dùng bất kỳ thành Staff hoặc giáng cấp Staff xuống Customer.
- **FR-04 (Lock Account):** NẾU Admin phát hiện tài khoản vi phạm nghiêm trọng, hệ thống PHẢI cho phép Admin khóa tài khoản (`is_active = 0`).
- **FR-05 (View Reports):** MẶC ĐỊNH, hệ thống PHẢI cung cấp Dashboard thống kê cho Admin về Tổng doanh thu sàn (từ phí và hoa hồng), Dòng tiền đang giam (Total Escrow), và Số lượng User mới.

## 4. Non-Functional Requirements
- **Security:** API của Admin phải có cơ chế kiểm tra Role nghiêm ngặt (`hasRole('ADMIN')`).
- **Performance:** Báo cáo Dashboard nên được cache hoặc tính toán trước (Materialized Views) nếu lượng dữ liệu lớn.

## 5. Data Model
- **Table `SystemConfig`:**
  - `config_key` (PK, VARCHAR(50))
  - `config_value` (VARCHAR(MAX))
  - `description` (NVARCHAR(200))
  - `updated_by` (FK to Users)
  - `updated_at` (DATETIME)
  *(Ví dụ key: `COMMISSION_RATE`, `FLAT_FEE`, `MAINTENANCE_MODE`)*
- **Bảng `Users`:** Update `role` hoặc `is_active` bởi Admin.

## 6. API Specification
- **PUT `/api/v1/admin/config`**
  - **Body:** `{ configs: [{ key: "COMMISSION_RATE", value: "5" }] }`
  - **Response:** 200 OK
- **POST `/api/v1/admin/users/{user_id}/role`**
  - **Body:** `{ new_role: "STAFF" }`
  - **Response:** 200 OK
- **POST `/api/v1/admin/users/{user_id}/lock`**
  - **Body:** `{ is_locked: true, reason: "Fraud" }`
  - **Response:** 200 OK
- **GET `/api/v1/admin/dashboard/revenue`**
  - **Query:** `?startDate=...&endDate=...`
  - **Response:** 200 OK `{ total_fee, total_commission, chart_data: [] }`

## 7. Error Handling
- `403 Forbidden`: Truy cập bị chặn do không có quyền Admin.
- `404 Not Found`: User cần cấu hình quyền không tồn tại.

## 8. Acceptance Criteria
- **AC-01:** GIVEN Admin cập nhật `COMMISSION_RATE` thành 10%, WHEN Customer mua đơn hàng 100.000 VNĐ, THEN hệ thống tính 10.000 VNĐ vào phí sàn và Seller chỉ nhận được 90.000 VNĐ khi Escrow release.
- **AC-02:** GIVEN Admin bật `MAINTENANCE_MODE`, WHEN Customer truy cập trang Homepage, THEN hệ thống trả về mã lỗi 503 Service Unavailable hoặc giao diện Bảo trì.

## 9. Out of Scope
- Tự động khóa hệ thống theo lịch (Maintenance Scheduling) — Admin phải tự bật/tắt bằng tay.
