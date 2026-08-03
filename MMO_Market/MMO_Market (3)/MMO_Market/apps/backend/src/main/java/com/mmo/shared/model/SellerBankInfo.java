package com.mmo.shared.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Thực thể lưu trữ thông tin tài khoản ngân hàng của Người bán (Seller).
 * Dùng để thực hiện các yêu cầu rút tiền từ ví số dư của Shop về tài khoản ngân hàng thực tế.
 */
@Entity
@Table(name = "SellerBankInfo")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerBankInfo {
    
    /**
     * ID tự tăng của bản ghi thông tin ngân hàng.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Người bán liên kết với thông tin tài khoản ngân hàng này.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Tên ngân hàng (ví dụ: Vietcombank, Techcombank, MB Bank...).
     */
    @Column(name = "bank_name", nullable = false, columnDefinition = "NVARCHAR(100)")
    private String bankName;

    /**
     * Số tài khoản ngân hàng dùng để nhận tiền.
     */
    @Column(name = "account_number", nullable = false, length = 50)
    private String accountNumber;

    /**
     * Tên chi nhánh mở tài khoản ngân hàng (nếu có).
     */
    @Column(columnDefinition = "NVARCHAR(100)")
    private String branch;

    /**
     * Thời gian thông tin ngân hàng được thêm hoặc cập nhật.
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
        if (isDelete == null) {
            isDelete = false;
        }
    }
}
