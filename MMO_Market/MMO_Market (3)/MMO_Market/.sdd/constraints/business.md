# Constraints — Business Rules

> **Phạm vi**: Ràng buộc nghiệp vụ (business constraints) cho hệ thống MMO Market.
> Đây là **luật nghiệp vụ sống còn** — mọi spec, code, test phải tuân thủ; vi phạm = bug nghiêm trọng.
> Liên quan: [`global.md`](./global.md) (ràng buộc kỹ thuật/tech stack), [`safety.md`](./safety.md) (ràng buộc an toàn vận hành), [`constitution.md`](../constitution.md).

---

## 1. Authentication, Authorization & RBAC

| ID | Rule | Rationale |
|----|------|-----------|
| BIZ-AUTH-01 | Mọi API (ngoại trừ các endpoint public như `/api/auth/register`, `/api/auth/login`, `/api/auth/verify-otp`, `/api/auth/resend-otp`, `/api/auth/forgot-password`, `/api/auth/reset-password` và `/api/search/**`) **phải** đi qua bộ lọc Spring Security JWT filter. | Ngăn chặn truy cập trái phép vào tài nguyên nhạy cảm. |
| BIZ-AUTH-02 | Mật khẩu người dùng được băm bằng thuật toán `bcrypt` với cost rate **≥ 12**. | Chống brute-force và cầu vồng bảng (rainbow table). |
| BIZ-AUTH-03 | Phân quyền và kiểm tra vai trò người dùng phải thực hiện nghiêm ngặt ở cả 2 cấp độ: Vai trò chung (Role: `CUSTOMER`, `SELLER`, `STAFF`, `ADMIN`) và Quyền hạn chi tiết (Permissions: `MANAGE_CATEGORIES`, `RESOLVE_COMPLAINT`, v.v.). | Đảm bảo tính nhất quán của mô hình RBAC (Role-Based Access Control). |
| BIZ-AUTH-04 | Giao diện hiển thị (Frontend) ẩn nút/menu chỉ nhằm tăng trải nghiệm người dùng (UX). Tầng Backend **bắt buộc** kiểm tra quyền và trả về lỗi `401 Unauthorized` hoặc `403 Forbidden` khi không đủ thẩm quyền. | Ngăn chặn lỗ hổng vượt quyền (Authorization Bypass) qua UI. |
| BIZ-AUTH-05 | Kiểm tra quyền sở hữu bản ghi (Ownership validation) ở Service Layer trước khi cho phép Seller chỉnh sửa hoặc xóa các sản phẩm, biến thể, hoặc cấu hình gian hàng của họ. | Tránh lỗi leo thang đặc quyền ngang (Horizontal Privilege Escalation). |
| BIZ-AUTH-06 | Nhân viên vận hành (`STAFF`) và Quản trị viên (`ADMIN`) **tuyệt đối không** được phép thực hiện hành động mua hàng (Make Order) trên hệ thống. | Tránh xung đột lợi ích và bảo đảm an toàn dữ liệu kinh doanh. |

---

## 2. Wallet & Financial Rules (Ràng buộc Tài chính & Ví)

