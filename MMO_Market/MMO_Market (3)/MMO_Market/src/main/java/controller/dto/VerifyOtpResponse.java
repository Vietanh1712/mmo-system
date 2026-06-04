package controller.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VerifyOtpResponse {
    private boolean success;
    private String message;
}