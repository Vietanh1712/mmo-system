package controller.dto;

import lombok.Data;

@Data
public class StaffUpsertRequest {
    private String email;
    private String password;
    private String fullName;
    private String phone;
}
