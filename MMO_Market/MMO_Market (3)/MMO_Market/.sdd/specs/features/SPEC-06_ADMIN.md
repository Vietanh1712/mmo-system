# SPEC-06 — Admin Dashboard & Configuration
> **Module:** Admin
> **Version:** 1.0 | **Status:** Active

---

## 1. Context and Goal
Admin là vai trò quản trị cao nhất của hệ thống MMO Market. Mục tiêu của Admin là theo dõi toàn cảnh sức khỏe tài chính của nền tảng, cấu hình phí giao dịch, và quản lý phân quyền cho đội ngũ Staff.

---

## 2. Actors
- **Admin**: Quản trị viên hệ thống.
- **System**: Cung cấp báo cáo tài chính và áp dụng cấu hình phí mới.

---

## 3. Functional Requirements
- **FR-ADMN-01**: WHEN Admin views the dashboard, THE SYSTEM SHALL display aggregate metrics: Total Platform Revenue, Total Escrow Balance, User Growth, and Sales volume.
- **FR-ADMN-02**: THE SYSTEM SHALL allow Admin to configure system variables such as `WITHDRAWAL_FEE_PERCENT`, `ESCROW_HOLD_DAYS`, and `MIN_WITHDRAWAL_VND`.
- **FR-ADMN-03**: WHEN Admin creates a Staff account, THE SYSTEM SHALL assign the `STAFF` role and send an initial password to their email.
- **FR-ADMN-04**: THE SYSTEM SHALL allow Admin to block/unblock any User or Shop in case of severe violations.

---

## 4. Non-Functional Requirements
- **Performance**: Việc tính toán tổng doanh thu hệ thống (Report) có thể nặng, cần được cache hoặc chạy batch job ban đêm, không truy vấn trực tiếp trên bảng Transactions liên tục.
- **Security**: Quyền năng Admin là tuyệt đối, bắt buộc kích hoạt 2FA nếu đăng nhập.

---

## 5. Data Model
```sql
CREATE TABLE SystemConfig (
    config_key VARCHAR(100) PRIMARY KEY,
    config_value VARCHAR(255) NOT NULL,
    updated_by BIGINT,
    updated_at DATETIME DEFAULT GETDATE()
);
```

---

## 6. API Specification
- `GET /api/admin/dashboard/metrics` -> 200 OK
- `GET /api/admin/configs` -> 200 OK
- `PUT /api/admin/configs` (Body: List of key-value pairs) -> 200 OK
- `POST /api/admin/staff` (Body: email, fullName) -> 200 OK

---

## 7. Error Handling
- `400 Bad Request`: "Tỷ lệ phí rút tiền không được vượt quá 50%."
- `403 Forbidden`: Chỉ tài khoản Admin mới có quyền truy cập.

---

## 8. Acceptance Criteria & Out of Scope
### Acceptance Criteria
- **AC-01**: Given Admin updates the `ESCROW_HOLD_DAYS` to 5, when a new order is created, its escrow release date is calculated as `now() + 5 days`.
- **AC-02**: Given an Admin views metrics, then the total platform revenue accurately reflects the sum of all commissions taken from sales.

### Out of Scope
- Tích hợp với hệ thống kế toán doanh nghiệp (ERP) bên ngoài.
