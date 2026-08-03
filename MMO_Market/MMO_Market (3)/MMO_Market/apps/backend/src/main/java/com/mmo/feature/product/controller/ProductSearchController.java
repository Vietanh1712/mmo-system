package com.mmo.feature.product.controller;
import com.mmo.shared.dto.ProductDetailDTO;
import com.mmo.shared.model.Transaction;
import com.mmo.shared.model.User;
import com.mmo.shared.dal.ShopFollowerRepository;
import com.mmo.shared.dto.ReviewRequestDTO;
import com.mmo.shared.dal.ProductRepository;
import com.mmo.shared.dal.TransactionRepository;
import com.mmo.shared.model.Product;
import com.mmo.shared.dal.ReviewRepository;
import com.mmo.shared.dal.UserRepository;
import com.mmo.shared.dto.ReviewResponseDTO;
import com.mmo.shared.model.Review;
import com.mmo.shared.model.ShopFollower;
import com.mmo.shared.dal.ComplaintRepository;

import com.mmo.shared.dto.FeaturedProductDTO;
import com.mmo.shared.dto.ProductSearchResultDTO;
import com.mmo.shared.dal.CategoryRepository;
import com.mmo.shared.model.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.mmo.feature.product.service.ProductSearchService;
import com.mmo.feature.product.service.ProductService;

import java.util.List;

@RestController
@RequestMapping("/api/search")
public class ProductSearchController {

    @Autowired
    private ProductSearchService productSearchService;

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private com.mmo.shared.dal.UserRepository userRepository;

    @Autowired
    private com.mmo.shared.dal.ProductRepository productRepository;

    /**
     * API lấy sản phẩm nổi bật cho homepage dựa trên top lượt bán thực tế từ DB.
     * GET /api/search/products/featured?limit=8
     */
    @GetMapping("/products/featured")
    public ResponseEntity<List<FeaturedProductDTO>> getFeaturedProducts(
            @RequestParam(defaultValue = "8") int limit) {
        List<FeaturedProductDTO> featured = productService.getFeaturedProducts(limit);
        return ResponseEntity.ok(featured);
    }

    @Autowired
    private com.mmo.shared.dal.ReviewRepository reviewRepository;

    @Autowired
    private com.mmo.shared.dal.TransactionRepository transactionRepository;

    @Autowired
    private com.mmo.shared.dal.ShopFollowerRepository shopFollowerRepository;

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private com.mmo.shared.dal.ChatRepository chatRepository;

