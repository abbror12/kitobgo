package com.example.kitobgo.product;

import com.example.kitobgo.common.NotFoundException;
import com.example.kitobgo.product.dto.ProductRequestDto;
import com.example.kitobgo.product.dto.ProductResponseDto;
import com.example.kitobgo.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
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
                .name(dto.name())
                .title(dto.title())
                .author(dto.author())
                .price(dto.price())
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
}
