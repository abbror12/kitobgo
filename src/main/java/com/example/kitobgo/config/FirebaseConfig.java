package com.example.kitobgo.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Firebase Admin SDK'ni service account kaliti bilan ishga tushiradi (FCM push uchun).
 * Kalit fayli topilmasa ilova baribir ishlayveradi — faqat push yuborilmaydi
 * (masalan, lokal ishlab chiqishda Firebase shart emas).
 */
@Configuration
@Slf4j
public class FirebaseConfig {

    @Value("${app.fcm.service-account}")
    private String serviceAccountPath;

    @PostConstruct
    void init() {
        if (!FirebaseApp.getApps().isEmpty()) {
            return;
        }
        Path path = Path.of(serviceAccountPath);
        if (!Files.exists(path)) {
            log.warn("Firebase service account fayli topilmadi ({}) — FCM push o'chirilgan holda davom etiladi",
                    path.toAbsolutePath());
            return;
        }
        try (InputStream in = Files.newInputStream(path)) {
            FirebaseApp.initializeApp(FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(in))
                    .build());
            log.info("Firebase Admin SDK ishga tushdi — FCM push yoqildi");
        } catch (IOException e) {
            log.error("Firebase Admin SDK'ni ishga tushirib bo'lmadi — FCM push o'chirilgan", e);
        }
    }
}
