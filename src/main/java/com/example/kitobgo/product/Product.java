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

    /**
     * Optimistik lock versiyasi. Hibernate avtomatik boshqaradi: har UPDATE'da
     * oshiriladi va WHERE shartiga qo'shiladi. Bir vaqtda ikki so'rov bir kitobni
     * o'zgartirsa, kechikkanida OptimisticLockException otiladi (masalan zaxira
     * bir vaqtda kamaytirilganda oversell'ning oldini oladi).
     */
    @Version
    private Long version;

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

    /** Rasmni o'chiradi va qolganlarini 0, 1, 2, ... bo'yicha qayta tartiblaydi. */
    public void removeImage(ProductImage image) {
        images.remove(image);
        image.setProduct(null);
        int order = 0;
        for (ProductImage img : images) {
            img.setSortOrder(order++);
        }
    }
}
