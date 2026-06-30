# SPEC — Xác Thực Danh Tính (KYC Verification Upload)

> **Feature ID:** `feat-kyc` | **Page:** `KycVerification`
> **Route:** `/account/kyc` | **Template:** `templates/account/kyc.html`
> **CSS Script:** `static/css/customer/style.css`
> **JS Script:** `static/js/account-kyc.js`
> **Version:** 1.0 | **Status:** Draft
> **Backend ref:** `feat-kyc/UC-03-kyc-verification.md`

---

## 1. TỔNG QUAN TRANG

Trang KYC cung cấp giao diện tải lên hồ sơ pháp lý gồm Số CCCD/CMND, ảnh Mặt trước CCCD, ảnh Mặt sau CCCD và ảnh Chân dung tự chụp. Trang tự động điều chỉnh hiển thị dựa trên trạng thái hồ sơ hiện tại (`NOT_SUBMITTED`, `PENDING`, `APPROVED`, `REJECTED`).

**Cấu trúc trang:**
1. **Sidebar điều hướng:** Menu quản lý tài khoản bên trái.
2. **Kyc Card (Thành phần chính):**
   * *Nếu chưa gửi:* Form điền số CCCD và 3 vùng tải lên hình ảnh có xem trước (image preview).
   * *Nếu đang chờ duyệt:* Panel thông báo trạng thái "Đang chờ kiểm duyệt" và khóa toàn bộ form.
   * *Nếu đã được duyệt:* Panel thông báo "Tài khoản đã xác minh thành công" kèm tick xanh bảo mật.
   * *Nếu bị từ chối:* Panel hiển thị lý do từ chối kiểm duyệt và hiển thị lại form để gửi lại hồ sơ mới.

---

## 2. DESIGN TOKENS ÁP DỤNG

```css
/* Color Palette */
--ds-primary:         #2563eb;
--ds-primary-hover:   #1d4ed8;
--ds-bg:              #f8fafc;
--ds-card:            #ffffff;
--ds-border:          #e2e8f0;
--ds-error:           #ef4444;
--ds-success:         #10b981;
--ds-warning:         #f59e0b;

/* Spacing & Borders */
--ds-radius-md:       8px;
--ds-radius-lg:       12px;
--ds-shadow:          0 1px 3px 0 rgba(0,0,0,0.1);
```

---

## 3. LAYOUT TỔNG THỂ & MOCKUP

```
┌────────────────────────────────────────────────────────┐
│ [Logo] MMO Market                              [User]  │
├────────────────────────────────────────────────────────┤
│  ┌───────────┐  ┌────────────────────────────────────┐ │
│  │  Sidebar  │  │  XÁC THỰC DANH TÍNH (KYC)          │ │
│  │  - Hồ sơ  │  │  Họ tên: [ Nguyễn Văn A        ]   │ │
│  │  - Ví tiền│  │  Số CCCD/CMND: [ 001095012345  ]   │ │
│  │  - KYC    │  │  ┌──────────┐ ┌──────────┐         │ │
│  │  - Orders │  │  │Mặt trước │ │Mặt sau   │         │ │
│  └───────────┘  │  │[Xem ảnh] │ │[Xem ảnh] │         │ │
│                 │  └──────────┘ └──────────┘         │ │
│                 │  [ GỬI YÊU CẦU XÁC THỰC ]          │ │
│                 └────────────────────────────────────┘ │
└────────────────────────────────────────────────────────┘
```

---

## 4. CÁC THÀNH PHẦN GIAO DIỆN CHÍNH

### 4.1 Vùng Tải Lên Ảnh — `.kyc-upload-zone`
* Gồm 3 hộp tải tệp riêng biệt: Mặt trước, Mặt sau và Ảnh chân dung.
* Thiết kế: Đường viền đứt nét (`border: 2px dashed var(--ds-border)`), căn giữa biểu tượng đám mây tải lên và nhãn text hướng dẫn. Hover sẽ đổi nét vẽ viền sang màu xanh thương hiệu.
* **Xem trước hình ảnh (Image Preview):** Khi người dùng chọn file ảnh thành công, JS đọc tệp bằng `FileReader` và kết xuất ảnh hiển thị đè lên vùng upload zone kèm một nút "Xóa ảnh" nhỏ ở góc phải để người dùng có thể tải lại ảnh khác.

