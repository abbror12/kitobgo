package com.example.kitobgo.presence;

import com.example.kitobgo.user.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Operator/kuryer "mavjudligi" (availability) siyosati — yagona ta'rif.
 * Foydalanuvchi <b>mavjud</b> hisoblanadi, agar {@code online=true} bo'lsa <b>va</b>
 * oxirgi heartbeat ({@code lastSeenAt}) timeout ichida bo'lsa (ilova tirik).
 */
@Component
public class Availability {

    /** Heartbeat timeout (soniya): shundan uzoq signal bermagan foydalanuvchi "mavjud emas". */
    @Value("${app.presence.heartbeat-timeout-seconds:120}")
    private long heartbeatTimeoutSeconds;

    /** Shundan eski {@code lastSeenAt} "mavjud emas" hisoblanadi. */
    public LocalDateTime threshold() {
        return LocalDateTime.now().minusSeconds(heartbeatTimeoutSeconds);
    }

    /** Foydalanuvchi ayni damda mavjudmi (online va heartbeat'i tirik). */
    public boolean isAvailable(User user) {
        return Boolean.TRUE.equals(user.getOnline())
                && user.getLastSeenAt() != null
                && user.getLastSeenAt().isAfter(threshold());
    }
}
