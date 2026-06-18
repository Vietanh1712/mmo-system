package controller;

import controller.dto.RevenueSummaryResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import service.AdminRevenueService;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/revenue")
public class AdminRevenueController {

    private final AdminRevenueService adminRevenueService;

    public AdminRevenueController(AdminRevenueService adminRevenueService) {
        this.adminRevenueService = adminRevenueService;
    }

    @GetMapping("/summary")
    public ResponseEntity<RevenueSummaryResponse> getRevenueSummary(@AuthenticationPrincipal Long operatorId) {
        return ResponseEntity.ok(adminRevenueService.getRevenueSummary(operatorId));
    }

    @GetMapping("/transactions")
    public ResponseEntity<Map<String, Object>> getCashflowTransactions(
            @AuthenticationPrincipal Long operatorId,
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(required = false, defaultValue = "") String type,
            @RequestParam(required = false, defaultValue = "") String time,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminRevenueService.getCashflowTransactions(operatorId, keyword, type, time, page, size));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportRevenueCsv(
            @AuthenticationPrincipal Long operatorId,
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(required = false, defaultValue = "") String type,
            @RequestParam(required = false, defaultValue = "") String time) {
        byte[] csvData = adminRevenueService.exportRevenueCsv(operatorId, keyword, type, time);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=bao-cao-doanh-thu.csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
    }
}
