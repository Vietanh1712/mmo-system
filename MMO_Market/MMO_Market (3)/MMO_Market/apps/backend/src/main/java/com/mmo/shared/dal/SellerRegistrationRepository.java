package com.mmo.shared.dal;

import com.mmo.shared.model.SellerRegistration;
import com.mmo.shared.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository quản lý dữ liệu Đăng ký mở Shop (SellerRegistration).
 * Cung cấp các thao tác CRUD và các truy vấn tùy chỉnh để tìm kiếm, thống kê yêu cầu mở Shop.
 */
@Repository
public interface SellerRegistrationRepository extends JpaRepository<SellerRegistration, Long> {
    
    /**
     * Tìm thông tin đăng ký Shop chưa bị xóa của một người dùng.
     */
    Optional<SellerRegistration> findByUserAndIsDeleteFalse(User user);
    
    /**
     * Tìm thông tin đăng ký Shop mới nhất chưa bị xóa của một người dùng.
     */
    Optional<SellerRegistration> findFirstByUserAndIsDeleteFalseOrderByIdDesc(User user);
    
    /**
     * Lấy toàn bộ danh sách đăng ký mở Shop chưa bị xóa, sắp xếp theo thời gian tạo giảm dần.
     */
    List<SellerRegistration> findAllByIsDeleteFalseOrderByCreatedAtDesc();
    
    /**
     * Đếm số lượng đơn đăng ký có trạng thái cụ thể và chưa bị xóa.
     */
    long countByStatusAndIsDeleteFalse(String status);

    /**
     * Đếm tổng số lượng đơn đăng ký chưa bị xóa.
     */
    long countByIsDeleteFalse();
    
    /**
     * Đếm số lượng đơn đăng ký có trạng thái cụ thể (không phân biệt hoa thường) và chưa bị xóa.
     */
    long countByStatusIgnoreCaseAndIsDeleteFalse(String status);

    /**
     * Tìm kiếm nâng cao các đơn đăng ký mở Shop dựa theo trạng thái duyệt đơn, trạng thái hoạt động của Shop, và từ khóa.
     * Hỗ trợ tìm kiếm theo tên shop, email, số điện thoại, danh mục, mã đăng ký,...
     *
     * @param status Trạng thái đơn đăng ký (APPROVED, PENDING, REJECTED)
     * @param shopStatus Trạng thái hoạt động của Shop (ACTIVE, SUSPENDED, LOCKED, Banned...)
     * @param keyword Từ khóa tìm kiếm
     * @param pageable Tham số phân trang và sắp xếp
     */
    @Query("SELECT r FROM SellerRegistration r WHERE r.isDelete = false " +
           "AND (:status IS NULL OR UPPER(r.status) = UPPER(:status)) " +
           "AND (:shopStatus IS NULL OR " +
           "     (:shopStatus = 'ACTIVE' AND (r.user.shopStatus IS NULL OR UPPER(r.user.shopStatus) IN ('ACTIVE', 'APPROVED'))) OR " +
           "     (:shopStatus = 'SUSPENDED' AND UPPER(r.user.shopStatus) IN ('SUSPENDED', 'TEMP_LOCKED', 'TEMP_SUSPENDED')) OR " +
           "     (:shopStatus = 'LOCKED' AND UPPER(r.user.shopStatus) IN ('LOCKED', 'INDEFINITE_LOCKED')) OR " +
           "     (:shopStatus = 'Banned' AND UPPER(r.user.shopStatus) IN ('BANNED', 'PERMANENT_BANNED')) OR " +
           "     (:shopStatus = 'WITHDRAWN' AND UPPER(r.user.shopStatus) IN ('WITHDRAWN', 'DELETED')) OR " +
           "     (UPPER(r.user.shopStatus) = UPPER(:shopStatus))) " +
           "AND (:keyword IS NULL OR " +
           "    LOWER(r.shopName) LIKE CONCAT('%', LOWER(:keyword), '%') OR " +
           "    LOWER(r.supportEmail) LIKE CONCAT('%', LOWER(:keyword), '%') OR " +
           "    LOWER(r.supportPhone) LIKE CONCAT('%', LOWER(:keyword), '%') OR " +
           "    LOWER(r.category) LIKE CONCAT('%', LOWER(:keyword), '%') OR " +
           "    CAST(r.id AS string) LIKE CONCAT('%', LOWER(:keyword), '%') OR " +
           "    LOWER(CONCAT('SHOP-', CAST(r.id AS string))) LIKE CONCAT('%', LOWER(:keyword), '%') OR " +
           "    LOWER(r.user.fullName) LIKE CONCAT('%', LOWER(:keyword), '%') OR " +
           "    LOWER(r.user.email) LIKE CONCAT('%', LOWER(:keyword), '%'))")
     Page<SellerRegistration> searchRegistrations(
            @Param("status") String status,
            @Param("shopStatus") String shopStatus,
            @Param("keyword") String keyword,
            Pageable pageable);

    /**
     * Lấy danh sách các trạng thái duyệt đơn đăng ký duy nhất đang tồn tại trong hệ thống.
     */
    @Query("SELECT DISTINCT r.status FROM SellerRegistration r WHERE r.status IS NOT NULL AND r.isDelete = false")
    List<String> findDistinctStatuses();
}
