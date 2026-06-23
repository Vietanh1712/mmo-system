package dal;

import model.Transaction;
import model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    /**
     * Đếm số lượt bán THỰC TẾ (chỉ đơn Completed) của một sản phẩm.
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

    @Query("SELECT COALESCE(SUM(t.commissionVnd), 0) FROM Transaction t WHERE t.status = 'Completed' AND t.isDelete = false")
    Long sumPlatformRevenue();

    long countByStatusAndIsDeleteFalse(String status);

    long countByIsDeleteFalse();



    @Query("""
SELECT t
FROM Transaction t
WHERE t.isDelete = false

AND (
    (:id IS NOT NULL AND t.id = :id)

    OR

    (:id IS NULL AND (
        :keyword IS NULL

        OR LOWER(t.customer.email)
        LIKE LOWER(CONCAT('%', :keyword, '%'))

        OR LOWER(t.customer.fullName)
        LIKE LOWER(CONCAT('%', :keyword, '%'))

        OR LOWER(t.product.name)
        LIKE LOWER(CONCAT('%', :keyword, '%'))
    ))
)

AND (
    :type IS NULL
    OR LOWER(TRIM(t.product.productType))
       = LOWER(TRIM(:type))
)

AND (
    :status IS NULL
    OR LOWER(t.status)=LOWER(:status)
)

AND (
    :fromDate IS NULL
    OR t.createdAt >= :fromDate
)

AND (
    :toDate IS NULL
    OR t.createdAt <= :toDate
)

""")
    Page<Transaction> searchTransactions(
            @Param("keyword") String keyword,
            @Param("id") Long id,
            @Param("type") String type,
            @Param("status") String status,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );
    @Query("""
SELECT DISTINCT TRIM(t.product.productType)
FROM Transaction t
WHERE t.isDelete = false
AND t.product.productType IS NOT NULL
""")
    List<String> findAllTransactionTypes();


    @Query("""
SELECT DISTINCT t.status
FROM Transaction t
WHERE t.isDelete = false
""")
    List<String> findAllStatus();


    // detail

    @Query("""
SELECT t
FROM Transaction t
LEFT JOIN FETCH t.customer
LEFT JOIN FETCH t.seller
LEFT JOIN FETCH t.product
LEFT JOIN FETCH t.variant
WHERE t.id = :id
AND t.isDelete = false
""")
    Transaction findDetailById(@Param("id") Long id);
}
