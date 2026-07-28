package com.mmo.shared.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreOrderDeliveryRequest {
    @NotBlank(message = "Dữ liệu trả hàng không được để trống")
    private String deliveryData;

    private String proofImage;
}
