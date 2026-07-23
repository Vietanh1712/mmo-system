package com.mmo.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseResponse {
    private String transactionCode;
    private String orderCode;
    private Long finalBalance;
    private String productName;
    private Long amount;
    private CredentialsDTO credentials;
    private Long transactionId;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CredentialsDTO {
        private String username;
        private String password;
        private String note;
    }
}
