# SPEC — Nạp Tiền Vào Ví (Wallet Top-up)

> **Feature ID:** `feat-customer` | **Page:** `Wallet Top-up`
> **Route:** `/wallet/topup` | **Template:** `templates/account/topup.html`
> **CSS Script:** `static/css/customer/style.css`
> **JS Script:** `static/js/customer/account-topup.js`

---

## 1. TỔNG QUAN TRANG

Trang cho phép người dùng tạo yêu cầu nạp tiền vào ví. Người dùng chỉ cần nhập số tiền muốn nạp, hệ thống (phía Frontend) sẽ tự động sinh ra mã VietQR động chứa đầy đủ thông tin tài khoản ngân hàng đích và cú pháp nội dung chuyển khoản riêng biệt cho tài khoản đó.

**Cấu trúc trang:**
1. **Sidebar điều hướng:** Menu bên trái.
2. **Khu vực Nạp tiền (Main):** 
   - Banner thông báo (Nhập số tiền để tạo hướng dẫn...).
   - Form nhập số tiền và chọn phương thức.
   - Bảng Lưu ý bên phải.

---

## 2. LAYOUT TỔNG THỂ & MOCKUP

```text
┌─────────────────────────────────────────────────────────────────┐
│ [Logo] MMO Market                         [Search]      [User]  │
├─────────────────────────────────────────────────────────────────┤
│  ┌──────────────────────┐  ┌─────────────────────────────────┐  │
│  │ (User Card)          │  │ Ví & giao dịch                  │  │
│  │                      │  │ Nạp tiền vào ví                 │  │
│  │ (Menu Sidebar)       │  │ Tạo yêu cầu nạp tiền và...      │  │
│  │ - Ví của tôi         │  │                      [Số dư]    │  │
│  │ - Nạp tiền (Active)  │  │                      [934.000 đ]│  │
│  │ - Lịch sử giao dịch  │  ├─────────────────────────────────┤  │
│  │                      │  │ Nhập số tiền để tạo hướng dẫn...│  │
│  │                      │  ├─────────────────┬───────────────┤  │
│  │                      │  │ Thông tin nạp   │ Lưu ý         │  │
│  │                      │  │ (<- Về ví)      │ • Chuyển đúng │  │
│  │                      │  │                 │   số tiền...  │  │
│  │                      │  │ Số tiền nạp     │ • Không sửa   │  │
│  │                      │  │ [ Ví dụ 100000] │   nội dung... │  │
│  │                      │  │                 │ • Số dư sẽ cập│  │
│  │                      │  │ [50k] [100k] [200k] [500k]      │  │
│  │                      │  │ [1.000.000đ]                  │  │
│  │                      │  │                                 │  │
│  │                      │  │ [Ngân hàng / Sepay (Selected)]  │  │
│  │                      │  │                                 │  │
│  │                      │  │ [Tạo yêu cầu nạp]               │  │
│  │                      │  └─────────────────┴───────────────┘  │
│  └──────────────────────┘                                       │
└─────────────────────────────────────────────────────────────────┘
```

---

## 3. CÁC THÀNH PHẦN GIAO DIỆN CHÍNH

### 3.1 Thẻ Tiêu Đề & Số dư
* **Header:** Tiêu đề "Nạp tiền vào ví" và mô tả ngắn.
* **Số dư hiện tại:** Hiển thị bên phải để người dùng tiện theo dõi trước khi nạp thêm.

### 3.2 Khối Thông Tin Nạp Tiền (Form)
* **Nút "Về ví":** Điều hướng người dùng quay lại trang Dashboard `/wallet`.
* **Input số tiền nạp:** Ô nhập liệu dạng số (VND).
* **Gợi ý số tiền (Quick amount buttons):** Các nút bấm (50.000đ, 100.000đ, 200.000đ, 500.000đ, 1.000.000đ). Khi nhấn vào, số tiền tương ứng sẽ tự động điền vào ô input.
* **Phương thức thanh toán:** Mặc định chọn "Chuyển khoản ngân hàng / Sepay" (thành phần giao diện chọn phương thức, có viền active màu xanh).
* **Nút "Tạo yêu cầu nạp":** Kích hoạt logic sinh mã QR.

### 3.3 Khối Lưu Ý
* Các gạch đầu dòng tĩnh cảnh báo người dùng phải chuyển đúng nội dung để hệ thống tự động nhận diện bằng SePay Webhook.

---

## 4. LUỒNG XỬ LÝ JS & AJAX

1. **Khởi tạo thông số (Lấy thông tin thụ hưởng):**
   * Khi trang load, gọi API `GET /api/sepay/config` để lấy thông tin ngân hàng thụ hưởng tĩnh từ Backend (BankID, AccountNumber, AccountName).
2. **Xử lý form nạp tiền:**
   * Bắt sự kiện người dùng bấm "Tạo yêu cầu nạp". Lấy giá trị từ Input số tiền.
   * Validate: Số tiền tối thiểu/tối đa.
   * Tạo **Mã nội dung chuyển khoản (Transfer Code)**: Format chuẩn là `MMO TOPUP {UserId}`. (UserId được gán sẵn qua session/thymeleaf hoặc API context).
3. **Sinh mã QR & Hiển thị (Local QR Generation):**
   * Sử dụng API của bên thứ 3 (ví dụ `https://img.vietqr.io/image/...`) để tạo mã QR dạng ảnh. Cú pháp URL ghép từ `BankID`, `AccountNumber`, `Số tiền`, và `Nội dung chuyển khoản`.
   * Chuyển đổi giao diện sang màn hình/modal "Quét mã thanh toán" (Hiển thị ảnh QR và nút Copy thông tin tài khoản + nội dung để nạp).
4. **Kiểm tra trạng thái (Polling):**
   * Setup `setInterval` cứ 3-5 giây gọi 1 API (có thể là `GET /api/v1/wallet/transactions` để xem có giao dịch mới type `TOPUP` hay không) hoặc lắng nghe WebSocket để biết tiền đã vào ví thành công và chuyển hướng về trang Ví.
