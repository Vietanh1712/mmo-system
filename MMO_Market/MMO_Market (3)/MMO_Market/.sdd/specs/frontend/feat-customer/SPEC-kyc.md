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
Trang KYC hỗ trợ 2 trạng thái: **Chế độ mặc định (View Mode)** và **Chế độ nộp hồ sơ (Submit Mode)** (khi click Gửi yêu cầu hoặc vào url `?mode=submit`).

### 3.1 Chế độ mặc định (View Mode)
```
┌─────────────────────────────────────────────────────────────────┐
│ [Logo] MMO Market                         [Search]      [User]  │
├─────────────────────────────────────────────────────────────────┤
│  ┌──────────────────────┐  ┌─────────────────────────────────┐  │
│  │ (User Card)          │  │ Định danh KYC     [Chưa định danh] │
│  │                      │  │ Xác minh danh tính để tăng...   │  │
│  │ (Menu Sidebar)       │  ├─────────────────────────────────┤  │
│  │                      │  │ [i] KYC giúp tăng độ tin cậy... │  │
│  │                      │  ├─────────────────────────────────┤  │
│  │                      │  │ (1) Chuẩn bị -> (2) Gửi yêu cầu │  │
│  │                      │  │ -> (3) Staff duyệt -> (4) Hoàn tất │
│  │                      │  ├─────────────────────────────────┤  │
│  │                      │  │ Bạn chưa gửi hồ sơ KYC          │  │
│  │                      │  │ [ Gửi yêu cầu định danh ]       │  │
│  │                      │  │ Mã yêu cầu: -     Ngày gửi: -   │  │
│  │                      │  │ Loại giấy tờ: -                 │  │
│  └──────────────────────┘  └─────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### 3.2 Chế độ nộp hồ sơ (Submit Mode - `?mode=submit`)
```
┌─────────────────────────────────────────────────────────────────┐
│  ┌──────────────────────┐  ┌─────────────────────────────────┐  │
│  │ (Sidebar)            │  │ (Giữ nguyên header và Stepper)  │  │
│  │                      │  ├─────────────────────────────────┤  │
│  │                      │  │ Gửi yêu cầu định danh           │  │
│  │                      │  │ Họ và tên theo giấy tờ          │  │
│  │                      │  │ [ Nguyễn Văn Khách            ] │  │
│  │                      │  │ Số CCCD/CMND/Hộ chiếu           │  │
│  │                      │  │ [ 001096001234                ] │  │
│  │                      │  │ Ngày sinh       Loại giấy tờ    │  │
│  │                      │  │ [ 15/05/1996 📅][ Chọn loại v ] │  │
│  │                      │  │ Địa chỉ thường trú              │  │
│  │                      │  │ [ 123 Đường Nguyễn Trãi...    ] │  │
│  │                      │  │                                 │  │
│  │                      │  │ [Ảnh mặt trước] [Ảnh mặt sau]   │  │
│  │                      │  │ [Ảnh chân dung]                 │  │
│  │                      │  │                                 │  │
│  │                      │  │ [x] Tôi xác nhận thông tin...   │  │
│  │                      │  │ [ Gửi yêu cầu ] [ Hủy ]         │  │
│  └──────────────────────┘  └─────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 4. CÁC THÀNH PHẦN GIAO DIỆN CHÍNH

### 4.1 Thanh tiến trình (Stepper)
* Hiển thị 4 bước của quy trình KYC: (1) Chuẩn bị hồ sơ, (2) Gửi yêu cầu, (3) Staff xét duyệt, (4) Hoàn tất. Trạng thái hiển thị phụ thuộc vào dữ liệu API trả về.

### 4.2 Lịch sử & Trạng thái KYC
* Hiển thị bảng tóm tắt "Bạn chưa gửi hồ sơ KYC" (hoặc đang duyệt) gồm Mã yêu cầu, Ngày gửi, Loại giấy tờ.
* Có nút `[ Gửi yêu cầu định danh ]` màu xanh (hiển thị khi chưa có yêu cầu nào PENDING). Nhấn vào sẽ chuyển giao diện sang dạng form (`?mode=submit`).

### 4.3 Form Gửi Yêu Cầu Định Danh (`?mode=submit`)
* **Input Text:** Họ và tên (`#fullName`), Số CCCD/CMND (`#idNumber`), Ngày sinh (`#dateOfBirth` - dạng datepicker), Địa chỉ (`#address` - textarea).
* **Dropdown:** Loại giấy tờ (`#idType` - CCCD/CMND/Hộ chiếu).
* **Vùng Tải Lên Ảnh:** Gồm 3 khối tải tệp riêng biệt (Ảnh mặt trước, Ảnh mặt sau, Ảnh chân dung). Thiết kế viền đứt nét đè xem trước ảnh (Preview) khi chọn file.
* **Checkbox:** Xác nhận trách nhiệm thông tin cung cấp.
* **Buttons:** `[ Gửi yêu cầu ]` (Submit form) và `[ Hủy ]` (Quay về chế độ View).

---

## 5. LUỒNG XỬ LÝ JS & AJAX

1. **Kiểm tra trạng thái KYC hiện tại khi load trang:**
   * Gửi API:
     * **Endpoint:** `GET /api/v1/kyc/me`
     * **Headers:** `Authorization: Bearer <token>`
   * **Xử lý kết quả (List lịch sử KYC):**
     * Danh sách rỗng -> Hiển thị "Chưa định danh", Form trống.
     * Có yêu cầu `PENDING` -> Hiển thị bước Stepper tương ứng, khóa nút Gửi yêu cầu mới.
     * Yêu cầu `APPROVED` / `REJECTED` -> Hiển thị badge trạng thái và lịch sử duyệt ở bảng tóm tắt.
2. **Gửi Hồ Sơ KYC Mới:**
   * Bắt đầu validate local: Các ô text không rỗng, chọn loại giấy tờ, bắt buộc tải lên đủ 3 ảnh (jpeg/png < 5MB), bắt buộc tích Checkbox xác nhận.
   * Tạo đối tượng `FormData`:
     ```javascript
     const formData = new FormData();
     formData.append('fullName', fullNameInput.value);
     formData.append('dateOfBirth', dobInput.value);
     formData.append('address', addressInput.value);
     formData.append('idNumber', idNumberInput.value);
     formData.append('idType', idTypeInput.value);
     formData.append('frontImage', frontFile);
     formData.append('backImage', backFile);
     formData.append('selfieImage', selfieFile);
     ```
   * Gửi API:
     * **Endpoint:** `POST /api/v1/kyc`
     * **Method:** `POST`
     * **Headers:** `Authorization: Bearer <token>` (Không set Content-Type)
     * **Body:** `formData`
   * **Thành công (HTTP 200/201):** Thông báo gửi hồ sơ thành công và reload lại trang về chế độ View.

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