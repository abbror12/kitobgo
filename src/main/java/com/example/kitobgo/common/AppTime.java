package com.example.kitobgo.common;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** Loyihadagi barcha biznes sanalari uchun yagona O'zbekiston vaqt manbasi. */
public final class AppTime {

    public static final String ZONE_NAME = "Asia/Tashkent";
    public static final ZoneId ZONE = ZoneId.of(ZONE_NAME);

    private AppTime() {
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE);
    }

    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }
}
