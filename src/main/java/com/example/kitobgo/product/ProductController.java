package com.example.kitobgo.product;

import com.example.kitobgo.common.PagedResponse;
import com.example.kitobgo.product.dto.DiscountRequestDto;
import com.example.kitobgo.product.dto.ProductRequestDto;
import com.example.kitobgo.product.dto.ProductResponseDto;
import com.example.kitobgo.product.dto.ProductStatusRequestDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductResponseDto> create(@Valid @RequestBody ProductRequestDto requestDto) {
        ProductResponseDto response = productService.create(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductResponseDto> uploadImages(
            @PathVariable Long id,
            @RequestParam("files") List<MultipartFile> files) {
        return ResponseEntity.ok(productService.addImages(id, files));
    }

    /** Kitobning asosiy maydonlarini to'liq yangilaydi (rasmlardan tashqari). */
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponseDto> update(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequestDto requestDto) {
        return ResponseEntity.ok(productService.update(id, requestDto));
    }

    /** Kitobga chegirma qo'yadi yoki (discountPrice=null bilan) olib tashlaydi. */
    @PatchMapping("/{id}/discount")
    public ResponseEntity<ProductResponseDto> setDiscount(
            @PathVariable Long id,
            @Valid @RequestBody DiscountRequestDto requestDto) {
        return ResponseEntity.ok(productService.setDiscount(id, requestDto.discountPrice()));
    }

    /** Mahsulot statusini o'zgartiradi, jumladan ARCHIVED mahsulotni ACTIVE holatiga qaytaradi. */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ProductResponseDto> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody ProductStatusRequestDto requestDto) {
        return ResponseEntity.ok(productService.changeStatus(id, requestDto.status()));
    }

    /** Berilgan rasmni birinchi (muqova) qilib qo'yadi. */
    @PutMapping("/{id}/images/{imageId}/primary")
    public ResponseEntity<ProductResponseDto> makeImagePrimary(
            @PathVariable Long id,
            @PathVariable Long imageId) {
        return ResponseEntity.ok(productService.makeImagePrimary(id, imageId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getById(id));
    }

    /** Mahsulotlar sahifasi; {@code ?page=&size=} — sahifalash (default 12, sarlavha bo'yicha). */
    @GetMapping
    public ResponseEntity<PagedResponse<ProductResponseDto>> getAll(
            @PageableDefault(size = 12, sort = "title") Pageable pageable) {
        return ResponseEntity.ok(productService.getAll(pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<PagedResponse<ProductResponseDto>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(required = false) Boolean hasDiscount,
            @RequestParam(required = false) Long categoryId,
            @PageableDefault(size = 12, sort = "title") Pageable pageable) {
        return ResponseEntity.ok(
                productService.search(q, minPrice, maxPrice, inStock, hasDiscount, categoryId, pageable));
    }

    /** Kitobning bitta rasmini o'chiradi. */
    @DeleteMapping("/{id}/images/{imageId}")
    public ResponseEntity<Void> deleteImage(
            @PathVariable Long id,
            @PathVariable Long imageId) {
        productService.deleteImage(id, imageId);
        return ResponseEntity.noContent().build();
    }

    /** Mahsulotni fizik o'chirmasdan ARCHIVED holatiga o'tkazadi. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> archive(@PathVariable Long id) {
        productService.archive(id);
        return ResponseEntity.noContent().build();
    }
}
