package controller;

import controller.dto.RoleUpdateRequest;
import controller.dto.StaffUpsertRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import service.AdminUserManagementService;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/user-management")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AdminUserController {
    private final AdminUserManagementService adminUserManagementService;

    public AdminUserController(AdminUserManagementService adminUserManagementService) {
        this.adminUserManagementService = adminUserManagementService;
    }

    @GetMapping("/users")
    public ResponseEntity<?> getUsers(@RequestParam(required = false) String search,
                                      @RequestParam(required = false) String role,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "10") int size) {
        try {
            return ResponseEntity.ok(adminUserManagementService.getUsers(currentUserId(), search, role, page, size));
        } catch (ResponseStatusException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(Map.of("message", ex.getReason()));
        }
    }

    @GetMapping("/summary")
    public ResponseEntity<?> getSummary() {
        try {
            return ResponseEntity.ok(adminUserManagementService.getDashboardSummary(currentUserId()));
        } catch (ResponseStatusException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(Map.of("message", ex.getReason()));
        }
    }

    @PostMapping("/users/{id}/toggle-lock")
    public ResponseEntity<?> toggleLock(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(adminUserManagementService.toggleLock(currentUserId(), id));
        } catch (ResponseStatusException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(Map.of("message", ex.getReason()));
        }
    }

    @PostMapping("/users/{id}/role")
    public ResponseEntity<?> updateRole(@PathVariable Long id, @RequestBody RoleUpdateRequest request) {
        try {
            String role = request == null ? null : request.getRole();
            return ResponseEntity.ok(adminUserManagementService.updateRole(currentUserId(), id, role));
        } catch (ResponseStatusException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(Map.of("message", ex.getReason()));
        }
    }

    @PostMapping("/staff")
    public ResponseEntity<?> createStaff(@RequestBody StaffUpsertRequest request) {
        try {
            return ResponseEntity.ok(adminUserManagementService.createStaff(currentUserId(), request));
        } catch (ResponseStatusException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(Map.of("message", ex.getReason()));
        }
    }

    @PutMapping("/staff/{id}")
    public ResponseEntity<?> updateStaff(@PathVariable Long id, @RequestBody StaffUpsertRequest request) {
        try {
            return ResponseEntity.ok(adminUserManagementService.updateStaff(currentUserId(), id, request));
        } catch (ResponseStatusException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(Map.of("message", ex.getReason()));
        }
    }

    @DeleteMapping("/staff/{id}")
    public ResponseEntity<?> deleteStaff(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(adminUserManagementService.deleteStaff(currentUserId(), id));
        } catch (ResponseStatusException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(Map.of("message", ex.getReason()));
        }
    }

    private Long currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof Long userId)) {
            return null;
        }
        return userId;
    }
}
