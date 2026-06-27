package com.mmo.feature.staff.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.mmo.feature.staff.service.StaffPermissionService;

import java.util.List;

@RestController
@RequestMapping("/api/staff")
public class StaffMyPermissionsController {

    private final StaffPermissionService staffPermissionService;

    public StaffMyPermissionsController(StaffPermissionService staffPermissionService) {
        this.staffPermissionService = staffPermissionService;
    }

    @GetMapping("/my-permissions")
    public ResponseEntity<List<String>> getMyPermissions(@AuthenticationPrincipal Long userId) {
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        List<String> permissions = staffPermissionService.getStaffPermissions(userId);
        return ResponseEntity.ok(permissions);
    }
}
