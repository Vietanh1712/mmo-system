package controller;

import controller.dto.AdminActionResponse;
import controller.dto.AdminUserResponse;
import controller.dto.StaffUpsertRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import service.AdminUserManagementService;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/user-management")
public class AdminUserManagementController {

    private final AdminUserManagementService adminUserManagementService;

    public AdminUserManagementController(AdminUserManagementService adminUserManagementService) {
        this.adminUserManagementService = adminUserManagementService;
    }

    @GetMapping("/summary")
    public Map<String, Object> getDashboardSummary(@AuthenticationPrincipal Long operatorId) {
        return adminUserManagementService.getDashboardSummary(operatorId);
    }

    @GetMapping("/users")
    public Map<String, Object> getUsers(
            @AuthenticationPrincipal Long operatorId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return adminUserManagementService.getUsers(operatorId, search, role, page, size);
    }

    @PostMapping("/users/{userId}/toggle-lock")
    public AdminActionResponse toggleLock(
            @AuthenticationPrincipal Long operatorId,
            @PathVariable Long userId) {
        return adminUserManagementService.toggleLock(operatorId, userId);
    }

    @PutMapping("/users/{userId}/role")
    public AdminActionResponse updateRole(
            @AuthenticationPrincipal Long operatorId,
            @PathVariable Long userId,
            @RequestBody Map<String, String> request) {
        return adminUserManagementService.updateRole(operatorId, userId, request.get("role"));
    }

    @PostMapping("/staff")
    public AdminUserResponse createStaff(
            @AuthenticationPrincipal Long operatorId,
            @RequestBody StaffUpsertRequest request) {
        return adminUserManagementService.createStaff(operatorId, request);
    }

    @PutMapping("/staff/{staffId}")
    public AdminUserResponse updateStaff(
            @AuthenticationPrincipal Long operatorId,
            @PathVariable Long staffId,
            @RequestBody StaffUpsertRequest request) {
        return adminUserManagementService.updateStaff(operatorId, staffId, request);
    }

    @DeleteMapping("/staff/{staffId}")
    public AdminActionResponse deleteStaff(
            @AuthenticationPrincipal Long operatorId,
            @PathVariable Long staffId) {
        return adminUserManagementService.deleteStaff(operatorId, staffId);
    }
}
