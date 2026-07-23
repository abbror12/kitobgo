package com.example.kitobgo;

import com.example.kitobgo.common.AppTime;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
@EnableCaching
public class KitobgoApplication {

    static {
        // LocalDateTime ishlatmaydigan kutubxonalar va loglar ham bir xil zonada bo'lsin.
        TimeZone.setDefault(TimeZone.getTimeZone(AppTime.ZONE));
    }

    public static void main(String[] args) {
        SpringApplication.run(KitobgoApplication.class, args);
    }

}
