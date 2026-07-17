package com.example.kitobgo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class KitobgoApplication {

    public static void main(String[] args) {
        SpringApplication.run(KitobgoApplication.class, args);
    }

}
