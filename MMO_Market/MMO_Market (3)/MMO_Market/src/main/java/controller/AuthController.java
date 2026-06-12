package controller;

import controller.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import service.AuthenticationService;

import jakarta.validation.Valid;

@Controller
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    @Autowired
    private AuthenticationService authenticationService;

    // ==============================================================================
    // PHẦN 1: ĐIỀU HƯỚNG GIAO DIỆN WEB (TRẢ VỀ FILE HTML)
    // ==============================================================================

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "auth/register";
    }

    @GetMapping("/verify-otp")
    public String verifyOtpPage() {
        return "auth/verify-otp";
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "auth/forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage() {
        return "auth/reset-password";
    }

    // ==============================================================================
    // PHẦN 2: XỬ LÝ API
    // ==============================================================================

    @PostMapping("/api/auth/register")
    @ResponseBody
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            log.info("Nhận yêu cầu đăng ký email: {}", request.getEmail());
            RegisterResponse response = authenticationService.register(request);

            if (response.getSuccess()) {
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
        } catch (Exception e) {
            log.error("Lỗi không mong muốn khi đăng ký: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new Object() {
                        public final String message = "Lỗi hệ thống: " + e.getMessage();
                        public final boolean success = false;
                    });
        }
    }

    @PostMapping("/api/auth/verify-otp")
    @ResponseBody
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        try {
            log.info("Nhận yêu cầu xác thực OTP cho email: {}", request.getEmail());
            VerifyOtpResponse response = authenticationService.verifyOtp(request);

            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
        } catch (Exception e) {
            log.error("Lỗi không mong muốn khi xác thực OTP: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new Object() {
                        public final String message = "Lỗi hệ thống: " + e.getMessage();
                        public final boolean success = false;
                    });
        }
    }

    // BỔ SUNG API CHECK OTP CHO LUỒNG QUÊN MẬT KHẨU
    @PostMapping("/api/auth/check-reset-otp")
    @ResponseBody
    public ResponseEntity<?> checkResetOtp(@Valid @RequestBody VerifyOtpRequest request) {
        try {
            log.info("Nhận yêu cầu kiểm tra OTP khôi phục mật khẩu cho email: {}", request.getEmail());
            VerifyOtpResponse response = authenticationService.checkResetOtp(request);

            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
        } catch (Exception e) {
            log.error("Lỗi không mong muốn khi kiểm tra OTP đặt lại mật khẩu: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new Object() {
                        public final String message = "Lỗi hệ thống: " + e.getMessage();
                        public final boolean success = false;
                    });
        }
    }

    @PostMapping("/api/auth/resend-otp")
    @ResponseBody
    public ResponseEntity<?> resendOtp(@RequestBody java.util.Map<String, String> request) {
        try {
            String email = request.get("email");
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Object() {
                    public final boolean success = false;
                    public final String message = "Email không được để trống";
                });
            }

            authenticationService.resendOtp(email);

            return ResponseEntity.ok(new Object() {
                public final boolean success = true;
                public final String message = "Một mã OTP mới đã được gửi (Check Console)";
            });
        } catch (Exception e) {
            log.error("Lỗi không mong muốn khi gửi lại OTP: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new Object() {
                        public final String message = "Lỗi hệ thống: " + e.getMessage();
                        public final boolean success = false;
                    });
        }
    }

    @PostMapping("/api/auth/login")
    @ResponseBody
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            log.info("Nhận yêu cầu đăng nhập email: {}", request.getEmail());
            LoginResponse response = authenticationService.login(request);

            if (response.getAccessToken() != null) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
        } catch (Exception e) {
            log.error("Lỗi không mong muốn khi đăng nhập: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new Object() {
                        public final String message = "Lỗi hệ thống: " + e.getMessage();
                    });
        }
    }

    @PostMapping("/api/auth/google")
    @ResponseBody
    public ResponseEntity<?> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        try {
            log.info("Nhận yêu cầu đăng nhập bằng Google OAuth2");
            LoginResponse response = authenticationService.loginWithGoogle(request.getCode());

            if (response.getAccessToken() != null) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
        } catch (Exception e) {
            log.error("Lỗi khi đăng nhập bằng Google: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new Object() {
                        public final String message = e.getMessage();
                        public final boolean success = false;
                    });
        }
    }

    @PostMapping("/api/auth/logout")
    @ResponseBody
    public ResponseEntity<?> logout(@Valid @RequestBody LogoutRequest request) {
        try {
            LogoutResponse response = authenticationService.logout(request.getRefreshToken());
            if (response.getSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new Object() {
                        public final String message = "Lỗi hệ thống: " + e.getMessage();
                        public final boolean success = false;
                    });
        }
    }

    @PostMapping("/api/auth/refresh")
    @ResponseBody
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            LoginResponse response = authenticationService.refreshAccessToken(request.getRefreshToken());
            if (response.getAccessToken() != null) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new Object() {
                        public final String message = "Lỗi hệ thống: " + e.getMessage();
                    });
        }
    }

    @PostMapping("/api/auth/forgot-password")
    @ResponseBody
    public ResponseEntity<?> forgotPassword(@RequestBody java.util.Map<String, String> request) {
        try {
            String email = request.get("email");
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Object() {
                    public final boolean success = false;
                    public final String message = "Vui lòng cung cấp email";
                });
            }

            authenticationService.forgotPassword(email);

            return ResponseEntity.ok(new Object() {
                public final boolean success = true;
                public final String message = "Mã khôi phục đã được gửi";
            });
        } catch (Exception e) {
            log.error("Lỗi khi gửi mã quên mật khẩu: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Object() {
                public final boolean success = false;
                public final String message = e.getMessage();
            });
        }
    }

    @PostMapping("/api/auth/reset-password")
    @ResponseBody
    public ResponseEntity<?> resetPassword(@RequestBody java.util.Map<String, String> request) {
        try {
            String email = request.get("email");
            String otp = request.get("otp");
            String newPassword = request.get("newPassword");

            authenticationService.resetPassword(email, otp, newPassword);

            return ResponseEntity.ok(new Object() {
                public final boolean success = true;
                public final String message = "Đổi mật khẩu thành công";
            });
        } catch (Exception e) {
            log.error("Lỗi khi reset mật khẩu: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Object() {
                public final boolean success = false;
                public final String message = e.getMessage();
            });
        }
    }

    @GetMapping("/api/auth/health")
    @ResponseBody
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(new Object() {
            public final String status = "OK";
            public final String message = "MMO Market System Authentication Service is running";
        });
    }
}