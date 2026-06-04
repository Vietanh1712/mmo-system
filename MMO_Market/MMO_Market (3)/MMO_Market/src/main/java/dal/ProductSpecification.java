package dal;

import model.Product;
import model.ProductVariant;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class ProductSpecification {

    public static Specification<Product> withDynamicQuery(
            String keyword,
            Long categoryId,
            Long minPrice,
            Long maxPrice,
            String stockStatus) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // --- Keyword Search (in product name and description) ---
            if (keyword != null && !keyword.trim().isEmpty()) {
                String likePattern = "%" + keyword.toLowerCase() + "%";
                Predicate namePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), likePattern);
                Predicate descriptionPredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), likePattern);
                predicates.add(criteriaBuilder.or(namePredicate, descriptionPredicate));
            }

            // --- Category Filter ---
            if (categoryId != null) {
                if (categoryId == 7L) {
                    // "Khác": danh mục con có tên chứa "khác" (Loại Mail Khác, Tài Khoản Khác, ...)
                    Predicate categoryKhac = criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("category").get("name")), "%khác%");
                    Predicate parentKhac = criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("category").get("parent").get("name")), "%khác%");
                    predicates.add(criteriaBuilder.or(categoryKhac, parentKhac));
                } else {
                    predicates.add(criteriaBuilder.or(
                            criteriaBuilder.equal(root.get("category").get("id"), categoryId),
                            criteriaBuilder.equal(root.get("category").get("parent").get("id"), categoryId)
                    ));
                }
            }

            // --- Join with ProductVariants for Price and Stock filters ---
            Join<Product, ProductVariant> variantJoin = root.join("variants", JoinType.INNER);

            // --- Price Range Filter ---
            if (minPrice != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(variantJoin.get("priceVnd"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(variantJoin.get("priceVnd"), maxPrice));
            }

            // --- Stock Status Filter ---
            if ("In Stock".equalsIgnoreCase(stockStatus)) {
                predicates.add(criteriaBuilder.greaterThan(variantJoin.get("stock"), 0));
            } else if ("Out of Stock".equalsIgnoreCase(stockStatus)) {
                predicates.add(criteriaBuilder.or(
                    criteriaBuilder.equal(variantJoin.get("stock"), 0),
                    criteriaBuilder.isNull(variantJoin.get("stock"))
                ));
            }

            // --- Ensure we don't get deleted products/variants ---
            predicates.add(criteriaBuilder.equal(root.get("isDelete"), false));
            predicates.add(criteriaBuilder.equal(variantJoin.get("isDelete"), false));

            // --- Avoid duplicates when joining with a one-to-many relationship ---
            query.distinct(true);

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}