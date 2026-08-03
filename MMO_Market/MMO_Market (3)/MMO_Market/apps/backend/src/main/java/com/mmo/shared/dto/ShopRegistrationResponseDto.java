package com.mmo.shared.dto;
import com.mmo.shared.model.Category;

import lombok.Builder;
import lombok.Data;

/**
 * DTO chứa thông tin phản hồi chi tiết về trạng thái và hồ sơ đăng ký Shop.
 */
@Data
@Builder
public class ShopRegistrationResponseDto {
    
    /**
     * ID của đơn đăng ký mở Shop.
     */
    private Long id;

    /**
     * Trạng thái phê duyệt của đơn (APPROVED, PENDING, REJECTED, v.v.).
     */
    private String status;

    /**
     * Mã số định danh đơn đăng ký (ví dụ: SHOP-123).
     */
    private String code;

    /**
     * Thời gian nộp đơn đăng ký.
     */
    private String submittedAt;

    /**
     * Tên Shop đăng ký.
     */
    private String shopName;

    /**
     * Lĩnh vực danh mục kinh doanh đăng ký.
     */
    private String category;

    /**
     * Mô tả giới thiệu của Shop.
     */
    private String description;

    /**
     * Email nhận liên hệ của Shop.
     */
    private String supportEmail;

    /**
     * Số điện thoại hỗ trợ của Shop.
     */
    private String supportPhone;

    /**
     * Lý do từ chối đơn đăng ký (nếu bị từ chối).
     */
    private String rejectionReason;

    /**
     * Trạng thái hoạt động của Shop (Active, Suspended, Locked, Banned...).
     */
    private String shopStatus;

    /**
     * Thời điểm kết thúc hình phạt đình chỉ tạm thời (nếu có).
     */
    private String suspendedUntil;

    /**
     * Số tiền đặt cọc của người bán (nếu có).
     */
    private Long depositVnd;

    /**
     * Số dư tài khoản hiện tại của người bán trong hệ thống.
     */
    private Long balanceVnd;

    /**
     * Họ tên đầy đủ của chủ gian hàng.
     */
    private String ownerName;

    /**
     * Số tài khoản ngân hàng liên kết nhận tiền.
     */
    private String bankAccountNumber;

    /**
     * Tên ngân hàng liên kết nhận tiền.
     */
    private String bankName;

    /**
     * Chi nhánh của ngân hàng liên kết.
     */
    private String bankBranch;
}

