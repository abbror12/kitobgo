package com.example.kitobgo.user;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String name;
    private String phone;
    private String password;
    @Enumerated(EnumType.STRING)
    private Role role;
    private LocalDateTime createdAt;
}