| ID | Rule | Rationale |
|----|------|-----------|
| BIZ-FIN-01 | Đơn vị tiền tệ duy nhất được sử dụng trong hệ thống là Việt Nam Đồng (VNĐ). Tuyệt đối **không** tạo hoặc sử dụng coin ảo, point, hay các đồng tiền trung gian khác. | Giảm thiểu rủi ro pháp lý và đơn giản hóa đối soát tài chính. |
| BIZ-FIN-02 | Số dư tài khoản trong ví người dùng (`User`) phải được phân chia thành hai thuộc tính độc lập: Số dư khả dụng (**`balance_vnd`**) và Số dư đóng băng/tạm giữ (**`deposit_vnd`**). | Đảm bảo tiền giam giữ do khiếu nại hoặc lệnh rút đang chờ xử lý không bị tiêu lạm. |
| BIZ-FIN-03 | Giá trị tiền tệ trong Cơ sở dữ liệu bắt buộc lưu trữ dưới kiểu số nguyên lớn (**`BIGINT`**), tương ứng với kiểu **`Long`** trong mã nguồn Java. | Tránh lỗi làm tròn số học (floating-point precision issues). |
| BIZ-FIN-04 | Mọi tác vụ thay đổi số dư ví người dùng bắt buộc phải được bọc trong `@Transactional(rollbackFor = Exception.class)` ở Service Layer và sử dụng khóa bi quan (**Pessimistic Locking**: `SELECT FOR UPDATE` hoặc `findByIdForUpdate`) để tránh Race Condition. | Bảo toàn tính toàn vẹn số dư ví khi có nhiều luồng giao dịch đồng thời. |
| BIZ-FIN-05 | Yêu cầu nạp tiền (Top-Up) qua VietQR hỗ trợ SePay Webhook tự động cộng tiền khi người dùng chuyển khoản chính xác nội dung định danh. Hệ thống phải kiểm tra chữ ký số bảo mật của webhook và cơ chế chống xử lý trùng (Idempotency Check). | Chống nạp khống tiền và trùng lặp giao dịch nạp. |
| BIZ-FIN-06 | Số tiền nạp tối thiểu mỗi lần là **10,000 VNĐ**. Hạn mức rút tiền tối thiểu, tối đa và phí dịch vụ rút tiền bắt buộc cấu hình động thông qua bảng `SystemConfigurations`. | Đảm bảo tính linh hoạt khi vận hành và quản lý dòng tiền. |

---

## 3. Escrow (Giam giữ tiền & Giải phóng ví)

| ID | Rule | Rationale |
|----|------|-----------|
| BIZ-ESC-01 | Khi đơn hàng mua sản phẩm số thành công, toàn bộ số tiền thanh toán (sau khi trừ phí commission của sàn) sẽ bị giam giữ trong ví hệ thống dưới dạng đóng băng (`deposit_vnd` của Seller) ở trạng thái giao dịch **`Held`**. | Thực thi cơ chế bảo lãnh giao dịch (Escrow) bảo vệ người mua. |
| BIZ-ESC-02 | Thời gian giam tiền Escrow được tính toán động (cộng thêm vào cột `escrow_release_date` của transaction): Mặc định là **72 giờ**; hoặc tăng lên **168 giờ (7 ngày)** đối với: Shop mới dưới 20 đơn hàng thành công, Shop bị cảnh cáo Level 0, hoặc Shop có tỷ lệ khiếu nại đúng từ 2% trở lên. | Giảm thiểu rủi ro lừa đảo từ các shop kém uy tín hoặc shop mới. |
| BIZ-ESC-03 | Tiền giam giữ chỉ được giải phóng (cộng vào số dư khả dụng `balance_vnd` và trừ khỏi số dư đóng băng `deposit_vnd` của Seller) khi: Hết thời gian giam giữ Escrow và không có khiếu nại, HOẶC Người mua chủ động nhấn nút "Hoàn thành sớm" đơn hàng. | Bảo vệ dòng tiền hợp pháp cho bên bán khi giao dịch thành công tốt đẹp. |

---

## 4. Product & Digital Asset Management

| ID | Rule | Rationale |
|----|------|-----------|
| BIZ-PROD-01 | Nội dung tài sản số nhạy cảm (như Giftcode, Key Game, tài khoản đăng nhập) của Seller bắt buộc phải được mã hóa trước khi lưu trữ vào database (`DigitalAssets` table) và chỉ được giải mã động khi phân phát cho khách hàng. | Chống rò rỉ dữ liệu (Data Leak) khi cơ sở dữ liệu bị tấn công. |
| BIZ-PROD-02 | Việc phân phối tài sản số được thực hiện tự động theo nguyên tắc **FIFO** (Vào trước, ra trước). Khi đơn hàng thanh toán thành công, hệ thống gán cờ `isUsed = 1`, liên kết với `transaction_id` và giải mã trả về thông tin cho người mua. | Đảm bảo giao hàng nhanh chóng, không trùng lặp và minh bạch. |
| BIZ-PROD-03 | Số lượng tồn kho (stock) của biến thể sản phẩm (`ProductVariant`) phải được cập nhật đồng bộ tăng/giảm tự động tương ứng với số lượng tài sản số khả dụng đang lưu trữ. | Tránh tình trạng bán khống sản phẩm (Over-selling) khi hết hàng trong kho số. |

