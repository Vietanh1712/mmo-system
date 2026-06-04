package controller.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminUserResponse {
    private Long id;
    private String email;
    private String fullName;
    private String gender;
    private String role;
    private String phone;
    private String shopStatus;
    private Long balanceVnd;
    private Boolean isVerified;
    private Boolean isLocked;
    private Boolean isOnline;
    private LocalDateTime createdAt;
}
