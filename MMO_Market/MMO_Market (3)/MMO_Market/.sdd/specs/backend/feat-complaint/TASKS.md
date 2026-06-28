# TASKS — Complaint Management (`feat-complaint`)

> **Feature ID:** `feat-complaint` | **UC Coverage:** UC-10 (Complaints)
> **Phiên bản:** 1.0 | **Cập nhật:** 2026-06-27

---

## Phase 1: Database & Entities

- [x] **1.1** Database schema tạo bảng `Complaints` lưu thông tin tranh chấp.

## Phase 2: Repositories

- [x] **2.1** `ComplaintRepository` — tìm kiếm khiếu nại theo đơn hàng và người dùng.

## Phase 4: Business Logic (Services)

- [x] **4.1** `ComplaintService` — đóng băng tiền giao dịch khi xảy ra khiếu nại, ngăn chặn việc tự động giải phóng tiền cho Seller sau 72h.

## Phase 5: Controllers & Security

- [x] **5.1** `ComplaintController` — API gửi khiếu nại `/api/complaints`.