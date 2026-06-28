# AGENTS.md — MMO Market Agent Guidelines & Best Practices

Tài liệu này định hướng hành vi nghiệp vụ và quy tắc lập trình dành cho lập trình viên và các AI Agent làm việc trên repository **MMO Market**.

---

## 1. DỰ ÁN & NGUỒN CHÂN LÝ (Source of Truth)

*   **Dự án**: MMO Market — Web application và REST API sàn giao dịch sản phẩm số C2C.
*   **Tech Stack**: Java 17, Spring Boot 3.1, SQL Server (T-SQL), Thymeleaf/CSS/JS thuần, JWT & Google OAuth2, SePay.
*   **Thứ tự ưu tiên tài liệu**:
    1.  Schema SQL Server đang chạy và các bản migration đã áp dụng.
    2.  Source code và Unit tests đang chạy thành công.
    3.  Tài liệu đặc tả nghiệp vụ ([Business Specification](docs/specifications/business-specification.md)).
    4.  Tài liệu tham chiếu Database ([Database Reference](docs/database/schema-reference.md)).

---

## 2. DOMAIN RULES (Luật Nghiệp Vụ Sống Còn)

*   **Tiền tệ**: Chỉ sử dụng VNĐ dạng số nguyên lớn (`BIGINT` trong Database / `Long` trong Java). Tuyệt đối không tạo coin, point trung gian.
*   **Phân chia số dư ví**: Ví người dùng phải tách biệt 2 trạng thái số dư:
    -   `available_balance` (Số dư khả dụng để mua hàng hoặc rút tiền).
    -   `hold_balance` (Số dư đóng băng do khiếu nại chưa phân định hoặc lệnh rút đang chờ xử lý).
*   **Escrow (Giam tiền)**: Mọi đơn hàng mua sản phẩm số thành công phải bị giam tiền trong ví trung gian hệ thống trong 72 giờ (`escrow_release_date` trong database). Sau 72 giờ không có khiếu nại hoặc khi người mua bấm xác nhận, tiền mới được giải phóng cho Seller.
*   **Tính an toàn dữ liệu số**: Nội dung sản phẩm số (giftcode, tài khoản, key game) bán trên sàn phải được mã hóa trước khi lưu trữ vào Database để chống rò rỉ dữ liệu.
*   **Soft Delete**: Không xóa vật lý (`DELETE`) dòng dữ liệu quan trọng như Users, Products, Orders. Phải sử dụng cờ `isDelete = 1` và luôn lọc `isDelete = 0` khi truy vấn.
*   **SQL Server Triggers**: Trigger bắt buộc phải xử lý set-based qua hai bảng ảo `inserted` và `deleted` để hỗ trợ batch update/insert. Cấm dùng row-by-row logic.

---

## 3. FORBIDDEN PATTERNS (Mẫu Thiết Kế Cấm Kỵ)

*   ❌ **Không hardcode credentials**: Cấm lưu password, JWT Secret Key, Google Client Secrets hay URL ngân hàng trực tiếp vào mã nguồn. Tất cả phải cấu hình qua Environment Variables.
*   ❌ **Không trả JPA Entity trực tiếp ra API**: Mọi API endpoint phải dùng DTO (Request/Response) để truyền nhận dữ liệu.
*   ❌ **Không tính tiền hay phân quyền ở Frontend**: Logic trừ tiền, tính toán hoa hồng, kiểm tra quyền Admin/Staff phải được xác thực hoàn toàn ở Backend. Frontend chỉ ẩn hiện nút để tăng UX.
*   ❌ **Không dùng `System.out.println` hay `printStackTrace`**: Bắt buộc ghi log có cấu trúc thông qua SLF4J / Logback logger.

---

## 4. GOLDEN PATTERNS (Mẫu Thiết Kế Vàng)

AI Agent và lập trình viên nên tham khảo và copy-paste các mẫu thiết kế chuẩn dưới đây khi triển khai logic.

### 4.1. DTO Mapping Pattern (Map Entity sang DTO ở Service Layer)
```java
// DTO Response sạch sẽ, không chứa annotations JPA
public class ProductResponse {
    private Long id;
    private String name;
    private Long price;
    private String sellerName;
    // Getters, Setters, Constructors
}

// Service Mapper logic
@Service
public class ProductService {
    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findByIdAndIsDelete(id, false)
            .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại"));
        
        // Chuyển đổi Entity sang DTO thủ công hoặc qua Mapper Helper
        ProductResponse response = new ProductResponse();
        response.setId(product.getId());
        response.setName(product.getName());
        response.setPrice(product.getPrice());
        response.setSellerName(product.getSeller().getDisplayName());
        
        return response;
    }
}
```

