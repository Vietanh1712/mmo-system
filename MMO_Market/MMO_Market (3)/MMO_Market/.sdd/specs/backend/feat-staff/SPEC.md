# SPEC — Staff Operations & Approvals
> **Feature ID:** `feat-staff`
> **Version:** 2.7 | **Status:** Active
> **Author:** Team | **Last Updated:** 2026-07-12

---

## 1. CONTEXT & GOAL (BỐI CẢNH & MỤC TIÊU)
Hỗ trợ Staff kiểm duyệt KYC, phê duyệt/từ chối các yêu cầu mở Shop của Seller, kiểm soát cờ cảnh báo (Flags) cho các Shop vi phạm chính sách, theo dõi lịch sử giao dịch toàn sàn, và giải quyết khiếu nại (Complaints) của người mua.

---

## 2. ACTORS (TÁC NHÂN)
| Actor | Role | Điều kiện tiền quyết |
|---|---|---|
| **Staff** | Nhân viên vận hành, kiểm duyệt KYC, phê duyệt rút tiền, cắm cờ cảnh báo Shop, xử lý khiếu nại của người dùng. | Tài khoản đăng nhập hợp lệ với vai trò `ROLE_STAFF`. |
| **Admin** | Quản trị viên hệ thống có toàn quyền của Staff cộng thêm các chức năng cấu hình hệ thống, xem báo cáo doanh thu toàn sàn. | Tài khoản đăng nhập hợp lệ với vai trò `ROLE_ADMIN`. |

---

## 3. FUNCTIONAL REQUIREMENTS (Cú pháp EARS)
| ID | EARS Requirement |
|---|---|
| **FR-STF-01** | WHEN a Staff retrieves the dashboard, THE SYSTEM SHALL calculate and display summary statistics (total shops, pending KYC, active/removed flags, pending withdrawals). |
| **FR-STF-02** | WHEN a Staff reviews a KYC request, THE SYSTEM SHALL update the KYCStatus and if approved, set the user's `isVerified = 1`. |
| **FR-STF-03** | WHEN a Staff reviews a shop registration request, THE SYSTEM SHALL approve or reject the request and automatically update the corresponding user's store status. |
| **FR-STF-04** | WHEN a Staff toggles a shop's active status, THE SYSTEM SHALL change the user's shop status between `Active` and `Banned`. |
| **FR-STF-05** | WHEN a Staff updates a Shop Flag, THE SYSTEM SHALL save the new severity level (Warning, Critical, Danger), status, and reason. |
| **FR-STF-06** | WHEN a Staff resolves a complaint, THE SYSTEM SHALL update the complaint status based on the selected value from the dropdown select element (`InProgress`, `Resolved`, `Rejected`), and update the corresponding escrow ledger. |
| **FR-STF-07** | WHEN a Staff requests their own permissions, THE SYSTEM SHALL query the database for user permissions and return a list of assigned permission strings. |
| **FR-STF-08** | WHEN an Admin retrieves staff permissions, THE SYSTEM SHALL query all permissions or specific permissions assigned to a given Staff ID. |
| **FR-STF-09** | WHEN an Admin assigns permissions, THE SYSTEM SHALL create mappings between specified Staff IDs and Permission names. |
| **FR-STF-10** | WHEN an Admin revokes permissions, THE SYSTEM SHALL remove mappings between a specified Staff ID and Permission names. |
| **FR-STF-11** | WHEN a Staff retrieves the documents dashboard, THE SYSTEM SHALL calculate and display the number of pending support tickets. |
| **FR-STF-12** | WHEN a Staff searches KYC requests, THE SYSTEM SHALL perform a multi-field search across request code, ID number, user full name, and email, automatically stripping any leading '#' character. |
| **FR-STF-13** | WHEN a Staff requests KYC statistics, THE SYSTEM SHALL return aggregated counts for total, pending, approved, and rejected KYC requests. |
| **FR-STF-14** | WHEN a Staff changes KYC filter dropdown options, THE SYSTEM SHALL NOT auto-trigger search until the Staff clicks the Search button or presses Enter. |
| **FR-STF-15** | WHEN a Staff manages shop account statuses, THE SYSTEM SHALL support 5 distinct status values with standardized Vietnamese display labels: Active ("Hoạt động"), Suspended ("Tạm ngưng"), Locked ("Tạm khóa"), Banned ("Khóa vĩnh viễn"), and Withdrawn ("Đã đóng Shop (Hoàn cọc)"). |
| **FR-STF-16** | WHEN a Staff accesses category management, THE SYSTEM SHALL allow Staff to view, search, create, update, and toggle active/deleted status (`is_delete`) of parent and child product categories. |
| **FR-STF-17** | WHEN a Staff manages support tickets, THE SYSTEM SHALL display all support requests, allow filtering by status and category, and enable Staff to respond and transition ticket statuses (`OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`). |
| **FR-STF-18** | WHEN a Staff updates a shop status to 'Suspended' with an expiration timestamp (`suspendedUntil`), THE SYSTEM SHALL save `suspended_until` in the Users table and automatically revert the shop status to 'Active' once the timestamp expires via scheduled job or read-trigger. |
| **FR-STF-19** | WHEN a Staff views `/staff/topups`, THE SYSTEM SHALL query and display all SePay top-up transactions, filter by status, search by keyword/ID/SePay code, and calculate top statistics (total, success, failed, total VND). |
| **FR-STF-20** | WHEN SePay webhook fails due to incorrect syntax, missing user ID, or amount outside configured min/max deposit limits, THE SYSTEM SHALL save a `TopupTransaction` record with status `Failed` recording the raw transfer content and failure reason. |
| **FR-STF-21** | WHEN a Staff manually retries/activates a `Failed` or `Pending` top-up transaction via `POST /api/v1/staff/topups/{id}/retry`, THE SYSTEM SHALL validate the target user, credit the user balance, record a `WalletTransaction` ledger, update the top-up status to `Success`, log staff note and staff ID, and send a notification to the target user. |
| **FR-STF-22** | WHEN a Staff searches shop registrations, THE SYSTEM SHALL perform a multi-field search across shop name, support email, support phone, category, registration ID, shop code format (e.g., `SHOP-1`), seller full name, and seller email. |

