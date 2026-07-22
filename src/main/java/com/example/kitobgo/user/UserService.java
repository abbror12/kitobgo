package com.example.kitobgo.user;

import com.example.kitobgo.common.ConflictException;
import com.example.kitobgo.common.NotFoundException;
import com.example.kitobgo.common.PagedResponse;
import com.example.kitobgo.user.dto.ChangePasswordRequest;
import com.example.kitobgo.user.dto.ChangeRoleRequest;
import com.example.kitobgo.user.dto.CreateUserRequest;
import com.example.kitobgo.user.dto.UpdateUserRequest;
import com.example.kitobgo.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    @CacheEvict(cacheNames = "userDetails", allEntries = true)
    public UserResponse create(CreateUserRequest request) {
        assertAssignableRole(request.role());
        assertPhoneAvailable(request.phone(), null);

        User user = User.builder()
                .name(request.name())
                .phone(request.phone())
                .password(passwordEncoder.encode(request.password()))
                .role(request.role())
                .build();

        return UserResponse.from(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public UserResponse getById(UUID id) {
        return UserResponse.from(getUserOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAll() {
        return userRepository.findAll().stream()
                .map(UserResponse::from)
                .toList();
    }

    /**
     * Kalit so'z va rol bo'yicha sahifalangan qidiruv.
     * Barcha filtrlar ixtiyoriy (null bo'lsa e'tiborsiz qoldiriladi).
     */
    @Transactional(readOnly = true)
    public PagedResponse<UserResponse> search(String q, Role role, Pageable pageable) {
        Specification<User> spec = UserSpecifications.withFilters(q, role);
        Page<UserResponse> page = userRepository.findAll(spec, pageable)
                .map(UserResponse::from);
        return PagedResponse.from(page);
    }

    /**
     * Foydalanuvchining asosiy maydonlarini to'liq yangilaydi (parolsiz, PUT semantikasi).
     */
    @Transactional
    @CacheEvict(cacheNames = "userDetails", allEntries = true)
    public UserResponse update(UUID id, UpdateUserRequest request) {
        User user = getUserOrThrow(id);
        assertNotSuperAdmin(user);
        assertAssignableRole(request.role());
        assertPhoneAvailable(request.phone(), id);

        user.setName(request.name());
        user.setPhone(request.phone());
        user.setRole(request.role());

        return UserResponse.from(userRepository.save(user));
    }

    /** Adminning foydalanuvchi parolini qayta o'rnatishi. */
    @Transactional
    @CacheEvict(cacheNames = "userDetails", allEntries = true)
    public UserResponse changePassword(UUID id, ChangePasswordRequest request) {
        User user = getUserOrThrow(id);
        assertNotSuperAdmin(user);

        user.setPassword(passwordEncoder.encode(request.password()));
        return UserResponse.from(userRepository.save(user));
    }

    /** Foydalanuvchi rolini o'zgartiradi. */
    @Transactional
    @CacheEvict(cacheNames = "userDetails", allEntries = true)
    public UserResponse changeRole(UUID id, ChangeRoleRequest request) {
        User user = getUserOrThrow(id);
        assertNotSuperAdmin(user);
        assertAssignableRole(request.role());

        user.setRole(request.role());
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    @CacheEvict(cacheNames = "userDetails", allEntries = true)
    public void delete(UUID id) {
        User user = getUserOrThrow(id);
        assertNotSuperAdmin(user);
        userRepository.delete(user);
    }

    /** API orqali biriktirish mumkin bo'lgan rollar (SUPER_ADMIN'dan tashqari). */
    public List<Role> assignableRoles() {
        return Arrays.stream(Role.values())
                .filter(role -> role != Role.SUPER_ADMIN)
                .toList();
    }

    // --- Yordamchi metodlar ---

    private User getUserOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Foydalanuvchi topilmadi: " + id));
    }

    /** SUPER_ADMIN rolini API orqali biriktirishni taqiqlaydi. */
    private void assertAssignableRole(Role role) {
        if (role == Role.SUPER_ADMIN) {
            throw new IllegalArgumentException("SUPER_ADMIN rolini API orqali biriktirib bo'lmaydi");
        }
    }

    /** SUPER_ADMIN hisobini API orqali o'zgartirish/o'chirishdan himoyalaydi. */
    private void assertNotSuperAdmin(User user) {
        if (user.getRole() == Role.SUPER_ADMIN) {
            throw new IllegalArgumentException("SUPER_ADMIN hisobini API orqali o'zgartirib yoki o'chirib bo'lmaydi");
        }
    }

    /** Telefon raqami boshqa foydalanuvchiga tegishli emasligini tekshiradi ({@code selfId} — o'zini istisno qilish). */
    private void assertPhoneAvailable(String phone, UUID selfId) {
        userRepository.findByPhone(phone)
                .filter(existing -> !existing.getId().equals(selfId))
                .ifPresent(existing -> {
                    throw new ConflictException("Bu telefon raqami allaqachon mavjud: " + phone);
                });
    }
}
