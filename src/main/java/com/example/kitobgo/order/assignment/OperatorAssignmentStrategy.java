package com.example.kitobgo.order.assignment;

import com.example.kitobgo.user.User;

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
     * Navbatdagi buyurtma uchun operatorni tanlaydi.
     *
     * @return biriktiriladigan operator
     */
    User assignOperator();
}
