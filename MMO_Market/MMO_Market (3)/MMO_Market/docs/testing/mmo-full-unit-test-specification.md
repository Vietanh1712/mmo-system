# ĐẶC TẢ THIẾT KẾ UNIT TEST TOÀN DỰ ÁN - MMO MARKET (JUNIT & DOMAIN RULES)

Tài liệu này đặc tả toàn bộ chiến lược kiểm thử đơn vị (Unit Test) sử dụng **JUnit 5** và **Mockito** cho dự án **MMO Market**. Tài liệu được chia thành hai phần chính:
1. **Quy tắc Kiểm thử các Luật Nghiệp vụ Sống còn (Domain Rules Base Tests)**
2. **Đặc tả kiểm thử chi tiết cho tất cả 14 Phân hệ (Feature Packages)**

---

## I. QUY TẮC KIỂM THỬ CÁC LUẬT NGHIỆP VỤ SỐNG CÒN (DOMAIN RULES BASE TESTS)

Mọi bộ test case viết ra bắt buộc phải có các ca kiểm thử kiểm tra tính toàn vẹn của các luật nghiệp vụ dưới đây:

### 1. Luật Tiền Tệ & Kiểu Dữ Liệu VNĐ (Long/BIGINT Only)
* **Quy tắc**: Tuyệt đối không sử dụng coin, point trung gian hay số thực (`double`, `float`) để tính tiền lẻ. Tất cả phải là VNĐ dạng số nguyên lớn (`Long` / `BIGINT`).
* **JUnit Assertion Base**:
  ```java
  // Đảm bảo số dư và số tiền giao dịch không bao giờ dùng kiểu dữ liệu số thực
  WalletTransaction tx = new WalletTransaction();
  tx.setAmountVnd(50000L); // Đúng chuẩn Long
  assertEquals(Long.class, ((Object)tx.getAmountVnd()).getClass());
  ```
* **Kịch bản kiểm thử bắt buộc**:
  - `testTransactionAmount_MustBeLongAndNoFraction`: Kiểm tra đầu vào các API tài chính ném lỗi nếu chứa số thực hoặc không chia hết cho đơn vị nhỏ nhất.

### 2. Luật Phân Tách Ví Tài Chính (`availableBalance` vs `holdBalance`)
* **Quy tắc**: Ví người dùng phải tách biệt 2 trạng thái số dư: Số dư khả dụng (`availableBalance`) và Số dư đóng băng do khiếu nại hoặc rút tiền đang xử lý (`holdBalance`).
* **JUnit Assertion Base**:
  ```java
  // Khi tạo yêu cầu rút tiền hoặc khiếu nại đơn hàng
  Long oldAvailable = user.getAvailableBalance();
  Long oldHold = user.getHoldBalance();
  
  // Thực thi nghiệp vụ giam tiền
  withdrawalService.requestWithdrawal(user.getId(), 100000L);
  
  assertEquals(oldAvailable - 100000L, user.getAvailableBalance());
  assertEquals(oldHold + 100000L, user.getHoldBalance());
  ```
* **Kịch bản kiểm thử bắt buộc**:
  - `testWithdrawalRequest_DecreasesAvailable_IncreasesHold`: Rút tiền chuyển khả dụng sang đóng băng.
  - `testComplaintDisputed_LocksTransactionAmountInEscrow`: Khiếu nại đóng băng tiền trong ví trung gian hệ thống.

### 3. Luật Giam Tiền Ký Quỹ Động (Escrow 72h vs 168h)
* **Quy tắc**: Giam giữ tiền của người bán 72h mặc định; tăng lên 168h (7 ngày) đối với: shop mới dưới 20 đơn hoàn thành, shop cấp độ cảnh cáo Level 0, hoặc shop có tỷ lệ khiếu nại đúng $\ge 2\%$.
* **JUnit Assertion Base**:
  ```java
  // Test với Shop mới (< 20 đơn)
  when(transactionRepository.countCompletedSalesBySeller(newSeller)).thenReturn(10L);
  long durationNew = escrowService.calculateEscrowDuration(newSeller);
  assertEquals(168L, durationNew); // 7 ngày
  
  // Test với Shop uy tín (>= 20 đơn, không tranh chấp)
  when(transactionRepository.countCompletedSalesBySeller(trustedSeller)).thenReturn(50L);
  long durationTrusted = escrowService.calculateEscrowDuration(trustedSeller);
  assertEquals(72L, durationTrusted); // 3 ngày
  ```
