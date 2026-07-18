package com.mmo.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletStatsDto {
    private Long totalTopup;
    private Long totalSpent;
    private Long pendingCount;
    private Long escrowAmount;
}
