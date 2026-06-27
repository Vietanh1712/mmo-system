package com.mmo.shared.dal;

import com.mmo.shared.model.SellerRegistration;
import com.mmo.shared.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SellerRegistrationRepository extends JpaRepository<SellerRegistration, Long> {
    Optional<SellerRegistration> findByUserAndIsDeleteFalse(User user);
    List<SellerRegistration> findAllByIsDeleteFalseOrderByCreatedAtDesc();
    long countByStatusAndIsDeleteFalse(String status);
}
