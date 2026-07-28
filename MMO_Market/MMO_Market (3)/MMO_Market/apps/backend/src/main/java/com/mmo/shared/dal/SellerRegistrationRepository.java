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

@Repository
public interface SellerRegistrationRepository extends JpaRepository<SellerRegistration, Long> {
    Optional<SellerRegistration> findByUserAndIsDeleteFalse(User user);
    Optional<SellerRegistration> findFirstByUserAndIsDeleteFalseOrderByIdDesc(User user);
    List<SellerRegistration> findAllByIsDeleteFalseOrderByCreatedAtDesc();
    long countByStatusAndIsDeleteFalse(String status);

    long countByIsDeleteFalse();
    long countByStatusIgnoreCaseAndIsDeleteFalse(String status);

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

    @Query("SELECT DISTINCT r.status FROM SellerRegistration r WHERE r.status IS NOT NULL AND r.isDelete = false")
    List<String> findDistinctStatuses();
}
