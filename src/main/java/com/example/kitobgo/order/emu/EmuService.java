package com.example.kitobgo.order.emu;

import com.example.kitobgo.common.ConflictException;
import com.example.kitobgo.common.ForbiddenException;
import com.example.kitobgo.common.NotFoundException;
import com.example.kitobgo.order.DeliveryMethod;
import com.example.kitobgo.order.Order;
import com.example.kitobgo.order.OrderRepository;
import com.example.kitobgo.order.OrderStatus;
import com.example.kitobgo.order.dto.OrderResponseDto;
import com.example.kitobgo.user.Role;
import com.example.kitobgo.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * EMU bo'limi: pochtaga topshiriladigan buyurtmalar ro'yxati, ularni tasdiqlab Excel'ga
 * chiqarish va trek-raqam kiritish. Hammasi faqat ADMIN/SUPER_ADMIN uchun.
 * <p>
 * Oqim: admin ro'yxatni ko'zdan kechiradi → eksport qiladi (shu paytda har buyurtmaga
 * {@link EmuShipment} yaratiladi va pasilka nomi muzlatiladi) → Excel EMU ga topshiriladi
 * → EMU bergan trek-raqamni admin kiritadi. Keyinchalik oxirgi ikki qadam API orqali
 * avtomatlashtirilsa, shu servisning ichi o'zgaradi, tashqi shartnomasi emas.
 */
@Service
@RequiredArgsConstructor
public class EmuService {

    private final OrderRepository orderRepository;
    private final EmuExcelWriter excelWriter;

    /**
     * EMU bo'limi ro'yxati — EMU orqali ketadigan, operator tasdiqlagan va hali
     * topshirilmagan buyurtmalar. Har birida pasilka nomining avtomatik taxmini bo'ladi
     * ({@code emu.parcelName}).
     */
    @Transactional(readOnly = true)
    public List<OrderResponseDto> pending(User actor) {
        assertAdmin(actor);
        return findPending().stream()
                .map(OrderResponseDto::from)
                .toList();
    }

    /**
     * Buyurtmalarni EMU ga topshirilgan deb belgilaydi va Excel fayl qaytaradi.
     * {@code orderIds} berilsa — faqat o'shalar (admin ro'yxatdan tanlaganlari),
     * bo'sh/null bo'lsa — ro'yxatdagi hammasi.
     * <p>
     * Har buyurtmaga pasilka yozuvi yaratiladi: nomi shu paytda hisoblanadi va muzlaydi,
     * shu sababli keyin kitob sarlavhasi tahrirlansa ham EMU dagi nom bilan mos qoladi.
     * Yozuvning o'zi buyurtmani ro'yxatdan chiqaradi — ikkinchi marta eksport bo'lmaydi.
     */
    @Transactional
    public ExcelFile export(List<UUID> orderIds, User actor) {
        assertAdmin(actor);

        List<Order> orders = (orderIds == null || orderIds.isEmpty())
                ? findPending()
                : findPendingByIds(orderIds);

        if (orders.isEmpty()) {
            throw new ConflictException("Eksport qilinadigan EMU buyurtmasi yo'q");
        }

        LocalDateTime now = LocalDateTime.now();
        for (Order order : orders) {
            order.attachEmuShipment(EmuShipment.builder()
                    .parcelName(EmuParcelName.of(order))
                    .exportedAt(now)
                    .build());
        }
        orderRepository.saveAll(orders);   // cascade — pasilka yozuvlari ham saqlanadi

        return excelWriter.write(orders);
    }

    /**
     * EMU bergan trek-raqamni pasilkaga yozadi. Buyurtma avval eksport qilingan
     * (EMU ga topshirilgan) bo'lishi shart — trek-raqam pochtadan keyin paydo bo'ladi.
     */
    @Transactional
    public OrderResponseDto setTrackingNumber(UUID orderId, String trackingNumber, User actor) {
        assertAdmin(actor);

        Order order = orderRepository.findWithItemsById(orderId)
                .orElseThrow(() -> new NotFoundException("Buyurtma topilmadi: " + orderId));

        EmuShipment shipment = order.getEmuShipment();
        if (shipment == null) {
            throw new ConflictException(order.getDeliveryMethod() == DeliveryMethod.EMU
                    ? "Buyurtma hali EMU ga topshirilmagan — avval eksport qiling"
                    : "Trek-raqam faqat EMU buyurtmalari uchun");
        }

        shipment.setTrackingNumber(trackingNumber);
        return OrderResponseDto.from(orderRepository.save(order));
    }

    private List<Order> findPending() {
        return orderRepository.findByDeliveryMethodAndStatusAndEmuShipmentIsNullOrderByCreatedAtAsc(
                DeliveryMethod.EMU, OrderStatus.CONFIRMED);
    }

    /**
     * Berilgan id'larni ro'yxatdagilar ichidan tanlaydi. Ro'yxatda yo'q id — xato:
     * buyurtma topilmagan, EMU emas, tasdiqlanmagan yoki allaqachon topshirilgan bo'lishi
     * mumkin, va bularning hech biri jimgina o'tkazib yuborilmasligi kerak.
     */
    private List<Order> findPendingByIds(List<UUID> orderIds) {
        List<Order> pending = findPending();
        List<Order> selected = new ArrayList<>();
        for (UUID id : orderIds) {
            Order match = pending.stream()
                    .filter(o -> o.getId().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new ConflictException(
                            "Buyurtma EMU ro'yxatida yo'q (topilmadi, tasdiqlanmagan yoki "
                                    + "allaqachon topshirilgan): " + id));
            selected.add(match);
        }
        return selected;
    }

    private void assertAdmin(User actor) {
        Role role = actor.getRole();
        if (role != Role.ADMIN && role != Role.SUPER_ADMIN) {
            throw new ForbiddenException("EMU bo'limi faqat admin uchun");
        }
    }
}
