package com.mmo.shared.dal;

import com.mmo.shared.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailAndIsDeleteFalse(String email);
    Optional<User> findByIdAndIsDeleteFalse(Long id);
    
    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u LEFT JOIN FETCH u.userPermissions WHERE u.id = :id AND u.isDelete = false")
    Optional<User> findByIdWithPermissions(@org.springframework.data.repository.query.Param("id") Long id);
    
    List<User> findAllByIsDeleteFalseOrderByCreatedAtDesc();
    
    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.userPermissions WHERE u.isDelete = false ORDER BY u.createdAt DESC")
    List<User> findAllWithPermissionsByIsDeleteFalseOrderByCreatedAtDesc();
    
    Boolean existsByEmail(String email);
    Boolean existsByEmailAndIsDeleteFalse(String email);
    long countByIsDeleteFalse();

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(u) FROM User u WHERE u.isDelete = false AND u.shopStatus IS NOT NULL AND UPPER(u.shopStatus) NOT IN ('PENDING', 'REJECTED')")
    long countTotalShops();

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(u) FROM User u WHERE u.isDelete = false AND (UPPER(u.shopStatus) = 'ACTIVE' OR UPPER(u.shopStatus) = 'APPROVED')")
    long countActiveShops();

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(u) FROM User u WHERE u.isDelete = false AND (UPPER(u.shopStatus) = 'BANNED' OR UPPER(u.shopStatus) = 'LOCKED')")
    long countBannedShops();

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(u) FROM User u WHERE u.isDelete = false AND (UPPER(u.shopStatus) = 'BANNED' OR UPPER(u.shopStatus) = 'PERMANENT_BANNED')")
    long countPermanentBannedShops();

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(u) FROM User u WHERE u.isDelete = false AND (UPPER(u.shopStatus) = 'LOCKED' OR UPPER(u.shopStatus) = 'INDEFINITE_LOCKED')")
    long countIndefiniteLockedShops();

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(u) FROM User u WHERE u.isDelete = false AND (UPPER(u.shopStatus) = 'SUSPENDED' OR UPPER(u.shopStatus) = 'TEMP_LOCKED')")
    long countTemporarySuspendedShops();

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(u) FROM User u WHERE u.isDelete = false AND (UPPER(u.shopStatus) = 'WITHDRAWN' OR UPPER(u.shopStatus) = 'DELETED')")
    long countWithdrawnShops();
    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(u.depositVnd), 0) FROM User u WHERE u.isDelete = false")
    long sumTotalDeposit();

    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u WHERE u.isDelete = false AND (LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<User> searchUsers(@org.springframework.data.repository.query.Param("keyword") String keyword);

    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u WHERE u.isDelete = false AND (LOWER(u.role) LIKE '%admin%' OR LOWER(u.role) LIKE '%staff%')")
    List<User> findStaffAndAdmins();

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT u.shopStatus FROM User u WHERE u.shopStatus IS NOT NULL AND u.isDelete = false")
    List<String> findDistinctShopStatuses();
}
