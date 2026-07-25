package com.mmo.shared.dal;
import com.mmo.shared.model.Category;
import com.mmo.shared.model.Review;

import com.mmo.shared.model.Product;
import com.mmo.shared.model.ProductVariant;
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
            String stockStatus,
            Long sellerId,
            List<Integer> ratings) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // --- Keyword Search (in product name only) ---
            if (keyword != null && !keyword.trim().isEmpty()) {
                String likePattern = "%" + keyword.toLowerCase() + "%";
                Predicate namePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), likePattern);
                predicates.add(namePredicate);
            }

            // --- Category Filter ---
            if (categoryId != null) {
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.equal(root.get("category").get("id"), categoryId),
                        criteriaBuilder.equal(root.get("category").get("parent").get("id"), categoryId)
                ));
            }

            // --- Join with ProductVariants for Price and Stock filters ---
            Join<Product, ProductVariant> variantJoin = root.join("variants", JoinType.LEFT);

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

            // --- Seller Filter ---
            if (sellerId != null) {
                predicates.add(criteriaBuilder.equal(root.get("seller").get("id"), sellerId));
            }

            // --- Rating Filter (based on AVG rating of Reviews table) ---
            if (ratings != null && !ratings.isEmpty()) {
                List<Predicate> ratingPredicates = new ArrayList<>();
                for (Integer rate : ratings) {
                    Subquery<Double> avgRatingSubquery = query.subquery(Double.class);
                    Root<com.mmo.shared.model.Review> reviewRoot = avgRatingSubquery.from(com.mmo.shared.model.Review.class);
                    avgRatingSubquery.select(criteriaBuilder.avg(reviewRoot.get("rating").as(Double.class)));
                    avgRatingSubquery.where(
                        criteriaBuilder.equal(reviewRoot.get("product").get("id"), root.get("id")),
                        criteriaBuilder.equal(reviewRoot.get("isDelete"), false)
                    );
                    
                    if (rate == 5) {
                        ratingPredicates.add(criteriaBuilder.greaterThanOrEqualTo(avgRatingSubquery, 5.0));
                    } else if (rate == 4) {
                        ratingPredicates.add(criteriaBuilder.and(
                            criteriaBuilder.greaterThanOrEqualTo(avgRatingSubquery, 4.0),
                            criteriaBuilder.lessThan(avgRatingSubquery, 5.0)
                        ));
                    } else if (rate == 3) {
                        ratingPredicates.add(criteriaBuilder.and(
                            criteriaBuilder.greaterThanOrEqualTo(avgRatingSubquery, 3.0),
                            criteriaBuilder.lessThan(avgRatingSubquery, 4.0)
                        ));
                    }
                }
                if (!ratingPredicates.isEmpty()) {
                    predicates.add(criteriaBuilder.or(ratingPredicates.toArray(new Predicate[0])));
                }
            }

            // --- Ensure we don't get deleted products/variants ---
            predicates.add(criteriaBuilder.equal(root.get("isDelete"), false));
            predicates.add(criteriaBuilder.or(
                criteriaBuilder.isNull(variantJoin.get("id")),
                criteriaBuilder.equal(variantJoin.get("isDelete"), false)
            ));

            // --- Exclude products from Locked, Banned, Pending, Suspended, etc. shops, or deleted sellers ---
            predicates.add(criteriaBuilder.equal(root.get("seller").get("isDelete"), false));
            predicates.add(criteriaBuilder.not(root.get("seller").get("shopStatus").in(
                    "Locked", "Banned", "Pending", "Suspended", "TEMP_LOCKED", "Withdrawn", "DELETED", "INDEFINITE_LOCKED", "PERMANENT_BANNED"
            )));

            // --- Avoid duplicates when joining with a one-to-many relationship ---
            query.distinct(true);

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}