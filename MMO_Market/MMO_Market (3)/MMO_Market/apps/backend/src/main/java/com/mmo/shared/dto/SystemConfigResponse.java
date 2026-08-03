package com.mmo.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemConfigResponse {
    private SystemConfigDto systemConfig;
    private CommissionsDto commissions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SystemConfigDto {
        private String appName;
        private Integer sessionTimeout;
        private Integer otpTimeout;
        private Integer maxLoginRetries;
        private Integer lockDurationMins;
        private Integer escrowHoldHours;
        private Integer escrowHoldHoursLevel0;
        private Integer escrowHoldHoursLevel1;
        private Integer escrowHoldHoursLevel2;
        private Boolean allowGoogleLogin;
        private Boolean allowRegister;
        private Boolean requireWithdraw2FA;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CommissionsDto {
        private Double basePercent;
        private Double withdrawalPercent;
        private Long shopOpeningFee;
        private Long minWithdrawLimit;
        private Long maxWithdrawLimit;
        private Long minDepositLimit;
        private Long maxDepositLimit;
    }
}
