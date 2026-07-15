package com.mmo.shared.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("isReviewed")
    private Boolean isReviewed;
    private Integer reviewRating;
    private String reviewComment;
    private Long transactionId;
    private Integer quantity;
    private Long complaintId;
    private java.util.Map<String, String> credentials;
    private java.util.List<java.util.Map<String, String>> credentialsList;
}
