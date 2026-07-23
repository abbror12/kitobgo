package com.example.kitobgo.product;

import com.example.kitobgo.common.ConflictException;
import com.example.kitobgo.common.NotFoundException;
import com.example.kitobgo.product.dto.CategoryRequestDto;
import com.example.kitobgo.product.dto.CategoryResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Transactional
    @CacheEvict(cacheNames = {"categories", "productCatalog"}, allEntries = true)
    public CategoryResponseDto create(CategoryRequestDto dto) {
        String name = normalizeName(dto.name());
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Bu kategoriya allaqachon mavjud: " + name);
        }
        Category category = Category.builder().name(name).build();
        return CategoryResponseDto.from(categoryRepository.save(category));
    }

    @Transactional
    @CacheEvict(cacheNames = {"categories", "productCatalog", "productDetails"}, allEntries = true)
    public CategoryResponseDto update(Long id, CategoryRequestDto dto) {
        Category category = findById(id);
        String name = normalizeName(dto.name());
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ConflictException("Bu kategoriya allaqachon mavjud: " + name);
        }
        category.setName(name);
        return CategoryResponseDto.from(categoryRepository.save(category));
    }

    /**
     * Kategoriyani o'chiradi. Guard: unga biriktirilgan mahsulot bo'lsa o'chirilmaydi
     * ({@code ConflictException}) — avval mahsulotlarni boshqa kategoriyaga ko'chirish kerak.
     * Bu DB dagi {@code product_categories_category_fk} (ON DELETE qoidasi yo'q) himoyasini
     * tushunarli xato bilan takrorlaydi.
     */
    @Transactional
    @CacheEvict(cacheNames = {"categories", "productCatalog", "productDetails"}, allEntries = true)
    public void delete(Long id) {
        Category category = findById(id);
        if (productRepository.existsByCategories_Id(id)) {
            throw new ConflictException("Kategoriyani o'chirib bo'lmaydi — unga biriktirilgan "
                    + "mahsulotlar bor: " + category.getName());
        }
        categoryRepository.delete(category);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "categories", sync = true)
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
