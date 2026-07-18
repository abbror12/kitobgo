package com.example.kitobgo.presence;

import com.example.kitobgo.common.NotFoundException;
import com.example.kitobgo.order.OrderAssignmentService;
import com.example.kitobgo.user.Role;
import com.example.kitobgo.user.User;
import com.example.kitobgo.user.UserRepository;
import com.example.kitobgo.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Operatorning "mavjudlik" (online + heartbeat) holatini boshqaradi va har o'zgarishda
 * buyurtma taqsimotini qayta muvozanatlaydi ({@link OrderAssignmentService#rebalance()}): egasiz
 * buyurtmalar mavjud operatorlarga avtomatik tarqatiladi (push modeli).
 * <p>
 * Kuryerlarga tegishli emas — kuryerga buyurtmani admin/operator qo'lda biriktiradi.
 */
@Service
@RequiredArgsConstructor
public class PresenceService {

    private final UserRepository userRepository;
    private final OrderAssignmentService assignmentService;

    /** Foydalanuvchini online/offline qiladi (ishni boshlash/tugatish). */
    @Transactional
    public UserResponse setOnline(UUID userId, boolean online) {
        User user = load(userId);
        user.setOnline(online);
        if (online) {
            user.setLastSeenAt(LocalDateTime.now());
        }
        userRepository.save(user);
        afterPresenceChange(user);
        return UserResponse.from(user);
    }

    /** Heartbeat — foydalanuvchini "tirik" saqlaydi (ilova muntazam yuboradi). */
    @Transactional
    public void heartbeat(UUID userId) {
        User user = load(userId);
        user.setLastSeenAt(LocalDateTime.now());
        userRepository.save(user);
        afterPresenceChange(user);
    }

    private void afterPresenceChange(User user) {
        if (user.getRole() == Role.OPERATOR) {
            assignmentService.rebalance();
        }
    }

    private User load(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Foydalanuvchi topilmadi: " + userId));
    }
}
