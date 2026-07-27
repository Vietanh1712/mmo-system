package com.mmo.shared.dal;

import com.mmo.shared.model.DigitalAsset;
import com.mmo.shared.model.ProductVariant;
import com.mmo.shared.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;

@Repository
public interface DigitalAssetRepository extends JpaRepository<DigitalAsset, Long> {

    List<DigitalAsset> findByVariantAndIsDeleteFalseOrderByCreatedAtDesc(ProductVariant variant);

    List<DigitalAsset> findByVariantAndIsDeleteFalse(ProductVariant variant);

    List<DigitalAsset> findByVariantAndIsUsedFalseAndIsDeleteFalse(ProductVariant variant);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT a FROM DigitalAsset a
            WHERE a.variant = :variant
              AND a.isUsed = false
              AND a.isDelete = false
            ORDER BY a.createdAt ASC, a.id ASC
            """)
    List<DigitalAsset> findAvailableForUpdate(@Param("variant") ProductVariant variant);

    long countByVariantAndIsUsedFalseAndIsDeleteFalse(ProductVariant variant);

    List<DigitalAsset> findByTransactionAndIsDeleteFalse(Transaction transaction);
}
