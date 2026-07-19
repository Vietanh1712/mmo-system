# SPEC — Upload Service
> **Feature ID:** `feat-upload`
> **UC Coverage:** UC-18 (File Upload Service)
> **Version:** 1.0 | **Status:** Active
> **Author:** Team | **Last Updated:** 2026-07-16

---

## 1. CONTEXT & GOAL (BỐI CẢNH & MỤC TIÊU)

### 1.1 Bối cảnh
Để hoàn tất hồ sơ KYC (ảnh căn cước, chân dung), gửi bằng chứng tranh chấp khiếu nại (ảnh/video lỗi) hoặc tải hình ảnh đại diện khi đăng bán sản phẩm, hệ thống cần cung cấp một dịch vụ upload tệp tin tập trung.

### 1.2 Mục tiêu
- Cho phép người dùng đăng nhập tải tệp tin đa phương tiện lên máy chủ.
- Ràng buộc an toàn: lọc định dạng tệp, chuẩn hóa tên file để tránh lỗ hổng bảo mật.

---

## 2. ACTOR (TÁC NHÂN)
- **User:** Bất kỳ người dùng nào đã đăng nhập hệ thống (Customer, Seller, Staff, Admin).

---

## 3. FUNCTIONAL REQUIREMENTS (Cú pháp EARS)

| ID | EARS Requirement |
|---|---|
| **FR-UPLOAD-01** | WHEN an authenticated User uploads a file, THE SYSTEM SHALL validate that the file is not empty and matches the extension whitelist. |
| **FR-UPLOAD-02** | THE SYSTEM SHALL copy the file to the local `uploads` directory, sanitizing its name using current timestamp and a random number to avoid collision. |
| **FR-UPLOAD-03** | THE SYSTEM SHALL expose the saved files under static route `/uploads/**` and return the relative URL in the API response. |

---

## 4. SECURITY & VALIDATION

- **Định dạng cho phép (Whitelist):** `.png`, `.jpg`, `.jpeg`, `.gif`, `.mp4`, `.webm`, `.avi`, `.mov`.
- **Hạn chế:** Chặn mọi tệp thực thi nguy hiểm (như `.exe`, `.sh`, `.php`, `.jsp`).
- **Xác thực:** Request bắt buộc đính kèm JWT token. Trả về `401 Unauthorized` nếu chưa đăng nhập.

---

## 5. API SPEC (Đặc tả API)

### `POST /api/upload`
*   **Request Headers:** `Authorization: Bearer <Access_Token>`
*   **Request Body (Multipart Form-Data):**
    *   `file`: Tệp tin ảnh/video.
*   **Response (200 OK):**
    ```json
    {
      "url": "/uploads/1783366091787_745.jpg"
    }
    ```