package com.example.kitobgo.product;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Kitoblarni dinamik filtrlash uchun Specification'lar.
 * Har bir filtr faqat qiymati berilgan bo'lsagina qo'llaniladi (null = e'tiborsiz).
 */
public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<Product> withFilters(
            String q,
            Integer minPrice,
            Integer maxPrice,
            Boolean inStock,
            Boolean hasDiscount) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Kalit so'z: title yoki author bo'yicha (harf-registrsiz, qismiy).
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(root.get("author")), like)
                ));
            }

            // Narx oralig'i.
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }

            // Sotuvda bor / tugagan.
            if (inStock != null) {
                if (inStock) {
                    predicates.add(cb.greaterThan(root.get("stockQuantity"), 0));
                } else {
                    predicates.add(cb.or(
                            cb.isNull(root.get("stockQuantity")),
                            cb.equal(root.get("stockQuantity"), 0)
                    ));
                }
            }

            // Chegirma bor/yo'q: discountPrice mavjud va price'dan past bo'lsa "bor".
            if (hasDiscount != null) {
                Predicate discounted = cb.and(
                        cb.isNotNull(root.get("discountPrice")),
                        cb.lessThan(root.get("discountPrice"), root.get("price"))
                );
                predicates.add(hasDiscount ? discounted : cb.not(discounted));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
