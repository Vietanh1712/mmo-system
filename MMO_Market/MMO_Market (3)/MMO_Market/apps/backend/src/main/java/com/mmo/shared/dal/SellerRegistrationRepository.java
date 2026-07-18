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
    List<SellerRegistration> findAllByIsDeleteFalseOrderByCreatedAtDesc();
    long countByStatusAndIsDeleteFalse(String status);

    long countByIsDeleteFalse();
    long countByStatusIgnoreCaseAndIsDeleteFalse(String status);

    @Query("SELECT r FROM SellerRegistration r WHERE r.isDelete = false " +
           "AND (:status IS NULL OR UPPER(r.status) = UPPER(:status)) " +
           "AND (:shopStatus IS NULL OR UPPER(r.user.shopStatus) = UPPER(:shopStatus)) " +
           "AND (:keyword IS NULL OR " +
           "    LOWER(r.shopName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "    LOWER(r.supportEmail) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "    LOWER(r.supportPhone) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "    LOWER(r.category) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "    (:searchId IS NOT NULL AND r.id = :searchId))")
    Page<SellerRegistration> searchRegistrations(
            @Param("status") String status,
            @Param("shopStatus") String shopStatus,
            @Param("keyword") String keyword,
            @Param("searchId") Long searchId,
            Pageable pageable);

    @Query("SELECT DISTINCT r.status FROM SellerRegistration r WHERE r.status IS NOT NULL AND r.isDelete = false")
    List<String> findDistinctStatuses();
}
