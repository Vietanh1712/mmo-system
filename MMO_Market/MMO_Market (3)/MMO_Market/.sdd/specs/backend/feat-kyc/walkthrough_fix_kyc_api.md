# Walkthrough - Sửa lỗi API KYC & Đăng ký Shop

Tôi đã sửa lỗi tiền tố `/api` bị dư thừa ở frontend khi gọi qua `authFetch` khiến API trả về 404 và làm Staff không nhận được hồ sơ KYC cũng như Customer không đăng ký shop được.

## Các thay đổi chính

### Frontend Javascript

#### 1. [staff-kyc.js](file:///c:/Users/pc/MMO_new1/MMO_Market/MMO_Market%20(3)/MMO_Market/apps/frontend/static/js/staff/staff-kyc.js)

- Loại bỏ `/api` trong URL truyền vào `authFetch`:
  ```javascript
  // Trước
  let url = `/api/v1/staff/kyc?page=${page}&size=10`;
  
  // Sau (Sửa đổi)
  let url = `/v1/staff/kyc?page=${page}&size=10`;
  ```

#### 2. [account-register-shop.js](file:///c:/Users/pc/MMO_new1/MMO_Market/MMO_Market%20(3)/MMO_Market/apps/frontend/static/js/customer/account-register-shop.js)

- Loại bỏ `/api` trong URL truyền vào `authFetch`:
  ```javascript
  // Trước
  const response = await authFetch('/api/v1/profile/register-shop', ...);
  
  // Sau (Sửa đổi)
  const response = await authFetch('/v1/profile/register-shop', ...);
  ```

---

## Kết quả kiểm nghiệm

1. Các trang HTML gọi `authFetch` đã ghép đúng URL `/api/v1/...` thay vì `/api/api/v1/...`.
2. Staff đã có thể load danh sách yêu cầu KYC chờ duyệt bình thường.
3. Người dùng (Customer) có thể hoàn tất gửi yêu cầu đăng ký shop mà không gặp lỗi 404 từ API `/api/v1/profile/register-shop`.
