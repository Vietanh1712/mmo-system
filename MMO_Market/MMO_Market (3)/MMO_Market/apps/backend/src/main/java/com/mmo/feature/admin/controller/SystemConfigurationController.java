package com.mmo.feature.admin.controller;

import com.mmo.shared.dto.AdminActionResponse;
import com.mmo.shared.dto.CommissionsUpdateRequest;
import com.mmo.shared.dto.SystemConfigResponse;
import com.mmo.shared.dto.SystemConfigUpdateRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.mmo.feature.admin.service.SystemConfigurationService;

@RestController
@RequestMapping("/api/admin/system-config")
public class SystemConfigurationController {

    private final SystemConfigurationService systemConfigurationService;

    public SystemConfigurationController(SystemConfigurationService systemConfigurationService) {
        this.systemConfigurationService = systemConfigurationService;
    }

    @GetMapping
    public SystemConfigResponse getConfigurations(@AuthenticationPrincipal Long operatorId) {
        return systemConfigurationService.getConfigurations(operatorId);
    }

    @PutMapping("/general")
    public AdminActionResponse updateGeneralConfig(
            @AuthenticationPrincipal Long operatorId,
            @RequestBody SystemConfigUpdateRequest request) {
        try {
            systemConfigurationService.updateGeneralConfig(operatorId, request);
            return AdminActionResponse.builder()
                    .success(true)
                    .message("Đã cập nhật cấu hình hệ thống thành công.")
                    .build();
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return AdminActionResponse.builder()
                    .success(false)
                    .message(e.getReason())
                    .build();
        } catch (Exception e) {
            return AdminActionResponse.builder()
                    .success(false)
                    .message("Không thể cập nhật cấu hình hệ thống: " + e.getMessage())
                    .build();
        }
    }

    @PutMapping("/commissions")
    public AdminActionResponse updateCommissionsConfig(
            @AuthenticationPrincipal Long operatorId,
            @RequestBody CommissionsUpdateRequest request) {
        try {
            systemConfigurationService.updateCommissionsConfig(operatorId, request);
            return AdminActionResponse.builder()
                    .success(true)
                    .message("Đã cập nhật cấu hình phí & hoa hồng thành công.")
                    .build();
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return AdminActionResponse.builder()
                    .success(false)
                    .message(e.getReason())
                    .build();
        } catch (Exception e) {
            return AdminActionResponse.builder()
                    .success(false)
                    .message("Không thể cập nhật cấu hình phí & hoa hồng: " + e.getMessage())
                    .build();
        }
    }
}
