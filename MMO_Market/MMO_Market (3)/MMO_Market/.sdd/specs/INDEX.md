# MMO Market — Specification Index

Tài liệu này là mục lục hệ thống các luồng chức năng của dự án MMO Market. Toàn bộ tài liệu đặc tả được tổ chức và quản lý theo chuẩn **Specification-Driven Development (SDD)**, đồng bộ trực tiếp với cấu trúc mã nguồn thực tế của hệ thống.

---

## 1. Core Documentation (Quy định & Luật cốt lõi)
- [Constitution (Hiến pháp hệ thống)](../constitution.md)
- [Design Architecture Specification (Quy chuẩn thiết kế UI/UX)](../../docs/architecture/DESIGN.md)

---

## 2. Backend Feature Specifications (Đặc tả nghiệp vụ xử lý của API)
Nằm trong thư mục `.sdd/specs/backend/`, quản lý chi tiết logic nghiệp vụ, sơ đồ Use Case, Plan và các Tasks triển khai:

- **Authentication & Profiles**: [feat-auth/SPEC.md](./backend/feat-auth/SPEC.md) (Đăng nhập, Đăng ký, OTP, Quên mật khẩu)
- **KYC Verification**: [feat-kyc/SPEC.md](./backend/feat-kyc/SPEC.md) (Luồng gửi và duyệt ảnh giấy tờ KYC)
- **Seller Store Management**: [feat-seller/SPEC.md](./backend/feat-seller/SPEC.md) (Đăng ký mở Shop, quản lý shop)
- **Product & Inventory**: [feat-product/SPEC.md](./backend/feat-product/SPEC.md) (Quản lý sản phẩm, biến thể, mã hóa code)
- **Wallet & Transactions**: [feat-wallet/SPEC.md](./backend/feat-wallet/SPEC.md) (Ví tài chính khả dụng/đóng băng, nạp SePay VietQR)
- **Order Escrow Checkout**: [feat-order/SPEC.md](./backend/feat-order/SPEC.md) (Thanh toán đơn hàng, giam tiền 72h trong Escrow)
- **Pre-Order Engine**: [feat-preorder/SPEC.md](./backend/feat-preorder/SPEC.md) (Đăng ký mua trước khi kho hàng hết)
- **Dispute & Arbitration**: [feat-complaint/SPEC.md](./backend/feat-complaint/SPEC.md) (Tranh chấp đơn hàng, WebSocket Chat giải quyết)
- **Support Tickets**: [feat-support/SPEC.md](./backend/feat-support/SPEC.md) (Gửi yêu cầu hỗ trợ khách hàng)
- **Direct Messaging**: [feat-chat/SPEC.md](./backend/feat-chat/SPEC.md) (WebSocket chat trực tiếp Buyer - Seller)
- **System Notifications**: [feat-notification/SPEC.md](./backend/feat-notification/SPEC.md) (Biến động tài khoản, thông báo đẩy)
- **Staff Operations**: [feat-staff/SPEC.md](./backend/feat-staff/SPEC.md) (Các quyền duyệt kiểm duyệt hệ thống)
- **Admin Revenue Reports**: [feat-admin/SPEC.md](./backend/feat-admin/SPEC.md) (Thống kê doanh số, biểu phí hoa hồng)
- **File Upload Service**: [feat-upload/SPEC.md](./backend/feat-upload/SPEC.md) (Quản lý lưu trữ tệp đính kèm)

---

## 3. Frontend Screen Specifications (Đặc tả trang hiển thị và tương tác)
Nằm trong thư mục `.sdd/specs/frontend/`, mô tả chi tiết giao diện (ASCII wireframe), biến trạng thái cục bộ (State) và luồng xử lý tương tác AJAX/Fetch:

### Customer Account Portal (`feat-customer`)
- [SPEC-profile.md](./frontend/feat-customer/SPEC-profile.md): Trang hồ sơ cá nhân và đổi mật khẩu.
- [SPEC-kyc.md](./frontend/feat-customer/SPEC-kyc.md): Trang gửi thông tin định danh KYC.
- [SPEC-wallet.md](./frontend/feat-customer/SPEC-wallet.md): Trang quản lý ví, nạp tiền tự động qua QR.
- [SPEC-orders.md](./frontend/feat-customer/SPEC-orders.md): Quản lý lịch sử đơn hàng đã mua, giải phóng Escrow sớm.
- [SPEC-order-detail.md](./frontend/feat-customer/SPEC-order-detail.md): Xem mã thẻ số đã giải mã AES.

### Seller Dashboard (`feat-seller`)
- [SPEC-dashboard.md](./frontend/feat-seller/SPEC-dashboard.md): Màn hình thống kê doanh số tổng quan cho Seller.
- [SPEC-inventory.md](./frontend/feat-seller/SPEC-inventory.md): Danh sách sản phẩm đăng bán & quản lý kho.
- [SPEC-product-form.md](./frontend/feat-seller/SPEC-product-form.md): Biểu mẫu CRUD đăng bán sản phẩm & mã hóa mật mã số.
- [SPEC-withdrawals.md](./frontend/feat-seller/SPEC-withdrawals.md): Yêu cầu rút tiền mặt về ngân hàng với OTP xác thực.

### Staff Console (`feat-staff`)
- [SPEC-kyc-review.md](./frontend/feat-staff/SPEC-kyc-review.md): Giao diện kiểm duyệt ảnh KYC.
- [SPEC-withdrawal-review.md](./frontend/feat-staff/SPEC-withdrawal-review.md): Duyệt lệnh rút tiền cho Seller.
- [SPEC-complaints-arbitration.md](./frontend/feat-staff/SPEC-complaints-arbitration.md): Phòng chat WebSocket xử lý tranh chấp đơn hàng.

### Public Landing Pages (`feat-landing-page`)
- [SPEC-home.md](./frontend/feat-landing-page/SPEC-home.md): Trang chủ hiển thị danh mục sản phẩm công khai.
- [SPEC-login.md](./frontend/feat-landing-page/SPEC-login.md) / [SPEC-register.md](./frontend/feat-landing-page/SPEC-register.md): Đăng nhập, đăng ký thành viên.
- [SPEC-verify-otp.md](./frontend/feat-landing-page/SPEC-verify-otp.md): Trang xác thực OTP đăng ký tài khoản.
