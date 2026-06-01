package controller.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogoutResponse {
    private String message;
    private Boolean success;
}

