# UC-09 — Withdrawal Flow
> **Feature ID:** `feat-wallet`
> **Actor:** Seller

## Mô tả kịch bản Use Case
1. Seller vào mục "Rút tiền".
2. Nhập số tiền muốn rút (lớn hơn 50,000đ và nhỏ hơn available_balance).
3. Hệ thống gửi mã OTP xác thực qua email đăng ký.
4. Seller nhập mã OTP xác thực thành công.
5. Hệ thống đóng băng số tiền (chuyển sang `hold_balance`) và tạo lệnh rút ở trạng thái `PENDING`.