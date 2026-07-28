# SPEC — Kiểm Duyệt Lệnh Rút Tiền (Staff Withdrawal Review)

> **Feature ID:** `feat-staff` | **Page:** `StaffWithdrawalReview`
> **Route:** `/staff/withdrawals` | **Template:** `templates/staff/withdrawals.html` & `templates/staff/withdrawal-detail.html`
> **CSS Script:** `static/css/customer/style.css`
> **JS Script:** *(None - Sử dụng server-side rendering Thymeleaf và Form POST truyền thống)*

---

## 1. TỔNG QUAN TRANG

Trang kiểm duyệt rút tiền hiển thị danh sách các lệnh xin rút tiền doanh thu đang chờ xử lý của các Seller. Nhân viên vận hành (Staff) hoặc Quản trị viên (Admin) thực hiện đối soát chuyển khoản thủ công bên ngoài hệ thống ngân hàng, sau đó cập nhật lệnh rút thành `Processing`, `Completed` (phê duyệt thành công và tải lên ảnh biên lai chuyển khoản `proofFile`) hoặc `Rejected` (từ chối giải ngân và tự động hoàn trả tiền về ví của Seller).

**Cấu trúc trang:**
1. **Trang danh sách (`/staff/withdrawals`):**
   * Hiển thị bảng danh sách các lệnh rút tiền toàn hệ thống kèm bộ lọc tìm kiếm theo trạng thái, từ khóa (email, tên, mã lệnh), và phân trang (sắp xếp giảm dần theo thời gian tạo `createdAt`).
   * Hiển thị stats số liệu ví: Tổng số lệnh, Đang chờ xử lý, Đã hoàn tất, Bị từ chối.
2. **Trang chi tiết và phê duyệt (`/staff/withdrawals/detail?id={id}`):**
   * Hiển thị thông tin ngân hàng thụ hưởng (Ngân hàng, Số tài khoản, Tên chủ thẻ) của Seller, số tiền rút và phí rút.
   * Form 1: Cập nhật trạng thái lệnh (nút "Tiếp nhận xử lý" chuyển sang `Processing`, nút "Hoàn tất chuyển tiền" chuyển sang `Completed` kèm theo ô tải tệp ảnh minh chứng chuyển khoản `proofFile`).
   * Form 2: Nhập lý do từ chối (textarea `reason`) và nút bấm "Từ chối" để chuyển trạng thái sang `Rejected`.

---

## 2. DESIGN TOKENS ÁP DỤNG

```css
/* Color Palette */
--ds-primary:         #2563eb;
--ds-primary-hover:   #1d4ed8;
--ds-bg:              #f8fafc;
--ds-card:            #ffffff;
--ds-border:          #cbd5e1;
--ds-error:           #ef4444;
--ds-success:         #10b981;
--ds-warning:         #f59e0b;

/* Shape & Spacing */
--ds-radius-md:       8px;
--ds-radius-lg:       12px;
--ds-shadow:          0 1px 3px 0 rgba(0,0,0,0.1);
```

---

## 3. LAYOUT TỔNG THỂ & MOCKUP

### 3.1 Trang Danh Sách (`/staff/withdrawals`)
```
┌────────────────────────────────────────────────────────┐
│ [Logo] MMO Market Staff Console                [Staff] │
│ ────────────────────────────────────────────────────── │
│  DANH SÁCH DUYỆT RÚT TIỀN                              │
│                                                        │
│  [ Tất cả: 12 ] [ Chờ duyệt: 3 ] [ Hoàn tất: 7 ] ...   │
│  [ Nhập từ khóa... ] [ Trạng thái: v ] [ Lọc lệnh ]    │
│  ────────────────────────────────────────────────────  │
│  Mã Lệnh │ Seller               │ Số Tiền     │ Trạng Thái  │
│  #WD-10  │ seller@gmail.com     │ 500.000 đ   │ Chờ duyệt   │
│  #WD-09  │ shopmmo@gmail.com    │ 200.000 đ   │ Hoàn tất    │
│                                                        │
│  [Trang trước]  Trang 1 / 2  [Trang sau]               │
└────────────────────────────────────────────────────────┘
```

