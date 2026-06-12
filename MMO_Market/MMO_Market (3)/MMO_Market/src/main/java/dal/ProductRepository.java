package dal;

import model.Category;
import model.Product;
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
    List<Product> findByCategoryAndIsDeleteFalse(model.Category category);
    List<Product> findByNameContainingIgnoreCaseAndIsDeleteFalse(String name);
    List<Product> findByDescriptionContainingIgnoreCaseAndIsDeleteFalse(String description);
    List<Product> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseAndIsDeleteFalse(String name, String description);
    List<Product> findByCategoryAndNameContainingIgnoreCaseOrCategoryAndDescriptionContainingIgnoreCaseAndIsDeleteFalse(Category category, String name, Category category2, String description);
    long countBySellerIdAndIsDeleteFalse(Long sellerId);

    // Lấy top sản phẩm bán chạy nhất dựa trên số lượng giao dịch thành công
    @Query(value = """
        SELECT p.*, COUNT(t.id) AS sales_count
        FROM Products p
        LEFT JOIN Transactions t ON t.product_id = p.id
            AND t.isDelete = 0
        WHERE p.isDelete = 0
        GROUP BY p.id, p.seller_id, p.category_id, p.name, p.description, p.image,
                 p.created_at, p.isDelete, p.is_delete
        ORDER BY sales_count DESC
        """, nativeQuery = true)
    List<Product> findTopBestSellingProducts(Pageable pageable);

    // Lấy tất cả sản phẩm đang active (dùng fallback khi chưa có giao dịch)
    List<Product> findAllByIsDeleteFalse();
}
