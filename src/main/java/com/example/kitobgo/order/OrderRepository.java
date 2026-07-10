package com.example.kitobgo.order;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    /** Buyurtmalarni items va ularning mahsuloti bilan birga (N+1 siz) yuklaydi. */
    @Override
    @EntityGraph(attributePaths = {"items", "items.product"})
    List<Order> findAll();

    /** Bitta buyurtmani items va mahsuloti bilan birga (N+1 siz) yuklaydi. */
    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Order> findWithItemsById(UUID id);
}
