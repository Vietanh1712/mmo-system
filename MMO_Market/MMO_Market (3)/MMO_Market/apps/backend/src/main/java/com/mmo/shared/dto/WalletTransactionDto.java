package com.mmo.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletTransactionDto {
    private String code;
    private String type;
    private Long amount;
    private Long balanceAfter;
    private String status;
    private String description;
    private String createdAt;
}
