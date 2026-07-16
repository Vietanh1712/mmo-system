package com.mmo.feature.seller.controller.api;
import com.mmo.shared.model.Review;

import com.mmo.shared.dto.ShopRegistrationRequestDto;
import com.mmo.shared.dto.ShopRegistrationResponseDto;
import com.mmo.shared.dto.ShopRegistrationReviewDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.mmo.feature.seller.service.ShopRegistrationService;

import java.util.List;
import java.util.Map;

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
    @PreAuthorize("hasAuthority('MANAGE_SHOPS') or hasRole('ADMIN')")
    public ResponseEntity<Page<ShopRegistrationResponseDto>> getAllRegistrations(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String shopStatus,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(shopRegistrationService.getAllRegistrations(status, shopStatus, keyword, page, size));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('MANAGE_SHOPS') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Long>> getRegistrationStats() {
        return ResponseEntity.ok(shopRegistrationService.getRegistrationStats());
    }

    @GetMapping("/shop-statuses")
    @PreAuthorize("hasAuthority('MANAGE_SHOPS') or hasRole('ADMIN')")
    public ResponseEntity<List<String>> getDistinctShopStatuses() {
        return ResponseEntity.ok(shopRegistrationService.getDistinctShopStatuses());
    }

    @GetMapping("/statuses")
    @PreAuthorize("hasAuthority('MANAGE_SHOPS') or hasRole('ADMIN')")
    public ResponseEntity<List<String>> getDistinctStatuses() {
        return ResponseEntity.ok(shopRegistrationService.getDistinctStatuses());
    }

    @PutMapping("/{id}/review")
    @PreAuthorize("hasAuthority('MANAGE_SHOPS') or hasRole('ADMIN')")
    public ResponseEntity<ShopRegistrationResponseDto> reviewRegistration(@PathVariable Long id, @RequestBody ShopRegistrationReviewDto review) {
        try {
            ShopRegistrationResponseDto response = shopRegistrationService.reviewRegistration(id, review);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ShopRegistrationResponseDto.builder().status("ERROR").description(e.getMessage()).build());
        }
    }

    @PutMapping("/{id}/toggle-status")
    @PreAuthorize("hasAuthority('MANAGE_SHOPS') or hasRole('ADMIN')")
    public ResponseEntity<ShopRegistrationResponseDto> toggleShopStatus(
            @PathVariable Long id,
            @RequestParam boolean active) {
        try {
            ShopRegistrationResponseDto response = shopRegistrationService.toggleShopStatus(id, active);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ShopRegistrationResponseDto.builder().status("ERROR").description(e.getMessage()).build());
        }
    }
}
