package controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import model.Product;
import model.ProductVariant;

import java.util.List;

/**
 * FeaturedProductDTO - Dữ liệu sản phẩm nổi bật trả về cho Homepage
 * Bao gồm thông tin tổng hợp: giá, tồn kho, lượt bán thực tế từ DB.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeaturedProductDTO {

    private Long id;
    private String name;
    private String imageUrl;

    // Thông tin người bán
    private Long sellerId;
    private String sellerName;

    // Thông tin danh mục
    private Long categoryId;
    private String categoryName;

    // Giá
    private Long minPrice;
    private Long maxPrice;

    // Tồn kho & lượt bán
    private Integer totalStock;
    private Long salesCount; // Số giao dịch thực tế từ bảng Transactions

    public static FeaturedProductDTO fromEntity(Product product, Long salesCount) {
        String sellerName = "Unknown";
        Long sellerId = 0L;
        if (product.getSeller() != null) {
            sellerName = product.getSeller().getFullName() != null
                    ? product.getSeller().getFullName() : "Unknown";
            sellerId = product.getSeller().getId();
        }

        String categoryName = "Khác";
        Long categoryId = 0L;
        if (product.getCategory() != null) {
            categoryName = product.getCategory().getName() != null
                    ? product.getCategory().getName() : "Khác";
            categoryId = product.getCategory().getId();
        }

        // Tính giá min/max và tổng tồn kho từ các biến thể sản phẩm
        long minPrice = 0L;
        long maxPrice = 0L;
        int totalStock = 0;
        List<ProductVariant> variants = product.getVariants();
        if (variants != null && !variants.isEmpty()) {
            minPrice = variants.stream()
                    .filter(v -> Boolean.FALSE.equals(v.getIsDelete()))
                    .mapToLong(ProductVariant::getPriceVnd)
                    .min().orElse(0L);
            maxPrice = variants.stream()
                    .filter(v -> Boolean.FALSE.equals(v.getIsDelete()))
                    .mapToLong(ProductVariant::getPriceVnd)
                    .max().orElse(0L);
            totalStock = variants.stream()
                    .filter(v -> Boolean.FALSE.equals(v.getIsDelete()))
                    .mapToInt(v -> v.getStock() != null ? v.getStock() : 0)
                    .sum();
        }

        return FeaturedProductDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .imageUrl(product.getImage() != null
                        ? product.getImage()
                        : "https://via.placeholder.com/300x160/fd761a/ffffff?text=Product")
                .sellerId(sellerId)
                .sellerName(sellerName)
                .categoryId(categoryId)
                .categoryName(categoryName)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .totalStock(totalStock)
                .salesCount(salesCount != null ? salesCount : 0L)
                .build();
    }
}
