package com.example.kitobgo.user;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Foydalanuvchilarni dinamik filtrlash uchun Specification'lar.
 * Har bir filtr faqat qiymati berilgan bo'lsagina qo'llaniladi (null = e'tiborsiz).
 */
public final class UserSpecifications {

    private UserSpecifications() {
    }

    public static Specification<User> withFilters(String q, Role role) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Kalit so'z: ism yoki telefon bo'yicha (harf-registrsiz, qismiy).
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("phone")), like)
                ));
            }

            // Rol bo'yicha.
            if (role != null) {
                predicates.add(cb.equal(root.get("role"), role));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
