package controller.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProfileResponse {
    private Long id;
    private String email;
    private String fullName;
    private String gender;
    private String nationalId;
    private String phone;
    private String role;
    private String shopStatus;
    private Long balanceVnd;
    private String dateOfBirth;
    private String address;
    private Boolean is2faEnabled;
    private String kycStatus;
}
