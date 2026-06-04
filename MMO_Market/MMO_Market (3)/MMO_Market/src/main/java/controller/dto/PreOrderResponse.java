package controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private Integer quantity;
    private Long expectedPriceVnd;
    private String status;
    private String notes;
    private String createdAt;
}
