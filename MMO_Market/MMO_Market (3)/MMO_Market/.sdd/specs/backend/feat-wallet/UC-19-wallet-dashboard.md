# UC-19 — Bảng Điều Khiển Ví (Wallet Dashboard)

> **Feature:** `feat-wallet` | **Phiên bản:** 1.0 | **Trạng thái:** Published
> **Tham chiếu FR:** FR-WALL-04
> **Cập nhật:** 2026-07-16

---

## 1. Tổng Quan

| Thuộc tính | Nội dung |
|:---|:---|
| **Mã Use Case** | UC-19 |
| **Tên** | Bảng Điều Khiển Ví (Wallet Dashboard) |
| **Tác nhân chính** | Người dùng (Customer, Seller) |
| **Mô tả ngắn** | Hệ thống cung cấp màn hình tổng quan để người dùng theo dõi số dư khả dụng, các thống kê dòng tiền (Tổng nạp, Tổng chi, Đang xử lý, Escrow), và xem lịch sử giao dịch (dòng tiền ra/vào). |
| **Độ ưu tiên** | Cao (P1) |

---

## 2. Tác Nhân & Điều Kiện

### 2.1 Tác Nhân

| Tác nhân | Vai trò |
|:---|:---|
| **Người dùng** | Xem tổng quan tài sản và theo dõi các giao dịch đã thực hiện |

### 2.2 Điều Kiện Tiền Quyết (Preconditions)

- Người dùng đã đăng nhập hệ thống hợp lệ (`@AuthenticationPrincipal`).

### 2.3 Hậu Điều Kiện (Postconditions)

- Hệ thống trả về dữ liệu số dư và danh sách giao dịch không làm thay đổi trạng thái database (Read-only).

---

## 3. Luồng Xử Lý

### 3.1 Luồng Chính — Xem Thống Kê Ví & Lịch Sử Giao Dịch

```
Bước 1  [User]:       Truy cập menu "Ví của tôi" (/wallet) hoặc "Lịch sử giao dịch" (/wallet/transactions).
Bước 2  [Frontend]:   Gọi song song 2 API lấy dữ liệu thống kê và lịch sử.
Bước 3  [Frontend]:   GET /api/v1/wallet/stats
Bước 4  [Backend]:    Tính toán `totalTopup`, `totalSpent`, `pendingCount`, `escrowAmount`.
                       Trả về WalletStatsDto.
Bước 5  [Frontend]:   GET /api/v1/wallet/transactions?page=0&size=10&keyword=&type=&status=&fromDate=&toDate=
Bước 6  [Backend]:    Lấy danh sách WalletTransaction của user, kết hợp bộ lọc (tìm kiếm, loại, trạng thái, thời gian) và phân trang.
                       Trả về Page<WalletTransactionDto>.
Bước 7  [Frontend]:   Hiển thị số dư khả dụng, các khối Thao tác nhanh (thống kê) và Bảng danh sách giao dịch gần đây.
```

---

## 4. Tham Chiếu API

| Phương thức | Endpoint | Mô tả |
|:---|:---|:---|
| `GET` | `/api/v1/wallet/stats` | Lấy dữ liệu thống kê (Tổng nạp, chi, pending, escrow) |
| `GET` | `/api/v1/wallet/transactions` | Lấy lịch sử biến động số dư (phân trang). **Cần bổ sung các tham số query filter**: `keyword` (mã GD/mô tả), `type` (loại GD), `status` (trạng thái), `fromDate`, `toDate`. |

---

## 5. Tiêu Chí Chấp Nhận (Acceptance Criteria)

### AC-19-01 — Truy xuất thành công
- **Cho trước:** Người dùng đã đăng nhập.
- **Khi:** Truy cập `/wallet`.
- **Thì:** Dữ liệu thống kê (`WalletStatsDto`) và bảng lịch sử giao dịch (`Page<WalletTransactionDto>`) được tải thành công (HTTP 200). Các giao dịch được sắp xếp theo thời gian mới nhất lên đầu.

### AC-19-02 — Chặn truy cập trái phép
- **Cho trước:** Request không có JWT Token.
- **Khi:** Gọi API `/api/v1/wallet/stats`.
- **Thì:** Trả về lỗi 401 Unauthorized.
