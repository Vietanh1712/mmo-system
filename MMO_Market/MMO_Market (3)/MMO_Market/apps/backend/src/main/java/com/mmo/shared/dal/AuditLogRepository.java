package com.mmo.shared.dal;
import com.mmo.shared.model.User;

import com.mmo.shared.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findAllByOrderByCreatedAtDesc();

    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:action IS NULL OR :action = '' OR " +
           "a.action = :action OR " +
           "(:action = 'Lock_User' AND LOWER(a.action) IN ('lock_user', 'unlock_user', 'create_staff', 'update_staff', 'delete_staff', 'soft_delete_user', 'update_role', 'change_user_role')) OR " +
           "(:action = 'Config_Update' AND LOWER(a.action) IN ('update_system_config', 'update_commissions_config', 'config_update'))) AND " +
           "(:search IS NULL OR :search = '' OR LOWER(a.details) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "EXISTS (SELECT u FROM User u WHERE u.id = a.userId AND (LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%'))))) " +
           "ORDER BY a.createdAt DESC")
    Page<AuditLog> searchLogs(
            @Param("action") String action,
            @Param("search") String search,
            Pageable pageable);
}
