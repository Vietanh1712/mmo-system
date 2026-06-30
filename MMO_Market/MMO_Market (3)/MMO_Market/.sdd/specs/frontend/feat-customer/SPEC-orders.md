# SPEC — Lịch Sử Đơn Hàng Đã Mua (Customer Order History)

> **Feature ID:** `feat-order` | **Page:** `OrderHistory`
> **Route:** `/account/orders` | **Template:** `templates/account/orders.html`
> **CSS Script:** `static/css/customer/style.css`
> **JS Script:** `static/js/account-orders.js`
> **Version:** 1.0 | **Status:** Draft
> **Backend ref:** `feat-order/UC-08-order-checkout.md`

---

## 1. TỔNG QUAN TRANG

Trang quản lý đơn hàng cho phép người mua (Customer) xem lại toàn bộ các sản phẩm số đã mua, hiển thị mã thẻ/tài sản số, tạo khiếu nại đóng băng giao dịch và thực hiện bấm xác nhận nhận hàng sớm để giải phóng tiền ký quỹ Escrow cho người bán.

**Cấu trúc trang:**
1. **Sidebar điều hướng:** Menu quản lý tài khoản bên trái.
2. **Order List (Thành phần chính):** Danh sách các thẻ đơn hàng (Order Cards) phân trang.
3. **Modal xem tài sản số:** Hộp thoại hiển thị danh sách các mã thẻ, tài khoản mật khẩu đã mua.
4. **Modal xác nhận nhận hàng:** Hộp thoại cảnh báo rủi ro mất quyền khiếu nại trước khi giải phóng tiền.

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

/* Shape & Shadows */
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
│  │  Sidebar  │  │  LỊCH SỬ ĐƠN HÀNG ĐÃ MUA           │ │
│  │  - Hồ sơ  │  │                                    │ │
│  │  - Ví tiền│  │  ┌──────────────────────────────┐  │ │
│  │  - KYC    │  │  │ Đơn #OR8572 - Netflix Premium│  │ │
│  │  - Orders │  │  │ 50.000 VNĐ | Trạng thái: ESCROW│  │ │
│  └───────────┘  │  │ [Xem Mã Thẻ] [Đã Nhận] [KhiếuNại]│  │ │
│                 │  └──────────────────────────────┘  │ │
│                 └────────────────────────────────────┘ │
└────────────────────────────────────────────────────────┘
```

---

## 4. CÁC THÀNH PHẦN GIAO DIỆN CHÍNH

### 4.1 Thẻ Đơn Hàng — `.order-item-card`
* Hiển thị: Mã đơn hàng dạng `#ORxxxx`, Tên sản phẩm, Tên phân loại biến thể, Giá tiền, Ngày đặt hàng, Tên Shop người bán và trạng thái hiện tại (`Escrow` - Đang giam tiền, `Completed` - Thành công, `Complaint` - Tranh chấp).
* **Nhóm nút chức năng:**
  * *Nút Xem mã thẻ (`.btn-view-asset`):* Chỉ hiển thị khi đơn hàng đã thanh toán thành công.
  * *Nút Đã nhận hàng (`.btn-confirm-received`):* Chỉ hiển thị khi đơn hàng ở trạng thái `Escrow`.
  * *Nút Khiếu nại (`.btn-dispute`):* Chỉ hiển thị khi đơn hàng ở trạng thái `Escrow`. Click sẽ dẫn sang trang tạo khiếu nại.

### 4.2 Modal Hiển Thị Mã Thẻ — `.order-asset-modal`
* Vùng chứa danh sách tài sản số bàn giao. Mỗi dòng chứa 1 mã thẻ dạng copy-paste được, có nút icon **Sao chép (Copy)** nhanh.

---

## 5. LUỒNG XỬ LÝ JS & AJAX

1. **Tải lịch sử đơn hàng:**
   * Gọi API: `GET /api/v1/orders/my-orders?page=0&size=10`
   * Kết xuất dữ liệu lên danh sách thẻ.
2. **Xem danh sách mã thẻ đã mua:**
   * Khi click "Xem mã thẻ số":
     * Gọi API: `GET /api/v1/orders/{orderId}/assets`
     * **Thành công (HTTP 200):** Nhận danh sách các tài sản đã giải mã (AES giải mã đầu backend). Hiển thị modal `.order-asset-modal` chứa danh sách mã thẻ để người dùng copy.
3. **Xác nhận đã nhận hàng sớm (Giải phóng Escrow):**
   * Khi click "Xác nhận đã nhận":
     * Hiển thị cảnh báo: "Bằng việc bấm xác nhận, tiền sẽ lập tức được chuyển cho người bán. Bạn sẽ không thể khiếu nại đơn hàng này."
     * Người dùng nhấn "Xác nhận đồng ý" -> Gửi request:
       * **Endpoint:** `POST /api/v1/orders/{orderId}/confirm-received`
       * **Headers:** `Authorization: Bearer <token>`
     * **Thành công (HTTP 200):** Đóng modal, cập nhật trạng thái đơn hàng trên UI thành `Completed`, ẩn 2 nút "Đã nhận hàng" và "Khiếu nại", kích hoạt Toast báo thành công.

---

## 6. RESPONSIVE

* **Viewport ≥ 768px:** Các nút chức năng đặt nằm ngang cạnh nhau bên phải thông tin đơn hàng.
* **Viewport < 768px:** Thẻ đơn hàng co hẹp, các nút chức năng xếp dọc chiếm 100% chiều ngang thẻ để dễ chạm bấm trên màn hình cảm ứng di động.

---

## 7. ACCESSIBILITY

- Sử dụng cảnh báo modal có nút tập trung mặc định vào hành động hủy bỏ để phòng tránh thao tác nhầm lẫn của người dùng.
- Thêm thuộc tính `aria-describedby` liên kết mã đơn hàng với thông báo lỗi nảy sinh.

---

## 8. OUT OF SCOPE

- ❌ Gửi trả hàng và tự động hoàn tiền trực tiếp từ giao diện lịch sử (bắt buộc phải qua quy trình giải quyết khiếu nại đối chất).
- ❌ Xuất hóa đơn đỏ VAT cho đơn hàng số đã mua.