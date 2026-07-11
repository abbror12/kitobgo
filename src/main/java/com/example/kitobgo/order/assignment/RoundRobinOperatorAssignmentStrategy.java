package com.example.kitobgo.order.assignment;

import com.example.kitobgo.user.Role;
import com.example.kitobgo.user.User;
import com.example.kitobgo.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
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
    private final AtomicInteger nextIndex = new AtomicInteger(0);

    /** Heartbeat timeout (soniya): shundan uzoq signal bermagan operator "mavjud emas". */
    @Value("${app.operator.heartbeat-timeout-seconds:120}")
    private long heartbeatTimeoutSeconds;

    @Override
    public User assignOperator() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(heartbeatTimeoutSeconds);
        List<User> operators = userRepository
                .findByRoleAndOnlineTrueAndLastSeenAtAfterOrderByCreatedAtAscIdAsc(Role.OPERATOR, threshold);

        if (operators.isEmpty()) {
            return null;   // hech qanday mavjud operator yo'q — buyurtma hovuzda kutadi
        }

        int index = Math.floorMod(nextIndex.getAndIncrement(), operators.size());
        return operators.get(index);
    }
}
