# MMO Market

---

## 1. Context & Goal

### Background

- Hệ thống MMO Market là nền tảng thương mại điện tử C2C (Customer-to-Customer) dành riêng cho các sản phẩm số, tài sản số (tài khoản, key, game card...).
- Nền tảng giải quyết nhu cầu giao dịch an toàn thông qua cơ chế khóa quỹ (Escrow) trong vòng 72 giờ và sử dụng VNĐ làm đơn vị tiền tệ chính thức.
- Nền tảng kết nối Người mua (Customer) và Người bán (Seller) thông qua các quy trình đã được số hóa: quản lý gian hàng, nạp tiền, giao dịch khóa quỹ, khiếu nại và rút tiền.
- Các quy trình kiểm duyệt (KYC, giải quyết khiếu nại, rút tiền) được thực hiện bởi đội ngũ Staff, và hệ thống được vận hành bởi Admin.

### Goals

- Quản lý tài khoản người dùng và xác thực.
- Xác minh danh tính người bán (KYC/Seller Registration).
- Quản lý gian hàng (Shop) và các sản phẩm/tài sản số.
- Nạp tiền (Top-up) vào ví và giao dịch thanh toán.
- Đặt hàng, đặt trước (Pre-order), giao tài sản số và cơ chế khóa quỹ (Escrow).
- Quản lý khiếu nại (Complaint) và yêu cầu hỗ trợ (Phiếu hỗ trợ).
- Quản lý việc rút tiền (Withdrawal) từ ví của người bán.
- Cảnh báo gian hàng (Flag Shop) và đánh giá (Review) sản phẩm.
- Nhắn tin (Chat) và nhận thông báo (Notification).
- Quản trị hệ thống (Administration) bao gồm quản lý người dùng, phân quyền và phí.

---

## 2. Actors & Roles

### Guest
Người dùng vãng lai chưa đăng nhập vào hệ thống.

**Permissions**
- Xem trang chủ, danh mục sản phẩm, tìm kiếm.
- Xem chi tiết sản phẩm.
- Đăng ký, đăng nhập và khôi phục mật khẩu.

### Customer
Người dùng đã đăng ký tài khoản hợp lệ.

**Permissions**
- Quản lý hồ sơ cá nhân, đổi mật khẩu.
- Thực hiện định danh (KYC) để gửi yêu cầu mở Shop.
- Nạp tiền vào ví, xem số dư và lịch sử giao dịch.
- Đặt mua sản phẩm số, đặt trước (Pre-Order), xem chi tiết đơn hàng.
- Theo dõi gian hàng (Follow Shop).
- Đánh giá sản phẩm đã mua.
- Gửi khiếu nại đối với đơn hàng gặp sự cố, tạo phiếu hỗ trợ (Support Ticket).
- Nhắn tin với người bán.
- Gửi cờ báo cáo gian hàng (Flag Shop).
- Nhận và quản lý thông báo từ hệ thống.

### Seller
Customer đã được Staff phê duyệt yêu cầu mở Shop.

**Permissions**
- Thực hiện mọi chức năng của một Customer.
- Truy cập Seller Portal.
- Quản lý Shop, tạo và chỉnh sửa thông tin sản phẩm, phân loại (Variant) và tài sản số (Digital Asset).
- Xem lịch sử giao dịch bán hàng, quản lý đơn hàng.
- Cập nhật thông tin tài khoản ngân hàng (SellerBankInfo).
- Gửi yêu cầu rút tiền (Withdrawal) từ số dư khả dụng.
- Phản hồi khiếu nại và nhắn tin hỗ trợ khách hàng.

### Staff
Nhân viên vận hành hệ thống.

**Permissions**
- Xem và duyệt/từ chối yêu cầu KYC (KycRequest) và Seller Registration.
- Xem và xử lý các khiếu nại (Complaint) từ Customer và Seller.
- Xem, duyệt/từ chối các yêu cầu rút tiền (Withdrawal) của Seller.
- Xử lý các cờ báo cáo (Flag) gian hàng và phiếu hỗ trợ (Support Ticket).
- Trao đổi (Chat) với người dùng trong luồng giải quyết khiếu nại/hỗ trợ.
- Xem quyền hạn cá nhân (MyPermissions).

