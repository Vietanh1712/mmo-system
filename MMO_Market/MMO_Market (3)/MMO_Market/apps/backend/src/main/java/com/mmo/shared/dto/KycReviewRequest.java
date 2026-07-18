package com.mmo.shared.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import com.mmo.shared.model.KycStatus;

@Data
public class KycReviewRequest {
    @NotNull(message = "Version is required cho Optimistic Locking")
    private Integer version;

    @NotNull(message = "Trạng thái không được để trống")
    private KycStatus status; // APPROVED or REJECTED

    private String rejectionReason;
}
