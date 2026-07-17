package com.example.kitobgo.order;

import com.example.kitobgo.common.ConflictException;
import com.example.kitobgo.common.ForbiddenException;
import com.example.kitobgo.common.NotFoundException;
import com.example.kitobgo.notification.PushNotificationService;
import com.example.kitobgo.order.assignment.OperatorAssignmentStrategy;
import com.example.kitobgo.order.dto.OrderItemRequest;
import com.example.kitobgo.order.dto.OrderRequestDto;
import com.example.kitobgo.order.dto.OrderResponseDto;
import com.example.kitobgo.order.dto.RegionResponse;
import com.example.kitobgo.order.dto.SmmOrderRequest;
import com.example.kitobgo.order.dto.UpdateDeliveryRequest;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OperatorAssignmentStrategy operatorAssignmentStrategy;
    private final Availability availability;
    private final PushNotificationService pushNotificationService;

    /**
     * Sayt (checkout) buyurtmasi — ochiq endpoint. Manba {@code WEBSITE}, ayni damda
     * mavjud operatorga round-robin biriktiriladi (yo'q bo'lsa hovuzda kutadi).
     * <p>
     * Mijozdan faqat viloyat olinadi — manzilni operator qo'ng'iroqda to'ldiradi.
     * Marshrut ham bu yerda qo'yilmaydi: u tasdiqlanganda viloyatdan avtomatik chiqadi.
     */
    @Transactional
    public OrderResponseDto create(OrderRequestDto dto) {
        requireItems(dto.items());

        User operator = operatorAssignmentStrategy.assignOperator();

        Order order = Order.builder()
                .operator(operator)
                .source(OrderSource.WEBSITE)
                .region(dto.region())
                .customerName(dto.customerName())
                .customerPhone(dto.customerPhone())
                .build();
        // Sayt buyurtmasi — actor null: buni mijozning o'zi yaratdi, tizimda foydalanuvchisi yo'q.
        order.changeStatus(OrderStatus.NEW, null);
        populateItems(order, dto.items());

        Order saved = orderRepository.save(order);
        if (saved.getOperator() != null) {
            pushNotificationService.notifyNewOrder(saved.getOperator(), saved);
        }
        return OrderResponseDto.from(saved);
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
     * marshrut ham shu yerda tug'iladi, xuddi {@link #changeStatus} dagi tasdiqlash yo'lidek.
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
        assertDeliverable(order);
        // Toshkent viloyatida null qoladi — turni admin tanlaydi (marshrutsiz buyurtmalar).
        order.setDeliveryMethod(order.getRegion().autoRoute());
        // SMM buyurtmasini xodimning o'zi kiritdi — tarixda o'sha ko'rinadi.
        order.changeStatus(OrderStatus.CONFIRMED, creator);
        populateItems(order, dto.items());

        // Yaratuvchining o'ziga push yuborilmaydi — buyurtmani o'zi kiritdi.
        return OrderResponseDto.from(orderRepository.save(order));
    }

    private void requireItems(List<OrderItemRequest> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Buyurtmada kamida bitta mahsulot bo'lishi kerak");
        }
    }

    /** Operator/admin paneli uchun viloyatlar ro'yxati (har biriga mumkin bo'lgan turlar bilan). */
    @Transactional(readOnly = true)
    public List<RegionResponse> regions() {
        return Arrays.stream(Region.values())
                .map(RegionResponse::from)
                .toList();
    }

    /** Buyurtma qatorlarini yaratadi: zaxirani tekshiradi, kamaytiradi va narxni muzlatadi. */
    private void populateItems(Order order, List<OrderItemRequest> items) {
        for (OrderItemRequest itemReq : items) {
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
    }

    /** Mahsulotning haqiqiy narxi: chegirma bo'lsa chegirma narxi, aks holda asl narx. */
    private Integer effectivePrice(Product product) {
        Integer price = product.getPrice();
        Integer discount = product.getDiscountPrice();
        return (discount != null && price != null && discount < price) ? discount : price;
    }

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

    /**
     * Buyurtmani tasdiqlash uchun manzil yetkazishga yaroqli bo'lishi shart.
     * <p>
     * Tasdiqlash — operator mijoz bilan gaplashib manzilni aniqlagan payt, ya'ni to'liqlikni
     * shu yerda talab qilish eng erta va eng arzon nuqta. Aks holda manzilsiz buyurtma
     * oxirigacha o'tib ketadi: EMU'da bo'sh manzilli pasilka pochtaga topshiriladi,
     * kuryerda esa buyurtma qayerga borishi noma'lum bo'ladi.
     * <p>
     * {@code landmark} (mo'ljal) ham shart: ko'cha/uy maydoni yo'q bo'lgani uchun manzilning
     * yagona aniq tafsiloti o'sha. Usiz manzil "Samarqand viloyati, Samarqand sh." bo'lib
     * qoladi — bunga yetkazib bo'lmaydi.
     */
    private void assertDeliverable(Order order) {
        List<String> missing = new ArrayList<>();
        if (isBlank(order.getCustomerName())) {
            missing.add("mijoz ismi");
        }
        if (isBlank(order.getCustomerPhone())) {
            missing.add("telefon");
        }
        if (order.getRegion() == null) {
            missing.add("viloyat");
        }
        if (isBlank(order.getDistrict())) {
            missing.add("tuman");
        }
        if (isBlank(order.getLandmark())) {
            missing.add("mo'ljal (uy/bog'cha/maktab orientiri)");
        }
        if (!missing.isEmpty()) {
            throw new ConflictException("Buyurtmani tasdiqlab bo'lmaydi — mijoz bilan gaplashib "
                    + "quyidagilarni to'ldiring: " + String.join(", ", missing));
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /** Yakuniy statusdagi buyurtma qulflangan — undan chiqib bo'lmaydi. */
    private void assertNotTerminal(OrderStatus current) {
        if (TERMINAL_STATUSES.contains(current)) {
            throw new ConflictException(current + " — yakuniy status; buyurtmani boshqa "
                    + "statusga o'tkazib bo'lmaydi. Kerak bo'lsa yangi buyurtma yarating");
        }
    }

    /**
     * Buyurtmadagi kitoblarni zaxiraga qaytaradi — {@link #populateItems} dagi
     * kamaytirishning teskarisi. Mahsulotlar managed obyekt bo'lgani uchun o'zgarish
     * transaksiya yakunida avtomatik saqlanadi (dirty checking).
     */
    private void restoreStock(Order order) {
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            Integer quantity = item.getQuantity();
            if (product == null || quantity == null) {
                continue;
            }
            Integer stock = product.getStockQuantity();
            product.setStockQuantity((stock != null ? stock : 0) + quantity);
        }
    }

    /**
     * Buyurtma statusini o'zgartiradi.
     * Ruxsat: ADMIN/SUPER_ADMIN har qanday buyurtmani; OPERATOR/COURIER faqat o'ziga
     * biriktirilgan buyurtmani va faqat o'z roliga ruxsat etilgan statuslarga
     * ({@link #assertStatusAllowedForRole}).
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

        assertCanManage(order, actor);
        assertStatusAllowedForRole(actor.getRole(), newStatus);
        assertNotTerminal(order.getStatus());

        if (newStatus == OrderStatus.CONFIRMED) {
            assertDeliverable(order);
            // Marshrut aynan shu yerda tug'iladi: manzil endi ma'lum va viloyat aniq.
            // Toshkent viloyatida ikkala tur ham mumkin — autoRoute null qaytaradi va
            // buyurtma marshrutsiz qoladi (admin tanlaguncha).
            order.setDeliveryMethod(order.getRegion().autoRoute());
        }

        if (RESTOCK_STATUSES.contains(newStatus)) {
            restoreStock(order);
        }
        order.changeStatus(newStatus, actor);   // statusni o'rnatadi + tarixga yozadi
        Order saved = orderRepository.save(order);
        notifyOrderParticipants(saved, newStatus, actor);
        return OrderResponseDto.from(saved);
    }

    /**
     * Buyurtmaga kuryer biriktiradi.
     * Ruxsat: faqat ADMIN/SUPER_ADMIN va faqat {@code CONFIRMED} buyurtmaga
     * ({@link #assertCourierAssignable}).
     */
    @Transactional
    public OrderResponseDto assignCourier(UUID orderId, UUID courierId, User actor) {
        Order order = orderRepository.findWithItemsById(orderId)
                .orElseThrow(() -> new NotFoundException("Buyurtma topilmadi: " + orderId));

        assertCanAssignCourier(actor);
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
     * Operator (yoki admin) mijoz bilan gaplashib yetkazish manzilini yozadi: tuman va
     * mo'ljal. {@code region} berilса va o'zgargan bo'lsa viloyat va yetkazish turi
     * (COURIER/EMU) qayta hisoblanadi.
     * <p>
     * Ruxsat: ADMIN/SUPER_ADMIN har qanday; OPERATOR faqat o'z buyurtmasi.
     * Kuryer va SMM manager manzilni o'zgartira olmaydi.
     */
    @Transactional
    public OrderResponseDto updateDelivery(UUID orderId, UpdateDeliveryRequest req, User actor) {
        Order order = orderRepository.findWithItemsById(orderId)
                .orElseThrow(() -> new NotFoundException("Buyurtma topilmadi: " + orderId));

        assertCanEditDelivery(order, actor);

        order.setDistrict(req.district());
        order.setLandmark(req.landmark());

        applyDeliveryChange(order, req.region(), req.deliveryMethod());

        return OrderResponseDto.from(orderRepository.save(order));
    }

    /**
     * Viloyat va/yoki yetkazish turini yangilaydi.
     * <p>
     * Marshrut buyurtma tasdiqlangandan keyin yashaydi, shuning uchun:
     * <ul>
     *   <li>hali tasdiqlanmagan (NEW) buyurtmaga yetkazish turini qo'yib bo'lmaydi — u
     *       tasdiqlashda baribir viloyatdan qayta hisoblanadi;</li>
     *   <li>tasdiqlangan buyurtmada admin turni aniq tanlashi mumkin (Toshkent viloyati),
     *       lekin faqat viloyat ruxsat etganini;</li>
     *   <li>tasdiqlangan buyurtmaning viloyati o'zgarsa, marshrut qayta hisoblanadi.</li>
     * </ul>
     */
    private void applyDeliveryChange(Order order, Region newRegion, DeliveryMethod requestedMethod) {
        boolean regionChanged = newRegion != null && newRegion != order.getRegion();
        if (newRegion != null) {
            order.setRegion(newRegion);
        }

        if (requestedMethod == null) {
            if (regionChanged && order.getStatus() != OrderStatus.NEW) {
                applyRoute(order, order.getRegion().autoRoute());
            }
            return;
        }

        if (order.getStatus() == OrderStatus.NEW) {
            throw new ConflictException("Yetkazish turi buyurtma tasdiqlangandan keyin belgilanadi");
        }
        Region region = order.getRegion();
        if (region == null) {
            throw new ConflictException("Avval viloyatni ko'rsating");
        }
        if (!region.allows(requestedMethod)) {
            throw new IllegalArgumentException(
                    region.getLabel() + " uchun " + requestedMethod + " yetkazish mumkin emas");
        }
        applyRoute(order, requestedMethod);
    }

    /**
     * Marshrutni qo'yadi va unga bog'liq tozalashni bajaradi. {@code method} null bo'lishi
     * mumkin — Toshkent viloyati (admin hali tanlamagan).
     */
    private void applyRoute(Order order, DeliveryMethod method) {
        if (method != DeliveryMethod.COURIER && order.getCourier() != null) {
            throw new ConflictException(
                    "Kuryer biriktirilgan — yetkazish turini o'zgartirishdan oldin avval kuryerni yeching");
        }
        order.setDeliveryMethod(method);
        if (method != DeliveryMethod.EMU) {
            order.setEmuShipment(null);   // orphanRemoval — pasilka yozuvi o'chadi
        }
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

        assertAdmin(actor);
        assertCourierAssignable(order.getStatus());
        order.setCourier(null);
        return OrderResponseDto.from(orderRepository.save(order));
    }

    /**
     * Marshrutsiz buyurtmalar (admin paneli) — tasdiqlangan, lekin yetkazish turi hali
     * tanlanmagan. Admin har biri uchun EMU yoki kuryerni belgilaydi
     * ({@code PATCH /api/orders/{id}/address} + {@code deliveryMethod}).
     */
    @Transactional(readOnly = true)
    public List<OrderResponseDto> unrouted(User actor) {
        assertAdmin(actor);
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
        orderRepository.findNewOrdersOfUnavailableOperators(OrderStatus.NEW, OrderSource.WEBSITE, threshold)
                .forEach(order -> order.setOperator(null));

        // 2) Hovuzdagilarni mavjud operatorlarga tarqat (round-robin strategiya orqali).
        for (Order order : orderRepository.findByOperatorIsNullOrderByCreatedAtAsc()) {
            User operator = operatorAssignmentStrategy.assignOperator();
            if (operator == null) {
                break;   // hozircha mavjud operator yo'q — hovuzda kutaversin
            }
            order.setOperator(operator);
            pushNotificationService.notifyNewOrder(operator, order);
        }
    }

    /** Status o'zgarganda biriktirilgan operator/kuryerga push yuboradi (o'zgartirgan shaxsdan tashqari). */
    private void notifyOrderParticipants(Order order, OrderStatus newStatus, User actor) {
        User operator = order.getOperator();
        if (operator != null && !isSameUser(operator, actor)) {
            pushNotificationService.notifyOrderUpdated(operator, order, newStatus);
        }
        User courier = order.getCourier();
        if (courier != null && !isSameUser(courier, actor)) {
            pushNotificationService.notifyOrderUpdated(courier, order, newStatus);
        }
    }

    /**
     * Xodimning o'ziga biriktirilgan (u egalik qiladigan) buyurtmalari; status — ixtiyoriy filtr.
     * Operator ham, SMM manager ham {@code operator} maydonida saqlanadi — shu metod ikkalasiga xizmat qiladi.
     */
    @Transactional(readOnly = true)
    public List<OrderResponseDto> getMyOwnedOrders(User actor, OrderStatus status) {
        List<Order> orders = status != null
                ? orderRepository.findByOperatorIdAndStatusOrderByCreatedAtDesc(actor.getId(), status)
                : orderRepository.findByOperatorIdOrderByCreatedAtDesc(actor.getId());
        return orders.stream()
                .map(OrderResponseDto::from)
                .toList();
    }

    /**
     * Kuryerning o'ziga biriktirilgan buyurtmalari (mobil ilova); status — ixtiyoriy filtr.
     * Kuryer faqat yetkazish bosqichidagi statuslarni ko'radi ({@link #COURIER_VISIBLE_STATUSES}):
     * NEW/CANCELLED/REPROCESSING dagi buyurtmalar ro'yxatga kirmaydi, ular bo'yicha
     * filtrlashga urinish esa 403 qaytaradi.
     */
    @Transactional(readOnly = true)
    public List<OrderResponseDto> getMyCourierOrders(User actor, OrderStatus status) {
        List<Order> orders;
        if (status != null) {
            assertCourierVisibleStatus(status);
            orders = orderRepository.findByCourierIdAndStatusOrderByCreatedAtDesc(actor.getId(), status);
        } else {
            orders = orderRepository.findByCourierIdAndStatusInOrderByCreatedAtDesc(
                    actor.getId(), COURIER_VISIBLE_STATUSES);
        }
        return orders.stream()
                .map(OrderResponseDto::from)
                .toList();
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
        assertCanManage(order, actor);
        if (actor.getRole() == Role.COURIER) {
            assertCourierVisibleStatus(order.getStatus());
        }
        return OrderResponseDto.from(order);
    }

    /**
     * Buyurtmalar ro'yxati (admin panel). Ikkala filtr ham ixtiyoriy:
     * operatorId — bitta operatorning buyurtmalari, status — ma'lum statusdagilar;
     * ikkalasi berilsa — kesishmasi, hech biri berilmasa — hammasi.
     */
    @Transactional(readOnly = true)
    public List<OrderResponseDto> getAll(UUID operatorId, OrderStatus status) {
        List<Order> orders;
        if (operatorId != null && status != null) {
            orders = orderRepository.findByOperatorIdAndStatusOrderByCreatedAtDesc(operatorId, status);
        } else if (operatorId != null) {
            orders = orderRepository.findByOperatorIdOrderByCreatedAtDesc(operatorId);
        } else if (status != null) {
            orders = orderRepository.findByStatusOrderByCreatedAtDesc(status);
        } else {
            orders = orderRepository.findAll();
        }
        return orders.stream()
                .map(OrderResponseDto::from)
                .toList();
    }

    // --- Ruxsat tekshiruvi ---

    /**
     * Buyurtmaga tegish huquqi bor-yo'qligi (egalik tekshiruvi): admin — har doim;
     * operator/SMM manager — o'ziga biriktirilgani; kuryer — o'ziga berilgani.
     * <p>
     * Bu faqat "qaysi buyurtma" savoliga javob beradi, "qanday amal" degani emas —
     * SMM manager shu tekshiruvdan o'tadi (o'z lead'ini ko'rish uchun), lekin statusni
     * baribir o'zgartira olmaydi: uni {@link #assertStatusAllowedForRole} to'xtatadi.
     */
    private void assertCanManage(Order order, User actor) {
        Role role = actor.getRole();
        if (role == Role.ADMIN || role == Role.SUPER_ADMIN) {
            return;
        }
        if ((role == Role.OPERATOR || role == Role.SMM_MANAGER) && isSameUser(order.getOperator(), actor)) {
            return;
        }
        if (role == Role.COURIER && isSameUser(order.getCourier(), actor)) {
            return;
        }
        throw new ForbiddenException("Bu buyurtmani boshqarishga ruxsatingiz yo'q");
    }

    /** Operator o'rnata oladigan statuslar — mijoz bilan ishlash bosqichi. */
    private static final Set<OrderStatus> OPERATOR_STATUSES =
            Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED, OrderStatus.REPROCESSING);

    /** Kuryer o'rnata oladigan statuslar — yetkazib berish bosqichi. */
    private static final Set<OrderStatus> COURIER_STATUSES =
            Set.of(OrderStatus.IN_DELIVERY, OrderStatus.DELIVERED, OrderStatus.RETURNED);

    /** Kuryer ko'ra oladigan statuslar — NEW/CANCELLED/REPROCESSING unga ko'rinmaydi. */
    private static final Set<OrderStatus> COURIER_VISIBLE_STATUSES = Set.of(
            OrderStatus.CONFIRMED, OrderStatus.IN_DELIVERY,
            OrderStatus.DELIVERED, OrderStatus.RETURNED);

    /** Kuryer so'ragan/ochmoqchi bo'lgan status unga ko'rinadigan bo'lishi shart. */
    private void assertCourierVisibleStatus(OrderStatus status) {
        if (!COURIER_VISIBLE_STATUSES.contains(status)) {
            throw new ForbiddenException("Kuryer " + status + " statusidagi buyurtmalarni ko'ra olmaydi");
        }
    }

    /**
     * Rol o'z bosqichidagi statuslarnigina o'rnata oladi: OPERATOR —
     * CONFIRMED/CANCELLED/REPROCESSING; COURIER — IN_DELIVERY/DELIVERED/RETURNED;
     * ADMIN/SUPER_ADMIN — hammasini.
     * <p>
     * SMM_MANAGER ro'yxatda yo'q — u lead kiritish nuqtasi, boshqaruv roli emas: buyurtmasi
     * yaratilishidayoq {@code CONFIRMED} bo'ladi ({@link #createBySmm}), undan keyingi ishni
     * operator/admin olib boradi. Shuning uchun unga hech qanday status o'rnatish berilmagan.
     */
    private void assertStatusAllowedForRole(Role role, OrderStatus newStatus) {
        if (role == Role.ADMIN || role == Role.SUPER_ADMIN) {
            return;
        }
        Set<OrderStatus> allowed = switch (role) {
            case OPERATOR -> OPERATOR_STATUSES;
            case COURIER -> COURIER_STATUSES;
            default -> Set.of();
        };
        if (!allowed.contains(newStatus)) {
            throw new ForbiddenException("Rolingiz " + newStatus + " statusini o'rnata olmaydi");
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

    /** Kuryer biriktirish — faqat admin (ADMIN/SUPER_ADMIN) huquqi. */
    private void assertCanAssignCourier(User actor) {
        Role role = actor.getRole();
        if (role == Role.ADMIN || role == Role.SUPER_ADMIN) {
            return;
        }
        throw new ForbiddenException("Kuryer biriktirish faqat admin uchun");
    }

    /** Amal faqat admin (ADMIN/SUPER_ADMIN) uchun ekanini tekshiradi. */
    private void assertAdmin(User actor) {
        Role role = actor.getRole();
        if (role != Role.ADMIN && role != Role.SUPER_ADMIN) {
            throw new ForbiddenException("Bu amal faqat admin uchun");
        }
    }

    /**
     * Manzilni tahrirlash — admin har qanday; buyurtma egasi OPERATOR o'ziniki.
     * Kuryer (u {@code courier} maydonida) manzilni o'zgartira olmaydi.
     * <p>
     * SMM_MANAGER ham o'zgartira olmaydi — u manzilni faqat lead yaratishda kiritadi
     * ({@link #createBySmm}); keyin buyurtma allaqachon tasdiqlangan va yetkazishga
     * ketgan bo'ladi, manzilni tuzatish operator/admin ishi.
     */
    private void assertCanEditDelivery(Order order, User actor) {
        Role role = actor.getRole();
        if (role == Role.ADMIN || role == Role.SUPER_ADMIN) {
            return;
        }
        if (role == Role.OPERATOR && isSameUser(order.getOperator(), actor)) {
            return;
        }
        throw new ForbiddenException("Bu buyurtma manzilini o'zgartirishga ruxsatingiz yo'q");
    }

    private boolean isSameUser(User assigned, User actor) {
        return assigned != null && assigned.getId().equals(actor.getId());
    }
}
