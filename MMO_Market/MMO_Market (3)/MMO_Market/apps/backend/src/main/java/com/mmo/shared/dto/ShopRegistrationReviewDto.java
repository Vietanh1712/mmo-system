package com.mmo.shared.dto;

import lombok.Data;

@Data
public class ShopRegistrationReviewDto {
    private boolean approved;
    private String reason;
}
