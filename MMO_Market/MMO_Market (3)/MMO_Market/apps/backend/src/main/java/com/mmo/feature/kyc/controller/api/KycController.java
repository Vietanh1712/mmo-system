package com.mmo.feature.kyc.controller.api;
import com.mmo.shared.model.IdType;

import com.mmo.shared.dto.KycResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.mmo.feature.kyc.service.KycService;
import com.mmo.feature.kyc.service.KycStorageService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

/**
 * Controller phục vụ cho quy trình Định danh khách hàng (KYC).
 * Cho phép người dùng (Customer, Seller) nộp hồ sơ KYC, xem lịch sử và xem lại tài liệu định danh của mình.
 */
@RestController
@RequestMapping("/api/v1/kyc")
@Slf4j
public class KycController {

    @Autowired
    private KycService kycService;

    @Autowired
    private KycStorageService kycStorageService;

    /**
     * Nộp hồ sơ xác minh danh tính (KYC).
     * @param fullName Họ và tên đầy đủ
     * @param dateOfBirth Ngày sinh
     * @param address Địa chỉ
     * @param idNumber Số CMND/CCCD/Hộ chiếu
     * @param idType Loại giấy tờ
     * @param frontImage Ảnh mặt trước
     * @param backImage Ảnh mặt sau
     * @param selfieImage Ảnh chụp chân dung (selfie)
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_CUSTOMER', 'ROLE_SELLER')")
    public ResponseEntity<?> submitKyc(
            Authentication authentication,
            @RequestParam("fullName") String fullName,
            @RequestParam("dateOfBirth") String dateOfBirth,
            @RequestParam("address") String address,
            @RequestParam("idNumber") String idNumber,
            @RequestParam("idType") String idType,
            @RequestParam("frontImage") MultipartFile frontImage,
            @RequestParam("backImage") MultipartFile backImage,
            @RequestParam("selfieImage") MultipartFile selfieImage
    ) {
        try {
            Long userId = (Long) authentication.getPrincipal();
            KycResponseDto response = kycService.submitKyc(userId, fullName, dateOfBirth, address, idNumber, idType, frontImage, backImage, selfieImage);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalStateException e) {
            log.warn("Lỗi trạng thái KYC: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new Object() {
                public final String message = e.getMessage();
                public final boolean success = false;
            });
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Object() {
                public final String message = e.getMessage();
                public final boolean success = false;
            });
        } catch (Exception e) {
            log.error("Lỗi server khi nộp KYC", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new Object() {
                public final String message = "Lỗi hệ thống khi nộp KYC.";
                public final boolean success = false;
            });
        }
    }

    /**
     * Lấy lịch sử các lần nộp hồ sơ KYC của chính người dùng đang đăng nhập.
     */
    @GetMapping("/me")
    @PreAuthorize("hasAnyAuthority('ROLE_CUSTOMER', 'ROLE_SELLER')")
    public ResponseEntity<List<KycResponseDto>> getMyKyc(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        List<KycResponseDto> history = kycService.getMyKycHistory(userId);
        return ResponseEntity.ok(history);
    }

    /**
     * Lấy file ảnh tài liệu KYC đã nộp.
     * Cấp quyền cho chủ sở hữu hồ sơ hoặc nhân viên/quản trị viên (Staff, Admin).
     * @param id ID của bản ghi KYC
     * @param docType Loại ảnh cần lấy (front, back, selfie)
     */
    @GetMapping("/{id}/documents/{docType}")
    @PreAuthorize("hasAnyAuthority('ROLE_CUSTOMER', 'ROLE_SELLER', 'ROLE_STAFF', 'ROLE_ADMIN')")
    public ResponseEntity<Resource> getKycDocument(
            Authentication authentication,
            @PathVariable Long id,
            @PathVariable String docType
    ) {
        Long userId = (Long) authentication.getPrincipal();
        String role = authentication.getAuthorities().toString();
        boolean isStaff = role.contains("ROLE_STAFF") || role.contains("ROLE_ADMIN");

        try {
            File file = kycService.getKycDocument(id, docType, userId, isStaff);
            if (file == null || !file.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            Resource resource = new FileSystemResource(file);
            String contentType = Files.probeContentType(file.toPath());
            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getName() + "\"")
                    .body(resource);

        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
