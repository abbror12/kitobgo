package com.example.kitobgo.security;

import com.example.kitobgo.user.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * {@link User} entity'sini Spring Security'ning {@link UserDetails} interfeysiga
 * moslovchi adapter. Shu tufayli entity Security kutubxonasiga bevosita
 * bog'lanib qolmaydi (SRP — entity faqat ma'lumot, autentifikatsiya logikasi emas).
 */
public record UserPrincipal(User user) implements UserDetails {

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Spring Security hasRole('ADMIN') uchun authority "ROLE_" prefiksi bilan bo'lishi kerak.
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        // Tizimda login telefon raqami orqali amalga oshiriladi.
        return user.getPhone();
    }
}