### Admin
Quản trị viên hệ thống cấp cao nhất.

**Permissions**
- Quản lý danh sách người dùng, xem thống kê (AdminUserManagement).
- Quản lý phân quyền (Role, Permission) cho Staff.
- Cấu hình hệ thống (SystemConfiguration).
- Quản lý mức phí, doanh thu (AdminRevenue).
- Xem nhật ký hoạt động (Audit log).

---

## 3. Functional Requirements

### FR-01 Authentication
- Đăng ký bằng email/password.
- Đăng nhập (Email/Password hoặc Google OAuth2).
- Xác thực email bằng mã (EmailVerification).
- Khôi phục mật khẩu.
- Đổi mật khẩu, quản lý đăng nhập 2 bước (2FA).
- Cấp và quản lý access token, refresh token (Authentication).

### FR-02 User Profile
- Xem thông tin hồ sơ cá nhân (email, họ tên, số điện thoại, trạng thái shop, số dư).
- Cập nhật thông tin cá nhân.

### FR-03 KYC
- Gửi yêu cầu định danh (KycRequest) với tài liệu (CMND/CCCD, ảnh chụp).
- Staff xem, duyệt hoặc từ chối KYC.

### FR-04 Seller Registration
- Gửi yêu cầu mở Shop (SellerRegistration).
- Staff duyệt hoặc từ chối đơn đăng ký.
- Khai báo thông tin ngân hàng (SellerBankInfo).

### FR-05 Product Discovery & Shop Follow
- Hiển thị sản phẩm nổi bật trên trang chủ.
- Tìm kiếm sản phẩm theo keyword, lọc theo danh mục (Category), giá, rating.
- Xem chi tiết sản phẩm.
- Theo dõi gian hàng (ShopFollower) và bỏ theo dõi.

### FR-06 Shop and Product Management
- Chỉ Seller mới được tạo, sửa thông tin sản phẩm của mình.
- Quản lý ProductVariant (phân loại giá, số lượng).
- Quản lý Digital Asset (kho tài sản số gắn với Variant).
- Xóa sản phẩm/variant (Soft delete).

### FR-07 Wallet and Top-up
- Xem số dư ví (Balance) bằng VNĐ.
- Top-up (Nạp tiền) thông qua cổng SePay.
- Xem lịch sử giao dịch nạp tiền (TopupTransaction) và biến động số dư (WalletTransaction).
- Chống trùng lặp xử lý webhook từ SePay (Idempotency).

### FR-08 Order and Purchase (Includes Pre-Order)
- Khách hàng (Customer) chọn mua ProductVariant.
- Hỗ trợ đặt hàng trước (PreOrder).
- Hệ thống kiểm tra số dư ví, tình trạng tồn kho, và xác thực sở hữu.
- Trừ tiền trong ví, giảm tồn kho.
- Khởi tạo Transaction (đơn hàng) và giao tài sản số.
- Thiết lập thời gian khóa quỹ (Escrow release date = thời gian tạo + 72 giờ).

### FR-09 Withdrawal
- Seller gửi yêu cầu rút tiền trong giới hạn số dư khả dụng (sau escrow).
- Trừ số dư ví ngay khi yêu cầu được tạo.
- Staff xem, duyệt/từ chối yêu cầu rút tiền.
- Upload bằng chứng chuyển tiền khi duyệt hoàn tất.

### FR-10 Complaint & Support Ticket (Phiếu hỗ trợ)
- Customer tạo khiếu nại cho đơn hàng thuộc sở hữu.
- Tạm dừng giải phóng quỹ (Escrow) nếu khiếu nại mở trong 72 giờ đầu.
- Gửi yêu cầu hỗ trợ chung (SupportTicket - Phiếu hỗ trợ).
- Staff ra quyết định giải quyết (Customer thắng: hoàn tiền, Seller thắng: giải phóng tiền).

