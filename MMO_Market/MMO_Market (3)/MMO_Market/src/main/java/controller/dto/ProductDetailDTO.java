package controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDetailDTO {
    private Long id;
    private String name;
    private String description;
    private String imageUrl;

    private Long categoryId;
    private String categoryName;

    private Long sellerId;
    private String sellerName;
    private Boolean sellerIsVerified;

    private Integer stock;
    private Long price; // Price of cheapest variant

    private Double averageRating;
    private Long reviewsCount;
    private Long salesCount;

    private List<VariantDTO> variants;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VariantDTO {
        private Long id;
        private String name;
        private Long price;
        private Long oldPrice;
        private Integer duration;
        private String label;
    }
}
