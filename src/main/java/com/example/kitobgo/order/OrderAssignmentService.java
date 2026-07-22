package com.example.kitobgo.order;

import com.example.kitobgo.common.ConflictException;
import com.example.kitobgo.common.NotFoundException;
import com.example.kitobgo.notification.PushNotificationService;
import com.example.kitobgo.order.assignment.OperatorAssignmentStrategy;
import com.example.kitobgo.order.dto.OrderResponseDto;
import com.example.kitobgo.presence.Availability;
import com.example.kitobgo.user.Role;
import com.example.kitobgo.user.User;
import com.example.kitobgo.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Buyurtmani xodimlarga biriktirish: kuryer tanlash/yechish (admin qo'lda) va
 * operatorlar o'rtasida avto-taqsimot (rebalance).
 */
@Service
@RequiredArgsConstructor
public class OrderAssignmentService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderAuthorizationPolicy authorizationPolicy;
    private final OperatorAssignmentStrategy operatorAssignmentStrategy;
    private final Availability availability;
    private final PushNotificationService pushNotificationService;

    @Value("${app.order.rebalance-batch-size:100}")
    private int rebalanceBatchSize;

    /**
     * Buyurtmaga kuryer biriktiradi.
     * Ruxsat: faqat ADMIN/SUPER_ADMIN va faqat {@code CONFIRMED} buyurtmaga
     * ({@link #assertCourierAssignable}).
     */
    @Transactional
    public OrderResponseDto assignCourier(UUID orderId, UUID courierId, User actor) {
        Order order = orderRepository.findWithItemsById(orderId)
                .orElseThrow(() -> new NotFoundException("Buyurtma topilmadi: " + orderId));

        authorizationPolicy.assertCanAssignCourier(actor);
        assertCourierAssignable(order.getStatus());

        // Kuryer faqat marshruti COURIER bo'lgan buyurtmaga biriktiriladi. Marshrut
        // tasdiqlanganda paydo bo'ladi, ya'ni bu tekshiruv NEW buyurtmani ham qamrab oladi.
        if (order.getDeliveryMethod() != DeliveryMethod.COURIER) {
            throw new ConflictException(order.getDeliveryMethod() == null
                    ? "Buyurtma marshruti hali belgilanmagan — avval tasdiqlang, "
                            + "Toshkent viloyati bo'lsa yetkazish turini tanlang"
                    : "Bu buyurtma EMU (pochta) orqali yetkaziladi — kuryer biriktirib bo'lmaydi");
        }

        User courier = userRepository.findById(courierId)
                .orElseThrow(() -> new NotFoundException("Kuryer topilmadi: " + courierId));
        if (courier.getRole() != Role.COURIER) {
            throw new IllegalArgumentException("Tanlangan foydalanuvchi kuryer emas");
        }

        order.setCourier(courier);
        Order saved = orderRepository.save(order);
        pushNotificationService.notifyNewDelivery(courier, saved);
        return OrderResponseDto.from(saved);
    }

    /**
     * Buyurtmadan kuryerni yechadi (masalan viloyat EMU'ga o'zgarishidan oldin). Faqat admin
     * va faqat {@code CONFIRMED} buyurtmada — kuryer yo'lga chiqqach almashtirib bo'lmaydi
     * ({@link #assertCourierAssignable}).
     */
    @Transactional
    public OrderResponseDto unassignCourier(UUID orderId, User actor) {
        Order order = orderRepository.findWithItemsById(orderId)
                .orElseThrow(() -> new NotFoundException("Buyurtma topilmadi: " + orderId));

        authorizationPolicy.assertAdmin(actor);
        assertCourierAssignable(order.getStatus());
        order.setCourier(null);
        return OrderResponseDto.from(orderRepository.save(order));
    }

    /**
     * Buyurtma taqsimotini qayta muvozanatlaydi (operator online/offline/heartbeat bo'lganda chaqiriladi):
     * <ol>
     *   <li>Mavjud bo'lmagan operatorlarning tegilmagan (NEW) buyurtmalarini egasiz hovuzga qaytaradi;</li>
     *   <li>Hovuzdagi buyurtmalarni ayni damda mavjud operatorlarga tarqatadi.</li>
     * </ol>
     * Bu offline'ni ham, ilova qulab tushgan (heartbeat eskirgan) holatni ham qamraydi —
     * kimdir faol bo'lsa, egasiz buyurtmalar qayta taqsimlanadi.
     */
    @Transactional
    public void rebalance() {
        LocalDateTime threshold = availability.threshold();

        // 1) Mavjud bo'lmagan operatorlarning NEW buyurtmalarini hovuzga qaytar.
        //    Faqat WEBSITE buyurtmalari — SMM (Instagram/Telegram) buyurtmalari yaratgan
        //    xodimga biriktirilib qoladi, avto-taqsimotga tushmaydi.
        orderRepository.lockUnavailableWebsiteOrders(threshold, rebalanceBatchSize)
                .forEach(order -> order.setOperator(null));

        // 2) Hovuzdagilarni mavjud operatorlarga tarqat (round-robin strategiya orqali).
        List<Order> orders = orderRepository.lockNextUnassignedBatch(rebalanceBatchSize);
        List<User> operators = operatorAssignmentStrategy.assignOperators(orders.size());
        for (int index = 0; index < operators.size(); index++) {
            Order order = orders.get(index);
            User operator = operators.get(index);
            if (operator == null) {
                break;   // hozircha mavjud operator yo'q — hovuzda kutaversin
            }
            order.setOperator(operator);
            pushNotificationService.notifyNewOrder(operator, order);
        }
    }

    /**
     * Kuryer faqat tasdiqlangan buyurtmada tanlanadi/almashtiriladi.
     * <p>
     * Oyna ataylab tor: {@code CONFIRMED}gacha (NEW/REPROCESSING) manzil ham, marshrut ham
     * hali aniq emas — kimga berishni bilib bo'lmaydi; {@code IN_DELIVERY}dan boshlab esa
     * kuryer allaqachon kitoblarni olib yo'lga chiqqan — uni almashtirish ikki kuryerni bir
     * buyurtmaga qo'yadi va yetkazganini tarixdan o'chiradi. Kuryerni haqiqatan ham
     * almashtirish kerak bo'lsa, buyurtma avval {@code REPROCESSING}ga qaytariladi va
     * qaytadan tasdiqlanadi.
     */
    private void assertCourierAssignable(OrderStatus status) {
        if (status != OrderStatus.CONFIRMED) {
            throw new ConflictException("Kuryerni faqat tasdiqlangan (CONFIRMED) buyurtmada "
                    + "tanlash mumkin — bu buyurtma hozir " + status + " statusida");
        }
    }
}
