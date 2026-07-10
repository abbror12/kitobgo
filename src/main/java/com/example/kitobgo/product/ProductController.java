package com.example.kitobgo.product;

import com.example.kitobgo.product.dto.ProductRequestDto;
import com.example.kitobgo.product.dto.ProductResponseDto;
import lombok.RequiredArgsConstructor;
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

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(productService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<ProductResponseDto>> getAll() {
        return ResponseEntity.ok(productService.getAll());
    }
}
