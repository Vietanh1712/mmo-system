package dal;

import model.User;
import model.Withdrawal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WithdrawalRepository extends JpaRepository<Withdrawal, Long> {

    List<Withdrawal> findBySellerAndIsDeleteFalseOrderByCreatedAtDesc(User seller);

    Optional<Withdrawal> findByIdAndIsDeleteFalse(Long id);
    List<Withdrawal> findByStatusAndIsDeleteFalse(String status);

    @Query("SELECT w FROM Withdrawal w JOIN FETCH w.seller WHERE w.isDelete = false")
    List<Withdrawal> findAllWithSellerByIsDeleteFalse();
    long countByStatusAndIsDeleteFalse(String status);
    @Query(
            value = """
SELECT w
FROM Withdrawal w
WHERE w.isDelete = false
AND (:status IS NULL OR w.status = :status)
AND (
      :keyword IS NULL
      OR LOWER(w.seller.fullName)
         LIKE LOWER(CONCAT('%', :keyword, '%'))
      OR CAST(w.id AS string) = :keyword
)
AND (:amount IS NULL OR w.amountVnd = :amount)
""",

            countQuery = """
SELECT COUNT(w)
FROM Withdrawal w
WHERE w.isDelete = false
AND (:status IS NULL OR w.status = :status)
AND (
      :keyword IS NULL
      OR LOWER(w.seller.fullName)
         LIKE LOWER(CONCAT('%', :keyword, '%'))
      OR CAST(w.id AS string) = :keyword
)
AND (:amount IS NULL OR w.amountVnd = :amount)
"""
    )


    Page<Withdrawal> searchWithdrawals(
            @Param("status") String status,
            @Param("keyword") String keyword,
            @Param("amount") Long amount,
            Pageable pageable
    );


    @Query("""
SELECT DISTINCT w.status
FROM Withdrawal w
WHERE w.isDelete = false
""")
    List<String> findAllStatus();


    //// chi tiet

    @Query("""
SELECT w
FROM Withdrawal w
LEFT JOIN FETCH w.seller
LEFT JOIN FETCH w.bankInfo
WHERE w.id = :id
AND w.isDelete = false
""")
    Optional<Withdrawal> findDetailById(
            @Param("id") Long id
    );
}
