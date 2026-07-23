package com.mmo.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopupRetryRequestDto {
    private Long targetUserId;
    private Boolean skipMinCheck;
    private String staffNote;
}
