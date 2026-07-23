package com.mmo.shared.dal;

import com.mmo.shared.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByIsDeleteFalse();
    List<Category> findAllByIsDeleteFalseOrderByCreatedAtDesc();
    List<Category> findAllByIsDeleteFalseOrderByIdAsc();
    List<Category> findByParentIsNullAndIsDeleteFalse();
    java.util.Optional<Category> findByName(String name);
    java.util.Optional<Category> findByNameAndParentIsNull(String name);

    @Query("SELECT c FROM Category c WHERE (:keyword IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND (:parentId IS NULL OR (:parentId = 0 AND c.parent IS NULL) OR (c.parent IS NOT NULL AND c.parent.id = :parentId)) AND (:isDelete IS NULL OR c.isDelete = :isDelete) ORDER BY c.createdAt DESC")
    List<Category> searchCategories(@Param("keyword") String keyword, @Param("parentId") Long parentId, @Param("isDelete") Boolean isDelete);

    boolean existsByNameAndParent_Id(String name, Long parentId);
    boolean existsByNameAndParentIsNull(String name);
    boolean existsByNameAndParent_IdAndIdNot(String name, Long parentId, Long id);
    boolean existsByNameAndParentIsNullAndIdNot(String name, Long id);

    long countByIsDeleteFalse();
    long countByParentIsNullAndIsDeleteFalse();
    long countByParentIsNotNullAndIsDeleteFalse();
    long countByIsDeleteTrue();
}
