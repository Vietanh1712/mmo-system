package dal;

import model.Category;
import model.Product;
import model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    Optional<Product> findByIdAndIsDeleteFalse(Long id);
    List<Product> findBySellerAndIsDeleteFalseOrderByCreatedAtDesc(User seller);
    List<Product> findByCategoryAndIsDeleteFalse(model.Category category);
    List<Product> findByNameContainingIgnoreCaseAndIsDeleteFalse(String name);
    List<Product> findByDescriptionContainingIgnoreCaseAndIsDeleteFalse(String description);
    List<Product> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseAndIsDeleteFalse(String name, String description);
    List<Product> findByCategoryAndNameContainingIgnoreCaseOrCategoryAndDescriptionContainingIgnoreCaseAndIsDeleteFalse(Category category, String name, Category category2, String description);
    long countBySellerIdAndIsDeleteFalse(Long sellerId);

    // Lấy top sản phẩm bán chạy nhất dựa trên số lượng giao dịch thành công
    @Query(value = """
        SELECT p.*
        FROM Products p
        LEFT JOIN (
            SELECT product_id, COUNT(*) AS sales_count
            FROM Transactions
            WHERE isDelete = 0
            GROUP BY product_id
        ) t ON t.product_id = p.id
        WHERE p.isDelete = 0
        ORDER BY COALESCE(t.sales_count, 0) DESC, p.created_at DESC
        """, nativeQuery = true)
    List<Product> findTopBestSellingProducts(Pageable pageable);

    // Lấy tất cả sản phẩm đang active (dùng fallback khi chưa có giao dịch)
    List<Product> findAllByIsDeleteFalse();
}
