package controller.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderDto {
    private String orderCode;
    private Long productId;
    private String productName;
    private String variantLabel;
    private String sellerName;
    private Long amount;
    private String status;
    private String paymentStatus;
    private String createdAt;
    private String escrowReleaseDate;
    private boolean isReviewed;
    private Integer reviewRating;
    private String reviewComment;
    private Long transactionId;
    private java.util.Map<String, String> credentials;
}
