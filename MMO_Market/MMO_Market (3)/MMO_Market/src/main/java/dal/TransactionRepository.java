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
    /**
     * Đếm số lượt bán THỰC TẾ (đơn Completed và Held) của một sản phẩm.
     * Không đếm đơn Pending / Cancelled / Refunded / Disputed.
     */
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.product.id = :productId AND t.status IN ('Completed', 'Held') AND t.isDelete = false")
    Long countByProductIdAndIsDeleteFalse(@Param("productId") Long productId);

    List<Transaction> findBySellerAndIsDeleteFalseOrderByCreatedAtDesc(User seller);
    List<Transaction> findByCustomerAndIsDeleteFalseOrderByCreatedAtDesc(User customer);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.seller = :seller AND t.status = 'Completed' AND t.isDelete = false")
    long countCompletedSalesBySeller(@Param("seller") User seller);

    @Query("SELECT SUM(t.amountVnd - t.commissionVnd) FROM Transaction t WHERE t.seller = :seller AND t.status = 'Completed' AND t.isDelete = false")
    Long sumCompletedEarningsBySeller(@Param("seller") User seller);

    /**
     * Kiểm tra xem customer đã có giao dịch HOÀN THÀNH (Completed) hoặc GIỮ TIỀN (Held) cho sản phẩm này chưa.
     * Dùng để validate trước khi cho phép đánh giá.
     */
    @Query("SELECT COUNT(t) > 0 FROM Transaction t WHERE t.customer.id = :customerId AND t.product.id = :productId AND t.status IN ('Completed', 'Held') AND t.isDelete = false")
    boolean existsCompletedPurchaseByCustomerAndProduct(@Param("customerId") Long customerId, @Param("productId") Long productId);

    @Query("SELECT COALESCE(SUM(t.commissionVnd), 0) FROM Transaction t WHERE t.status IN ('Completed', 'Held') AND t.isDelete = false")
    long sumCommissionForCompletedOrHeldTransactions();

    @Query("SELECT t FROM Transaction t JOIN FETCH t.customer WHERE t.isDelete = false")
    List<Transaction> findAllWithCustomerByIsDeleteFalse();
}