### FR-11 Feedback, Review and Flag
- Customer đánh giá (Review, Rating từ 1-5 sao) sản phẩm sau khi mua thành công.
- Customer báo cáo (ShopFlag) gian hàng vi phạm.
- Staff tiếp nhận và xử lý cờ báo cáo Shop.

### FR-12 Chat
- Gửi tin nhắn trực tiếp giữa các người dùng (Chat).
- Tính năng chặn (ChatBlock) và tắt thông báo (ChatMute).
- Trao đổi (Chat) giữa Customer, Seller và Staff.

### FR-13 Notification
- Tạo và phát thông báo cá nhân, thông báo hệ thống (Notification).
- Quản lý trạng thái đọc thông báo (Read/Unread).

### FR-14 Staff Management
- Quản lý danh sách Staff.
- Gán quyền hạn (Permission) cho Staff.

### FR-15 Administration
- Thống kê doanh thu (Revenue).
- Cấu hình hệ thống động (SystemConfiguration).
- Quản lý danh mục (Category).
- Ghi nhận và xem nhật ký hoạt động (AuditLog).

---

## 4. Non-Functional Requirements

### Performance
- Tác vụ tài chính (mua hàng, nạp, rút tiền) phải đảm bảo tính toàn vẹn (ACID properties).

### Security
- Password dùng thuật toán BCrypt.
- Xác thực và phân quyền Role-based Access Control (RBAC).
- Ownership validation (kiểm tra quyền sở hữu) được yêu cầu trên các thao tác bảo mật.
- Secret lấy từ environment variables.
- Webhook thanh toán yêu cầu Unique/Idempotent key.

### Scalability
- Thiết kế hệ thống bảng riêng cho WalletTransactions và Transactions, không ảnh hưởng Users.

### Usability
- Giao diện sử dụng Thymeleaf HTML Template.
- Hệ thống UI Design System đồng bộ, thiết kế Responsive.

### Maintainability
- Cấu trúc sử dụng Database-First (Database là chuẩn mực cấu trúc).
- API trả về Error Response và HTTP Status nhất quán.
- Hầu hết các Entity sử dụng Soft Delete thay vì Hard Delete.

---

## 5. Data Model

### Users
| Attribute | Type | Description |
| --------- | ---- | ----------- |
| id | BIGINT | Primary key |
| email | VARCHAR | Unique email |
| password | VARCHAR | BCrypt hashed password |
| full_name | VARCHAR | Tên đầy đủ |
| role | VARCHAR | Role của user |
| balance_vnd | BIGINT | Số dư ví |
| isDelete | BIT | Soft delete flag |

### Authentication
| Attribute | Type | Description |
| --------- | ---- | ----------- |
| id | BIGINT | Primary key |
| user_id | BIGINT | FK -> Users(id) |
| provider | VARCHAR | System / Google |
| refresh_token | VARCHAR | Token |

### EmailVerification
| Attribute | Type | Description |
| --------- | ---- | ----------- |
| id | BIGINT | Primary key |
| user_id | BIGINT | FK -> Users(id) |
| verification_code | VARCHAR | Mã OTP |

### KycRequest
| Attribute | Type | Description |
| --------- | ---- | ----------- |
| id | BIGINT | Primary key |
| user_id | BIGINT | FK -> Users(id) |
| id_number | VARCHAR | CMND/CCCD |
| status | VARCHAR | PENDING, APPROVED, REJECTED |

### SellerRegistration
| Attribute | Type | Description |
| --------- | ---- | ----------- |
| id | BIGINT | Primary key |
| user_id | BIGINT | FK -> Users(id) |
| status | VARCHAR | Pending, Approved, Rejected |

### SellerBankInfo
| Attribute | Type | Description |
| --------- | ---- | ----------- |
| id | BIGINT | Primary key |
| user_id | BIGINT | FK -> Users(id) |
| bank_name | VARCHAR | Tên ngân hàng |
| account_number | VARCHAR | Số tài khoản |

