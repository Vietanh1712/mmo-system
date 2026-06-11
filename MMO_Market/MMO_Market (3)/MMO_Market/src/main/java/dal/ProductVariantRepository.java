package dal;

import model.Product;
import model.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    List<ProductVariant> findByProductAndIsDeleteFalse(Product product);
    Optional<ProductVariant> findByIdAndIsDeleteFalse(Long id);
}
