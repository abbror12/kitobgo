package com.example.kitobgo.product;

import com.example.kitobgo.common.AppTime;
import com.example.kitobgo.common.ConflictException;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {
    /**
     * Katalog ID'si — ketma-ket raqam (bigint identity), UUID emas: katalog baribir ochiq
     * ({@code GET /api/products} permitAll), yashiradigan narsa yo'q, "42-kitob" deb
     * gaplashish va URL esa qulay. Buyurtma/foydalanuvchi ID'lari esa ataylab UUID —
     * ularni taxmin qilib bo'lmasligi kerak.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @Column(unique = true, length = 13)
    private String isbn;

    private String publisher;

    @Column(length = 3)
    private String language;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ProductStatus status = ProductStatus.ACTIVE;

    private Integer price;

    private Integer discountPrice;

    @Builder.Default
    private Float rating = 0.0f;

    private Integer pageCount;

    private Integer publishedYear;

    private Integer stockQuantity;

    @ManyToMany
    @JoinTable(
            name = "product_categories",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    @OrderBy("name ASC")
    @Builder.Default
    private Set<Category> categories = new LinkedHashSet<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

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

    /** Mahsulotni va uning tarixiy bog'lanishlarini o'chirmasdan katalogdan arxivlaydi. */
    public void archive() {
        changeStatus(ProductStatus.ARCHIVED);
    }

    public void changeStatus(ProductStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("Mahsulot statusi bo'sh bo'lishi mumkin emas");
        }
        status = newStatus;
    }

    /** Chegirma narxi mavjud va asl narxdan past bo'lsa {@code true}. */
    public boolean hasDiscount() {
        return discountPrice != null && price != null && discountPrice < price;
    }

    /** Buyurtmada muzlatiladigan amaldagi narx. */
    public Integer effectivePrice() {
        return hasDiscount() ? discountPrice : price;
    }

    public boolean isInStock() {
        return stockQuantity != null && stockQuantity > 0;
    }

    public boolean canFulfill(int quantity) {
        return quantity > 0 && availableStock() >= quantity;
    }

    /** Yetarli zaxira bo'lsa miqdorni kamaytiradi. */
    public void decreaseStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Miqdor musbat bo'lishi kerak: " + title);
        }
        int available = availableStock();
        if (available < quantity) {
            throw new ConflictException("Yetarli zaxira yo'q: " + title
                    + " (mavjud: " + available + ", so'ralgan: " + quantity + ")");
        }
        stockQuantity = available - quantity;
    }

    /** Bekor qilingan yoki qaytarilgan buyurtma miqdorini zaxiraga qaytaradi. */
    public void increaseStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Miqdor musbat bo'lishi kerak: " + title);
        }
        stockQuantity = availableStock() + quantity;
    }

    private int availableStock() {
        return stockQuantity != null ? stockQuantity : 0;
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = AppTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (status == null) {
            status = ProductStatus.ACTIVE;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = AppTime.now();
    }
}
