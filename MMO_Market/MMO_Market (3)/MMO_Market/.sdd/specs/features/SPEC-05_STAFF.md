# SPEC-05 — Staff Operations & Dispute Resolution
> **Module:** Staff
> **Version:** 1.0 | **Status:** Active

---

## 1. Context and Goal
Staff là lực lượng hỗ trợ vận hành hệ thống MMO Market. Họ có nhiệm vụ xét duyệt các yêu cầu quan trọng như KYC, đăng ký Shop, rút tiền, và đóng vai trò trọng tài phân xử (Arbitrator) khi có khiếu nại giữa Customer và Seller.

---

## 2. Actors
- **Staff**: Nhân viên hỗ trợ nền tảng.
- **System**: Cung cấp dashboard tổng hợp các task cần xử lý.

---

## 3. Functional Requirements
- **FR-STAF-01**: WHEN Staff views the dashboard, THE SYSTEM SHALL list pending KYC requests, Shop Registrations, and Withdrawals.
- **FR-STAF-02**: WHEN Staff approves a KYC request, THE SYSTEM SHALL update the User's `kyc_status` to `VERIFIED`.
- **FR-STAF-03**: WHEN Staff processes a Dispute (Complaint), THE SYSTEM SHALL allow Staff to view chat history between Customer and Seller, and view the delivered Digital Asset.
- **FR-STAF-04**: WHEN Staff resolves a Dispute in favor of the Customer, THE SYSTEM SHALL refund the escrow balance to the Customer and update Order status to `REFUNDED`.
- **FR-STAF-05**: WHEN Staff resolves a Dispute in favor of the Seller, THE SYSTEM SHALL release the escrow balance to the Seller and update Order status to `COMPLETED`.

---

## 4. Non-Functional Requirements
- **Auditability**: Mọi quyết định của Staff (Duyệt KYC, Quyết định Dispute) phải lưu lại `staff_id` và `note` để Admin có thể tra cứu khi cần.
- **Security**: Endpoint của Staff yêu cầu quyền truy cập `STAFF` hoặc `ADMIN`.

---

## 5. Data Model
```sql
CREATE TABLE Complaints (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    order_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    staff_id BIGINT NULL, -- The staff handling the dispute
    reason NVARCHAR(MAX),
    status VARCHAR(50), -- PENDING, IN_DISCUSSION, RESOLVED_CUSTOMER, RESOLVED_SELLER
    staff_note NVARCHAR(MAX),
    created_at DATETIME DEFAULT GETDATE()
);
```

---

## 6. API Specification
- `GET /api/staff/kyc-requests` -> 200 OK
- `PUT /api/staff/kyc-requests/{id}/review` (Body: status, note) -> 200 OK
- `GET /api/staff/complaints` -> 200 OK
- `POST /api/staff/complaints/{id}/resolve` (Body: winner, note) -> 200 OK

---

## 7. Error Handling
- `400 Bad Request`: "Khiếu nại này đã được xử lý và đóng lại."
- `403 Forbidden`: "Bạn không có quyền truy cập chức năng này."

---

## 8. Acceptance Criteria & Out of Scope
### Acceptance Criteria
- **AC-01**: Given a dispute, when Staff resolves it for Customer, then the money leaves the Seller's escrow and returns to the Customer's available balance.
- **AC-02**: Given a KYC request, when Staff rejects it, the user remains `NOT_VERIFIED` and receives an email notification.

### Out of Scope
- Tự động hóa giải quyết khiếu nại bằng AI chưa được hỗ trợ.
