package com.mmo.feature.staff.controller.api;

import com.mmo.feature.wallet.service.TopupService;
import com.mmo.shared.dto.TopupResponseDto;
import com.mmo.shared.dto.TopupRetryRequestDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/staff/topups")
@PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
public class StaffTopupController {

    @Autowired
    private TopupService topupService;

    @GetMapping
    public ResponseEntity<Page<TopupResponseDto>> getAllTopups(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(topupService.getAllTopups(status, keyword, page, size));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getTopupStats() {
        return ResponseEntity.ok(topupService.getTopupStats());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TopupResponseDto> getTopupById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(topupService.getTopupById(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<?> retryTopup(
            @PathVariable Long id,
            @RequestBody TopupRetryRequestDto dto,
            @AuthenticationPrincipal Long staffUserId) {
        try {
            TopupResponseDto response = topupService.retryTopup(id, dto, staffUserId);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Lỗi xử lý duyệt nạp tiền thủ công: " + e.getMessage()));
        }
    }
}
