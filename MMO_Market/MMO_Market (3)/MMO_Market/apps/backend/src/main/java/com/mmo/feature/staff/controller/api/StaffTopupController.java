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

/**
 * Controller xử lý nghiệp vụ liên quan đến Nạp tiền (Topup) dành cho Nhân viên (Staff) và Admin.
 * Cho phép nhân viên xem danh sách giao dịch nạp tiền, thống kê, xem chi tiết và xử lý lại (retry) thủ công
 * đối với các giao dịch nạp tiền bị lỗi hoặc bị nghẽn mạng.
 */
@RestController
@RequestMapping("/api/v1/staff/topups")
@PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
public class StaffTopupController {

    @Autowired
    private TopupService topupService;

    /**
     * Lấy danh sách các giao dịch nạp tiền với các tham số lọc.
     * @param status Trạng thái đơn nạp (VD: pending, completed, failed)
     * @param keyword Từ khóa tìm kiếm (mã giao dịch, tên người nạp, số tiền)
     * @param page Trang hiện tại (mặc định 0)
     * @param size Số lượng bản ghi trên một trang (mặc định 10)
     * @return Danh sách các đơn nạp tiền đã được phân trang.
     */
    @GetMapping
    public ResponseEntity<Page<TopupResponseDto>> getAllTopups(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(topupService.getAllTopups(status, keyword, page, size));
    }

    /**
     * Lấy các số liệu thống kê tổng quát về Nạp tiền.
     * @return Map chứa các thống kê (tổng số giao dịch, tổng tiền nạp thành công, v.v).
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getTopupStats() {
        return ResponseEntity.ok(topupService.getTopupStats());
    }

    /**
     * Lấy chi tiết một giao dịch nạp tiền cụ thể thông qua ID.
     * @param id ID của giao dịch nạp tiền.
     * @return DTO chứa thông tin chi tiết giao dịch.
     */
    @GetMapping("/{id}")
    public ResponseEntity<TopupResponseDto> getTopupById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(topupService.getTopupById(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Xử lý lại (Retry) một giao dịch nạp tiền bị lỗi thủ công (Duyệt nạp tay).
     * @param id ID của đơn nạp tiền cần xử lý
     * @param dto Dữ liệu đính kèm khi xử lý (ví dụ: Ghi chú của nhân viên duyệt)
     * @param staffUserId ID của nhân viên đang thực hiện thao tác duyệt (lấy từ Security Context)
     * @return Kết quả giao dịch sau khi duyệt.
     */
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
