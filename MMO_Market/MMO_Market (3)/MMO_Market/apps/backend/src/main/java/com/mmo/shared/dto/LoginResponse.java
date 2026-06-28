package com.mmo.shared.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private Long userId;
    private String email;
    private String fullName;
    private String role;
    private Long balanceVnd; // Thêm trường balanceVnd
    private Boolean requires2FA;
    private String message;
}