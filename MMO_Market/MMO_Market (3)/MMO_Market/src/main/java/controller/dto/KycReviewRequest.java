package controller.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import model.KycStatus;

@Data
public class KycReviewRequest {
    @NotNull(message = "Version is required cho Optimistic Locking")
    private Integer version;

    @NotNull(message = "Trạng thái không được để trống")
    private KycStatus status; // APPROVED or REJECTED

    private String rejectionReason;
}
