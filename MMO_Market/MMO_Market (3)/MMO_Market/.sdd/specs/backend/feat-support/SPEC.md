# SPEC — Support Ticket Management (Phiếu Hỗ Trợ)
> **Feature ID:** `feat-support`
> **UC Coverage:** UC-27
> **Version:** 1.2 | **Status:** Active
> **Author:** Team | **Last Updated:** 2026-07-18

---

## 1. CONTEXT & GOAL (BỐI CẢNH & MỤC TIÊU)

### 1.1 Bối cảnh
Hệ thống sàn giao dịch C2C cần cơ chế xử lý các yêu cầu trợ giúp kỹ thuật, nạp/rút tiền, vấn đề tài khoản, và thắc mắc giao dịch từ người dùng (Customer/Seller) đến đội ngũ nhân viên vận hành (Staff/Admin).

### 1.2 Mục tiêu
- Người dùng tạo phiếu hỗ trợ (Support Ticket) theo phân loại danh mục và theo dõi trạng thái phản hồi.
- Nhân viên vận hành (Staff/Admin) quản lý toàn bộ phiếu hỗ trợ, xem chi tiết, đưa ra hướng giải quyết (`resolution`), và chuyển đổi trạng thái phiếu (`OPEN`, `RESOLVED`, `CLOSED`).

---

## 2. ACTORS (TÁC NHÂN)
| Actor | Role | Điều kiện tiền quyết |
|---|---|---|
| **Customer / Seller** | Người gửi phiếu hỗ trợ | Tài khoản đăng nhập hợp lệ với vai trò `ROLE_CUSTOMER` hoặc `ROLE_SELLER` |
| **Staff / Admin** | Nhân viên xử lý phiếu hỗ trợ | Tài khoản đăng nhập hợp lệ với vai trò `ROLE_STAFF` hoặc `ROLE_ADMIN` |

---

## 3. FUNCTIONAL REQUIREMENTS (Cú pháp EARS)
| ID | EARS Requirement |
|---|---|
| **FR-SUP-01** | WHEN a Customer or Seller submits a support request, THE SYSTEM SHALL save the ticket with category, subject, message, and initial status 'OPEN'. |
| **FR-SUP-02** | WHEN a Customer or Seller requests their support history, THE SYSTEM SHALL return all non-deleted tickets owned by that user. |
| **FR-SUP-03** | WHEN a Staff or Admin requests system support tickets, THE SYSTEM SHALL return all non-deleted tickets sorted by creation date descending. |
| **FR-SUP-04** | WHEN a Staff or Admin updates a support ticket status, THE SYSTEM SHALL update the status (`OPEN`, `RESOLVED`, `CLOSED`), store the resolution message, and log the action. |
| **FR-SUP-05** | WHEN a user requests details of a ticket, THE SYSTEM SHALL enforce ownership validation, allowing access only to the ticket owner or authorized Staff/Admin. |

---

## 4. NON-FUNCTIONAL REQUIREMENTS
| ID | Category | Requirement |
|---|---|---|
| **NFR-SUP-01** | Security | Endpoint xem chi tiết phiếu hỗ trợ bắt buộc kiểm tra quyền sở hữu (Ownership Validation) để chống lỗ hổng IDOR. |
| **NFR-SUP-02** | Localization | Toàn bộ thuật ngữ hiển thị trên giao diện được chuẩn hóa thành "Phiếu Hỗ Trợ" thay cho "Ticket". |

---

## 5. DATA MODEL (Mô hình dữ liệu)

```sql
CREATE TABLE SupportTickets (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    category VARCHAR(50) NOT NULL, -- TECHNICAL, TRANSACTION, ACCOUNT, OTHER
    title NVARCHAR(255) NOT NULL,
    description NVARCHAR(MAX) NOT NULL,
    status VARCHAR(50) DEFAULT 'Open', -- Open, Processing, Resolved
    resolution NVARCHAR(MAX) NULL,
    isDelete BIT DEFAULT 0,
    created_at DATETIME DEFAULT GETDATE(),
    updated_at DATETIME DEFAULT GETDATE(),
    is_delete BIT DEFAULT 0,
    CONSTRAINT FK_ST_User FOREIGN KEY (user_id) REFERENCES Users(id)
);
```

