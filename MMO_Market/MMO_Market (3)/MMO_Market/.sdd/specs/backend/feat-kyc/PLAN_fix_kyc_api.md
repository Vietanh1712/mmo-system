# PLAN — Fix KYC and Shop Registration API Call Bug

Sửa lỗi dư thừa tiền tố `/api` trong URL truyền vào hàm `authFetch` ở frontend. Hàm `authFetch` đã tự động thêm tiền tố `/api` (được cấu hình qua `API_BASE = '/api'`), việc truyền `/api/v1/...` sẽ dẫn đến URL thực tế bị nhân đôi thành `/api/api/v1/...` gây lỗi 404.

## Proposed Changes

---

### Frontend Components

#### [MODIFY] [staff-kyc.js](file:///c:/Users/pc/MMO_new1/MMO_Market/MMO_Market%20(3)/MMO_Market/apps/frontend/static/js/staff/staff-kyc.js)

- Thay đổi URL truy vấn danh sách KYC của Staff:
  - Dòng 11: Đổi `let url = \`/api/v1/staff/kyc?page=\${page}&size=10\`;` thành `let url = \`/v1/staff/kyc?page=\${page}&size=10\`;`.

#### [MODIFY] [account-register-shop.js](file:///c:/Users/pc/MMO_new1/MMO_Market/MMO_Market%20(3)/MMO_Market/apps/frontend/static/js/customer/account-register-shop.js)

- Thay đổi URL đăng ký shop của Customer:
  - Dòng 188: Đổi `authFetch('/api/v1/profile/register-shop', ...)` thành `authFetch('/v1/profile/register-shop', ...)`.

## Verification Plan

### Manual Verification
1. Đăng nhập tài khoản Customer gửi KYC.
2. Đăng nhập tài khoản Staff/Admin kiểm tra danh sách KYC ở trang `/staff/kyc`.
3. Kiểm tra tính năng Đăng ký Shop của Customer.
