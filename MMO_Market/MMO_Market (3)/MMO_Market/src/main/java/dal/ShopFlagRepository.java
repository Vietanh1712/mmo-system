package dal;

import model.ShopFlag;
import model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import java.util.List;
import java.util.Optional;

@Repository
public interface ShopFlagRepository extends JpaRepository<ShopFlag, Long> {
    List<ShopFlag> findBySellerAndIsDeleteFalseOrderByCreatedAtDesc(User seller);

    // thêm
    long countByIsDeleteFalse();

    long countByFlagLevelAndIsDeleteFalse(String flagLevel);

    long countByStatusAndIsDeleteFalse(String status);

    List<ShopFlag> findAllByIsDeleteFalseOrderByCreatedAtDesc();

    Page<ShopFlag> findAllByIsDeleteFalse(Pageable pageable);

    Optional<ShopFlag> findByIdAndIsDeleteFalse(Long id);

    @Query("""
SELECT f FROM ShopFlag f
LEFT JOIN f.seller s
WHERE f.isDelete = false

AND (
    :keyword IS NULL
    OR LOWER(s.fullName)
       LIKE LOWER(CONCAT('%', :keyword, '%'))
    OR CAST(f.id AS string) = :keyword
       
)

AND (
    :level IS NULL
    OR LOWER(f.flagLevel)
       = LOWER(:level)
)

AND (
    :status IS NULL
    OR LOWER(f.status)
       = LOWER(:status)
)
""")
    Page<ShopFlag> searchFlags(
            @Param("keyword") String keyword,
            @Param("level") String level,
            @Param("status") String status,
            Pageable pageable
    );

}
