package com.mmo.shared.dto;
import com.mmo.shared.model.IdType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycResponseDto {
    private Long id;
    private String idNumber;
    private String idType;
    private String requestCode;
    private String status;
    private String rejectionReason;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
