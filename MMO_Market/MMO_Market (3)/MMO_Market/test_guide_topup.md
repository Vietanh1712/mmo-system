# 🧪 Hướng Dẫn Test: Chức Năng Nạp Tiền Vào Ví (Wallet Top-up)

> **Phạm vi test**: Quy trình nạp tiền qua cổng SePay, tạo mã VietQR động ở Frontend, cơ chế Polling nhận diện tiền vào, kiểm tra giới hạn nạp tiền tối thiểu/tối đa, bảo mật Endpoint cấu hình ngân hàng.

---

## 📌 BƯỚC 0 — Chuẩn bị môi trường

### 0.1 Đảm bảo server đang chạy
- Server Spring Boot chạy tại cổng **8080**.
- Đảm bảo file cấu hình `apps/backend/src/main/resources/application.properties` đã kết nối thành công tới SQL Server database của bạn.

### 0.2 Tài khoản và Cấu hình có sẵn trong DB
Các tài khoản mặc định và cấu hình hạn mức sẽ được chèn ở Bước 1.

---

## 🗃️ BƯỚC 1 — Chèn dữ liệu mock vào SQL Server

Mở **SQL Server Management Studio (SSMS)** hoặc **Azure Data Studio**, kết nối tới database và chạy script SQL sau:

```sql
-- 1. Xóa các bản ghi test cũ (nếu có) để tránh xung đột
DELETE FROM Users WHERE email IN ('customer_topup_test@mmo.com', 'staff_topup_test@mmo.com');
DELETE FROM SystemConfigurations WHERE config_key IN ('MIN_DEPOSIT_LIMIT_VND', 'MAX_DEPOSIT_LIMIT_VND');

-- 2. Tạo tài khoản Customer dùng để test nạp tiền (Mật khẩu: 123456)
INSERT INTO Users (
    email, password, full_name, role, phone, isVerified, isLocked, isDelete,
    balance_vnd, deposit_vnd, created_at
)
VALUES (
    'customer_topup_test@mmo.com',
    '$2y$10$uRFrFYP.Ld32A.LSRCm10.LlUfbJH7vgelUd4h1dsvZCyMKz1c0Bq', -- 123456
    'Nguyễn Văn Nạp Tiền',
    '{"role":"Customer"}',
    '0901234599',
    1, 0, 0,
    50000, -- Số dư ban đầu: 50,000 đ
    0,
    GETDATE()
);

-- 3. Tạo tài khoản Staff dùng để test chặn quyền truy cập nạp tiền (Mật khẩu: 123456)
INSERT INTO Users (
    email, password, full_name, role, phone, isVerified, isLocked, isDelete,
    balance_vnd, deposit_vnd, created_at
)
VALUES (
    'staff_topup_test@mmo.com',
    '$2y$10$uRFrFYP.Ld32A.LSRCm10.LlUfbJH7vgelUd4h1dsvZCyMKz1c0Bq', -- 123456
    'Nhân Viên Kiểm Thử',
    '{"role":"Staff"}',
    '0987654399',
    1, 0, 0,
    0,
    0,
    GETDATE()
);

-- 4. Tạo các cấu hình hạn mức nạp tiền hệ thống
INSERT INTO SystemConfigurations (config_key, config_value, description, updated_at)
VALUES 
    ('MIN_DEPOSIT_LIMIT_VND', '10000', N'Hạn mức nạp tiền tối thiểu', GETDATE()),
    ('MAX_DEPOSIT_LIMIT_VND', '50000000', N'Hạn mức nạp tiền tối đa', GETDATE());
```

---

## 🧪 PHẦN A — Các kịch bản kiểm thử giao diện (Frontend)

Đăng nhập bằng tài khoản **Buyer/Customer** vừa tạo: `customer_topup_test@mmo.com` / `123456`

### 1. TC-TOPUP-002: Kiểm tra chuyển hướng khách vãng lai (Guest)
- **Mô tả**: Khách vãng lai chưa đăng nhập cố tình truy cập trang nạp tiền.
- **Các bước**:
  1. Mở trình duyệt ẩn danh hoặc đăng xuất khỏi hệ thống.
  2. Truy cập trực tiếp địa chỉ: `http://localhost:8080/wallet/topup`.
- **Kết quả mong đợi**: Hệ thống tự động chuyển hướng người dùng về trang Đăng nhập (`/login`).

### 2. TC-TOPUP-003: Chặn quyền truy cập đối với Staff / Admin
- **Mô tả**: Tài khoản có vai trò Staff hoặc Admin không được phép nạp tiền.
- **Các bước**:
  1. Đăng nhập bằng tài khoản `staff_topup_test@mmo.com` / `123456`.
  2. Truy cập địa chỉ `http://localhost:8080/wallet/topup`.
- **Kết quả mong đợi**: Giao diện hiển thị lỗi 403 Forbidden hoặc trả về trang lỗi thông báo vai trò này không được phép nạp tiền.

