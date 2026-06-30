# SPEC — KYC Verification
> **Feature ID:** `feat-kyc`
> **UC Coverage:** UC-03 (KYC Verification)
> **Version:** 1.0 | **Status:** Active
> **Author:** Team | **Last Updated:** 2026-06-27

---

## 1. CONTEXT & GOAL (BỐI CẢNH & MỤC TIÊU)

### 1.1 Bối cảnh
Để đảm bảo an toàn giao dịch trên sàn giao dịch C2C và tuân thủ các quy định phòng chống lừa đảo, người dùng muốn bán hàng hoặc rút tiền số lượng lớn cần thực hiện định danh danh tính (Know Your Customer - KYC).

### 1.2 Mục tiêu
- Người dùng tải lên thông tin CCCD/CMND bao gồm mặt trước, mặt sau và ảnh chụp chân dung chân thật.
- Cho phép nhân viên vận hành (Staff) duyệt hoặc từ chối yêu cầu KYC kèm theo lý do cụ thể.

---

## 2. ACTOR (TÁC NHÂN)
| Actor | Role | Điều kiện tiền quyết |
|---|---|---|
| **User** | Người gửi hồ sơ | Đã đăng nhập, chưa hoàn tất định danh KYC |
| **Staff** | Người kiểm duyệt | Đã đăng nhập với tài khoản có vai trò Staff |

---

## 3. FUNCTIONAL REQUIREMENTS (Cú pháp EARS)
| ID | EARS Requirement |
|---|---|
| FR-KYC-01 | WHEN a User submits KYC files and details, THE SYSTEM SHALL save the request with status 'Pending'. |
| FR-KYC-02 | WHILE a User has a 'Pending' KYC request, THE SYSTEM SHALL prevent them from submitting another KYC. |
| FR-KYC-03 | WHEN a Staff approves the KYC request, THE SYSTEM SHALL set user verification status `isVerified = 1`. |
| FR-KYC-04 | WHEN a Staff rejects the KYC request, THE SYSTEM SHALL update status to 'Rejected' and require a rejection reason. |

---

## 4. NON-FUNCTIONAL REQUIREMENTS
| ID | Category | Requirement |
|---|---|---|
| NFR-KYC-01 | Security | Ảnh tài liệu KYC nhạy cảm phải được bảo vệ tránh rò rỉ. |
| NFR-KYC-02 | Compliance | Mã số định danh `citizen_id` phải được chuẩn hóa độ dài từ 9 đến 12 ký tự số. |

---

## 5. DATA MODEL (Mô hình dữ liệu)

```sql
CREATE TABLE KYCRequests (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    full_name NVARCHAR(255) NOT NULL,
    citizen_id VARCHAR(20) NOT NULL,
    date_of_birth DATE NULL,
    front_id_image VARCHAR(255) NOT NULL,
    back_id_image VARCHAR(255) NOT NULL,
    selfie_image VARCHAR(255) NOT NULL,
    status VARCHAR(20) DEFAULT 'Pending', -- Pending, Approved, Rejected
    rejection_reason NVARCHAR(MAX) NULL,
    reviewed_by BIGINT NULL,
    reviewed_at DATETIME NULL,
    created_at DATETIME DEFAULT GETDATE(),
    updated_at DATETIME DEFAULT GETDATE(),
    isDelete BIT DEFAULT 0,
    CONSTRAINT FK_KYC_User FOREIGN KEY(user_id) REFERENCES Users(id),
    CONSTRAINT FK_KYC_Staff FOREIGN KEY(reviewed_by) REFERENCES Users(id)
);
```

---

## 6. API SPEC (Đặc tả API)

### `POST /api/v1/kyc`
*   **Description**: Tải lên hồ sơ định danh KYC mới.
*   **Request Body (Multipart Form-Data):**
    *   `fullName`: String
    *   `citizenId`: String
    *   `frontImage`: Multipart File
    *   `backImage`: Multipart File
    *   `selfieImage`: Multipart File
*   **Response (200 OK):**
    ```json
    {
      "id": 1,
      "fullName": "Nguyen Van A",
      "citizenId": "001095012345",
      "status": "Pending",
      "createdDate": "2026-06-27T08:00:00"
    }
    ```

### `GET /api/v1/kyc/me`
*   **Response (200 OK):** Trả về trạng thái hồ sơ KYC hiện tại của User đăng nhập.

### `GET /api/v1/staff/kyc`
*   **Description**: Lấy danh sách hồ sơ KYC toàn hệ thống (phục vụ Staff).
*   **Request Query Parameters:**
    *   `status`: String (PENDING, APPROVED, REJECTED - optional)
    *   `requestCode`: String (optional)
    *   `idType`: String (CCCD, CMND, PASSPORT, DRIVER_LICENSE - optional)
    *   `page`: int (default: 0)
    *   `size`: int (default: 10)
*   **Response (200 OK):** Page object containing list of KYC requests sorted by ID ascending.

---

## 7. ERROR HANDLING (Xử lý lỗi)
| HTTP Code | Error Code | Message | Lý do kích hoạt |
|---|---|---|---|
| 400 | BAD_REQUEST | "Hồ sơ KYC đã tồn tại hoặc đang chờ duyệt" | Gửi KYC mới khi yêu cầu cũ chưa phân định |

---

## 8. ACCEPTANCE CRITERIA (Tiêu chí nghiệm thu)
| ID | Scenario | Given (Bối cảnh) | When (Hành động) | Then (Kết quả) |
|---|---|---|---|---|
| AC-KYC-01 | Gửi KYC thành công | User chưa được định danh | Upload đầy đủ 3 ảnh chụp và CCCD hợp lệ | Hệ thống lưu yêu cầu dạng "Pending" và phản hồi thành công |