### Categories
| Attribute | Type | Description |
| --------- | ---- | ----------- |
| id | BIGINT | Primary key |
| parent_id | BIGINT | Hỗ trợ danh mục cha/con |
| name | VARCHAR | Tên danh mục |

### Products
| Attribute | Type | Description |
| --------- | ---- | ----------- |
| id | BIGINT | Primary key |
| seller_id | BIGINT | FK -> Users(id) |
| category_id | BIGINT | FK -> Categories(id) |
| name | VARCHAR | Tên sản phẩm |

### ProductVariants
| Attribute | Type | Description |
| --------- | ---- | ----------- |
| id | BIGINT | Primary key |
| product_id | BIGINT | FK -> Products(id) |
| price_vnd | BIGINT | Giá VNĐ |
| stock | INT | Số lượng tồn |

### DigitalAssets
| Attribute | Type | Description |
| --------- | ---- | ----------- |
| id | BIGINT | Primary key |
| variant_id | BIGINT | FK -> ProductVariants(id) |
| transaction_id | BIGINT | Nullable, FK -> Transactions(id) |

### TopupTransaction
| Attribute | Type | Description |
| --------- | ---- | ----------- |
| id | BIGINT | Primary key |
| user_id | BIGINT | FK -> Users(id) |
| amount_vnd | BIGINT | Tiền nạp |

### Transaction
| Attribute | Type | Description |
| --------- | ---- | ----------- |
| id | BIGINT | Primary key |
| customer_id | BIGINT | FK -> Users(id) |
| seller_id | BIGINT | FK -> Users(id) |
| amount_vnd | BIGINT | Tổng tiền |
| status | VARCHAR | Trạng thái giao dịch |
| escrow_release_date | DATETIME | Hạn giải phóng quỹ |

### WalletTransaction
| Attribute | Type | Description |
| --------- | ---- | ----------- |
| id | BIGINT | Primary key |
| user_id | BIGINT | FK -> Users(id) |
| amount_vnd | BIGINT | Số tiền biến động |
| type | VARCHAR | TOPUP, PAYMENT, WITHDRAWAL... |

### Withdrawal
| Attribute | Type | Description |
| --------- | ---- | ----------- |
| id | BIGINT | Primary key |
| seller_id | BIGINT | FK -> Users(id) |
| amount | BIGINT | Số tiền |

### Complaint
| Attribute | Type | Description |
| --------- | ---- | ----------- |
| id | BIGINT | Primary key |
| transaction_id | BIGINT | FK -> Transactions |
| resolution | VARCHAR | Cách giải quyết |

### SupportTicket
| Attribute | Type | Description |
| --------- | ---- | ----------- |
| id | BIGINT | Primary key |
| user_id | BIGINT | FK -> Users |
| title | VARCHAR | Tiêu đề |

### ShopFollower
| Attribute | Type | Description |
| --------- | ---- | ----------- |
| id | BIGINT | Primary key |
| follower_id | BIGINT | FK -> Users |
| seller_id | BIGINT | FK -> Users |

### PreOrder
| Attribute | Type | Description |
| --------- | ---- | ----------- |
| id | BIGINT | Primary key |
| product_id | BIGINT | FK -> Products |
| expected_price | BIGINT | Giá dự kiến |

### Review
| Attribute | Type | Description |
| --------- | ---- | ----------- |
| id | BIGINT | Primary key |
| product_id | BIGINT | FK -> Products |
| rating | INT | Đánh giá (1-5) |

### Chat
| Attribute | Type | Description |
| --------- | ---- | ----------- |
| id | BIGINT | Primary key |
| sender_id | BIGINT | FK -> Users |
| receiver_id | BIGINT | FK -> Users |
| message | VARCHAR | Nội dung |

### ChatBlock
| Attribute | Type | Description |
| --------- | ---- | ----------- |
| id | BIGINT | Primary key |
| blocker_id | BIGINT | FK -> Users |
| blocked_id | BIGINT | FK -> Users |