---

## 4. NON-FUNCTIONAL REQUIREMENTS
| ID | Category | Requirement |
|---|---|---|
| **NFR-STF-01** | Security | Mọi hoạt động kiểm duyệt nhạy cảm (duyệt rút tiền, duyệt KYC, cắm cờ shop) bắt buộc phải được bảo vệ phân quyền chặt chẽ thông qua Spring Security `@PreAuthorize("hasAnyAuthority('ROLE_STAFF', 'ROLE_ADMIN')")`. |
| **NFR-STF-02** | UX/UI | Các bảng danh sách hiển thị của Staff phải được hỗ trợ phân trang thống nhất và sắp xếp theo thời gian tạo mới nhất trước (`createdAt DESC`). |

---

## 5. DATA MODEL (Mô hình dữ liệu)

```sql
CREATE TABLE ShopFlags (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    shop_id BIGINT NOT NULL,
    reason NVARCHAR(MAX) NOT NULL,
    flagLevel VARCHAR(50) NOT NULL, -- Warning | Critical | Danger
    status VARCHAR(50) DEFAULT 'Effect', -- Effect | Removed
    created_by BIGINT NOT NULL,
    created_at DATETIME DEFAULT GETDATE(),
    isDelete BIT DEFAULT 0,
    CONSTRAINT FK_Flag_Shop FOREIGN KEY(shop_id) REFERENCES Users(id),
    CONSTRAINT FK_Flag_Creator FOREIGN KEY(created_by) REFERENCES Users(id)
);
```

---

## 6. API SPEC (Đặc tả API)

