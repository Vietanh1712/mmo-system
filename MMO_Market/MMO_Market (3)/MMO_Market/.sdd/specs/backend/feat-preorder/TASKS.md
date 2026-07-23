# TASKS — Pre-Orders (`feat-preorder`)

> **Feature ID:** `feat-preorder` | **UC Coverage:** UC-16 (Pre-order)
> **Phiên bản:** 1.0 | **Cập nhật:** 2026-07-16

---

## Phase 1: Database & Entities

- [x] **1.1** Tạo bảng `PreOrders` lưu trữ các đơn đặt trước sản phẩm trong CSDL SQL Server.

## Phase 2: Repositories

- [x] **2.1** `PreOrderRepository` — cung cấp các phương thức truy vấn danh sách đặt trước theo tài khoản khách hàng, sắp xếp theo thời gian khởi tạo giảm dần.

## Phase 3: Business Logic (Services)

- [x] **3.1** `PreOrderService.createPreOrder()` — xử lý nghiệp vụ tạo mới đơn đặt hàng trước của khách hàng cho sản phẩm đang hết hàng.
- [x] **3.2** `PreOrderService.getPreOrdersByCustomer()` — lấy ra lịch sử các yêu cầu đặt mua trước của Buyer đã đăng nhập.

## Phase 4: Controllers & Security

- [x] **4.1** `PreOrderController` — API REST `/api/v1/pre-orders` để gửi và xem danh sách pre-orders qua AJAX.
- [x] **4.2** `PreOrderPageController` — MVC Controller điều hướng và render giao diện đặt trước `/pre-orders/new` và danh sách đơn đặt trước `/pre-orders`.