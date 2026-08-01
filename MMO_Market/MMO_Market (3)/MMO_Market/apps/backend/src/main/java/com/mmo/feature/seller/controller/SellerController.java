package com.mmo.feature.seller.controller;

import com.mmo.shared.dal.*;
import com.mmo.shared.model.*;
import com.mmo.feature.auth.service.EmailService;
import com.mmo.feature.auth.service.AuthenticationService;
import com.mmo.feature.wallet.service.WithdrawalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/seller")
@Slf4j
public class SellerController {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final WithdrawalRepository withdrawalRepository;
    private final ComplaintRepository complaintRepository;
    private final ShopFlagRepository shopFlagRepository;
    private final ReviewRepository reviewRepository;
    private final ChatRepository chatRepository;
    private final SellerBankInfoRepository sellerBankInfoRepository;
    private final SellerRegistrationRepository sellerRegistrationRepository;
    private final DigitalAssetRepository digitalAssetRepository;
    private final SystemConfigurationRepository systemConfigurationRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final EmailService emailService;
    private final AuthenticationService authenticationService;
    private final WithdrawalService withdrawalService;
    private final com.mmo.feature.preorder.service.PreOrderService preOrderService;

    public SellerController(UserRepository userRepository, ProductRepository productRepository,
                            ProductVariantRepository productVariantRepository, CategoryRepository categoryRepository,
                            TransactionRepository transactionRepository, WithdrawalRepository withdrawalRepository,
                            ComplaintRepository complaintRepository, ShopFlagRepository shopFlagRepository,
                            ReviewRepository reviewRepository, ChatRepository chatRepository,
                            SellerBankInfoRepository sellerBankInfoRepository,
                            SellerRegistrationRepository sellerRegistrationRepository,
                            DigitalAssetRepository digitalAssetRepository,
                            SystemConfigurationRepository systemConfigurationRepository,
                            EmailVerificationRepository emailVerificationRepository,
                            EmailService emailService,
                            AuthenticationService authenticationService,
                            WithdrawalService withdrawalService,
                            com.mmo.feature.preorder.service.PreOrderService preOrderService) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.withdrawalRepository = withdrawalRepository;
        this.complaintRepository = complaintRepository;
        this.shopFlagRepository = shopFlagRepository;
        this.reviewRepository = reviewRepository;
        this.chatRepository = chatRepository;
        this.sellerBankInfoRepository = sellerBankInfoRepository;
        this.sellerRegistrationRepository = sellerRegistrationRepository;
        this.digitalAssetRepository = digitalAssetRepository;
        this.systemConfigurationRepository = systemConfigurationRepository;
        this.emailVerificationRepository = emailVerificationRepository;
        this.emailService = emailService;
        this.authenticationService = authenticationService;
        this.withdrawalService = withdrawalService;
        this.preOrderService = preOrderService;
    }


    private User getSeller(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("Phiên đăng nhập không hợp lệ.");
        }
        User user = userRepository.findByIdAndIsDeleteFalse(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin tài khoản người bán."));
        String role = String.valueOf(user.getRole()).toLowerCase(Locale.ROOT);
        if (!role.contains("seller")) {
            throw new IllegalArgumentException("Tài khoản không có quyền truy cập Seller Portal.");
        }
        return user;
    }



    // 1. Dashboard API
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard(@AuthenticationPrincipal Long userId) {
        try {
            User seller = getSeller(userId);
            long completedSales = transactionRepository.countCompletedSalesBySeller(seller);
            Long totalRevenue = transactionRepository.sumCompletedEarningsBySeller(seller);
            if (totalRevenue == null) totalRevenue = 0L;

            List<Product> products = productRepository.findBySellerAndIsDeleteFalseOrderByCreatedAtDesc(seller);
            long activeProductsCount = products.size();

            long openComplaints = complaintRepository.countOpenComplaintsBySeller(seller);

            // Recent sales
            List<Transaction> transactions = transactionRepository.findBySellerAndIsDeleteFalseOrderByCreatedAtDesc(seller);
            List<Map<String, Object>> recentTransactions = transactions.stream()
                    .limit(5)
                    .map(t -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", t.getId());
                        map.put("productName", t.getProduct().getName());
                        map.put("customerEmail", t.getCustomer().getEmail());
                        map.put("amountVnd", t.getAmountVnd());
                        map.put("status", t.getStatus());
                        map.put("createdAt", t.getCreatedAt().toString());
                        return map;
                    })
                    .collect(Collectors.toList());

            long resolvedComplaints = complaintRepository.countResolvedComplaintsBySeller(seller);
            long totalSold = transactionRepository.countTotalSalesBySeller(seller);
            double disputeRate = totalSold > 0 ? (double) resolvedComplaints / totalSold : 0.0;

            Map<String, Object> result = new HashMap<>();
            result.put("fullName", seller.getFullName());
            result.put("email", seller.getEmail());
            result.put("balanceVnd", seller.getBalanceVnd());
            result.put("shopStatus", seller.getShopStatus());
            result.put("completedSales", completedSales);
            result.put("totalRevenue", totalRevenue);
            result.put("activeProductsCount", activeProductsCount);
            result.put("openComplaintsCount", openComplaints);
            result.put("recentTransactions", recentTransactions);
            result.put("shopLevel", seller.getShopLevel() != null ? seller.getShopLevel() : 1);
            result.put("disputeRate", disputeRate);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 2. Shop Info GET
    @GetMapping("/shop-info")
    public ResponseEntity<?> getShopInfo(@AuthenticationPrincipal Long userId) {
        try {
            User seller = getSeller(userId);
            SellerRegistration reg = sellerRegistrationRepository.findFirstByUserAndIsDeleteFalseOrderByIdDesc(seller).orElse(null);
            SellerBankInfo bank = sellerBankInfoRepository.findFirstByUserAndIsDeleteFalseOrderByIdDesc(seller).orElse(null);

            if (seller.getShopStatus() != null &&
                    ("Suspended".equalsIgnoreCase(seller.getShopStatus()) || "TEMP_LOCKED".equalsIgnoreCase(seller.getShopStatus())) &&
                    seller.getSuspendedUntil() != null &&
                    java.time.LocalDateTime.now().isAfter(seller.getSuspendedUntil())) {
                seller.setShopStatus("Active");
                seller.setSuspendedUntil(null);
                userRepository.save(seller);
            }

            String effectiveStatus = seller.getShopStatus();
            if (effectiveStatus == null || effectiveStatus.isBlank()) {
                if (reg != null && reg.getStatus() != null) {
                    effectiveStatus = reg.getStatus();
                } else {
                    effectiveStatus = "Active";
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("fullName", seller.getFullName());
            result.put("shopStatus", effectiveStatus);
            result.put("suspendedUntil", seller.getSuspendedUntil() != null ? seller.getSuspendedUntil().toString() : null);
            result.put("shopName", reg != null ? reg.getShopName() : "Cửa hàng của tôi");
            result.put("description", reg != null ? reg.getDescription() : "");
            result.put("bankName", bank != null ? bank.getBankName() : "");
            result.put("accountNumber", bank != null ? bank.getAccountNumber() : "");
            result.put("accountHolder", seller.getFullName() != null ? seller.getFullName().toUpperCase() : "");
            result.put("branch", bank != null ? bank.getBranch() : "");
            result.put("shopLevel", seller.getShopLevel() != null ? seller.getShopLevel() : 1);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 3. Shop Info PUT
    @PutMapping("/shop-info")
    public ResponseEntity<?> updateShopInfo(@AuthenticationPrincipal Long userId, @RequestBody Map<String, String> request) {
        try {
            User seller = getSeller(userId);
            String shopName = request.get("shopName");
            String description = request.get("description");
            String bankName = request.get("bankName");
            String accountNumber = request.get("accountNumber");
            String branch = request.get("branch");

            if (shopName == null || shopName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Tên cửa hàng không được để trống."));
            }

            SellerRegistration reg = sellerRegistrationRepository.findFirstByUserAndIsDeleteFalseOrderByIdDesc(seller)
                    .orElse(new SellerRegistration());
            reg.setUser(seller);
            reg.setShopName(shopName);
            reg.setDescription(description);
            sellerRegistrationRepository.save(reg);

            if (bankName != null && !bankName.trim().isEmpty() && accountNumber != null && !accountNumber.trim().isEmpty()) {
                SellerBankInfo bank = sellerBankInfoRepository.findFirstByUserAndIsDeleteFalseOrderByIdDesc(seller)
                        .orElse(new SellerBankInfo());
                bank.setUser(seller);
                bank.setBankName(bankName);
                bank.setAccountNumber(accountNumber);
                bank.setBranch(branch);
                sellerBankInfoRepository.save(bank);
            }

            return ResponseEntity.ok(Map.of("message", "Cập nhật thông tin cửa hàng thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 3.1 Toggle / Update Shop Operating Status for Seller
    @PutMapping("/shop-status")
    public ResponseEntity<?> toggleSellerShopStatus(@AuthenticationPrincipal Long userId, @RequestBody(required = false) Map<String, String> request) {
        try {
            User seller = getSeller(userId);
            String currentStatus = seller.getShopStatus();
            if (currentStatus != null && ("Banned".equalsIgnoreCase(currentStatus) || "PERMANENT_BANNED".equalsIgnoreCase(currentStatus) || "Locked".equalsIgnoreCase(currentStatus) || "INDEFINITE_LOCKED".equalsIgnoreCase(currentStatus))) {
                return ResponseEntity.badRequest().body(Map.of("message", "Cửa hàng của bạn đang bị khóa do vi phạm quy định. Không thể tự thay đổi trạng thái."));
            }

            String requestedStatus = (request != null) ? request.get("status") : null;
            String newStatus;
            if (requestedStatus != null && !requestedStatus.isBlank()) {
                if ("Suspended".equalsIgnoreCase(requestedStatus) || "TEMPORARILY_CLOSED".equalsIgnoreCase(requestedStatus)) {
                    newStatus = "Suspended";
                } else if ("Active".equalsIgnoreCase(requestedStatus)) {
                    newStatus = "Active";
                } else {
                    newStatus = "Active".equalsIgnoreCase(currentStatus) ? "Suspended" : "Active";
                }
            } else {
                newStatus = "Active".equalsIgnoreCase(currentStatus) ? "Suspended" : "Active";
            }

            seller.setShopStatus(newStatus);
            seller.setSuspendedUntil(null);
            userRepository.save(seller);

            String message = "Active".equalsIgnoreCase(newStatus) ? "Cửa hàng của bạn đã hoạt động trở lại!" : "Cửa hàng đã được chuyển sang trạng thái tạm đóng.";
            return ResponseEntity.ok(Map.of("message", message, "shopStatus", newStatus));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 4. Load Categories for dropdown filter/product creation
    @GetMapping("/categories")
    public ResponseEntity<?> getCategories() {
        // Return only sub-categories
        List<Category> allCategories = categoryRepository.findAllByIsDeleteFalseOrderByIdAsc();
        List<Map<String, Object>> result = allCategories.stream()
                .filter(c -> c.getParent() == null)
                .map(parent -> {
                    Map<String, Object> parentMap = new HashMap<>();
                    parentMap.put("id", parent.getId());
                    parentMap.put("name", parent.getName());
                    
                    List<Map<String, Object>> subList = allCategories.stream()
                            .filter(c -> c.getParent() != null && c.getParent().getId().equals(parent.getId()))
                            .sorted((a, b) -> Long.compare(a.getId(), b.getId()))
                            .map(c -> {
                                Map<String, Object> map = new HashMap<>();
                                map.put("id", c.getId());
                                map.put("name", c.getName());
                                return map;
                            })
                            .collect(Collectors.toList());
                    
                    parentMap.put("subCategories", subList);
                    return parentMap;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    // 5. Products GET
    @GetMapping("/products")
    public ResponseEntity<?> getProducts(@AuthenticationPrincipal Long userId) {
        try {
            User seller = getSeller(userId);
            List<Product> products = productRepository.findBySellerAndIsDeleteFalseOrderByCreatedAtDesc(seller);

            List<Map<String, Object>> result = products.stream().map(p -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", p.getId());
                map.put("name", p.getName());
                map.put("categoryName", p.getCategory().getName());
                map.put("description", p.getDescription());
                map.put("image", p.getImage());
                map.put("productType", p.getProductType());

                List<ProductVariant> variants = productVariantRepository.findByProductAndIsDeleteFalse(p);
                map.put("variantCount", variants.size());
                map.put("totalStock", variants.stream().mapToInt(v -> v.getStock() != null ? v.getStock() : 0).sum());
                map.put("status", variants.stream().anyMatch(v -> "Active".equals(v.getStatus())) ? "Active" : "Locked");

                long unusedAssetsCount = 0;
                for (com.mmo.shared.model.ProductVariant v : variants) {
                    unusedAssetsCount += digitalAssetRepository.countByVariantAndIsUsedFalseAndIsDeleteFalse(v);
                }
                map.put("unusedAssetsCount", unusedAssetsCount);

                return map;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 6. Product GET by ID
    @GetMapping("/products/{id}")
    public ResponseEntity<?> getProductById(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        try {
            User seller = getSeller(userId);
            Product p = productRepository.findByIdAndIsDeleteFalse(id)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm."));

            if (!p.getSeller().getId().equals(seller.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Bạn không có quyền truy cập sản phẩm này."));
            }

            List<ProductVariant> variants = productVariantRepository.findByProductAndIsDeleteFalse(p);
            List<Map<String, Object>> variantList = variants.stream().map(v -> {
                Map<String, Object> vMap = new HashMap<>();
                vMap.put("id", v.getId());
                vMap.put("variantName", v.getVariantName());
                vMap.put("priceVnd", v.getPriceVnd());
                vMap.put("stock", v.getStock());
                vMap.put("status", v.getStatus());
                vMap.put("imageUrl", v.getImageUrl());
                return vMap;
            }).collect(Collectors.toList());

            Map<String, Object> map = new HashMap<>();
            map.put("id", p.getId());
            map.put("name", p.getName());
            map.put("categoryId", p.getCategory().getId());
            map.put("categoryName", p.getCategory().getName());
            map.put("description", p.getDescription());
            map.put("image", p.getImage());
            map.put("userGuide", p.getUserGuide());
            map.put("productType", p.getProductType());
            map.put("variants", variantList);

            return ResponseEntity.ok(map);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    private String validateShopActiveStatus(User seller) {
        if (seller.getShopStatus() != null &&
                ("Suspended".equalsIgnoreCase(seller.getShopStatus()) || "TEMP_LOCKED".equalsIgnoreCase(seller.getShopStatus())) &&
                seller.getSuspendedUntil() != null &&
                java.time.LocalDateTime.now().isAfter(seller.getSuspendedUntil())) {
            seller.setShopStatus("Active");
            seller.setSuspendedUntil(null);
            userRepository.save(seller);
        }

        String status = seller.getShopStatus();
        if (status != null) {
            if ("Suspended".equalsIgnoreCase(status) || "TEMP_LOCKED".equalsIgnoreCase(status)) {
                return "Cửa hàng của bạn đang ở trạng thái Tạm ngưng, không thể thực hiện thao tác này.";
            }
            if ("Locked".equalsIgnoreCase(status) || "INDEFINITE_LOCKED".equalsIgnoreCase(status)) {
                return "Cửa hàng của bạn đang bị Tạm khóa, không thể thực hiện thao tác này.";
            }
            if ("Banned".equalsIgnoreCase(status) || "PERMANENT_BANNED".equalsIgnoreCase(status)) {
                return "Cửa hàng của bạn đã bị Khóa vĩnh viễn, không thể thực hiện thao tác này.";
            }
            if ("Withdrawn".equalsIgnoreCase(status) || "DELETED".equalsIgnoreCase(status) || "Pending".equalsIgnoreCase(status)) {
                return "Cửa hàng của bạn hiện không ở trạng thái hoạt động, không thể thực hiện thao tác này.";
            }
        }
        return null;
    }

    // 7. Product POST (Create)
    @PostMapping("/products")
    public ResponseEntity<?> createProduct(@AuthenticationPrincipal Long userId, @RequestBody Map<String, Object> request) {
        try {
            User seller = getSeller(userId);
            String statusErr = validateShopActiveStatus(seller);
            if (statusErr != null) {
                return ResponseEntity.badRequest().body(Map.of("message", statusErr));
            }
            long activeProductsCount = productRepository.countBySellerIdAndIsDeleteFalse(seller.getId());
            int shopLevel = seller.getShopLevel() != null ? seller.getShopLevel() : 1;
            
            long balance = seller.getBalanceVnd() != null ? seller.getBalanceVnd() : 0L;
            if (balance < 0 && (shopLevel == 0 || shopLevel == 1)) {
                return ResponseEntity.badRequest().body(Map.of("message", "Tài khoản của bạn đang có số dư âm. Vui lòng nạp tiền thanh toán nợ để tiếp tục đăng bán sản phẩm."));
            }

            if (shopLevel == 0 && activeProductsCount >= 5) {
                return ResponseEntity.badRequest().body(Map.of("message", "Cửa hàng của bạn đang ở Level 0 (Cảnh cáo) do tỷ lệ lỗi khiếu nại >= 2%. Bạn chỉ được hiển thị tối đa 5 sản phẩm cùng lúc trên sàn. Hãy xóa bớt sản phẩm cũ hoặc khắc phục tỷ lệ khiếu nại để đăng bán thêm."));
            }

            String name = (String) request.get("name");
            String description = (String) request.get("description");
            String userGuide = (String) request.get("userGuide");
            Object catIdObj = request.get("categoryId");
            String productType = (String) request.get("productType");
            List<Map<String, Object>> variantsList = (List<Map<String, Object>>) request.get("variants");

            if (name == null || name.trim().isEmpty() || catIdObj == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Thông tin tên sản phẩm và danh mục không được để trống."));
            }
            if (variantsList == null || variantsList.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Sản phẩm phải có ít nhất 1 biến thể."));
            }

            Long categoryId = Long.valueOf(catIdObj.toString());
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục được chọn."));

            // Check shop level restrictions
            if (shopLevel == 0) {
                if (activeProductsCount >= 5) {
                    return ResponseEntity.badRequest().body(Map.of("message", "Shop của bạn đang trong trạng thái cảnh cáo. Chỉ được đăng tối đa 5 sản phẩm."));
                }
            } else if (shopLevel == 1) {
                for (Map<String, Object> vData : variantsList) {
                    Object priceObj = vData.get("priceVnd");
                    if (priceObj != null) {
                        Long price = Long.valueOf(priceObj.toString());
                        if (price > 200000) {
                            return ResponseEntity.badRequest().body(Map.of("message", "Shop Mới (Level 1) chỉ được phép đăng bán sản phẩm có giá tối đa 200.000 VNĐ."));
                        }
                    }
                }
            }

            Product p = new Product();
            p.setSeller(seller);
            p.setCategory(category);
            p.setName(name);
            p.setDescription(description);
            p.setUserGuide(userGuide);
            p.setIsDelete(false);
            String image = (String) request.get("image");
            p.setImage((image != null && !image.trim().isEmpty()) ? image : "https://via.placeholder.com/300x160/2563eb/ffffff?text=MMO+Market");
            if (productType != null && !productType.trim().isEmpty()) {
                p.setProductType(productType);
            } else {
                p.setProductType("ACCOUNT");
            }
            Product saved = productRepository.save(p);

            // Save variants
            for (Map<String, Object> vData : variantsList) {
                ProductVariant pv = new ProductVariant();
                pv.setProduct(saved);
                pv.setVariantName((String) vData.get("variantName"));
                
                Object priceObj = vData.get("priceVnd");
                if (priceObj == null) {
                    return ResponseEntity.badRequest().body(Map.of("message", "Biến thể phải có giá bán."));
                }
                pv.setPriceVnd(Long.valueOf(priceObj.toString()));
                
                String imgUrl = (String) vData.get("imageUrl");
                if (imgUrl == null || imgUrl.trim().isEmpty()) {
                    imgUrl = saved.getProductImageUrl();
                }
                pv.setImageUrl(imgUrl);
                
                pv.setStock(0);
                pv.setStatus("Active");
                pv.setIsDelete(false);
                productVariantRepository.save(pv);
            }

            return ResponseEntity.ok(Map.of("id", saved.getId(), "message", "Đã tạo sản phẩm và các biến thể thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 7.5 Image Upload POST
    @PostMapping("/upload-image")
    public ResponseEntity<?> uploadImage(@RequestBody Map<String, String> request) {
        try {
            String base64Image = request.get("image");
            if (base64Image == null || !base64Image.contains(",")) {
                return ResponseEntity.badRequest().body(Map.of("message", "Hình ảnh không hợp lệ."));
            }
            String[] parts = base64Image.split(",");
            String header = parts[0];
            String data = parts[1];
            String extension = "png";
            if (header.contains("image/jpeg") || header.contains("image/jpg")) {
                extension = "jpg";
            } else if (header.contains("image/gif")) {
                extension = "gif";
            } else if (header.contains("image/webp")) {
                extension = "webp";
            }
            
            byte[] imageBytes = java.util.Base64.getDecoder().decode(data);
            String filename = java.util.UUID.randomUUID().toString() + "." + extension;
            
            java.io.File uploadDir = new java.io.File("uploads");
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }
            java.nio.file.Path path = java.nio.file.Paths.get("uploads/" + filename);
            java.nio.file.Files.write(path, imageBytes);
            
            String fileUrl = "/uploads/" + filename;
            return ResponseEntity.ok(Map.of("url", fileUrl));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi tải ảnh: " + e.getMessage()));
        }
    }

    // 8. Product PUT (Update)
    @PutMapping("/products/{id}")
    public ResponseEntity<?> updateProduct(@AuthenticationPrincipal Long userId, @PathVariable Long id, @RequestBody Map<String, Object> request) {
        try {
            User seller = getSeller(userId);
            String statusErr = validateShopActiveStatus(seller);
            if (statusErr != null) {
                return ResponseEntity.badRequest().body(Map.of("message", statusErr));
            }
            Product p = productRepository.findByIdAndIsDeleteFalse(id)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm."));

            if (!p.getSeller().getId().equals(seller.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Bạn không có quyền chỉnh sửa sản phẩm này."));
            }

            String name = (String) request.get("name");
            String description = (String) request.get("description");
            String userGuide = (String) request.get("userGuide");
            Object catIdObj = request.get("categoryId");

            if (name == null || name.trim().isEmpty() || catIdObj == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Tên sản phẩm và danh mục không được để trống."));
            }

            Long categoryId = Long.valueOf(catIdObj.toString());
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục."));

            p.setName(name);
            p.setDescription(description);
            p.setUserGuide(userGuide);
            p.setCategory(category);
            productRepository.save(p);

            return ResponseEntity.ok(Map.of("message", "Cập nhật sản phẩm thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 9. Product DELETE
    @DeleteMapping("/products/{id}")
    public ResponseEntity<?> deleteProduct(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        try {
            User seller = getSeller(userId);
            Product p = productRepository.findByIdAndIsDeleteFalse(id)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm."));

            if (!p.getSeller().getId().equals(seller.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Bạn không có quyền xóa sản phẩm này."));
            }

            p.setIsDelete(true);
            productRepository.save(p);

            // Cascade delete variants
            List<ProductVariant> variants = productVariantRepository.findByProductAndIsDeleteFalse(p);
            for (ProductVariant v : variants) {
                v.setIsDelete(true);
                productVariantRepository.save(v);
            }

            return ResponseEntity.ok(Map.of("message", "Đã xóa sản phẩm thành công."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 10. Variant GET by ID
    @GetMapping("/variants/{id}")
    public ResponseEntity<?> getVariantById(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        try {
            User seller = getSeller(userId);
            ProductVariant v = productVariantRepository.findByIdAndIsDeleteFalse(id)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy biến thể."));

            if (!v.getProduct().getSeller().getId().equals(seller.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Bạn không có quyền xem biến thể này."));
            }

            Map<String, Object> map = new HashMap<>();
            map.put("id", v.getId());
            map.put("productId", v.getProduct().getId());
            map.put("productName", v.getProduct().getName());
            map.put("variantName", v.getVariantName());
            map.put("priceVnd", v.getPriceVnd());
            map.put("stock", v.getStock());
            map.put("status", v.getStatus());
            map.put("imageUrl", v.getImageUrl());

            List<DigitalAsset> assetList = digitalAssetRepository.findByVariantAndIsDeleteFalse(v);
            List<Map<String, Object>> assetMapList = assetList.stream().map(asset -> {
                Map<String, Object> amap = new HashMap<>();
                amap.put("id", asset.getId());
                amap.put("assetType", asset.getAssetType());
                amap.put("accountUsername", asset.getAccountUsername());
                amap.put("accountPassword", asset.getAccountPassword()); // We need to return password so seller can view it
                amap.put("keyCode", asset.getKeyCode());
                amap.put("cardCode", asset.getCardCode());
                amap.put("cardPin", asset.getCardPin());
                amap.put("notes", asset.getNotes());
                amap.put("isUsed", asset.getIsUsed());
                amap.put("createdAt", asset.getCreatedAt() != null ? asset.getCreatedAt().toString() : null);
                return amap;
            }).collect(Collectors.toList());
            map.put("assets", assetMapList);

            return ResponseEntity.ok(map);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 11. Variant POST (Create)
    @PostMapping("/variants")
    public ResponseEntity<?> createVariant(@AuthenticationPrincipal Long userId, @RequestBody Map<String, Object> request) {
        try {
            User seller = getSeller(userId);
            String statusErr = validateShopActiveStatus(seller);
            if (statusErr != null) {
                return ResponseEntity.badRequest().body(Map.of("message", statusErr));
            }
            Object prodIdObj = request.get("productId");
            String variantName = (String) request.get("variantName");
            Object priceObj = request.get("priceVnd");
            Object stockObj = request.get("stock");
            String status = (String) request.get("status");
            String imageUrl = (String) request.get("imageUrl");

            if (prodIdObj == null || variantName == null || variantName.trim().isEmpty() || priceObj == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Thông tin tên biến thể và giá bán không được để trống."));
            }
            Long productId = Long.valueOf(prodIdObj.toString());
            Product p = productRepository.findByIdAndIsDeleteFalse(productId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm."));

            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                imageUrl = p.getProductImageUrl();
            }

            if (!p.getSeller().getId().equals(seller.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Bạn không có quyền thao tác trên sản phẩm này."));
            }

            int shopLevel = seller.getShopLevel() != null ? seller.getShopLevel() : 1;
            long balance = seller.getBalanceVnd() != null ? seller.getBalanceVnd() : 0L;
            if (balance < 0 && (shopLevel == 0 || shopLevel == 1)) {
                return ResponseEntity.badRequest().body(Map.of("message", "Tài khoản của bạn đang có số dư âm. Vui lòng nạp tiền thanh toán nợ để tiếp tục đăng bán sản phẩm."));
            }

            if (shopLevel == 1) {
                Long price = Long.valueOf(priceObj.toString());
                if (price > 200000) {
                    return ResponseEntity.badRequest().body(Map.of("message", "Shop Mới (Level 1) chỉ được phép đăng bán sản phẩm có giá tối đa 200.000 VNĐ."));
                }
            }

            ProductVariant v = new ProductVariant();
            v.setProduct(p);
            v.setVariantName(variantName);
            v.setPriceVnd(Long.valueOf(priceObj.toString()));
            v.setStock(stockObj != null ? Integer.valueOf(stockObj.toString()) : 0);
            v.setStatus(status != null ? status : "Active");
            v.setImageUrl(imageUrl);
            v.setIsDelete(false);
            ProductVariant saved = productVariantRepository.save(v);

            return ResponseEntity.ok(Map.of("id", saved.getId(), "message", "Tạo biến thể thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 12. Variant PUT (Update)
    @PutMapping("/variants/{id}")
    public ResponseEntity<?> updateVariant(@AuthenticationPrincipal Long userId, @PathVariable Long id, @RequestBody Map<String, Object> request) {
        try {
            User seller = getSeller(userId);
            String statusErr = validateShopActiveStatus(seller);
            if (statusErr != null) {
                return ResponseEntity.badRequest().body(Map.of("message", statusErr));
            }
            ProductVariant v = productVariantRepository.findByIdAndIsDeleteFalse(id)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy biến thể."));

            if (!v.getProduct().getSeller().getId().equals(seller.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Bạn không có quyền cập nhật biến thể này."));
            }

            String variantName = (String) request.get("variantName");
            Object priceObj = request.get("priceVnd");
            Object stockObj = request.get("stock");
            String status = (String) request.get("status");
            String imageUrl = (String) request.get("imageUrl");

            int shopLevel = seller.getShopLevel() != null ? seller.getShopLevel() : 1;

            // Chặn cập nhật biến thể đối với Shop Level 0 & 1 khi ví âm
            long balance = seller.getBalanceVnd() != null ? seller.getBalanceVnd() : 0L;
            if (balance < 0 && (shopLevel == 0 || shopLevel == 1)) {
                return ResponseEntity.badRequest().body(Map.of("message", "Tài khoản của bạn đang có số dư âm. Vui lòng nạp tiền thanh toán nợ để tiếp tục đăng bán sản phẩm."));
            }

            if (shopLevel == 1 && priceObj != null) {
                Long price = Long.valueOf(priceObj.toString());
                if (price > 200000) {
                    return ResponseEntity.badRequest().body(Map.of("message", "Shop Mới (Level 1) chỉ được phép đăng bán sản phẩm có giá tối đa 200.000 VNĐ."));
                }
            }

            if (variantName == null || variantName.trim().isEmpty() || priceObj == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Tên biến thể và giá bán không được để trống."));
            }

            v.setVariantName(variantName);
            v.setPriceVnd(Long.valueOf(priceObj.toString()));
            v.setStock(stockObj != null ? Integer.valueOf(stockObj.toString()) : 0);
            v.setStatus(status != null ? status : "Active");
            if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                v.setImageUrl(imageUrl);
            }
            productVariantRepository.save(v);

            return ResponseEntity.ok(Map.of("message", "Cập nhật biến thể thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 13. Variant DELETE
    @DeleteMapping("/variants/{id}")
    public ResponseEntity<?> deleteVariant(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        try {
            User seller = getSeller(userId);
            ProductVariant v = productVariantRepository.findByIdAndIsDeleteFalse(id)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy biến thể."));

            if (!v.getProduct().getSeller().getId().equals(seller.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Bạn không có quyền xóa biến thể này."));
            }

            v.setIsDelete(true);
            productVariantRepository.save(v);

            // Cascade delete digital assets
            List<DigitalAsset> assets = digitalAssetRepository.findByVariantAndIsDeleteFalse(v);
            for (DigitalAsset asset : assets) {
                asset.setIsDelete(true);
                digitalAssetRepository.save(asset);
            }

            return ResponseEntity.ok(Map.of("message", "Xóa biến thể thành công."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 14. Transactions GET (Sales history)
    @GetMapping("/transactions")
    public ResponseEntity<?> getTransactions(@AuthenticationPrincipal Long userId) {
        try {
            User seller = getSeller(userId);
            List<Transaction> transactions = transactionRepository.findBySellerAndIsDeleteFalseOrderByCreatedAtDesc(seller);

            List<Map<String, Object>> result = transactions.stream().map(t -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", t.getId());
                map.put("customerId", t.getCustomer().getId());
                map.put("customerEmail", t.getCustomer().getEmail());
                map.put("productName", t.getProduct().getName());
                map.put("variantName", t.getVariant().getVariantName());
                map.put("amountVnd", t.getAmountVnd());
                map.put("commissionVnd", t.getCommissionVnd());
                map.put("netEarningVnd", t.getAmountVnd() - t.getCommissionVnd());
                map.put("status", t.getStatus());
                map.put("createdAt", t.getCreatedAt().toString());
                map.put("escrowReleaseDate", t.getEscrowReleaseDate() != null ? t.getEscrowReleaseDate().toString() : "");
                return map;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 15. Withdrawals GET
    @GetMapping("/withdrawals")
    public ResponseEntity<?> getWithdrawals(@AuthenticationPrincipal Long userId) {
        try {
            User seller = getSeller(userId);
            List<Withdrawal> withdrawals = withdrawalRepository.findBySellerAndIsDeleteFalseOrderByCreatedAtDesc(seller);

            List<Map<String, Object>> result = withdrawals.stream().map(w -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", w.getId());
                map.put("amountVnd", w.getAmountVnd());
                map.put("feeVnd", w.getFeeVnd());
                map.put("bankName", w.getBankInfo().getBankName());
                map.put("accountNumber", w.getBankInfo().getAccountNumber());
                map.put("status", w.getStatus());
                map.put("proofFile", w.getProofFile() != null ? w.getProofFile() : "");
                map.put("createdAt", w.getCreatedAt().toString());
                return map;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 15.5. Withdrawal GET by ID
    @GetMapping("/withdrawals/{id}")
    public ResponseEntity<?> getWithdrawalById(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        try {
            User seller = getSeller(userId);
            Withdrawal w = withdrawalRepository.findByIdAndIsDeleteFalse(id)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu rút tiền."));

            if (!w.getSeller().getId().equals(seller.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Bạn không có quyền truy cập thông tin rút tiền này."));
            }

            Map<String, Object> map = new HashMap<>();
            map.put("id", w.getId());
            map.put("amountVnd", w.getAmountVnd());
            map.put("feeVnd", w.getFeeVnd());
            map.put("status", w.getStatus());
            map.put("bankName", w.getBankInfo().getBankName());
            map.put("accountNumber", w.getBankInfo().getAccountNumber());
            map.put("accountHolder", seller.getFullName().toUpperCase());
            map.put("branch", w.getBankInfo().getBranch() != null ? w.getBankInfo().getBranch() : "");
            map.put("proofFile", w.getProofFile() != null ? w.getProofFile() : "");
            map.put("createdAt", w.getCreatedAt().toString());
            map.put("reviewedAt", w.getReviewedAt() != null ? w.getReviewedAt().toString() : "");
            map.put("rejectionReason", w.getRejectionReason() != null ? w.getRejectionReason() : "");
            return ResponseEntity.ok(map);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 15.6. Withdrawal Config GET
    @GetMapping("/withdrawals/config")
    public ResponseEntity<?> getWithdrawalConfig(@AuthenticationPrincipal Long userId) {
        try {
            getSeller(userId); // Verify seller role

            double withdrawalFeePercent = systemConfigurationRepository.findByConfigKey("WITHDRAWAL_FEE_PERCENT")
                    .map(c -> {
                        try { return Double.parseDouble(c.getConfigValue()); }
                        catch (NumberFormatException e) { return 1.5; }
                    }).orElse(1.5);
            long minWithdrawalLimit = systemConfigurationRepository.findByConfigKey("MIN_WITHDRAWAL_VND")
                    .map(c -> {
                        try { return Long.parseLong(c.getConfigValue()); }
                        catch (NumberFormatException e) { return 50000L; }
                    }).orElse(50000L);
            long maxWithdrawalLimit = systemConfigurationRepository.findByConfigKey("MAX_WITHDRAWAL_VND")
                    .map(c -> {
                        try { return Long.parseLong(c.getConfigValue()); }
                        catch (NumberFormatException e) { return 50000000L; }
                    }).orElse(50000000L);
            boolean requireWithdraw2FA = systemConfigurationRepository.findByConfigKey("REQUIRE_WITHDRAW_2FA")
                    .map(c -> Boolean.parseBoolean(c.getConfigValue()))
                    .orElse(false);

            return ResponseEntity.ok(Map.of(
                    "withdrawalFeePercent", withdrawalFeePercent,
                    "minWithdrawalLimit", minWithdrawalLimit,
                    "maxWithdrawalLimit", maxWithdrawalLimit,
                    "requireWithdraw2FA", requireWithdraw2FA
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 15.7. Send Withdrawal OTP (2FA)
    @PostMapping("/withdrawals/send-otp")
    public ResponseEntity<?> sendWithdrawalOtp(@AuthenticationPrincipal Long userId) {
        try {
            User seller = getSeller(userId);
            authenticationService.sendWithdrawalOtp(seller);
            return ResponseEntity.ok(Map.of("message", "Đã gửi mã OTP xác thực rút tiền về email của bạn."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 16. Withdrawal POST (Create request)
    @PostMapping("/withdrawals")
    public ResponseEntity<?> requestWithdrawal(@AuthenticationPrincipal Long userId, @RequestBody Map<String, Object> request) {
        try {
            Object amountObj = request.get("amountVnd");
            if (amountObj == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Vui lòng nhập số tiền rút."));
            }

            long amount = Long.parseLong(amountObj.toString());
            Object otpObj = request.get("otp");
            String otp = otpObj != null ? otpObj.toString() : null;

            Map<String, Object> result = withdrawalService.requestWithdrawal(userId, amount, otp);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }


    // 17. Statistics GET
    @GetMapping("/statistics")
    public ResponseEntity<?> getStatistics(@AuthenticationPrincipal Long userId) {
        try {
            User seller = getSeller(userId);
            List<Transaction> transactions = transactionRepository.findBySellerAndIsDeleteFalseOrderByCreatedAtDesc(seller);

            // Calculate weekly sales chart (last 7 days)
            Map<String, Long> weeklySales = new LinkedHashMap<>();
            LocalDateTime limit = LocalDateTime.now().minusDays(7);

            for (int i = 6; i >= 0; i--) {
                String day = LocalDateTime.now().minusDays(i).getDayOfWeek().toString();
                weeklySales.put(day, 0L);
            }

            transactions.stream()
                    .filter(t -> "Completed".equals(t.getStatus()) && t.getCreatedAt().isAfter(limit))
                    .forEach(t -> {
                        String day = t.getCreatedAt().getDayOfWeek().toString();
                        weeklySales.put(day, weeklySales.getOrDefault(day, 0L) + (t.getAmountVnd() - t.getCommissionVnd()));
                    });

            List<Map<String, Object>> chartData = new ArrayList<>();
            for (Map.Entry<String, Long> entry : weeklySales.entrySet()) {
                Map<String, Object> map = new HashMap<>();
                map.put("label", entry.getKey());
                map.put("value", entry.getValue());
                chartData.add(map);
            }

            // Top products by completed sales
            Map<String, long[]> productStats = new LinkedHashMap<>(); // productName -> [count, revenue]
            transactions.stream()
                    .filter(t -> "Completed".equals(t.getStatus()))
                    .forEach(t -> {
                        String pName = t.getProduct().getName();
                        productStats.computeIfAbsent(pName, k -> new long[]{0, 0});
                        productStats.get(pName)[0]++;
                        productStats.get(pName)[1] += (t.getAmountVnd() - t.getCommissionVnd());
                    });

            List<Map<String, Object>> topProducts = productStats.entrySet().stream()
                    .sorted((a, b) -> Long.compare(b.getValue()[0], a.getValue()[0]))
                    .limit(10)
                    .map(entry -> {
                        Map<String, Object> pm = new HashMap<>();
                        pm.put("productName", entry.getKey());
                        pm.put("soldCount", entry.getValue()[0]);
                        pm.put("revenue", entry.getValue()[1]);
                        return pm;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> stats = new HashMap<>();
            stats.put("chartData", chartData);
            stats.put("totalSalesCount", transactions.stream().filter(t -> "Completed".equals(t.getStatus())).count());
            stats.put("escrowBalance", transactions.stream().filter(t -> "Held".equals(t.getStatus())).mapToLong(t -> t.getAmountVnd() - t.getCommissionVnd()).sum());
            stats.put("topProducts", topProducts);

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 18. Shop Flags GET
    @GetMapping("/shop-flags")
    public ResponseEntity<?> getShopFlags(@AuthenticationPrincipal Long userId) {
        try {
            User seller = getSeller(userId);
            List<ShopFlag> flags = shopFlagRepository.findBySellerAndIsDeleteFalseOrderByCreatedAtDesc(seller);

            List<Map<String, Object>> result = flags.stream().map(f -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", f.getId());
                map.put("flagLevel", f.getFlagLevel());
                map.put("reason", f.getReason());
                map.put("createdAt", f.getCreatedAt().toString());
                map.put("staffName", f.getStaff().getFullName());
                map.put("complaintId", f.getComplaint() != null ? f.getComplaint().getId() : null);
                return map;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 19. Reviews GET
    @GetMapping("/reviews")
    public ResponseEntity<?> getReviews(@AuthenticationPrincipal Long userId) {
        try {
            getSeller(userId);
            List<Review> reviews = reviewRepository.findReviewsBySellerId(userId);

            List<Map<String, Object>> result = reviews.stream().map(r -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", r.getId());
                map.put("productName", r.getProduct().getName());
                map.put("customerName", r.getUser().getFullName());
                map.put("rating", r.getRating());
                map.put("comment", r.getComment());
                map.put("createdAt", r.getCreatedAt().toString());
                return map;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 20. Complaints GET
    @GetMapping("/complaints")
    public ResponseEntity<?> getComplaints(@AuthenticationPrincipal Long userId) {
        try {
            User seller = getSeller(userId);
            List<Complaint> complaints = complaintRepository.findBySellerAndIsDeleteFalseOrderByCreatedAtDesc(seller);

            List<Map<String, Object>> result = complaints.stream().map(c -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", c.getId());
                map.put("transactionId", c.getTransaction().getId());
                map.put("productName", c.getTransaction().getProduct().getName());
                map.put("variantName", c.getTransaction().getVariant().getVariantName());
                map.put("customerEmail", c.getCustomer().getEmail());
                map.put("description", c.getDescription());
                map.put("amountVnd", c.getTransaction().getAmountVnd());
                map.put("status", c.getStatus());
                map.put("createdAt", c.getCreatedAt().toString());
                return map;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 21. Complaint GET Details
    @GetMapping("/complaints/{id}")
    public ResponseEntity<?> getComplaintDetails(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        try {
            User seller = getSeller(userId);
            Complaint c = complaintRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khiếu nại."));

            if (!c.getSeller().getId().equals(seller.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Bạn không có quyền truy cập khiếu nại này."));
            }

            List<Chat> chats = chatRepository.findByComplaintAndIsDeleteFalseOrderByCreatedAtAsc(c);
            List<Map<String, Object>> chatList = chats.stream().map(msg -> {
                Map<String, Object> map = new HashMap<>();
                map.put("senderName", msg.getSender().getFullName());
                String role = "Customer";
                if (msg.getSender().getId().equals(c.getSeller().getId())) {
                    role = "Seller";
                } else if (msg.getSender().getRole() != null && (msg.getSender().getRole().toLowerCase().contains("staff") || msg.getSender().getRole().toLowerCase().contains("admin"))) {
                    role = "Staff";
                }
                map.put("senderRole", role);
                map.put("message", msg.getMessage());
                map.put("createdAt", msg.getCreatedAt().toString());
                return map;
            }).collect(Collectors.toList());

            Map<String, Object> details = new HashMap<>();
            details.put("id", c.getId());
            details.put("transactionId", c.getTransaction().getId());
            details.put("productName", c.getTransaction().getProduct().getName());
            details.put("variantName", c.getTransaction().getVariant().getVariantName());
            details.put("amountVnd", c.getTransaction().getAmountVnd());
            details.put("customerName", c.getCustomer().getFullName());
            details.put("customerEmail", c.getCustomer().getEmail());
            details.put("description", c.getDescription());
            details.put("preferredSolution", c.getPreferredSolution() != null ? c.getPreferredSolution() : "");
            details.put("evidence", c.getEvidence() != null ? c.getEvidence() : "");
            details.put("status", c.getStatus());
            details.put("resolution", c.getResolution() != null ? c.getResolution() : "");
            details.put("createdAt", c.getCreatedAt().toString());
            details.put("chats", chatList);

            return ResponseEntity.ok(details);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 22. Send chat in Complaint
    @PostMapping("/complaints/{id}/chat")
    public ResponseEntity<?> sendComplaintChat(@AuthenticationPrincipal Long userId, @PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            User seller = getSeller(userId);
            Complaint c = complaintRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khiếu nại."));

            if (!c.getSeller().getId().equals(seller.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Bạn không có quyền chat trong khiếu nại này."));
            }

            if (!"In_Progress".equalsIgnoreCase(c.getStatus()) && !"InProgress".equalsIgnoreCase(c.getStatus())) {
                return ResponseEntity.badRequest().body(Map.of("message", "Phòng chat đối chất chưa được mở hoặc đã kết thúc."));
            }

            String msgText = request.get("message");
            if (msgText == null || msgText.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Tin nhắn không được để trống."));
            }

            Chat chat = new Chat();
            chat.setComplaint(c);
            chat.setSender(seller);
            chat.setReceiver(c.getCustomer()); // Customer is receiver
            chat.setChatType("Complaint");
            chat.setMessage(msgText);
            chat.setIsDelete(false);
            chatRepository.save(chat);

            return ResponseEntity.ok(Map.of("message", "Gửi tin nhắn thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ========== DIGITAL ASSETS ENDPOINTS ==========

    // 23. Get Digital Assets for a Variant
    @GetMapping("/variants/{variantId}/assets")
    public ResponseEntity<?> getVariantAssets(@AuthenticationPrincipal Long userId, @PathVariable Long variantId) {
        try {
            User seller = getSeller(userId);
            ProductVariant variant = productVariantRepository.findByIdAndIsDeleteFalse(variantId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy biến thể."));

            if (!variant.getProduct().getSeller().getId().equals(seller.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Bạn không có quyền xem tài sản này."));
            }

            List<DigitalAsset> assets = digitalAssetRepository.findByVariantAndIsDeleteFalse(variant);
            List<Map<String, Object>> result = assets.stream().map(asset -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", asset.getId());
                map.put("assetType", asset.getAssetType());
                map.put("accountUsername", asset.getAccountUsername());
                map.put("keyCode", asset.getKeyCode());
                map.put("cardCode", asset.getCardCode());
                map.put("notes", asset.getNotes());
                map.put("isUsed", asset.getIsUsed());
                map.put("createdAt", asset.getCreatedAt().toString());
                return map;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 24. Create Digital Assets (Batch or Single)
    @PostMapping("/digital-assets")
    public ResponseEntity<?> createDigitalAssets(@AuthenticationPrincipal Long userId, @RequestBody Map<String, Object> request) {
        try {
            User seller = getSeller(userId);
            String statusErr = validateShopActiveStatus(seller);
            if (statusErr != null) {
                return ResponseEntity.badRequest().body(Map.of("message", statusErr));
            }
            Object variantIdObj = request.get("variantId");
            String assetType = (String) request.get("assetType");
            List<Map<String, Object>> assetsList = (List<Map<String, Object>>) request.get("assets");

            if (variantIdObj == null || assetType == null || assetsList == null || assetsList.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Thông tin biến thể và danh sách tài sản không được để trống."));
            }

            Long variantId = Long.valueOf(variantIdObj.toString());
            ProductVariant variant = productVariantRepository.findByIdAndIsDeleteFalse(variantId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy biến thể."));

            if (!variant.getProduct().getSeller().getId().equals(seller.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Bạn không có quyền thêm tài sản cho biến thể này."));
            }

            List<DigitalAsset> savedAssets = new ArrayList<>();
            for (Map<String, Object> assetData : assetsList) {
                DigitalAsset asset = new DigitalAsset();
                asset.setVariant(variant);
                asset.setAssetType(assetType);
                asset.setIsUsed(false);
                asset.setIsDelete(false);

                // Validate and set fields based on asset type
                if ("ACCOUNT".equals(assetType)) {
                    String username = (String) assetData.get("accountUsername");
                    String password = (String) assetData.get("accountPassword");
                    if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
                        return ResponseEntity.badRequest().body(Map.of("message", "Tài khoản ACCOUNT phải có username và password."));
                    }
                    asset.setAccountUsername(username);
                    asset.setAccountPassword(password);
                } else if ("KEY".equals(assetType)) {
                    String keyCode = (String) assetData.get("keyCode");
                    if (keyCode == null || keyCode.trim().isEmpty()) {
                        return ResponseEntity.badRequest().body(Map.of("message", "Tài sản KEY phải có mã key."));
                    }
                    // Check for duplicate key
                    long existingCount = digitalAssetRepository.findByVariantAndIsDeleteFalse(variant)
                            .stream()
                            .filter(a -> "KEY".equals(a.getAssetType()) && keyCode.equals(a.getKeyCode()))
                            .count();
                    if (existingCount > 0) {
                        return ResponseEntity.badRequest().body(Map.of("message", "Mã key này đã tồn tại trong kho."));
                    }
                    asset.setKeyCode(keyCode);
                } else if ("GAME_CARD".equals(assetType)) {
                    String cardCode = (String) assetData.get("cardCode");
                    String cardPin = (String) assetData.get("cardPin");
                    if (cardCode == null || cardCode.trim().isEmpty()) {
                        return ResponseEntity.badRequest().body(Map.of("message", "Thẻ nạp phải có mã thẻ."));
                    }
                    asset.setCardCode(cardCode);
                    asset.setCardPin(cardPin != null ? cardPin : "");
                } else {
                    return ResponseEntity.badRequest().body(Map.of("message", "Loại tài sản không hợp lệ."));
                }

                // Set notes
                String notes = (String) assetData.get("notes");
                if (notes != null && !notes.trim().isEmpty()) {
                    asset.setNotes(notes);
                }

                // Set JSON data for backward compatibility
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                asset.setAssetData(mapper.writeValueAsString(assetData));

                savedAssets.add(digitalAssetRepository.save(asset));
            }

            // Tự động xử lý Đơn đặt trước (Pre-Order)
            try {
                preOrderService.autoFulfillPreOrders(variant, savedAssets);
            } catch (Exception ex) {
                log.error("Tự động giao preorder thất bại cho variant {}.", variant.getId(), ex);
            }

            // Recalculate and update the variant stock!
            if (!"SERVICE".equals(variant.getProduct().getProductType())) {
                List<DigitalAsset> activeAssets = digitalAssetRepository.findByVariantAndIsDeleteFalse(variant);
                int availableStock = (int) activeAssets.stream().filter(a -> !a.getIsUsed()).count();
                variant.setStock(availableStock);
                productVariantRepository.save(variant);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("message", "Thêm " + savedAssets.size() + " tài sản thành công!");
            result.put("count", savedAssets.size());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 25. Delete Digital Asset
    @DeleteMapping("/digital-assets/{id}")
    public ResponseEntity<?> deleteDigitalAsset(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        try {
            User seller = getSeller(userId);
            String statusErr = validateShopActiveStatus(seller);
            if (statusErr != null) {
                return ResponseEntity.badRequest().body(Map.of("message", statusErr));
            }
            DigitalAsset asset = digitalAssetRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài sản."));

            if (!asset.getVariant().getProduct().getSeller().getId().equals(seller.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Bạn không có quyền xóa tài sản này."));
            }

            asset.setIsDelete(true);
            digitalAssetRepository.save(asset);

            // Recalculate and update the variant stock!
            ProductVariant variant = asset.getVariant();
            if (!"SERVICE".equals(variant.getProduct().getProductType())) {
                List<DigitalAsset> activeAssets = digitalAssetRepository.findByVariantAndIsDeleteFalse(variant);
                int availableStock = (int) activeAssets.stream().filter(a -> !a.getIsUsed()).count();
                variant.setStock(availableStock);
                productVariantRepository.save(variant);
            }

            return ResponseEntity.ok(Map.of("message", "Xóa tài sản thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 26. Update Product Type and Image
    @PutMapping("/products/{id}/details")
    public ResponseEntity<?> updateProductDetails(@AuthenticationPrincipal Long userId, @PathVariable Long id, @RequestBody Map<String, Object> request) {
        try {
            User seller = getSeller(userId);
            String statusErr = validateShopActiveStatus(seller);
            if (statusErr != null) {
                return ResponseEntity.badRequest().body(Map.of("message", statusErr));
            }
            Product p = productRepository.findByIdAndIsDeleteFalse(id)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm."));

            if (!p.getSeller().getId().equals(seller.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Bạn không có quyền chỉnh sửa sản phẩm này."));
            }

            String productType = (String) request.get("productType");
            String imageUrl = (String) request.get("productImageUrl");

            if ("ACCOUNT".equals(productType) || "KEY".equals(productType) || "GAME_CARD".equals(productType) || "SERVICE".equals(productType)) {
                p.setProductType(productType);
            }

            if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                p.setProductImageUrl(imageUrl);
            }

            productRepository.save(p);
            return ResponseEntity.ok(Map.of("message", "Cập nhật thông tin sản phẩm thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
