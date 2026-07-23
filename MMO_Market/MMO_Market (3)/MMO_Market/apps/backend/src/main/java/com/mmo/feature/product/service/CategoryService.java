package com.mmo.feature.product.service;

import com.mmo.shared.dal.CategoryRepository;
import com.mmo.shared.dal.ProductRepository;
import com.mmo.shared.dto.CategoryRequest;
import com.mmo.shared.dto.CategoryResponse;
import com.mmo.shared.model.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    public List<Category> getAllActiveCategories() {
        return categoryRepository.findByIsDeleteFalse();
    }

    public List<Category> getParentCategories() {
        return categoryRepository.findByParentIsNullAndIsDeleteFalse();
    }

    public List<CategoryResponse> searchCategoriesForStaff(String keyword, Long parentId, String type, Boolean isDelete, String sortBy) {
        List<Category> categories = categoryRepository.findAll();

        // 1. Filter by Keyword
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim().toLowerCase();
            categories = categories.stream().filter(c ->
                    (c.getName() != null && c.getName().toLowerCase().contains(kw)) ||
                    (c.getDescription() != null && c.getDescription().toLowerCase().contains(kw))
            ).collect(Collectors.toList());
        }

        // 2. Filter by Parent ID
        if (parentId != null) {
            if (parentId == 0L) {
                categories = categories.stream().filter(c -> c.getParent() == null).collect(Collectors.toList());
            } else {
                categories = categories.stream().filter(c -> c.getParent() != null && parentId.equals(c.getParent().getId())).collect(Collectors.toList());
            }
        }

        // 3. Filter by Type (PARENT vs CHILD)
        if ("PARENT".equalsIgnoreCase(type)) {
            categories = categories.stream().filter(c -> c.getParent() == null).collect(Collectors.toList());
        } else if ("CHILD".equalsIgnoreCase(type)) {
            categories = categories.stream().filter(c -> c.getParent() != null).collect(Collectors.toList());
        }

        // 4. Filter by isDelete
        if (isDelete != null) {
            categories = categories.stream().filter(c -> Boolean.valueOf(isDelete).equals(c.getIsDelete())).collect(Collectors.toList());
        }

        List<CategoryResponse> responses = categories.stream().map(cat -> {
            long pCount = 0;
            try {
                if (cat.getParent() == null) {
                    List<Long> catIds = new java.util.ArrayList<>();
                    catIds.add(cat.getId());
                    if (cat.getSubCategories() != null) {
                        for (Category sub : cat.getSubCategories()) {
                            if (!Boolean.TRUE.equals(sub.getIsDelete())) {
                                catIds.add(sub.getId());
                            }
                        }
                    }
                    pCount = productRepository.countByCategoryIdInAndIsDeleteFalse(catIds);
                } else {
                    pCount = productRepository.countByCategoryIdAndIsDeleteFalse(cat.getId());
                }
            } catch (Exception e) {
                pCount = 0;
            }
            Long pId = cat.getParent() != null ? cat.getParent().getId() : null;
            String parentName = cat.getParent() != null ? cat.getParent().getName() : "—";
            boolean deleted = Boolean.TRUE.equals(cat.getIsDelete());
            return new CategoryResponse(
                    cat.getId(),
                    cat.getName(),
                    pId,
                    parentName,
                    cat.getDescription(),
                    pCount,
                    deleted,
                    cat.getCreatedAt(),
                    cat.getUpdatedAt()
            );
        }).collect(Collectors.toList());

        // 5. Sort responses
        if ("products".equalsIgnoreCase(sortBy)) {
            responses.sort(Comparator.comparingLong(CategoryResponse::getProductCount).reversed());
        } else if ("name".equalsIgnoreCase(sortBy)) {
            responses.sort(Comparator.comparing(CategoryResponse::getName, String.CASE_INSENSITIVE_ORDER));
        } else {
            // default: newest
            responses.sort(Comparator.comparing(CategoryResponse::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        }

        return responses;
    }

    public Map<String, Object> getCategoryStats() {
        Map<String, Object> stats = new HashMap<>();
        List<Category> all = categoryRepository.findAll();
        long totalActive = all.stream().filter(c -> !Boolean.TRUE.equals(c.getIsDelete())).count();
        long totalParents = all.stream().filter(c -> !Boolean.TRUE.equals(c.getIsDelete()) && c.getParent() == null).count();
        long totalChildren = all.stream().filter(c -> !Boolean.TRUE.equals(c.getIsDelete()) && c.getParent() != null).count();
        long totalDeleted = all.stream().filter(c -> Boolean.TRUE.equals(c.getIsDelete())).count();

        stats.put("totalActive", totalActive);
        stats.put("totalParents", totalParents);
        stats.put("totalChildren", totalChildren);
        stats.put("totalDeleted", totalDeleted);
        return stats;
    }

    public CategoryResponse getCategoryById(Long id) {
        Category cat = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục yêu cầu."));

        long pCount = 0;
        if (cat.getParent() == null) {
            java.util.List<Long> targetIds = new java.util.ArrayList<>();
            targetIds.add(cat.getId());
            if (cat.getSubCategories() != null) {
                for (Category sub : cat.getSubCategories()) {
                    if (!Boolean.TRUE.equals(sub.getIsDelete())) {
                        targetIds.add(sub.getId());
                    }
                }
            }
            pCount = productRepository.countByCategoryIdInAndIsDeleteFalse(targetIds);
        } else {
            pCount = productRepository.countByCategoryIdAndIsDeleteFalse(cat.getId());
        }

        Long parentId = cat.getParent() != null ? cat.getParent().getId() : null;
        String parentName = cat.getParent() != null ? cat.getParent().getName() : "—";
        boolean isDelete = Boolean.TRUE.equals(cat.getIsDelete());

        return new CategoryResponse(
                cat.getId(),
                cat.getName(),
                parentId,
                parentName,
                cat.getDescription(),
                pCount,
                isDelete,
                cat.getCreatedAt(),
                cat.getUpdatedAt()
        );
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên danh mục không được để trống.");
        }
        String name = request.getName().trim();
        Long parentId = request.getParentId();

        Category parentCategory = null;
        if (parentId != null && parentId > 0) {
            parentCategory = categoryRepository.findById(parentId)
                    .orElseThrow(() -> new IllegalArgumentException("Danh mục cha không tồn tại."));
        } else if (request.getParentName() != null && !request.getParentName().trim().isEmpty()) {
            String pName = request.getParentName().trim();
            parentCategory = categoryRepository.findByNameAndParentIsNull(pName).orElse(null);
            if (parentCategory == null) {
                parentCategory = new Category();
                parentCategory.setName(pName);
                parentCategory.setParent(null);
                parentCategory.setDescription("Danh mục cha " + pName);
                parentCategory.setIsDelete(false);
                parentCategory.setCreatedAt(LocalDateTime.now());
                parentCategory.setUpdatedAt(LocalDateTime.now());
                parentCategory = categoryRepository.save(parentCategory);
            }
        }

        if (parentCategory != null) {
            if (Boolean.TRUE.equals(parentCategory.getIsDelete())) {
                throw new IllegalArgumentException("Không thể chọn danh mục cha đã bị ẩn.");
            }
            if (categoryRepository.existsByNameAndParent_Id(name, parentCategory.getId())) {
                throw new IllegalArgumentException("Danh mục với tên này đã tồn tại trong cùng danh mục cha.");
            }
        } else {
            if (categoryRepository.existsByNameAndParentIsNull(name)) {
                throw new IllegalArgumentException("Danh mục với tên này đã tồn tại ở cấp gốc.");
            }
        }

        Category category = new Category();
        category.setName(name);
        category.setParent(parentCategory);
        category.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        category.setIsDelete(false);
        category.setCreatedAt(LocalDateTime.now());
        category.setUpdatedAt(LocalDateTime.now());

        Category saved = categoryRepository.save(category);

        Long savedParentId = saved.getParent() != null ? saved.getParent().getId() : null;
        String parentName = saved.getParent() != null ? saved.getParent().getName() : "—";

        return new CategoryResponse(
                saved.getId(),
                saved.getName(),
                savedParentId,
                parentName,
                saved.getDescription(),
                0,
                false,
                saved.getCreatedAt(),
                saved.getUpdatedAt()
        );
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục yêu cầu."));

        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên danh mục không được để trống.");
        }
        String name = request.getName().trim();
        Long parentId = request.getParentId();

        Category parentCategory = null;
        if (parentId != null && parentId > 0) {
            if (parentId.equals(id)) {
                throw new IllegalArgumentException("Danh mục không thể tự làm danh mục cha của chính nó.");
            }
            parentCategory = categoryRepository.findById(parentId).orElse(null);
        } else if (request.getParentName() != null && !request.getParentName().trim().isEmpty()) {
            String pName = request.getParentName().trim();
            parentCategory = categoryRepository.findByNameAndParentIsNull(pName).orElse(null);
            if (parentCategory == null) {
                parentCategory = new Category();
                parentCategory.setName(pName);
                parentCategory.setParent(null);
                parentCategory.setDescription("Danh mục cha " + pName);
                parentCategory.setIsDelete(false);
                parentCategory.setCreatedAt(LocalDateTime.now());
                parentCategory.setUpdatedAt(LocalDateTime.now());
                parentCategory = categoryRepository.save(parentCategory);
            }
        }

        if (parentCategory != null) {
            if (parentCategory.getId().equals(id)) {
                throw new IllegalArgumentException("Danh mục không thể tự làm danh mục cha của chính nó.");
            }
            if (Boolean.TRUE.equals(parentCategory.getIsDelete())) {
                throw new IllegalArgumentException("Không thể chọn danh mục cha đã bị ẩn.");
            }
            if (categoryRepository.existsByNameAndParent_IdAndIdNot(name, parentCategory.getId(), id)) {
                throw new IllegalArgumentException("Tên danh mục đã trùng với một danh mục khác trong cùng danh mục cha.");
            }
        } else {
            if (categoryRepository.existsByNameAndParentIsNullAndIdNot(name, id)) {
                throw new IllegalArgumentException("Tên danh mục đã trùng với một danh mục gốc khác.");
            }
        }

        category.setName(name);
        category.setParent(parentCategory);
        category.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        category.setUpdatedAt(LocalDateTime.now());

        Category updated = categoryRepository.save(category);
        long pCount = 0;
        try {
            pCount = productRepository.countByCategoryIdAndIsDeleteFalse(updated.getId());
        } catch (Exception e) {
            pCount = 0;
        }

        Long updatedParentId = updated.getParent() != null ? updated.getParent().getId() : null;
        String parentName = updated.getParent() != null ? updated.getParent().getName() : "—";
        boolean deleted = Boolean.TRUE.equals(updated.getIsDelete());

        return new CategoryResponse(
                updated.getId(),
                updated.getName(),
                updatedParentId,
                parentName,
                updated.getDescription(),
                pCount,
                deleted,
                updated.getCreatedAt(),
                updated.getUpdatedAt()
        );
    }

    @Transactional
    public CategoryResponse toggleCategoryStatus(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục."));

        boolean newStatus = !Boolean.TRUE.equals(category.getIsDelete());
        category.setIsDelete(newStatus);
        category.setUpdatedAt(LocalDateTime.now());

        Category updated = categoryRepository.save(category);
        long pCount = 0;
        try {
            pCount = productRepository.countByCategoryIdAndIsDeleteFalse(updated.getId());
        } catch (Exception e) {
            pCount = 0;
        }

        Long updatedParentId = updated.getParent() != null ? updated.getParent().getId() : null;
        String parentName = updated.getParent() != null ? updated.getParent().getName() : "—";

        return new CategoryResponse(
                updated.getId(),
                updated.getName(),
                updatedParentId,
                parentName,
                updated.getDescription(),
                pCount,
                newStatus,
                updated.getCreatedAt(),
                updated.getUpdatedAt()
        );
    }
}
