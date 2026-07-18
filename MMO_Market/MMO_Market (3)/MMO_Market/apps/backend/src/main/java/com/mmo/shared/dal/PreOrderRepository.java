package com.mmo.shared.dal;

import com.mmo.shared.model.PreOrder;
import com.mmo.shared.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PreOrderRepository extends JpaRepository<PreOrder, Long> {
    List<PreOrder> findByCustomerAndIsDeleteFalse(User customer);
    List<PreOrder> findByCustomerAndIsDeleteFalseOrderByCreatedAtDesc(User customer);
}
