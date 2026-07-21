# UC-18 — Tải Lên Tệp Tin Minh Chứng (File Upload Service)

> **Feature:** `feat-upload` | **Phiên bản:** 1.0 | **Trạng thái:** Published
> **Cập nhật:** 2026-07-16

---

## 1. Tổng Quan

| Thuộc tính | Nội dung |
|:---|:---|
| **Mã Use Case** | UC-18 |
| **Tên** | Tải Lên Tệp Tin Minh Chứng (File Upload Service) |
| **Tác nhân chính** | Khách hàng (Customer), Người bán (Seller), Nhân viên (Staff) |
| **Mô tả ngắn** | Người dùng thực hiện tải lên tệp tin (ảnh hoặc video) để phục vụ gửi ảnh căn cước KYC, bằng chứng lỗi khiếu nại tranh chấp, hoặc ảnh đại diện sản phẩm đăng bán. Hệ thống kiểm tra tính hợp lệ và lưu trữ tệp tin trên máy chủ. |
| **Độ ưu tiên** | Cao (P1) — tính năng nền tảng phục vụ các luồng nghiệp vụ cốt lõi |

---

## 2. Tác Nhân & Điều Kiện

### 2.1 Tác Nhân
- **Khách hàng / Người bán / Nhân viên:** Người dùng đã đăng nhập vào hệ thống.

### 2.2 Điều Kiện Tiền Quyết (Preconditions)
- Người dùng đã đăng nhập thành công và đính kèm JWT Token trong tiêu đề request.

### 2.3 Hậu Điều Kiện (Postconditions)
- Tệp tin được sao chép và lưu trữ an toàn trong thư mục `uploads/` trên máy chủ.
- Trả về đường dẫn URL của tệp để Client sử dụng hiển thị hoặc lưu vào các thực thể dữ liệu khác (như KYCRequests, Complaints).

---

## 3. Luồng Xử Lý

### 3.1 Luồng Chính — Tải Lên Tệp Tin Thành Công (Happy Path)

```
Bước 1  [User]:       Thực hiện hành động tải tệp tin (chọn file từ thiết bị hoặc kéo thả file vào dropzone)
Bước 2  [Frontend]:   Tạo đối tượng FormData, gán tệp tin vào tham số "file"
Bước 3  [Frontend]:   Gửi yêu cầu POST /api/upload kèm theo Multipart File và JWT token
Bước 4  [Backend]:    Xác thực Token người dùng.
Bước 5  [Backend]:    Validate tệp tin:
                       - Tệp tin không được rỗng.
                       - Định dạng đuôi mở rộng phải hợp lệ (PNG, JPG, JPEG, GIF, MP4, WEBM, AVI, MOV).
Bước 6  [Backend]:    Tự động chuẩn hóa tên file: System.currentTimeMillis() + số ngẫu nhiên + đuôi mở rộng (để tránh trùng lặp và lỗi traversal).
Bước 7  [Backend]:    Sao chép nội dung tệp vào thư mục "uploads/" trên máy chủ.
Bước 8  [Backend]:    Trả về URL của tệp dạng: /uploads/{ten_file_da_chuan_hoa} (HTTP 200 OK)
Bước 9  [Frontend]:   Nhận URL, hiển thị hình ảnh/video preview trên giao diện người dùng.
```

---

## 4. Quy Tắc Kiểm Tra Đầu Vào (Validation)

### POST /api/upload

| Trường | Kiểm tra | Lỗi khi vi phạm |
|:---|:---|:---|
| `file` | Bắt buộc, không được để trống | "Tệp tin tải lên không được để trống." |
| Đuôi tệp | PNG, JPG, JPEG, GIF, MP4, WEBM, AVI, MOV | "Hệ thống chỉ hỗ trợ các định dạng ảnh và video thông dụng." |
