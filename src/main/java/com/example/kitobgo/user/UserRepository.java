package com.example.kitobgo.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByPhone(String phone);

    // Operatorlarni doimiy (barqaror) tartibda qaytaradi — round-robin uchun zarur
    List<User> findByRoleOrderByCreatedAtAscIdAsc(Role role);
}
