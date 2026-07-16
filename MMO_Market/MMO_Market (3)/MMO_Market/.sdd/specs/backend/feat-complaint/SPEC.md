# SPEC — Complaint & Dispute Resolution
> **Feature ID:** `feat-complaint`
> **UC Coverage:** UC-10 (Complaints & Dispute Resolution)
> **Version:** 2.0 | **Status:** Active
> **Author:** Team | **Last Updated:** 2026-07-16

---

## 1. CONTEXT & GOAL (BỐI CẢNH & MỤC TIÊU)

### 1.1 Bối cảnh
Trong giao dịch C2C sản phẩm số, người mua có thể nhận được hàng không đúng mô tả (sai mật khẩu, key hết hạn, tài khoản bị khóa/die). Trong thời gian Escrow bảo lãnh đơn hàng (3 ngày đối với Shop tiêu chuẩn hoặc 7 ngày đối với Shop mới/cảnh cáo) chưa giải phóng tiền cho Seller, hệ thống cho phép người mua gửi khiếu nại để tạm thời khóa số dư giao dịch, mở phòng chat đối chất để Staff phân xử.

### 1.2 Mục tiêu
- Người mua (Customer) tạo khiếu nại giao dịch trước khi Escrow giải phóng tiền, yêu cầu bắt buộc cung cấp chi tiết lỗi và bằng chứng hình ảnh/video.
- Hệ thống tự động chuyển trạng thái giao dịch sang `Disputed` để đóng băng tiền.
- Mở phòng chat đối chất (WebSocket) giữa Customer, Seller và Staff.
- Staff xem xét, phán quyết thay đổi trạng thái khiếu nại (`Resolved`, `Completed`, `Rejected`), tự động xử lý tiền hoàn/giải ngân và gắn cờ cảnh cáo nếu Shop sai phạm.

---

## 2. ACTOR (TÁC NHÂN)

| Actor | Role | Điều kiện tiền quyết |
|---|---|---|
| **Customer** | Tạo khiếu nại, gửi chat đối chất | Đã mua hàng thành công (chủ sở hữu transaction), trạng thái giao dịch chưa bị hủy/hoàn tiền và chưa quá hạn Escrow. |
| **Seller** | Xem khiếu nại chống lại Shop, gửi chat đối chất | Là người bán của sản phẩm trong giao dịch bị khiếu nại. |
| **Staff / Admin** | Mở đối chất, phán quyết khiếu nại | Tài khoản có vai trò `Staff` hoặc `Admin`. |

---

## 3. FUNCTIONAL REQUIREMENTS (Cú pháp EARS)

| ID | EARS Requirement |
|---|---|
| **FR-COMP-01** | WHEN a Customer submits a complaint with `transactionId`, `description`, and `evidence`, THE SYSTEM SHALL create a `Complaint` record with `status = PENDING_REVIEW`. |
| **FR-COMP-02** | THE SYSTEM SHALL validate that the Customer is the buyer of the transaction, and that the transaction status is not already `Disputed`, `Cancelled`, or `Refunded`. |
| **FR-COMP-03** | THE SYSTEM SHALL set the transaction status to `Disputed` to lock transaction escrow balance immediately upon complaint creation. |
| **FR-COMP-04** | THE SYSTEM SHALL require `evidence` (image/video URL) to be non-empty when creating a complaint, throwing an error otherwise. |
| **FR-COMP-05** | WHEN a Staff accepts a complaint for dispute resolution, THE SYSTEM SHALL update complaint status to `In_Progress` and send a system message to activate the chat room. |
| **FR-COMP-06** | WHEN a Staff updates the complaint status to `Resolved`, `Completed` or `Rejected`, THE SYSTEM SHALL save `resolution` text, update the status, and record the resolver ID. |
| **FR-COMP-07** | THE SYSTEM SHALL allow Staff to assign a `flagLevel` (None, Alert, Warning) and `flagReason` to the seller's shop during resolution. |

---

## 4. BUSINESS RULES (Ràng buộc nghiệp vụ)