* **Kịch bản kiểm thử bắt buộc**:
  - `calculateEscrowDuration_NewShop_Under20Sales_Returns168Hours`
  - `calculateEscrowDuration_TrustedShop_Over20Sales_Returns72Hours`
  - `calculateEscrowDuration_DisputedRateAbove2Percent_Returns168Hours`

### 4. Luật An Toàn Dữ Liệu Số (Mã hóa trước khi lưu trữ)
* **Quy tắc**: Nội dung sản phẩm số (giftcode, key game, tài khoản) bán trên sàn phải được mã hóa trước khi lưu vào Database và chỉ giải mã khi giao dịch hoàn tất bàn giao cho khách hàng.
* **JUnit Assertion Base**:
  ```java
  // Kiểm tra trước khi lưu
  digitalAssetService.saveAsset(rawGiftCode);
  verify(cryptoService).encrypt(rawGiftCode);
  ```
* **Kịch bản kiểm thử bắt buộc**:
  - `testDigitalAsset_Save_EncryptsContent`: Kiểm tra nội dung lưu trữ trong CSDL không phải dạng clear-text.
  - `testDigitalAsset_DecryptOnDelivery`: Đảm bảo chỉ giải mã khi bàn giao cho người mua hợp lệ.

### 5. Luật Xóa Mềm (Soft Delete - `isDelete = 1`)
* **Quy tắc**: Tuyệt đối cấm sử dụng câu lệnh `DELETE` vật lý đối với các bảng quan trọng (`Users`, `Products`, `Orders`, `Transactions`). Luôn set `isDelete = true` và chỉ lọc bản ghi `isDelete = false`.
* **JUnit Assertion Base**:
  ```java
  // Thực thi hành động xóa
  productService.deleteProduct(productId);
  
  // Xác minh trạng thái trong DB vẫn tồn tại nhưng set cờ xóa mềm
  verify(productRepository).save(argThat(product -> product.getIsDelete() == true));
  ```
* **Kịch bản kiểm thử bắt buộc**:
  - `testDeleteUser_SetsDeleteFlagTrue_DoesNotCallPhysicalDelete`
  - `testFindAllActive_FiltersOutDeletedRecords`

---

## II. ĐẶC TẢ CHI TIẾT UNIT TEST CHO 14 PHÂN HỆ (FEATURE PACKAGES)

---

### Phân hệ 1: `admin` (Quản trị hệ thống)
* **Dịch vụ cần test**: `AdminUserManagementService`, `AdminRevenueService`
* **Mocks**: `UserRepository`, `AuditLogRepository`, `TransactionRepository`
* **JUnit Test Cases**:
    * 🔍 `toggleLock_LocksUser_WritesAuditLog`: Admin khóa người dùng $\rightarrow$ Trạng thái người dùng chuyển sang `Locked` $\rightarrow$ Ghi log hoạt động.
    * 🔍 `toggleLock_SelfLock_ThrowsException`: Admin tự khóa tài khoản của chính mình $\rightarrow$ Ném lỗi `ResponseStatusException` (400).
    * 🔍 `updateRole_RequiresAdminOperator`: Người dùng vai trò khác tự thay đổi quyền $\rightarrow$ Ném lỗi `ResponseStatusException` (403).
    * 🔍 `calculateRevenue_ReturnsCorrectSums`: Thống kê doanh thu theo thời gian, kiểm tra tính toán tổng hoa hồng sàn nhận được.

---

