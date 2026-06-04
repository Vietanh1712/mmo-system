package service;

import dal.CategoryRepository;
import dal.ProductRepository;
import model.Category;
import model.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    public List<Product> getProductsByCategory(Category category) {
        return productRepository.findByCategoryAndIsDeleteFalse(category);
    }

    public List<Product> searchProducts(String query) {
        return productRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseAndIsDeleteFalse(query, query);
    }

    public List<Product> searchProducts(String query, Long categoryId) {
        if (categoryId == null || categoryId == 0) {
            return searchProducts(query);
        }
        Optional<Category> category = categoryRepository.findById(categoryId);
        if (category.isPresent()) {
            return productRepository.findByCategoryAndNameContainingIgnoreCaseOrCategoryAndDescriptionContainingIgnoreCaseAndIsDeleteFalse(category.get(), query, category.get(), query);
        }
        return new ArrayList<>();
    }
}
