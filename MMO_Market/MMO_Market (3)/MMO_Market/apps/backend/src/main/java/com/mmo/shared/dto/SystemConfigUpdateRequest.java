package com.mmo.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemConfigUpdateRequest {
    private Integer sessionTimeout;
    private Integer otpTimeout;
    private Integer maxLoginRetries;
    private Integer lockDurationMins;
    private Integer escrowHoldHours;
    private Integer escrowHoldHoursLevel0;
    private Integer escrowHoldHoursLevel1;
    private Integer escrowHoldHoursLevel2;
}
