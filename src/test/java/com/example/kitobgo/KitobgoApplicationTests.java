package com.example.kitobgo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class KitobgoApplicationTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void contextLoads() {
    }

    @Test
    void applicationAndDatabaseUseUzbekistanTimeZone() {
        assertThat(TimeZone.getDefault().getID()).isEqualTo("Asia/Tashkent");
        assertThat(jdbcTemplate.queryForObject("show timezone", String.class))
                .isEqualTo("Asia/Tashkent");
    }

}
