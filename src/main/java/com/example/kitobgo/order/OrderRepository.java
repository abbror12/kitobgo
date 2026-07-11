package com.example.kitobgo.order;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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

    /** Operatorga biriktirilgan buyurtmalar (mobil ilova — "mening buyurtmalarim"). */
    @EntityGraph(attributePaths = {"items", "items.product"})
    List<Order> findByOperatorIdOrderByCreatedAtDesc(UUID operatorId);

    /** Kuryerga biriktirilgan buyurtmalar (mobil ilova — "mening yetkazishlarim"). */
    @EntityGraph(attributePaths = {"items", "items.product"})
    List<Order> findByCourierIdOrderByCreatedAtDesc(UUID courierId);

    /** Egasiz (operatorsiz) buyurtmalar hovuzi — eskidan yangiga (FIFO taqsimot uchun). */
    List<Order> findByOperatorIsNullOrderByCreatedAtAsc();

    /**
     * Mavjud bo'lmagan operatorlarga biriktirilgan, hali tegilmagan (NEW) buyurtmalar.
     * "Mavjud emas" = operator offline (online null/false) yoki heartbeat'i eskirgan
     * (lastSeenAt null yoki threshold'dan eski). Bunday buyurtmalar hovuzga qaytariladi.
     */
    @Query("""
            select o from Order o
            where o.status = :status
              and o.operator is not null
              and (o.operator.online is null
                   or o.operator.online = false
                   or o.operator.lastSeenAt is null
                   or o.operator.lastSeenAt < :threshold)
            order by o.createdAt asc
            """)
    List<Order> findNewOrdersOfUnavailableOperators(
            @Param("status") OrderStatus status, @Param("threshold") LocalDateTime threshold);
}
