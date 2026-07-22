package com.example.kitobgo.order.assignment;

import com.example.kitobgo.user.User;

import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * Buyurtmaga qaysi operator biriktirilishini aniqlaydigan strategiya.
 * <p>
 * Abstraksiya (DIP): {@code OrderService} shu interfeysga bog'lanadi, aniq
 * amalga oshirilishga emas. Kelajakda boshqa taqsimlash qoidasi (masalan,
 * eng kam bandi yoki smenaga qarab) qo'shilsa, faqat yangi implementatsiya
 * yoziladi — mavjud kod o'zgarmaydi (OCP).
 */
public interface OperatorAssignmentStrategy {

    /**
     * Navbatdagi buyurtma uchun ayni damda <b>mavjud</b> (online va heartbeat'i
     * tirik) operatorni tanlaydi.
     *
     * @return biriktiriladigan operator, yoki hech qanday mavjud operator bo'lmasa
     *         {@code null} (bunda buyurtma egasiz hovuzda kutadi)
     */
    User assignOperator();

    /** Batch taqsimotida implementatsiya operatorlarni bir marta o'qib qaytarishi mumkin. */
    default List<User> assignOperators(int count) {
        return IntStream.range(0, count)
                .mapToObj(ignored -> assignOperator())
                .takeWhile(Objects::nonNull)
                .toList();
    }
}
