---
title: API Reference
status: active
owner: Backend Team
last_updated: 2026-06-18
---

# API Reference

## Phạm vi

Danh mục này phản ánh REST endpoint được khai báo trong `src/main/java/controller`
tại ngày cập nhật. MVC route trả Thymeleaf template không nằm trong danh sách.

Ký hiệu quyền:

- `Public`: `SecurityConfig` cho phép không cần JWT.
- `Authenticated`: cần JWT hợp lệ.
- `Role check`: Service/Controller kiểm tra role.
- `Handler check`: route đi qua public matcher nhưng method tự yêu cầu principal.

Một số route chưa dùng `/api/v1`; đây là legacy route, không phải mẫu cho API mới.

## Authentication

Controller: `controller.AuthController`

| Method | Path | Quyền | Mục đích |
|---|---|---|---|
| POST | `/api/auth/register` | Public | Đăng ký tài khoản |
| POST | `/api/auth/verify-otp` | Public | Xác thực OTP đăng ký |
| POST | `/api/auth/check-reset-otp` | Public | Kiểm tra OTP đặt lại mật khẩu |
| POST | `/api/auth/resend-otp` | Public | Gửi lại OTP |
| POST | `/api/auth/login` | Public | Đăng nhập hệ thống |
| POST | `/api/auth/google` | Public | Đăng nhập Google OAuth2 |
| POST | `/api/auth/logout` | Authenticated | Thu hồi refresh token |
| POST | `/api/auth/refresh` | Public | Đổi refresh token lấy token mới |
| POST | `/api/auth/forgot-password` | Public | Gửi OTP quên mật khẩu |
| POST | `/api/auth/reset-password` | Public | Đặt lại mật khẩu |
| GET | `/api/auth/health` | Public | Health check authentication API |

DTO liên quan nằm trong `controller/dto`: `RegisterRequest`, `LoginRequest`,
`GoogleLoginRequest`, `VerifyOtpRequest`, `RefreshTokenRequest`,
`LogoutRequest` và các response tương ứng.

## Profile

Controller: `controller.ProfileController`

| Method | Path | Quyền | Mục đích |
|---|---|---|---|
| GET | `/api/v1/profile` | Authenticated | Xem hồ sơ của user hiện tại |
| PUT | `/api/v1/profile` | Authenticated | Cập nhật hồ sơ của user hiện tại |

Service: `service.UserService`.

## Product search, seller public profile và review

Controller: `controller.ProductSearchController`

| Method | Path | Quyền hiện tại | Mục đích |
|---|---|---|---|
| GET | `/api/search/products/featured` | Public | Danh sách sản phẩm nổi bật |
| GET | `/api/search/products` | Public | Search/filter/sort/pagination sản phẩm |
| GET | `/api/search/products/{productId}` | Public | Chi tiết sản phẩm |
| GET | `/api/search/products/{productId}/reviews` | Public | Danh sách đánh giá |
| POST | `/api/search/products/{productId}/reviews` | Handler check | Gửi đánh giá sau giao dịch hoàn thành |
| GET | `/api/search/seller/{sellerId}` | Public | Hồ sơ Shop/Seller |
| POST | `/api/search/seller/{sellerId}/follow` | Handler check | Follow hoặc unfollow Seller |
| GET | `/api/search/categories` | Public | Danh sách category hoạt động |

Lưu ý bảo mật: `SecurityConfig` hiện permit toàn bộ `/api/search/**`. Hai POST
endpoint review/follow tự kiểm tra principal trong handler; nên tách matcher
public/protected rõ ràng ở bước hardening.

Chi tiết filter Search được mô tả tại [Search Module](../modules/search.md).

## Seller portal

Controller: `controller.SellerController`. Các endpoint yêu cầu JWT và
`getSeller(userId)` kiểm tra role có chứa `seller`.

| Method | Path | Mục đích |
|---|---|---|
| GET | `/api/seller/dashboard` | Số liệu dashboard Seller |
| GET | `/api/seller/shop-info` | Xem thông tin Shop/ngân hàng |
| PUT | `/api/seller/shop-info` | Cập nhật thông tin Shop/ngân hàng |
| GET | `/api/seller/categories` | Category dùng trong Seller portal |
| GET | `/api/seller/products` | Danh sách sản phẩm của Seller |
| GET | `/api/seller/products/{id}` | Chi tiết sản phẩm thuộc Seller |
| POST | `/api/seller/products` | Tạo sản phẩm |
| PUT | `/api/seller/products/{id}` | Cập nhật sản phẩm |
| DELETE | `/api/seller/products/{id}` | Xóa mềm sản phẩm và variant |
| PUT | `/api/seller/products/{id}/details` | Cập nhật chi tiết sản phẩm |
| GET | `/api/seller/variants/{id}` | Chi tiết variant |
| POST | `/api/seller/variants` | Tạo variant |
| PUT | `/api/seller/variants/{id}` | Cập nhật variant |
| DELETE | `/api/seller/variants/{id}` | Xóa mềm variant |
| GET | `/api/seller/variants/{variantId}/assets` | Danh sách tài sản số của variant |
| POST | `/api/seller/digital-assets` | Tạo/import tài sản số |
| DELETE | `/api/seller/digital-assets/{id}` | Xóa mềm tài sản số |
| GET | `/api/seller/transactions` | Danh sách giao dịch bán |
| GET | `/api/seller/withdrawals` | Danh sách yêu cầu rút |
| GET | `/api/seller/withdrawals/{id}` | Chi tiết yêu cầu rút |
| POST | `/api/seller/withdrawals` | Tạo yêu cầu rút tiền |
| GET | `/api/seller/statistics` | Thống kê Seller |
| GET | `/api/seller/shop-flags` | Danh sách cảnh báo Shop |
| GET | `/api/seller/reviews` | Đánh giá Shop/sản phẩm |
| GET | `/api/seller/complaints` | Danh sách khiếu nại |
| GET | `/api/seller/complaints/{id}` | Chi tiết khiếu nại |
| POST | `/api/seller/complaints/{id}/chat` | Gửi tin nhắn trong khiếu nại |

