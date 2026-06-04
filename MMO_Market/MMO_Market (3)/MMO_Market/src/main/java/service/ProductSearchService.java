package service;

import dal.ProductRepository;
import dal.ProductSpecification;
import controller.dto.ProductSearchResultDTO;
import model.Product;
import model.ProductVariant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.Optional;

@Service
public class ProductSearchService {

    @Autowired
    private ProductRepository productRepository;

    public Page<ProductSearchResultDTO> searchProducts(
            String keyword, Long categoryId, Long minPrice, Long maxPrice,
            String stockStatus, Pageable pageable) {

        Specification<Product> spec = ProductSpecification.withDynamicQuery(
                keyword, categoryId, minPrice, maxPrice, stockStatus);

        Page<Product> productPage = productRepository.findAll(spec, pageable);

        return productPage.map(this::convertToDTO);
    }

    private ProductSearchResultDTO convertToDTO(Product product) {
        ProductSearchResultDTO dto = new ProductSearchResultDTO();
        dto.setProductId(product.getId());
        dto.setProductName(product.getName());
        dto.setImageUrl(product.getImage());

        if (product.getCategory() != null) {
            dto.setCategoryName(product.getCategory().getName());
        }

        if (product.getSeller() != null) {
            // Using fullName as a fallback if shopName is null, since shopName might not exist in User entity right now
            String shopName = product.getSeller().getFullName(); 
            dto.setSellerName(shopName != null ? shopName : "Unknown Seller");
            dto.setSellerIsVerified(false); // Placeholder
        }

        // Find the cheapest active variant to display on the card
        if (product.getVariants() != null && !product.getVariants().isEmpty()) {
            Optional<ProductVariant> cheapestVariant = product.getVariants().stream()
                    .filter(v -> v.getIsDelete() != null && !v.getIsDelete())
                    .min(Comparator.comparing(ProductVariant::getPriceVnd));

            if (cheapestVariant.isPresent()) {
                ProductVariant variant = cheapestVariant.get();
                dto.setPrice(variant.getPriceVnd());
                dto.setStock(variant.getStock());
            } else {
                dto.setPrice(0L);
                dto.setStock(0);
            }
        } else {
            dto.setPrice(0L);
            dto.setStock(0);
        }

        return dto;
    }
}