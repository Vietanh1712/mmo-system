package service;

import controller.dto.FeaturedProductDTO;
import dal.CategoryRepository;
import dal.ProductRepository;
import model.Category;
import model.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private dal.TransactionRepository transactionRepository;

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

    /**
     * Lấy top sản phẩm nổi bật dựa trên lượt bán thực tế từ bảng Transactions.
     * Nếu chưa có giao dịch nào, fallback về top N sản phẩm mới nhất.
     *
     * @param limit Số lượng sản phẩm nổi bật cần lấy (mặc định 8)
     * @return Danh sách FeaturedProductDTO đã xử lý
     */
    public List<FeaturedProductDTO> getFeaturedProducts(int limit) {
        // Trước tiên thử lấy theo số lượng giao dịch
        List<Product> products = productRepository.findTopBestSellingProducts(
                PageRequest.of(0, limit));

        // Fallback: nếu DB chưa có giao dịch nào, lấy sản phẩm mới
        if (products == null || products.isEmpty()) {
            products = productRepository.findAllByIsDeleteFalse();
            if (products.size() > limit) {
                products = products.subList(0, limit);
            }
        }

        // Đếm số giao dịch cho từng sản phẩm từ DB để hiển thị lượt bán thực tế
        return products.stream()
                .map(p -> {
                    Long salesCount = transactionRepository.countByProductIdAndIsDeleteFalse(p.getId());
                    return FeaturedProductDTO.fromEntity(p, salesCount);
                })
                .collect(Collectors.toList());
    }
}
