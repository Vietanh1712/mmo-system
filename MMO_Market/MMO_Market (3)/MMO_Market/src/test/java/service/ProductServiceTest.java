package service;

import controller.dto.FeaturedProductDTO;
import dal.ProductRepository;
import dal.TransactionRepository;
import model.Category;
import model.Product;
import model.ProductVariant;
import model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void getFeaturedProductsReturnsBestSellingProductsSuccessfully() {
        // Arrange
        User seller = User.builder()
                .id(1L)
                .fullName("VipStore")
                .isVerified(true)
                .build();
        Category category = Category.builder()
                .id(2L)
                .name("Tài khoản")
                .build();

        Product product1 = new Product();
        product1.setId(101L);
        product1.setName("Product A");
        product1.setSeller(seller);
        product1.setCategory(category);
        product1.setIsDelete(false);

        ProductVariant variant1 = new ProductVariant();
        variant1.setId(10L);
        variant1.setProduct(product1);
        variant1.setPriceVnd(50_000L);
        variant1.setStock(10);
        variant1.setIsDelete(false);
        product1.setVariants(List.of(variant1));

        Product product2 = new Product();
        product2.setId(102L);
        product2.setName("Product B");
        product2.setSeller(seller);
        product2.setCategory(category);
        product2.setIsDelete(false);

        ProductVariant variant2 = new ProductVariant();
        variant2.setId(11L);
        variant2.setProduct(product2);
        variant2.setPriceVnd(100_000L);
        variant2.setStock(5);
        variant2.setIsDelete(false);
        product2.setVariants(List.of(variant2));

        List<Product> mockBestSellers = List.of(product1, product2);

        when(productRepository.findTopBestSellingProducts(PageRequest.of(0, 8)))
                .thenReturn(mockBestSellers);
        when(transactionRepository.countByProductIdAndIsDeleteFalse(101L)).thenReturn(10L);
        when(transactionRepository.countByProductIdAndIsDeleteFalse(102L)).thenReturn(5L);

        // Act
        List<FeaturedProductDTO> results = productService.getFeaturedProducts(8);

        // Assert
        assertNotNull(results);
        assertEquals(2, results.size());

        FeaturedProductDTO dtoA = results.get(0);
        assertEquals(101L, dtoA.getId());
        assertEquals("Product A", dtoA.getName());
        assertEquals(10L, dtoA.getSalesCount());
        assertEquals(50_000L, dtoA.getMinPrice());
        assertEquals(10, dtoA.getTotalStock());

        FeaturedProductDTO dtoB = results.get(1);
        assertEquals(102L, dtoB.getId());
        assertEquals("Product B", dtoB.getName());
        assertEquals(5L, dtoB.getSalesCount());
        assertEquals(100_000L, dtoB.getMinPrice());
        assertEquals(5, dtoB.getTotalStock());

        verify(productRepository, times(1)).findTopBestSellingProducts(PageRequest.of(0, 8));
        verify(productRepository, never()).findAllByIsDeleteFalse();
        verify(transactionRepository, times(1)).countByProductIdAndIsDeleteFalse(101L);
        verify(transactionRepository, times(1)).countByProductIdAndIsDeleteFalse(102L);
    }

    @Test
    void getFeaturedProductsFallbacksToAllProductsWhenNoBestSelling() {
        // Arrange
        User seller = User.builder()
                .id(1L)
                .fullName("VipStore")
                .isVerified(true)
                .build();
        Category category = Category.builder()
                .id(2L)
                .name("Tài khoản")
                .build();

        Product productFallback = new Product();
        productFallback.setId(201L);
        productFallback.setName("Fallback Product");
        productFallback.setSeller(seller);
        productFallback.setCategory(category);
        productFallback.setIsDelete(false);

        ProductVariant variant = new ProductVariant();
        variant.setId(20L);
        variant.setProduct(productFallback);
        variant.setPriceVnd(30_000L);
        variant.setStock(20);
        variant.setIsDelete(false);
        productFallback.setVariants(List.of(variant));

        when(productRepository.findTopBestSellingProducts(PageRequest.of(0, 8)))
                .thenReturn(Collections.emptyList());
        when(productRepository.findAllByIsDeleteFalse())
                .thenReturn(List.of(productFallback));
        when(transactionRepository.countByProductIdAndIsDeleteFalse(201L)).thenReturn(0L);

        // Act
        List<FeaturedProductDTO> results = productService.getFeaturedProducts(8);

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());

        FeaturedProductDTO dto = results.get(0);
        assertEquals(201L, dto.getId());
        assertEquals("Fallback Product", dto.getName());
        assertEquals(0L, dto.getSalesCount());
        assertEquals(30_000L, dto.getMinPrice());
        assertEquals(20, dto.getTotalStock());

        verify(productRepository, times(1)).findTopBestSellingProducts(PageRequest.of(0, 8));
        verify(productRepository, times(1)).findAllByIsDeleteFalse();
        verify(transactionRepository, times(1)).countByProductIdAndIsDeleteFalse(201L);
    }
}
