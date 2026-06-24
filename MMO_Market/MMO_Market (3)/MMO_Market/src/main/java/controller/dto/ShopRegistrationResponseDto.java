package controller.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShopRegistrationResponseDto {
    private Long id;
    private String status;
    private String code;
    private String submittedAt;
    private String shopName;
    private String category;
    private String description;
    private String supportEmail;
    private String supportPhone;
    private String rejectionReason;
}