### Phân hệ 2: `auth` (Xác thực người dùng)
* **Dịch vụ cần test**: `AuthenticationService`, `EmailService`, `GoogleOAuth2Service`
* **Mocks**: `UserRepository`, `PasswordEncoder`, `JwtService`
* **JUnit Test Cases**:
    * 🔍 `register_DuplicateEmail_ThrowsConflict`: Đăng ký tài khoản với email đã tồn tại $\rightarrow$ Ném lỗi trùng lặp email.
    * 🔍 `login_WrongPassword_ThrowsUnauthorized`: Đăng nhập sai mật khẩu $\rightarrow$ Ném lỗi chưa xác thực.
    * 🔍 `verifyOtp_Expired_ThrowsOtpExpired`: Xác thực OTP quá hạn $\rightarrow$ Ném lỗi OTP hết hiệu lực.

---

### Phân hệ 3: `chat` (Liên lạc giữa Khách hàng & Người bán)
* **Dịch vụ cần test**: `ChatResponseTimeService`, `ChatService`
* **Mocks**: `ChatRepository`, `UserRepository`
* **JUnit Test Cases**:
    * 🔍 `calculateResponseTime_DeduplicatesMultipleMessages`: Bỏ qua các tin nhắn dồn dập của cùng một người mua trước khi người bán phản hồi, chỉ tính từ mốc tin nhắn đầu tiên.
    * 🔍 `calculateResponseTime_NoHistory_ReturnsDefaultLabel`: Không có tin nhắn cũ $\rightarrow$ Trả về mặc định nhãn `"Trong vài giờ"`.

---

### Phân hệ 4: `complaint` (Khiếu nại & Tranh chấp đơn hàng)
* **Dịch vụ cần test**: `ComplaintService`
* **Mocks**: `ComplaintRepository`, `TransactionRepository`, `WalletService`
* **JUnit Test Cases**:
    * 🔍 `fileComplaint_FreezesEscrowAmount`: Tạo khiếu nại thành công $\rightarrow$ Đóng băng trạng thái tiền giam giữ, cấm giải phóng tự động.
    * 🔍 `resolveDispute_BuyerWins_InitiatesRefund`: Giải quyết khiếu nại (Người mua thắng) $\rightarrow$ Hoàn trả toàn bộ tiền về ví khả dụng người mua.

---

### Phân hệ 5: `kyc` (Xác minh danh tính)
* **Dịch vụ cần test**: `KycService`
* **Mocks**: `KycRequestRepository`, `UserRepository`
* **JUnit Test Cases**:
    * 🔍 `submitKyc_SetsPendingStatus`: Người dùng gửi ảnh CCCD $\rightarrow$ Trạng thái KYC chuyển sang `PENDING`.
    * 🔍 `approveKyc_SetsUserVerified`: Nhân viên duyệt ảnh KYC $\rightarrow$ Tài khoản người dùng được cập nhật cờ `isVerified = true`.

---

### Phân hệ 6: `notification` (Thông báo hệ thống)
* **Dịch vụ cần test**: `NotificationService`
* **Mocks**: `NotificationRepository`, `SimpMessagingTemplate`
* **JUnit Test Cases**:
    * 🔍 `sendNotification_PushesWebsocket`: Gửi thông báo $\rightarrow$ Đẩy qua kênh WebSocket tương ứng.
    * 🔍 `markAsRead_UpdatesStatus`: Đọc thông báo $\rightarrow$ Trạng thái chuyển sang `READ`.

---

### Phân hệ 7: `order` (Đơn đặt hàng sản phẩm số)
* **Dịch vụ cần test**: `OrderService`
* **Mocks**: `ProductRepository`, `TransactionRepository`, `WalletService`
* **JUnit Test Cases**:
    * 🔍 `createOrder_OutOfStock_ThrowsException`: Mua hàng khi kho rỗng $\rightarrow$ Ném ra lỗi không đủ hàng.
    * 🔍 `createOrder_Success_DeductsBuyerFunds`: Đặt hàng thành công $\rightarrow$ Trừ tiền ví khả dụng người mua và ghi vào ví trung gian tạm giữ.

---

