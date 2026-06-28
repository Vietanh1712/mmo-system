# SPEC — Customer Order History & Escrow Confirm (`/account/orders`)
> **Feature ID:** `feat-customer` | **Page:** `Orders`
> **Route:** `/account/orders` | **Template:** `templates/account/orders.html`
> **JS Script:** `static/js/account-orders.js` | **Prefix:** `cord-`

---

## 1. MÔ TẢ TRANG
Quản lý lịch sử toàn bộ các đơn hàng đã mua của khách hàng. Điểm mấu chốt là điều khiển trạng thái quỹ tạm giữ **Escrow**.

---

## 2. MOCKUP GIAO DIỆN (ASCII)
```
┌──────────────────────────────────────────────────────────────────┐
│  MMO Market > Lịch sử đơn hàng đã mua                            │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Danh sách đơn hàng đã mua                                       │
│  ──────────────────────────────────────────────────────────────  │
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │ Đơn hàng #OR8572 - Netflix Premium 1 tháng                  │  │
│  │ Số tiền: 50.000đ | Người bán: ShopNetflix                    │  │
│  │ Trạng thái: Tạm Giữ Escrow (Giải phóng trong 48 giờ)       │  │
│  │                                                            │  │
│  │   [ Xem mã thẻ số ]   [ Xác nhận đã nhận ]   [ Gửi khiếu nại ]│  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

---

## 3. LUỒNG XỬ LÝ & GỌI API (FLOW)

### 3.1 Nhận mã sản phẩm số
1. Khi đơn hàng ở trạng thái thanh toán thành công, click "Xem mã thẻ số".
2. JS gửi yêu cầu giải mã lên `/api/v1/orders/{id}/decrypt-code` đính kèm token.
3. Giải mã và hiển thị lên modal dạng popup cho phép khách hàng Copy.

### 3.2 Xác nhận đơn hàng sớm (Release Escrow)
1. Khách hàng click "Xác nhận đã nhận".
2. Hiển thị modal cảnh báo: "Bằng việc bấm xác nhận, tiền sẽ lập tức được chuyển cho người bán. Bạn sẽ không thể khiếu nại đơn hàng này."
3. Click "Đồng ý" -> Gửi `POST /api/v1/orders/{id}/release-escrow`.
4. Đơn hàng chuyển sang `Completed`. Tiền được giải phóng từ ví đóng băng sang ví khả dụng của Seller ở backend.

---

## 4. FUNCTIONAL REQUIREMENTS (EARS)
| ID | EARS Requirement |
|---|---|
| FR-ORD-FE-01 | WHEN a Customer clicks "Xác nhận đã nhận", THE SYSTEM SHALL show a confirmation alert warning about losing dispute rights. |
| FR-ORD-FE-02 | WHEN the release escrow call fails, THE SYSTEM SHALL display the error message via the global toast notification system. |