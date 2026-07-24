# UC-19 — Bảng Điều Khiển Ví (Wallet Dashboard)

> **Feature:** `feat-wallet` | **Trạng thái:** Published
> **Tham chiếu FR:** FR-WALL-03, FR-WALL-07

---

## 1. Tổng Quan

| Thuộc tính | Nội dung |
|:---|:---|
| **Mã Use Case** | UC-19 |
| **Tên** | Bảng Điều Khiển Ví (Wallet Dashboard) |
| **Tác nhân chính** | Người dùng (Customer, Seller) |
| **Mô tả ngắn** | Hệ thống cung cấp màn hình tổng quan để người dùng theo dõi số dư ví (`balanceVnd`), các thống kê dòng tiền (Tổng nạp, Tổng chi, Đang xử lý, Escrow), và xem danh sách các giao dịch ví gần đây (phân trang). |
| **Độ ưu tiên** | Cao (P1) |

---

## 2. Tác Nhân & Điều Kiện

### 2.1 Tác Nhân

| Tác nhân | Vai trò |
|:---|:---|
| **Người dùng** | Xem tổng quan số dư tài sản và theo dõi danh sách lịch sử giao dịch ví |

### 2.2 Điều Kiện Tiền Quyết (Preconditions)

- Người dùng đã đăng nhập hệ thống hợp lệ (`@AuthenticationPrincipal`).

### 2.3 Hậu Điều Kiện (Postconditions)

- Hệ thống trả về dữ liệu số dư và danh sách giao dịch không làm thay đổi trạng thái database (Read-only).

---

## 3. Luồng Xử Lý

### 3.1 Luồng Chính — Xem Thống Kê Ví & Lịch Sử Giao Dịch

```
Bước 1  [User]:       Truy cập menu "Ví của tôi" (/wallet) hoặc "Lịch sử giao dịch" (/wallet/transactions).
Bước 2  [Frontend]:   Gọi song song các API lấy dữ liệu thông tin profile, thống kê ví và lịch sử giao dịch.
Bước 3  [Frontend]:   GET /api/v1/profile
Bước 4  [Backend]:    Lấy thông tin User hiện tại gồm số dư balanceVnd và trạng thái kycStatus. Trả về ProfileResponse.
Bước 5  [Frontend]:   GET /api/v1/wallet/stats
Bước 6  [Backend]:    Tính toán:
                       - totalTopup (Tổng nạp thành công - TOPUP & REFUND)
                       - totalSpent (Tổng chi thành công - PAYMENT)
                       - pendingCount (Số lượng giao dịch ở trạng thái PENDING)
                       - escrowAmount (Tổng tiền đang giam giữ - ESCROW)
                       Trả về WalletStatsDto.
Bước 7  [Frontend]:   GET /api/v1/wallet/transactions?page=0&size=5 (đối với dashboard) hoặc size=1000 (đối với trang lịch sử để lọc client-side)
Bước 8  [Backend]:    Lấy danh sách WalletTransaction của user sắp xếp theo thời gian mới nhất (createdAt DESC) và phân trang.
                       Trả về Page<WalletTransactionDto>.
Bước 9  [Frontend]:   Hiển thị số dư khả dụng, các khối Thao tác nhanh (thống kê) và Bảng danh sách giao dịch ví.
```

---

## 4. Tham Chiếu API

| Phương thức | Endpoint | Mô tả |
|:---|:---|:---|
| `GET` | `/api/v1/profile` | Lấy thông tin cá nhân và số dư ví `balanceVnd` hiện tại |
| `GET` | `/api/v1/wallet/stats` | Lấy dữ liệu thống kê dòng tiền (Tổng nạp, chi, pending, escrow) |
| `GET` | `/api/v1/wallet/transactions` | Lấy lịch sử biến động số dư phân trang (sắp xếp giảm dần theo thời gian tạo). Hỗ trợ các query params phân trang: `page`, `size`. |

---

## 5. Tiêu Chí Chấp Nhận (Acceptance Criteria)

### AC-19-01 — Truy xuất thành công
- **Cho trước:** Người dùng đã đăng nhập.
- **Khi:** Truy cập `/wallet`.
- **Thì:** Dữ liệu thống kê (`WalletStatsDto`), thông tin ví (`balanceVnd`) và bảng lịch sử giao dịch (`Page<WalletTransactionDto>`) được tải thành công (HTTP 200). Các giao dịch được sắp xếp theo thời gian mới nhất lên đầu.

### AC-19-02 — Chặn truy cập trái phép
- **Cho trước:** Request không có JWT Token hợp lệ.
- **Khi:** Gọi các API `/api/v1/wallet/stats` hoặc `/api/v1/wallet/transactions`.
- **Thì:** Trả về lỗi 401 Unauthorized.
