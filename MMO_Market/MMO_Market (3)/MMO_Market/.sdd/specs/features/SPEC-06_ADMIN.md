# SPEC-06 — Admin Dashboard & Configuration
> **Module:** Admin
> **Version:** 2.0 | **Status:** Active
> **Last Updated:** 2026-07-16

---

## 1. Context and Goal
Admin là vai trò quản trị cao nhất của hệ thống MMO Market. Mục tiêu của Admin là theo dõi toàn cảnh sức khỏe tài chính của nền tảng (doanh thu từ commissions, shop opening fee, withdrawal fee), cấu hình phí giao dịch, quản lý phân quyền cho đội ngũ Staff, và phát hành thông báo hệ thống / quản lý trạng thái bảo trì.

---

## 2. Actors
- **Admin**: Quản trị viên hệ thống.
- **System**: Cung cấp báo cáo tài chính, áp dụng cấu hình phí mới và lưu trữ lịch sử kiểm toán (Audit logs).

---

## 3. Functional Requirements
- **FR-ADMN-01**: WHEN Admin views the dashboard, THE SYSTEM SHALL display aggregate metrics: commissions, shopOpeningFees, withdrawalFees, and netTotal revenue.
- **FR-ADMN-02**: THE SYSTEM SHALL allow Admin to configure system variables such as `SESSION_TIMEOUT_MINS`, `DEFAULT_COMMISSION_PERCENT`, `WITHDRAWAL_FEE_PERCENT`, `SHOP_OPENING_FEE_VND`, and limits.
- **FR-ADMN-03**: WHEN Admin creates a Staff account, THE SYSTEM SHALL assign the `Staff` role.
- **FR-ADMN-04**: THE SYSTEM SHALL allow Admin to block/unblock any User or Shop in case of violations by setting `isLocked = 1` or `0`.
- **FR-ADMN-05**: THE SYSTEM SHALL allow Admin/Staff to create and broadcast system-wide notifications (announcements, maintenance warnings).
- **FR-ADMN-06**: WHEN Admin activates maintenance mode through notifications or configs, THE SYSTEM SHALL update `MAINTENANCE_MODE` to `TRUE` and log the audit log.

---

## 4. Non-Functional Requirements
- **Performance**: Việc tính toán tổng doanh thu hệ thống (Report) cần được thực hiện nhanh chóng (< 500ms).
- **Security**: Quyền năng Admin là tuyệt đối, bắt buộc được phân quyền chính xác qua bộ lọc bảo mật và lưu nhật ký hoạt động bất biến (Audit logs).

---

## 5. Data Model

```sql
CREATE TABLE SystemConfigurations (
    id INT IDENTITY(1,1) PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL UNIQUE,
    config_value NVARCHAR(MAX) NOT NULL,
    description NVARCHAR(500) NULL,
    updated_by BIGINT NULL,
    updated_at DATETIME DEFAULT GETDATE()
);

CREATE TABLE AuditLogs (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    action VARCHAR(255) NOT NULL,
    details NVARCHAR(MAX) NULL,
    target_user_id BIGINT NULL,
    target_id BIGINT NULL,
    target_type VARCHAR(100) NULL,
    created_at DATETIME DEFAULT GETDATE()
);
```

---

## 6. API Specification
- `GET /api/admin/revenue/summary` -> 200 OK (Revenue Summary)
- `GET /api/admin/revenue/transactions` -> 200 OK (Cashflow Transactions)
- `GET /api/admin/revenue/export` -> CSV file (Export Cashflow)
- `GET /api/admin/system-config` -> 200 OK (Get current configurations)
- `PUT /api/admin/system-config/general` -> 200 OK (Update general settings)
- `PUT /api/admin/system-config/commissions` -> 200 OK (Update commissions and limits)
- `POST /api/admin/user-management/users/{userId}/toggle-lock` -> 200 OK (Toggle Lock User)
- `POST /api/admin/notifications` -> 200 OK (Create/Broadcast notification)
- `POST /api/admin/notifications/toggle-maintenance` -> 200 OK (Toggle maintenance status)
- `DELETE /api/admin/notifications/{id}` -> 200 OK (Delete system notification)

---

## 7. Error Handling
- `400 Bad Request`: "Hoa hồng C2C phải nằm trong khoảng từ 0% đến 100%."
- `403 Forbidden`: Chỉ tài khoản có vai trò hợp lệ mới được thực hiện.

---

## 8. Acceptance Criteria & Out of Scope
### Acceptance Criteria
- **AC-01**: Given Admin updates default commission to 5%, when a sale is made, the platform takes 5% commission.
- **AC-02**: Given Admin views revenue summary, the system displays the correct sum of commissions, shop opening fees, and withdrawal fees.
- **AC-03**: Given Admin locks User A, User A's requests are blocked by the JWT filter dynamically.

### Out of Scope
- Tích hợp với hệ thống kế toán doanh nghiệp (ERP) bên ngoài.
