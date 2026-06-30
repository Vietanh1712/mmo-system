package com.mmo.feature.kyc.controller.api;
import com.mmo.shared.model.Review;

import com.mmo.shared.dto.KycResponseDto;
import com.mmo.shared.dto.KycReviewRequest;
import lombok.extern.slf4j.Slf4j;
import com.mmo.shared.model.KycStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.mmo.feature.kyc.service.KycService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/staff/kyc")
@Slf4j
public class StaffKycController {

    @Autowired
    private KycService kycService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_STAFF', 'ROLE_ADMIN')")
    public ResponseEntity<Page<KycResponseDto>> getAllKycRequests(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String requestCode,
            @RequestParam(required = false) String idType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        KycStatus kycStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                kycStatus = KycStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
        }

        com.mmo.shared.model.IdType typeEnum = null;
        if (idType != null && !idType.isBlank()) {
            try {
                typeEnum = com.mmo.shared.model.IdType.valueOf(idType.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
        }
        
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
        Page<KycResponseDto> result = kycService.getAllKycRequests(kycStatus, requestCode, typeEnum, pageRequest);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_STAFF', 'ROLE_ADMIN')")
    public ResponseEntity<?> getKycRequestById(@PathVariable Long id) {
        try {
            KycResponseDto response = kycService.getKycRequestById(id);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Object() {
                public final String message = e.getMessage();
            });
        }
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("hasAnyAuthority('ROLE_STAFF', 'ROLE_ADMIN')")
    public ResponseEntity<?> reviewKycRequest(
            @PathVariable Long id,
            @Valid @RequestBody KycReviewRequest request,
            Authentication authentication
    ) {
        try {
            Long reviewerId = (Long) authentication.getPrincipal();
            KycResponseDto response = kycService.reviewKycRequest(id, reviewerId, request);
            return ResponseEntity.ok(response);
        } catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new Object() {
                public final String message = "Dữ liệu đã bị thay đổi bởi một người dùng khác. Vui lòng tải lại trang và thử lại.";
                public final boolean success = false;
            });
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Object() {
                public final String message = e.getMessage();
                public final boolean success = false;
            });
        } catch (Exception e) {
            log.error("Lỗi khi staff duyệt KYC", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new Object() {
                public final String message = "Lỗi hệ thống khi duyệt KYC.";
                public final boolean success = false;
            });
        }
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyAuthority('ROLE_STAFF', 'ROLE_ADMIN')")
    public ResponseEntity<java.util.Map<String, Long>> getKycStatistics() {
        return ResponseEntity.ok(kycService.getKycStatistics());
    }
}
