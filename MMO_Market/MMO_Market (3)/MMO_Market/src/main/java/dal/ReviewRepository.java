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
    List<Review> findByProductIdAndIsDeleteFalse(Long productId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId AND r.isDelete = false")
    Double findAverageRatingByProductId(@Param("productId") Long productId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.product.id = :productId AND r.isDelete = false")
    Long countByProductIdAndIsDeleteFalse(@Param("productId") Long productId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.seller.id = :sellerId AND r.isDelete = false")
    Double findAverageRatingBySellerId(@Param("sellerId") Long sellerId);

    List<Review> findByProductAndIsDeleteFalseOrderByCreatedAtDesc(Product product);

    @Query("SELECT r FROM Review r WHERE r.product.seller.id = :sellerId AND r.isDelete = false ORDER BY r.createdAt DESC")
    List<Review> findReviewsBySellerId(@Param("sellerId") Long sellerId);

    /**
     * Kiểm tra user đã đánh giá sản phẩm này chưa (fallback khi không có transactionId).
     * Chỉ dùng khi không có thông tin đơn hàng cụ thể.
     */
    boolean existsByProductIdAndUserIdAndIsDeleteFalse(Long productId, Long userId);

    /**
     * Kiểm tra đơn hàng cụ thể (transactionId) đã được đánh giá chưa.
     * Dùng để cho phép user đánh giá cùng sản phẩm nhiều lần nếu mua nhiều đơn khác nhau.
     */
    boolean existsByTransactionIdAndIsDeleteFalse(Long transactionId);
}

