package com.mmo.shared.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CategoryRequest {

    @NotBlank(message = "Tên danh mục không được để trống")
    @Size(max = 100, message = "Tên danh mục tối đa 100 ký tự")
    private String name;

    private Long parentId;

    private String parentName;

    @Size(max = 500, message = "Mô tả tối đa 500 ký tự")
    private String description;

    public CategoryRequest() {}

    public CategoryRequest(String name, Long parentId, String description) {
        this.name = name;
        this.parentId = parentId;
        this.description = description;
    }

    public CategoryRequest(String name, Long parentId, String parentName, String description) {
        this.name = name;
        this.parentId = parentId;
        this.parentName = parentName;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getParentName() {
        return parentName;
    }

    public void setParentName(String parentName) {
        this.parentName = parentName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
