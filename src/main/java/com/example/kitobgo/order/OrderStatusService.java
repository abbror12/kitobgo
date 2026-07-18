package com.example.kitobgo.order;

import com.example.kitobgo.common.ConflictException;
import com.example.kitobgo.common.NotFoundException;
import com.example.kitobgo.notification.PushNotificationService;
import com.example.kitobgo.order.dto.OrderResponseDto;
import com.example.kitobgo.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

/**
 * Buyurtma hayot sikli (statuslar) boshqaruvi: o'tishlar, yakuniy statuslar quloflari
 * va status o'zgarishiga bog'liq oqibatlar (zaxira qaytishi, marshrut tug'ilishi, push).
 */
@Service
@RequiredArgsConstructor
public class OrderStatusService {

    /**
     * Buyurtma hayoti shu statuslarning birida tugaydi — ulardan boshqa statusga
     * o'tib bo'lmaydi. Bu qulf zaxira hisobini ham himoya qiladi: {@code CANCELLED} va
     * {@code RETURNED} ga faqat bir marta kirilgani uchun kitoblar ikki marta
     * qaytarilmaydi (masalan {@code RETURNED -> CANCELLED} yo'li yopiq).
     */
    private static final Set<OrderStatus> TERMINAL_STATUSES =
            Set.of(OrderStatus.DELIVERED, OrderStatus.CANCELLED, OrderStatus.RETURNED);

    /** Kitoblar zaxiraga qaytadigan statuslar — buyurtma amalga oshmadi. */
    private static final Set<OrderStatus> RESTOCK_STATUSES =
            Set.of(OrderStatus.CANCELLED, OrderStatus.RETURNED);

    private final OrderRepository orderRepository;
    private final OrderAuthorizationPolicy authorizationPolicy;
    private final OrderStockService stockService;
    private final OrderDeliveryService deliveryService;
    private final PushNotificationService pushNotificationService;

    /**
     * Buyurtma statusini o'zgartiradi.
     * Ruxsat: ADMIN/SUPER_ADMIN har qanday buyurtmani; OPERATOR/COURIER faqat o'ziga
     * biriktirilgan buyurtmani va faqat o'z roliga ruxsat etilgan statuslarga
     * ({@link OrderAuthorizationPolicy#assertStatusAllowedForRole}).
     * <p>
     * Yakuniy statusdagi ({@link #TERMINAL_STATUSES}) buyurtma qulflanadi — undan chiqib
     * bo'lmaydi, hatto admin ham. {@code CANCELLED}/{@code RETURNED} ga o'tilganda kitoblar
     * zaxiraga qaytariladi; yakuniy statusga faqat bir marta kirish mumkin bo'lgani uchun
     * zaxira ikki marta qaytib qolmaydi.
     */
    @Transactional
    public OrderResponseDto changeStatus(UUID orderId, OrderStatus newStatus, User actor) {
        Order order = orderRepository.findWithItemsById(orderId)
                .orElseThrow(() -> new NotFoundException("Buyurtma topilmadi: " + orderId));

        authorizationPolicy.assertCanManage(order, actor);
        authorizationPolicy.assertStatusAllowedForRole(actor.getRole(), newStatus);
        assertNotTerminal(order.getStatus());

        if (newStatus == OrderStatus.CONFIRMED) {
            deliveryService.assertDeliverable(order);
            // Marshrut aynan shu yerda tug'iladi: manzil endi ma'lum va viloyat aniq.
            // Toshkent viloyatida ikkala tur ham mumkin — autoRoute null qaytaradi va
            // buyurtma marshrutsiz qoladi (admin tanlaguncha).
            order.setDeliveryMethod(order.getRegion().autoRoute());
        }

        if (RESTOCK_STATUSES.contains(newStatus)) {
            stockService.restoreStock(order);
        }
        order.changeStatus(newStatus, actor);   // statusni o'rnatadi + tarixga yozadi
        Order saved = orderRepository.save(order);
        notifyOrderParticipants(saved, newStatus, actor);
        return OrderResponseDto.from(saved);
    }

    /** Yakuniy statusdagi buyurtma qulflangan — undan chiqib bo'lmaydi. */
    private void assertNotTerminal(OrderStatus current) {
        if (TERMINAL_STATUSES.contains(current)) {
            throw new ConflictException(current + " — yakuniy status; buyurtmani boshqa "
                    + "statusga o'tkazib bo'lmaydi. Kerak bo'lsa yangi buyurtma yarating");
        }
    }

    /** Status o'zgarganda biriktirilgan operator/kuryerga push yuboradi (o'zgartirgan shaxsdan tashqari). */
    private void notifyOrderParticipants(Order order, OrderStatus newStatus, User actor) {
        User operator = order.getOperator();
        if (operator != null && !authorizationPolicy.isSameUser(operator, actor)) {
            pushNotificationService.notifyOrderUpdated(operator, order, newStatus);
        }
        User courier = order.getCourier();
        if (courier != null && !authorizationPolicy.isSameUser(courier, actor)) {
            pushNotificationService.notifyOrderUpdated(courier, order, newStatus);
        }
    }
}
