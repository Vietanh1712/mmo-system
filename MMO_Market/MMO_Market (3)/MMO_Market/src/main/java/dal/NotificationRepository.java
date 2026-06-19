package dal;

import model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("SELECT n FROM Notification n WHERE n.isDelete = false AND " +
           "n.userId IN (SELECT u.id FROM User u WHERE LOWER(u.role) LIKE '%admin%' OR LOWER(u.role) LIKE '%staff%') AND " +
           "LOWER(n.type) IN ('info', 'warning', 'maintenance', 'policy') AND " +
           "(:type IS NULL OR :type = '' OR n.type = :type) AND " +
           "(:search IS NULL OR :search = '' OR LOWER(n.title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(n.content) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY n.createdAt DESC")
    Page<Notification> searchNotifications(
            @Param("type") String type,
            @Param("search") String search,
            Pageable pageable);

    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.isDelete = false AND " +
           "LOWER(n.type) NOT IN ('info', 'warning', 'maintenance', 'policy') " +
           "ORDER BY n.createdAt DESC")
    List<Notification> findAllByUserIdAndIsDeleteFalseOrderByCreatedAtDesc(@Param("userId") Long userId);

    @Query("SELECT n FROM Notification n WHERE n.isDelete = false AND " +
           "n.userId IN (SELECT u.id FROM User u WHERE LOWER(u.role) LIKE '%admin%' OR LOWER(u.role) LIKE '%staff%') AND " +
           "LOWER(n.type) IN ('info', 'warning', 'maintenance', 'policy') " +
           "ORDER BY n.createdAt DESC")
    List<Notification> findAllBroadcastNotifications();
}