### 3. TC-TOPUP-004: Validate trường nhập số tiền nạp
- **Mô tả**: Xác minh các điều kiện đầu vào của trường số tiền.
- **Các bước**:
  1. Đăng nhập bằng `customer_topup_test@mmo.com` / `123456`.
  2. Vào trang `http://localhost:8080/wallet/topup`.
  3. Để trống ô nhập số tiền hoặc điền chữ cái (e.g. `abc`), click "Tạo yêu cầu nạp".
  4. Điền số tiền nhỏ hơn hạn mức tối thiểu, ví dụ `5000` (5,000 đ), click "Tạo yêu cầu nạp".
- **Kết quả mong đợi**:
  - Không cho phép nhập chữ hoặc tự động lọc.
  - Khi số tiền < 10,000đ, nút "Tạo yêu cầu nạp" bị vô hiệu hóa hoặc hiển thị lỗi cảnh báo đỏ: *"Số tiền nạp tối thiểu là 10,000 đ"*. Không sinh mã QR.

### 4. TC-TOPUP-005: Hoạt động của các nút chọn nhanh (Quick-amount)
- **Mô tả**: Các nút chọn nhanh số tiền hoạt động chính xác.
- **Các bước**:
  1. Nhấp lần lượt vào các nút `50k`, `100k`, `200k`, `500k`, `1.000.000đ`.
- **Kết quả mong đợi**: Giá trị trong ô nhập tiền tự động thay đổi thành các số tương ứng: `50000`, `100000`, `200000`, `500000`, `1000000`.

### 5. TC-TOPUP-006: Hiển thị mã QR và tính năng sao chép (Copy)
- **Mô tả**: Hệ thống sinh VietQR động chứa đúng thông tin tài khoản và cú pháp chuyển tiền.
- **Các bước**:
  1. Nhập số tiền nạp: `50000`.
  2. Click **"Tạo yêu cầu nạp"**.
  3. Kiểm tra ảnh mã QR và thông tin tài khoản hiển thị trên màn hình.
  4. Lấy ID tài khoản người dùng từ DB (e.g. `12` hoặc xem trong bảng Users).
  5. Click thử các nút **Copy** bên cạnh Số tài khoản, Số tiền, và Nội dung.
- **Kết quả mong đợi**:
  - Xuất hiện ảnh QR được tải động từ VietQR API.
  - Thông tin thụ hưởng trùng khớp cấu hình: Ngân hàng **TPB**, số tài khoản **68917122004**, tên tài khoản **TRINH VIET ANH**.
  - Nội dung chuyển khoản hiển thị đúng cú pháp: `MMO-TOPUP-<userId>` (ví dụ: `MMO-TOPUP-15`).
  - Khi click nút copy, nhãn nút chuyển sang biểu tượng tick xanh ✅ hoặc hiển thị thông báo "Đã copy!" thành công.

---

## 🧪 PHẦN B — Các kịch bản kiểm thử API/Webhook (Backend)

Sử dụng công cụ **cURL** (trong Terminal/PowerShell) hoặc **Postman** để kiểm tra API nạp tiền tự động:

### 1. TC-TOPUP-001: Webhook nạp tiền thành công
- **Mô tả**: Giả lập cổng thanh toán SePay gửi webhook báo giao dịch thành công.
- **Cách thực hiện**:
  1. Tìm ID của user `customer_topup_test@mmo.com` trong DB:
     ```sql
     SELECT id, balance_vnd FROM Users WHERE email = 'customer_topup_test@mmo.com';
     ```
  2. Gửi lệnh cURL sau (thay thế `<userId>` bằng ID tìm được, ví dụ `18`):
     ```powershell
     curl -X POST "http://localhost:8080/api/sepay/webhook" `
       -H "Authorization: Apikey IR7WFW2P3SXCNHZOVTX9BD1BYKOKXHQ9MMZJF0OAY6EV85IPDGYNZJSACJKAELHA" `
       -H "Content-Type: application/json" `
       -d "{
         \"id\": 888001,
         \"transferType\": \"in\",
         \"transferAmount\": 150000,
         \"content\": \"MMO-TOPUP-<userId>\",
         \"referenceCode\": \"REF888001\"
       }"
     ```
- **Kết quả mong đợi**:
  - Phản hồi HTTP 200: `{ "success": true, "message": "Webhook processed successfully" }`.
  - Số dư của user tăng thêm 150,000 đ.
  - Bảng `TopupTransactions` được lưu thêm 1 dòng trạng thái `Success` với `sepayCode = 888001`.
  - Bảng `WalletTransactions` lưu thêm lịch sử giao dịch nạp tiền.

### 2. TC-TOPUP-001-B: Sai cú pháp nội dung chuyển tiền
- **Mô tả**: Webhook gửi nội dung không khớp cú pháp định dạng.
- **Cách thực hiện**:
  ```powershell
  curl -X POST "http://localhost:8080/api/sepay/webhook" `
    -H "Authorization: Apikey IR7WFW2P3SXCNHZOVTX9BD1BYKOKXHQ9MMZJF0OAY6EV85IPDGYNZJSACJKAELHA" `
    -H "Content-Type: application/json" `
    -d "{
      \"id\": 888002,
      \"transferType\": \"in\",
      \"transferAmount\": 150000,
      \"content\": \"NAP TIEN CHO TRINH VIET ANH\",
      \"referenceCode\": \"REF888002\"
    }"
  ```
