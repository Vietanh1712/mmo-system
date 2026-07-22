package com.mmo.feature.product.service;
import com.mmo.shared.model.Category;

import com.mmo.shared.dal.ProductRepository;
import com.mmo.shared.dal.ProductSpecification;
import com.mmo.shared.dal.ReviewRepository;
import com.mmo.shared.dal.TransactionRepository;
import com.mmo.shared.dto.ProductSearchResultDTO;
import com.mmo.shared.model.Product;
import com.mmo.shared.model.ProductVariant;
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

    @Autowired
    private com.mmo.shared.dal.CategoryRepository categoryRepository;

    public Page<ProductSearchResultDTO> searchProducts(
            String keyword, Long categoryId, Long minPrice, Long maxPrice,
            String stockStatus, Long sellerId, List<Integer> ratings, Pageable pageable) {
        return searchProducts(keyword, categoryId, null, minPrice, maxPrice, stockStatus, sellerId, ratings, pageable);
    }

    public Page<ProductSearchResultDTO> searchProducts(
            String keyword, Long categoryId, String subCategory, Long minPrice, Long maxPrice,
            String stockStatus, Long sellerId, List<Integer> ratings, Pageable pageable) {

        Long effectiveCategoryId = categoryId;

        if (subCategory != null && !subCategory.trim().isEmpty()) {
            String subTrim = subCategory.trim();
            try {
                effectiveCategoryId = Long.parseLong(subTrim);
            } catch (NumberFormatException e) {
                Optional<Category> found = categoryRepository.findByName(subTrim);
                if (found.isPresent()) {
                    effectiveCategoryId = found.get().getId();
                } else {
                    List<Category> allCats = categoryRepository.findAllByIsDeleteFalseOrderByIdAsc();
                    for (Category cat : allCats) {
                        if (cat.getName() != null && cat.getName().equalsIgnoreCase(subTrim)) {
                            effectiveCategoryId = cat.getId();
                            break;
                        }
                    }
                }
            }
        }

        Specification<Product> spec = ProductSpecification.withDynamicQuery(
                keyword, effectiveCategoryId, minPrice, maxPrice, stockStatus, sellerId, ratings);

        Page<Product> productPage = productRepository.findAll(spec, pageable);
        
        if (productPage.isEmpty()) {
            return productPage.map(p -> convertToDTO(p, 0.0, 0L, 0L));
        }
        
        List<Long> productIds = productPage.getContent().stream().map(Product::getId).toList();
        
        java.util.Map<Long, Double> avgRatings = reviewRepository.findAverageRatingByProductIds(productIds).stream()
                .collect(java.util.stream.Collectors.toMap(obj -> (Long) obj[0], obj -> (Double) obj[1]));
                
        java.util.Map<Long, Long> reviewsCounts = reviewRepository.countByProductIdsAndIsDeleteFalse(productIds).stream()
                .collect(java.util.stream.Collectors.toMap(obj -> (Long) obj[0], obj -> (Long) obj[1]));
                
        java.util.Map<Long, Long> salesCounts = transactionRepository.countByProductIdsAndIsDeleteFalse(productIds).stream()
                .collect(java.util.stream.Collectors.toMap(obj -> (Long) obj[0], obj -> (Long) obj[1]));

        return productPage.map(product -> convertToDTO(product, 
            avgRatings.getOrDefault(product.getId(), 0.0), 
            reviewsCounts.getOrDefault(product.getId(), 0L), 
            salesCounts.getOrDefault(product.getId(), 0L)));
    }

    private ProductSearchResultDTO convertToDTO(Product product, Double avgRating, Long reviewsCount, Long salesCount) {
        ProductSearchResultDTO dto = new ProductSearchResultDTO();
        dto.setProductId(product.getId());
        dto.setProductName(product.getName());
        dto.setImageUrl(product.getImage());

        if (product.getCategory() != null) {
            if (product.getCategory().getParent() != null && product.getCategory().getParent().getName() != null) {
                dto.setCategoryName(product.getCategory().getParent().getName());
            } else if (product.getCategory().getName() != null) {
                dto.setCategoryName(product.getCategory().getName());
            }
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

        dto.setAverageRating(avgRating != null ? avgRating.floatValue() : 0.0f);
        dto.setReviewsCount(reviewsCount != null ? reviewsCount : 0L);
        dto.setSalesCount(salesCount != null ? salesCount : 0L);
        dto.setBestseller(salesCount != null && salesCount >= 5);
        dto.setInstant(true); // Set instant delivery by default for this category of products

        return dto;
    }
}