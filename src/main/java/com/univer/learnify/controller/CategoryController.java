package com.univer.learnify.controller;

import com.univer.learnify.dto.request.CreateCategoryRequest;
import com.univer.learnify.dto.response.CategoryResponse;
import com.univer.learnify.mapper.DtoMapper;
import com.univer.learnify.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(@Valid @RequestBody CreateCategoryRequest request) {
        return DtoMapper.toCategory(categoryService.createCategory(request));
    }

    @GetMapping
    public List<CategoryResponse> list() {
        return categoryService.getCategories().stream().map(DtoMapper::toCategory).toList();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        categoryService.deleteCategory(id);
    }
}
