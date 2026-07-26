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
    
    @org.springframework.data.jpa.repository.Query("SELECT p FROM PreOrder p WHERE p.product.seller = :seller AND p.isDelete = false ORDER BY p.createdAt DESC")
    List<PreOrder> findBySellerOrderByCreatedAtDesc(@org.springframework.data.repository.query.Param("seller") User seller);

    List<PreOrder> findByProductAndStatusIgnoreCaseAndIsDeleteFalseOrderByCreatedAtAsc(com.mmo.shared.model.Product product, String status);
}
