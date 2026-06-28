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

    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u WHERE u.isDelete = false AND (LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<User> searchUsers(@org.springframework.data.repository.query.Param("keyword") String keyword);
}
