package com.mmo.feature.staff.controller;

import com.mmo.shared.model.Permission;
import com.mmo.shared.model.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.mmo.feature.staff.service.StaffPermissionService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/staff-permissions")
@PreAuthorize("hasRole('ADMIN')")
public class StaffPermissionController {

    private final StaffPermissionService staffPermissionService;

    public StaffPermissionController(StaffPermissionService staffPermissionService) {
        this.staffPermissionService = staffPermissionService;
    }

    @GetMapping("/permissions")
    public ResponseEntity<List<Permission>> getAllPermissions() {
        return ResponseEntity.ok(staffPermissionService.getAllPermissions());
    }

    @GetMapping("/staffs/{staffId}")
    public ResponseEntity<List<String>> getStaffPermissions(@PathVariable Long staffId) {
        return ResponseEntity.ok(staffPermissionService.getStaffPermissions(staffId));
    }

    @GetMapping("/all-assigned")
    public ResponseEntity<Map<Long, List<String>>> getAllAssignedPermissions() {
        List<User> staffs = staffPermissionService.searchStaffByPermissions(null, "ALL");
        Map<Long, List<String>> result = new HashMap<>();
        for (User staff : staffs) {
            List<String> perms = staff.getUserPermissions().stream()
                    .map(Permission::getName)
                    .collect(Collectors.toList());
            result.put(staff.getId(), perms);
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/assign")
    public ResponseEntity<Map<String, Object>> assignPermissions(@RequestBody AssignRequest request) {
        staffPermissionService.assignPermissions(request.getUserIds(), request.getPermissionNames());
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Gán quyền thành công.");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/revoke")
    public ResponseEntity<Map<String, Object>> revokePermissions(@RequestBody RevokeRequest request) {
        staffPermissionService.revokePermissions(request.getUserId(), request.getPermissionNames());
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Thu hồi quyền thành công.");
        return ResponseEntity.ok(response);
    }

    // Requests DTOs
    public static class AssignRequest {
        private List<Long> userIds;
        private List<String> permissionNames;

        public List<Long> getUserIds() { return userIds; }
        public void setUserIds(List<Long> userIds) { this.userIds = userIds; }
        public List<String> getPermissionNames() { return permissionNames; }
        public void setPermissionNames(List<String> permissionNames) { this.permissionNames = permissionNames; }
    }

    public static class RevokeRequest {
        private Long userId;
        private List<String> permissionNames;

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public List<String> getPermissionNames() { return permissionNames; }
        public void setPermissionNames(List<String> permissionNames) { this.permissionNames = permissionNames; }
    }
}
