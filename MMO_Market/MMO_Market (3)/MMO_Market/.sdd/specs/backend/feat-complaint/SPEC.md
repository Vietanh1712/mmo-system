# SPEC — Complaint Management
> **Feature ID:** `feat-complaint`
> **UC Coverage:** UC-10 (Complaints & Dispute Resolution)
> **Version:** 2.0 | **Status:** Active
> **Author:** Team | **Last Updated:** 2026-06-27

---

## 1. CONTEXT & GOAL (BỐI CẢNH & MỤC TIÊU)

### 1.1 Bối cảnh
Trong giao dịch C2C sản phẩm số, người mua có thể nhận được hàng không đúng mô tả (sai mật khẩu, key hết hạn, tài khoản bị die). Trong thời gian escrow 72h chưa giải phóng, hệ thống cho phép người mua tạo khiếu nại để đóng băng tiền và yêu cầu Staff phán quyết.

### 1.2 Mục tiêu
- Người mua (Customer) tạo khiếu nại trong thời hạn escrow với mô tả và bằng chứng.
- Hệ thống đóng băng escrow và tạo kênh chat giữa Customer, Seller và Staff.
- Staff xem xét, phán quyết (RESOLVED / CLOSED) kèm kết quả giải quyết.
- Seller được thông báo và có thể xem khiếu nại liên quan đến gian hàng của mình.

---

## 2. ACTOR (TÁC NHÂN)

| Actor | Role | Điều kiện tiền quyết |
|---|---|---|
| **Customer** | Tạo khiếu nại, xem lịch sử | Đã mua hàng thành công (transaction tồn tại), escrow chưa giải phóng |
| **Seller** | Xem khiếu nại liên quan đến shop | Là Seller của sản phẩm trong giao dịch bị khiếu nại |
| **Staff / Admin** | Xem tất cả khiếu nại, phán quyết kết quả | Có role `STAFF` hoặc `ADMIN` |

---

## 3. FUNCTIONAL REQUIREMENTS (Cú pháp EARS)

| ID | EARS Requirement |
|---|---|
| **FR-COMP-01** | WHEN a Customer submits a complaint with `transactionId` and `description`, THE SYSTEM SHALL create a `Complaint` record with `status = OPEN` linked to that transaction. |
| **FR-COMP-02** | THE SYSTEM SHALL prevent a Customer from submitting a complaint if they are not the buyer of the transaction (`transaction.customer_id != userId`). |
| **FR-COMP-03** | WHEN a Customer retrieves their complaints, THE SYSTEM SHALL return only complaints where `customer_id = currentUserId`. |
| **FR-COMP-04** | WHEN a Staff/Admin retrieves all complaints (`GET /all`), THE SYSTEM SHALL return all complaints regardless of customer. |
| **FR-COMP-05** | WHEN a Staff/Admin views a complaint detail, THE SYSTEM SHALL return full complaint info (customer, seller, transaction, resolution). |
| **FR-COMP-06** | WHEN a Customer views a complaint detail, THE SYSTEM SHALL only return it if they are the owner (`customer_id = userId`). |
| **FR-COMP-07** | WHEN a Staff updates the complaint status (to `InProgress`, `RESOLVED`, or `REJECTED`), THE SYSTEM SHALL save `resolution` text and update the `status` field. |
| **FR-COMP-08** | THE SYSTEM SHALL allow the Seller to view complaints raised against their shop via `/api/seller/complaints/**` (see feat-seller SPEC). |

---

## 4. BUSINESS RULES (Ràng buộc nghiệp vụ)

| Rule | Mô tả |
|---|---|
| **BR-COMP-01** | Chỉ người mua (Customer) mới được tạo khiếu nại, không phải Seller hay Staff. |
| **BR-COMP-02** | Một giao dịch chỉ có thể có một khiếu nại đang OPEN tại một thời điểm. |
| **BR-COMP-03** | Soft delete: không xóa vật lý bản ghi `Complaints`. |
| **BR-COMP-04** | Khi giải quyết (`RESOLVED`) hoặc từ chối (`REJECTED`), bắt buộc phải có `resolution` (lý do phán quyết). |

