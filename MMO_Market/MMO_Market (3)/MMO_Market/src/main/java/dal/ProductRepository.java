package dal;

import model.Category;
import model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByCategoryAndIsDeleteFalse(model.Category category);
    List<Product> findByNameContainingIgnoreCaseAndIsDeleteFalse(String name);
    List<Product> findByDescriptionContainingIgnoreCaseAndIsDeleteFalse(String description);
    List<Product> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseAndIsDeleteFalse(String name, String description);
    List<Product> findByCategoryAndNameContainingIgnoreCaseOrCategoryAndDescriptionContainingIgnoreCaseAndIsDeleteFalse(Category category, String name, Category category2, String description);
}
