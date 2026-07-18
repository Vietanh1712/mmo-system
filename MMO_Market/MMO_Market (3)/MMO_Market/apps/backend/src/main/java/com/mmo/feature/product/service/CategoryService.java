package com.mmo.feature.product.service;
import com.mmo.shared.model.Product;

import com.mmo.shared.dal.CategoryRepository;
import com.mmo.shared.model.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    public List<Category> getAllActiveCategories() {
        return categoryRepository.findByIsDeleteFalse();
    }
}
