package com.mmo.shared.dal;
import com.mmo.shared.model.User;

import com.mmo.shared.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("SELECT n FROM Notification n WHERE n.isDelete = false AND " +
           "n.userId IN (SELECT u.id FROM User u WHERE LOWER(u.role) LIKE '%admin%' OR LOWER(u.role) LIKE '%staff%') AND " +
           "LOWER(n.type) IN ('info', 'warning', 'maintenance', 'policy') AND " +
           "(:type IS NULL OR :type = '' OR n.type = :type) AND " +
           "(:status IS NULL OR :status = '' OR :status = 'ALL' OR UPPER(n.status) = UPPER(:status)) AND " +
           "(:startDate IS NULL OR n.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR n.createdAt <= :endDate) AND " +
           "(:search IS NULL OR :search = '' OR LOWER(n.title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(n.content) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "EXISTS (SELECT u FROM User u WHERE u.id = n.userId AND (LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')))))")
    Page<Notification> searchNotifications(
            @Param("type") String type,
            @Param("status") String status,
            @Param("search") String search,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.isDelete = false AND " +
           "LOWER(n.type) NOT IN ('info', 'warning', 'maintenance', 'policy') " +
           "ORDER BY n.createdAt DESC")
    List<Notification> findAllByUserIdAndIsDeleteFalseOrderByCreatedAtDesc(@Param("userId") Long userId);

    @Query("SELECT n FROM Notification n WHERE n.isDelete = false AND " +
           "(n.status IS NULL OR UPPER(n.status) = 'PUBLISHED') AND " +
           "n.userId IN (SELECT u.id FROM User u WHERE LOWER(u.role) LIKE '%admin%' OR LOWER(u.role) LIKE '%staff%') AND " +
           "LOWER(n.type) IN ('info', 'warning', 'maintenance', 'policy') " +
           "ORDER BY n.createdAt DESC")
    List<Notification> findAllBroadcastNotifications();
}
