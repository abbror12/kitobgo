package com.example.kitobgo.user;

import com.example.kitobgo.common.ConflictException;
import com.example.kitobgo.user.dto.CreateUserRequest;
import com.example.kitobgo.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        userRepository.findByPhone(request.phone()).ifPresent(existing -> {
            throw new ConflictException("Bu telefon raqami allaqachon mavjud: " + request.phone());
        });

        User user = User.builder()
                .name(request.name())
                .phone(request.phone())
                .password(passwordEncoder.encode(request.password()))
                .role(request.role())
                .build();

        return UserResponse.from(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAll() {
        return userRepository.findAll().stream()
                .map(UserResponse::from)
                .toList();
    }
}
