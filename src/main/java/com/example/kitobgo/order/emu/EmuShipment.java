package com.example.kitobgo.order.emu;

import com.example.kitobgo.order.Order;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * EMU ga topshirilgan pasilka — admin buyurtmani ko'zdan kechirib tasdiqlaganda (eksport)
 * yaratiladi. Shu paytdan boshlab buyurtmaning EMU tomonidagi holati shu yerda yashaydi.
 * <p>
 * Shipment mavjudligining o'zi "bu buyurtma EMU ga topshirilgan" degani — EMU bo'limi
 * ro'yxati aynan shipment'i yo'q buyurtmalardan tuziladi.
 * <p>
 * {@code parcelName} yaratilishda {@link EmuParcelName} orqali avtomatik to'ldiriladi va
 * shu holida <b>muzlab qoladi</b>: Excel EMU ga ketgandan keyin kitob sarlavhasi tahrirlansa
 * ham pasilka nomi o'zgarmaydi (xuddi {@code OrderItem.priceAtPurchase} kabi).
 */
@Entity
@Table(name = "emu_shipments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmuShipment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    /** Pasilka nomi — eksport paytida buyurtma qatorlaridan avtomatik yasaladi va o'zgarmaydi. */
    @Column(nullable = false)
    private String parcelName;

    /** EMU trek-raqami — pochta bergach admin kiritadi (API integratsiyada avtomatik to'ladi). */
    private String trackingNumber;

    /** Excel yuklab olingan (EMU ga topshirilgan deb belgilangan) vaqt. */
    private LocalDateTime exportedAt;

    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
