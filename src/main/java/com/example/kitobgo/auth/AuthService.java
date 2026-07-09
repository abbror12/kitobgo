package com.example.kitobgo.auth;

import com.example.kitobgo.auth.dto.AuthResponse;
import com.example.kitobgo.auth.dto.LoginRequest;
import com.example.kitobgo.security.CustomUserDetailsService;
import com.example.kitobgo.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    /**
     * Telefon + parolni tekshirib, muvaffaqiyatli bo'lsa JWT token qaytaradi.
     * Ma'lumot noto'g'ri bo'lsa Spring Security {@code 401} (BadCredentials) qaytaradi.
     */
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.phone(), request.password()));

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.phone());
        String token = jwtService.generateToken(userDetails);
        return AuthResponse.bearer(token);
    }
}
