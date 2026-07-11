package com.example.kitobgo.order.assignment;

import com.example.kitobgo.presence.Availability;
import com.example.kitobgo.user.Role;
import com.example.kitobgo.user.User;
import com.example.kitobgo.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Buyurtmalarni ayni damda <b>mavjud</b> (online va heartbeat'i tirik) operatorlarga
 * navbatma-navbat (round-robin) taqsimlaydi. Offline yoki heartbeat'i eskirgan
 * operatorlar e'tiborga olinmaydi — shu tufayli buyurtmalar ishda bo'lmagan
 * operatorga tegib yotib qolmaydi.
 * <p>
 * Navbat hisoblagichi xotirada saqlanadi ({@link AtomicInteger}). Dastur qayta
 * ishga tushsa noldan boshlanadi; taqsimot baribir teng bo'lib qoladi.
 */
@Component
@RequiredArgsConstructor
public class RoundRobinOperatorAssignmentStrategy implements OperatorAssignmentStrategy {

    private final UserRepository userRepository;
    private final Availability availability;
    private final AtomicInteger nextIndex = new AtomicInteger(0);

    @Override
    public User assignOperator() {
        List<User> operators = userRepository
                .findByRoleAndOnlineTrueAndLastSeenAtAfterOrderByCreatedAtAscIdAsc(
                        Role.OPERATOR, availability.threshold());

        if (operators.isEmpty()) {
            return null;   // hech qanday mavjud operator yo'q — buyurtma hovuzda kutadi
        }

        int index = Math.floorMod(nextIndex.getAndIncrement(), operators.size());
        return operators.get(index);
    }
}
