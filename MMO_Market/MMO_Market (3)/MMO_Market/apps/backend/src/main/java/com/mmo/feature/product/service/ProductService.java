package com.mmo.feature.product.service;
import com.mmo.shared.dal.TransactionRepository;

import com.mmo.shared.dto.FeaturedProductDTO;
import com.mmo.shared.dal.CategoryRepository;
import com.mmo.shared.dal.ProductRepository;
import com.mmo.shared.model.Category;
import com.mmo.shared.model.Product;
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
    private com.mmo.shared.dal.TransactionRepository transactionRepository;

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
            // Lọc bỏ sản phẩm của shop bị Locked, Banned, Pending, Suspended, TEMP_LOCKED, Withdrawn
            products = products.stream()
                    .filter(p -> p.getSeller() != null 
                        && Boolean.FALSE.equals(p.getSeller().getIsDelete())
                        && (p.getSeller().getShopStatus() == null 
                            || (!"Locked".equalsIgnoreCase(p.getSeller().getShopStatus())
                            && !"Banned".equalsIgnoreCase(p.getSeller().getShopStatus())
                            && !"Pending".equalsIgnoreCase(p.getSeller().getShopStatus())
                            && !"Suspended".equalsIgnoreCase(p.getSeller().getShopStatus())
                            && !"TEMP_LOCKED".equalsIgnoreCase(p.getSeller().getShopStatus())
                            && !"Withdrawn".equalsIgnoreCase(p.getSeller().getShopStatus())
                            && !"DELETED".equalsIgnoreCase(p.getSeller().getShopStatus()))))
                    .collect(Collectors.toList());
            if (products.size() > limit) {
                products = products.subList(0, limit);
            }
        }

        if (products == null || products.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> productIds = products.stream().map(Product::getId).collect(Collectors.toList());
        java.util.Map<Long, Long> salesCounts = transactionRepository.countByProductIdsAndIsDeleteFalse(productIds).stream()
                .collect(Collectors.toMap(obj -> (Long) obj[0], obj -> (Long) obj[1]));

        return products.stream()
                .map(p -> FeaturedProductDTO.fromEntity(p, salesCounts.getOrDefault(p.getId(), 0L)))
                .collect(Collectors.toList());
    }
}
