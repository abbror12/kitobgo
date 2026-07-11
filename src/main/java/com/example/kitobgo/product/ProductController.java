package com.example.kitobgo.product;

import com.example.kitobgo.common.PagedResponse;
import com.example.kitobgo.product.dto.DiscountRequestDto;
import com.example.kitobgo.product.dto.ProductRequestDto;
import com.example.kitobgo.product.dto.ProductResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductResponseDto> create(@RequestBody ProductRequestDto requestDto) {
        ProductResponseDto response = productService.create(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductResponseDto> uploadImages(
            @PathVariable UUID id,
            @RequestParam("files") List<MultipartFile> files) {
        return ResponseEntity.ok(productService.addImages(id, files));
    }

    /** Kitobning asosiy maydonlarini to'liq yangilaydi (rasmlardan tashqari). */
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponseDto> update(
            @PathVariable UUID id,
            @RequestBody ProductRequestDto requestDto) {
        return ResponseEntity.ok(productService.update(id, requestDto));
    }

    /** Kitobga chegirma qo'yadi yoki (discountPrice=null bilan) olib tashlaydi. */
    @PatchMapping("/{id}/discount")
    public ResponseEntity<ProductResponseDto> setDiscount(
            @PathVariable UUID id,
            @RequestBody DiscountRequestDto requestDto) {
        return ResponseEntity.ok(productService.setDiscount(id, requestDto.discountPrice()));
    }

    /** Berilgan rasmni birinchi (muqova) qilib qo'yadi. */
    @PutMapping("/{id}/images/{imageId}/primary")
    public ResponseEntity<ProductResponseDto> makeImagePrimary(
            @PathVariable UUID id,
            @PathVariable UUID imageId) {
        return ResponseEntity.ok(productService.makeImagePrimary(id, imageId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(productService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<ProductResponseDto>> getAll() {
        return ResponseEntity.ok(productService.getAll());
    }

    @GetMapping("/search")
    public ResponseEntity<PagedResponse<ProductResponseDto>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(required = false) Boolean hasDiscount,
            @PageableDefault(size = 12, sort = "title") Pageable pageable) {
        return ResponseEntity.ok(
                productService.search(q, minPrice, maxPrice, inStock, hasDiscount, pageable));
    }

    /** Kitobning bitta rasmini o'chiradi. */
    @DeleteMapping("/{id}/images/{imageId}")
    public ResponseEntity<Void> deleteImage(
            @PathVariable UUID id,
            @PathVariable UUID imageId) {
        productService.deleteImage(id, imageId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
