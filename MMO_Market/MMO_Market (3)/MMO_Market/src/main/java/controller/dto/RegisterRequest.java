package controller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*[\\W_]).{6,}$",
            message = "Mật khẩu phải ít nhất 6 ký tự, gồm 1 chữ HOA và 1 ký tự đặc biệt"
    )
    private String password;

    @NotBlank(message = "Họ tên không được để trống")
    @Size(min = 3, message = "Họ tên phải có ít nhất 3 ký tự")
    private String fullName;

    // Số điện thoại là tùy chọn.
    // Nếu có gửi lên thì phải đúng 10 số và bắt đầu bằng số 0.
    // ^$ cho phép chuỗi rỗng (không nhập gì) đi qua bình thường.
    @Pattern(regexp = "^$|^0\\d{9}$", message = "Số điện thoại phải gồm 10 chữ số và bắt đầu bằng số 0")
    private String phone;

    @NotBlank(message = "Vui lòng chọn vai trò (Customer hoặc Seller)")
    private String role;
}