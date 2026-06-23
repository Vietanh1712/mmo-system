package controller;

import controller.dto.ProfileResponse;
import controller.dto.UpdateProfileRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import service.UserService;

import org.springframework.web.bind.annotation.PostMapping;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ProfileResponse viewProfile(@AuthenticationPrincipal Long userId) {
        return userService.getMyProfile(userId);
    }

    @PutMapping
    public ProfileResponse updateProfile(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateMyProfile(userId, request);
    }

    @PostMapping("/register-shop")
    public ProfileResponse registerShop(
            @AuthenticationPrincipal Long userId,
            @RequestBody Map<String, String> request) {
        return userService.registerShop(userId, request);
    }
}
