# SPEC — Kiểm Duyệt Hồ Sơ KYC (Staff KYC Review)

> **Feature ID:** `feat-staff` | **Pages:** `KycReviewList`, `KycReviewDetail`
> **Routes:** `/staff/kyc`, `/staff/kyc/{kycId}`
> **Templates:** `templates/staff/kyc.html`, `templates/staff/kyc-detail.html`
> **CSS Script:** `static/css/customer/style.css`
> **JS Scripts:** `static/js/staff-kyc.js`, `static/js/staff-kyc-detail.js`
> **Version:** 1.0 | **Status:** Draft
> **Backend ref:** `feat-kyc/UC-03-kyc-verification.md`

---

## 1. TỔNG QUAN TRANG

Trang kiểm duyệt hồ sơ KYC cung cấp giao diện dành riêng cho nhân viên vận hành (Staff) và quản trị viên (Admin) để đánh giá tính xác thực của thông tin định danh do người dùng gửi lên, đối chiếu ảnh CCCD mặt trước, mặt sau và ảnh selfie chân dung để đưa ra quyết định phê duyệt nâng cấp tài khoản hoặc từ chối kèm lý do.

**Cấu trúc phân hệ gồm hai trang chính:**
1. **Trang danh sách yêu cầu (`/staff/kyc`):** Hiển thị bảng phân trang các yêu cầu KYC có trạng thái `Pending` xếp theo thứ tự thời gian cũ nhất được đưa lên đầu (FIFO).
2. **Trang chi tiết yêu cầu (`/staff/kyc/{kycId}`):** Giao diện so sánh trực quan, hiển thị phóng to 3 ảnh tài liệu, các nút hành động "Phê duyệt" và "Từ chối" kèm ô nhập lý do.

---

## 2. DESIGN TOKENS ÁP DỤNG

```css
/* Color Palette */
--ds-primary:         #2563eb;
--ds-primary-hover:   #1d4ed8;
--ds-bg:              #f8fafc;
--ds-card:            #ffffff;
--ds-border:          #cbd5e1;
--ds-error:           #ef4444;
--ds-success:         #10b981;
--ds-warning:         #f59e0b;

/* Shape & Spacing */
--ds-radius-md:       8px;
--ds-radius-lg:       12px;
--ds-shadow:          0 4px 6px -1px rgba(0,0,0,0.1);
```

---

## 3. LAYOUT TỔNG THỂ & MOCKUP

```
┌────────────────────────────────────────────────────────┐
│ [Logo] MMO Market Staff Console                [Staff] │
├────────────────────────────────────────────────────────┤
│  CHI TIẾT KIỂM DUYỆT KYC #KYC452                       │
│  Thành viên: Nguyễn Văn A | Số CCCD: 001095012345      │
│  ────────────────────────────────────────────────────  │
│                                                        │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐    │
│  │ Mặt trước    │ │ Mặt sau      │ │ Chân dung    │    │
│  │ [Ảnh Zoom]   │ │ [Ảnh Zoom]   │ │ [Ảnh Zoom]   │    │
│  └──────────────┘ └──────────────┘ └──────────────┘    │
│                                                        │
│         [ PHÊ DUYỆT ]         [ TỪ CHỐI DUYỆT ]        │
└────────────────────────────────────────────────────────┘
```

---

## 4. CÁC THÀNH PHẦN GIAO DIỆN CHÍNH

### 4.1 Khung So Sánh Hình Ảnh Đa Chiều — `.stf-kyc-grid`
* Grid 3 cột trên màn hình rộng, hiển thị 3 khung ảnh: Mặt trước CCCD, Mặt sau CCCD và Ảnh Selfie chân dung của người dùng.
* **Tích hợp zoom ảnh kỹ thuật số (Lightbox):** Nhấp chọn bất kỳ ảnh nào sẽ mở ra cửa sổ phóng to toàn màn hình (Overlay Lightbox) để Staff có thể đọc rõ từng chữ số nhỏ nhất ghi trên CCCD và so sánh khuôn mặt cận cảnh.

### 4.2 Nút Phê Duyệt & Hộp Thoại Từ Chối
* **Nút Phê Duyệt (`.btn-approve`):** Màu xanh lá, click hiển thị modal xác nhận nhanh "Phê duyệt hồ sơ này?".
* **Nút Từ Chối (`.btn-reject`):** Màu đỏ, click hiển thị hộp thoại yêu cầu nhập bắt buộc lý do từ chối (rejection reason) từ Staff trước khi gửi.

---

## 5. LUỒNG XỬ LÝ JS & AJAX

### 5.1 Trang danh sách KYC (`/staff/kyc`):
* Gọi API: `GET /api/v1/staff/kyc?status=Pending&page=0` để chèn dữ liệu vào bảng danh sách. Click một dòng sẽ điều hướng sang `/staff/kyc/{kycId}`.

### 5.2 Trang chi tiết KYC (`/staff/kyc/{kycId}`):
1. **Phê duyệt hồ sơ:**
   * Staff click "Phê duyệt" và xác nhận:
     * **Endpoint:** `POST /api/v1/staff/kyc/approve/{kycId}`
     * **Headers:** `Authorization: Bearer <token>`
   * **Thành công (HTTP 200):** Chuyển hướng quay lại danh sách chờ duyệt `/staff/kyc`, hiển thị Toast báo thành công nâng cấp tài khoản người dùng.
2. **Từ chối hồ sơ:**
   * Staff click "Từ chối", nhập lý do "Ảnh selfie mờ không rõ khuôn mặt" và bấm gửi:
     * **Endpoint:** `POST /api/v1/staff/kyc/reject/{kycId}`
     * **Headers:** `Content-Type: application/json`, `Authorization: Bearer <token>`
     * **Payload:** `{ "reason": "Ảnh selfie mờ không rõ khuôn mặt" }`
   * **Thành công (HTTP 200):** Quay lại trang danh sách, gửi thông báo báo lỗi KYC cho người dùng đích.

---

## 6. RESPONSIVE

* Bố cục thích nghi tốt trên các kích thước màn hình. Trên máy tính bảng hoặc di động, 3 ảnh CCCD chuyển sang dạng dọc xếp lớp để cuộn xem dễ dàng.

---

## 7. ACCESSIBILITY

- Modal hộp thoại nhập lý do từ chối tự động di chuyển tiêu điểm (Focus trap) vào ô input text lý do để hỗ trợ người dùng bàn phím.
- Các ảnh CCCD đều chứa thuộc tính `alt="Ảnh tài liệu định danh người dùng"`.

---

## 8. OUT OF SCOPE

- ❌ Tự động nhận diện OCR số CCCD từ ảnh (Nhân viên Staff bắt buộc kiểm tra bằng mắt thường và tự đối chiếu).
- ❌ Kết nối liên thông thông tin với bên thứ ba (Cục quản lý dân cư).