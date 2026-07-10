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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public ProductResponseDto create(ProductRequestDto dto) {
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
     * Kitobga bir yoki bir nechta rasm faylini yuklab biriktiradi.
     */
    @Transactional
    public ProductResponseDto addImages(UUID productId, List<MultipartFile> files) {
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
    public ProductResponseDto makeImagePrimary(UUID productId, UUID imageId) {
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
     * Kitobni o'chiradi. cascade + orphanRemoval tufayli unga tegishli rasmlar ham o'chadi.
     */
    @Transactional
    public void delete(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Book not found: " + id));
        productRepository.delete(product);
    }

    @Transactional(readOnly = true)
    public ProductResponseDto getById(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Book not found: " + id));
        return ProductResponseDto.from(product);
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDto> getAll() {
        return productRepository.findAll().stream()
                .map(ProductResponseDto::from)
                .toList();
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
