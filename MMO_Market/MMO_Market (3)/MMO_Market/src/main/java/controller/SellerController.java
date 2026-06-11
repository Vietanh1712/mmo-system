package controller;

import dal.*;
import model.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/seller")
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

    public SellerController(UserRepository userRepository, ProductRepository productRepository,
                            ProductVariantRepository productVariantRepository, CategoryRepository categoryRepository,
                            TransactionRepository transactionRepository, WithdrawalRepository withdrawalRepository,
                            ComplaintRepository complaintRepository, ShopFlagRepository shopFlagRepository,
                            ReviewRepository reviewRepository, ChatRepository chatRepository,
                            SellerBankInfoRepository sellerBankInfoRepository,
                            SellerRegistrationRepository sellerRegistrationRepository,
                            DigitalAssetRepository digitalAssetRepository) {
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
    }

    private User getSeller(Long userId) {
        return userRepository.findByIdAndIsDeleteFalse(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin tài khoản người bán."));
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
            SellerRegistration reg = sellerRegistrationRepository.findByUserAndIsDeleteFalse(seller).orElse(null);
            SellerBankInfo bank = sellerBankInfoRepository.findByUserAndIsDeleteFalse(seller).orElse(null);

            Map<String, Object> result = new HashMap<>();
            result.put("fullName", seller.getFullName());
            result.put("shopStatus", seller.getShopStatus());
            result.put("shopName", reg != null ? reg.getShopName() : "Cửa hàng của tôi");
            result.put("description", reg != null ? reg.getDescription() : "");
            result.put("bankName", bank != null ? bank.getBankName() : "");
            result.put("accountNumber", bank != null ? bank.getAccountNumber() : "");
            result.put("accountHolder", seller.getFullName().toUpperCase());
            result.put("branch", bank != null ? bank.getBranch() : "");

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

            SellerRegistration reg = sellerRegistrationRepository.findByUserAndIsDeleteFalse(seller)
                    .orElse(new SellerRegistration());
            reg.setUser(seller);
            reg.setShopName(shopName);
            reg.setDescription(description);
            sellerRegistrationRepository.save(reg);

            if (bankName != null && !bankName.trim().isEmpty() && accountNumber != null && !accountNumber.trim().isEmpty()) {
                SellerBankInfo bank = sellerBankInfoRepository.findByUserAndIsDeleteFalse(seller)
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

    // 4. Load Categories for dropdown filter/product creation
    @GetMapping("/categories")
    public ResponseEntity<?> getCategories() {
        // Return only sub-categories
        List<Category> allCategories = categoryRepository.findAllByIsDeleteFalseOrderByCreatedAtDesc();
        List<Map<String, Object>> result = allCategories.stream()
                .filter(c -> c.getParent() != null)
                .map(c -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", c.getId());
                    map.put("name", c.getName());
                    return map;
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

                List<ProductVariant> variants = productVariantRepository.findByProductAndIsDeleteFalse(p);
                map.put("variantCount", variants.size());
                map.put("totalStock", variants.stream().mapToInt(v -> v.getStock() != null ? v.getStock() : 0).sum());
                map.put("status", variants.stream().anyMatch(v -> "Active".equals(v.getStatus())) ? "Active" : "Locked");
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
                return vMap;
            }).collect(Collectors.toList());

            Map<String, Object> map = new HashMap<>();
            map.put("id", p.getId());
            map.put("name", p.getName());
            map.put("categoryId", p.getCategory().getId());
            map.put("categoryName", p.getCategory().getName());
            map.put("description", p.getDescription());
            map.put("image", p.getImage());
            map.put("variants", variantList);

            return ResponseEntity.ok(map);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 7. Product POST (Create)
    @PostMapping("/products")
    public ResponseEntity<?> createProduct(@AuthenticationPrincipal Long userId, @RequestBody Map<String, Object> request) {
        try {
            User seller = getSeller(userId);
            String name = (String) request.get("name");
            String description = (String) request.get("description");
            Object catIdObj = request.get("categoryId");

            if (name == null || name.trim().isEmpty() || catIdObj == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Thông tin tên sản phẩm và danh mục không được để trống."));
            }

            Long categoryId = Long.valueOf(catIdObj.toString());
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục được chọn."));

            Product p = new Product();
            p.setSeller(seller);
            p.setCategory(category);
            p.setName(name);
            p.setDescription(description);
            p.setIsDelete(false);
            p.setImage("https://via.placeholder.com/300x160/2563eb/ffffff?text=MMO+Market");
            Product saved = productRepository.save(p);

            return ResponseEntity.ok(Map.of("id", saved.getId(), "message", "Đã tạo sản phẩm thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 8. Product PUT (Update)
    @PutMapping("/products/{id}")
    public ResponseEntity<?> updateProduct(@AuthenticationPrincipal Long userId, @PathVariable Long id, @RequestBody Map<String, Object> request) {
        try {
            User seller = getSeller(userId);
            Product p = productRepository.findByIdAndIsDeleteFalse(id)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm."));

            if (!p.getSeller().getId().equals(seller.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Bạn không có quyền chỉnh sửa sản phẩm này."));
            }

            String name = (String) request.get("name");
            String description = (String) request.get("description");
            Object catIdObj = request.get("categoryId");

            if (name == null || name.trim().isEmpty() || catIdObj == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Tên sản phẩm và danh mục không được để trống."));
            }

            Long categoryId = Long.valueOf(catIdObj.toString());
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục."));

            p.setName(name);
            p.setDescription(description);
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
            Object prodIdObj = request.get("productId");
            String variantName = (String) request.get("variantName");
            Object priceObj = request.get("priceVnd");
            Object stockObj = request.get("stock");
            String status = (String) request.get("status");

            if (prodIdObj == null || variantName == null || variantName.trim().isEmpty() || priceObj == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Thông tin tên biến thể và giá bán không được để trống."));
            }

            Long productId = Long.valueOf(prodIdObj.toString());
            Product p = productRepository.findByIdAndIsDeleteFalse(productId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm."));

            if (!p.getSeller().getId().equals(seller.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Bạn không có quyền thao tác trên sản phẩm này."));
            }

            ProductVariant v = new ProductVariant();
            v.setProduct(p);
            v.setVariantName(variantName);
            v.setPriceVnd(Long.valueOf(priceObj.toString()));
            v.setStock(stockObj != null ? Integer.valueOf(stockObj.toString()) : 0);
            v.setStatus(status != null ? status : "Active");
            v.setIsDelete(false);
            productVariantRepository.save(v);

            return ResponseEntity.ok(Map.of("message", "Tạo biến thể thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 12. Variant PUT (Update)
    @PutMapping("/variants/{id}")
    public ResponseEntity<?> updateVariant(@AuthenticationPrincipal Long userId, @PathVariable Long id, @RequestBody Map<String, Object> request) {
        try {
            User seller = getSeller(userId);
            ProductVariant v = productVariantRepository.findByIdAndIsDeleteFalse(id)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy biến thể."));

            if (!v.getProduct().getSeller().getId().equals(seller.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Bạn không có quyền cập nhật biến thể này."));
            }

            String variantName = (String) request.get("variantName");
            Object priceObj = request.get("priceVnd");
            Object stockObj = request.get("stock");
            String status = (String) request.get("status");

            if (variantName == null || variantName.trim().isEmpty() || priceObj == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Tên biến thể và giá bán không được để trống."));
            }

            v.setVariantName(variantName);
            v.setPriceVnd(Long.valueOf(priceObj.toString()));
            v.setStock(stockObj != null ? Integer.valueOf(stockObj.toString()) : 0);
            v.setStatus(status != null ? status : "Active");
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
            map.put("status", w.getStatus());
            map.put("bankName", w.getBankInfo().getBankName());
            map.put("accountNumber", w.getBankInfo().getAccountNumber());
            map.put("accountHolder", seller.getFullName().toUpperCase());
            map.put("branch", w.getBankInfo().getBranch() != null ? w.getBankInfo().getBranch() : "");
            map.put("proofFile", w.getProofFile() != null ? w.getProofFile() : "");
            map.put("createdAt", w.getCreatedAt().toString());
            return ResponseEntity.ok(map);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 16. Withdrawal POST (Create request)
    @PostMapping("/withdrawals")
    public ResponseEntity<?> requestWithdrawal(@AuthenticationPrincipal Long userId, @RequestBody Map<String, Object> request) {
        try {
            User seller = getSeller(userId);
            Object amountObj = request.get("amountVnd");

            if (amountObj == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Vui lòng nhập số tiền rút."));
            }

            long amount = Long.parseLong(amountObj.toString());
            if (amount < 50000) {
                return ResponseEntity.badRequest().body(Map.of("message", "Số tiền rút tối thiểu phải là 50,000 VNĐ."));
            }

            if (seller.getBalanceVnd() < amount) {
                return ResponseEntity.badRequest().body(Map.of("message", "Số dư ví không đủ để thực hiện yêu cầu này."));
            }

            SellerBankInfo bank = sellerBankInfoRepository.findByUserAndIsDeleteFalse(seller)
                    .orElseThrow(() -> new IllegalArgumentException("Vui lòng cấu hình thông tin ngân hàng trước khi rút tiền."));

            // Deduct balance and save
            seller.setBalanceVnd(seller.getBalanceVnd() - amount);
            userRepository.save(seller);

            Withdrawal w = new Withdrawal();
            w.setSeller(seller);
            w.setBankInfo(bank);
            w.setAmountVnd(amount);
            w.setStatus("Pending");
            w.setIsDelete(false);
            withdrawalRepository.save(w);

            return ResponseEntity.ok(Map.of(
                    "newBalance", seller.getBalanceVnd(),
                    "message", "Yêu cầu rút tiền đã được gửi thành công!"
            ));
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
                map.put("senderRole", msg.getSender().getEmail().contains("seller") ? "Seller" : (msg.getSender().getEmail().contains("staff") ? "Staff" : "Customer"));
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
                    if (cardCode == null || cardCode.trim().isEmpty() || cardPin == null || cardPin.trim().isEmpty()) {
                        return ResponseEntity.badRequest().body(Map.of("message", "Thẻ GAME_CARD phải có mã thẻ và PIN."));
                    }
                    asset.setCardCode(cardCode);
                    asset.setCardPin(cardPin);
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
            DigitalAsset asset = digitalAssetRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài sản."));

            if (!asset.getVariant().getProduct().getSeller().getId().equals(seller.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Bạn không có quyền xóa tài sản này."));
            }

            asset.setIsDelete(true);
            digitalAssetRepository.save(asset);

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
            Product p = productRepository.findByIdAndIsDeleteFalse(id)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm."));

            if (!p.getSeller().getId().equals(seller.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Bạn không có quyền chỉnh sửa sản phẩm này."));
            }

            String productType = (String) request.get("productType");
            String imageUrl = (String) request.get("productImageUrl");

            if ("ACCOUNT".equals(productType) || "KEY".equals(productType) || "GAME_CARD".equals(productType)) {
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
