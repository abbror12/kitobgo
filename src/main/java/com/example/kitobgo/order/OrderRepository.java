package com.example.kitobgo.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Bitta buyurtmani o'qiydigan metodlar {@link Order#DETAIL_GRAPH} bilan hamma bog'lanishni
 * (items, mahsulot, operator, kuryer, pasilka, tarix) birga N+1 siz yuklaydi.
 * <p>
 * Ro'yxatlar esa <b>ikki bosqichda</b> o'qiladi: avval graf<b>siz</b> {@code Page} so'rovi
 * (sahifalash SQL darajasida — {@code limit/offset}), keyin sahifadagi id'lar
 * {@link #findWithDetailByIdIn} orqali to'liq graf bilan yuklanadi. Collection fetch'li
 * graf {@code Pageable} bilan birga ishlatilsa Hibernate sahifalashni xotirada qiladi
 * (HHH90003004 — hamma qator baribir yuklanadi), shu sababli bu ikkisi ataylab ajratilgan.
 */
public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {

    @EntityGraph(Order.DETAIL_GRAPH)
    Optional<Order> findWithItemsById(UUID id);

    @EntityGraph(Order.DETAIL_GRAPH)
    Optional<Order> findByClientRequestId(UUID clientRequestId);

    /** 2-bosqich: sahifadagi buyurtmalarni to'liq graf bilan yuklash. */
    @EntityGraph(Order.DETAIL_GRAPH)
    List<Order> findWithDetailByIdIn(Collection<UUID> ids);

    /** Operatorga biriktirilgan buyurtmalar sahifasi (mobil ilova — "mening buyurtmalarim"). */
    Page<Order> findByOperatorId(UUID operatorId, Pageable pageable);

    /** Kuryerga biriktirilgan buyurtmalar sahifasi (mobil ilova — "mening yetkazishlarim"). */
    Page<Order> findByCourierId(UUID courierId, Pageable pageable);

    /** Bitta operatorning ma'lum statusdagi buyurtmalari (admin panel / mobil filtr). */
    Page<Order> findByOperatorIdAndStatus(UUID operatorId, OrderStatus status, Pageable pageable);

    /** Bitta kuryerning ma'lum statusdagi yetkazishlari (mobil filtr). */
    Page<Order> findByCourierIdAndStatus(UUID courierId, OrderStatus status, Pageable pageable);

    /** Kuryerning faqat ko'rishga ruxsat etilgan statuslardagi yetkazishlari. */
    Page<Order> findByCourierIdAndStatusIn(UUID courierId, Collection<OrderStatus> statuses, Pageable pageable);

    /** Ma'lum statusdagi barcha buyurtmalar sahifasi (admin panel filtri). */
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    /** Egasiz (operatorsiz) buyurtmalar hovuzi — eskidan yangiga (FIFO taqsimot uchun). */
    @Query(value = """
            select * from orders
            where operator_id is null
            order by created_at asc
            for update skip locked
            limit :batchSize
            """, nativeQuery = true)
    List<Order> lockNextUnassignedBatch(@Param("batchSize") int batchSize);

    /**
     * Marshrutsiz buyurtmalar — tasdiqlangan, lekin yetkazish turi hali yo'q. Amalda bu
     * Toshkent viloyati buyurtmalari: u yerda EMU ham, kuryer ham mumkin, shuning uchun
     * tizim taxmin qilmaydi va qaror adminga qoladi. Bu ro'yxatsiz ular hech qayerda
     * ko'rinmasdi — EMU bo'limiga ham tushmaydi, kuryer ham biriktirilmaydi.
     */
    @EntityGraph(Order.DETAIL_GRAPH)
    List<Order> findByStatusAndDeliveryMethodIsNullOrderByCreatedAtAsc(OrderStatus status);

    /**
     * EMU bo'limi ro'yxati — pochtaga topshirishga tayyor, lekin hali topshirilmagan
     * buyurtmalar: EMU orqali ketadi, operator tasdiqlagan va hali pasilka yozuvi yo'q
     * ({@code emuShipment is null} = hali eksport qilinmagan). Eskidan yangiga.
     */
    @EntityGraph(Order.DETAIL_GRAPH)
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
    @Query(value = """
            select o.*
            from orders o
            join users u on u.id = o.operator_id
            where o.status = 'NEW'
              and o.source = 'WEBSITE'
              and (u.online is null
                   or u.online = false
                   or u.last_seen_at is null
                   or u.last_seen_at < :threshold)
            order by o.created_at asc
            for update of o skip locked
            limit :batchSize
            """, nativeQuery = true)
    List<Order> lockUnavailableWebsiteOrders(
            @Param("threshold") LocalDateTime threshold,
            @Param("batchSize") int batchSize);
}
