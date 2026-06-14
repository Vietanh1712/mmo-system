package controller;

import controller.dto.FeaturedProductDTO;
import controller.dto.ProductSearchResultDTO;
import dal.CategoryRepository;
import model.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import service.ProductSearchService;
import service.ProductService;

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
    private dal.UserRepository userRepository;

    @Autowired
    private dal.ProductRepository productRepository;

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
    private dal.ReviewRepository reviewRepository;

    @Autowired
    private dal.TransactionRepository transactionRepository;

    @GetMapping("/products")
    public ResponseEntity<Page<ProductSearchResultDTO>> searchProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long minPrice,
            @RequestParam(required = false) Long maxPrice,
            @RequestParam(required = false) String stockStatus, // e.g., "In Stock"
            @RequestParam(required = false) Long sellerId,
            @RequestParam(required = false) List<Integer> rating,
            @PageableDefault(size = 12, sort = "createdAt") Pageable pageable) {

        Page<ProductSearchResultDTO> results = productSearchService.searchProducts(
                keyword, categoryId, minPrice, maxPrice, stockStatus, sellerId, rating, pageable);

        return ResponseEntity.ok(results);
    }

    /**
     * API lấy thông tin chi tiết một sản phẩm kèm thống kê thực tế từ database
     */
    @GetMapping("/products/{productId}")
    public ResponseEntity<controller.dto.ProductDetailDTO> getProductDetail(@PathVariable Long productId) {
        return productRepository.findByIdAndIsDeleteFalse(productId)
                .map(product -> {
                    // Query statistics from DB
                    Double avgRating = reviewRepository.findAverageRatingByProductId(product.getId());
                    Long reviewsCount = reviewRepository.countByProductIdAndIsDeleteFalse(product.getId());
                    Long salesCount = transactionRepository.countByProductIdAndIsDeleteFalse(product.getId());

                    // Map variants to DTOs
                    List<controller.dto.ProductDetailDTO.VariantDTO> variantDTOs = new java.util.ArrayList<>();
                    if (product.getVariants() != null) {
                        product.getVariants().stream()
                                .filter(v -> v.getIsDelete() != null && !v.getIsDelete())
                                .forEach(v -> {
                                    variantDTOs.add(controller.dto.ProductDetailDTO.VariantDTO.builder()
                                            .id(v.getId())
                                            .name(v.getVariantName())
                                            .price(v.getPriceVnd())
                                            .oldPrice((long) (v.getPriceVnd() * 1.5)) // High-fidelity mock old price
                                            .duration(v.getVariantName().contains("Năm") || v.getVariantName().contains("12 Tháng") ? 12 : (v.getVariantName().contains("6 Tháng") ? 6 : (v.getVariantName().contains("3 Tháng") ? 3 : 1)))
                                            .label(v.getVariantName())
                                            .build());
                                });
                    }

                    // Find cheapest price & stock
                    long cheapestPrice = 0L;
                    int totalStock = 0;
                    if (!variantDTOs.isEmpty()) {
                        cheapestPrice = variantDTOs.stream()
                                .mapToLong(controller.dto.ProductDetailDTO.VariantDTO::getPrice)
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

                    controller.dto.ProductDetailDTO detail = controller.dto.ProductDetailDTO.builder()
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
                            .build();

                    return ResponseEntity.ok(detail);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * API lấy danh sách đánh giá của sản phẩm bằng tiếng Việt từ DB
     */
    @GetMapping("/products/{productId}/reviews")
    public ResponseEntity<List<controller.dto.ReviewResponseDTO>> getProductReviews(@PathVariable Long productId) {
        List<model.Review> reviews = reviewRepository.findByProductIdAndIsDeleteFalse(productId);
        List<controller.dto.ReviewResponseDTO> dtos = reviews.stream()
                .map(r -> controller.dto.ReviewResponseDTO.builder()
                        .id(r.getId())
                        .userName(r.getUser() != null && r.getUser().getFullName() != null ? r.getUser().getFullName() : "Người dùng MMO")
                        .rating(r.getRating())
                        .comment(r.getComment())
                        .createdAt(r.getCreatedAt())
                        .build())
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<?> getSellerProfile(@PathVariable Long sellerId) {
        return userRepository.findByIdAndIsDeleteFalse(sellerId)
                .map(user -> {
                    long productCount = productRepository.countBySellerIdAndIsDeleteFalse(sellerId);
                    
                    // Format joined date: e.g., "tháng 06/2026"
                    String joinedDate = "tháng " + (user.getCreatedAt() != null 
                        ? user.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("MM/yyyy"))
                        : "06/2026");
                    
                    java.util.Map<String, Object> profile = new java.util.HashMap<>();
                    profile.put("id", user.getId());
                    profile.put("shopName", user.getFullName() != null ? user.getFullName() : "Gian hàng đối tác");
                    profile.put("joinedDate", joinedDate);
                    profile.put("isVerified", Boolean.TRUE.equals(user.getIsVerified()));
                    profile.put("totalProducts", productCount);
                    Double avgSellerRating = reviewRepository.findAverageRatingBySellerId(sellerId);
                    profile.put("rating", avgSellerRating != null ? avgSellerRating : 0.0);
                    profile.put("responseRate", "98%"); // High-fidelity mock response rate
                    profile.put("responseTime", "Trong vài giờ"); // High-fidelity mock response time
                    profile.put("email", user.getEmail());
                    return ResponseEntity.ok(profile);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/categories")
    public ResponseEntity<List<Category>> getCategories() {
        return ResponseEntity.ok(categoryRepository.findByIsDeleteFalse());
    }
}