Phần lớn Seller API hiện thao tác repository trực tiếp trong Controller; cần
dịch chuyển business logic sang Service theo Architecture Rules.

## Admin user management

Controller: `controller.AdminUserManagementController`. API yêu cầu JWT;
`AdminUserManagementService.requireAdmin()` kiểm tra role Admin.

| Method | Path | Mục đích |
|---|---|---|
| GET | `/api/admin/user-management/summary` | Tổng quan tài khoản |
| GET | `/api/admin/user-management/users` | Danh sách/filter user |
| GET | `/api/admin/user-management/users/{userId}` | Chi tiết user |
| DELETE | `/api/admin/user-management/users/{userId}` | Xóa mềm user |
| POST | `/api/admin/user-management/users/{userId}/toggle-lock` | Khóa/mở khóa user |
| PUT | `/api/admin/user-management/users/{userId}/role` | Đổi role user |
| POST | `/api/admin/user-management/staff` | Tạo Staff |
| PUT | `/api/admin/user-management/staff/{staffId}` | Cập nhật Staff |
| DELETE | `/api/admin/user-management/staff/{staffId}` | Xóa mềm Staff |

Service: `service.AdminUserManagementService`.

## Pre-order

Controller: `controller.PreOrderController`

| Method | Path | Quyền | Mục đích |
|---|---|---|---|
| POST | `/api/v1/pre-orders` | Authenticated | Tạo yêu cầu đặt trước |
| GET | `/api/v1/pre-orders` | Authenticated | Danh sách đặt trước của user hiện tại |

Service: `service.PreOrderService`.

## Purchase

Controller: `controller.TransactionController`

| Method | Path | Quyền | Mục đích |
|---|---|---|---|
| POST | `/api/transactions/purchase` | Authenticated | Mua sản phẩm và tạo giao dịch |

Service: `service.TransactionService`.

Route này là legacy chưa version; API mới cùng nhóm nên ưu tiên `/api/v1/...`.

## SePay

Controller: `controller.TopupController`

| Method | Path | Quyền hiện tại | Mục đích |
|---|---|---|---|
| GET | `/api/sepay/config` | Public | Trả cấu hình hiển thị chuyển khoản |
| POST | `/api/sepay/webhook` | Public + webhook token | Nhận callback giao dịch SePay |

Service: `service.TopupService`.

Webhook phải xác thực token/signature, xử lý idempotency và không log toàn bộ
payload nhạy cảm.

## KYC (Định danh)

Controller: `controller.api.KycController` (User) và `controller.api.StaffKycController` (Staff)

| Method | Path | Quyền | Mục đích |
|---|---|---|---|
| POST | `/api/v1/kyc` | Authenticated | Nộp hồ sơ định danh KYC |
| GET | `/api/v1/kyc/me` | Authenticated | Lấy lịch sử nộp hồ sơ của user hiện tại |
| GET | `/api/v1/kyc/{id}/documents/{docType}` | Authenticated | Tải/Xem ảnh tài liệu KYC (yêu cầu jwt ở query token) |
| GET | `/api/v1/staff/kyc` | Staff, Admin | Lấy danh sách hồ sơ KYC có filter và phân trang |
| GET | `/api/v1/staff/kyc/{id}` | Staff, Admin | Xem chi tiết hồ sơ KYC |
| POST | `/api/v1/staff/kyc/{id}/review` | Staff, Admin | Phê duyệt hoặc từ chối hồ sơ (yêu cầu version) |

Service: `service.KycService`, `service.KycStorageService`.

## API chưa triển khai

Các màn hình sau có UI/mock nhưng chưa có REST API production đầy đủ:

- Đăng ký Shop/approval.
- Một số Staff portal: complaint, withdrawal, flag và transaction review.
- Notification/broadcast production.

Khi triển khai, endpoint mới phải được thêm vào tài liệu này trong cùng task.

## Contract còn thiếu cần bổ sung dần

Danh mục trên mới xác nhận method/path/quyền/mục đích từ source. Các module cần
bổ sung request schema, response schema, status/error và ví dụ sanitized theo
[API Guideline](README.md), ưu tiên Auth, Purchase, Seller, Admin và KYC.