- **Kết quả mong đợi**:
  - Phản hồi HTTP 400 Bad Request.
  - Số dư tài khoản không thay đổi.
  - Bảng `TopupTransactions` lưu trạng thái `Failed` kèm theo nguyên nhân: *"Nội dung chuyển khoản không đúng cú pháp..."*.

### 3. TC-TOPUP-001-C: Phòng chống nạp trùng lặp (Idempotency)
- **Mô tả**: Hệ thống từ chối xử lý khi nhận cùng một mã giao dịch SePay hai lần.
- **Cách thực hiện**:
  1. Gửi lại chính xác lệnh cURL thành công ở kịch bản `TC-TOPUP-001` (trùng mã `id = 888001`).
- **Kết quả mong đợi**:
  - Phản hồi HTTP 200 (để SePay không báo lỗi hệ thống).
  - Số dư người dùng **không** được cộng thêm lần hai.
  - Không phát sinh bản ghi giao dịch mới trong database.

### 4. TC-TOPUP-001-D: Số tiền nạp không hợp lệ (<= 0)
- **Mô tả**: Từ chối giao dịch có số tiền nạp âm hoặc bằng 0.
- **Cách thực hiện**:
  ```powershell
  curl -X POST "http://localhost:8080/api/sepay/webhook" `
    -H "Authorization: Apikey IR7WFW2P3SXCNHZOVTX9BD1BYKOKXHQ9MMZJF0OAY6EV85IPDGYNZJSACJKAELHA" `
    -H "Content-Type: application/json" `
    -d "{
      \"id\": 888003,
      \"transferType\": \"in\",
      \"transferAmount\": 0,
      \"content\": \"MMO-TOPUP-<userId>\",
      \"referenceCode\": \"REF888003\"
    }"
  ```
- **Kết quả mong đợi**: Phản hồi HTTP 400 Bad Request, giao dịch bị từ chối, số dư không đổi.

### 5. TC-TOPUP-001-E: Kiểm tra giới hạn nạp tiền tối thiểu/tối đa
- **Mô tả**: Từ chối các khoản nạp nằm ngoài phạm vi 10,000 đ - 50,000,000 đ.
- **Cách thực hiện**:
  - Thử nạp dưới mức tối thiểu (5,000 đ):
    ```powershell
    curl -X POST "http://localhost:8080/api/sepay/webhook" `
      -H "Authorization: Apikey IR7WFW2P3SXCNHZOVTX9BD1BYKOKXHQ9MMZJF0OAY6EV85IPDGYNZJSACJKAELHA" `
      -H "Content-Type: application/json" `
      -d "{
        \"id\": 888004,
        \"transferType\": \"in\",
        \"transferAmount\": 5000,
        \"content\": \"MMO-TOPUP-<userId>\",
        \"referenceCode\": \"REF888004\"
      }"
    ```
  - Thử nạp vượt mức tối đa (60,000,000 đ):
    ```powershell
    curl -X POST "http://localhost:8080/api/sepay/webhook" `
      -H "Authorization: Apikey IR7WFW2P3SXCNHZOVTX9BD1BYKOKXHQ9MMZJF0OAY6EV85IPDGYNZJSACJKAELHA" `
      -H "Content-Type: application/json" `
      -d "{
        \"id\": 888005,
        \"transferType\": \"in\",
        \"transferAmount\": 60000000,
        \"content\": \"MMO-TOPUP-<userId>\",
        \"referenceCode\": \"REF888005\"
      }"
    ```
- **Kết quả mong đợi**:
  - Cả hai request đều trả về HTTP 400 Bad Request.
  - Bảng `TopupTransactions` ghi nhận lỗi: *"Số tiền nạp nhỏ hơn hạn mức tối thiểu..."* hoặc *"Số tiền nạp vượt quá hạn mức tối đa..."*.

---

## ⚡ BƯỚC 3 — Xác minh cơ chế tự động cập nhật số dư (Polling)
1. Đăng nhập tài khoản `customer_topup_test@mmo.com` trên trình duyệt.
2. Truy cập `http://localhost:8080/wallet/topup`. Nhập số tiền `50000` và bấm "Tạo yêu cầu nạp" để hiển thị modal quét mã QR.
3. Không tắt trình duyệt. Chạy lệnh cURL nạp thành công ở **TC-TOPUP-001** (nhớ đổi sang mã `id` giao dịch mới, ví dụ `888010`).
4. Xem màn hình trình duyệt:
   - **Kết quả**: Sau khi chạy cURL khoảng 3-5 giây, trang quét mã QR tự động đóng lại, hiển thị thông báo nạp thành công, và màn hình cập nhật số dư của bạn tăng thêm 50,000 đ.
