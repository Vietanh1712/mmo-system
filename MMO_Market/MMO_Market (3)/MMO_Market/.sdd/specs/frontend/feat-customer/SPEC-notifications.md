# SPEC — Thông Báo Tài Khoản (Account Notifications for Customer / Seller / Staff / Admin)

> **Feature ID:** `feat-notification` | **Page:** `Account Notifications`
> **Route:** `/account/notifications` | **Template:** `templates/account/notifications.html`
> **CSS Script:** `static/css/features/notifications.css`
> **JS Script:** `static/js/customer/account-notifications.js`
> **Backend ref:** `feat-notification/SPEC.md`

---

## 1. TỔNG QUAN TRANG

Trang Thông Báo Tài Khoản cho phép tất cả các vai trò người dùng sau khi đăng nhập (bao gồm **Customer, Seller, Staff, và Admin**) theo dõi danh sách tất cả các thông báo cá nhân gửi tới tài khoản của họ (KYC, nạp/rút tiền, đơn hàng mua/bán, bảo mật, v.v.) và các tin thông báo chung/broadcast từ Admin/Staff.

**Áp dụng vai trò (Roles):**
*   **Customer / Seller:** Xem thông báo về đơn mua/bán hàng, biến động số dư ví, trạng thái KYC/Shop, và thông báo hệ thống.
*   **Staff / Admin:** Xem thông báo về các nhiệm vụ kiểm duyệt (nếu có sự kiện liên quan trực tiếp đến tài khoản) và thông báo hệ thống.
*   *Lưu ý:* Tất cả các vai trò đều có biểu tượng Chuông (`#headerNotifButton`) trên header và liên kết "Thông báo" trên Sidebar.

**Cấu trúc trang:**
1. **Sidebar điều hướng:** Menu quản lý tài khoản bên trái (lọc hiển thị các tùy chọn theo vai trò của người dùng).
2. **Khối tóm tắt thống kê (Stat Grid):** Hiển thị tổng số thông báo, số thông báo chưa đọc, số lượng thông báo về ví, và thông báo về đơn hàng.
3. **Thanh bộ lọc (Filter Panel):** Hỗ trợ tìm kiếm theo tiêu đề/nội dung và lọc theo loại (Tất cả, Đơn hàng, Ví & giao dịch, KYC & Shop, Bảo mật & hệ thống) và trạng thái (Tất cả, Chưa đọc, Đã đọc).
4. **Danh sách thông báo (Notifications List):** Hiển thị danh sách thông báo phân trang, hiển thị mức độ nghiêm trọng bằng màu sắc (INFO, WARNING, DANGER, SUCCESS).
5. **Nút "Đánh dấu tất cả đã đọc":** Gửi yêu cầu cập nhật nhanh toàn bộ danh sách về trạng thái đã đọc.
6. **Modal Xem Chi Tiết:** Hiển thị nội dung chi tiết của thông báo khi người dùng nhấp chọn.

---

## 2. DESIGN TOKENS ÁP DỤNG

```css
/* Color & Severity Style Rules */
--ds-alert-success-bg:    #ecfdf5;
--ds-alert-success-text:  #065f46;
--ds-alert-warning-bg:    #fffbef;
--ds-alert-warning-text:  #92400e;
--ds-alert-danger-bg:     #fef2f2;
--ds-alert-danger-text:   #991b1b;
--ds-alert-info-bg:       #f0f9ff;
--ds-alert-info-text:     #075985;
```

---

## 3. LAYOUT TỔNG THỂ & MOCKUP

```
┌────────────────────────────────────────────────────────┐
│ [Logo] MMO Market                              [User]  │
│                                                (🔔 Badge)
├────────────────────────────────────────────────────────┤
│  ┌───────────┐  ┌────────────────────────────────────┐ │
│  │  Sidebar  │  │  THÔNG BÁO HỆ THỐNG                │ │
│  │  - Hồ sơ  │  │  [✓ Đánh dấu tất cả đã đọc]        │ │
│  │  - Ví tiền│  │  ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ │ │
│  │  - KYC    │  │  │ Tổng │ │ Chưa │ │ Ví   │ │ Đơn  │ │ │
│  │  - Orders │  │  │  10  │ │  2   │ │  5   │ │  3   │ │ │
│  │ >- T.Báo  │  │  └──────┘ └──────┘ └──────┘ └──────┘ │ │
│  └───────────┘  │  BỘ LỌC                            │ │
│                 │  [ Tìm kiếm... ] [ Loại: Tất cả ▾] │ │
│                 │  ┌──────────────────────────────┐  │ │
│                 │  │ 🔔 Nạp tiền thành công (Đã đọc)│  │ │
│                 │  ├──────────────────────────────┤  │ │
│                 │  │ ⚠️ Tài khoản KYC bị từ chối  │  │ │
│                 │  └──────────────────────────────┘  │ │
│                 └────────────────────────────────────┘ │
└────────────────────────────────────────────────────────┘
```

---

## 4. LUỒNG XỬ LÝ JS & AJAX

1. **Khởi tạo và Lấy danh sách:**
   * Gửi yêu cầu `GET /api/v1/notifications` để lấy danh sách thông báo.
   * Lọc và phân trang danh sách ở client dựa trên từ khóa tìm kiếm, loại thông báo và trạng thái đọc.
   * Tính toán và hiển thị các số liệu thống kê lên các thẻ Stat Grid.
2. **Cập nhật trạng thái đã đọc:**
   * Khi người dùng click vào một thông báo chưa đọc, Modal chi tiết mở lên, đồng thời gửi AJAX:
     * **Endpoint:** `POST /api/v1/notifications/{id}/read`
     * **Response (200 OK):** Trả về thành công, cập nhật trạng thái item thành "Đã đọc", và cập nhật giảm badge chuông ở Header.
3. **Đánh dấu đọc tất cả:**
   * Khi nhấn "Đánh dấu tất cả đã đọc", gửi AJAX:
     * **Endpoint:** `POST /api/v1/notifications/mark-all-read`
     * **Response (200 OK):** Chuyển toàn bộ danh sách hiển thị sang "Đã đọc", đặt badge chuông trên Header về ẩn.
4. **Header Notification Badge:**
   * Trên header của tất cả các trang, hàm `refreshHeaderNotifBadge()` được kích hoạt khi tải trang để lấy danh sách từ `GET /api/v1/notifications`. Đếm số lượng thông báo có `status === 'UNREAD'` để cập nhật số đếm badge đỏ trên icon chuông (`#headerNotifCount`).
