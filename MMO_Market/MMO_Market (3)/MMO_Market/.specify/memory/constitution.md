<!--
SYNC IMPACT REPORT:
- Version Change: None -> 1.0.0
- Modified Principles:
  - PRINCIPLE_1: I. Currency and Wallet Integrity
  - PRINCIPLE_2: II. Escrow (Giam tiền) Policy
  - PRINCIPLE_3: III. Data Security and Soft Delete
  - PRINCIPLE_4: IV. Service Layer Isolation
  - PRINCIPLE_5: V. Set-Based Database Triggers
- Added Sections: Development Constraints & Quality Standards, Development Workflow & DoD
- Removed Sections: None
- Templates requiring updates:
  - .specify/templates/plan-template.md (✅ updated)
  - .specify/templates/spec-template.md (✅ updated)
  - .specify/templates/tasks-template.md (✅ updated)
- Follow-up TODOs: None
-->

# MMO Market Constitution

## Core Principles

### I. Currency and Wallet Integrity
Hệ thống chỉ sử dụng VNĐ dạng số nguyên lớn (Java `Long` / SQL Server `BIGINT`). Tuyệt đối không tạo coin, point trung gian. Ví người dùng phải tách biệt 2 trạng thái số dư: khả dụng (`available_balance`) để mua hàng/rút tiền, và đóng băng (`hold_balance`) cho khiếu nại chưa phân định hoặc lệnh rút đang chờ xử lý.

### II. Escrow (Giam tiền) Policy
Mọi đơn hàng mua sản phẩm số thành công phải bị giam tiền trong ví trung gian hệ thống. Thời gian giam tiền mặc định là 72 giờ; hoặc 168 giờ (7 ngày) đối với shop mới (dưới 20 đơn), shop bị cảnh cáo Level 0, hoặc shop có tỷ lệ khiếu nại đúng >= 2%. Tiền chỉ được giải phóng cho ví khả dụng của Seller sau khi hết hạn giam tiền hoặc người mua xác nhận hoàn thành sớm.

### III. Data Security and Soft Delete
Nội dung sản phẩm số (giftcode, tài khoản, key game) bán trên sàn phải được mã hóa trước khi lưu trữ vào Database để chống rò rỉ dữ liệu. Tuyệt đối không xóa vật lý (`DELETE`) các bản ghi quan trọng như Users, Products, Orders. Phải sử dụng cờ `isDelete = 1` và luôn lọc `isDelete = 0` khi truy vấn.

### IV. Service Layer Isolation
Mọi API endpoint phải dùng DTO (Request/Response) để truyền nhận dữ liệu, cấm trả JPA Entity trực tiếp ra API. Mọi logic tính tiền, tính hoa hồng, kiểm tra quyền hạn (Admin/Staff/Seller) và kiểm tra sở hữu bản ghi (Ownership) phải được xử lý và xác thực ở Backend, không được thực hiện ở Frontend.

### V. Set-Based Database Triggers
Các Trigger trong SQL Server bắt buộc phải xử lý set-based thông qua hai bảng ảo `inserted` và `deleted` để hỗ trợ batch update/insert. Cấm sử dụng row-by-row logic hoặc cursor tuần tự trong trigger.

## Development Constraints & Quality Standards
- **Không hardcode credentials**: Cấm lưu password, JWT Secret Key, Google Client Secrets hay URL ngân hàng trực tiếp vào mã nguồn. Tất cả phải cấu hình qua Environment Variables.
- **Không dùng System.out/printStackTrace**: Bắt buộc ghi log có cấu trúc thông qua SLF4J / Logback logger.
- **Clean Coding**: Độ dài của mỗi hàm không vượt quá 40 dòng, độ dài của mỗi file source code không quá 300 dòng.

## Development Workflow & DoD
- **Specification-Driven**: Phải có đặc tả spec tương ứng được đặt trong `.sdd/specs/` và tuân thủ chặt chẽ chuẩn EARS (8 thành phần).
- **Database First**: Bất kỳ thay đổi schema nào phải đi kèm script T-SQL di chuyển dữ liệu & rollback rõ ràng.
- **Code Coverage**: Đạt tối thiểu 80% độ bao phủ dòng code logic ở Service layer và 100% integration tests cho API mới.
- **Git Convention**: Commit message viết theo Conventional Commits bằng tiếng Việt.

## Governance
Hiến pháp này là nguồn quy chuẩn cao nhất của dự án MMO Market. Tất cả các Pull Request và đợt duyệt mã nguồn (Code Review) phải đối chiếu trực tiếp với các điều khoản của Hiến pháp này. Mọi thay đổi hoặc bổ sung đối với Hiến pháp phải được biểu quyết, ghi nhận phiên bản mới, và cập nhật tài liệu kiểm chứng tương ứng.

**Version**: 1.0.0 | **Ratified**: 2026-07-24 | **Last Amended**: 2026-07-24
