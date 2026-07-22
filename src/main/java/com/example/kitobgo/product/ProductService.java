package com.example.kitobgo.product;

import com.example.kitobgo.common.ConflictException;
import com.example.kitobgo.common.NotFoundException;
import com.example.kitobgo.common.PagedResponse;
import com.example.kitobgo.product.dto.ProductRequestDto;
import com.example.kitobgo.product.dto.ProductResponseDto;
import com.example.kitobgo.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final FileStorageService fileStorageService;
    private final CategoryRepository categoryRepository;

    @Transactional
    @CacheEvict(cacheNames = {"productCatalog", "productDetails"}, allEntries = true)
    public ProductResponseDto create(ProductRequestDto dto) {
        validateDiscount(dto.price(), dto.discountPrice());
        String isbn = normalizeIsbn(dto.isbn());
        validateUniqueIsbn(isbn, null);

        Product product = Product.builder()
                .title(dto.title())
                .description(dto.description())
                .author(dto.author())
                .isbn(isbn)
                .publisher(dto.publisher())
                .language(normalizeLanguage(dto.language()))
                .status(dto.status() != null ? dto.status() : ProductStatus.ACTIVE)
                .price(dto.price())
                .discountPrice(dto.discountPrice())
                .pageCount(dto.pageCount())
                .publishedYear(dto.publishedYear())
                .stockQuantity(dto.stockQuantity())
                .categories(resolveCategories(dto.categoryIds()))
                .build();

        if (dto.imageUrls() != null) {
            dto.imageUrls().stream()
                    .map(url -> ProductImage.builder().url(url).build())
                    .forEach(product::addImage);
        }

        return ProductResponseDto.from(productRepository.save(product));
    }

    /**
     * Kitobning asosiy maydonlarini to'liq yangilaydi (PUT semantikasi).
     * Rasmlar bu yerda o'zgartirilmaydi — ular alohida endpointlar orqali boshqariladi.
     */
    @Transactional
    @CacheEvict(cacheNames = {"productCatalog", "productDetails"}, allEntries = true)
    public ProductResponseDto update(Long id, ProductRequestDto dto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Book not found: " + id));

        validateDiscount(dto.price(), dto.discountPrice());
        String isbn = normalizeIsbn(dto.isbn());
        validateUniqueIsbn(isbn, id);

        product.setTitle(dto.title());
        product.setDescription(dto.description());
        product.setAuthor(dto.author());
        product.setIsbn(isbn);
        product.setPublisher(dto.publisher());
        product.setLanguage(normalizeLanguage(dto.language()));
        if (dto.status() != null) {
            product.changeStatus(dto.status());
        }
        product.setPrice(dto.price());
        product.setDiscountPrice(dto.discountPrice());
        product.setPageCount(dto.pageCount());
        product.setPublishedYear(dto.publishedYear());
        product.setStockQuantity(dto.stockQuantity());
        if (dto.categoryIds() != null) {
            product.getCategories().clear();
            product.getCategories().addAll(resolveCategories(dto.categoryIds()));
        }

        return ProductResponseDto.from(productRepository.save(product));
    }

    /**
     * Kitobga chegirma qo'yadi yoki olib tashlaydi (PATCH semantikasi).
     * {@code discountPrice == null} bo'lsa chegirma olib tashlanadi.
     */
    @Transactional
    @CacheEvict(cacheNames = {"productCatalog", "productDetails"}, allEntries = true)
    public ProductResponseDto setDiscount(Long id, Integer discountPrice) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Book not found: " + id));

        validateDiscount(product.getPrice(), discountPrice);

        product.setDiscountPrice(discountPrice);
        return ProductResponseDto.from(productRepository.save(product));
    }

    /** DRAFT, ACTIVE va ARCHIVED holatlari orasida o'tkazadi; ARCHIVED -> ACTIVE ham shu yerda. */
    @Transactional
    @CacheEvict(cacheNames = {"productCatalog", "productDetails"}, allEntries = true)
    public ProductResponseDto changeStatus(Long id, ProductStatus status) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Book not found: " + id));
        product.changeStatus(status);
        return ProductResponseDto.from(productRepository.save(product));
    }

    /** Chegirma narxi to'g'riligini tekshiradi (null = chegirma yo'q, ruxsat etiladi). */
    private void validateDiscount(Integer price, Integer discountPrice) {
        if (discountPrice == null) {
            return;
        }
        if (discountPrice <= 0) {
            throw new IllegalArgumentException("Chegirma narxi musbat bo'lishi kerak");
        }
        if (price == null || discountPrice >= price) {
            throw new IllegalArgumentException("Chegirma narxi asl narxdan past bo'lishi kerak");
        }
    }

    /** ISBN tire va bo'shliqlarsiz saqlanadi; ISBN-10 hamda ISBN-13 qabul qilinadi. */
    private String normalizeIsbn(String isbn) {
        if (isbn == null || isbn.isBlank()) {
            return null;
        }
        String normalized = isbn.replaceAll("[-\\s]", "").toUpperCase(Locale.ROOT);
        if (!normalized.matches("(?:\\d{13}|\\d{9}[\\dX])")) {
            throw new IllegalArgumentException("ISBN-10 yoki ISBN-13 formati noto'g'ri");
        }
        return normalized;
    }

    /** Til ISO 639 kodi ko'rinishida saqlanadi: uz, ru, en va hokazo. */
    private String normalizeLanguage(String language) {
        if (language == null || language.isBlank()) {
            return null;
        }
        String normalized = language.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z]{2,3}")) {
            throw new IllegalArgumentException("Til kodi 2 yoki 3 ta lotin harfidan iborat bo'lishi kerak");
        }
        return normalized;
    }

    private void validateUniqueIsbn(String isbn, Long currentProductId) {
        if (isbn == null) {
            return;
        }
        boolean exists = currentProductId == null
                ? productRepository.existsByIsbn(isbn)
                : productRepository.existsByIsbnAndIdNot(isbn, currentProductId);
        if (exists) {
            throw new ConflictException("Bu ISBN bilan mahsulot allaqachon mavjud: " + isbn);
        }
    }

    private Set<Category> resolveCategories(Set<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return new LinkedHashSet<>();
        }
        List<Category> categories = categoryRepository.findAllById(categoryIds);
        Set<Long> foundIds = categories.stream()
                .map(Category::getId)
                .collect(Collectors.toSet());
        Set<Long> missingIds = categoryIds.stream()
                .filter(id -> !foundIds.contains(id))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (!missingIds.isEmpty()) {
            throw new NotFoundException("Categories not found: " + missingIds);
        }
        return new LinkedHashSet<>(categories);
    }

    /**
     * Kitobga bir yoki bir nechta rasm faylini yuklab biriktiradi.
     */
    @Transactional
    @CacheEvict(cacheNames = {"productCatalog", "productDetails"}, allEntries = true)
    public ProductResponseDto addImages(Long productId, List<MultipartFile> files) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Book not found: " + productId));

        if (files != null) {
            for (MultipartFile file : files) {
                String url = fileStorageService.store(file);
                product.addImage(ProductImage.builder().url(url).build());
            }
        }

        return ProductResponseDto.from(productRepository.save(product));
    }

    /**
     * Berilgan rasmni birinchi (muqova) qilib qo'yadi va qolganlarini qayta tartiblaydi.
     */
    @Transactional
    @CacheEvict(cacheNames = {"productCatalog", "productDetails"}, allEntries = true)
    public ProductResponseDto makeImagePrimary(Long productId, Long imageId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Book not found: " + productId));

        List<ProductImage> images = product.getImages();   // sortOrder bo'yicha tartiblangan
        boolean exists = images.stream().anyMatch(img -> img.getId().equals(imageId));
        if (!exists) {
            throw new NotFoundException("Image not found: " + imageId);
        }

        int order = 1;
        for (ProductImage img : images) {
            if (img.getId().equals(imageId)) {
                img.setSortOrder(0);          // tanlangan rasm — birinchi
            } else {
                img.setSortOrder(order++);    // qolganlari mavjud tartibda 1, 2, ...
            }
        }

        return ProductResponseDto.from(productRepository.save(product));
    }

    /**
     * Kitobning bitta rasmini o'chiradi va qolganlarini qayta tartiblaydi.
     * orphanRemoval tufayli rasm bazadan, commit'dan keyin esa diskdan ham o'chadi.
     */
    @Transactional
    @CacheEvict(cacheNames = {"productCatalog", "productDetails"}, allEntries = true)
    public void deleteImage(Long productId, Long imageId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Book not found: " + productId));

        ProductImage image = product.getImages().stream()
                .filter(img -> img.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Image not found: " + imageId));

        product.removeImage(image);
        productRepository.save(product);
        fileStorageService.deleteAfterCommit(image.getUrl());
    }

    /** Mahsulot va rasmlarini o'chirmasdan ARCHIVED holatiga o'tkazadi. */
    @Transactional
    @CacheEvict(cacheNames = {"productCatalog", "productDetails"}, allEntries = true)
    public void archive(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Book not found: " + id));
        product.archive();
        productRepository.save(product);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "productDetails", key = "#id", sync = true)
    public ProductResponseDto getById(Long id) {
        Product product = productRepository.findByIdAndStatus(id, ProductStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("Book not found: " + id));
        return ProductResponseDto.from(product);
    }

    /** Ochiq katalogda faqat ACTIVE mahsulotlar sahifalab beriladi. */
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "productCatalog", sync = true)
    public PagedResponse<ProductResponseDto> getAll(Pageable pageable) {
        return PagedResponse.from(productRepository.findAllByStatus(ProductStatus.ACTIVE, pageable)
                .map(ProductResponseDto::from));
    }

    /**
     * Kalit so'z va filtrlar bo'yicha sahifalangan qidiruv.
     * Barcha filtrlar ixtiyoriy (null bo'lsa e'tiborsiz qoldiriladi).
     */
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "productCatalog", sync = true)
    public PagedResponse<ProductResponseDto> search(
            String q,
            Integer minPrice,
            Integer maxPrice,
            Boolean inStock,
            Boolean hasDiscount,
            Long categoryId,
            Pageable pageable) {

        Specification<Product> spec =
                ProductSpecifications.withFilters(q, minPrice, maxPrice, inStock, hasDiscount, categoryId);

        Page<ProductResponseDto> page = productRepository.findAll(spec, pageable)
                .map(ProductResponseDto::from);

        return PagedResponse.from(page);
    }
}
