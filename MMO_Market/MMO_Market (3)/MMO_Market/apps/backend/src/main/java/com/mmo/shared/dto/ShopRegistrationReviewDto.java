package com.mmo.shared.dto;

import lombok.Data;

/**
 * DTO chứa thông tin phê duyệt hoặc từ chối đơn đăng ký Shop của Staff/Admin.
 */
@Data
public class ShopRegistrationReviewDto {
    
    /**
     * Xác định đơn đăng ký có được phê duyệt (true) hay bị từ chối (false).
     */
    private boolean approved;

    /**
     * Lý do phê duyệt hoặc lý do từ chối (nếu từ chối thì bắt buộc điền).
     */
    private String reason;
}
