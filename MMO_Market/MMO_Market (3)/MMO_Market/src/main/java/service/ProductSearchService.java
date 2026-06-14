package service;

import dal.ProductRepository;
import dal.ProductSpecification;
import dal.ReviewRepository;
import dal.TransactionRepository;
import controller.dto.ProductSearchResultDTO;
import model.Product;
import model.ProductVariant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class ProductSearchService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    public Page<ProductSearchResultDTO> searchProducts(
            String keyword, Long categoryId, Long minPrice, Long maxPrice,
            String stockStatus, Long sellerId, List<Integer> ratings, Pageable pageable) {

        Specification<Product> spec = ProductSpecification.withDynamicQuery(
                keyword, categoryId, minPrice, maxPrice, stockStatus, sellerId, ratings);

        Page<Product> productPage = productRepository.findAll(spec, pageable);

        return productPage.map(this::convertToDTO);
    }

    private ProductSearchResultDTO convertToDTO(Product product) {
        ProductSearchResultDTO dto = new ProductSearchResultDTO();
        dto.setProductId(product.getId());
        dto.setProductName(product.getName());
        dto.setImageUrl(product.getImage());

        if (product.getCategory() != null) {
            dto.setCategoryName(product.getCategory().getName());
        }

        if (product.getSeller() != null) {
            String shopName = product.getSeller().getFullName(); 
            dto.setSellerName(shopName != null ? shopName : "Unknown Seller");
            dto.setSellerIsVerified(Boolean.TRUE.equals(product.getSeller().getIsVerified()));
        }

        // Find the cheapest active variant to display on the card
        if (product.getVariants() != null && !product.getVariants().isEmpty()) {
            Optional<ProductVariant> cheapestVariant = product.getVariants().stream()
                    .filter(v -> v.getIsDelete() != null && !v.getIsDelete())
                    .min(Comparator.comparing(ProductVariant::getPriceVnd));

            if (cheapestVariant.isPresent()) {
                ProductVariant variant = cheapestVariant.get();
                dto.setPrice(variant.getPriceVnd());
                dto.setStock(variant.getStock());
            } else {
                dto.setPrice(0L);
                dto.setStock(0);
            }
        } else {
            dto.setPrice(0L);
            dto.setStock(0);
        }

        // Query actual rating from Reviews database table
        Double avgRating = reviewRepository.findAverageRatingByProductId(product.getId());
        dto.setAverageRating(avgRating != null ? avgRating.floatValue() : 0.0f); // Default to 0.0 stars if no reviews yet

        Long reviewsCount = reviewRepository.countByProductIdAndIsDeleteFalse(product.getId());
        dto.setReviewsCount(reviewsCount != null ? reviewsCount : 0L);

        // Query actual sales count from Transactions database table
        Long salesCount = transactionRepository.countByProductIdAndIsDeleteFalse(product.getId());
        dto.setSalesCount(salesCount != null ? salesCount : 0L);

        // Classify as bestseller if sales count is high enough (e.g. >= 5 sales)
        dto.setBestseller(salesCount != null && salesCount >= 5);
        dto.setInstant(true); // Set instant delivery by default for this category of products

        return dto;
    }
}