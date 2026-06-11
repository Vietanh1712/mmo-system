package dal;

import model.Product;
import model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByProductAndIsDeleteFalseOrderByCreatedAtDesc(Product product);

    @Query("SELECT r FROM Review r WHERE r.product.seller.id = :sellerId AND r.isDelete = false ORDER BY r.createdAt DESC")
    List<Review> findReviewsBySellerId(@Param("sellerId") Long sellerId);
}
