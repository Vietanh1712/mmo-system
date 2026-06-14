package dal;

import model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByProductIdAndIsDeleteFalse(Long productId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId AND r.isDelete = false")
    Double findAverageRatingByProductId(@Param("productId") Long productId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.product.id = :productId AND r.isDelete = false")
    Long countByProductIdAndIsDeleteFalse(@Param("productId") Long productId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.seller.id = :sellerId AND r.isDelete = false")
    Double findAverageRatingBySellerId(@Param("sellerId") Long sellerId);
}
