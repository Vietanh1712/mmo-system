package com.mmo.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogDto {
    private Long id;
    private String timestamp;
    private String operator;
    private String action;
    private String ipAddress;
    private String desc;
    private String diff;
    private Long targetUserId;
    private Long targetId;
    private String targetType;
}