### ChatMute
| Attribute | Type | Description |
| --------- | ---- | ----------- |
| id | BIGINT | Primary key |
| user_id | BIGINT | FK -> Users |
| contact_id | BIGINT | FK -> Users |

### ShopFlag
| Attribute | Type | Description |
| --------- | ---- | ----------- |
| id | BIGINT | Primary key |
| seller_id | BIGINT | FK -> Users |
| staff_id | BIGINT | FK -> Users |

### Notification
| Attribute | Type | Description |
| --------- | ---- | ----------- |
| id | BIGINT | Primary key |
| user_id | BIGINT | FK -> Users |
| content | VARCHAR | Nội dung |
| isRead | BIT | Đã đọc chưa |

### SystemConfiguration
| Attribute | Type | Description |
| --------- | ---- | ----------- |
| id | INT | Primary key |
| config_key | VARCHAR | Tên cấu hình |
| config_value | VARCHAR | Giá trị |

### Permission
| Attribute | Type | Description |
| --------- | ---- | ----------- |
| id | INT | Primary key |
| name | VARCHAR | Tên quyền |

### AuditLog
| Attribute | Type | Description |
| --------- | ---- | ----------- |
| id | BIGINT | Primary key |
| action | VARCHAR | Hành động |
| details | VARCHAR | Chi tiết |

---

## 6. Error Handling

### Authentication Errors
- Email hoặc mật khẩu không hợp lệ.
- Tài khoản bị khóa.
- Email chưa xác minh.
- OTP hết hạn hoặc không hợp lệ.

### Validation Errors
- Thiếu trường bắt buộc.
- Email không hợp lệ.
- Số tiền không hợp lệ (Bị âm).
- Quantity không hợp lệ (Bị âm).

### Business Errors
- Shop chưa được phê duyệt.
- Customer chưa hoàn thành KYC.
- Sản phẩm không còn tồn kho.
- Số dư không đủ (INSUFFICIENT_FUNDS).
- Withdrawal vượt quá số dư khả dụng.
- Complaint đã được đóng.
- Order không thuộc về user hiện tại (Lỗi Ownership).

### System Errors
- Database connection failure.
- Lỗi tích hợp Payment (SePay callback).
- Rollback transaction khi hệ thống gặp sự cố bất ngờ.

### Error Response
- Trả về mã HTTP status chuẩn và error body thống nhất.
- Hiển thị thông báo thân thiện tới người dùng qua UI, không làm hệ thống crash.

---

## 7. Acceptance Criteria

### User Management
- User có thể đăng ký tài khoản hợp lệ.
- User có thể đăng nhập và đăng xuất.
- User chỉ truy cập được chức năng đúng quyền.

### KYC & Seller Registration
- User có thể gửi KYC và mở Shop.
- Staff có thể duyệt hoặc từ chối KYC.
- Lý do từ chối được lưu và hiển thị.

### Product and Shop
- Seller có thể tạo và cập nhật sản phẩm.
- Customer có thể tìm kiếm, lọc và xem sản phẩm.
- Hỗ trợ PreOrder thành công cho các sản phẩm hợp lệ.

### Order
- Customer có thể tạo đơn hàng.
- Tổng tiền được tính đúng, trừ chính xác số dư.
- Trạng thái order được cập nhật đúng.
- Escrow được áp dụng chính xác 72 giờ cho các giao dịch.

### Withdrawal
- Seller có thể gửi yêu cầu rút tiền hợp lệ.
- Staff có thể phê duyệt hoặc từ chối và upload bằng chứng.
- Không được xử lý trùng yêu cầu.

### Complaint & Support Ticket
- User có thể tạo complaint cho order hợp lệ hoặc gửi Phiếu hỗ trợ chung.
- Staff có thể xem, tương tác Chat và xử lý yêu cầu.

### Chat & Notification
- Người dùng có thể nhắn tin trực tiếp và quản lý liên lạc (Block/Mute).
- Hệ thống ghi nhận và đánh dấu thông báo chính xác (Read/Unread).

