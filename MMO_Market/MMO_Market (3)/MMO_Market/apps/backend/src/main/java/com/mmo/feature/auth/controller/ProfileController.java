package com.mmo.feature.auth.controller;
import com.mmo.shared.dto.TwoFactorRequest;
import com.mmo.shared.dto.ChangePasswordRequest;
import com.mmo.feature.auth.service.AuthenticationService;

import com.mmo.shared.dto.ProfileResponse;
import com.mmo.shared.dto.UpdateProfileRequest;
import com.mmo.shared.dto.ShopRegistrationRequestDto;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.mmo.feature.auth.service.UserService;

import org.springframework.web.bind.annotation.PostMapping;

/**
 * Controller quản lý Hồ sơ cá nhân (Profile), Bảo mật (2FA, Đổi mật khẩu)
 * và Đăng ký mở Shop dành cho người dùng chung (Customer/Seller).
 */
@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController {

    private final UserService userService;
    private final AuthenticationService authenticationService;

    public ProfileController(UserService userService, AuthenticationService authenticationService) {
        this.userService = userService;
        this.authenticationService = authenticationService;
    }

    /**
     * Lấy thông tin hồ sơ cá nhân hiện tại.
     * @param userId ID của người dùng (từ JWT Token)
     */
    @GetMapping
    public ProfileResponse viewProfile(@AuthenticationPrincipal Long userId) {
        return userService.getMyProfile(userId);
    }

    /**
     * Cập nhật thông tin hồ sơ cá nhân (Họ tên, giới tính, avatar, SĐT).
     * @param request Dữ liệu cập nhật
     */
    @PutMapping
    public ProfileResponse updateProfile(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateMyProfile(userId, request);
    }

    /**
     * Thay đổi mật khẩu người dùng.
     * @param request Yêu cầu nhập mật khẩu cũ và mật khẩu mới.
     */
    @PutMapping("/password")
    public org.springframework.http.ResponseEntity<?> changePassword(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody com.mmo.shared.dto.ChangePasswordRequest request) {
        try {
            userService.changePassword(userId, request.getCurrentPassword(), request.getNewPassword());
            return org.springframework.http.ResponseEntity.ok(new Object() {
                public final boolean success = true;
                public final String message = "Đổi mật khẩu thành công";
            });
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return org.springframework.http.ResponseEntity.status(e.getStatusCode())
                    .body(new Object() {
                        public final boolean success = false;
                        public final String message = e.getReason();
                    });
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new Object() {
                        public final boolean success = false;
                        public final String message = "Lỗi hệ thống: " + e.getMessage();
                    });
        }
    }

    /**
     * Gửi mã OTP về email để thực hiện thiết lập Bảo mật 2 lớp (2FA).
     */
    @org.springframework.web.bind.annotation.PostMapping("/2fa/send-otp")
    public org.springframework.http.ResponseEntity<?> send2faOtp(@AuthenticationPrincipal Long userId) {
        try {
            authenticationService.send2faOtp(userId);
            return org.springframework.http.ResponseEntity.ok(new Object() {
                public final boolean success = true;
                public final String message = "Mã OTP đã được gửi về email của bạn";
            });
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.badRequest()
                    .body(new Object() {
                        public final boolean success = false;
                        public final String message = e.getMessage();
                    });
        }
    }

    /**
     * Kích hoạt bảo mật 2 lớp (2FA) bằng cách xác thực mã OTP vừa gửi.
     * @param request Chứa mã OTP do người dùng nhập.
     */
    @org.springframework.web.bind.annotation.PostMapping("/2fa/enable")
    public org.springframework.http.ResponseEntity<?> enable2fa(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody com.mmo.shared.dto.TwoFactorRequest request) {
        try {
            authenticationService.enable2fa(userId, request.getOtp());
            return org.springframework.http.ResponseEntity.ok(new Object() {
                public final boolean success = true;
                public final String message = "Đã bật bảo mật 2 lớp thành công";
            });
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.badRequest()
                    .body(new Object() {
                        public final boolean success = false;
                        public final String message = e.getMessage();
                    });
        }
    }

    /**
     * Hủy bỏ (Tắt) bảo mật 2 lớp (2FA).
     */
    @org.springframework.web.bind.annotation.PostMapping("/2fa/disable")
    public org.springframework.http.ResponseEntity<?> disable2fa(@AuthenticationPrincipal Long userId) {
        try {
            authenticationService.disable2fa(userId);
            return org.springframework.http.ResponseEntity.ok(new Object() {
                public final boolean success = true;
                public final String message = "Đã tắt bảo mật 2 lớp";
            });
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.badRequest()
                    .body(new Object() {
                        public final boolean success = false;
                        public final String message = e.getMessage();
                    });
        }
    }

    /**
     * Gửi yêu cầu Đăng ký trở thành Người bán (Mở Shop).
     * @param request Dữ liệu tên shop và mô tả.
     */
    @PostMapping("/register-shop")
    public org.springframework.http.ResponseEntity<?> registerShop(
            @AuthenticationPrincipal Long userId,
            @RequestBody ShopRegistrationRequestDto request) {
        try {
            ProfileResponse response = userService.registerShop(userId, request);
            return org.springframework.http.ResponseEntity.ok(response);
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return org.springframework.http.ResponseEntity.status(e.getStatusCode())
                    .body(new Object() {
                        public final boolean success = false;
                        public final String message = e.getReason();
                    });
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new Object() {
                        public final boolean success = false;
                        public final String message = "Lỗi hệ thống: " + e.getMessage();
                    });
        }
    }

    @PostMapping("/avatar")
    public org.springframework.http.ResponseEntity<?> uploadAvatar(
            @AuthenticationPrincipal Long userId,
            @org.springframework.web.bind.annotation.RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        try {
            String avatarUrl = userService.uploadAvatar(userId, file);
            return org.springframework.http.ResponseEntity.ok(new Object() {
                public final boolean success = true;
                public final String avatar = avatarUrl;
                public final String message = "Cập nhật ảnh đại diện thành công";
            });
        } catch (IllegalArgumentException e) {
            return org.springframework.http.ResponseEntity.badRequest().body(new Object() {
                public final boolean success = false;
                public final String message = e.getMessage();
            });
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new Object() {
                        public final boolean success = false;
                        public final String message = "Lỗi hệ thống: " + e.getMessage();
                    });
        }
    }
}
