# TASKS — Pre-Orders (`feat-preorder`)

> **Feature ID:** `feat-preorder` | **UC Coverage:** UC-16 (Pre-order)
> **Phiên bản:** 1.0 | **Cập nhật:** 2026-06-27

---

## Phase 1: Database & Entities

- [x] **1.1** Tạo bảng `PreOrders` lưu trữ các đơn đặt trước sản phẩm.

## Phase 2: Repositories

- [x] **2.1** `PreOrderRepository` — truy vấn các đơn hàng đặt trước của khách hàng.

## Phase 4: Business Logic (Services)

- [x] **4.1** `PreOrderService.createPreOrder()` — ghi nhận yêu cầu đặt trước sản phẩm từ người dùng khi sản phẩm hết hàng.

## Phase 5: Controllers & Security

- [x] **5.1** `PreOrderController` — API `/api/v1/pre-orders` yêu cầu vai trò `Customer`.