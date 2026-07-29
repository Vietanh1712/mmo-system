package com.mmo.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreOrderResponse {
    private boolean success;
    private String message;
    private Long id;
    private Long productId;
    private String productName;
    private Long variantId;
    private String variantName;
    private String customerEmail;
    private Integer quantity;
    private Long expectedPriceVnd;
    private String status;
    private String notes;
    private String deliveryData;
    private String proofImage;
    private String createdAt;
}
