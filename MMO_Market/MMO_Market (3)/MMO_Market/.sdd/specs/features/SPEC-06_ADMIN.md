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
- **FR-ADMN-02**: THE SYSTEM SHALL allow Admin to configure system variables such as `SESSION_TIMEOUT_MINS` (được hiển thị trên UI là "Thời gian hết hạn đăng nhập (phút)"), `DEFAULT_COMMISSION_PERCENT`, `WITHDRAWAL_FEE_PERCENT`, `SHOP_OPENING_FEE_VND`, `OTP_TIMEOUT_MINS`, `MAX_LOGIN_RETRIES`, `LOCK_DURATION_MINS`, `ESCROW_HOLD_HOURS`, `REQUIRE_WITHDRAW_2FA`, `ALLOW_GOOGLE_LOGIN`, `ALLOW_REGISTER`. Toàn bộ giá trị này phải lưu trực tiếp CSDL SQL Server và được Backend Java tác động thời gian thực.
- **FR-ADMN-03**: WHEN Admin creates a Staff account, THE SYSTEM SHALL assign the `Staff` role.
- **FR-ADMN-04**: THE SYSTEM SHALL allow Admin to block/unblock any User or Shop in case of violations by setting `isLocked = 1` or `0`.
- **FR-ADMN-05**: THE SYSTEM SHALL allow Admin/Staff to create and broadcast system-wide notifications (announcements, maintenance warnings). Khi xem thông báo đã phát, hệ thống hiển thị chi tiết qua Popup Modal.
- **FR-ADMN-06**: WHEN Admin activates maintenance mode through notifications or configs, THE SYSTEM SHALL update `MAINTENANCE_MODE` to `TRUE` and log the audit log.
- **FR-ADMN-07**: THE SYSTEM SHALL provide Admin with a 6-field unified search and filter panel for User Management (`search`, `role`, `status`, `startDate`, `endDate`, `sortOrder`). Cột "Xác thực" trên giao diện danh sách người dùng được loại bỏ để đơn giản hóa UX.
- **FR-ADMN-08**: THE SYSTEM SHALL record Audit Logs with 100% Vietnamese action labels and statuses. Bảng Audit Logs loại bỏ cột Chi tiết không cần thiết để tối ưu không gian hiển thị.

---

## 4. Non-Functional Requirements
- **Performance**: Việc tính toán tổng doanh thu hệ thống (Report) cần được thực hiện nhanh chóng (< 500ms).
- **Security**: Quyền năng Admin là tuyệt đối, bắt buộc được phân quyền chính xác qua bộ lọc bảo mật và lưu nhật ký hoạt động bất biến (Audit logs).
- **Localization**: 100% giao diện, nhãn nút bấm, bộ lọc, loại hành động và thông báo lỗi trong Admin Console phải hiển thị chuẩn tiếng Việt.

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
- `GET /api/admin/users` -> 200 OK (Get filtered user list with search, role, status, date range, sort)
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
- **AC-04**: Admin có thể lọc danh sách người dùng linh hoạt theo 6 tiêu chí (từ khóa, vai trò, trạng thái, khoảng ngày tạo, thứ tự sắp xếp).
- **AC-05**: Toàn bộ cấu hình hệ thống (Thời gian hết hạn đăng nhập, OTP, Số lần thử sai, Escrow, Phí & Hoa hồng) được lưu thẳng CSDL và tác động ngay lập tức vào backend.
- **AC-06**: Khi xem thông báo hệ thống đã phát, chi tiết được hiển thị dạng Popup Modal thân thiện người dùng.

### Out of Scope
- Tích hợp với hệ thống kế toán doanh nghiệp (ERP) bên ngoài.
