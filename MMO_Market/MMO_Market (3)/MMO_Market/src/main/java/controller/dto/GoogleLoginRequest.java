package controller.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class GoogleLoginRequest {
    @NotBlank(message = "Authorization code cannot be blank")
    private String code;
}
