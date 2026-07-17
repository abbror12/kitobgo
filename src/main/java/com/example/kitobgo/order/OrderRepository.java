package com.example.kitobgo.order;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    /** Buyurtmalarni items, mahsulot, operator va kuryer bilan birga (N+1 siz) yuklaydi. */
    @Override
    @EntityGraph(attributePaths = {"items", "items.product", "operator", "courier", "emuShipment",
            "history", "history.changedBy"})
    List<Order> findAll();

    /** Bitta buyurtmani items, mahsulot, operator va kuryer bilan birga (N+1 siz) yuklaydi. */
    @EntityGraph(attributePaths = {"items", "items.product", "operator", "courier", "emuShipment",
            "history", "history.changedBy"})
    Optional<Order> findWithItemsById(UUID id);

    /** Operatorga biriktirilgan buyurtmalar (mobil ilova — "mening buyurtmalarim"). */
    @EntityGraph(attributePaths = {"items", "items.product", "operator", "courier", "emuShipment",
            "history", "history.changedBy"})
    List<Order> findByOperatorIdOrderByCreatedAtDesc(UUID operatorId);

    /** Kuryerga biriktirilgan buyurtmalar (mobil ilova — "mening yetkazishlarim"). */
    @EntityGraph(attributePaths = {"items", "items.product", "operator", "courier", "emuShipment",
            "history", "history.changedBy"})
    List<Order> findByCourierIdOrderByCreatedAtDesc(UUID courierId);

    /** Bitta operatorning ma'lum statusdagi buyurtmalari (admin panel / mobil filtr). */
    @EntityGraph(attributePaths = {"items", "items.product", "operator", "courier", "emuShipment",
            "history", "history.changedBy"})
    List<Order> findByOperatorIdAndStatusOrderByCreatedAtDesc(UUID operatorId, OrderStatus status);

    /** Bitta kuryerning ma'lum statusdagi yetkazishlari (mobil filtr). */
    @EntityGraph(attributePaths = {"items", "items.product", "operator", "courier", "emuShipment",
            "history", "history.changedBy"})
    List<Order> findByCourierIdAndStatusOrderByCreatedAtDesc(UUID courierId, OrderStatus status);

    /** Kuryerning faqat ko'rishga ruxsat etilgan statuslardagi yetkazishlari. */
    @EntityGraph(attributePaths = {"items", "items.product", "operator", "courier", "emuShipment",
            "history", "history.changedBy"})
    List<Order> findByCourierIdAndStatusInOrderByCreatedAtDesc(UUID courierId, Collection<OrderStatus> statuses);

    /** Ma'lum statusdagi barcha buyurtmalar (admin panel filtri). */
    @EntityGraph(attributePaths = {"items", "items.product", "operator", "courier", "emuShipment",
            "history", "history.changedBy"})
    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);

    /** Egasiz (operatorsiz) buyurtmalar hovuzi — eskidan yangiga (FIFO taqsimot uchun). */
    List<Order> findByOperatorIsNullOrderByCreatedAtAsc();

    /**
     * Marshrutsiz buyurtmalar — tasdiqlangan, lekin yetkazish turi hali yo'q. Amalda bu
     * Toshkent viloyati buyurtmalari: u yerda EMU ham, kuryer ham mumkin, shuning uchun
     * tizim taxmin qilmaydi va qaror adminga qoladi. Bu ro'yxatsiz ular hech qayerda
     * ko'rinmasdi — EMU bo'limiga ham tushmaydi, kuryer ham biriktirilmaydi.
     */
    @EntityGraph(attributePaths = {"items", "items.product", "operator", "courier", "emuShipment",
            "history", "history.changedBy"})
    List<Order> findByStatusAndDeliveryMethodIsNullOrderByCreatedAtAsc(OrderStatus status);

    /**
     * EMU bo'limi ro'yxati — pochtaga topshirishga tayyor, lekin hali topshirilmagan
     * buyurtmalar: EMU orqali ketadi, operator tasdiqlagan va hali pasilka yozuvi yo'q
     * ({@code emuShipment is null} = hali eksport qilinmagan). Eskidan yangiga.
     */
    @EntityGraph(attributePaths = {"items", "items.product", "operator", "courier", "emuShipment",
            "history", "history.changedBy"})
    List<Order> findByDeliveryMethodAndStatusAndEmuShipmentIsNullOrderByCreatedAtAsc(
            DeliveryMethod deliveryMethod, OrderStatus status);

    /**
     * Mavjud bo'lmagan operatorlarga biriktirilgan, hali tegilmagan (NEW) buyurtmalar.
     * "Mavjud emas" = operator offline (online null/false) yoki heartbeat'i eskirgan
     * (lastSeenAt null yoki threshold'dan eski). Bunday buyurtmalar hovuzga qaytariladi.
     * <p>
     * {@code source} filtri bilan faqat kerakli kanal (WEBSITE) buyurtmalari olinadi —
     * SMM (Instagram/Telegram) buyurtmalari avto-taqsimotga tushmasligi uchun.
     */
    @Query("""
            select o from Order o
            where o.status = :status
              and o.source = :source
              and o.operator is not null
              and (o.operator.online is null
                   or o.operator.online = false
                   or o.operator.lastSeenAt is null
                   or o.operator.lastSeenAt < :threshold)
            order by o.createdAt asc
            """)
    List<Order> findNewOrdersOfUnavailableOperators(
            @Param("status") OrderStatus status,
            @Param("source") OrderSource source,
            @Param("threshold") LocalDateTime threshold);
}
