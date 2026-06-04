package controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import model.Product;

/**
 * ProductResponseDTO - Cấu trúc dữ liệu sản phẩm trả về từ API
 *
 * Được sử dụng để:
 * - Trả về danh sách sản phẩm từ API /api/v1/search
 * - Render sản phẩm trên trang HTML search-results
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponseDTO {

    // Thông tin cơ bản
    private Long id;
    private String name;
    private String description;
    private String imageUrl;

    // Thông tin danh mục
    private Long categoryId;
    private String categoryName;

    // Thông tin người bán
    private Long sellerId;
    private String sellerName;
    private Boolean sellerVerified;

    // Giá tiền (lấy từ ProductVariant)
    private Long minPrice;  // Giá thấp nhất
    private Long maxPrice;  // Giá cao nhất

    // Đánh giá & tình trạng
    private Double rating = 5.0;
    private Integer reviewCount = 0;
    private String badge; // "Instant", "Bestseller", etc.
    private Boolean inStock = true;
    private Integer totalStock = 0;

    public static ProductResponseDTO fromEntity(Product product) {
        String sellerName = "Unknown";
        Boolean sellerVerified = false;
        Long sellerId = 0L;

        if (product.getSeller() != null) {
            sellerName = product.getSeller().getFullName() != null ?
                product.getSeller().getFullName() : "Unknown";
            sellerVerified = product.getSeller().getIsVerified() != null ?
                product.getSeller().getIsVerified() : false;
            sellerId = product.getSeller().getId();
        }

        String categoryName = "Other";
        Long categoryId = 0L;

        if (product.getCategory() != null) {
            categoryName = product.getCategory().getName() != null ?
                product.getCategory().getName() : "Other";
            categoryId = product.getCategory().getId();
        }

        return ProductResponseDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                // Fixed: Use getImage() instead of getImageUrl() to match the SQL schema
                .imageUrl(product.getImage() != null ? product.getImage() : "https://via.placeholder.com/300x160/fd761a/ffffff?text=Product")
                .categoryId(categoryId)
                .categoryName(categoryName)
                .sellerId(sellerId)
                .sellerName(sellerName)
                .sellerVerified(sellerVerified)
                .rating(5.0)
                .reviewCount(0)
                .badge("Giao Tuc Thi")
                .inStock(true)
                .totalStock(10)
                .minPrice(45000L)
                .maxPrice(450000L)
                .build();
    }
}