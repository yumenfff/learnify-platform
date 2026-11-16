package com.univer.learnify.service;

import com.univer.learnify.dto.request.CreateCategoryRequest;
import com.univer.learnify.entity.Category;
import com.univer.learnify.exception.ConflictException;
import com.univer.learnify.exception.ResourceNotFoundException;
import com.univer.learnify.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional
    public Category createCategory(CreateCategoryRequest request) {
        String name = request.name().trim();
        if (categoryRepository.existsByName(name)) {
            throw new ConflictException("Категория «" + name + "» уже существует");
        }
        Category saved = categoryRepository.save(new Category(name));
        log.info("Создана категория id={}, name={}", saved.getId(), name);
        return saved;
    }

    public List<Category> getCategories() {
        return categoryRepository.findAll();
    }


    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Категория", id));
        categoryRepository.delete(category);
        log.info("Удалена категория id={}", id);
    }
}
