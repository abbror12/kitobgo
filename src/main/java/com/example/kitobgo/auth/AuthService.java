package com.example.kitobgo.auth;

import com.example.kitobgo.common.AppTime;
import com.example.kitobgo.auth.dto.AuthResponse;
import com.example.kitobgo.auth.dto.LoginRequest;
import com.example.kitobgo.auth.dto.RefreshRequest;
import com.example.kitobgo.common.UnauthorizedException;
import com.example.kitobgo.security.JwtService;
import com.example.kitobgo.security.UserPrincipal;
import com.example.kitobgo.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    /**
     * Telefon + parolni tekshirib, muvaffaqiyatli bo'lsa access (JWT) va refresh
     * token juftligini qaytaradi. Ma'lumot noto'g'ri bo'lsa Spring Security
     * {@code 401} (BadCredentials) qaytaradi.
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.phone(), request.password()));

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = principal.user();

        // Foydalanuvchining muddati o'tgan eski tokenlarini tozalab ketamiz.
        refreshTokenRepository.deleteByUserAndExpiresAtBefore(user, AppTime.now());

        return AuthResponse.bearer(jwtService.generateToken(principal), issueRefreshToken(user));
    }

    /**
     * Refresh token evaziga yangi access + refresh juftligini beradi (rotation:
     * eski refresh token o'chiriladi va qayta ishlamaydi). Token topilmasa yoki
     * muddati o'tgan bo'lsa — 401, mijoz qaytadan login qilishi kerak.
     */
    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        RefreshToken stored = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new UnauthorizedException("Refresh token yaroqsiz"));

        refreshTokenRepository.delete(stored);

        if (stored.getExpiresAt().isBefore(AppTime.now())) {
            throw new UnauthorizedException("Refresh token muddati tugagan, qaytadan kiring");
        }

        User user = stored.getUser();
        return AuthResponse.bearer(
                jwtService.generateToken(new UserPrincipal(user)), issueRefreshToken(user));
    }

    /**
     * Logout — refresh tokenni bekor qiladi; shu qurilma endi token yangilay olmaydi.
     * (Berilgan access token o'z muddati tugaguncha amal qilib turadi.)
     */
    @Transactional
    public void logout(RefreshRequest request) {
        refreshTokenRepository.deleteByToken(request.refreshToken());
    }

    /** Yangi tasodifiy refresh token yaratib bazaga saqlaydi va qiymatini qaytaradi. */
    private String issueRefreshToken(User user) {
        byte[] bytes = new byte[48];
        SECURE_RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        refreshTokenRepository.save(RefreshToken.builder()
                .token(token)
                .user(user)
                .expiresAt(AppTime.now().plusSeconds(refreshExpirationMs / 1000))
                .build());
        return token;
    }
}
