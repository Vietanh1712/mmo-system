# SPEC — Đánh Giá Sản Phẩm (Customer Leave Feedback)

> **Feature ID:** `feat-review` | **Page:** `LeaveFeedback`
> **Route:** `/account/orders/{orderCode}/feedback` | **Template:** `templates/account/leave-feedback.html`
> **CSS Script:** `static/css/features/leave-feedback.css`
> **JS Script:** Tích hợp trực tiếp thẻ `<script>` trong file template.
> **Version:** 1.0 | **Status:** Active
> **Backend ref:** `feat-review/UC-17-product-review.md`

---

## 1. TỔNG QUAN TRANG

Trang Đánh giá sản phẩm hiển thị giao diện cho phép khách hàng (Customer) gửi đánh giá, phản hồi cho một đơn hàng đã mua thành công. Khách hàng có thể chọn số sao từ 1 đến 5, viết nhận xét chia sẻ trải nghiệm, tải lên hình ảnh/video bằng chứng, và tùy chọn đăng ẩn danh để bảo mật danh tính.

**Cấu trúc trang:**
1. **Thanh điều hướng phụ (Breadcrumb):** Trang chủ > Lịch sử mua hàng > Đơn hàng #MÃ_ĐƠN > Đánh giá sản phẩm.
2. **Card thông tin sản phẩm (Product Info Card):** Ảnh đại diện, tên sản phẩm và tên người bán (Shop).
3. **Card điểm số đánh giá (Rating Card):** Giao diện tương tác chọn số sao từ 1 đến 5.
4. **Card nội dung đánh giá (Experience Card):** Ô nhập nhận xét (tối đa 1000 ký tự) và vùng tải ảnh/video minh họa (kéo thả hoặc chọn file).
5. **Card tùy chọn ẩn danh (Anonymous Toggle Card):** Nút toggle bật/tắt chế độ ẩn danh khi đăng đánh giá.
6. **Thanh nút bấm hành động (Action Buttons):** Nút "Hủy bỏ" và "Gửi đánh giá".
7. **Hộp thoại thành công (Success Modal):** Popup thông báo gửi đánh giá thành công và nút quay về danh sách đơn hàng.

---

## 2. DESIGN TOKENS ÁP DỤNG

```css
/* Color System */
--ds-primary:         #fd761a;      /* Cam thương hiệu chính */
--ds-primary-hover:   #ea580c;
--ds-bg:              #f8fafc;
--ds-card:            #ffffff;
--ds-border:          #cbd5e1;
--ds-star-active:     #fbbf24;      /* Màu vàng cho sao đã chọn */
--ds-star-inactive:   #cbd5e1;      /* Màu xám cho sao chưa chọn */

/* Shapes & Shadow */
--ds-radius-md:       8px;
--ds-radius-lg:       12px;
--ds-shadow-sm:       0 1px 2px 0 rgba(0,0,0,0.05);
```

---

## 3. CÁC THÀNH PHẦN GIAO DIỆN CHÍNH

### 3.1 Khối Đánh Giá Sao — `.rating-stars`
* Sử dụng 5 icon ngôi sao Font-Awesome (`.rating-star`).
* Hỗ trợ thuộc tính ARIA: `role="radio"`, `aria-checked="false"`, `tabindex="0"`.
* **Trạng thái tương tác:**
  * Hover/Click: Tô màu vàng `var(--ds-star-active)` cho ngôi sao được chọn và các ngôi sao phía trước nó.
  * Hiển thị text mô tả tương ứng điểm số (ví dụ: `Tuyệt vời`, `Tốt`, `Bình thường`, `Kém`, `Rất kém`).

### 3.2 Vùng Tải Tệp Đính Kèm — `.upload-area`
* Nhận diện kéo thả tập tin (`dragover`, `dragleave`, `drop`).
* Hạn chế dung lượng file tối đa: Hình ảnh 10MB, Video 50MB.
* Kết xuất danh sách preview ảnh/video đã chọn kèm nút xóa nhanh cho từng tệp.

### 3.3 Nút Toggle Ẩn Danh — `#anonymousToggle`
* Sử dụng thiết kế Toggle Switch chuẩn hóa của hệ thống.
* Khi ở trạng thái hoạt động: đổi màu nền sang màu cam thương hiệu và thay đổi thuộc tính `aria-pressed="true"`.

---

## 4. LUỒNG XỬ LÝ JS & AJAX

### 4.1 Khởi tạo thông tin đơn hàng
* Trích xuất `orderCode` từ URL.
* Thực hiện gọi API lấy thông tin chi tiết đơn hàng:
  * **Endpoint:** `GET /api/v1/orders/detail/{orderCode}` (hoặc fallback `GET /api/v1/orders/{orderCode}`)
  * **Headers:** `Authorization: Bearer <Access_Token>`
* Hiển thị tên sản phẩm, thumbnail và tên seller tương ứng lên các card thông tin.

### 4.2 Gửi đánh giá lên hệ thống
* Khi click nút **"Gửi đánh giá"**, JS thực hiện các bước kiểm tra đầu vào:
  * Điểm số đánh giá `selectedScore` bắt buộc phải $> 0$.
* Gọi API lưu đánh giá:
  * **Endpoint:** `POST /api/products/{productId}/reviews`
  * **Request Body (JSON):**
    ```json
    {
      "rating": 5,
      "comment": "Nội dung nhận xét...",
      "mediaUrl": "https://mmo-market.s3.amazonaws.com/evidence.jpg",
      "transactionId": 42
    }
    ```
* **Thành công (HTTP 200):** Hiển thị Success Modal, cho phép người dùng click để quay lại trang lịch sử đơn hàng `/account/orders`.
