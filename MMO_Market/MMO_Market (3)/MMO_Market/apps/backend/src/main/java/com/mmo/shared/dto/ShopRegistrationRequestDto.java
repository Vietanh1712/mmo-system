package com.mmo.shared.dto;
import com.mmo.shared.model.Category;

import lombok.Data;

/**
 * DTO chứa thông tin yêu cầu đăng ký mở Shop gửi từ người dùng (Customer).
 */
@Data
public class ShopRegistrationRequestDto {
    
    /**
     * Tên Shop muốn đăng ký hiển thị.
     */
    private String shopName;

    /**
     * Mô tả thông tin giới thiệu về Shop.
     */
    private String description;

    /**
     * Danh mục/mặt hàng kinh doanh chính của Shop.
     */
    private String category;

    /**
     * Email nhận phản hồi/hỗ trợ khách hàng của Shop.
     */
    private String supportEmail;

    /**
     * Số điện thoại hỗ trợ khách hàng của Shop.
     */
    private String supportPhone;
}