### Administration
- Staff và Admin chỉ truy cập được chức năng đúng phạm vi.
- Quản trị viên thay đổi thành công cấu hình hệ thống (SystemConfiguration).

---

## 8. Out of Scope

Hiện tại repository chưa có tài liệu chính thức xác định đầy đủ phạm vi ngoài hệ thống. Danh sách dưới đây được tổng hợp từ các chức năng chưa xuất hiện trong source code và cần được Product Owner xác nhận.

- Giao tiếp thời gian thực (Chat Realtime): Mặc dù Backend đã có Module Chat lưu trữ tin nhắn (REST API `ChatController`, Models `Chat`, `ChatBlock`), tuy nhiên không có bất kỳ cấu hình WebSocket nào (như STOMP, SignalR/Hub) được tìm thấy, chứng tỏ ứng dụng chưa có giao tiếp thời gian thực. `Cần xác nhận`
- Danh sách yêu thích (Wishlist): Frontend có thể hiển thị, nhưng Backend không hề có Entity/Model `Wishlist` hay `WishlistController`. Chức năng này không được hỗ trợ xử lý trên DB. `Cần xác nhận`

*(Lưu ý: Các module từng bị cho là Mock như ShopRegistration, Notification nay đã có bằng chứng hiện diện đầy đủ qua Repository, Service và Controller thực tế).*




## Appendix A – Functional Traceability Matrix

| FR ID | Feature | Actor | Screen | Route/API | Controller | Service | Repository | Entity/Table | Status | Evidence |
|---|---|---|---|---|---|---|---|---|---|---|
| FR-01 | Authentication | Guest | /login, /register | /api/v1/auth | AuthController | AuthenticationService | UserRepository, AuthenticationRepository | Users, Authentications | Implemented | AuthController.java, Users table |
| FR-02 | User Profile | Customer | /profile | /api/v1/profile | ProfileController | UserService | UserRepository | Users | Implemented | ProfileController.java, Users table |
| FR-03 | KYC | Customer, Staff | /account/kyc | /api/v1/kyc, /api/v1/staff/kyc | KycController, StaffKycController | KycService | KycRequestRepository | KYCRequests | Implemented | KycController.java, KYCRequests table |
| FR-04 | Seller Registration | Customer, Staff | /account/register-shop | /api/v1/shop-registrations | ShopRegistrationController | ShopRegistrationService | SellerRegistrationRepository | SellerRegistrations | Implemented | ShopRegistrationController.java |
| FR-05 | Shop Follow | Customer | /shop/{id} | /api/products/shop/{id}/follow | ProductSearchController | ProductService | ShopFollowerRepository | ShopFollowers | Implemented | ProductSearchController.java, ShopFollowers DB |
| FR-06 | Product & Shop Mgt | Seller | /seller/inventory | /api/seller | SellerController | SellerService | ProductRepository, CategoryRepository | Categories, Products, ProductVariants, DigitalAssets | Implemented | SellerController.java, Products table |
| FR-07 | Wallet & Top-up | Customer | /wallet/topup | /api/v1/wallet, /api/sepay | WalletController, TopupController | WalletService, SepayService | WalletTransactionRepository | WalletTransactions, TopupTransactions | Implemented | WalletController.java, TopupTransactions DB |
| FR-08 | Order & Purchase | Customer | /checkout | /api/transactions/purchase | TransactionController | TransactionService | TransactionRepository | Transactions, DigitalAssets | Implemented | TransactionController.java, Transactions table |
| FR-09 | Withdrawal | Seller, Staff | /seller/withdrawals | /api/seller/withdrawals | SellerController | WithdrawalService | WithdrawalRepository | Withdrawals | Implemented | SellerController.java, trg_CheckWithdrawalMin |
| FR-10 | Complaint & Support | Customer, Staff | /account/tickets | /api/complaints | ComplaintController, SupportTicketController | ComplaintService | ComplaintRepository | Complaints, SupportTickets | Implemented | ComplaintController.java, Complaints DB |
| FR-11 | Feedback & Flag | Customer | /account/orders/{id}/feedback | /api/reviews | ReviewController | ReviewService | ReviewRepository | Reviews, ShopFlags | Implemented | ReviewController.java, Reviews table |
| FR-12 | Chat | Customer, Seller, Staff | /messages | /api/chat | ChatController, StaffChatRestController | ChatService | ChatRepository | Chats, ChatBlocks, ChatMutes | Implemented | ChatController.java, Chats table |
| FR-13 | Notification | Customer, Seller | /notifications | /api/notifications | NotificationController | NotificationService | NotificationRepository | Notifications | Implemented | NotificationController.java, Notifications DB |
| FR-14 | Staff Management | Admin | /admin/users | /api/admin/staff | StaffController | StaffService | UserRepository | Users, UserPermissions, Permissions | Implemented | StaffController.java, Permissions DB |
| FR-15 | Administration | Admin | /admin | /api/admin/system-config | SystemConfigurationController | SystemConfigService | SystemConfigRepository | SystemConfigurations, AuditLogs | Implemented | SystemConfigurationController.java |
| FR-16 | PreOrder | Customer | /pre-orders | /api/pre-orders | PreOrderController | PreOrderService | PreOrderRepository | PreOrders | Implemented | PreOrderController.java, PreOrders table |

