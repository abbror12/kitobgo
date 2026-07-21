package com.example.kitobgo.product;

import com.example.kitobgo.common.ConflictException;
import com.example.kitobgo.common.NotFoundException;
import com.example.kitobgo.product.dto.CategoryRequestDto;
import com.example.kitobgo.product.dto.CategoryResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional
    public CategoryResponseDto create(CategoryRequestDto dto) {
        String name = normalizeName(dto.name());
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Bu kategoriya allaqachon mavjud: " + name);
        }
        Category category = Category.builder().name(name).build();
        return CategoryResponseDto.from(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponseDto update(Long id, CategoryRequestDto dto) {
        Category category = findById(id);
        String name = normalizeName(dto.name());
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ConflictException("Bu kategoriya allaqachon mavjud: " + name);
        }
        category.setName(name);
        return CategoryResponseDto.from(categoryRepository.save(category));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponseDto> getAll() {
        return categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
                .map(CategoryResponseDto::from)
                .toList();
    }

    private Category findById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found: " + id));
    }

    private String normalizeName(String name) {
        return name.trim().replaceAll("\\s+", " ");
    }
}
