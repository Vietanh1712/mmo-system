package com.mmo.feature.auth.service;

import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.UserCredentials;
import com.mmo.shared.dal.SystemConfigurationRepository;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.mail.javamail.JavaMailSender;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Properties;

@Service
@Slf4j
public class EmailService {

    private static final String APPLICATION_NAME = "MMO Market";

    private final SystemConfigurationRepository systemConfigurationRepository;
    private final String googleClientId;
    private final String googleClientSecret;
    private final String googleRefreshToken;
    private final String fromEmail;
    private final String fromName;

    private volatile Gmail gmailClient;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private JavaMailSender mailSender;

    private boolean isGmailApiConfigured() {
        return StringUtils.hasText(googleClientId)
                && StringUtils.hasText(googleClientSecret)
                && StringUtils.hasText(googleRefreshToken)
                && StringUtils.hasText(fromEmail);
    }

    public EmailService(
            SystemConfigurationRepository systemConfigurationRepository,
            @Value("${google.oauth2.client-id:}${google.oauth2.client-id-suffix:}") String googleClientId,
            @Value("${google.oauth2.client-secret:}") String googleClientSecret,
            @Value("${google.oauth2.refresh-token:}") String googleRefreshToken,
            @Value("${gmail.api.from-email:}") String fromEmail,
            @Value("${gmail.api.from-name:MMO Market}") String fromName) {
        this.systemConfigurationRepository = systemConfigurationRepository;
        
        if (googleClientId != null && org.springframework.util.StringUtils.hasText(googleClientId)) {
            googleClientId = googleClientId.trim();
            if (googleClientId.endsWith(".apps.googleusercontent.com.apps.googleusercontent.com")) {
                googleClientId = googleClientId.substring(0, googleClientId.length() - ".apps.googleusercontent.com".length());
            } else if (!googleClientId.endsWith(".apps.googleusercontent.com")) {
                googleClientId += ".apps.googleusercontent.com";
            }
        }
        
        this.googleClientId = googleClientId;
        this.googleClientSecret = googleClientSecret;
        this.googleRefreshToken = googleRefreshToken;
        this.fromEmail = fromEmail;
        this.fromName = fromName;
    }

    private int getOtpTimeoutMins() {
        return systemConfigurationRepository.findByConfigKey("OTP_TIMEOUT_MINS")
                .map(configuration -> {
                    try {
                        return Integer.parseInt(configuration.getConfigValue());
                    } catch (NumberFormatException exception) {
                        return 5;
                    }
                })
                .orElse(5);
    }

    public void sendOtpEmail(String toEmail, String otp) {
        int timeoutMins = getOtpTimeoutMins();
        sendEmail(
                toEmail,
                "Mã xác thực OTP - MMO Market",
                "Chào bạn,\n\n"
                        + "Mã OTP xác thực tài khoản của bạn là: " + otp + "\n"
                        + "Mã OTP này có hiệu lực trong vòng " + timeoutMins + " phút.\n\n"
                        + "Trân trọng,\nMMO Market Team");
    }

    public void sendResetPasswordOtpEmail(String toEmail, String otp) {
        int timeoutMins = getOtpTimeoutMins();
        sendEmail(
                toEmail,
                "Mã khôi phục mật khẩu - MMO Market",
                "Chào bạn,\n\n"
                        + "Mã OTP khôi phục mật khẩu của bạn là: " + otp + "\n"
                        + "Mã OTP này có hiệu lực trong vòng " + timeoutMins + " phút.\n\n"
                        + "Trân trọng,\nMMO Market Team");
    }

    public void sendWithdrawalOtpEmail(String toEmail, String otp, int timeoutMins) {
        sendEmail(
                toEmail,
                "Mã xác thực rút tiền - MMO Market",
                "Chào bạn,\n\n"
                        + "Mã OTP xác thực yêu cầu rút tiền của bạn là: " + otp + "\n"
                        + "Mã OTP này có hiệu lực trong vòng " + timeoutMins + " phút.\n\n"
                        + "Trân trọng,\nMMO Market Team");
    }

