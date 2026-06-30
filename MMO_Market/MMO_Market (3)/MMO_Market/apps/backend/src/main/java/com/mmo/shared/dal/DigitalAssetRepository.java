package com.mmo.shared.dal;

import com.mmo.shared.model.DigitalAsset;
import com.mmo.shared.model.ProductVariant;
import com.mmo.shared.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DigitalAssetRepository extends JpaRepository<DigitalAsset, Long> {

    List<DigitalAsset> findByVariantAndIsDeleteFalseOrderByCreatedAtDesc(ProductVariant variant);

    List<DigitalAsset> findByVariantAndIsDeleteFalse(ProductVariant variant);

    List<DigitalAsset> findByVariantAndIsUsedFalseAndIsDeleteFalse(ProductVariant variant);

    long countByVariantAndIsUsedFalseAndIsDeleteFalse(ProductVariant variant);

    java.util.Optional<DigitalAsset> findByTransactionAndIsDeleteFalse(Transaction transaction);
}
