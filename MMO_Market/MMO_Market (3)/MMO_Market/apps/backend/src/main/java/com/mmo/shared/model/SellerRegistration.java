package com.mmo.shared.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Thực thể lưu trữ thông tin đăng ký mở gian hàng (Shop) của người dùng.
 * Quản lý trạng thái phê duyệt từ Ban quản trị (Staff/Admin) và các thông tin liên hệ của Shop.
 */
@Entity
@Table(name = "SellerRegistrations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerRegistration {
    
    /**
     * ID tự tăng của yêu cầu đăng ký mở Shop.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Người dùng gửi đơn đăng ký mở Shop.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Tên hiển thị của Shop trên sàn thương mại điện tử.
     */
    @Column(name = "shop_name", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String shopName;

    /**
     * Mô tả giới thiệu về Shop hoặc các mặt hàng kinh doanh.
     */
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String description;

    /**
     * Lĩnh vực/Danh mục sản phẩm chính mà Shop đăng ký bán (ví dụ: Game, Tool, Account...).
     */
    @Column(columnDefinition = "NVARCHAR(100)")
    private String category;

    /**
     * Email liên hệ hỗ trợ khách hàng của Shop.
     */
    @Column(name = "support_email")
    private String supportEmail;

    /**
     * Số điện thoại liên hệ hỗ trợ khách hàng của Shop.
     */
    @Column(name = "support_phone", length = 20)
    private String supportPhone;

    /**
     * Đường dẫn hoặc nội dung hợp đồng mở gian hàng mẫu.
     */
    @Column
    private String contract;

    /**
     * Đường dẫn tệp hợp đồng đã được người bán ký kết và tải lên.
     */
    @Column(name = "signed_contract")
    private String signedContract;

    /**
     * Trạng thái phê duyệt đơn đăng ký.
     * Các giá trị hợp lệ: "Pending" (Chờ duyệt), "Approved" (Đã duyệt), "Rejected" (Từ chối).
     */
    @Column(length = 20)
    private String status = "Pending";

    /**
     * Lý do từ chối đơn đăng ký (ghi nhận bởi Staff/Admin nếu từ chối).
     */
    @Column(name = "rejection_reason", columnDefinition = "NVARCHAR(MAX)")
    private String rejectionReason;

    /**
     * Phí đăng ký mở Shop hoặc phí duy trì gian hàng (nếu có).
     */
    @Column(name = "fee_vnd")
    private Long feeVnd;

    /**
     * Thời gian gửi đơn đăng ký mở Shop.
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * Đánh dấu bản ghi đã bị xóa logic hay chưa (soft delete).
     */
    @Column(name = "isDelete")
    private Boolean isDelete = false;

    /**
     * Tự động khởi tạo thời gian tạo và trạng thái mặc định trước khi lưu vào DB.
     */
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = "Pending";
        }
        if (isDelete == null) {
            isDelete = false;
        }
    }
}
