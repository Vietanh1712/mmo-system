package com.mmo.shared.dal;

import com.mmo.shared.model.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    // 1. Hàm này dùng cho luồng Đăng ký tài khoản (Bắt buộc phải chưa sử dụng)
    Optional<EmailVerification> findByUserIdAndVerificationCodeAndIsUsedFalse(Long userId, String verificationCode);

    // 2. Hàm này dùng cho luồng Quên mật khẩu (Không check điều kiện IsUsedFalse)
    Optional<EmailVerification> findByUserIdAndVerificationCode(Long userId, String verificationCode);

    // 3. Lấy mã OTP mới nhất của User để phục vụ kiểm tra giới hạn tần suất (Rate Limiting)
    Optional<EmailVerification> findFirstByUserIdOrderByExpiryDateDesc(Long userId);
}