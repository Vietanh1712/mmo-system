# PLAN — Upload Service (`feat-upload`)

## 1. Mục tiêu (Goals)

Triển khai dịch vụ tải lên hình ảnh sản phẩm, ảnh KYC danh tính và video chứng từ lên máy chủ hệ thống theo đặc tả `SPEC.md` (feat-upload). Hệ thống cho phép người dùng đăng nhập lưu trữ tệp tin đa phương tiện lên máy chủ, nhận lại đường dẫn URL tĩnh để lưu trữ vào cơ sở dữ liệu.

## 2. Kiến trúc & Công nghệ

- **Backend:** Java 17, Spring Boot 3.1.
- **Frontend:** Các form HTML chứa `<input type="file">` sử dụng `FormData` gửi request dạng `multipart/form-data`.
- **Bảo mật:**
  - Kiểm tra quyền sở hữu phiên đăng nhập (`@AuthenticationPrincipal Long userId`).
  - Lọc loại tập tin (chỉ cho phép ảnh `.png`, `.jpg`, `.jpeg`, `.gif` và video `.mp4`, `.webm`, `.avi`, `.mov` thông dụng).
  - Tránh xung đột trùng tên bằng cách gán định danh ngẫu nhiên dựa trên timestamp (`System.currentTimeMillis()`).
  - Chống lỗ hổng Directory Traversal bằng cách chuẩn hoá tên tệp.

## 3. Các thành phần Backend

### 3.1. Database Migration & Entities

- Không sử dụng bảng cơ sở dữ liệu chuyên biệt. Các đường dẫn file trả về được lưu trực tiếp vào các thực thể liên quan (ví dụ: `Product.image`, `KycRequest.frontIdImage`).

### 3.2. Repositories

- Không có.

### 3.3. DTOs

- Request: Đối tượng `MultipartFile` gửi qua Form-Data.
- Response: Đối tượng Map chứa `url` dạng JSON (ví dụ: `{"url": "/uploads/1781743882173_image.jpg"}`).

### 3.4. Services (Business Logic)

- Thực hiện trực tiếp trên Controller hoặc tích hợp bộ lưu trữ nội bộ (Local Disk Storage) thông qua lớp `Files` của Java NIO.

### 3.5. Controllers & Security

- **`UploadController`**:
  - Endpoint: `POST /api/upload`.
  - Kiểm tra xác thực của người dùng. Nếu chưa đăng nhập, trả về lỗi `401 Unauthorized`.
  - Validate file rỗng, kiểm tra định dạng mở rộng (whitelist extensions).
  - Copy file stream vào thư mục `uploads/` trên root dự án.
  - Cấu hình Spring Boot Static Resource Handler để phục vụ file tĩnh qua URL `/uploads/**`.

---

## 4. Các thành phần Frontend

- Tích hợp trực tiếp tại các module sử dụng file upload như:
  - Tải ảnh CCCD trong [account-kyc.js](file:///c:/Users/pc/MMO_new1/MMO_Market/MMO_Market%20(3)/MMO_Market/apps/frontend/static/js/customer/account-kyc.js).
  - Tải ảnh sản phẩm trong trang đăng bán sản phẩm của Seller.
  - Tải ảnh minh chứng trong tranh chấp đơn hàng (Dispute).

---

## 5. Definition of Done

- Chỉ cho phép người dùng đã xác thực tải lên tệp tin.
- Hệ thống bắt buộc phải kiểm tra và loại bỏ các định dạng file thực thi nguy hiểm (như `.exe`, `.sh`, `.php`, `.jsp`, `.asp`) để tránh lỗi bảo mật Upload Vulnerability.