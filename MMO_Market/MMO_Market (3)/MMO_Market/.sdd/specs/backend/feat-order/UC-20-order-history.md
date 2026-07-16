# UC-20 — Lịch Sử Đơn Hàng Đã Mua (Customer Order History)

> **Feature:** `feat-order` | **Phiên bản:** 1.0 | **Trạng thái:** Published
> **Tham chiếu FR:** FR-ORD-05
> **Cập nhật:** 2026-07-16

---

## 1. Tổng Quan

| Thuộc tính | Nội dung |
|:---|:---|
| **Mã Use Case** | UC-20 |
| **Tên** | Quản lý Lịch Sử Đơn Hàng (Customer) |
| **Tác nhân chính** | Người mua (Customer) |
| **Mô tả ngắn** | Cung cấp giao diện để người mua xem danh sách toàn bộ các sản phẩm đã mua, theo dõi trạng thái thanh toán và tiến trình giải phóng tiền (Escrow). Hỗ trợ xem chi tiết tài sản số hoặc báo cáo tranh chấp. |
| **Độ ưu tiên** | Cao (P1) |

---

## 2. Tác Nhân & Điều Kiện

### 2.1 Tác Nhân

| Tác nhân | Vai trò |
|:---|:---|
| **Người mua** | Xem danh sách, bộ lọc, xem chi tiết và trạng thái của các đơn hàng. |

### 2.2 Điều Kiện Tiền Quyết (Preconditions)

- Người dùng đã đăng nhập hệ thống (`@AuthenticationPrincipal`).

### 2.3 Hậu Điều Kiện (Postconditions)

- Danh sách các đơn hàng (Transactions) được trả về mà không làm thay đổi trạng thái database.

---

## 3. Luồng Xử Lý

### 3.1 Luồng Chính — Xem Danh Sách Đơn Hàng

```
Bước 1  [User]:       Truy cập menu "Đơn hàng của tôi" (/account/orders).
Bước 2  [Frontend]:   Gọi API lấy danh sách toàn bộ đơn hàng của người dùng.
Bước 3  [Frontend]:   GET /api/transactions/me
Bước 4  [Backend]:    Lấy toàn bộ Transaction thuộc về userId. Mapping sang `OrderDto` với các thông tin:
                       - `orderCode`
                       - `productName`, `variantLabel`
                       - `amount`
                       - Trạng thái `status` (Tạm giữ, Hoàn tất, Tranh chấp...)
                       - Trạng thái thanh toán `paymentStatus` (Đã thanh toán)
                       - Ngày mua `createdAt`
Bước 5  [Backend]:    Trả về danh sách List<OrderDto>.
Bước 6  [Frontend]:   Sử dụng mảng dữ liệu để tự động tính toán 4 chỉ số thống kê (Tổng đơn, Hoàn tất, Đang xử lý, Tranh chấp).
Bước 7  [Frontend]:   Kết xuất dữ liệu thành Bảng phân trang và áp dụng tính năng tìm kiếm / bộ lọc cục bộ (Local Filter).
```

---

## 4. Tham Chiếu API

| Phương thức | Endpoint | Mô tả |
|:---|:---|:---|
| `GET` | `/api/transactions/me` | Lấy danh sách toàn bộ các đơn hàng đã mua (Không phân trang ở Backend, Frontend tự phân trang và filter) |
| `GET` | `/api/transactions/{id}` | Lấy chi tiết đơn hàng kèm tài sản số giải mã (Asset/Credentials) |

---

## 5. Tiêu Chí Chấp Nhận (Acceptance Criteria)

### AC-20-01 — Trả về đúng dữ liệu DTO
- **Cho trước:** Người dùng đã mua 2 đơn hàng.
- **Khi:** Truy cập `/account/orders`.
- **Thì:** Dữ liệu JSON trả về phải bao gồm 2 đối tượng `OrderDto`, trong đó `productName`, `amount`, `status`, `paymentStatus` không được rỗng (null).

### AC-20-02 — Ẩn tài sản số ở màn hình danh sách
- **Cho trước:** Dữ liệu danh sách đơn hàng được tải về.
- **Khi:** Kiểm tra API Response `/api/transactions/me`.
- **Thì:** Dữ liệu tài khoản/mật khẩu không được phơi bày trực tiếp ở API này (Chỉ gọi chi tiết mới giải mã).
