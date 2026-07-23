package com.mmo.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopupResponseDto {
    private Long id;
    private Long userId;
    private String userFullName;
    private String userEmail;
    private Long amountVnd;
    private String sepayCode;
    private String status;
    private String transferContent;
    private Long balanceBefore;
    private Long balanceAfter;
    private String failureReason;
    private String staffNote;
    private Long processedByStaffId;
    private String processedByStaffName;
    private LocalDateTime createdAt;
}