    @GetMapping("/products")
    public ResponseEntity<Page<ProductSearchResultDTO>> searchProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String subCategory,
            @RequestParam(required = false) Long minPrice,
            @RequestParam(required = false) Long maxPrice,
            @RequestParam(required = false) String stockStatus, // e.g., "In Stock"
            @RequestParam(required = false) Long sellerId,
            @RequestParam(required = false) List<Integer> rating,
            @PageableDefault(size = 12, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {

        Page<ProductSearchResultDTO> results = productSearchService.searchProducts(
                keyword, categoryId, subCategory, minPrice, maxPrice, stockStatus, sellerId, rating, pageable);

        return ResponseEntity.ok(results);
    }

    /**
     * API lấy thông tin chi tiết một sản phẩm kèm thống kê thực tế từ database
     */
    @GetMapping("/products/{productId}")
    public ResponseEntity<com.mmo.shared.dto.ProductDetailDTO> getProductDetail(@PathVariable Long productId) {
        return productRepository.findByIdAndIsDeleteFalse(productId)
                .filter(product -> {
                    com.mmo.shared.model.User seller = product.getSeller();
                    if (seller == null || Boolean.TRUE.equals(seller.getIsDelete())) return false;
                    if (seller.getSuspendedUntil() != null && java.time.LocalDateTime.now().isBefore(seller.getSuspendedUntil())) {
                        return false;
                    }
                    String status = seller.getShopStatus();
                    if (status == null) return true;
                    return !status.equalsIgnoreCase("Locked") && 
                           !status.equalsIgnoreCase("Banned") && 
                           !status.equalsIgnoreCase("Pending") && 
                           !status.equalsIgnoreCase("Suspended") && 
                           !status.equalsIgnoreCase("TEMP_LOCKED") && 
                           !status.equalsIgnoreCase("Withdrawn") && 
                           !status.equalsIgnoreCase("DELETED") && 
                           !status.equalsIgnoreCase("INDEFINITE_LOCKED") && 
                           !status.equalsIgnoreCase("PERMANENT_BANNED");
                })
                .map(product -> {
                    // Query statistics from DB
                    Double avgRating = reviewRepository.findAverageRatingByProductId(product.getId());
                    Long reviewsCount = reviewRepository.countByProductIdAndIsDeleteFalse(product.getId());
                    Long salesCount = transactionRepository.countByProductIdAndIsDeleteFalse(product.getId());

                    // Map variants to DTOs
                    List<com.mmo.shared.dto.ProductDetailDTO.VariantDTO> variantDTOs = new java.util.ArrayList<>();
                    if (product.getVariants() != null) {
                        product.getVariants().stream()
                                .filter(v -> v.getIsDelete() != null && !v.getIsDelete())
                                .forEach(v -> {
                                    variantDTOs.add(com.mmo.shared.dto.ProductDetailDTO.VariantDTO.builder()
                                            .id(v.getId())
                                            .name(v.getVariantName())
                                            .price(v.getPriceVnd())
                                            .oldPrice((long) (v.getPriceVnd() * 1.5)) // High-fidelity mock old price
                                            .duration(v.getVariantName().contains("Năm") || v.getVariantName().contains("12 Tháng") ? 12 : (v.getVariantName().contains("6 Tháng") ? 6 : (v.getVariantName().contains("3 Tháng") ? 3 : 1)))
                                            .label(v.getVariantName())
                                            .stock(v.getStock() != null ? v.getStock() : 0)
                                            .build());
                                });
                    }

                    // Find cheapest price & stock
                    long cheapestPrice = 0L;
                    int totalStock = 0;
                    if (!variantDTOs.isEmpty()) {
                        cheapestPrice = variantDTOs.stream()
                                .mapToLong(com.mmo.shared.dto.ProductDetailDTO.VariantDTO::getPrice)
                                .min().orElse(0L);
                        totalStock = product.getVariants().stream()
                                .filter(v -> v.getIsDelete() != null && !v.getIsDelete())
                                .mapToInt(v -> v.getStock() != null ? v.getStock() : 0)
                                .sum();
                    }

                    String sellerName = "VipStore";
                    Boolean sellerIsVerified = false;
                    Long sellerId = 0L;
                    if (product.getSeller() != null) {
                        sellerName = product.getSeller().getFullName() != null ? product.getSeller().getFullName() : "VipStore";
                        sellerIsVerified = Boolean.TRUE.equals(product.getSeller().getIsVerified());
                        sellerId = product.getSeller().getId();
                    }

                    String categoryName = "Sản phẩm số";
                    Long catId = 0L;
                    if (product.getCategory() != null) {
                        categoryName = product.getCategory().getName() != null ? product.getCategory().getName() : "Sản phẩm số";
                        catId = product.getCategory().getId();
                    }

                    com.mmo.shared.dto.ProductDetailDTO detail = com.mmo.shared.dto.ProductDetailDTO.builder()
                            .id(product.getId())
                            .name(product.getName())
                            .description(product.getDescription())
                            .imageUrl(product.getImage())
                            .categoryId(catId)
                            .categoryName(categoryName)
                            .sellerId(sellerId)
                            .sellerName(sellerName)
                            .sellerIsVerified(sellerIsVerified)
                            .stock(totalStock)
                            .price(cheapestPrice)
                            .averageRating(avgRating != null ? avgRating : 0.0)
                            .reviewsCount(reviewsCount != null ? reviewsCount : 0L)
                            .salesCount(salesCount != null ? salesCount : 0L)
                            .variants(variantDTOs)
                            .userGuide(product.getUserGuide())
                            .build();

                    return ResponseEntity.ok(detail);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * API lấy danh sách đánh giá của sản phẩm bằng tiếng Việt từ DB
     */
    @GetMapping("/products/{productId}/reviews")
    public ResponseEntity<List<com.mmo.shared.dto.ReviewResponseDTO>> getProductReviews(@PathVariable Long productId) {
        List<com.mmo.shared.model.Review> reviews = reviewRepository.findByProductIdAndIsDeleteFalse(productId);
        List<com.mmo.shared.dto.ReviewResponseDTO> dtos = reviews.stream()
                .map(r -> com.mmo.shared.dto.ReviewResponseDTO.builder()
                        .id(r.getId())
                        .userName(r.getUser() != null && r.getUser().getFullName() != null ? r.getUser().getFullName() : "Người dùng MMO")
                        .rating(r.getRating())
                        .comment(r.getComment())
                        .mediaUrl(r.getMediaUrl())
                        .createdAt(r.getCreatedAt())
                        .build())
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * API lưu đánh giá của người dùng cho một sản phẩm vào DB.
     * Luồng hợp lệ: User phải có ít nhất 1 giao dịch HOÀN THÀNH (Completed) với sản phẩm này.
     */
    @org.springframework.web.bind.annotation.PostMapping("/products/{productId}/reviews")
    public ResponseEntity<?> submitProductReview(
            @PathVariable Long productId,
            @org.springframework.security.core.annotation.AuthenticationPrincipal Long userId,
            @RequestBody com.mmo.shared.dto.ReviewRequestDTO request) {

        if (userId == null) {
            return ResponseEntity.status(401).body(java.util.Map.of("message", "Chưa đăng nhập."));
        }

        if (request.getRating() == null || request.getRating() < 1 || request.getRating() > 5) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", "Số sao đánh giá phải từ 1 đến 5."));
        }

        java.util.Optional<com.mmo.shared.model.User> userOpt = userRepository.findByIdAndIsDeleteFalse(userId);
        if (!userOpt.isPresent()) {
            return ResponseEntity.status(401).body(java.util.Map.of("message", "Người dùng không tồn tại."));
        }

        java.util.Optional<com.mmo.shared.model.Product> productOpt = productRepository.findByIdAndIsDeleteFalse(productId);
        if (!productOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        // ============================================================
        // BẮT BUỘC: Kiểm tra user đã MUA và ĐƠN HÀNG ĐÃ HOÀN THÀNH
        // (status = 'Completed' hoặc 'Held') trước khi cho phép đánh giá.
        // ============================================================
        Long transactionId = request.getTransactionId();

        // Nếu có transactionId: validate giao dịch thuộc về user và sản phẩm này
        if (transactionId != null) {
            java.util.Optional<com.mmo.shared.model.Transaction> txOpt = transactionRepository.findById(transactionId);
            if (!txOpt.isPresent() || txOpt.get().getIsDelete()) {
                return ResponseEntity.status(403).body(java.util.Map.of(
                        "message", "Giao dịch không tồn tại hoặc đã bị xóa."
                ));
            }
            com.mmo.shared.model.Transaction tx = txOpt.get();
            if (!tx.getCustomer().getId().equals(userId)) {
                return ResponseEntity.status(403).body(java.util.Map.of(
                        "message", "Giao dịch này không thuộc về tài khoản của bạn."
                ));
            }
            if (!tx.getProduct().getId().equals(productId)) {
                return ResponseEntity.status(403).body(java.util.Map.of(
                        "message", "Giao dịch này không liên quan đến sản phẩm đang được đánh giá."
                ));
            }
            if (!java.util.List.of("Completed", "Held").contains(tx.getStatus())) {
                return ResponseEntity.status(403).body(java.util.Map.of(
                        "message", "Đơn hàng chưa hoàn thành. Chỉ đơn đã thanh toán mới được đánh giá."
                ));
            }
            // Kiểm tra giao dịch CỤ THỂ này đã được đánh giá chưa
            boolean txAlreadyReviewed = reviewRepository.existsByTransactionIdAndIsDeleteFalse(transactionId);
            if (txAlreadyReviewed) {
                return ResponseEntity.status(409).body(java.util.Map.of(
                        "message", "Đơn hàng này đã được đánh giá rồi. Mỗi đơn hàng chỉ được đánh giá một lần."
                ));
            }
        } else {
            // Fallback (backward compatibility): kiểm tra đã mua sản phẩm chưa
            boolean hasPurchased = transactionRepository.existsCompletedPurchaseByCustomerAndProduct(userId, productId);
            if (!hasPurchased) {
                return ResponseEntity.status(403).body(java.util.Map.of(
                        "message", "Bạn chưa mua sản phẩm này hoặc đơn hàng chưa hoàn thành. Chỉ người mua hàng thành công mới được đánh giá."
                ));
            }
            // Fallback: chặn review trùng ở cấp độ sản phẩm
            boolean alreadyReviewed = reviewRepository.existsByProductIdAndUserIdAndIsDeleteFalse(productId, userId);
            if (alreadyReviewed) {
                return ResponseEntity.status(409).body(java.util.Map.of(
                        "message", "Bạn đã đánh giá sản phẩm này rồi. Vui lòng gửi đánh giá từ trang chi tiết đơn hàng."
                ));
            }
        }

        com.mmo.shared.model.User user = userOpt.get();
        com.mmo.shared.model.Product product = productOpt.get();

        com.mmo.shared.model.Review review = com.mmo.shared.model.Review.builder()
                .product(product)
                .user(user)
                .transactionId(transactionId)
                .rating(request.getRating())
                .comment(request.getComment() != null ? request.getComment() : "")
                .mediaUrl(request.getMediaUrl())
                .isDelete(false)
                .build();
        com.mmo.shared.model.Review saved = reviewRepository.save(review);

        com.mmo.shared.dto.ReviewResponseDTO responseDTO = com.mmo.shared.dto.ReviewResponseDTO.builder()
                .id(saved.getId())
                .userName(user.getFullName() != null ? user.getFullName() : "Người dùng MMO")
                .rating(saved.getRating())
                .comment(saved.getComment())
                .mediaUrl(saved.getMediaUrl())
                .createdAt(saved.getCreatedAt())
                .build();

        return ResponseEntity.ok(responseDTO);
    }


    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<?> getSellerProfile(
            @PathVariable Long sellerId,
            @org.springframework.security.core.annotation.AuthenticationPrincipal Long activeUserId) {
        return userRepository.findByIdAndIsDeleteFalse(sellerId)
                .map(user -> {
                    long productCount = productRepository.countBySellerIdAndIsDeleteFalse(sellerId);
                    
                    // Format joined date: e.g., "tháng 06/2026"
                    String joinedDate = "tháng " + (user.getCreatedAt() != null 
                        ? user.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("MM/yyyy"))
                        : "06/2026");
                    
                    long followerCount = shopFollowerRepository.countBySellerIdAndIsDeleteFalse(sellerId);
                    boolean isFollowing = false;
                    if (activeUserId != null) {
                        isFollowing = shopFollowerRepository.findByFollowerIdAndSellerIdAndIsDeleteFalse(activeUserId, sellerId).isPresent();
                    }

                    long completedCount = transactionRepository.countCompletedSalesBySeller(user);
                    long totalSold = transactionRepository.countTotalSalesBySeller(user);
                    long resolvedComplaints = complaintRepository.countResolvedComplaintsBySeller(user);

                    double disputeRate = totalSold > 0 ? (double) resolvedComplaints / totalSold : 0.0;
                    int shopLevel = 1;
                    if (disputeRate >= 0.02) {
                        shopLevel = 0;
                    } else if (completedCount >= 20) {
                        shopLevel = 2;
                    }

                    java.util.Map<String, Object> profile = new java.util.HashMap<>();
                    profile.put("id", user.getId());
                    profile.put("shopName", user.getFullName() != null ? user.getFullName() : "Gian hàng đối tác");
                    profile.put("joinedDate", joinedDate);
                    profile.put("isVerified", Boolean.TRUE.equals(user.getIsVerified()));
                    profile.put("totalProducts", productCount);
                    Double avgSellerRating = reviewRepository.findAverageRatingBySellerId(sellerId);
                    profile.put("rating", avgSellerRating != null ? avgSellerRating : 0.0);
                    
                    Double avgMinutes = chatRepository.findAverageResponseTimeInMinutes(sellerId, java.time.LocalDateTime.now().minusDays(30));
                    String responseTimeStr = "Trong vài giờ";
                    if (avgMinutes != null) {
                        if (avgMinutes <= 60) {
                            responseTimeStr = "Trong vài phút";
                        } else if (avgMinutes <= 300) {
                            responseTimeStr = "Trong vài giờ";
                        } else if (avgMinutes <= 1440) {
                            responseTimeStr = "Trong vòng 1 ngày";
                        } else {
                            responseTimeStr = "Trong vài ngày";
                        }
                    }
                    profile.put("responseTime", responseTimeStr);

                    profile.put("email", user.getEmail());
                    profile.put("followerCount", followerCount);
                    profile.put("isFollowing", isFollowing);
                    profile.put("shopLevel", shopLevel);
                    return ResponseEntity.ok(profile);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/seller/{sellerId}/follow")
    public ResponseEntity<?> toggleFollowSeller(
            @PathVariable Long sellerId,
            @org.springframework.security.core.annotation.AuthenticationPrincipal Long activeUserId) {
        
        if (activeUserId == null) {
            return ResponseEntity.status(401).body(java.util.Map.of("message", "Chương trình yêu cầu đăng nhập."));
        }

        if (activeUserId.equals(sellerId)) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", "Bạn không thể tự theo dõi chính mình."));
        }

        java.util.Optional<com.mmo.shared.model.User> followerOpt = userRepository.findByIdAndIsDeleteFalse(activeUserId);
        java.util.Optional<com.mmo.shared.model.User> sellerOpt = userRepository.findByIdAndIsDeleteFalse(sellerId);

        if (!followerOpt.isPresent() || !sellerOpt.isPresent()) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", "Người dùng không tồn tại."));
        }

        java.util.Optional<com.mmo.shared.model.ShopFollower> existingFollowOpt = 
                shopFollowerRepository.findByFollowerIdAndSellerId(activeUserId, sellerId);

        boolean isFollowingNow;
        if (existingFollowOpt.isPresent()) {
            com.mmo.shared.model.ShopFollower existing = existingFollowOpt.get();
            boolean newDeleteStatus = !Boolean.TRUE.equals(existing.getIsDelete());
            existing.setIsDelete(newDeleteStatus);
            shopFollowerRepository.save(existing);
            isFollowingNow = !newDeleteStatus;
        } else {
            com.mmo.shared.model.ShopFollower newFollow = com.mmo.shared.model.ShopFollower.builder()
                    .follower(followerOpt.get())
                    .seller(sellerOpt.get())
                    .isDelete(false)
                    .build();
            shopFollowerRepository.save(newFollow);
            isFollowingNow = true;
        }

        long followerCount = shopFollowerRepository.countBySellerIdAndIsDeleteFalse(sellerId);

        return ResponseEntity.ok(java.util.Map.of(
                "isFollowing", isFollowingNow,
                "followerCount", followerCount,
                "message", isFollowingNow ? "Theo dõi cửa hàng thành công!" : "Bỏ theo dõi cửa hàng thành công!"
        ));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<Category>> getCategories(@RequestParam(required = false) Boolean parentOnly) {
        if (Boolean.TRUE.equals(parentOnly)) {
            return ResponseEntity.ok(categoryRepository.findByParentIsNullAndIsDeleteFalse());
        }
        return ResponseEntity.ok(categoryRepository.findByIsDeleteFalse());
    }
}