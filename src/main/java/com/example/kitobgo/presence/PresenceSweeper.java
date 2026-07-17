package com.example.kitobgo.presence;

import com.example.kitobgo.order.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Hech kim heartbeat yubormasa ham buyurtma taqsimotini muntazam qayta muvozanatlaydi.
 * {@link PresenceService} rebalance'ni faqat signal kelganda chaqiradi — operator ilovasi
 * jimgina o'chib qolsa (internet uzilishi, crash) va boshqa faol operator bo'lmasa,
 * uning NEW buyurtmalari osilib qolar edi. Bu sweeper o'sha bo'shliqni yopadi.
 */
@Component
@RequiredArgsConstructor
public class PresenceSweeper {

    private final OrderService orderService;

    @Scheduled(fixedDelayString = "${app.presence.sweep-interval-ms:60000}")
    public void sweep() {
        orderService.rebalance();
    }
}
