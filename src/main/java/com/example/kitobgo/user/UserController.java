package com.example.kitobgo.user;

import com.example.kitobgo.common.PagedResponse;
import com.example.kitobgo.security.UserPrincipal;
import com.example.kitobgo.user.dto.ChangePasswordRequest;
import com.example.kitobgo.user.dto.ChangeRoleRequest;
import com.example.kitobgo.user.dto.CreateUserRequest;
import com.example.kitobgo.user.dto.UpdateUserRequest;
import com.example.kitobgo.user.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        UserResponse response = userService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAll() {
        return ResponseEntity.ok(userService.getAll());
    }

    /** Kalit so'z (ism/telefon) va rol bo'yicha sahifalangan qidiruv. */
    @GetMapping("/search")
    public ResponseEntity<PagedResponse<UserResponse>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Role role,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(userService.search(q, role, pageable));
    }

    /** API orqali biriktirish mumkin bo'lgan rollar ro'yxati (SUPER_ADMIN'dan tashqari). */
    @GetMapping("/roles")
    public ResponseEntity<List<Role>> getAssignableRoles() {
        return ResponseEntity.ok(userService.assignableRoles());
    }

    /** Joriy (autentifikatsiyadan o'tgan) foydalanuvchi ma'lumotlarini qaytaradi. */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(UserResponse.from(principal.user()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getById(id));
    }

    /** Foydalanuvchining asosiy maydonlarini to'liq yangilaydi (parolsiz). */
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.update(id, request));
    }

    /** Foydalanuvchi parolini qayta o'rnatadi. */
    @PatchMapping("/{id}/password")
    public ResponseEntity<UserResponse> changePassword(
            @PathVariable UUID id,
            @Valid @RequestBody ChangePasswordRequest request) {
        return ResponseEntity.ok(userService.changePassword(id, request));
    }

    /** Foydalanuvchi rolini o'zgartiradi. */
    @PatchMapping("/{id}/role")
    public ResponseEntity<UserResponse> changeRole(
            @PathVariable UUID id,
            @Valid @RequestBody ChangeRoleRequest request) {
        return ResponseEntity.ok(userService.changeRole(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