### 6.1 Giao diện MVC (`StaffController`)
*   `GET /staff/dashboard`: Hiển thị trang tổng quan chính (Tổng số Shop, Yêu cầu KYC chờ xử lý, số cờ cảnh báo,...).
*   `GET /staff/documents`: Hiển thị trang tổng quan riêng biệt cho phân hệ "Quản lý đơn từ" (khiếu nại đang mở, rút tiền chờ duyệt, giao dịch).
*   `GET /staff/kyc`: Xem danh sách yêu cầu KYC toàn hệ thống.
*   `GET /staff/kyc/detail`: Xem chi tiết yêu cầu KYC cụ thể.
*   `GET /staff/transactions`: Xem danh sách tất cả các giao dịch trên sàn giao dịch.
*   `GET /staff/transactions/detail`: Xem chi tiết thông tin một giao dịch và thời gian bảo lãnh.
*   `GET /staff/withdrawals`: Xem danh sách yêu cầu rút tiền của các Seller.
*   `GET /staff/withdrawals/detail`: Xem thông tin chi tiết tài khoản ngân hàng và số tiền yêu cầu rút của Seller.
*   `POST /staff/withdrawals/update-status`: Cập nhật trạng thái của lệnh rút tiền.
*   `POST /staff/withdrawals/reject`: Từ chối lệnh rút tiền của Seller.
*   `GET /staff/flags`: Xem danh sách cờ cảnh báo Shop vi phạm.
*   `GET /staff/flags/detail`: Xem chi tiết và cập nhật cờ cảnh báo.
*   `POST /staff/flags/update`: Cập nhật mức độ cảnh báo, lý do cắm cờ hoặc trạng thái của cờ.
*   `POST /staff/flags/remove`: Gỡ bỏ cờ cảnh báo (soft delete).
*   `GET /staff/complaints`: Xem danh sách khiếu nại của người mua.
*   `GET /staff/complaints/detail`: Xem chi tiết cuộc hội thoại khiếu nại, mô tả lỗi, và cập nhật trạng thái xử lý thống nhất qua bộ chọn trạng thái.
*   `GET /staff/shop-registrations`: Xem danh sách tất cả các Shop và bộ lọc.
*   `GET /staff/shop-registrations/detail`: Xem chi tiết thông tin Shop và tích hợp khu vực duyệt/cập nhật trạng thái hoạt động trực tiếp trên cùng một trang.
*   `GET /staff/topups`: Xem danh sách các giao dịch nạp tiền SePay, số liệu thống kê và bộ lọc.
*   `GET /staff/topups/detail`: Xem chi tiết giao dịch nạp tiền, mã SePay, nội dung CK gốc và nhật ký xử lý thủ công.
*   `GET /staff/categories`: Xem trang quản lý danh mục sản phẩm (Category Management).


### 6.2 Các API REST của KYC, Shop & Nạp tiền
*   `GET /api/v1/staff/topups`: Danh sách phân trang giao dịch nạp tiền (`status`, `keyword`, `page`, `size`).
*   `GET /api/v1/staff/topups/stats`: Thống kê giao dịch nạp tiền (`totalTopups`, `successTopups`, `failedTopups`, `pendingTopups`, `totalTopupVnd`).
*   `GET /api/v1/staff/topups/{id}`: Xem chi tiết 1 giao dịch nạp tiền.
*   `POST /api/v1/staff/topups/{id}/retry`: Kích hoạt nạp tiền thủ công cho giao dịch thất bại (`targetUserId`, `skipMinCheck`, `staffNote`).
*   `GET /api/v1/staff/categories`: Danh sách phân trang danh mục sản phẩm kèm từ khóa và trạng thái (`keyword`, `isDelete`).
*   `POST /api/v1/staff/categories`: Tạo mới danh mục (Cha hoặc Con).
*   `PUT /api/v1/staff/categories/{id}`: Cập nhật thông tin danh mục (`name`, `parentId`, `description`).
*   `PATCH /api/v1/staff/categories/{id}/toggle-status`: Chuyển đổi trạng thái Ẩn/Hiện của danh mục (`is_delete`).
*   `GET /api/v1/staff/kyc`: Lọc và phân trang các yêu cầu KYC theo mã hồ sơ, số giấy tờ, tên, email (`status`, `requestCode`, `idType`). Tự động xử lý loại bỏ ký tự `#` ở đầu mã.
*   `GET /api/v1/staff/kyc/stats`: Lấy thống kê số lượng hồ sơ KYC (tổng số, chờ duyệt, đã duyệt, từ chối).
*   `GET /api/v1/staff/kyc/{id}`: Xem chi tiết yêu cầu KYC.
*   `POST /api/v1/staff/kyc/{id}/review`: Phê duyệt hoặc từ chối yêu cầu KYC kèm lý do từ chối (`status`, `rejectionReason`, `version`).
*   `GET /api/v1/shop-registrations`: Danh sách phân trang yêu cầu đăng ký shop.
*   `GET /api/v1/shop-registrations/{id}`: Chi tiết yêu cầu đăng ký Shop.
*   `GET /api/v1/shop-registrations/stats`: Thống kê 6 chỉ số của Shop (trả về JSON dạng Map gồm `totalShops`, `totalDeposit`, `permanentBannedShops`, `indefiniteLockedShops`, `temporarySuspendedShops`, `withdrawnShops`).
*   `GET /api/v1/shop-registrations/statuses`: Lấy danh sách các trạng thái yêu cầu duyệt shop duy nhất.
*   `GET /api/v1/shop-registrations/shop-statuses`: Lấy danh sách các trạng thái tài khoản shop duy nhất.
*   `PUT /api/v1/shop-registrations/{id}/review`: Duyệt hoặc từ chối yêu cầu mở shop (`approved`, `reason`).
*   `PUT /api/v1/shop-registrations/{id}/toggle-status`: Bật/Tắt trạng thái hoạt động của Shop (Banned / Active).
*   `PUT /api/v1/shop-registrations/{id}/update-status`: Cập nhật trạng thái cụ thể của Shop (`status`).

