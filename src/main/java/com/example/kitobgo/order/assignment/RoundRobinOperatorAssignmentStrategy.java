package com.example.kitobgo.order.assignment;

import com.example.kitobgo.common.NotFoundException;
import com.example.kitobgo.user.Role;
import com.example.kitobgo.user.User;
import com.example.kitobgo.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Buyurtmalarni operatorlarga navbatma-navbat (round-robin) taqsimlaydi:
 * 1-buyurtma -> 1-operator, 2-buyurtma -> 2-operator, ... oxirgisidan keyin
 * yana boshiga qaytadi.
 * <p>
 * Navbat hisoblagichi xotirada saqlanadi ({@link AtomicInteger} — bir nechta
 * so'rov bir vaqtda kelganda ham xavfsiz). Dastur qayta ishga tushsa hisoblagich
 * noldan boshlanadi; bu qat'iy izchillikni buzmaydi, chunki taqsimot baribir
 * teng bo'lib qoladi.
 */
@Component
@RequiredArgsConstructor
public class RoundRobinOperatorAssignmentStrategy implements OperatorAssignmentStrategy {

    private final UserRepository userRepository;
    private final AtomicInteger nextIndex = new AtomicInteger(0);

    @Override
    public User assignOperator() {
        List<User> operators = userRepository.findByRoleOrderByCreatedAtAscIdAsc(Role.OPERATOR);

        if (operators.isEmpty()) {
            throw new NotFoundException("Buyurtmani biriktirish uchun operator topilmadi");
        }

        // getAndIncrement -> keyingi safar avtomatik navbatdagi operatorga o'tadi.
        // floorMod salbiy qiymatlarda ham (int to'lib ketsa) to'g'ri ishlaydi.
        int index = Math.floorMod(nextIndex.getAndIncrement(), operators.size());
        return operators.get(index);
    }
}
