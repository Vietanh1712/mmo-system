package controller.api;

import controller.dto.ShopRegistrationRequestDto;
import controller.dto.ShopRegistrationResponseDto;
import controller.dto.ShopRegistrationReviewDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import service.ShopRegistrationService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/shop-registrations")
public class ShopRegistrationController {

    @Autowired
    private ShopRegistrationService shopRegistrationService;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('SELLER')")
    public ResponseEntity<ShopRegistrationResponseDto> submitRegistration(
            @AuthenticationPrincipal Long userId,
            @RequestBody ShopRegistrationRequestDto request) {

        try {
            ShopRegistrationResponseDto response = shopRegistrationService.submitRegistration(userId, request);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ShopRegistrationResponseDto.builder().status("ERROR").description(e.getMessage()).build());
        }
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('SELLER')")
    public ResponseEntity<ShopRegistrationResponseDto> getMyRegistration(@AuthenticationPrincipal Long userId) {

        ShopRegistrationResponseDto response = shopRegistrationService.getMyRegistration(userId);
        if (response == null) {
            return ResponseEntity.ok(ShopRegistrationResponseDto.builder().status("NOT_SUBMITTED").build());
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    public ResponseEntity<List<ShopRegistrationResponseDto>> getAllPendingRegistrations() {
        return ResponseEntity.ok(shopRegistrationService.getAllPendingRegistrations());
    }

    @PutMapping("/{id}/review")
    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    public ResponseEntity<ShopRegistrationResponseDto> reviewRegistration(@PathVariable Long id, @RequestBody ShopRegistrationReviewDto review) {
        try {
            ShopRegistrationResponseDto response = shopRegistrationService.reviewRegistration(id, review);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ShopRegistrationResponseDto.builder().status("ERROR").description(e.getMessage()).build());
        }
    }
}
