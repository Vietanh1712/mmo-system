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

/**
 * Controller phục vụ cho quy trình Mở Shop (Đăng ký gian hàng).
 * Cung cấp API cho Khách hàng nộp đơn đăng ký mở Shop,
 * và API cho Nhân viên (Staff)/Admin xem danh sách, duyệt đơn và quản lý trạng thái Shop.
 */
@RestController
@RequestMapping("/api/v1/shop-registrations")
public class ShopRegistrationController {

    @Autowired
    private ShopRegistrationService shopRegistrationService;

    /**
     * Nộp yêu cầu đăng ký mở Shop mới.
     * Áp dụng cho người dùng phổ thông (Customer) hoặc người bán (Seller) muốn sửa hồ sơ.
     * @param request Chứa tên Shop, mô tả Shop, v.v.
     */
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

    /**
     * Lấy thông tin hồ sơ đăng ký mở Shop của chính người dùng đang đăng nhập.
     * Giúp người dùng biết trạng thái hồ sơ của mình (Chờ duyệt, Đã duyệt, Từ chối).
     */
    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('SELLER')")
    public ResponseEntity<ShopRegistrationResponseDto> getMyRegistration(@AuthenticationPrincipal Long userId) {

        ShopRegistrationResponseDto response = shopRegistrationService.getMyRegistration(userId);
        if (response == null) {
            return ResponseEntity.ok(ShopRegistrationResponseDto.builder().status("NOT_SUBMITTED").build());
        }
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy danh sách các đơn đăng ký mở Shop dành cho Staff/Admin (hỗ trợ phân trang và tìm kiếm).
     * @param status Trạng thái của Đơn đăng ký (PENDING, APPROVED, REJECTED)
     * @param shopStatus Trạng thái hoạt động của Shop (ACTIVE, SUSPENDED, BLOCKED)
     * @param keyword Tìm kiếm theo tên Shop hoặc Email chủ Shop
     */
    @GetMapping
    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    public ResponseEntity<Page<ShopRegistrationResponseDto>> getAllRegistrations(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String shopStatus,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(shopRegistrationService.getAllRegistrations(status, shopStatus, keyword, page, size));
    }

    /**
     * Thống kê tổng số lượng các Đơn đăng ký theo từng trạng thái.
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Long>> getRegistrationStats() {
        return ResponseEntity.ok(shopRegistrationService.getRegistrationStats());
    }

    @GetMapping("/shop-statuses")
    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    public ResponseEntity<List<String>> getDistinctShopStatuses() {
        return ResponseEntity.ok(shopRegistrationService.getDistinctShopStatuses());
    }

    @GetMapping("/statuses")
    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    public ResponseEntity<List<String>> getDistinctStatuses() {
        return ResponseEntity.ok(shopRegistrationService.getDistinctStatuses());
    }

    /**
     * Xem chi tiết một hồ sơ đăng ký mở Shop thông qua ID.
     * Dành cho Staff/Admin xem trước khi đưa ra quyết định Duyệt hay Từ chối.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    public ResponseEntity<ShopRegistrationResponseDto> getRegistrationById(@PathVariable Long id) {
        try {
            ShopRegistrationResponseDto response = shopRegistrationService.getRegistrationById(id);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }


    /**
     * Phê duyệt (Approve) hoặc Từ chối (Reject) đơn đăng ký mở Shop.
     * Nếu được phê duyệt, Role của người dùng sẽ tự động nâng lên thành SELLER.
     */
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

    /**
     * Kích hoạt hoặc Tạm ngưng hoạt động nhanh một gian hàng (Shop).
     * @param active true: Kích hoạt, false: Tạm ngưng.
     */
    @PutMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
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

    /**
     * Cập nhật trạng thái chi tiết của Shop (Khóa vĩnh viễn, Tạm đình chỉ có thời hạn).
     * Dành cho việc xử lý vi phạm của người bán.
     * @param status Mã trạng thái mới (Ví dụ: BLOCKED, SUSPENDED)
     * @param suspendedUntil Nếu bị đình chỉ, thời hạn đình chỉ đến bao giờ.
     */
    @PutMapping("/{id}/update-status")
    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    public ResponseEntity<ShopRegistrationResponseDto> updateShopStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) String suspendedUntil) {
        try {
            ShopRegistrationResponseDto response = shopRegistrationService.updateShopStatus(id, status, suspendedUntil);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ShopRegistrationResponseDto.builder().status("ERROR").description(e.getMessage()).build());
        }
    }
}
