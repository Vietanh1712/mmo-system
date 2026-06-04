package controller;

import controller.dto.ProductSearchResultDTO;
import dal.CategoryRepository;
import model.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import service.ProductSearchService;

import java.util.List;

@RestController
@RequestMapping("/api/search")
public class ProductSearchController {

    @Autowired
    private ProductSearchService productSearchService;

    @Autowired
    private CategoryRepository categoryRepository;

    @GetMapping("/products")
    public ResponseEntity<Page<ProductSearchResultDTO>> searchProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long minPrice,
            @RequestParam(required = false) Long maxPrice,
            @RequestParam(required = false) String stockStatus, // e.g., "In Stock"
            @PageableDefault(size = 12, sort = "createdAt") Pageable pageable) {

        Page<ProductSearchResultDTO> results = productSearchService.searchProducts(
                keyword, categoryId, minPrice, maxPrice, stockStatus, pageable);

        return ResponseEntity.ok(results);
    }

    @GetMapping("/categories")
    public ResponseEntity<List<Category>> getCategories() {
        return ResponseEntity.ok(categoryRepository.findByIsDeleteFalse());
    }
}