# SPEC — Staff Withdrawal Approval Panel
> **Feature ID:** `feat-staff` | **Page:** `StaffWithdrawals`
> **Route:** `/staff/withdrawals` | **Template:** `templates/staff/withdrawals.html`
> **JS Script:** `static/js/staff-ui.js` | **Prefix:** `swdr-`

---

## 1. MÔ TẢ TRANG
Giao diện của Staff kiểm duyệt danh sách các lệnh rút tiền từ người bán. Nhân viên đối soát mã chuyển tiền ngân hàng thực tế rồi thực hiện click Duyệt (Approve) hoặc Từ chối (Reject) kèm lý do.

---

## 2. MOCKUP GIAO DIỆN (ASCII)
```
┌──────────────────────────────────────────────────────────────────┐
│  Staff Console > Duyệt rút tiền mặt                              │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Danh sách yêu cầu rút tiền đang chờ                             │
│  ──────────────────────────────────────────────────────────────  │
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │ Yêu cầu #WD752 - Seller: shopnetflix@gmail.com             │  │
│  │ Số tiền: 1.500.000đ | Phí: 22.500đ                         │  │
│  │ Ngân hàng: Vietcombank - 001100123456 - NGUYEN VAN A       │  │
│  │                                                            │  │
│  │      [ Phê Duyệt (Hoàn thành) ]   [ Từ Chối Lệnh Rút ]     │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

---

## 3. LUỒNG XỬ LÝ DUYỆT / TỪ CHỐI (FLOW)
1. Khi Staff click "Phê Duyệt":
   * Gửi `POST /api/staff/withdrawals/approve/{id}`.
   * Số tiền trong `hold_balance` của Seller bị trừ vĩnh viễn, trạng thái chuyển thành `Completed`.
2. Khi Staff click "Từ Chối Lệnh Rút":
   * Mở modal phụ yêu cầu nhập lý do từ chối.
   * Gửi `POST /api/staff/withdrawals/reject/{id}` kèm body chứa `rejectionReason`.
   * Số tiền được trả lại từ `hold_balance` sang `available_balance` của Seller.