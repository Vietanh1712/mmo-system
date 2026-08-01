package com.mmo.feature.admin.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.mmo.feature.admin.service.AuditLogService;

import java.util.Map;

/**
 * Controller truy xuất Nhật ký hệ thống (Audit Logs) dành cho Admin.
 * Lưu vết và trả về các lịch sử thao tác quan trọng của hệ thống (như thay đổi quyền, duyệt rút tiền, duyệt KYC).
 */
@RestController
@RequestMapping("/api/admin/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    /**
     * Lấy danh sách nhật ký hệ thống kèm các tiêu chí lọc.
     * @param operatorId ID của Admin (người đang thao tác xem log)
     * @param search Từ khóa tìm kiếm (theo tên người thao tác, IP, nội dung)
     * @param action Mã hành động cụ thể (ví dụ: KYC_Approve, Fund_Withdraw)
     * @param category Nhóm hành động (ví dụ: FINANCE, SHOP, USER_MGMT)
     * @param startDate Ngày bắt đầu (để lọc theo khoảng thời gian)
     * @param endDate Ngày kết thúc
     * @return Map chứa content (danh sách log) và thông tin phân trang.
     */
    @GetMapping
    public Map<String, Object> getAuditLogs(
            @AuthenticationPrincipal Long operatorId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "DESC") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return auditLogService.getAuditLogs(operatorId, search, action, category, startDate, endDate, sort, page, size);
    }

    /**
     * Xuất dữ liệu nhật ký hệ thống ra file CSV để Admin tải về.
     * Các tham số lọc tương tự như API lấy danh sách log.
     * @return File CSV chứa dữ liệu log.
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportAuditLogsCsv(
            @AuthenticationPrincipal Long operatorId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "DESC") String sort) {
        byte[] csvData = auditLogService.exportAuditLogsCsv(operatorId, search, action, category, startDate, endDate, sort);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=nhat-ky-he-thong.csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
    }
}
