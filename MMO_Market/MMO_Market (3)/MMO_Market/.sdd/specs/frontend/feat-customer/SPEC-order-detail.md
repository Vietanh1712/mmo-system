# SPEC — Chi Tiết Đơn Hàng & Giải Mã Tài Sản Số (Customer Order Detail)

> **Feature ID:** `feat-order` | **Page:** `OrderDetail`
> **Route:** `/account/orders/{orderId}` | **Template:** `templates/account/order-detail.html`
> **CSS Script:** `static/css/customer/style.css`
> **JS Script:** `static/js/account-order-detail.js`
> **Version:** 1.0 | **Status:** Draft
> **Backend ref:** `feat-order/UC-08-order-checkout.md`

---

## 1. TỔNG QUAN TRANG

Trang Chi tiết đơn hàng hiển thị chi tiết hóa đơn thanh toán của một đơn hàng cụ thể, bao gồm thông tin thanh toán tài chính, thông tin liên hệ của Shop người bán, thời gian đếm ngược giải phóng quỹ Escrow 72h và vùng bàn giao tài sản kỹ thuật số (tài khoản mật khẩu, giftcode) được tích hợp tính năng giải mã bảo mật và sao chép nhanh.

**Cấu trúc trang:**
1. **Sidebar điều hướng:** Menu quản lý tài khoản bên trái.
2. **Khối chi tiết đơn hàng (Order Info Section):** Tên sản phẩm, đơn giá, số lượng, tổng tiền thanh toán, phương thức thanh toán, mã giao dịch và trạng thái.
3. **Khối thông tin Shop người bán:** Tên Shop, nút chat trực tiếp, nút chuyển sang gian hàng của Shop.
4. **Vùng bàn giao sản phẩm số (Digital Asset Delivery Card):** Danh sách các mã code hoặc thông tin tài khoản được che bằng dấu hoa thị và các nút thao tác hiển thị/sao chép.

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
--ds-text-code:       #0f172a;
--ds-bg-code:         #f1f5f9;

/* Shape & Spacing */
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
│  │  Sidebar  │  │  CHI TIẾT ĐƠN HÀNG #OR8572         │ │
│  │  - Hồ sơ  │  │  Trạng thái: TẠM GIỮ ESCROW        │ │
│  │  - Ví tiền│  │  Sản phẩm: Netflix Premium (1 tháng)│ │
│  │  - KYC    │  │  Tổng tiền: 50.000 VNĐ             │ │
│  │  - Orders │  ├────────────────────────────────────┤ │
│  └───────────┘  │  MÃ SẢN PHẨM BÀN GIAO:             │ │
│                 │  ┌──────────────────────────────┐  │ │
│                 │  │ ****************  [Hiện] [Copy]│  │ │
│                 │  └──────────────────────────────┘  │ │
│                 └────────────────────────────────────┘ │
└────────────────────────────────────────────────────────┘
```

---

## 4. CÁC THÀNH PHẦN GIAO DIỆN CHÍNH

### 4.1 Khối Hiển Thị Tài Sản Kỹ Thuật Số — `.asset-delivery-box`
* Nền xám nhạt `var(--ds-bg-code)`, viền bo góc tròn `var(--ds-radius-md)`. Chữ đơn cách (Monospace) dễ đọc.
* **Mặt nạ che thông tin:** Mặc định, nội dung mã thẻ sẽ bị che đi bằng chuỗi ký tự dấu hoa thị `●●●●●●●●●●●●` để chống rò rỉ khi người dùng mở trang nơi công cộng.
* **Nút Toggle Hiển thị (`.btn-toggle-view`):** Icon hình con mắt. Khi click, đổi hiển thị sang chuỗi text thô đã giải mã.
* **Nút Sao chép nhanh (`.btn-copy`):** Sao chép trực tiếp chuỗi text thô vào clipboard của người dùng mà không cần bôi đen chọn.

### 4.2 Khối Bộ Đếm Ngược Escrow — `.escrow-countdown-timer`
* Hiển thị cảnh báo trực quan: "Số tiền giao dịch đang được hệ thống tạm giữ an toàn. Tiền sẽ tự động chuyển cho người bán sau {Giờ}:{Phút}:{Giây}." nếu đơn hàng ở trạng thái `Escrow`.

---

## 5. LUỒNG XỬ LÝ JS & AJAX

1. **Khởi tạo thông tin chi tiết đơn hàng:**
   * Trình duyệt thực hiện gửi request lấy thông tin đơn hàng:
     * **Endpoint:** `GET /api/v1/orders/{orderId}`
     * **Headers:** `Authorization: Bearer <token>`
   * **Thành công (HTTP 200):** Kết xuất toàn bộ thông tin tài chính và thông tin Shop lên UI.
2. **Giải mã hiển thị tài sản số:**
   * Khi người dùng click nút "Hiển thị" (hoặc con mắt):
     * Gửi request lấy dữ liệu tài sản giải mã:
       * **Endpoint:** `GET /api/v1/orders/{orderId}/assets`
       * **Headers:** `Authorization: Bearer <token>`
     * **Thành công (HTTP 200):** Trả về mảng JSON chứa các chuỗi tài sản đã giải mã (đã giải mã AES ở backend).
     * JS lưu dữ liệu thô vào bộ nhớ tạm thời của script (State variable), cập nhật UI thay thế dấu hoa thị bằng text thô, đổi icon mắt thành mắt gạch chéo.
3. **Sao chép nội dung:**
   * Người dùng click nút "Sao chép":
     * JS sử dụng `navigator.clipboard.writeText(decryptedText)` để đưa mã vào bộ nhớ máy, hiển thị tooltip báo "Đã sao chép!" trong 1.5 giây.

---

## 6. RESPONSIVE

* Bố cục thích nghi tốt trên mọi kích thước màn hình. Khối bàn giao mã thẻ `.asset-delivery-box` sử dụng thuộc tính `word-break: break-all` để ngăn các đoạn mã dài làm tràn vỡ layout trên thiết bị di động có bề ngang hẹp.

---

## 7. ACCESSIBILITY

- Sử dụng `aria-hidden="true"` cho các chuỗi ký tự dấu hoa thị khi thông tin đang bị che.
- Các nút sao chép và hiển thị có nhãn mô tả `aria-label` tương ứng.

---

## 8. OUT OF SCOPE

- ❌ Gửi lại mã thẻ qua Email của khách hàng (chỉ hiển thị xem trực tiếp bảo mật trên trình duyệt).