---

## 5. DATA MODEL (Mô hình dữ liệu)

```sql
CREATE TABLE Complaints (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    transaction_id  BIGINT NOT NULL,
    customer_id     BIGINT NOT NULL,
    seller_id       BIGINT NOT NULL,
    description     NVARCHAR(MAX) NOT NULL,
    evidence        NVARCHAR(MAX) NULL,     -- URL ảnh/video bằng chứng
    status          VARCHAR(50) DEFAULT 'OPEN', -- OPEN | InProgress | RESOLVED | CLOSED
    resolution      NVARCHAR(MAX) NULL,
    created_at      DATETIME DEFAULT GETDATE(),
    isDelete        BIT DEFAULT 0,
    CONSTRAINT FK_Comp_Trans    FOREIGN KEY (transaction_id) REFERENCES Transactions(id),
    CONSTRAINT FK_Comp_Customer FOREIGN KEY (customer_id) REFERENCES Users(id),
    CONSTRAINT FK_Comp_Seller   FOREIGN KEY (seller_id) REFERENCES Users(id)
);
```

---

## 6. API SPEC (Đặc tả API)

### `POST /api/complaints`
- **Auth:** Bất kỳ user đã đăng nhập (Customer/Seller)
- **Request Body:**
  ```json
  {
    "transactionId": 42,
    "description": "Tài khoản Netflix không đăng nhập được sau khi mua.",
    "evidence": "https://example.com/screenshot.jpg"
  }
  ```
- **Response (200 OK):** Đối tượng Complaint DTO đã tạo.
- **Response (400):** `transactionId` hoặc `description` để trống; không phải người mua của giao dịch.

### `GET /api/complaints`
- **Auth:** Customer / Seller đã đăng nhập
- **Response (200 OK):** `List<ComplaintDTO>` — chỉ trả về khiếu nại của user hiện tại.

### `GET /api/complaints/{id}`
- **Auth:** Bất kỳ user đã đăng nhập
- **Logic:** Staff/Admin xem mọi khiếu nại; Customer chỉ xem được của mình.
- **Response (200 OK):** `ComplaintDTO` đầy đủ (customer, seller, transaction info, resolution).
- **Response (404):** Không tìm thấy hoặc không có quyền.

### `GET /api/complaints/all`
- **Auth:** `STAFF` hoặc `ADMIN` only
- **Response (200 OK):** `List<ComplaintDTO>` — toàn bộ khiếu nại hệ thống.
- **Response (403):** Không phải Staff/Admin.

### `PUT /api/complaints/{id}/status`
- **Auth:** `STAFF` hoặc `ADMIN` only
- **Request Body:**
  ```json
  {
    "status": "RESOLVED",
    "resolution": "Đã hoàn tiền cho người mua do Seller cung cấp sai thông tin."
  }
  ```
- **Response (200 OK):** `ComplaintDTO` đã cập nhật.
- **Response (400):** `status` để trống.
- **Response (403):** Không phải Staff/Admin.

---

## 7. DTO RESPONSE (ComplaintDTO)

```json
{
  "id": 5,
  "description": "Tài khoản không đăng nhập được",
  "evidence": "https://...",
  "status": "OPEN",
  "resolution": null,
  "createdAt": "2026-06-27T08:00:00",
  "transaction": {
    "id": 42,
    "amountVnd": 95000,
    "productName": "Netflix Premium 1 tháng"
  },
  "customer": { "id": 10, "email": "buyer@example.com", "fullName": "Nguyen A" },
  "seller": { "id": 5, "email": "seller@example.com", "fullName": "Tran B" }
}
```

---

## 8. ERROR HANDLING (Xử lý lỗi)

| HTTP | Tình huống |
|---|---|
| `401 Unauthorized` | Chưa đăng nhập |
| `403 Forbidden` | Customer gọi `/all` hoặc `PUT /status` |
| `400 Bad Request` | Thiếu `transactionId`/`description`; không phải buyer của giao dịch |
| `404 Not Found` | Complaint không tồn tại hoặc không thuộc về user |