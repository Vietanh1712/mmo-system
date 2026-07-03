package com.mmo.shared.dal;

import com.mmo.shared.model.Complaint;
import com.mmo.shared.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

import java.util.List;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    List<Complaint> findBySellerAndIsDeleteFalseOrderByCreatedAtDesc(User seller);
    List<Complaint> findByCustomerAndIsDeleteFalseOrderByCreatedAtDesc(User customer);
    List<Complaint> findByIsDeleteFalseOrderByCreatedAtDesc();

    @Query("SELECT COUNT(c) FROM Complaint c WHERE c.seller = :seller AND c.status = 'InProgress' AND c.isDelete = false")
    long countOpenComplaintsBySeller(@Param("seller") User seller);



    long countByStatusAndIsDeleteFalse(String status);

    long countByIsDeleteFalse();

    //List<Complaint> findTop10ByIsDeleteFalseOrderByCreatedAtDesc();

    java.util.Optional<Complaint> findFirstByTransactionIdAndIsDeleteFalseOrderByIdDesc(Long transactionId);

    List<Complaint> findAllByIsDeleteFalseOrderByCreatedAtDesc();

    List<Complaint> findAllByIsDeleteFalseOrderByIdAsc();

    // phân trang

    Page<Complaint> findByIsDeleteFalse(Pageable pageable);

    Page<Complaint> findByDescriptionContainingIgnoreCaseAndIsDeleteFalse(
            String keyword,
            Pageable pageable);

    Page<Complaint> findByStatusAndIsDeleteFalse(
            String status,
            Pageable pageable);

    Page<Complaint> findByDescriptionContainingIgnoreCaseAndStatusAndIsDeleteFalse(
            String keyword,
            String status,
            Pageable pageable);

    @Query("SELECT DISTINCT c.status FROM Complaint c WHERE c.isDelete = false")
    List<String> getAllStatuses();

    Optional<Complaint> findByIdAndIsDeleteFalse(Long id);

    @Query("""
SELECT c
FROM Complaint c
WHERE c.isDelete = false
AND (
      :keyword IS NULL
      OR CAST(c.id AS string) = :keyword
      OR LOWER(c.description)
         LIKE LOWER(CONCAT('%', :keyword, '%'))
)
AND (
      :status IS NULL
      OR c.status = :status
)
""")
    Page<Complaint> searchComplaints(
            @Param("keyword") String keyword,
            @Param("status") String status,
            Pageable pageable
    );

    @Query("""
SELECT c
FROM Complaint c
LEFT JOIN c.customer cust
LEFT JOIN c.transaction tx
LEFT JOIN tx.product prod
WHERE c.isDelete = false
AND (
      (:complaintId IS NOT NULL AND c.id = :complaintId)
      OR
      (:complaintId IS NULL AND (
            :keyword IS NULL
            OR LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(cust.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(cust.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(prod.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
      ))
)
AND (
      :status IS NULL
      OR c.status = :status
)
""")
    Page<Complaint> searchComplaintsForStaff(
            @Param("complaintId") Long complaintId,
            @Param("keyword") String keyword,
            @Param("status") String status,
            Pageable pageable
    );
}
