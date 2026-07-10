package com.example.kitobgo.product;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String title;

    private String description;

    private String author;

    private Integer price;

    private Integer discountPrice;

    @Builder.Default
    private Float rating = 0.0f;

    private Integer pageCount;

    private Integer publishedYear;

    private Integer stockQuantity;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<ProductImage> images = new ArrayList<>();

    /** Rasmni qo'shadi va navbatdagi sortOrder ni beradi (birinchi rasm = 0). */
    public void addImage(ProductImage image) {
        image.setSortOrder(images.size());
        images.add(image);
        image.setProduct(this);
    }
}