### 6.3 Các API REST phân quyền Staff & Admin
*   `GET /api/staff/my-permissions`: Lấy danh sách quyền hạn của Staff hiện tại đang đăng nhập.
*   `GET /api/admin/staff-permissions/permissions`: Lấy danh sách tất cả các quyền hạn hiện có (chỉ dành cho Admin).
*   `GET /api/admin/staff-permissions/staffs/{staffId}`: Lấy danh sách quyền hạn đã được gán cho một Staff cụ thể (chỉ dành cho Admin).
*   `GET /api/admin/staff-permissions/all-assigned`: Lấy toàn bộ danh sách gán quyền của tất cả Staff (chỉ dành cho Admin).
*   `POST /api/admin/staff-permissions/assign`: Gán thêm các quyền hạn mới cho danh sách Staff (chỉ dành cho Admin).
*   `POST /api/admin/staff-permissions/revoke`: Thu hồi các quyền hạn của một Staff cụ thể (chỉ dành cho Admin).

---

## 7. ERROR HANDLING (Xử lý lỗi)
| HTTP Code | Tình huống | Thông điệp lỗi |
|---|---|---|
| `401 Unauthorized` | Người dùng chưa đăng nhập hệ thống. | `Chưa đăng nhập` |
| `403 Forbidden` | Người dùng không có vai trò STAFF hoặc ADMIN truy cập vào các API quản trị. | `Truy cập bị từ chối` |
| `404 Not Found` | Không tìm thấy yêu cầu KYC, rút tiền hoặc Shop tương ứng. | `Không tìm thấy tài nguyên yêu cầu` |
| `409 Conflict` | KYC đã được xử lý bởi một Staff khác cùng thời điểm (Optimistic Lock). | `Dữ liệu đã bị thay đổi bởi người khác. Vui lòng tải lại trang.` |

---

## 8. ACCEPTANCE CRITERIA (Tiêu chí nghiệm thu)
*   **AC-STF-01 (Phân quyền truy cập)**: Chỉ có tài khoản có quyền `ROLE_STAFF` hoặc `ROLE_ADMIN` mới có thể truy cập thành công vào các route `/staff/**` và các API `/api/v1/staff/**`. Các role khác (như `CUSTOMER`, `SELLER`) sẽ bị chặn và trả về lỗi 403.
*   **AC-STF-02 (Cập nhật trạng thái khiếu nại thống nhất)**: Khi Staff xem chi tiết khiếu nại, thay đổi trạng thái xử lý trên dropdown (`InProgress`, `Resolved`, `Rejected`) và nhấn nút "Cập nhật", hệ thống phải lưu chính xác trạng thái đã chọn và chuyển hướng thành công.
*   **AC-STF-03 (Bảo toàn số thứ tự STT)**: Tất cả bảng dữ liệu của Staff (KYC, Giao dịch, Rút tiền, Cắm cờ, Duyệt Shop, Khiếu nại) hiển thị cột STT chính xác theo công thức liên tục qua các trang: `currentPage * pageSize + index + 1`.
*   **AC-STF-04 (Ẩn cột Lý do trong bảng cờ cảnh báo)**: Giao diện danh sách cờ cảnh báo (`/staff/flags`) không hiển thị cột "Lý do" để tránh làm vỡ layout của bảng do nội dung lý do quá dài. Nội dung lý do này chỉ hiển thị trong trang chi tiết cờ cảnh báo (`/staff/flags/detail`).
*   **AC-STF-05 (Thống kê Nạp tiền cần xử lý)**: API `StaffDashboardService.getDashboardData()` truy vấn đếm tổng số lượng giao dịch nạp tiền thất bại/chờ xử lý (`Failed` và `Pending`) và trả về qua thuộc tính `pendingTopups` của `StaffDashboardDTO`.