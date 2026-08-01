package com.mmo.feature.admin.controller;

import com.mmo.shared.dto.RevenueSummaryResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.mmo.feature.admin.service.AdminRevenueService;

import java.util.Map;

/**
 * Controller truy xuất báo cáo Doanh thu (Revenue) và dòng tiền (Cashflow) dành cho Admin.
 * Thống kê tổng doanh thu sàn, phí giao dịch, phí mở shop và hỗ trợ xuất file báo cáo.
 */
@RestController
@RequestMapping("/api/admin/revenue")
public class AdminRevenueController {

    private final AdminRevenueService adminRevenueService;

    public AdminRevenueController(AdminRevenueService adminRevenueService) {
        this.adminRevenueService = adminRevenueService;
    }

    /**
     * Lấy dữ liệu tổng quan về Doanh thu (Tổng doanh thu, Tổng phí nạp rút, Phí giao dịch).
     * @param operatorId ID của Admin đang xem báo cáo.
     */
    @GetMapping("/summary")
    public ResponseEntity<RevenueSummaryResponse> getRevenueSummary(@AuthenticationPrincipal Long operatorId) {
        return ResponseEntity.ok(adminRevenueService.getRevenueSummary(operatorId));
    }

    /**
     * Lấy danh sách giao dịch dòng tiền (nạp, rút, mua bán) với các tiêu chí lọc.
     * Hỗ trợ phân trang cho bảng dữ liệu.
     * @return Danh sách giao dịch cùng metadata phân trang.
     */
    @GetMapping("/transactions")
    public ResponseEntity<Map<String, Object>> getCashflowTransactions(
            @AuthenticationPrincipal Long operatorId,
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(required = false, defaultValue = "") String type,
            @RequestParam(required = false, defaultValue = "") String status,
            @RequestParam(required = false, defaultValue = "") String startDate,
            @RequestParam(required = false, defaultValue = "") String endDate,
            @RequestParam(required = false, defaultValue = "DESC") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminRevenueService.getCashflowTransactions(operatorId, keyword, type, status, startDate, endDate, sort, page, size));
    }

    /**
     * Xuất báo cáo dòng tiền ra định dạng file CSV để tải về máy.
     * Lọc dữ liệu theo các tham số tương tự như xem trên bảng.
     * @return Nội dung file CSV được encode.
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportRevenueCsv(
            @AuthenticationPrincipal Long operatorId,
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(required = false, defaultValue = "") String type,
            @RequestParam(required = false, defaultValue = "") String status,
            @RequestParam(required = false, defaultValue = "") String startDate,
            @RequestParam(required = false, defaultValue = "") String endDate,
            @RequestParam(required = false, defaultValue = "DESC") String sort) {
        byte[] csvData = adminRevenueService.exportRevenueCsv(operatorId, keyword, type, status, startDate, endDate, sort);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=bao-cao-doanh-thu.csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
    }
}