### Phân hệ 8: `preorder` (Đặt hàng trước)
* **Dịch vụ cần test**: `PreOrderService`
* **Mocks**: `PreOrderRepository`, `ProductRepository`
* **JUnit Test Cases**:
    * 🔍 `createPreOrder_CalculatesDeliveryDate`: Tạo đơn đặt trước $\rightarrow$ Tính toán chính xác thời gian bàn giao dự kiến từ nhà bán.

---

### Phân hệ 9: `product` (Quản lý & Tìm kiếm sản phẩm)
* **Dịch vụ cần test**: `ProductSearchService`, `ProductService`
* **Mocks**: `ProductRepository`, `CategoryRepository`
* **JUnit Test Cases**:
    * 🔍 `search_ResolvesCategoryHierarchy`: Tìm kiếm theo danh mục cha $\rightarrow$ Trả về toàn bộ sản phẩm thuộc tất cả danh mục con của danh mục cha đó.
    * 🔍 `deleteProduct_PerformsSoftDelete`: Xóa sản phẩm $\rightarrow$ Đảm bảo `isDelete` chuyển sang `true`, không thực thi xóa vật lý khỏi bảng.

---

### Phân hệ 10: `seller` (Cửa hàng / Người bán)
* **Dịch vụ cần test**: `SellerService`
* **Mocks**: `SellerRegistrationRepository`, `UserRepository`
* **JUnit Test Cases**:
    * 🔍 `registerSeller_ValidBankInfo_SubmitsRequest`: Đăng ký gian hàng với số tài khoản ngân hàng và mã ngân hàng hợp lệ.
    * 🔍 `calculateDisputeRate_ReturnsFraction`: Đo lường chính xác tỷ lệ khiếu nại đúng của shop để điều chỉnh thời gian giam giữ tiền.

---

### Phân hệ 11: `staff` (Quản lý nghiệp vụ của Nhân viên)
* **Dịch vụ cần test**: `StaffDashboardService`, `StaffPermissionService`
* **Mocks**: `ComplaintRepository`, `WithdrawalRepository`, `TransactionRepository`
* **JUnit Test Cases**:
    * 🔍 `dashboard_CountsHeldAsPending`: Thống kê Dashboard đếm các giao dịch đang tạm giữ (`Held`) vào mục **Đang xử lý**, không đếm vào **Thành công**.
    * 🔍 `searchTransactions_ParsesDisplayCodeToNumericId`: Tìm kiếm theo `#TXN-25` $\rightarrow$ Tự động bóc tách và tìm kiếm theo khóa chính số `25`.

---

### Phân hệ 12: `support` (Trung tâm hỗ trợ / Ticket)
* **Dịch vụ cần test**: `SupportTicketService`
* **Mocks**: `SupportTicketRepository`
* **JUnit Test Cases**:
    * 🔍 `createTicket_AssignsUniqueCode`: Tạo ticket yêu cầu hỗ trợ $\rightarrow$ Sinh mã ticket ngẫu nhiên không trùng lặp dạng UUID.

---

### Phân hệ 13: `upload` (Tải lên hình ảnh tài liệu)
* **Dịch vụ cần test**: `UploadService`
* **JUnit Test Cases**:
    * 🔍 `uploadFile_ValidImage_ReturnsUrl`: Tải ảnh JPG/PNG hợp lệ $\rightarrow$ Trả về URL đường dẫn ảnh lưu trữ.
    * 🔍 `uploadFile_InvalidExtension_ThrowsException`: Tải ảnh định dạng bị cấm (như `.exe`, `.bat`) $\rightarrow$ Từ chối và ném ra ngoại lệ.

---

### Phân hệ 14: `wallet` (Ví tài chính cá nhân)
* **Dịch vụ cần test**: `WalletService`, `WithdrawalService`
* **Mocks**: `WalletTransactionRepository`, `UserRepository`
* **JUnit Test Cases**:
    * 🔍 `getWalletStats_CalculatesTotalsCorrectly`: Đọc và tính toán số dư ví chính xác.
    * 🔍 `recordTransaction_PositiveAmount_SetsInflow`: Đảm bảo các giao dịch nạp tiền được gắn nhãn `"IN"`, các giao dịch trừ tiền được gắn nhãn `"OUT"`.
