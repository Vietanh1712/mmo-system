# SPEC — Seller Withdrawal Panel (`/seller/withdrawals`)
> **Feature ID:** `feat-seller` | **Page:** `SellerWithdrawals`
> **Route:** `/seller/withdrawals` | **Template:** `templates/seller/withdrawals.html`
> **JS Script:** `static/js/seller-console.js` | **Prefix:** `swth-`

---

## 1. MÔ TẢ TRANG
Giao diện giúp người bán rút tiền mặt tích lũy trong ví khả dụng về tài khoản ngân hàng cá nhân. Luồng xử lý yêu cầu nhập mã OTP gửi qua hòm thư điện tử để xác minh bảo mật.

---

## 2. MOCKUP GIAO DIỆN (ASCII)
```
┌──────────────────────────────────────────────────────────────────┐
│  MMO Seller Console > Rút tiền về ngân hàng                      │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Tạo lệnh rút tiền mặt                                           │
│  ──────────────────────────────────────────────────────────────  │
│  Số dư khả dụng: 3.500.000đ                                      │
│                                                                  │
│  Số tiền muốn rút (VNĐ): [ 1500000                  ]            │
│  (Tối thiểu 50.000đ - Phí rút 1.5%)                              │
│                                                                  │
│  Tài khoản ngân hàng thụ hưởng:                                  │
│  Vietcombank | 001100123456 | NGUYEN VAN A                       │
│                                                                  │
│  Mã OTP xác thực email:  [ 456123 ]    [ Gửi mã OTP ]            │
│                                                                  │
│                  [ Xác Nhận Yêu Cầu Rút ]                        │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

---

## 3. LUỒNG XỬ LÝ OTP & TẠO LỆNH RÚT (FLOW)
1. Seller điền số tiền rút. Click "Gửi mã OTP".
2. JS gửi POST tới `/api/seller/withdrawals/send-otp` -> Vô hiệu hóa nút trong 60 giây (đếm ngược hiển thị `Gửi lại sau (59s)`).
3. Email được gửi kèm mã OTP 6 số.
4. Seller điền OTP và bấm "Xác Nhận Yêu Cầu Rút".
5. Gửi POST tới `/api/seller/withdrawals/submit` kèm body `{ amount, otp }`.
6. Thành công -> Trừ ví khả dụng, chuyển tiền sang ví tạm giữ `hold_balance`, tạo lệnh ở trạng thái `Pending` chờ nhân viên Staff duyệt.

---

## 4. FUNCTIONAL REQUIREMENTS (EARS)
| ID | EARS Requirement |
|---|---|
| FR-WTH-FE-01 | WHEN a Seller requests OTP, THE SYSTEM SHALL trigger a cooldown timer of 60 seconds on the request button. |
| FR-WTH-FE-02 | WHEN a Seller submits withdrawal, THE SYSTEM SHALL validate that the amount is at least 50,000 VND and does not exceed available balance. |