### 3.2 Trang Chi Tiết và Xử Lý (`/staff/withdrawals/detail?id=10`)
```
┌────────────────────────────────────────────────────────┐
│ Chi tiết lệnh rút #WD-10                               │
│ ────────────────────────────────────────────────────── │
│ Người yêu cầu: Nguyễn Văn Seller (seller@gmail.com)     │
│ Số tiền rút: 500.000 VNĐ | Phí giao dịch: 7.500 VNĐ   │
│ Thực nhận: 492.500 VNĐ                                 │
│                                                        │
│ Tài khoản thụ hưởng:                                   │
│ - Ngân hàng: VIETCOMBANK                               │
│ - Số tài khoản: 001100123456                           │
│ - Tên chủ thẻ: NGUYEN VAN SELLER                       │
│                                                        │
│ Cập nhật tiến độ:                                      │
│ [ Tiếp nhận xử lý ] (chuyển sang trạng thái Processing)  │
│                                                        │
│ Minh chứng chuyển tiền (bắt buộc khi Hoàn tất):        │
│ [ Chọn tệp ảnh... ] (proofFile)                        │
│ [ Hoàn tất chuyển tiền ]                               │
│ ────────────────────────────────────────────────────── │
│ Lý do từ chối (bắt buộc khi Từ chối):                  │
│ [ Nhập lý do từ chối rút tiền tại đây...           ]   │
│ [ Từ chối yêu cầu rút tiền ]                           │
└────────────────────────────────────────────────────────┘
```

---

## 4. LUỒNG XỬ LÝ (Server-Side Form POST)

1. **Duyệt / Cập Nhật Tiến Độ lệnh rút:**
   * Staff chọn tiếp nhận xử lý (`status=Processing`) hoặc hoàn tất giải ngân (`status=Completed` hoặc `status=Approved`).
   * Gửi Form POST lên:
     * **Endpoint:** `POST /staff/withdrawals/update-status`
     * **Enctype:** `multipart/form-data`
     * **Payload:** 
       * `id`: Long (ID lệnh rút)
       * `status`: String (`Processing`, `Completed`, v.v.)
       * `proofFile`: MultipartFile (Tệp ảnh biên lai chuyển tiền)
     * **Hậu quả:** Hệ thống xử lý thông tin tại `WithdrawalService.updateWithdrawalStatus`. Khi hoàn tất (`Completed`), trạng thái chuyển đổi thành công, ảnh minh chứng được lưu và cập nhật `proof_file` URL, giao dịch ví gốc được set thành `COMPLETED`. Sau khi xử lý xong, controller thực hiện Redirect ngược về trang chi tiết kèm thông báo.

2. **Từ Chối Yêu Cầu Rút Tiền:**
   * Staff nhập lý do và bấm Từ chối.
   * Gửi Form POST lên:
     * **Endpoint:** `POST /staff/withdrawals/reject`
     * **Payload:**
       * `id`: Long (ID lệnh rút)
       * `reason`: String (Lý do từ chối)
     * **Hậu quả:** Hệ thống cập nhật trạng thái lệnh rút thành `Rejected`, hoàn lại toàn bộ số tiền gốc và phí rút về ví Seller thông qua một giao dịch `REFUND` `COMPLETED`. Controller thực hiện Redirect về trang chi tiết và hiển thị thông báo.

---

## 5. RESPONSIVE & ACCESSIBILITY

* **Responsive:** Bảng danh sách rút tiền tự động co giãn theo chiều rộng màn hình. Trên các màn hình di động, hiển thị thông tin ngân hàng thụ hưởng gọn gàng để Staff không bị nhầm lẫn khi thao tác chuyển tiền e-banking trên điện thoại.
* **Accessibility:** Form từ chối kiểm tra lý do bắt buộc (`required`) và validate dung lượng file ảnh upload phía client tránh nộp các tệp tin quá lớn hoặc sai định dạng.