## Appendix B – Screen Inventory

| Screen | Actor | Template File | Route | Backend Handler | Main Function | Status |
|---|---|---|---|---|---|---|
| Home | Guest | /index.html | / | HomeController | Trang chủ | Implemented |
| Search | Guest | /search.html | /search | HomeController | Tìm kiếm sản phẩm | Implemented |
| Product Detail | Guest | /product-detail.html | /products/{id} | HomeController | Xem chi tiết SP | Implemented |
| Cart | Customer | /cart.html | /cart | HomeController | Giỏ hàng tạm | Implemented |
| Checkout | Customer | /checkout.html | /checkout | HomeController | Thanh toán đơn hàng | Implemented |
| Profile | Customer | /account/profile.html | /profile | ProfilePageController | Quản lý hồ sơ | Implemented |
| KYC | Customer | /account/kyc.html | /account/kyc | ProfilePageController | Định danh | Implemented |
| Wallet | Customer | /account/wallet.html | /wallet | ProfilePageController | Quản lý ví | Implemented |
| Top-up | Customer | /account/topup.html | /wallet/topup | ProfilePageController | Nạp tiền | Implemented |
| Order History | Customer | /account/orders.html | /account/orders | ProfilePageController | Lịch sử mua hàng | Implemented |
| Seller Dashboard | Seller | /seller/dashboard.html | /seller/dashboard | SellerPageController | Bảng điều khiển Shop | Implemented |
| Inventory | Seller | /seller/inventory.html | /seller/inventory | SellerPageController | Quản lý kho | Implemented |
| Withdrawals | Seller | /seller/withdrawals.html | /seller/withdrawals | SellerPageController | Rút tiền | Implemented |
| Staff Dashboard | Staff | /staff/dashboard.html | /staff/dashboard | StaffController | Bảng điều khiển Staff | Implemented |
| Admin Users | Admin | /admin/users.html | /admin/users | AdminPageController | Quản lý người dùng | Implemented |

## Appendix C – API and Route Inventory