---

## 6. API SPEC (Đặc tả API)

### `POST /api/support-tickets`
*   **Access:** `ROLE_CUSTOMER`, `ROLE_SELLER`
*   **Description**: Người dùng gửi yêu cầu hỗ trợ mới.
*   **Request Body (JSON):**
    ```json
    {
      "category": "TECHNICAL",
      "title": "Không nhận được OTP email",
      "description": "Tôi đăng ký tài khoản từ 10 phút trước nhưng chưa nhận được mã."
    }
    ```
*   **Response (200 OK):** Thông tin phiếu hỗ trợ vừa tạo.

### `GET /api/support-tickets`
*   **Access:** `ROLE_CUSTOMER`, `ROLE_SELLER`
*   **Description**: Người dùng xem danh sách phiếu hỗ trợ cá nhân của mình.
*   **Response (200 OK):** Danh sách phiếu hỗ trợ.

### `GET /api/support-tickets/all`
*   **Access:** `ROLE_STAFF`, `ROLE_ADMIN`
*   **Description**: Staff/Admin xem danh sách phiếu hỗ trợ toàn hệ thống.
*   **Response (200 OK):** Danh sách tất cả phiếu hỗ trợ.

### `GET /api/support-tickets/{id}`
*   **Access:** Chủ sở hữu phiếu hỗ trợ hoặc `ROLE_STAFF`, `ROLE_ADMIN`
*   **Description**: Xem chi tiết một phiếu hỗ trợ cụ thể.
*   **Response (200 OK):** Chi tiết phiếu hỗ trợ.

### `PUT /api/support-tickets/{id}/status`
*   **Access:** `ROLE_STAFF`, `ROLE_ADMIN`
*   **Description**: Staff/Admin cập nhật trạng thái/phản hồi giải pháp cho phiếu hỗ trợ.
*   **Request Body (JSON):**
    ```json
    {
      "status": "Resolved",
      "resolution": "Đã cấu hình lại dịch vụ SMTP, email đã được gửi thành công."
    }
    ```
*   **Response (200 OK):** Thông tin phiếu hỗ trợ sau khi cập nhật.

---

## 7. BẢN ĐỊA HÓA (LOCALIZATION)
*   Việt hóa toàn bộ thuật ngữ hiển thị trên giao diện liên quan đến "Ticket" / "Ticket hỗ trợ" thành "Phiếu hỗ trợ" (ví dụ: "Ticket Hỗ Trợ" -> "Phiếu Hỗ Trợ", "Ticket của tôi" -> "Phiếu hỗ trợ của tôi", "Mã Ticket" -> "Mã Phiếu", v.v.) để tăng tính thân thiện và dễ hiểu đối với người dùng Việt Nam.

---

## 8. ERROR HANDLING (Xử lý lỗi)
| HTTP Code | Error Code | Message | Lý do kích hoạt |
|---|---|---|---|
| 401 | UNAUTHORIZED | "Chưa đăng nhập" | Người dùng chưa xác thực |
| 403 | FORBIDDEN | "Truy cập bị từ chối" | Xem hoặc chỉnh sửa phiếu hỗ trợ của người khác khi không phải Staff |
| 404 | NOT_FOUND | "Không tìm thấy phiếu hỗ trợ" | ID phiếu không tồn tại hoặc đã bị xóa soft delete |

---

## 9. ACCEPTANCE CRITERIA (Tiêu chí nghiệm thu)
* **AC-SUP-01**: Khách hàng tạo phiếu hỗ trợ mới thành công, hệ thống lưu trạng thái `Open` và hiển thị trong danh sách "Phiếu hỗ trợ của tôi".
* **AC-SUP-02**: Nhân viên Staff xem được tất cả phiếu hỗ trợ, cập nhật phản hồi giải pháp và đổi trạng thái sang `Resolved` hoặc `Closed`.
* **AC-SUP-03**: Người dùng A không thể truy cập URL xem chi tiết phiếu hỗ trợ của người dùng B (trả về HTTP 403).
