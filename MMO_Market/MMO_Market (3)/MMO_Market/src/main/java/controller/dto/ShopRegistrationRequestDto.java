package controller.dto;

import lombok.Data;

@Data
public class ShopRegistrationRequestDto {
    private String shopName;
    private String description;
    private String category;
    private String supportEmail;
    private String supportPhone;
}
