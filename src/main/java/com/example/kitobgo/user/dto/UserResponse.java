package com.example.kitobgo.user.dto;

import com.example.kitobgo.user.Role;
import com.example.kitobgo.user.User;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String name,
        String phone,
        Role role,
        boolean online,
        LocalDateTime lastSeenAt,
        LocalDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getPhone(),
                user.getRole(),
                Boolean.TRUE.equals(user.getOnline()),
                user.getLastSeenAt(),
                user.getCreatedAt()
        );
    }
}
