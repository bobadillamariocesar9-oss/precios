package com.apiprecios.service;

import com.apiprecios.entity.Category;
import com.apiprecios.exception.BadRequestException;
import com.apiprecios.exception.ResourceNotFoundException;
import com.apiprecios.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<Category> findAll() {
        return categoryRepository.findAllByOrderByNombreCategAsc();
    }

    public Category findById(Integer id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }

    public Category create(Category category) {
        if (categoryRepository.existsByNombreCategIgnoreCase(category.getNombreCateg())) {
            throw new BadRequestException(
                    "Ya existe una categoría con el nombre '" + category.getNombreCateg() + "'");
        }
        return categoryRepository.save(category);
    }

    public Category update(Integer id, Category data) {
        Category existing = findById(id);
        existing.setNombreCateg(data.getNombreCateg());
        existing.setCateg(data.getCateg());
        return categoryRepository.save(existing);
    }

    public void delete(Integer id) {
        Category existing = findById(id);
        categoryRepository.delete(existing);
    }
}
