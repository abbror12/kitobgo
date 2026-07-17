package com.example.kitobgo.notification;

import com.example.kitobgo.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeviceTokenService {

    private final DeviceTokenRepository deviceTokenRepository;

    /**
     * Upsert: token mavjud bo'lmasa qo'shadi, mavjud bo'lsa hozirgi foydalanuvchiga
     * qayta biriktiradi (bitta telefonda avval boshqa hisob kirgan bo'lishi mumkin).
     * Idempotent — ilova buni har login/ochilishda yuboradi.
     */
    @Transactional
    public void register(User user, String token, String platform) {
        deviceTokenRepository.findByToken(token).ifPresentOrElse(existing -> {
            existing.setUser(user);
            existing.setPlatform(platform);
        }, () -> deviceTokenRepository.save(DeviceToken.builder()
                .user(user)
                .token(token)
                .platform(platform)
                .build()));
    }

    /** Logout: token'ni o'chiradi — faqat shu foydalanuvchiga tegishli bo'lsa. */
    @Transactional
    public void unregister(User user, String token) {
        deviceTokenRepository.findByToken(token)
                .filter(dt -> dt.getUser().getId().equals(user.getId()))
                .ifPresent(deviceTokenRepository::delete);
    }
}
