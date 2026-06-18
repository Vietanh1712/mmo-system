# Database Documentation - MMO Market System

## Thông Tin Kết Nối

### Server
- **Database Engine**: SQL Server
- **Database Name**: MMO_System
- **Host**: 
- **Port**: 1433
- **Username**: 
- **Password**: 

## Cấu Trúc Database

### PHẦN 1: TÀI KHOẢN VÀ BẢO MẬT

#### Bảng: Users
| Cột | Loại | Mô tả |
|-----|------|-------|
| id | BIGINT (PK) | ID người dùng |
| email | VARCHAR(255) | Email (UNIQUE) |
| password | VARCHAR(255) | Mật khẩu |
| full_name | NVARCHAR(255) | Họ tên |
| role | NVARCHAR(MAX) | JSON: {"role": "Customer"} hoặc {"role": "Seller"} |
| phone | VARCHAR(20) | Số điện thoại |
| shop_status | VARCHAR(20) | Status cửa hàng (Pending/Active/Banned) |
| balance_vnd | BIGINT | Số dư ví tiền mặt |
| permissions | NVARCHAR(MAX) | Quyền người dùng |
| isVerified | BIT | Trạng thái xác thực email |
| created_at | DATETIME | Ngày tạo |
| isDelete | BIT | Đánh dấu xóa mềm |

#### Bảng: Authentications
Lưu thông tin xác thực từ nhiều provider (System / Google)
- **Foreign Key**: user_id → Users(id)

#### Bảng: EmailVerifications
Lưu mã xác thực email
- **Foreign Key**: user_id → Users(id)

---

### PHẦN 2: THÔNG TIN NGƯỜI BÁN VÀ CỬA HÀNG (KYC)

#### Bảng: SellerRegistrations
Đơn đăng ký người bán
- **Foreign Key**: user_id → Users(id)
- **Status**: Pending, Approved, Rejected

#### Bảng: SellerBankInfo
Thông tin ngân hàng của người bán
- **Foreign Key**: user_id → Users(id)

---
### PHẦN 3: QUẢN LÝ SẢN PHẨM VÀ KHO SỐ

#### Bảng: Categories
Danh mục sản phẩm

#### Bảng: Products
| Cột | Loại | Mô tả |
|-----|------|-------|
| id | BIGINT (PK) | ID sản phẩm |
| seller_id | BIGINT (FK) | ID người bán |
| category_id | BIGINT (FK) | ID danh mục |
| name | NVARCHAR(255) | Tên sản phẩm |
| description | NVARCHAR(MAX) | Mô tả sản phẩm |
| image | VARCHAR(255) | Đường dẫn hình ảnh |
| created_at | DATETIME | Ngày tạo |
| isDelete | BIT | Đánh dấu xóa mềm |

#### Bảng: ProductVariants
Các biến thể sản phẩm (kích cỡ, màu sắc, phiên bản...)
| Cột | Loại | Mô tả |
|-----|------|-------|
| id | BIGINT (PK) | ID biến thể |
| product_id | BIGINT (FK) | ID sản phẩm |
| variant_name | NVARCHAR(255) | Tên biến thể |
| price_vnd | BIGINT | Giá bán (VNĐ) |
| stock | INT | Số lượng tồn kho |
| status | VARCHAR(20) | Trạng thái |

#### Bảng: DigitalAssets
Tài sản kỹ thuật số (account, key, license...)
- **Foreign Key**: variant_id → ProductVariants(id), transaction_id → Transactions(id)
- **asset_data**: Dữ liệu tài khoản/key bảo mật

---

### PHẦN 4: GIAO DỊCH VÀ VÍ ĐIỆN TỬ (TÀI CHÍNH)

#### Bảng: TopupTransactions
Giao dịch nạp tiền vào ví
| Cột | Loại | Mô tả |
|-----|------|-------|
| id | BIGINT (PK) | ID giao dịch nạp |
| user_id | BIGINT (FK) | ID người dùng |
| amount_vnd | BIGINT | Số tiền nạp |
| sepay_code | VARCHAR(255) | Mã giao dịch SePay |
| status | VARCHAR(20) | Pending/Completed/Failed |

#### Bảng: Transactions
Giao dịch mua - bán sản phẩm (HỖ TRỢ ESCROW 3 NGÀY)
| Cột | Loại | Mô tả |
|-----|------|-------|
| id | BIGINT (PK) | ID giao dịch |
| customer_id | BIGINT (FK) | ID khách hàng |
| seller_id | BIGINT (FK) | ID người bán |
| product_id | BIGINT (FK) | ID sản phẩm |
| variant_id | BIGINT (FK) | ID biến thể |
| amount_vnd | BIGINT | Giá sản phẩm |
| commission_vnd | BIGINT | Hoa hồng sàn |
| status | VARCHAR(20) | Pending/Held/Completed |
| escrow_release_date | DATETIME | Ngày giải phóng tiền (3 ngày) |

