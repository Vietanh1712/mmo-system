package com.mmo.shared.dal;

import com.mmo.shared.model.TopupTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TopupTransactionRepository extends JpaRepository<TopupTransaction, Long> {
    Optional<TopupTransaction> findBySepayCode(String sepayCode);
    List<TopupTransaction> findByUserIdAndIsDeleteFalse(Long userId);
    List<TopupTransaction> findAllByIsDeleteFalse();

    @Query("SELECT t FROM TopupTransaction t WHERE (t.isDelete = false OR t.isDelete IS NULL) " +
           "AND (:status IS NULL OR UPPER(t.status) = UPPER(:status)) " +
           "AND (:keywordPattern IS NULL OR LOWER(t.sepayCode) LIKE :keywordPattern OR LOWER(t.transferContent) LIKE :keywordPattern OR (:searchId IS NOT NULL AND t.id = :searchId) OR (:searchUserId IS NOT NULL AND t.userId = :searchUserId))")
    Page<TopupTransaction> searchTopups(
            @Param("status") String status,
            @Param("keywordPattern") String keywordPattern,
            @Param("searchId") Long searchId,
            @Param("searchUserId") Long searchUserId,
            Pageable pageable);

    @Query("SELECT COUNT(t) FROM TopupTransaction t WHERE (t.isDelete = false OR t.isDelete IS NULL) AND UPPER(t.status) = UPPER(:status)")
    long countByStatusIgnoreCase(@Param("status") String status);

    @Query("SELECT COUNT(t) FROM TopupTransaction t WHERE (t.isDelete = false OR t.isDelete IS NULL)")
    long countTotalTopups();
}
