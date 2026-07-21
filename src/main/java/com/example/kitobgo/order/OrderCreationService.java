package com.example.kitobgo.order;

import com.example.kitobgo.common.ConflictException;
import com.example.kitobgo.notification.PushNotificationService;
import com.example.kitobgo.order.assignment.OperatorAssignmentStrategy;
import com.example.kitobgo.order.dto.OrderItemRequest;
import com.example.kitobgo.order.dto.OrderRequestDto;
import com.example.kitobgo.order.dto.OrderResponseDto;
import com.example.kitobgo.order.dto.SmmOrderRequest;
import com.example.kitobgo.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Buyurtma yaratish yo'llari: sayt (checkout) va SMM lead. Har ikkalasi ham qatorlarni
 * {@link OrderStockService} orqali yaratadi (zaxira kamayadi, narx muzlaydi).
 */
@Service
@RequiredArgsConstructor
public class OrderCreationService {

    private final OrderRepository orderRepository;
    private final OperatorAssignmentStrategy operatorAssignmentStrategy;
    private final OrderStockService stockService;
    private final OrderDeliveryService deliveryService;
    private final PushNotificationService pushNotificationService;
    private final OrderIdempotencyLock idempotencyLock;

    /**
     * Sayt (checkout) buyurtmasi — ochiq endpoint. Manba {@code WEBSITE}, ayni damda
     * mavjud operatorga round-robin biriktiriladi (yo'q bo'lsa hovuzda kutadi).
     * <p>
     * Mijozdan faqat viloyat olinadi — manzilni operator qo'ng'iroqda to'ldiradi.
     * Marshrut ham bu yerda qo'yilmaydi: u tasdiqlanganda viloyatdan avtomatik chiqadi.
    */
    @Transactional
    public OrderCreationResult create(OrderRequestDto dto, UUID clientRequestId) {
        if (clientRequestId == null) {
            throw new IllegalArgumentException("Idempotency-Key header'i ko'rsatilishi shart");
        }
        requireItems(dto.items());

        String requestHash = OrderRequestFingerprint.sha256(dto);
        idempotencyLock.acquire(clientRequestId);

        Order existing = orderRepository.findByClientRequestId(clientRequestId).orElse(null);
        if (existing != null) {
            if (!requestHash.equals(existing.getClientRequestHash())) {
                throw new ConflictException(
                        "Idempotency-Key boshqa checkout ma'lumoti bilan oldin ishlatilgan");
            }
            return new OrderCreationResult(OrderResponseDto.from(existing), false);
        }

        User operator = operatorAssignmentStrategy.assignOperator();

        Order order = Order.builder()
                .operator(operator)
                .clientRequestId(clientRequestId)
                .clientRequestHash(requestHash)
                .source(OrderSource.WEBSITE)
                .region(dto.region())
                .customerName(dto.customerName())
                .customerPhone(dto.customerPhone())
                .build();
        // Sayt buyurtmasi — actor null: buni mijozning o'zi yaratdi, tizimda foydalanuvchisi yo'q.
        order.changeStatus(OrderStatus.NEW, null);
        stockService.populateItems(order, dto.items());

        Order saved = orderRepository.save(order);
        if (saved.getOperator() != null) {
            pushNotificationService.notifyNewOrder(saved.getOperator(), saved);
        }
        return new OrderCreationResult(OrderResponseDto.from(saved), true);
    }

    /**
     * SMM manager ijtimoiy tarmoq lead'idan qo'lda buyurtma yaratadi. Buyurtma yaratgan
     * xodimning o'ziga biriktiriladi va avto-taqsimotga (round-robin/rebalance)
     * <b>tushmaydi</b>. Manba har doim {@code SOCIAL_NETWORK}.
     * <p>
     * Buyurtma to'g'ridan-to'g'ri {@code CONFIRMED} yaratiladi — {@code NEW} bosqichi
     * o'tkazib yuboriladi. NEW'ning yagona vazifasi "operator qo'ng'iroq qilib manzilni
     * to'ldirsin" edi; SMM manager esa lead bilan chatda allaqachon gaplashgan va manzilni
     * to'liq kiritadi ({@link SmmOrderRequest} — hamma maydon majburiy). Shu sababli
     * marshrut ham shu yerda tug'iladi, xuddi {@link OrderStatusService#changeStatus} dagi
     * tasdiqlash yo'lidek.
     */
    @Transactional
    public OrderResponseDto createBySmm(SmmOrderRequest dto, User creator) {
        requireItems(dto.items());

        Order order = Order.builder()
                .operator(creator)
                .source(OrderSource.SOCIAL_NETWORK)
                .region(dto.region())
                .customerName(dto.customerName())
                .customerPhone(dto.customerPhone())
                .district(dto.district())
                .landmark(dto.landmark())
                .build();
        // DTO validatsiyasi maydonlarni allaqachon talab qiladi — bu esa "CONFIRMED buyurtma
        // har doim yetkazishga yaroqli" invariantining servis darajasidagi qulfi.
        deliveryService.assertDeliverable(order);
        // Toshkent viloyatida null qoladi — turni admin tanlaydi (marshrutsiz buyurtmalar).
        order.setDeliveryMethod(order.getRegion().autoRoute());
        // SMM buyurtmasini xodimning o'zi kiritdi — tarixda o'sha ko'rinadi.
        order.changeStatus(OrderStatus.CONFIRMED, creator);
        stockService.populateItems(order, dto.items());

        // Yaratuvchining o'ziga push yuborilmaydi — buyurtmani o'zi kiritdi.
        return OrderResponseDto.from(orderRepository.save(order));
    }

    private void requireItems(List<OrderItemRequest> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Buyurtmada kamida bitta mahsulot bo'lishi kerak");
        }
    }
}
