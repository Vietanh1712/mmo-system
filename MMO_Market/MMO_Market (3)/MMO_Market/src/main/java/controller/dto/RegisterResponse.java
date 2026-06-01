package controller.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterResponse {
    private Long userId;
    private String email;
    private String fullName;
    private String message;
    private Boolean success;
}

