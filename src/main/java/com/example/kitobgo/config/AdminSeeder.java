package com.example.kitobgo.config;

import com.example.kitobgo.user.Role;
import com.example.kitobgo.user.User;
import com.example.kitobgo.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Dastur ishga tushganda bazada bironta ADMIN bo'lmasa, sozlamadagi (application.yaml)
 * ma'lumotlar asosida birinchi ADMIN akkauntini yaratadi.
 * <p>
 * Bu zarur, chunki yangi user qo'shishni (POST /api/users) faqat ADMIN chaqira
 * oladi — birinchi adminni qo'lda yaratmasdan, tizimga kirishning boshqa yo'li bo'lmaydi.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.phone}")
    private String adminPhone;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Value("${app.admin.name}")
    private String adminName;

    @Override
    public void run(org.springframework.boot.ApplicationArguments args) {
        boolean adminExists = !userRepository.findByRoleOrderByCreatedAtAscIdAsc(Role.ADMIN).isEmpty();
        if (adminExists) {
            return;
        }

        User admin = User.builder()
                .name(adminName)
                .phone(adminPhone)
                .password(passwordEncoder.encode(adminPassword))
                .role(Role.SUPER_ADMIN)
                .build();
        userRepository.save(admin);

        log.warn("Birinchi ADMIN yaratildi (telefon: {}). Xavfsizlik uchun parolni darhol o'zgartiring!", adminPhone);
    }
}
