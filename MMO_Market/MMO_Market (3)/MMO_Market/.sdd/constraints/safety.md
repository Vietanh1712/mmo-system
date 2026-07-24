# Constraints — Safety (Operational)

> **Phạm vi**: Ràng buộc an toàn vận hành cho hệ thống MMO Market.
> Các quy tắc này bảo vệ hệ thống, dữ liệu tài chính và người dùng khỏi các thao tác
> nguy hiểm, sự cố không thể phục hồi, và hành vi không xác định.
> Liên quan: [`global.md`](./global.md) (ràng buộc kỹ thuật/tech stack), [`business.md`](./business.md) (ràng buộc nghiệp vụ), [`constitution.md`](../constitution.md).

---

## 1. Bảo toàn Dữ liệu & Tài chính (Data & Financial Safety)

| ID | Rule | Mức độ | Rationale |
|----|------|--------|-----------|
| SAFE-DATA-01 | **TUYỆT ĐỐI KHÔNG** chạy `DELETE FROM` trực tiếp trên DB production — bắt buộc dùng Soft Delete (`isDelete = 1`). | 🔴 CRITICAL | Tránh mất mát dữ liệu giao dịch và thông tin người dùng vĩnh viễn. |
| SAFE-DATA-02 | **TUYỆT ĐỐI KHÔNG** chạy lệnh `UPDATE` hàng loạt (bulk UPDATE) tác động lên số dư ví (`balance_vnd`, `deposit_vnd`) trên DB production mà không có điều kiện `WHERE` chặt chẽ hoặc chạy ngoài transaction kiểm soát. | 🔴 CRITICAL | Tránh lỗi ghi đè số dư ví toàn sàn, gây thiệt hại tài chính nghiêm trọng. |
| SAFE-DATA-03 | **TUYỆT ĐỐI KHÔNG** chạy lệnh `TRUNCATE TABLE` trên các bảng tài chính (`users`, `transactions`, `wallet_transactions`, `withdrawals`). | 🔴 CRITICAL | Lệnh này bypass trigger và không thể rollback trong transaction thường. |
| SAFE-DATA-04 | Mọi thay đổi schema database phải đi kèm script migration (T-SQL) được review và chạy thử nghiệm ở môi trường Staging. | 🔴 CRITICAL | Đảm bảo tính đồng bộ giữa source code Java và database thực tế. |
| SAFE-DATA-05 | Lịch sử giao dịch mua bán (`Transactions`) và biến động số dư (`WalletTransactions`) sau khi tạo là **bất biến** (Immutable). Tuyệt đối không cho phép API sửa đổi số tiền hay đối tượng giao dịch. | 🔴 CRITICAL | Ngăn chặn gian lận tài chính và phục vụ kiểm toán đối soát. |
| SAFE-DATA-06 | Bắt buộc thực hiện sao lưu (Backup) cơ sở dữ liệu định kỳ hằng ngày và trước mỗi đợt chạy script migration lớn trên production. | 🟠 HIGH | Phòng ngừa sự cố hỏng dữ liệu không thể phục hồi. |

---

## 2. An toàn Triển khai & Cấu hình (Deployment Safety)

| ID | Rule | Mức độ | Rationale |
|----|------|--------|-----------|
| SAFE-DEPLOY-01 | **TUYỆT ĐỐI KHÔNG** commit và push trực tiếp code vào branch `main` hoặc `production`. Mọi thay đổi phải tạo Pull Request (PR) và được duyệt bởi ít nhất 1-2 thành viên. | 🔴 CRITICAL | Đảm bảo chất lượng code và tránh đưa lỗi ngớ ngẩn lên server. |
| SAFE-DEPLOY-02 | **TUYỆT ĐỐI KHÔNG** cấu hình `spring.jpa.hibernate.ddl-auto=create` hoặc `update` ở môi trường production. | 🔴 CRITICAL | Hibernate auto-DDL có thể drop table hoặc thay đổi cấu trúc bảng ngoài ý muốn dẫn đến mất sạch dữ liệu. |
| SAFE-DEPLOY-03 | **TUYỆT ĐỐI KHÔNG** commit các file cấu hình chứa mật khẩu, private key (`.env`, `application-prod.yml`) lên GitHub. | 🔴 CRITICAL | Lộ thông tin tài khoản ngân hàng, Google API key, JWT Secret. |
| SAFE-DEPLOY-04 | Hạn chế thực hiện deploy hoặc cập nhật hệ thống lớn vào giờ cao điểm giao dịch (8:00 - 22:00) để giảm thiểu ảnh hưởng đến người dùng mua bán. | 🟡 MEDIUM | Giảm thiểu gián đoạn trải nghiệm người dùng. |

---

## 3. An toàn Xác thực & Quyền hạn (Auth & Access Safety)

