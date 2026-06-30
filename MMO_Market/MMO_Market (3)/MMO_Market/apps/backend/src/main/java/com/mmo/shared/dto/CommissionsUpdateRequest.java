package com.mmo.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommissionsUpdateRequest {
    private Double basePercent;
    private Double withdrawalPercent;
    private Long sellerUpgradeFee;
    private Long productFeaturedFee;
    private Long minWithdrawLimit;
    private Long maxWithdrawLimit;
    private Long minDepositLimit;
    private Long maxDepositLimit;
}
