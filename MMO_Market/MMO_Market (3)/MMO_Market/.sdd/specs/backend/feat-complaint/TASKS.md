# TASKS — Complaint Management (`feat-complaint`)

> **Feature ID:** `feat-complaint` | **UC Coverage:** UC-10 (Complaints & Dispute Resolution)
> **Phiên bản:** 2.0 | **Cập nhật:** 2026-07-16

---

## Phase 1: Database & Entities

- [x] **1.1** Tạo bảng `Complaints` lưu trữ thông tin khiếu nại (bằng chứng, giải pháp mong muốn, trạng thái).

## Phase 2: Repositories

- [x] **2.1** `ComplaintRepository` — truy vấn danh sách khiếu nại của khách hàng, shop người bán, và toàn hệ thống hỗ trợ phân trang cho Staff.

## Phase 3: Business Logic (Services)

- [x] **3.1** `ComplaintService.createComplaint()` — ghi nhận khiếu nại, validate bằng chứng bắt buộc, đóng băng tiền giao dịch bằng cách chuyển trạng thái sang `Disputed`.
- [x] **3.2** `ComplaintService.startDispute()` — chuyển đổi trạng thái khiếu nại sang `In_Progress` và tự động gửi tin nhắn đối chất hệ thống.
- [x] **3.3** `ComplaintService.updateComplaintStatus()` — phán quyết khiếu nại (Resolved, Completed, Rejected), tính toán hoàn tiền pro-rata theo tỷ lệ số ngày sử dụng và thực hiện cập nhật ví Buyer/Seller.
- [x] **3.4** Tự động liên kết đánh giá lại cấp độ cửa hàng (`shopLevelService.evaluateSellerLevel`) ngay sau khi phán quyết.

## Phase 4: Controllers & Security

- [x] **4.1** `ComplaintController` — API REST `/api/complaints` kiểm soát các phân quyền và xử lý AJAX khiếu nại, chat đối chất 3 bên (Staff Read-only).
- [x] **4.2** `ProfilePageController` & `StaffController` — MVC Pages cung cấp giao diện quản lý khiếu nại của Buyer và phòng đối chất tranh chấp của Staff.