| ID | Rule | Mức độ | Rationale |
|----|------|--------|-----------|
| SAFE-AUTH-01 | **TUYỆT ĐỐI KHÔNG** bypass các lớp kiểm tra của Spring Security JWT Filter trên các API nhạy cảm. | 🔴 CRITICAL | Tránh lỗ hổng truy cập API không cần đăng nhập. |
| SAFE-AUTH-02 | **TUYỆT ĐỐI KHÔNG** tin tưởng và sử dụng trực tiếp các thông tin tính toán tiền tệ, hoa hồng, phân quyền gửi lên từ Frontend. Mọi phép tính phải được tính và xác thực lại ở Backend. | 🔴 CRITICAL | Frontend là môi trường không an toàn (untrusted client) dễ bị can thiệp. |
| SAFE-AUTH-03 | **TUYỆT ĐỐI KHÔNG** lưu trữ mật khẩu người dùng hoặc khóa JWT Secret dưới dạng văn bản thô (Plain Text). Mật khẩu phải băm qua bcrypt. | 🔴 CRITICAL | Bảo vệ tài khoản người dùng kể cả khi DB bị lộ lọt. |
| SAFE-AUTH-04 | Kiểm tra quyền sở hữu bản ghi (Ownership Validation) bắt buộc thực thi ở tầng Service. Seller chỉ được sửa sản phẩm của chính họ; Customer chỉ được xem ví của chính họ. | 🔴 CRITICAL | Chống lỗi leo thang đặc quyền ngang. |

---

## 4. An toàn Tệp tin & Kho hàng số (Asset & Storage Safety)

| ID | Rule | Mức độ | Rationale |
|----|------|--------|-----------|
| SAFE-FILE-01 | **TUYỆT ĐỐI KHÔNG** lưu trữ trực tiếp file credentials, giftcode, tài khoản số của Seller dưới dạng plain-text vào database. Bắt buộc mã hóa đối xứng trước khi lưu. | 🔴 CRITICAL | Đảm bảo an toàn kho hàng số của Seller trước các vụ hack DB. |
| SAFE-FILE-02 | **TUYỆT ĐỐI KHÔNG** lưu trữ file tải lên (ảnh sản phẩm, ảnh KYC) trực tiếp dưới dạng BLOB trong database SQL Server. | 🔴 CRITICAL | Tránh phình to dung lượng database, làm chậm quá trình backup/restore dữ liệu. |
| SAFE-FILE-03 | Validate chặt chẽ loại tệp tin (Whitelist Extension: `.png`, `.jpg`, `.jpeg`, `.pdf`) và giới hạn dung lượng tải lên tối đa (ví dụ: < 5MB). Sanitize tên file để loại bỏ path traversal (`../`). | 🟠 HIGH | Chống tải lên shellcode, virus thực thi mã độc trên server hoặc ghi đè file hệ thống. |
| SAFE-FILE-04 | Thư mục lưu trữ file tải lên phải được mount ra volume ngoài container Docker. | 🟠 HIGH | Tránh mất file khi container bị restart hoặc rebuild. |

---

## 5. An toàn Tích hợp Ngoại vi (External Integration Safety)

| ID | Rule | Mức độ | Rationale |
|----|------|--------|-----------|
| SAFE-EXT-01 | Các cuộc gọi REST API đến bên thứ ba (như SePay Webhook, Google OAuth, Mail SMTP) **không được phép** làm treo luồng xử lý chính. Bắt buộc cấu hình Connect/Read Timeout ngắn (≤ 3 giây) và chạy bất đồng bộ (Async) hoặc có hàng đợi retry rõ ràng. | 🔴 CRITICAL | Ngăn chặn hệ thống bị nghẽn (Thread Starvation) dẫn đến sập toàn sàn khi API bên thứ ba gặp sự cố. |
| SAFE-EXT-02 | Giao thức truyền nhận thông tin thanh toán nạp tiền tự động (SePay Webhook) bắt buộc xác thực chữ ký (Signature) và kiểm tra tính duy nhất (Idempotency) của mã giao dịch ngân hàng trước khi xử lý cộng tiền. | 🔴 CRITICAL | Chống nạp khống tiền bằng cách gọi đè Webhook giả mạo hoặc xử lý trùng lặp giao dịch. |
| SAFE-EXT-03 | Log hệ thống ghi nhận lỗi tích hợp ngoại vi **tuyệt đối không** được ghi kèm API keys, Secrets hoặc mã PIN ngân hàng. | 🟠 HIGH | Tránh rò rỉ keys qua log file. |

---

## Ngưỡng Cảnh báo & Xử lý Sự cố

*   **Sự cố nạp tiền trùng lặp (SePay Webhook)**: Khóa ngay tính năng tiếp nhận webhook, thực hiện roll back giao dịch ví bị trùng và đối soát thủ công với lịch sử ngân hàng.
*   **Khi xảy ra lỗi DDL/Migration trên Production**: Thực hiện chạy `flyway repair` hoặc rollback phiên bản DB về bản backup gần nhất, tìm nguyên nhân lỗi ở Staging trước khi thử lại.
*   **Phát hiện lộ lọt JWT Secret hoặc Credentials**: Thực hiện xoay vòng khóa (Rotate Keys) ngay lập tức, thu hồi toàn bộ token cũ đang hoạt động và cập nhật biến môi trường trên server.

---

## Tham chiếu

| Nguồn | Nội dung liên quan |
|-------|---------------------|
| `.sdd/constitution.md` | Điều 3 (Bảo mật & Tài chính), Điều 4 (Quy trình Git & Commit) |
| `AGENTS.md` | Mục 2 (Domain Rules), Mục 3 (Forbidden Patterns) |
| `constraints/global.md` | Ràng buộc kỹ thuật MMO Market |
| `constraints/business.md` | Ràng buộc nghiệp vụ MMO Market |
