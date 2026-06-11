package dal;

import model.DigitalAsset;
import model.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DigitalAssetRepository extends JpaRepository<DigitalAsset, Long> {

    List<DigitalAsset> findByVariantAndIsDeleteFalseOrderByCreatedAtDesc(ProductVariant variant);

    List<DigitalAsset> findByVariantAndIsDeleteFalse(ProductVariant variant);

    List<DigitalAsset> findByVariantAndIsUsedFalseAndIsDeleteFalse(ProductVariant variant);

    long countByVariantAndIsUsedFalseAndIsDeleteFalse(ProductVariant variant);
}
