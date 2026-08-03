package com.mmo.feature.staff.controller;

import com.mmo.feature.product.service.CategoryService;
import com.mmo.shared.dto.CategoryRequest;
import com.mmo.shared.dto.CategoryResponse;
import com.mmo.shared.model.Category;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller API phục vụ chức năng quản lý danh mục sản phẩm (Categories) cho Nhân viên (Staff).
 * Cung cấp các thao tác xem, thêm, sửa, ẩn/hiện danh mục trên hệ thống.
 * Yêu cầu quyền ROLE_STAFF, ROLE_ADMIN hoặc quyền đặc biệt MANAGE_CATEGORIES.
 */
@RestController
@RequestMapping("/api/v1/staff/categories")
@PreAuthorize("hasAnyAuthority('ROLE_STAFF', 'ROLE_ADMIN', 'MANAGE_CATEGORIES') or hasAnyRole('STAFF', 'ADMIN')")
public class StaffCategoryApiController {

    @Autowired
    private CategoryService categoryService;

    /**
     * Tìm kiếm và hiển thị danh sách các danh mục (hỗ trợ phân trang).
     * @param keyword Từ khóa tìm kiếm theo tên danh mục
     * @param parentId ID danh mục cha (để lọc các danh mục con)
     * @param type Loại danh mục (tài khoản, phần mềm, dịch vụ, v.v)
     * @param isDelete Trạng thái (đang hoạt động hoặc đã bị ẩn)
     * @param sortBy Tiêu chí sắp xếp (VD: mới nhất)
     * @param page Số trang hiện tại
     * @param size Số bản ghi trên mỗi trang
     * @return Map chứa danh sách các danh mục và metadata phân trang
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> searchCategories(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long parentId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean isDelete,
            @RequestParam(required = false, defaultValue = "newest") String sortBy,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size) {
        
        List<CategoryResponse> categories = categoryService.searchCategoriesForStaff(keyword, parentId, type, isDelete, sortBy);
        
        int totalElements = categories.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        if (totalPages == 0) totalPages = 1;

        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);
        List<CategoryResponse> pagedContent = categories.subList(fromIndex, toIndex);

        Map<String, Object> response = new HashMap<>();
        response.put("content", pagedContent);
        response.put("currentPage", page);
        response.put("pageSize", size);
        response.put("totalElements", totalElements);
        response.put("totalPages", totalPages);
        response.put("fromIndex", totalElements > 0 ? fromIndex + 1 : 0);
        response.put("toIndex", toIndex);

        return ResponseEntity.ok(response);
    }

    /**
     * Lấy dữ liệu thống kê tổng quát về số lượng Danh mục.
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(categoryService.getCategoryStats());
    }

    /**
     * Lấy danh sách các danh mục cha (không có parentId) để làm dữ liệu dropdown.
     */
    @GetMapping("/parents")
    public ResponseEntity<List<Category>> getParentCategories() {
        return ResponseEntity.ok(categoryService.getParentCategories());
    }

    /**
     * Lấy chi tiết thông tin của một danh mục dựa theo ID.
     * @param id ID của danh mục.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getCategoryById(@PathVariable Long id) {
        try {
            CategoryResponse response = categoryService.getCategoryById(id);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    /**
     * Tạo mới một danh mục sản phẩm.
     * @param request Object chứa tên, mô tả, hình ảnh và loại danh mục mới.
     */
    @PostMapping
    public ResponseEntity<?> createCategory(@Valid @RequestBody CategoryRequest request) {
        try {
            CategoryResponse created = categoryService.createCategory(request);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Tạo danh mục thành công!");
            response.put("data", created);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Cập nhật thông tin một danh mục đã có.
     * @param id ID danh mục cần sửa.
     * @param request Thông tin mới của danh mục.
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {
        try {
            CategoryResponse updated = categoryService.updateCategory(id, request);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Cập nhật danh mục thành công!");
            response.put("data", updated);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Ẩn hoặc Khôi phục hoạt động của một danh mục (Toggle status).
     * @param id ID danh mục cần thay đổi trạng thái.
     */
    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<?> toggleCategoryStatus(@PathVariable Long id) {
        try {
            CategoryResponse updated = categoryService.toggleCategoryStatus(id);
            Map<String, Object> response = new HashMap<>();
            String action = updated.isDelete() ? "Đã ẩn danh mục thành công!" : "Đã khôi phục hoạt động danh mục!";
            response.put("message", action);
            response.put("data", updated);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
