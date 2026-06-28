# SPEC — Customer Wallet & Deposit (`/wallet`)
> **Feature ID:** `feat-customer` | **Page:** `Wallet`
> **Route:** `/wallet` | **Template:** `templates/account/wallet.html`
> **JS Script:** `static/js/account-wallet.js` | **Prefix:** `cwlt-`
> **Guard:** Private (sessionToken check)

---

## 1. MÔ TẢ TRANG
Trang ví điện tử cá nhân cho phép khách hàng theo dõi số dư tài khoản của họ, được phân loại rõ ràng thành:
- **Số dư khả dụng (`available_balance`)**: Số tiền thực tế người dùng có thể sử dụng để mua hàng hoặc rút ra.
- **Số dư đóng băng (`hold_balance`)**: Số tiền tạm giữ do đơn hàng đang khiếu nại chưa được giải quyết hoặc do lệnh rút đang chờ Staff duyệt.
- Tích hợp cổng nạp tiền tự động: sinh mã VietQR và kiểm tra giao dịch nạp thông qua cổng thanh toán SePay.

---

## 2. MOCKUP GIAO DIỆN (ASCII WIREFRAME)
```
┌──────────────────────────────────────────────────────────────────┐
│  MMO Market Header (Logo | Ví: 1.200.000đ | [Avatar] Menu)       │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Ví Của Tôi / Quản Lý Số Dư                                      │
│  ──────────────────────────────────────────────────────────────  │
│                                                                  │
│  ┌─────────────────────────────┐ ┌────────────────────────────┐  │
│  │ Số dư khả dụng (VNĐ)        │ │ Số dư tạm giữ (VNĐ)        │  │
│  │ 1.200.000đ                  │ │ 300.000đ                   │  │
│  │                             │ │                            │  │
│  │  [ ➕ Nạp Tiền ]             │ │ (Do đang có tranh chấp)    │  │
│  └─────────────────────────────┘ └────────────────────────────┘  │
│                                                                  │
│  Lịch sử giao dịch ví                                            │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │ ID     │ Loại GD   │ Số tiền    │ Trạng thái   │ Ngày tạo   │  │
│  ├────────┼───────────┼────────────┼──────────────┼────────────┤  │
│  │ #WT102 │ Nạp tiền  │ +500.000đ  │ Thành công   │ 27/06/2026 │  │
│  │ #WT101 │ Mua hàng  │ -300.000đ  │ Escrow       │ 26/06/2026 │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

### Mockup Modal Nạp Tiền:
```
┌────────────────────────────────────────────────────┐
│  NẠP TIỀN TỰ ĐỘNG QUA NGÂN HÀNG                     │
├────────────────────────────────────────────────────┤
│  Nhập số tiền cần nạp (Tối thiểu 10.000đ):         │
│  [ 500000                       ] VNĐ              │
│                                                    │
│               [ Tiếp Tục Nạp ]                     │
├────────────────────────────────────────────────────┤
│  Mã QR thanh toán (Tải lại sau 15 phút):           │
│                                                    │
│              ┌──────────────────┐                  │
│              │     [ QR CODE ]  │                  │
│              │   (Quét để nạp)  │                  │
│              └──────────────────┘                  │
│  Nội dung chuyển khoản (bắt buộc):                 │
│  [ NAPTIEN 12                       ] [Copy]       │
│                                                    │
│  [Chờ thanh toán...] 🔄                            │
└────────────────────────────────────────────────────┘
```

---

## 3. CẤU TRÚC FILE HOẠT ĐỘNG
- View: `templates/account/wallet.html` (Thymeleaf template)
- Script: `static/js/account-wallet.js` (Vanilla JS)
- API endpoint phụ thuộc:
  * `GET /api/v1/wallet/balance` (Lấy số dư thực tế)
  * `POST /api/v1/wallet/topup/init` (Khởi tạo yêu cầu nạp tiền)

---

## 4. KHAI BÁO BIẾN TRẠNG THÁI (STATE VARIABLES)
```javascript
let availableBalance = 0;
let holdBalance = 0;
let topupAmount = 10000;
let activeTransactionCode = '';
let checkInterval = null;
```

---

## 5. LUỒNG XỬ LÝ SỰ KIỆN CHI TIẾT (EVENT FLOWS)

### 5.1 Khởi tạo trang (Page Initialization)
1. Kiểm tra session của User qua token:
   ```javascript
   const token = sessionStorage.getItem('accessToken');
   if (!token) { window.location.href = '/login'; return; }
   ```
2. Thực hiện gọi API lấy số dư:
   ```javascript
   fetch('/api/v1/wallet/balance', {
       headers: { 'Authorization': 'Bearer ' + token }
   })
   .then(res => res.json())
   .then(data => {
       document.querySelector('#availableBalance').innerText = formatVND(data.availableBalance);
       document.querySelector('#holdBalance').innerText = formatVND(data.holdBalance);
   });
   ```

### 5.2 Sinh mã QR & Lắng nghe thanh toán VietQR
1. Khách hàng click button "Tiếp Tục Nạp" sau khi điền số tiền.
2. Gửi request:
   ```javascript
   fetch('/api/v1/wallet/topup/init', {
       method: 'POST',
       headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token },
       body: JSON.stringify({ amount: topupAmount })
   })
   ```
3. Nhận phản hồi chứa `transactionCode` và `qrUrl`.
4. Điền mã chuyển khoản `NAPTIEN <UserID>` vào input, hiển thị hình ảnh QR lên modal.
5. Kích hoạt `setInterval` mỗi 5 giây gọi `GET /api/v1/wallet/topup/status?code=` + `transactionCode`.
6. Khi trạng thái chuyển thành `Success` -> Ẩn modal, cập nhật lại số dư khả dụng mới lên UI, hiển thị Toast thông báo.

---

## 6. ĐẶC TẢ CSS & LAYOUT TOKENS
- **Card chứa ví**: `.cwlt-balance-card` sử dụng gradient `#4f46e5` sang `#06b6d4`.
- **Spinner chờ**: `.cwlt-spinner` xoay tròn vô hạn 360 độ sử dụng CSS Keyframes animation.

---

## 7. FUNCTIONAL REQUIREMENTS (EARS)
| ID | EARS Requirement |
|---|---|
| FR-WLT-01 | WHEN a Customer opens the Wallet page, THE SYSTEM SHALL fetch wallet balances and display them in VND format. |
| FR-WLT-02 | WHEN a Customer inputs a top-up amount less than 10,000 VND, THE SYSTEM SHALL disable the submit button and show a validation message. |
| FR-WLT-03 | WHEN the top-up check API returns success, THE SYSTEM SHALL clear the polling timer, close the modal, and refresh balances. |

---

## 8. ACCEPTANCE CRITERIA (Gherkin Scenarios)
- **Kịch bản: Nạp tiền tự động qua QR thành công**
  * **Given** Khách hàng đã đăng nhập và đang mở modal nạp tiền
  * **When** Nhập số tiền 100,000đ và click "Tiếp Tục Nạp"
  * **Then** Giao diện hiển thị mã QR VietQR và dòng nội dung "NAPTIEN 12"
  * **When** Khách hàng hoàn tất chuyển khoản và Webhook SePay ghi nhận thành công
  * **Then** Polling nhận được status Success, modal tự động đóng, số dư ví khả dụng tăng thêm 100,000đ kèm âm thanh/Toast thông báo.