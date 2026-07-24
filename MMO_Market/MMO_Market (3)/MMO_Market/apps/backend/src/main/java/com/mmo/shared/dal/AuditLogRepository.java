package com.mmo.shared.dal;

import com.mmo.shared.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findAllByOrderByCreatedAtDesc();

    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:action IS NULL OR :action = '' OR a.action = :action) AND " +
           "(:category IS NULL OR :category = '' OR :category = 'ALL' OR " +
           " (:category = 'FINANCE' AND a.action IN ('Fund_Withdraw', 'Withdrawal_Reject')) OR " +
           " (:category = 'COMPLAINT' AND a.action IN ('Complaint_Resolve', 'Dispute_Start')) OR " +
           " (:category = 'SHOP' AND a.action IN ('Shop_Approve', 'Shop_Reject')) OR " +
           " (:category = 'USER_MGMT' AND a.action IN ('KYC_Approve', 'Lock_User', 'Unlock_User', 'Role_Update', 'Perm_Update')) OR " +
           " (:category = 'SUPPORT' AND a.action IN ('Support_Resolve')) OR " +
           " (:category = 'SYSTEM' AND a.action IN ('Config_Update', 'Maintenance_Toggle', 'Notification_Create', 'Notification_Delete'))) AND " +
           "(:startDate IS NULL OR a.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR a.createdAt <= :endDate) AND " +
           "(:search IS NULL OR :search = '' OR LOWER(a.details) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "EXISTS (SELECT u FROM User u WHERE u.id = a.userId AND (LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')))))")
    Page<AuditLog> searchLogs(
            @Param("category") String category,
            @Param("action") String action,
            @Param("search") String search,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);
}
