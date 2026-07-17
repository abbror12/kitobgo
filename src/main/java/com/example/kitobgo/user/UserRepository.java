package com.example.kitobgo.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository
        extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {
    Optional<User> findByPhone(String phone);

    boolean existsByRoleIn(Collection<Role> roles);

    List<User> findByRoleOrderByCreatedAtAscIdAsc(Role role);

    /**
     * Ayni damda "mavjud" (online va heartbeat'i tirik) operatorlar.
     * {@code online=true} va {@code lastSeenAt > threshold} bo'lganlar.
     */
    List<User> findByRoleAndOnlineTrueAndLastSeenAtAfterOrderByCreatedAtAscIdAsc(
            Role role, LocalDateTime threshold);
}