### 4.2 Hộp Thông Báo Trạng Thái — `.kyc-status-banner`
* Trạng thái `PENDING`: Nền màu vàng nhạt, chữ màu cam đậm, hiển thị icon đồng hồ cát.
* Trạng thái `APPROVED`: Nền màu xanh lá nhạt, chữ màu xanh lá đậm, hiển thị icon tick tròn xanh.
* Trạng thái `REJECTED`: Nền màu đỏ nhạt, chữ màu đỏ đậm, hiển thị rõ lý do từ chối cụ thể (`rejectionReason`).

---

## 5. LUỒNG XỬ LÝ JS & AJAX

1. **Kiểm tra trạng thái KYC hiện tại khi load trang:**
   * Gửi API:
     * **Endpoint:** `GET /api/v1/kyc/me`
     * **Headers:** `Authorization: Bearer <token>`
   * **Xử lý kết quả:**
     * Trả về trạng thái `NOT_SUBMITTED` -> Hiển thị form trống.
     * Trả về trạng thái `PENDING` -> Hiển thị thông báo "Đang chờ duyệt", vô hiệu hóa tất cả nút và ô nhập liệu.
     * Trả về trạng thái `APPROVED` -> Hiển thị thông báo "Đã xác thực", hiển thị các thông tin cơ bản ở dạng chỉ đọc.
     * Trả về trạng thái `REJECTED` -> Hiển thị thông báo lý do từ chối kèm form điền thông tin cũ để người dùng chỉnh sửa.
2. **Gửi Hồ Sơ KYC Mới:**
   * Bắt đầu validate local: Số CCCD phải dài từ 9 đến 12 chữ số. Cả 3 file ảnh bắt buộc phải được chọn, định dạng file phải là `image/jpeg` hoặc `image/png`, kích thước mỗi file `< 5MB`.
   * Tạo đối tượng `FormData`:
     ```javascript
     const formData = new FormData();
     formData.append('fullName', fullNameInput.value);
     formData.append('citizenId', citizenIdInput.value);
     formData.append('frontImage', frontFile);
     formData.append('backImage', backFile);
     formData.append('selfieImage', selfieFile);
     ```
   * Gửi API:
     * **Endpoint:** `POST /api/v1/kyc`
     * **Method:** `POST`
     * **Headers:** `Authorization: Bearer <token>` (Không set Content-Type để trình duyệt tự động thiết lập multipart boundary).
     * **Body:** `formData`
   * **Xử lý phản hồi thành công (HTTP 200):** Hiển thị Toast thông báo gửi thành công và tải lại trang để chuyển giao diện sang trạng thái `PENDING`.

---

## 6. RESPONSIVE

* **Viewport ≥ 768px:** Vùng tải ảnh hiển thị dạng hàng ngang gồm 3 cột tương ứng 3 vùng ảnh.
* **Viewport < 768px:** Vùng tải ảnh xếp chồng lên nhau thành 3 hàng dọc.

---

## 7. ACCESSIBILITY

- Input file ẩn có thuộc tính `tabindex="-1"`, kích hoạt click gián tiếp thông qua nhãn `<label>` hoặc nút bấm có focus ring.
- Banner trạng thái dùng thuộc tính `role="status"` để người khiếm thị sử dụng trình đọc màn hình nhận biết ngay biến đổi trạng thái duyệt.

---

## 8. OUT OF SCOPE

- ❌ Cắt ảnh (Crop) hoặc xoay ảnh trực tiếp trên trình duyệt.
- ❌ Hỗ trợ chụp ảnh trực tiếp bằng Webcam trên trình duyệt (chỉ cho phép tải tệp có sẵn).