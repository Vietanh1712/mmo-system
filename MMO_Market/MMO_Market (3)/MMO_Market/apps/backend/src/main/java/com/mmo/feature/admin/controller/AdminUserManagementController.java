package com.mmo.feature.admin.controller;
import com.mmo.shared.model.User;

import com.mmo.shared.dto.AdminActionResponse;
import com.mmo.shared.dto.AdminUserResponse;
import com.mmo.shared.dto.StaffUpsertRequest;
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
import com.mmo.feature.admin.service.AdminUserManagementService;

import java.util.Map;

/**
 * Controller quản lý tài khoản người dùng và nhân viên dành riêng cho quyền Admin.
 * Cung cấp các API để thống kê, tạo, sửa, xóa (soft delete), khóa/mở khóa tài khoản
 * và phân quyền (Role) cho người dùng trong hệ thống.
 */
@RestController
@RequestMapping("/api/admin/user-management")
public class AdminUserManagementController {

    private final AdminUserManagementService adminUserManagementService;

    public AdminUserManagementController(AdminUserManagementService adminUserManagementService) {
        this.adminUserManagementService = adminUserManagementService;
    }

    /**
     * Lấy dữ liệu tổng quan (Dashboard Summary) cho trang chủ Admin.
     * @param operatorId ID của Admin đang thao tác (lấy từ Security Context)
     * @return Map chứa các số liệu thống kê (tổng người dùng, tổng doanh thu, v.v)
     */
    @GetMapping("/summary")
    public Map<String, Object> getDashboardSummary(@AuthenticationPrincipal Long operatorId) {
        return adminUserManagementService.getDashboardSummary(operatorId);
    }

    /**
     * Lấy thông tin chi tiết của một tài khoản người dùng theo ID.
     */
    @GetMapping("/users/{userId}")
    public AdminUserResponse getUser(
            @AuthenticationPrincipal Long operatorId,
            @PathVariable Long userId) {
        return adminUserManagementService.getUser(operatorId, userId);
    }

    /**
     * Lấy danh sách người dùng với các bộ lọc tùy chọn (email, số điện thoại, tên, giới tính, vai trò, trạng thái).
     * Hỗ trợ phân trang.
     * @return Map chứa danh sách người dùng (content) và các thông số phân trang (totalPages, totalElements).
     */
    @GetMapping("/users")
    public Map<String, Object> getUsers(
            @AuthenticationPrincipal Long operatorId,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return adminUserManagementService.getUsers(operatorId, email, phone, name, gender, role, status, page, size);
    }

    /**
     * Xóa mềm (Soft delete) một người dùng khỏi hệ thống.
     * Đánh dấu is_delete = true thay vì xóa hẳn khỏi Database.
     * @param userId ID của người dùng cần xóa.
     */
    @DeleteMapping("/users/{userId}")
    public AdminActionResponse softDeleteUser(
            @AuthenticationPrincipal Long operatorId,
            @PathVariable Long userId) {
        return adminUserManagementService.softDeleteUser(operatorId, userId);
    }

    /**
     * Khóa hoặc mở khóa một tài khoản người dùng.
     * @param userId ID của người dùng cần thay đổi trạng thái khóa.
     */
    @PostMapping("/users/{userId}/toggle-lock")
    public AdminActionResponse toggleLock(
            @AuthenticationPrincipal Long operatorId,
            @PathVariable Long userId) {
        return adminUserManagementService.toggleLock(operatorId, userId);
    }

    /**
     * Cập nhật vai trò (Role) cho người dùng (Ví dụ: từ Customer lên Staff).
     */
    @PutMapping("/users/{userId}/role")
    public AdminActionResponse updateRole(
            @AuthenticationPrincipal Long operatorId,
            @PathVariable Long userId,
            @RequestBody Map<String, String> request) {
        return adminUserManagementService.updateRole(operatorId, userId, request.get("role"));
    }

    /**
     * Tạo mới một tài khoản nhân viên (Staff).
     * @param request Dữ liệu chứa thông tin nhân viên (email, tên, mật khẩu, v.v).
     */
    @PostMapping("/staff")
    public AdminUserResponse createStaff(
            @AuthenticationPrincipal Long operatorId,
            @RequestBody StaffUpsertRequest request) {
        return adminUserManagementService.createStaff(operatorId, request);
    }

    /**
     * Cập nhật thông tin tài khoản nhân viên (Staff) theo ID.
     */
    @PutMapping("/staff/{staffId}")
    public AdminUserResponse updateStaff(
            @AuthenticationPrincipal Long operatorId,
            @PathVariable Long staffId,
            @RequestBody StaffUpsertRequest request) {
        return adminUserManagementService.updateStaff(operatorId, staffId, request);
    }

    /**
     * Xóa mềm (Soft delete) tài khoản nhân viên (Staff) khỏi hệ thống.
     */
    @DeleteMapping("/staff/{staffId}")
    public AdminActionResponse deleteStaff(
            @AuthenticationPrincipal Long operatorId,
            @PathVariable Long staffId) {
        return adminUserManagementService.deleteStaff(operatorId, staffId);
    }
}
