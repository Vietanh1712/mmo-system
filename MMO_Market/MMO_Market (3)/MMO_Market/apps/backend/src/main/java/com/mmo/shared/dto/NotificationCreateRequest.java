package com.mmo.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationCreateRequest {
    private String title;
    private String content;
    private String type;
    private Boolean activateMaintenance;
}
