# UC-09 — Withdrawal Flow
> **Feature ID:** `feat-wallet`
> **Actor:** Seller

## Mô tả kịch bản Use Case
1. Seller vào mục "Rút tiền".
2. Nhập số tiền muốn rút (lớn hơn 50,000đ và nhỏ hơn available_balance).
3. Hệ thống đóng băng số tiền (chuyển sang `hold_balance`) và tạo lệnh rút ở trạng thái `PENDING`.
4. Staff tiến hành phê duyệt hoặc từ chối lệnh rút tiền.