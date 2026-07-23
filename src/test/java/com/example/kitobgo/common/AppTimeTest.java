package com.example.kitobgo.common;

import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class AppTimeTest {

    @Test
    void usesUzbekistanTimeForAllBusinessDates() {
        assertThat(AppTime.ZONE.getId()).isEqualTo("Asia/Tashkent");
        assertThat(AppTime.ZONE.getRules().getOffset(AppTime.now().atZone(AppTime.ZONE).toInstant()))
                .isEqualTo(ZoneOffset.ofHours(5));
        assertThat(AppTime.today()).isEqualTo(AppTime.now().toLocalDate());
    }
}