    private void sendEmail(String toEmail, String subject, String content) {
        validateEmailAddress(toEmail, "Địa chỉ email người nhận không hợp lệ");

        boolean sentSuccessfully = false;

        if (isGmailApiConfigured()) {
            try {
                log.info("Sending email via Gmail API to {}", toEmail);
                validateConfiguration();
                MimeMessage mimeMessage = createMimeMessage(toEmail, subject, content);
                Message gmailMessage = new Message().setRaw(encodeMessage(mimeMessage));
                Message sentMessage = getGmailClient()
                        .users()
                        .messages()
                        .send("me", gmailMessage)
                        .execute();

                log.info("Gmail API sent email successfully to {} with message ID {}", toEmail, sentMessage.getId());
                sentSuccessfully = true;
            } catch (Exception exception) {
                log.error("Gmail API failed to send email to {} (invalid credentials or token?): {}. Attempting fallback to SMTP...", 
                        toEmail, exception.getMessage(), exception);
            }
        }

        if (!sentSuccessfully) {
            if (mailSender != null) {
                try {
                    log.info("Falling back to SMTP JavaMailSender to send email to {}", toEmail);
                    MimeMessage mimeMessage = createMimeMessage(toEmail, subject, content);
                    mailSender.send(mimeMessage);
                    log.info("SMTP sent email successfully to {}", toEmail);
                    sentSuccessfully = true;
                } catch (Exception exception) {
                    log.error("SMTP failed to send email to {}: {}", toEmail, exception.getMessage(), exception);
                }
            }
        }

        if (!sentSuccessfully) {
            log.warn("=========================================================================");
            log.warn("🚨 KHÔNG THỂ GỬI EMAIL THỰC TẾ (Lỗi kết nối mạng hoặc sai cấu hình SMTP/Gmail API).");
            log.warn("🔑 ĐÃ GHI NHẬN OTP VÀO HỆ THỐNG. CHI TIẾT EMAIL ĐỂ KIỂM THỬ (MOCK):");
            log.warn("   - Gửi tới: {}", toEmail);
            log.warn("   - Tiêu đề: {}", subject);
            log.warn("   - Nội dung: \n{}", content);
            log.warn("=========================================================================");
        }
    }

    private MimeMessage createMimeMessage(String toEmail, String subject, String content) throws Exception {
        Session session = Session.getInstance(new Properties());
        MimeMessage email = new MimeMessage(session);
        email.setFrom(new InternetAddress(fromEmail, fromName, StandardCharsets.UTF_8.name()));
        email.setRecipient(jakarta.mail.Message.RecipientType.TO, new InternetAddress(toEmail));
        email.setSubject(subject, StandardCharsets.UTF_8.name());
        email.setText(content, StandardCharsets.UTF_8.name());
        return email;
    }

    private String encodeMessage(MimeMessage mimeMessage) throws Exception {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            mimeMessage.writeTo(outputStream);
            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(outputStream.toByteArray());
        }
    }

    private Gmail getGmailClient() {
        Gmail currentClient = gmailClient;
        if (currentClient != null) {
            return currentClient;
        }

        synchronized (this) {
            if (gmailClient == null) {
                UserCredentials credentials = UserCredentials.newBuilder()
                        .setClientId(googleClientId)
                        .setClientSecret(googleClientSecret)
                        .setRefreshToken(googleRefreshToken)
                        .build();

                gmailClient = new Gmail.Builder(
                        new NetHttpTransport(),
                        GsonFactory.getDefaultInstance(),
                        new HttpCredentialsAdapter(credentials))
                        .setApplicationName(APPLICATION_NAME)
                        .build();
            }
            return gmailClient;
        }
    }

    private void validateConfiguration() {
        if (!StringUtils.hasText(googleClientId)
                || !StringUtils.hasText(googleClientSecret)
                || !StringUtils.hasText(googleRefreshToken)
                || !StringUtils.hasText(fromEmail)) {
            throw new IllegalStateException(
                    "Thiếu cấu hình Gmail API. Kiểm tra GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET, "
                            + "GOOGLE_REFRESH_TOKEN và GMAIL_FROM_EMAIL.");
        }
        validateEmailAddress(fromEmail, "GMAIL_FROM_EMAIL không hợp lệ");
    }

    private void validateEmailAddress(String email, String errorMessage) {
        try {
            InternetAddress internetAddress = new InternetAddress(email);
            internetAddress.validate();
        } catch (Exception exception) {
            throw new IllegalStateException(errorMessage, exception);
        }
    }
}
