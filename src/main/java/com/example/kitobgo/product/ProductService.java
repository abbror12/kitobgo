package com.example.kitobgo.product;

import com.example.kitobgo.common.NotFoundException;
import com.example.kitobgo.common.PagedResponse;
import com.example.kitobgo.product.dto.ProductRequestDto;
import com.example.kitobgo.product.dto.ProductResponseDto;
import com.example.kitobgo.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public ProductResponseDto create(ProductRequestDto dto) {
        validateDiscount(dto.price(), dto.discountPrice());

        Product product = Product.builder()
                .title(dto.title())
                .description(dto.description())
                .author(dto.author())
                .price(dto.price())
                .discountPrice(dto.discountPrice())
                .pageCount(dto.pageCount())
                .publishedYear(dto.publishedYear())
                .stockQuantity(dto.stockQuantity())
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
    public ProductResponseDto update(Long id, ProductRequestDto dto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Book not found: " + id));

        validateDiscount(dto.price(), dto.discountPrice());

        product.setTitle(dto.title());
        product.setDescription(dto.description());
        product.setAuthor(dto.author());
        product.setPrice(dto.price());
        product.setDiscountPrice(dto.discountPrice());
        product.setPageCount(dto.pageCount());
        product.setPublishedYear(dto.publishedYear());
        product.setStockQuantity(dto.stockQuantity());

        return ProductResponseDto.from(productRepository.save(product));
    }

    /**
     * Kitobga chegirma qo'yadi yoki olib tashlaydi (PATCH semantikasi).
     * {@code discountPrice == null} bo'lsa chegirma olib tashlanadi.
     */
    @Transactional
    public ProductResponseDto setDiscount(Long id, Integer discountPrice) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Book not found: " + id));

        validateDiscount(product.getPrice(), discountPrice);

        product.setDiscountPrice(discountPrice);
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

    /**
     * Kitobga bir yoki bir nechta rasm faylini yuklab biriktiradi.
     */
    @Transactional
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

    /**
     * Kitobni o'chiradi. cascade + orphanRemoval tufayli unga tegishli rasmlar ham o'chadi;
     * rasm fayllari commit'dan keyin diskdan tozalanadi.
     */
    @Transactional
    public void delete(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Book not found: " + id));
        product.getImages().forEach(img -> fileStorageService.deleteAfterCommit(img.getUrl()));
        productRepository.delete(product);
    }

    @Transactional(readOnly = true)
    public ProductResponseDto getById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Book not found: " + id));
        return ProductResponseDto.from(product);
    }

    /** Mahsulotlar sahifasi (katalog). Filtrsiz to'liq ro'yxat o'rniga sahifalab beriladi. */
    @Transactional(readOnly = true)
    public PagedResponse<ProductResponseDto> getAll(Pageable pageable) {
        return PagedResponse.from(productRepository.findAll(pageable).map(ProductResponseDto::from));
    }

    /**
     * Kalit so'z va filtrlar bo'yicha sahifalangan qidiruv.
     * Barcha filtrlar ixtiyoriy (null bo'lsa e'tiborsiz qoldiriladi).
     */
    @Transactional(readOnly = true)
    public PagedResponse<ProductResponseDto> search(
            String q,
            Integer minPrice,
            Integer maxPrice,
            Boolean inStock,
            Boolean hasDiscount,
            Pageable pageable) {

        Specification<Product> spec =
                ProductSpecifications.withFilters(q, minPrice, maxPrice, inStock, hasDiscount);

        Page<ProductResponseDto> page = productRepository.findAll(spec, pageable)
                .map(ProductResponseDto::from);

        return PagedResponse.from(page);
    }
}
