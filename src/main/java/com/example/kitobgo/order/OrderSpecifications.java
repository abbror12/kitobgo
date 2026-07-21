package com.example.kitobgo.order;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Admin panelidagi buyurtma qidiruvi uchun dinamik filtrlar. */
public final class OrderSpecifications {

    private OrderSpecifications() {
    }

    public static Specification<Order> withFilters(
            String q,
            UUID operatorId,
            OrderStatus status) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (q != null && !q.isBlank()) {
                String term = q.trim();
                String like = "%" + term.toLowerCase() + "%";
                List<Predicate> searchPredicates = new ArrayList<>();
                searchPredicates.add(cb.like(cb.lower(root.get("customerName")), like));
                searchPredicates.add(cb.like(cb.lower(root.get("customerPhone")), like));
                searchPredicates.add(cb.like(
                        cb.lower(root.join("emuShipment", JoinType.LEFT).get("trackingNumber")), like));

                try {
                    searchPredicates.add(cb.equal(root.get("id"), UUID.fromString(term)));
                } catch (IllegalArgumentException ignored) {
                    // Oddiy matn UUID bo'lmasa ism/telefon/trek-raqam bo'yicha qidirish davom etadi.
                }

                predicates.add(cb.or(searchPredicates.toArray(new Predicate[0])));
            }

            if (operatorId != null) {
                predicates.add(cb.equal(root.get("operator").get("id"), operatorId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
