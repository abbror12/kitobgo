package com.example.kitobgo.order;

import com.example.kitobgo.common.NotFoundException;
import com.example.kitobgo.common.PagedResponse;
import com.example.kitobgo.order.dto.OrderResponseDto;
import com.example.kitobgo.order.dto.RegionResponse;
import com.example.kitobgo.user.Role;
import com.example.kitobgo.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Buyurtmalarni o'qish (read-only) yo'llari: admin ro'yxatlari, xodimning "o'z"
 * buyurtmalari va ma'lumotnomalar (viloyatlar). Hech narsani o'zgartirmaydi.
 */
@Service
@RequiredArgsConstructor
public class OrderQueryService {

    private final OrderRepository orderRepository;
    private final OrderAuthorizationPolicy authorizationPolicy;

    /** Operator/admin paneli uchun viloyatlar ro'yxati (har biriga mumkin bo'lgan turlar bilan). */
    @Transactional(readOnly = true)
    public List<RegionResponse> regions() {
        return Arrays.stream(Region.values())
                .map(RegionResponse::from)
                .toList();
    }

    /**
     * Marshrutsiz buyurtmalar (admin paneli) — tasdiqlangan, lekin yetkazish turi hali
     * tanlanmagan. Admin har biri uchun EMU yoki kuryerni belgilaydi
     * ({@code PATCH /api/orders/{id}/address} + {@code deliveryMethod}).
     */
    @Transactional(readOnly = true)
    public List<OrderResponseDto> unrouted(User actor) {
        authorizationPolicy.assertAdmin(actor);
        return orderRepository.findByStatusAndDeliveryMethodIsNullOrderByCreatedAtAsc(OrderStatus.CONFIRMED)
                .stream()
                .map(OrderResponseDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponseDto getById(UUID id) {
        Order order = orderRepository.findWithItemsById(id)
                .orElseThrow(() -> new NotFoundException("Buyurtma topilmadi: " + id));
        return OrderResponseDto.from(order);
    }

    /**
     * Xodimning o'ziga biriktirilgan (u egalik qiladigan) buyurtmalari sahifasi;
     * status — ixtiyoriy filtr. Operator ham, SMM manager ham {@code operator} maydonida
     * saqlanadi — shu metod ikkalasiga xizmat qiladi.
     */
    @Transactional(readOnly = true)
    public PagedResponse<OrderResponseDto> getMyOwnedOrders(User actor, OrderStatus status, Pageable pageable) {
        Page<Order> page = status != null
                ? orderRepository.findByOperatorIdAndStatus(actor.getId(), status, pageable)
                : orderRepository.findByOperatorId(actor.getId(), pageable);
        return toDetailedPage(page);
    }

    /**
     * Kuryerning o'ziga biriktirilgan buyurtmalari sahifasi (mobil ilova); status — ixtiyoriy filtr.
     * Kuryer faqat yetkazish bosqichidagi statuslarni ko'radi
     * ({@link OrderAuthorizationPolicy#COURIER_VISIBLE_STATUSES}): NEW/CANCELLED/REPROCESSING
     * dagi buyurtmalar ro'yxatga kirmaydi, ular bo'yicha filtrlashga urinish esa 403 qaytaradi.
     */
    @Transactional(readOnly = true)
    public PagedResponse<OrderResponseDto> getMyCourierOrders(User actor, OrderStatus status, Pageable pageable) {
        Page<Order> page;
        if (status != null) {
            authorizationPolicy.assertCourierVisibleStatus(status);
            page = orderRepository.findByCourierIdAndStatus(actor.getId(), status, pageable);
        } else {
            page = orderRepository.findByCourierIdAndStatusIn(
                    actor.getId(), OrderAuthorizationPolicy.COURIER_VISIBLE_STATUSES, pageable);
        }
        return toDetailedPage(page);
    }

    /**
     * Bitta buyurtma — faqat unga biriktirilgan operator/kuryer (yoki admin) ko'ra oladi.
     * Kuryer uchun qo'shimcha: buyurtma ko'rinmas statusda (NEW/CANCELLED/REPROCESSING)
     * bo'lsa — 403.
     */
    @Transactional(readOnly = true)
    public OrderResponseDto getMyOrder(UUID orderId, User actor) {
        Order order = orderRepository.findWithItemsById(orderId)
                .orElseThrow(() -> new NotFoundException("Buyurtma topilmadi: " + orderId));
        authorizationPolicy.assertCanManage(order, actor);
        if (actor.getRole() == Role.COURIER) {
            authorizationPolicy.assertCourierVisibleStatus(order.getStatus());
        }
        return OrderResponseDto.from(order);
    }

    /**
     * Buyurtmalar sahifasi (admin panel). Ikkala filtr ham ixtiyoriy:
     * operatorId — bitta operatorning buyurtmalari, status — ma'lum statusdagilar;
     * ikkalasi berilsa — kesishmasi, hech biri berilmasa — hammasi.
     */
    @Transactional(readOnly = true)
    public PagedResponse<OrderResponseDto> getAll(UUID operatorId, OrderStatus status, Pageable pageable) {
        Page<Order> page;
        if (operatorId != null && status != null) {
            page = orderRepository.findByOperatorIdAndStatus(operatorId, status, pageable);
        } else if (operatorId != null) {
            page = orderRepository.findByOperatorId(operatorId, pageable);
        } else if (status != null) {
            page = orderRepository.findByStatus(status, pageable);
        } else {
            page = orderRepository.findAll(pageable);
        }
        return toDetailedPage(page);
    }

    /**
     * Sahifadagi buyurtmalarni to'liq graf bilan qayta yuklab, DTO sahifasiga aylantiradi
     * (2-bosqich). Sahifalash allaqachon SQL'da bo'lgan — bu yerda faqat sahifadagi
     * id'lar uchun bog'lanishlar bitta so'rovda olinadi; tartib birinchi so'rovdagidek qoladi.
     */
    private PagedResponse<OrderResponseDto> toDetailedPage(Page<Order> page) {
        if (page.isEmpty()) {
            return PagedResponse.from(page.map(OrderResponseDto::from));
        }
        List<UUID> ids = page.getContent().stream().map(Order::getId).toList();
        Map<UUID, Order> detailed = orderRepository.findWithDetailByIdIn(ids).stream()
                .collect(Collectors.toMap(Order::getId, Function.identity()));
        return PagedResponse.from(page.map(order -> OrderResponseDto.from(detailed.get(order.getId()))));
    }
}