---

## 5. Dispute & Complaint Resolution (Quy trình Khiếu nại)

| ID | Rule | Rationale |
|----|------|-----------|
| BIZ-COMP-01 | Người mua có quyền mở khiếu nại (Complaint) trong khoảng thời gian tiền đang bị giam giữ (Escrow). Khi khiếu nại được tạo, lệnh giải phóng tiền Escrow của giao dịch đó bị **đóng băng vô thời hạn** cho tới khi có phán quyết cuối cùng. | Đảm bảo tiền không bị chuyển đi khi đang xảy ra tranh chấp. |
| BIZ-COMP-02 | Khi bắt đầu xử lý khiếu nại (`start-dispute`), hệ thống kích hoạt phòng chat đối chất trực tiếp giữa Người mua và Người bán. Nhân viên hỗ trợ (`STAFF`) và Quản trị viên (`ADMIN`) chỉ có quyền **Read-only** để giám sát và đưa ra quyết định, không nhắn tin trực tiếp trong phòng chat này. | Đảm bảo tính khách quan và minh bạch của quy trình đối chất. |
| BIZ-COMP-03 | Quyết định phân định khiếu nại từ Staff/Admin: Nếu phê duyệt khiếu nại (Approved) -> Hệ thống tự động hoàn trả 100% tiền giao dịch về ví khả dụng (`balance_vnd`) của Buyer; Nếu từ chối khiếu nại (Rejected) -> Hệ thống lập tức giải phóng tiền giam giữ sang ví khả dụng của Seller. | Hoàn thành chu trình giải quyết tranh chấp tài chính tự động. |

---

## 6. Dữ liệu & Soft Delete

| ID | Rule | Rationale |
|----|------|-----------|
| BIZ-DATA-01 | Áp dụng cơ chế **Soft Delete** (Xóa mềm) bằng cách thiết lập cờ `isDelete = 1` trên toàn bộ các thực thể quan trọng (Users, Products, ProductVariants, Categories, Transactions, Complaints, Withdrawals). Tuyệt đối **cấm** dùng lệnh `DELETE` vật lý. | Bảo toàn lịch sử dữ liệu, phục vụ kiểm toán tài chính và phục hồi thông tin. |
| BIZ-DATA-02 | Mọi câu lệnh truy vấn nghiệp vụ (SELECT) của ứng dụng bắt buộc phải lọc điều kiện **`isDelete = 0`** (hoặc `isDelete = false` trong Java code). | Ngăn chặn hiển thị dữ liệu đã bị xóa ra phía người dùng. |
| BIZ-DATA-03 | Trigger trên SQL Server bắt buộc phải xử lý set-based thông qua hai bảng ảo `inserted` và `deleted` để hỗ trợ batch update/insert. Cấm sử dụng row-by-row cursor logic. | Đảm bảo hiệu năng DB khi chạy batch update hoặc nhập hàng số lượng lớn. |
| BIZ-DATA-04 | Mọi thao tác hành chính nhạy cảm của Admin/Staff (cấu hình hệ thống, phán quyết khiếu nại, duyệt rút tiền, khóa tài khoản) phải được lưu trữ vết trong bảng nhật ký kiểm toán (Audit Logs). | Trách nhiệm giải trình và an toàn vận hành hệ thống. |

---

## Tham chiếu

| Nguồn | Nội dung liên quan |
|-------|---------------------|
| `.sdd/constitution.md` | Điều 1 (Tech Stack), Điều 3 (Chính sách bảo mật & tài chính) |
| `AGENTS.md` | Mục 2 (Domain Rules), Mục 4 (Golden Patterns) |
| `.sdd/specs/backend/` | Chi tiết specs của `feat-wallet`, `feat-order`, `feat-complaint` |
| `constraints/global.md` | Ràng buộc kỹ thuật hệ thống |
| `constraints/safety.md` | Ràng buộc an toàn vận hành |
