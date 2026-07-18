package com.example.kitobgo.order;

import com.example.kitobgo.common.ForbiddenException;
import com.example.kitobgo.user.Role;
import com.example.kitobgo.user.User;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Buyurtmalar bo'yicha ruxsat siyosati — "kim nimani qila oladi" savollari bitta joyda.
 * Holatga ega emas (stateless): faqat rol/egalik tekshiruvlari, hech qanday query yoki
 * o'zgartirish yo'q.
 */
@Component
public class OrderAuthorizationPolicy {

    /** Operator o'rnata oladigan statuslar — mijoz bilan ishlash bosqichi. */
    private static final Set<OrderStatus> OPERATOR_STATUSES =
            Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED, OrderStatus.REPROCESSING);

    /** Kuryer o'rnata oladigan statuslar — yetkazib berish bosqichi. */
    private static final Set<OrderStatus> COURIER_STATUSES =
            Set.of(OrderStatus.IN_DELIVERY, OrderStatus.DELIVERED, OrderStatus.RETURNED);

    /** Kuryer ko'ra oladigan statuslar — NEW/CANCELLED/REPROCESSING unga ko'rinmaydi. */
    static final Set<OrderStatus> COURIER_VISIBLE_STATUSES = Set.of(
            OrderStatus.CONFIRMED, OrderStatus.IN_DELIVERY,
            OrderStatus.DELIVERED, OrderStatus.RETURNED);

    /**
     * Buyurtmaga tegish huquqi bor-yo'qligi (egalik tekshiruvi): admin — har doim;
     * operator/SMM manager — o'ziga biriktirilgani; kuryer — o'ziga berilgani.
     * <p>
     * Bu faqat "qaysi buyurtma" savoliga javob beradi, "qanday amal" degani emas —
     * SMM manager shu tekshiruvdan o'tadi (o'z lead'ini ko'rish uchun), lekin statusni
     * baribir o'zgartira olmaydi: uni {@link #assertStatusAllowedForRole} to'xtatadi.
     */
    public void assertCanManage(Order order, User actor) {
        Role role = actor.getRole();
        if (isAdmin(role)) {
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

    /**
     * Rol o'z bosqichidagi statuslarnigina o'rnata oladi: OPERATOR —
     * CONFIRMED/CANCELLED/REPROCESSING; COURIER — IN_DELIVERY/DELIVERED/RETURNED;
     * ADMIN/SUPER_ADMIN — hammasini.
     * <p>
     * SMM_MANAGER ro'yxatda yo'q — u lead kiritish nuqtasi, boshqaruv roli emas: buyurtmasi
     * yaratilishidayoq {@code CONFIRMED} bo'ladi ({@link OrderCreationService#createBySmm}),
     * undan keyingi ishni operator/admin olib boradi. Shuning uchun unga hech qanday status
     * o'rnatish berilmagan.
     */
    public void assertStatusAllowedForRole(Role role, OrderStatus newStatus) {
        if (isAdmin(role)) {
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

    /** Kuryer so'ragan/ochmoqchi bo'lgan status unga ko'rinadigan bo'lishi shart. */
    public void assertCourierVisibleStatus(OrderStatus status) {
        if (!COURIER_VISIBLE_STATUSES.contains(status)) {
            throw new ForbiddenException("Kuryer " + status + " statusidagi buyurtmalarni ko'ra olmaydi");
        }
    }

    /** Kuryer biriktirish — faqat admin (ADMIN/SUPER_ADMIN) huquqi. */
    public void assertCanAssignCourier(User actor) {
        if (!isAdmin(actor.getRole())) {
            throw new ForbiddenException("Kuryer biriktirish faqat admin uchun");
        }
    }

    /** Amal faqat admin (ADMIN/SUPER_ADMIN) uchun ekanini tekshiradi. */
    public void assertAdmin(User actor) {
        if (!isAdmin(actor.getRole())) {
            throw new ForbiddenException("Bu amal faqat admin uchun");
        }
    }

    /**
     * "Admin kim?" savolining yagona javobi — ADMIN va SUPER_ADMIN. Yangi boshqaruv roli
     * qo'shilsa, faqat shu yer o'zgaradi. HTTP darajasidagi {@code SecurityConfig}
     * ({@code SUPER_ADMIN implies ADMIN} ierarxiyasi) shu qoidaning o'zi, boshqa qatlamda.
     */
    public boolean isAdmin(Role role) {
        return role == Role.ADMIN || role == Role.SUPER_ADMIN;
    }

    /**
     * Manzilni tahrirlash — admin har qanday; buyurtma egasi OPERATOR o'ziniki.
     * Kuryer (u {@code courier} maydonida) manzilni o'zgartira olmaydi.
     * <p>
     * SMM_MANAGER ham o'zgartira olmaydi — u manzilni faqat lead yaratishda kiritadi
     * ({@link OrderCreationService#createBySmm}); keyin buyurtma allaqachon tasdiqlangan va
     * yetkazishga ketgan bo'ladi, manzilni tuzatish operator/admin ishi.
     */
    public void assertCanEditDelivery(Order order, User actor) {
        Role role = actor.getRole();
        if (isAdmin(role)) {
            return;
        }
        if (role == Role.OPERATOR && isSameUser(order.getOperator(), actor)) {
            return;
        }
        throw new ForbiddenException("Bu buyurtma manzilini o'zgartirishga ruxsatingiz yo'q");
    }

    /** Buyurtmaga biriktirilgan xodim va amal bajaruvchi bitta shaxsmi. */
    public boolean isSameUser(User assigned, User actor) {
        return assigned != null && assigned.getId().equals(actor.getId());
    }
}
