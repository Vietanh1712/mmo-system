package com.mmo.feature.staff.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.mmo.feature.staff.service.StaffPermissionService;

import java.util.List;

/**
 * Controller cung cấp API cho phép Nhân viên (Staff) tự kiểm tra các quyền hiện có của mình.
 * API này được Frontend sử dụng để ẩn/hiện các menu chức năng trên giao diện tùy thuộc vào quyền của nhân viên.
 */
@RestController
@RequestMapping("/api/staff")
public class StaffMyPermissionsController {

    private final StaffPermissionService staffPermissionService;

    public StaffMyPermissionsController(StaffPermissionService staffPermissionService) {
        this.staffPermissionService = staffPermissionService;
    }

    /**
     * Lấy danh sách các quyền (Permissions) mà nhân viên đang đăng nhập được cấp.
     * @param userId ID của nhân viên (lấy từ Security Context JWT)
     * @return Danh sách các chuỗi định danh quyền (VD: ["APPROVE_KYC", "MANAGE_SHOPS"]).
     */
    @GetMapping("/my-permissions")
    public ResponseEntity<List<String>> getMyPermissions(@AuthenticationPrincipal Long userId) {
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        List<String> permissions = staffPermissionService.getStaffPermissions(userId);
        return ResponseEntity.ok(permissions);
    }
}