| Rule | Mô tả |
|---|---|
| **BR-COMP-01** | Chỉ có tài khoản mua hàng (Customer) mới được tạo khiếu nại đơn hàng. |
| **BR-COMP-02** | Bằng chứng (evidence) là **bắt buộc** khi tạo đơn khiếu nại để hạn chế các khiếu nại rác. |
| **BR-COMP-03** | Soft delete: Không xóa vật lý bản ghi `Complaints`, luôn dùng cờ `isDelete = 0`. |
| **BR-COMP-04** | Trạng thái vòng đời khiếu nại: `PENDING_REVIEW` (Tạo mới) $\rightarrow$ `In_Progress` (Đang đối chất) $\rightarrow$ `Resolved` (Chấp nhận khiếu nại, hoàn tiền) hoặc `Rejected` (Từ chối khiếu nại, giải ngân Seller) / `Completed` (Đã hoàn tất). |

---

## 5. DATA MODEL (Mô hình dữ liệu)

Cấu trúc bảng `Complaints` trong CSDL SQL Server:

```sql
CREATE TABLE Complaints (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    transaction_id      BIGINT NOT NULL,
    customer_id         BIGINT NOT NULL,
    seller_id           BIGINT NOT NULL,
    description         NVARCHAR(MAX) NOT NULL,
    evidence            NVARCHAR(MAX) NOT NULL,    -- Đường dẫn ảnh/video bằng chứng (Bắt buộc)
    status              VARCHAR(20) DEFAULT 'Open', -- Open | PENDING_REVIEW | In_Progress | Resolved | Rejected | Completed
    preferred_solution  VARCHAR(50) NULL,           -- Giải pháp mong muốn: REPLACEMENT | REFUND
    resolution          NVARCHAR(MAX) NULL,         -- Kết luận phán quyết của Staff
    resolved_by         BIGINT NULL,                -- Nhân viên xử lý (FK Users)
    resolved_at         DATETIME NULL,
    decision_type       VARCHAR(50) NULL,
    created_at          DATETIME DEFAULT GETDATE(),
    isDelete            BIT DEFAULT 0,
    CONSTRAINT FK_Comp_Trans    FOREIGN KEY (transaction_id) REFERENCES Transactions(id),
    CONSTRAINT FK_Comp_Customer FOREIGN KEY (customer_id) REFERENCES Users(id),
    CONSTRAINT FK_Comp_Seller   FOREIGN KEY (seller_id) REFERENCES Users(id),
    CONSTRAINT FK_Comp_Staff    FOREIGN KEY (resolved_by) REFERENCES Users(id)
);
```

---

## 6. API SPEC (Đặc tả API)

### 6.1. Tạo mới khiếu nại
*   **Endpoint:** `POST /api/complaints`
*   **Request Body (JSON):**
    ```json
    {
      "transactionId": 42,
      "description": "Tài khoản không đăng nhập được, báo sai mật khẩu.",
      "evidence": "https://mmo-market.s3.amazonaws.com/evidence-42.jpg",
      "preferredSolution": "REFUND"
    }
    ```
*   **Response (200 OK):** Trả về Complaint DTO chi tiết.

### 6.2. Staff mở cuộc đối chất (Dispute)
*   **Endpoint:** `POST /api/complaints/{id}/start-dispute`
*   **Response (200 OK):** Chuyển trạng thái khiếu nại thành `In_Progress` và tự động tạo tin nhắn hệ thống kích hoạt phòng chat.

### 6.3. Staff cập nhật trạng thái/phán quyết khiếu nại
*   **Endpoint:** `PUT /api/complaints/{id}/status`
*   **Request Body (JSON):**
    ```json
    {
      "status": "Resolved",
      "resolution": "Seller cung cấp sai thông tin tài khoản và không hỗ trợ.",
      "flagLevel": "Warning",
      "flagReason": "Bán hàng sai mô tả và không hợp tác giải quyết khiếu nại"
    }
    ```
*   **Response (200 OK):** Trả về Complaint DTO đã cập nhật.

### 6.4. Xem danh sách khiếu nại của Staff (hỗ trợ phân trang và tìm kiếm)
*   **Endpoint:** `GET /api/complaints/all`
*   **Request Params:** `keyword` (Tùy chọn), `status` (Tùy chọn), `page` (Mặc định 0), `size` (Mặc định 10).
*   **Response (200 OK):** `Page<ComplaintDTO>`.