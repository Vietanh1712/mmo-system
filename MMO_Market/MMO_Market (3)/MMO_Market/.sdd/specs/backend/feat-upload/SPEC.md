# SPEC — Upload Service
> **Feature ID:** `feat-upload`
> **Version:** 1.0 | **Status:** Active
> **Author:** Team | **Last Updated:** 2026-06-27

---

## 1. CONTEXT & GOAL (BỐI CẢNH & MỤC TIÊU)
Dịch vụ tải lên hình ảnh sản phẩm, ảnh KYC danh tính lên máy chủ hệ thống.

---

## 6. API SPEC (Đặc tả API)

### `POST /api/upload`
*   **Request (Multipart File):** file
*   **Response (200 OK):**
    ```json
    {
      "url": "/uploads/1781743882173_image.jpg"
    }
    ```