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

@RestController
@RequestMapping("/api/admin/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public Map<String, Object> getAuditLogs(
            @AuthenticationPrincipal Long operatorId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String action,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return auditLogService.getAuditLogs(operatorId, search, action, page, size);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportAuditLogsCsv(
            @AuthenticationPrincipal Long operatorId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String action) {
        byte[] csvData = auditLogService.exportAuditLogsCsv(operatorId, search, action);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=nhat-ky-he-thong.csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
    }
}
