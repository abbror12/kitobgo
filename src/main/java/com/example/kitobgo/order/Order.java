package com.example.kitobgo.order;

import com.example.kitobgo.order.emu.EmuShipment;
import com.example.kitobgo.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * {@code Order.detail} — buyurtmani API javobi ({@code OrderResponseDto}) uchun kerak
 * bo'lgan hamma bog'lanish bilan birga (N+1 siz) yuklaydigan graf. Repository'dagi
 * o'qish metodlari {@code @EntityGraph(Order.DETAIL_GRAPH)} bilan shuni ishlatadi.
 */
@NamedEntityGraph(
        name = Order.DETAIL_GRAPH,
        attributeNodes = {
                @NamedAttributeNode(value = "items", subgraph = "items"),
                @NamedAttributeNode("operator"),
                @NamedAttributeNode("courier"),
                @NamedAttributeNode("emuShipment"),
                @NamedAttributeNode(value = "history", subgraph = "history")
        },
        subgraphs = {
                @NamedSubgraph(name = "items", attributeNodes = @NamedAttributeNode("product")),
                @NamedSubgraph(name = "history", attributeNodes = @NamedAttributeNode("changedBy"))
        }
)
@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    /** {@link NamedEntityGraph} nomi — repository metodlarida ishlatiladi. */
    public static final String DETAIL_GRAPH = "Order.detail";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Sayt checkout retry'larini duplicate orderga aylantirmaydigan client kaliti. */
    @Column(unique = true)
    private UUID clientRequestId;

    /** Bir kalit boshqa payload bilan qayta ishlatilganini aniqlash uchun SHA-256. */
    @Column(length = 64)
    private String clientRequestHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operator_id")
    private User operator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "courier_id")
    private User courier;

    /**
     * Buyurtma qatorlari.
     * <p>
     * {@code Set} — {@code List} emas, va bu majburiy: {@code items} bilan {@code history}
     * bir so'rovda fetch qilinganda SQL ularning dekart ko'paytmasini qaytaradi. Hibernate
     * {@code Set}'ni dedupe qiladi, {@code List}'ni (bag) esa yo'q — bag bo'lganda har item
     * tarix qatorlari soniga karrali takrorlanib, {@code totalPrice} va zaxira hisobi
     * jimgina buziladi.
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<OrderItem> items = new LinkedHashSet<>();

    private String customerName;

    private String customerPhone;

    // --- Yetkazish manzili ---
    //
    // To'liq manzil uchta maydondan yig'iladi — alohida "address" (ko'cha/uy) maydoni YO'Q,
    // chunki amalda mijozlar ko'cha nomini emas, mo'ljalni aytadi:
    //
    //   region    SAMARKAND          -> "Samarqand viloyati"   (mijoz saytda tanlaydi)
    //   district  "Samarqand sh."    -> tuman                  (operator qo'ng'iroqda yozadi)
    //   landmark  "5-maktab yonida"  -> mo'ljal                (operator qo'ng'iroqda yozadi)
    //
    // Kuryer/pochta ko'radigani: "Samarqand viloyati, Samarqand sh., 5-maktab yonida".
    // Uchalasi ham buyurtmani tasdiqlash uchun shart (OrderDeliveryService.assertDeliverable) —
    // mo'ljalsiz manzil "Samarqand viloyati, Samarqand sh." bo'lib qoladi, bunga yetkazib
    // bo'lmaydi.

    /** Tuman — masalan "Chilonzor" yoki "Samarqand sh.". Operator qo'ng'iroqda to'ldiradi. */
    private String district;

    /**
     * Mo'ljal — yetkazish nuqtasini topish uchun yagona tafsilot: mijozning uyi yoki
     * yaqinidagi bog'cha/maktab kabi orientir ("5-maktab yonida", "12-uy").
     * Operator qo'ng'iroqda to'ldiradi.
     */
    private String landmark;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    /**
     * Buyurtma manbasi (kanal). {@code WEBSITE} — sayt (round-robin taqsimot);
     * {@code SOCIAL_NETWORK} — SMM manager qo'lda yaratgan (yaratganda o'ziga qoladi).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OrderSource source = OrderSource.WEBSITE;

    /**
     * Yetkazish viloyati — checkout'da mijoz ro'yxatdan tanlaydi. Operator qo'ng'iroqdan
     * keyin to'g'rilashi mumkin; o'zgarsa marshrut qayta hisoblanadi.
     */
    @Enumerated(EnumType.STRING)
    private Region region;

    /**
     * Yetkazish turi. Buyurtma tasdiqlanganda {@link Region#autoRoute()} orqali qo'yiladi,
     * shungacha {@code null} — marshrut hali ma'lum emas.
     * <p>
     * Tasdiqlangandan keyin ham {@code null} bo'lishi mumkin: Toshkent viloyatida ikkala tur
     * ham mumkin, qarorni admin qabul qiladi (marshrutsiz buyurtmalar ro'yxati orqali).
     */
    @Enumerated(EnumType.STRING)
    private DeliveryMethod deliveryMethod;

    /**
     * EMU pasilkasi — admin buyurtmani ko'zdan kechirib eksport qilganda yaratiladi
     * (COURIER buyurtmalarida va hali eksport qilinmagan EMU buyurtmalarida null).
     * Trek-raqam va pasilka nomi shu yerda.
     */
    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private EmuShipment emuShipment;

    /**
     * Status o'zgarishlari tarixi — eng eskidan yangiga. Har o'tish alohida qator,
     * hech narsa ustiga yozilmaydi ({@link OrderStatusChange}).
     * <p>
     * {@code Set} — {@code List} emas: {@code items} allaqachon bag (indekssiz {@code List}),
     * ikkinchi bag'ni bir vaqtda fetch qilganda Hibernate {@code MultipleBagFetchException}
     * otadi. {@code @OrderBy} bilan Hibernate {@code LinkedHashSet} ishlatadi va tartib saqlanadi.
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("changedAt ASC")
    @Builder.Default
    private Set<OrderStatusChange> history = new LinkedHashSet<>();

    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = OrderStatus.NEW;
        }
        if (this.source == null) {
            this.source = OrderSource.WEBSITE;
        }
        // deliveryMethod ataylab qo'yilmaydi — marshrut tasdiqlanganda aniqlanadi.
    }

    /** Buyurtma qatorini qo'shib, ikki tomonlama bog'lanishni o'rnatadi. */
    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    /** EMU pasilkasini biriktirib, ikki tomonlama bog'lanishni o'rnatadi. */
    public void attachEmuShipment(EmuShipment shipment) {
        this.emuShipment = shipment;
        shipment.setOrder(this);
    }

    /**
     * Statusni o'zgartirib, tarixga yozib qo'yadi — status va tarix hech qachon
     * bir-biridan ajralib qolmasligi uchun ikkalasi shu yerda birga bajariladi.
     *
     * @param actor o'zgartirgan xodim; sayt checkout'i uchun {@code null}
     */
    public void changeStatus(OrderStatus newStatus, User actor) {
        this.status = newStatus;
        history.add(OrderStatusChange.builder()
                .order(this)
                .status(newStatus)
                .changedBy(actor)
                .changedAt(LocalDateTime.now())
                .build());
    }
}
