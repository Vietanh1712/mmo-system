# SPEC — Kiểm Duyệt Lệnh Rút Tiền (Staff Withdrawal Review)

> **Feature ID:** `feat-staff` | **Page:** `StaffWithdrawalReview`
> **Route:** `/staff/withdrawals` | **Template:** `templates/staff/withdrawals.html`
> **CSS Script:** `static/css/customer/style.css`
> **JS Script:** `static/js/staff-withdrawals.js`
> **Version:** 1.0 | **Status:** Draft
> **Backend ref:** `feat-staff/UC-14-staff-operations.md`

---

## 1. TỔNG QUAN TRANG

Trang kiểm duyệt rút tiền hiển thị danh sách các lệnh xin rút tiền doanh thu đang chờ xử lý của các Seller. Nhân viên vận hành (Staff) thực hiện đối soát thủ công bên ngoài hệ thống ngân hàng, sau đó đánh dấu lệnh rút là thành công (giải phóng tiền đóng băng) hoặc từ chối (hoàn tiền về ví khả dụng cho Seller).

**Cấu trúc trang:**
1. **Sidebar điều hướng Console Admin/Staff:** Quản lý User, Duyệt KYC, Duyệt rút tiền, Tranh chấp khiếu nại.
2. **Khối bảng yêu cầu rút tiền (Withdrawal Table):**
   * Hiển thị danh sách các lệnh rút tiền đang ở trạng thái `Pending` (xếp theo thời gian FIFO để xử lý tuần tự).
   * Cột hiển thị: Thông tin Seller, Số tiền rút, Phí giao dịch, Ngân hàng, Số tài khoản, Tên chủ thẻ thụ hưởng và Nhóm nút xử lý.
3. **Modal nhập lý do từ chối:** Hộp thoại chèn lý do khi click từ chối lệnh rút tiền.

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

```
┌────────────────────────────────────────────────────────┐
│ [Logo] MMO Market Staff Console                [Staff] │
├────────────────────────────────────────────────────────┤
│  DANH SÁCH DUYỆT RÚT TIỀN                              │
│  ────────────────────────────────────────────────────  │
│                                                        │
│  ┌──────────────────────────────────────────────────┐  │
│  │ Lệnh #W752 - shopnetflix@gmail.com               │  │
│  │ Số tiền: 1.500.000 VNĐ | Phí rút: 22.500 VNĐ      │  │
│  │ Ngân hàng: Vietcombank - 001100123456 - NG VAN A  │  │
│  │                                                  │  │
│  │         [ PHÊ DUYỆT ]         [ TỪ CHỐI DUYỆT ]  │  │
│  └──────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────┘
```

---

## 4. CÁC THÀNH PHẦN GIAO DIỆN CHÍNH

### 4.1 Thẻ Dòng Yêu Cầu Rút — `.stf-wdr-card`
* Mỗi yêu cầu rút tiền hiển thị thành một panel riêng biệt hoặc một hàng bảng chuẩn chỉ.
* Thông tin ngân hàng thụ hưởng hiển thị to rõ để tránh lỗi nhìn nhầm của Staff khi đối soát chuyển khoản.

### 4.2 Modal Từ Chối Lệnh Rút Tiền — `.stf-wdr-reject-modal`
* Hộp thoại bật lên yêu cầu nhập text lý do từ chối (ví dụ: "Tài khoản ngân hàng bị khóa", "Sai tên chủ thẻ"). Nút xác nhận chỉ sáng và click được sau khi nhập tối thiểu 5 ký tự.

---

## 5. LUỒNG XỬ LÝ JS & AJAX

1. **Tải danh sách lệnh rút:**
   * Gọi API:
     * **Endpoint:** `GET /api/v1/staff/withdrawals?status=Pending`
     * **Headers:** `Authorization: Bearer <token>`
   * **Thành công (HTTP 200):** Đổ dữ liệu hiển thị lên bảng.
2. **Duyệt lệnh rút tiền:**
   * Staff click "Phê duyệt" (sau khi đã tự tay chuyển tiền qua ứng dụng e-banking thành công):
     * Gửi API:
       * **Endpoint:** `POST /api/v1/staff/withdrawals/approve/{withdrawalId}`
       * **Headers:** `Authorization: Bearer <token>`
     * **Thành công (HTTP 200):** Xóa dòng đó khỏi UI, trừ ví đóng băng của Seller vĩnh viễn, gửi thông báo báo rút tiền thành công cho Seller, hiển thị Toast báo duyệt thành công.
3. **Từ chối lệnh rút tiền:**
   * Staff click "Từ chối", nhập lý do "Tên chủ tài khoản không khớp" và gửi:
     * Gửi API:
       * **Endpoint:** `POST /api/v1/staff/withdrawals/reject/{withdrawalId}`
       * **Headers:** `Content-Type: application/json`, `Authorization: Bearer <token>`
       * **Payload:** `{ "reason": "Tên chủ tài khoản không khớp" }`
     * **Thành công (HTTP 200):** Cập nhật ví Seller (cộng lại tiền vào available_balance, trừ tiền hold_balance), gửi thông báo báo lý do cho Seller, xóa dòng lệnh rút khỏi giao diện kiểm duyệt, kích hoạt Toast.

---

## 6. RESPONSIVE

* **Viewport ≥ 768px:** Hiển thị dạng bảng lưới nhiều cột ngang đầy đủ.
* **Viewport < 768px:** Biến đổi bảng thành danh sách thẻ card xếp chồng. Mỗi thẻ hiển thị đầy đủ thông tin ngân hàng và 2 nút bấm thao tác lớn ở dưới đáy.

---

## 7. ACCESSIBILITY

- Sử dụng `aria-hidden` ẩn modal từ chối khỏi cây DOM hỗ trợ đọc màn hình khi modal chưa được bật lên.
- Ô nhập lý do từ chối có thuộc tính `required="true"`.

---

## 8. OUT OF SCOPE

- ❌ Tự động quét hóa đơn chuyển tiền ngân hàng (Bill chuyển khoản) để tự động đối chiếu hình ảnh.
- ❌ Thực hiện chuyển tiền tự động bằng API ngân hàng.