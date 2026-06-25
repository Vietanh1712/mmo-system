package service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    private static final String FROM_EMAIL = "nguyenthingoclinh291104@gmail.com";

    @Async
    public void sendOtpEmail(String toEmail, String otp) {
        try {
            log.info("Sending registration OTP email to {}", toEmail);
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(FROM_EMAIL);
            message.setTo(toEmail);
            message.setSubject("Mã xác thực OTP - MMO Market");
            message.setText("Chào bạn,\n\nMã OTP xác thực tài khoản của bạn là: " + otp + "\nMã OTP này có hiệu lực trong vòng 5 phút.\n\nTrân trọng,\nMMO Market Team");
            mailSender.send(message);
            log.info("Successfully sent registration OTP email to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    @Async
    public void sendResetPasswordOtpEmail(String toEmail, String otp) {
        try {
            log.info("Sending password reset OTP email to {}", toEmail);
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(FROM_EMAIL);
            message.setTo(toEmail);
            message.setSubject("Mã khôi phục mật khẩu - MMO Market");
            message.setText("Chào bạn,\n\nMã OTP khôi phục mật khẩu của bạn là: " + otp + "\nMã OTP này có hiệu lực trong vòng 5 phút.\n\nTrân trọng,\nMMO Market Team");
            mailSender.send(message);
            log.info("Successfully sent password reset OTP email to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send password reset OTP email to {}: {}", toEmail, e.getMessage(), e);
        }
    }
}
