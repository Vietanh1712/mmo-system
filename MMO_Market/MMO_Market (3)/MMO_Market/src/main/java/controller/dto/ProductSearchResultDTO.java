package controller.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ProductSearchResultDTO {
    private Long productId;
    private String productName;
    private String imageUrl;
    private String categoryName;

    // From Seller (User)
    private String sellerName;
    private boolean sellerIsVerified;

    // From the cheapest Variant
    private Long price;
    private Integer stock;

    // Placeholders for future implementation
    private Float averageRating = 0.0f;
    private boolean isInstant = false;
    private boolean isBestseller = false;
}