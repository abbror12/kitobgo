package com.example.kitobgo.config;

import com.example.kitobgo.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Rollar ierarxiyasi: SUPER_ADMIN barcha ADMIN huquqlarini avtomatik meros oladi.
     * Shu tufayli ADMIN uchun ochilgan endpointlarga SUPER_ADMIN ham kira oladi,
     * har birini alohida sanab o'tish shart emas.
     */
    @Bean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.withDefaultRolePrefix()
                .role("SUPER_ADMIN").implies("ADMIN")
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login", "/api/auth/refresh", "/api/auth/logout").permitAll()
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .requestMatchers("/uploads/**").permitAll()
                        .requestMatchers(
                                "/actuator/health", "/actuator/health/**", "/actuator/prometheus"
                        ).permitAll()
                        .requestMatchers("/actuator/**").hasRole("SUPER_ADMIN")
                        // Servlet ERROR dispatch (/error) — @ResponseStatus xatolar sendError orqali
                        // shu yerga qaytadi. Ochilmasa, ochiq endpointlardagi 404/409 kabi xatolar
                        // anonim so'rovda 403 bilan niqoblanadi.
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/orders").permitAll()
                        // EMU eksporti — faqat admin sayti. Yuqoridagi permitAll aynan "/api/orders"
                        // uchun (ostidagi yo'llarga tarqalmaydi), bu qoida esa buni aniq qilib qo'yadi.
                        .requestMatchers(HttpMethod.POST, "/api/orders/emu/**").hasRole("ADMIN")
                        // Checkout viloyatlar ro'yxati — ochiq (sayt formasi uchun).
                        .requestMatchers(HttpMethod.GET, "/api/orders/regions").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/users/me").authenticated()
                        // Buyurtmalarni ko'rish — faqat admin sayti (ADMIN + SUPER_ADMIN).
                        // COURIER/OPERATOR/SMM_MANAGER admin saytiga kira olmaydi — har biri
                        // quyidagi o'z namespace'idan faqat o'ziga tegishlisini ko'radi.
                        .requestMatchers(HttpMethod.GET, "/api/orders/**").hasRole("ADMIN")
                        // Status/kuryer o'zgartirish — istalgan tizimga kirgan foydalanuvchi HTTP darajasida
                        // o'tadi; aniq rol va "o'z buyurtmasi" tekshiruvi OrderAuthorizationPolicy'da bajariladi.
                        .requestMatchers(HttpMethod.PATCH, "/api/orders/**").authenticated()
                        // Rolga xos ilova endpointlari — har rol o'z namespace'ida.
                        // Operator/kuryer mobil ilovadan, SMM manager esa web'dan ishlaydi;
                        // ikkalasi ham admin saytiga kirmagani uchun gate bir xil.
                        .requestMatchers("/api/operator/**").hasRole("OPERATOR")
                        .requestMatchers("/api/courier/**").hasRole("COURIER")
                        .requestMatchers("/api/smm/**").hasRole("SMM_MANAGER")
                        // FCM qurilma token'lari — istalgan tizimga kirgan foydalanuvchi
                        // (operator/kuryer mobil ilovadan) ro'yxatga oladi/o'chiradi.
                        .requestMatchers("/api/devices/**").authenticated()
                        // Foydalanuvchilarni ko'rish (ro'yxat, qidiruv, bittasi) — ADMIN ham ko'ra oladi;
                        // yaratish/o'zgartirish/o'chirish esa faqat SUPER_ADMIN.
                        .requestMatchers(HttpMethod.GET, "/api/users/**").hasRole("ADMIN")
                        .requestMatchers("/api/users/**").hasRole("SUPER_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")
                        .requestMatchers("/api/categories/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
