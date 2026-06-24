package controller.dto;

import lombok.Data;

@Data
public class ShopRegistrationReviewDto {
    private boolean approved;
    private String reason;
}
