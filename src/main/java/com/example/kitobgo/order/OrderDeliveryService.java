package com.example.kitobgo.order;

import com.example.kitobgo.common.ConflictException;
import com.example.kitobgo.common.NotFoundException;
import com.example.kitobgo.order.dto.OrderResponseDto;
import com.example.kitobgo.order.dto.UpdateDeliveryRequest;
import com.example.kitobgo.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Yetkazish manzili va marshruti: operator kiritadigan manzil (tuman/mo'ljal),
 * viloyat o'zgarishi va yetkazish turini (COURIER/EMU) hisoblash qoidalari.
 */
@Service
@RequiredArgsConstructor
public class OrderDeliveryService {

    private final OrderRepository orderRepository;
    private final OrderAuthorizationPolicy authorizationPolicy;

    /**
     * Operator (yoki admin) mijoz bilan gaplashib yetkazish manzilini yozadi: tuman va
     * mo'ljal. {@code region} berilsa va o'zgargan bo'lsa viloyat va yetkazish turi
     * (COURIER/EMU) qayta hisoblanadi.
     * <p>
     * Ruxsat: ADMIN/SUPER_ADMIN har qanday; OPERATOR faqat o'z buyurtmasi.
     * Kuryer va SMM manager manzilni o'zgartira olmaydi.
     */
    @Transactional
    public OrderResponseDto updateDelivery(UUID orderId, UpdateDeliveryRequest req, User actor) {
        Order order = orderRepository.findWithItemsById(orderId)
                .orElseThrow(() -> new NotFoundException("Buyurtma topilmadi: " + orderId));

        authorizationPolicy.assertCanEditDelivery(order, actor);

        order.setDistrict(req.district());
        order.setLandmark(req.landmark());

        applyDeliveryChange(order, req.region(), req.deliveryMethod());

        return OrderResponseDto.from(orderRepository.save(order));
    }

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
    public void assertDeliverable(Order order) {
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

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