### 4.2. Service Layer with Transaction (Xử lý Transaction nghiệp vụ ví tài chính)
```java
@Service
public class WalletService {
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    public WalletService(UserRepository userRepository, TransactionRepository transactionRepository) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional(rollbackFor = Exception.class)
    public void transferToEscrow(Long buyerId, Long amount, String orderId) {
        // Khóa bi quan (Pessimistic Locking) để tránh race condition số dư ví
        User buyer = userRepository.findByIdForUpdate(buyerId)
            .orElseThrow(() -> new ResourceNotFoundException("Người mua không tồn tại"));

        if (buyer.getAvailableBalance() < amount) {
            throw new InsufficientBalanceException("Số dư khả dụng không đủ");
        }

        // Thực hiện giao dịch ví
        buyer.setAvailableBalance(buyer.getAvailableBalance() - amount);
        userRepository.save(buyer);

        // Tạo Transaction ledger
        WalletTransaction tx = new WalletTransaction();
        tx.setUserId(buyerId);
        tx.setAmount(amount);
        tx.setType(TransactionType.PURCHASE_ESCROW);
        tx.setOrderId(orderId);
        tx.setCreatedAt(Instant.now());
        transactionRepository.save(tx);
        
        log.info("Đã chuyển {} VNĐ sang Escrow cho Order: {}", amount, orderId);
    }
}
```

### 4.3. Authorization Check Pattern (Phân quyền nhiều lớp ở Controller/Service)
```java
@RestController
@RequestMapping("/api/v1/seller/products")
public class SellerProductController {

    private final ProductService productService;

    public SellerProductController(ProductService productService) {
        this.productService = productService;
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductUpdateRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        
        // Kiểm tra quyền sở hữu đối với sản phẩm (Ownership validation) ở Service layer
        ProductResponse updated = productService.updateProductAsSeller(id, request, currentUser.getId());
        
        return ResponseEntity.ok(new ApiResponse<>(200, "Cập nhật sản phẩm thành công", updated));
    }
}
```

### 4.4. AI Async Pattern with Fallback (Giao tiếp API ngoài / Gọi webhook không đồng bộ)
```java
@Service
public class NotificationService {

    private final RestTemplate restTemplate;
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    public NotificationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void sendWebhookNotificationAsync(String url, WebhookPayload payload) {
        executorService.submit(() -> {
            try {
                // Đặt Timeout ngắn (ví dụ: 3 giây) để không làm treo luồng
                SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
                factory.setConnectTimeout(3000);
                factory.setReadTimeout(3000);
                restTemplate.setRequestFactory(factory);

                restTemplate.postForEntity(url, payload, String.class);
                log.info("Webhook gửi thành công đến: {}", url);
            } catch (Exception ex) {
                // Cơ chế fallback: Ghi log cảnh báo và đưa vào hàng đợi retry sau
                log.error("Webhook thất bại đến {}. Kích hoạt Fallback/Retry. Chi tiết: {}", url, ex.getMessage());
                enqueueForRetry(url, payload);
            }
        });
    }

    private void enqueueForRetry(String url, WebhookPayload payload) {
        // logic lưu hàng đợi/db retry
    }
}
```

### 4.5. Global Exception Handling Pattern (Chuẩn hóa API response khi lỗi)
```java
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ApiResponse<Void>> handleInsufficientBalance(InsufficientBalanceException ex) {
        ApiResponse<Void> response = new ApiResponse<>(422, ex.getMessage(), null);
        return new ResponseEntity<>(response, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        ApiResponse<Map<String, String>> response = new ApiResponse<>(400, "Dữ liệu đầu vào không hợp lệ", errors);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
}
```

---

## 5. DEFINITION OF DONE (DoD)

Mọi Task phát triển tính năng hoặc sửa lỗi chỉ được đánh giá là hoàn thành khi đáp ứng toàn bộ checklist sau:
*   [ ] **Specification-Driven**: Phải có đặc tả spec tương ứng được đặt trong `.sdd/specs/`.
*   [ ] **Business Invariants**: Tuân thủ luật escrow 72 giờ, tiền tệ VNĐ BIGINT, ví khả dụng/đóng băng, soft delete.
*   [ ] **Database First**: Bất kỳ thay đổi schema nào phải đi kèm script T-SQL di chuyển dữ liệu & rollback rõ ràng.
*   [ ] **Authorization & RBAC**: Endpoint được bảo vệ, phân quyền Role chặt chẽ ở mức API và kiểm tra quyền sở hữu bản ghi ở Service layer.
*   [ ] **Clean Coding & Standards**: Code hàm không vượt quá 40 dòng, file không quá 300 dòng. Không dùng debug output.
*   [ ] **Code Coverage**: Đạt tối thiểu 80% độ bao phủ dòng code logic ở Service layer và 100% integration tests cho API mới.
*   [ ] **Git Convention**: Commit message viết theo Conventional Commits bằng tiếng Việt. Branch đặt đúng prefix.
