package com.mmo.shared.dal;

import com.mmo.shared.model.WalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    Page<WalletTransaction> findByUser_IdAndIsDeleteFalseOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    List<WalletTransaction> findByUser_IdAndIsDeleteFalse(Long userId);
}
