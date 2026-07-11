package com.example.kitobgo.mobile;

import com.example.kitobgo.common.NotFoundException;
import com.example.kitobgo.order.OrderService;
import com.example.kitobgo.user.User;
import com.example.kitobgo.user.UserRepository;
import com.example.kitobgo.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Operatorning "mavjudlik" (online + heartbeat) holatini boshqaradi va har
 * o'zgarishda buyurtma taqsimotini qayta muvozanatlaydi (rebalance).
 */
@Service
@RequiredArgsConstructor
public class OperatorPresenceService {

    private final UserRepository userRepository;
    private final OrderService orderService;

    @Value("${app.operator.heartbeat-timeout-seconds:120}")
    private long heartbeatTimeoutSeconds;

    /**
     * Operatorni online/offline qiladi (ishni boshlash/tugatish tugmasi).
     * Har ikki holatda ham rebalance chaqiriladi: online bo'lsa hovuzdan ish oladi,
     * offline bo'lsa tegilmagan buyurtmalari boshqa mavjud operatorlarga o'tadi.
     */
    @Transactional
    public UserResponse setOnline(UUID operatorId, boolean online) {
        User operator = getOperator(operatorId);
        operator.setOnline(online);
        if (online) {
            operator.setLastSeenAt(LocalDateTime.now());
        }
        userRepository.save(operator);

        orderService.rebalance(threshold());
        return UserResponse.from(operator);
    }

    /**
     * Heartbeat — ilova muntazam yuboradi, operatorni "tirik" saqlaydi.
     * Online bo'lsa rebalance ham chaqiriladi (eskirgan operatorlardan qaytgan
     * buyurtmalarni tarqatish uchun).
     */
    @Transactional
    public void heartbeat(UUID operatorId) {
        User operator = getOperator(operatorId);
        operator.setLastSeenAt(LocalDateTime.now());
        userRepository.save(operator);

        if (Boolean.TRUE.equals(operator.getOnline())) {
            orderService.rebalance(threshold());
        }
    }

    private User getOperator(UUID operatorId) {
        return userRepository.findById(operatorId)
                .orElseThrow(() -> new NotFoundException("Operator topilmadi: " + operatorId));
    }

    private LocalDateTime threshold() {
        return LocalDateTime.now().minusSeconds(heartbeatTimeoutSeconds);
    }
}
