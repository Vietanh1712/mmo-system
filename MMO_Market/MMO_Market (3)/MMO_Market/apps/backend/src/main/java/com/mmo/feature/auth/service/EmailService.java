package com.mmo.feature.auth.service;
import com.mmo.shared.model.Withdrawal;

import com.mmo.shared.dal.SystemConfigurationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private SystemConfigurationRepository systemConfigurationRepository;

    @Value("${spring.mail.username}")
    private String fromEmail;

    private int getOtpTimeoutMins() {
        if (systemConfigurationRepository == null) {
            return 5;
        }
        return systemConfigurationRepository.findByConfigKey("OTP_TIMEOUT_MINS")
                .map(c -> {
                    try { return Integer.parseInt(c.getConfigValue()); }
                    catch (NumberFormatException e) { return 5; }
                }).orElse(5);
    }

    @Async
    public void sendOtpEmail(String toEmail, String otp) {
        try {
            log.info("Sending registration OTP email to {}", toEmail);
            int timeoutMins = getOtpTimeoutMins();
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Mã xác thực OTP - MMO Market");
            message.setText("Chào bạn,\n\nMã OTP xác thực tài khoản của bạn là: " + otp + "\nMã OTP này có hiệu lực trong vòng " + timeoutMins + " phút.\n\nTrân trọng,\nMMO Market Team");
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
            int timeoutMins = getOtpTimeoutMins();
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Mã khôi phục mật khẩu - MMO Market");
            message.setText("Chào bạn,\n\nMã OTP khôi phục mật khẩu của bạn là: " + otp + "\nMã OTP này có hiệu lực trong vòng " + timeoutMins + " phút.\n\nTrân trọng,\nMMO Market Team");
            mailSender.send(message);
            log.info("Successfully sent password reset OTP email to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send password reset OTP email to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    @Async
    public void sendWithdrawalOtpEmail(String toEmail, String otp, int timeoutMins) {
        try {
            log.info("Sending withdrawal OTP email to {}", toEmail);
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Mã xác thực rút tiền - MMO Market");
            message.setText("Chào bạn,\n\nMã OTP xác thực yêu cầu rút tiền của bạn là: " + otp + "\nMã OTP này có hiệu lực trong vòng " + timeoutMins + " phút.\n\nTrân trọng,\nMMO Market Team");
            mailSender.send(message);
            log.info("Successfully sent withdrawal OTP email to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send withdrawal OTP email to {}: {}", toEmail, e.getMessage(), e);
        }
    }
}

