package com.example.kitobgo.order;

import com.example.kitobgo.common.ConflictException;
import com.example.kitobgo.common.ForbiddenException;
import com.example.kitobgo.common.NotFoundException;
import com.example.kitobgo.order.assignment.OperatorAssignmentStrategy;
import com.example.kitobgo.order.dto.OrderItemRequest;
import com.example.kitobgo.order.dto.OrderRequestDto;
import com.example.kitobgo.order.dto.OrderResponseDto;
import com.example.kitobgo.presence.Availability;
import com.example.kitobgo.product.Product;
import com.example.kitobgo.product.ProductRepository;
import com.example.kitobgo.user.Role;
import com.example.kitobgo.user.User;
import com.example.kitobgo.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OperatorAssignmentStrategy operatorAssignmentStrategy;
    private final Availability availability;

    @Transactional
    public OrderResponseDto create(OrderRequestDto dto) {
        if (dto.items() == null || dto.items().isEmpty()) {
            throw new IllegalArgumentException("Buyurtmada kamida bitta mahsulot bo'lishi kerak");
        }

        User operator = operatorAssignmentStrategy.assignOperator();

        Order order = Order.builder()
                .operator(operator)
                .customerName(dto.customerName())
                .customerPhone(dto.customerPhone())
                .address(dto.address())
                .status(OrderStatus.NEW)
                .build();

        for (OrderItemRequest itemReq : dto.items()) {
            Product product = productRepository.findById(itemReq.productId())
                    .orElseThrow(() -> new NotFoundException("Mahsulot topilmadi: " + itemReq.productId()));

            int quantity = itemReq.quantity() != null ? itemReq.quantity() : 1;
            if (quantity <= 0) {
                throw new IllegalArgumentException("Miqdor musbat bo'lishi kerak: " + product.getTitle());
            }

            // Zaxirani tekshirish va kamaytirish. product managed obyekt bo'lgani uchun
            // stockQuantity o'zgarishi transaksiya yakunida avtomatik saqlanadi (dirty checking).
            Integer stock = product.getStockQuantity();
            int available = stock != null ? stock : 0;
            if (available < quantity) {
                throw new ConflictException("Yetarli zaxira yo'q: " + product.getTitle()
                        + " (mavjud: " + available + ", so'ralgan: " + quantity + ")");
            }
            product.setStockQuantity(available - quantity);

            OrderItem item = OrderItem.builder()
                    .product(product)
                    .quantity(quantity)
                    .priceAtPurchase(effectivePrice(product))
                    .build();
            order.addItem(item);
        }

        Order saved = orderRepository.save(order);
        return OrderResponseDto.from(saved);
    }

    /** Mahsulotning haqiqiy narxi: chegirma bo'lsa chegirma narxi, aks holda asl narx. */
    private Integer effectivePrice(Product product) {
        Integer price = product.getPrice();
        Integer discount = product.getDiscountPrice();
        return (discount != null && price != null && discount < price) ? discount : price;
    }

    /**
     * Buyurtma statusini o'zgartiradi (erkin o'tish — har qanday statusga).
     * Ruxsat: ADMIN/SUPER_ADMIN har qanday buyurtmani; OPERATOR/COURIER faqat
     * o'ziga biriktirilgan buyurtmani boshqara oladi.
     */
    @Transactional
    public OrderResponseDto changeStatus(UUID orderId, OrderStatus newStatus, User actor) {
        Order order = orderRepository.findWithItemsById(orderId)
                .orElseThrow(() -> new NotFoundException("Buyurtma topilmadi: " + orderId));

        assertCanManage(order, actor);

        order.setStatus(newStatus);
        stampStatusTime(order, newStatus);
        return OrderResponseDto.from(orderRepository.save(order));
    }

    /** Status o'zgarganda tegishli vaqt maydonini to'ldiradi (NEW uchun createdAt yetarli). */
    private void stampStatusTime(Order order, OrderStatus status) {
        LocalDateTime now = LocalDateTime.now();
        switch (status) {
            case CONFIRMED -> order.setConfirmedAt(now);
            case IN_DELIVERY -> order.setInDeliveryAt(now);
            case DELIVERED -> order.setDeliveredAt(now);
            case RETURNED -> order.setReturnedAt(now);
            case NEW -> { /* boshlang'ich holat — createdAt yetarli */ }
        }
    }

    /**
     * Buyurtmaga kuryer biriktiradi.
     * Ruxsat: ADMIN/SUPER_ADMIN har qanday buyurtmaga; biriktirilgan OPERATOR o'z buyurtmasiga.
     */
    @Transactional
    public OrderResponseDto assignCourier(UUID orderId, UUID courierId, User actor) {
        Order order = orderRepository.findWithItemsById(orderId)
                .orElseThrow(() -> new NotFoundException("Buyurtma topilmadi: " + orderId));

        assertCanAssignCourier(order, actor);

        User courier = userRepository.findById(courierId)
                .orElseThrow(() -> new NotFoundException("Kuryer topilmadi: " + courierId));
        if (courier.getRole() != Role.COURIER) {
            throw new IllegalArgumentException("Tanlangan foydalanuvchi kuryer emas");
        }

        order.setCourier(courier);
        return OrderResponseDto.from(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public OrderResponseDto getById(UUID id) {
        Order order = orderRepository.findWithItemsById(id)
                .orElseThrow(() -> new NotFoundException("Buyurtma topilmadi: " + id));
        return OrderResponseDto.from(order);
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
        orderRepository.findNewOrdersOfUnavailableOperators(OrderStatus.NEW, threshold)
                .forEach(order -> order.setOperator(null));

        // 2) Hovuzdagilarni mavjud operatorlarga tarqat (round-robin strategiya orqali).
        for (Order order : orderRepository.findByOperatorIsNullOrderByCreatedAtAsc()) {
            User operator = operatorAssignmentStrategy.assignOperator();
            if (operator == null) {
                break;   // hozircha mavjud operator yo'q — hovuzda kutaversin
            }
            order.setOperator(operator);
        }
    }

    /** Operatorning o'ziga biriktirilgan buyurtmalari (mobil ilova). */
    @Transactional(readOnly = true)
    public List<OrderResponseDto> getMyOperatorOrders(User actor) {
        return orderRepository.findByOperatorIdOrderByCreatedAtDesc(actor.getId()).stream()
                .map(OrderResponseDto::from)
                .toList();
    }

    /** Kuryerning o'ziga biriktirilgan buyurtmalari (mobil ilova). */
    @Transactional(readOnly = true)
    public List<OrderResponseDto> getMyCourierOrders(User actor) {
        return orderRepository.findByCourierIdOrderByCreatedAtDesc(actor.getId()).stream()
                .map(OrderResponseDto::from)
                .toList();
    }

    /** Bitta buyurtma — faqat unga biriktirilgan operator/kuryer (yoki admin) ko'ra oladi. */
    @Transactional(readOnly = true)
    public OrderResponseDto getMyOrder(UUID orderId, User actor) {
        Order order = orderRepository.findWithItemsById(orderId)
                .orElseThrow(() -> new NotFoundException("Buyurtma topilmadi: " + orderId));
        assertCanManage(order, actor);
        return OrderResponseDto.from(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDto> getAll() {
        return orderRepository.findAll().stream()
                .map(OrderResponseDto::from)
                .toList();
    }

    // --- Ruxsat tekshiruvi ---

    /** Statusni o'zgartira oladimi: admin — har doim; operator/kuryer — faqat o'z buyurtmasi. */
    private void assertCanManage(Order order, User actor) {
        Role role = actor.getRole();
        if (role == Role.ADMIN || role == Role.SUPER_ADMIN) {
            return;
        }
        if (role == Role.OPERATOR && isSameUser(order.getOperator(), actor)) {
            return;
        }
        if (role == Role.COURIER && isSameUser(order.getCourier(), actor)) {
            return;
        }
        throw new ForbiddenException("Bu buyurtmani boshqarishga ruxsatingiz yo'q");
    }

    /** Kuryer biriktira oladimi: admin — har doim; biriktirilgan operator — o'z buyurtmasi. */
    private void assertCanAssignCourier(Order order, User actor) {
        Role role = actor.getRole();
        if (role == Role.ADMIN || role == Role.SUPER_ADMIN) {
            return;
        }
        if (role == Role.OPERATOR && isSameUser(order.getOperator(), actor)) {
            return;
        }
        throw new ForbiddenException("Bu buyurtmaga kuryer biriktirishga ruxsatingiz yo'q");
    }

    private boolean isSameUser(User assigned, User actor) {
        return assigned != null && assigned.getId().equals(actor.getId());
    }
}
