package controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @NotBlank(message = "Họ tên không được để trống")
    @Size(min = 3, max = 255, message = "Họ tên phải từ 3 đến 255 ký tự")
    private String fullName;

    @Pattern(
            regexp = "^$|^0\\d{9}$",
            message = "Số điện thoại phải gồm 10 chữ số và bắt đầu bằng số 0"
    )
    private String phone;
}