#### Bảng: Withdrawals
Yêu cầu rút tiền của người bán
| Cột | Loại | Mô tả |
|-----|------|-------|
| id | BIGINT (PK) | ID yêu cầu rút |
| seller_id | BIGINT (FK) | ID người bán |
| bank_info_id | BIGINT (FK) | ID thông tin ngân hàng |
| amount_vnd | BIGINT | Số tiền rút (≥ 50,000) |
| status | VARCHAR(20) | Pending/Completed |
| proof_file | VARCHAR(255) | Tệp chứng minh |

#### Bảng: WalletTransactions
Lịch sử giao dịch ví (chi tiết mỗi thay đổi số dư)
| Cột | Loại | Mô tả |
|-----|------|-------|
| transaction_type | VARCHAR(50) | DEPOSIT/WITHDRAW/PURCHASE/SALE/REFUND |
| balance_after | BIGINT | Số dư sau giao dịch |

---

### PHẦN 5: CHĂM SÓC KHÁCH HÀNG VÀ KIỂM DUYỆT

#### Bảng: Complaints
Khiếu nại từ khách hàng
- **Status**: Open/In_Progress/Resolved/Closed
- **Foreign Key**: transaction_id, customer_id, seller_id → Users(id)

#### Bảng: ShopFlags
Cảnh báo cửa hàng (Warning/Suspension/Ban)
- **flag_level**: Warning, Suspension, Ban
- **Foreign Key**: seller_id, staff_id, complaint_id

#### Bảng: Chats
Tin nhắn giữa người dùng (hỗ trợ chat khiếu nại)
- **chat_type**: Normal / Complaint
- **Foreign Key**: sender_id, receiver_id, complaint_id

---

### PHẦN 6: TÍNH NĂNG MỞ RỘNG

#### Bảng: Wishlists
Danh sách yêu thích sản phẩm
- **UNIQUE**: (customer_id, product_id)

#### Bảng: PreOrders
Đặt trước sản phẩm
- **Status**: Pending/Approved/Rejected

#### Bảng: Reviews
Đánh giá sản phẩm
- **rating**: 1-5 sao
- **Foreign Key**: product_id, user_id → Users(id)

---

### PHẦN 7: HỆ THỐNG VÀ KIỂM TOÁN

#### Bảng: SystemConfigurations
Cấu hình hệ thống
- **DEFAULT_COMMISSION_PERCENT**: 5.0 (mặc định)
- **MIN_WITHDRAWAL_VND**: 50000
- **MAINTENANCE_MODE**: FALSE/TRUE

#### Bảng: Notifications
Thông báo cho người dùng

#### Bảng: AuditLogs
Ghi lại thao tác của người dùng

---

## Triggers Nghiệp Vụ (SQL Server)

### 1. trg_CheckWithdrawalMin
- **Mục đích**: Kiểm tra số tiền rút ≥ 50,000 VNĐ
- **Sự kiện**: AFTER INSERT on Withdrawals

### 2. trg_HoldFundsEscrow
- **Mục đích**: Tự động giữ tiền 3 ngày khi tạo giao dịch
- **Sự kiện**: AFTER INSERT on Transactions

### 3. trg_UpdateWithdrawalProof
- **Mục đích**: Tự động sinh tên file chứng minh khi rút tiền Completed
- **Sự kiện**: AFTER UPDATE on Withdrawals

### 4. trg_UpdateShopStatus
- **Mục đích**: Tự động cấp quyền Customer_Seller khi đơn đăng ký được Approved
- **Sự kiện**: AFTER UPDATE on SellerRegistrations

---

## Backup & Recovery

- **Vị trí backup**: 
- **Tần suất backup**: 
- **Hướng dẫn restore**: 

---

## Ghi chú khác

- **Xóa mềm**: Hầu hết bảng có cột `isDelete` để đánh dấu xóa mềm thay vì xóa vật lý
- **Thời gian**: Tất cả bảng dùng `DATETIME DEFAULT GETDATE()` cho thời gian tạo/cập nhật
- **Escrow Policy**: Giao dịch bị giữ tiền trong 3 ngày trước khi nhà bán được nhận tiền
- **Commission**: Mặc định 5% hoa hồng từ mỗi giao dịch


