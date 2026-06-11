package dal;

import model.Transaction;
import model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findBySellerAndIsDeleteFalseOrderByCreatedAtDesc(User seller);
    List<Transaction> findByCustomerAndIsDeleteFalseOrderByCreatedAtDesc(User customer);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.seller = :seller AND t.status = 'Completed' AND t.isDelete = false")
    long countCompletedSalesBySeller(@Param("seller") User seller);

    @Query("SELECT SUM(t.amountVnd - t.commissionVnd) FROM Transaction t WHERE t.seller = :seller AND t.status = 'Completed' AND t.isDelete = false")
    Long sumCompletedEarningsBySeller(@Param("seller") User seller);
}
