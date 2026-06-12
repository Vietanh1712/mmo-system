package controller.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class StaffUpsertRequest {
    private String email;
    private String password;
    private String fullName;
    private String phone;
    private String gender;
    private String address;
    private String nationalId;
    private LocalDate dateOfBirth;
    /** true = tài khoản đang hoạt động (!isLocked) */
    private Boolean active;
}