*Inventory chi tiết đã được trích xuất từ các Annotation @RequestMapping, bao gồm hơn 100 endpoints thuộc 24 Controller. Tham khảo source code src/main/java/controller để biết chi tiết từng Method và Authorization.*
Một số Route quan trọng:
- /api/v1/auth/*: Xác thực và cấp token.
- /api/sepay/webhook: Webhook cộng tiền tự động.
- /api/transactions/purchase: Xử lý giao dịch mua hàng trừ tiền.
- /api/v1/shop-registrations: Xử lý đăng ký cửa hàng.

## Appendix D – Data Model Inventory

| Entity/Table | Purpose | Primary Key | Important Foreign Keys | Related Module | Used By Code | Verified Against Physical DB |
|---|---|---|---|---|---|---|
| Users | Lưu thông tin người dùng | id | - | User Profile | Yes | Yes |
| WalletTransactions | Ghi nhận biến động số dư | id | user_id | Wallet | Yes | Yes |
| Transactions | Lưu đơn hàng/giao dịch mua | id | customer_id, seller_id | Purchase | Yes | Yes |
| DigitalAssets | Tài sản số (Key, Account) | id | variant_id, transaction_id | Product, Purchase | Yes | Yes |
| Products | Thông tin gốc sản phẩm | id | seller_id, category_id | Product Mgt | Yes | Yes |
| ProductVariants | Phân loại sản phẩm (giá, tồn) | id | product_id | Product Mgt | Yes | Yes |
| SystemConfigurations | Cấu hình động hệ thống | id | - | Administration | Yes | Yes (trg_HoldFundsEscrow) |
| Withdrawals | Lệnh rút tiền | id | seller_id | Withdrawal | Yes | Yes |
| SellerRegistrations | Lịch sử duyệt Shop | id | user_id | Shop Registration | Yes | Yes (trg_UpdateShopStatus) |
| Wishlists | Lưu sản phẩm yêu thích | id | user_id, product_id | Wishlist | No (Missing Entity) | Yes (Table exists) |

## Appendix E – Role and Permission Matrix

| Function | Guest | Customer | Seller | Staff | Admin | Permission/Policy Evidence |
|---|---|---|---|---|---|---|
| View Products | Yes | Yes | Yes | Yes | Yes | @PreAuthorize("permitAll()") |
| Top-up | No | Yes | Yes | No | No | WalletController requires Auth |
| Purchase | No | Yes | Yes | No | No | TransactionController.purchase |
| Manage Products | No | No | Yes | No | No | SellerController requires Seller |
| Resolve Complaints| No | No | No | Yes | No | ComplaintController Staff Role |
| Modify System | No | No | No | No | Yes | SystemConfigurationController Admin Role |

## Appendix F – Implementation Status Matrix

| Module | Feature | UI | Backend | Database | Authorization | Runtime Verified | Final Status |
|---|---|---|---|---|---|---|---|
| Authentication | Login, Register | Yes | Yes | Yes | Yes | No (Read-only) | Implemented |
| Purchase | Escrow Hold | Yes | Yes | Yes (Trigger) | Yes | No | Implemented |
| Purchase | Escrow Release | N/A | No (No Cron) | N/A | N/A | No | Partially Implemented |
| Wishlist | Add/Remove | Yes | No | Yes | N/A | No | Broken/Not Implemented |
| Realtime Chat | WebSocket | No | No | No | N/A | No | Not Implemented |
| Wallet | Balance Deduct | Yes | Yes | Yes | Yes | No | Implemented |

## Appendix G – Error and Validation Matrix

| Module | Error Condition | Validation Layer | User Message | Technical Handling | Verified |
|---|---|---|---|---|---|
| Purchase | Insufficient Funds | Service | "Số dư tài khoản không đủ..." | IllegalArgumentException | Yes |
| Withdrawal | Below Minimum | Database Trigger | "Số tiền rút tối thiểu phải là..." | RAISERROR in 	rg_CheckWithdrawalMin | Yes |

## Appendix H – Unverified and Pending Confirmation

- **Wishlist**: Bảng Wishlists tồn tại trong Database, UI có đề cập, nhưng source code Backend không có bất kỳ Entity Wishlist.java hay Controller nào.
- **Escrow Release**: Bảng Transactions có lưu escrow_release_date và có trigger chuyển status sang "Held". Tuy nhiên không tìm thấy @Scheduled job hoặc logic tự động chuyển tiền từ Held sang Available cho Seller sau 72 giờ. Cần xác nhận quy trình giải phóng quỹ.
- **UserPermissions**: Bảng UserPermissions tồn tại trong DB nhưng code không có Entity UserPermission.java (có thể Hibernate @ManyToMany xử lý ẩn, cần chạy test runtime để xác minh hoàn toàn).

