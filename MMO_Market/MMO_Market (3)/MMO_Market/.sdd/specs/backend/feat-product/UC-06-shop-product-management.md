# UC-06 — Quản Lý Đăng Bán Sản Phẩm & Hàng Tồn Kho (Shop Product Management)

> **Feature:** `feat-product` | **Phiên bản:** 2.0 | **Trạng thái:** Published
> **Cập nhật:** 2026-07-16

---

## 1. Tổng Quan

| Thuộc tính | Nội dung |
|:---|:---|
| **Mã Use Case** | UC-06 |
| **Tên** | Quản Lý Đăng Bán Sản Phẩm & Hàng Tồn Kho (Shop Product Management) |
| **Tác nhân chính** | Người bán (Seller) |
| **Mô tả ngắn** | Người bán (Seller) đăng tải sản phẩm số mới lên sàn (nhập thông tin, giá bán, chọn danh mục) và bổ sung các mã tài sản số (giftcode, tài khoản) vào kho hàng tồn. Hệ thống tự động mã hóa bảo mật các thông tin tài sản nhạy cảm trước khi lưu xuống cơ sở dữ liệu. |
| **Độ ưu tiên** | Cao (P1) — tính năng cốt lõi cho hoạt động cung ứng sản phẩm trên sàn |

---

## 2. Tác Nhân & Điều Kiện

### 2.1 Tác Nhân
- **Người bán (Seller):** Người dùng đã đăng ký mở gian hàng thành công (Shop) và tài khoản đang hoạt động bình thường.

### 2.2 Điều Kiện Tiền Quyết (Preconditions)
- Người bán đã được KYC và kích hoạt quyền bán hàng (`ROLE_SELLER`).
- Đã đăng nhập vào bảng điều khiển Seller Console.
- **Ràng buộc Ví âm:** Đối với các Shop ở Level 0 hoặc Level 1, số dư ví khả dụng của Seller phải không âm (`balanceVnd >= 0`) để thực hiện đăng bán sản phẩm mới, thêm biến thể hoặc cập nhật biến thể. *(Lưu ý: Bổ sung hàng tồn kho/Restock vẫn được phép thực hiện khi ví âm để hỗ trợ Seller trả nợ)*.

### 2.3 Hậu Điều Kiện (Postconditions)
- Sản phẩm mới được tạo thành công với trạng thái mặc định.
- Các mã tài sản số (tồn kho) được mã hóa bằng thuật toán đối xứng (AES-256) và lưu vào bảng `DigitalAssets`.
- Số lượng hàng tồn kho (`stock`) của biến thể sản phẩm được cập nhật tự động dựa trên số lượng mã tài sản số chưa bán.

---

## 3. Luồng Xử Lý

### 3.1 Luồng A — Đăng Bán Sản Phẩm Mới (Product Creation)

```
Bước 1  [Seller]:     Vào Seller Console, chọn mục "Quản lý sản phẩm", click "Đăng sản phẩm mới"
Bước 2  [Frontend]:   Hiển thị biểu mẫu đăng sản phẩm (Tên, danh mục, mô tả chi tiết, danh sách các biến thể ban đầu kèm giá tiền và hình ảnh)
Bước 3  [Seller]:     Điền đầy đủ thông tin sản phẩm và biến thể, click "Xác nhận tạo sản phẩm"
Bước 4  [Frontend]:   Gửi yêu cầu POST /api/seller/products với các thông tin đã điền kèm JWT Token
Bước 5  [Backend]:    Validate dữ liệu:
                       - Tên sản phẩm và danh mục không trống. Phải có ít nhất 1 biến thể.
                       - Giá bán phải là số nguyên lớn (Long) >= 1,000 VNĐ.
                       - Kiểm tra ví âm: Nếu balanceVnd < 0 và shopLevel là 0 hoặc 1, trả về 400 Bad Request.
                       - Kiểm tra giới hạn Level 0: Nếu shopLevel = 0 và đã có >= 5 sản phẩm hoạt động, trả về 400 Bad Request.
                       - Kiểm tra giá Level 1: Nếu shopLevel = 1 và có bất kỳ biến thể nào có giá > 200,000 VNĐ, trả về 400 Bad Request.
Bước 6  [Backend]:    Lưu thực thể Product và các ProductVariant vào CSDL với isDelete = 0 và stock = 0.
Bước 7  [Backend]:    Trả về Product DTO chi tiết (HTTP 200 OK)
Bước 8  [Frontend]:   Thông báo tạo thành công, chuyển hướng người dùng sang giao diện Quản lý kho hàng để bổ sung mã thẻ
```

### 3.2 Luồng B — Bổ Sung Hàng Tồn Kho (Restocking Inventory)

```
Bước 1  [Seller]:     Tại danh sách sản phẩm, chọn biến thể cần nhập kho, click "Bổ sung hàng tồn"
Bước 2  [Frontend]:   Hiển thị màn hình nhập kho (cho phép nhập nhiều dòng mã code, mỗi dòng là một sản phẩm số bàn giao)
Bước 3  [Seller]:     Nhập danh sách mã tài khoản/mật khẩu hoặc mã key game, click "Xác nhận nhập kho"
Bước 4  [Frontend]:   Gửi yêu cầu POST /api/seller/digital-assets với body chứa: variantId, assetType và danh sách assets (keyCode, accountUsername, accountPassword, notes)
Bước 5  [Backend]:    Tìm kiếm biến thể theo variantId, kiểm tra quyền sở hữu của Seller đối với sản phẩm cha.
Bước 6  [Backend]:    Duyệt qua danh sách assets được gửi lên:
                       - Kiểm tra mã key trùng lặp trong kho (nếu thuộc loại KEY).
                       - Sử dụng mã khóa bảo mật hệ thống để mã hóa chuỗi code/mật khẩu bằng thuật toán AES-256.
                       - Tạo bản ghi DigitalAsset lưu chuỗi đã mã hóa vào các cột dữ liệu tương ứng, isUsed = false, isDelete = false.
Bước 7  [Backend]:    Nếu loại sản phẩm không phải là SERVICE, cập nhật lại số lượng tồn kho: variant.stock = (số lượng assets chưa bán có isUsed = false).
Bước 8  [Backend]:    Lưu thay đổi của ProductVariant và danh sách DigitalAssets vào CSDL (bọc trong @Transactional).
Bước 9  [Backend]:    Trả về thông báo cập nhật kho hàng thành công (HTTP 200 OK)
```

---

## 4. Quy Tắc Bảo Mật & Mã Hóa

- **Mã hóa thông tin nhạy cảm:** Bắt buộc sử dụng AES-256 đối với cột `asset_data` trong bảng `DigitalAssets` để chống rò rỉ dữ liệu khi bị tấn công DB hoặc lỗi cấu hình.
- **Tránh trùng lặp:** Loại bỏ các dòng trống hoặc các dòng trùng lặp hoàn toàn trong cùng một lô hàng tải lên.