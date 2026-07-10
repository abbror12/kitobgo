package com.example.kitobgo.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader(HEADER);
        if (authHeader == null || !authHeader.startsWith(PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(PREFIX.length());

        try {
            String phone = jwtService.extractUsername(token);

            // Foydalanuvchi aniqlangan va hali kontekstga qo'yilmagan bo'lsa
            if (phone != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(phone);

                if (jwtService.isTokenValid(token, userDetails)) {
                    var authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception ex) {
            // Buzuq yoki muddati o'tgan token — autentifikatsiyasiz davom etamiz.
            // Security keyinchalik himoyalangan endpoint uchun 401/403 qaytaradi.
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
