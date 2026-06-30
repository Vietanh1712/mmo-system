# TASKS — Orders & Escrow Purchase (`feat-order`)

> **Feature ID:** `feat-order` | **UC Coverage:** UC-08 (Order Purchase & Escrow)
> **Phiên bản:** 1.0 | **Cập nhật:** 2026-06-27

---

## Phase 1: Database & Entities

- [x] **1.1** Định nghĩa bảng `Transactions` lưu thông tin đơn hàng và ngày giải phóng tiền `escrow_release_date`.
- [x] **1.2** JPA Entity `Transaction` — map quan hệ với người mua, người bán, sản phẩm và biến thể.

## Phase 2: Repositories

- [x] **2.1** `TransactionRepository` — truy vấn danh sách giao dịch mua/bán của người dùng.

## Phase 3: DTOs & Validation

- [x] **3.1** `PurchaseRequest` — xác thực tính hợp lệ của ID sản phẩm và số lượng đặt mua.

## Phase 4: Business Logic (Services)

- [x] **4.1** `TransactionService.purchaseProduct()` — kiểm tra tồn kho, trừ tiền khả dụng của người mua và giữ tiền trong tài khoản Escrow trung gian trong 72 giờ.
- [x] **4.2** `TransactionService.releaseEscrow()` — thực hiện chuyển tiền từ tài khoản tạm giữ sang ví khả dụng của Seller khi hết hạn hoặc khi được xác nhận.

## Phase 5: Controllers & Security

- [x] **5.1** `TransactionController` — API mua hàng `/api/transactions/purchase` yêu cầu xác thực người dùng.

## Phase 6: Testing

- [x] **6.1** Unit Test `ProductServiceTest` và `UserServiceTest` — kiểm thử thành công luồng mua hàng hạnh phúc và luồng không đủ tiền khả dụng.