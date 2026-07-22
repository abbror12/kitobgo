package com.example.kitobgo.security;

import com.example.kitobgo.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Cacheable(cacheNames = "userDetails", key = "#phone", sync = true)
    public UserDetails loadUserByUsername(String phone) throws UsernameNotFoundException {
        return userRepository.findByPhone(phone)
                .map(UserPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("Foydalanuvchi topilmadi: " + phone));
    }
}
