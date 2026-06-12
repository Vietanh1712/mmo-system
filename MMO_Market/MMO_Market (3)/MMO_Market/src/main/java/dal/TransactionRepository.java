package dal;

import model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.product.id = :productId AND t.isDelete = false")
    Long countByProductIdAndIsDeleteFalse(@Param("productId") Long productId);
}
